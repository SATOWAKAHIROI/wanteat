package com.example.wanteat.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.example.wanteat.domain.AiPhase;
import com.example.wanteat.domain.RequestStatus;
import com.example.wanteat.domain.UserPreference;
import com.example.wanteat.dto.GeneratedMenuDetail;
import com.example.wanteat.dto.SuggestRequest;
import com.example.wanteat.dto.SuggestResponse;
import com.example.wanteat.dto.SuggestedDish;
import com.example.wanteat.exception.AiGenerationException;
import com.example.wanteat.repository.DishRepository;
import com.example.wanteat.repository.RequestRepository;
import com.example.wanteat.repository.UserPreferenceRepository;

import lombok.RequiredArgsConstructor;

/**
 * 献立提案の AI 連携。2段階で生成する。
 *
 * <p>第1段階（SUGGEST）は料理名・説明・調理時間・主な材料だけを返して素早く提示し、
 * 第2段階（DETAIL）は確定した1件についてのみ材料・分量・手順を生成する。
 * 「別の案」を何度押しても軽い生成しか走らないようにするための分割。
 *
 * <p>このクラスはトランザクション境界の外で呼ぶこと。AI の応答には十数秒かかるため、
 * DB コネクションを掴んだまま待たせない。
 */
@Service
@RequiredArgsConstructor
public class AiMenuService {

    private static final Logger log = LoggerFactory.getLogger(AiMenuService.class);

    /** 重複提案を避けるために参照する履歴の日数。 */
    private static final int HISTORY_DAYS = 14;

    private static final String SUGGEST_SYSTEM_PROMPT = """
            あなたは日本の家庭料理に詳しい献立アドバイザーです。
            夕食の主菜と副菜を1品ずつ提案してください。

            厳守すること:
            - アレルギーとして挙げられた食材は絶対に使わない
            - 苦手な食材は使わない
            - 最近作った料理と同じもの、および調理法や主材料が似すぎたものは提案しない
            - 却下された料理は再提案しない
            - スーパーで普通に買える食材だけを使う
            - 主菜と副菜で調理法が重ならないようにする（両方を揚げ物にしない、など）

            出力の決まり:
            - description は20〜40字程度で、その料理の魅力が伝わる一文にする
            - mainIngredients は3〜5個。分量は書かず食材名だけを挙げる
            - cookingMinutes は各料理の調理にかかる現実的な分数
            - 調理時間の上限が指定された場合、それは主菜と副菜を合わせた合計の目安である。
              主菜の cookingMinutes と副菜の cookingMinutes の合計が上限を超えないようにする
            """;

    private static final String DETAIL_SYSTEM_PROMPT = """
            あなたは日本の家庭料理に詳しい料理研究家です。
            指定された2品について、材料と分量、調理手順を作成してください。

            厳守すること:
            - アレルギーとして挙げられた食材は絶対に使わない
            - 指定された人数分の分量にする

            出力の決まり:
            - amount には数量を入れる。「大さじ」「小さじ」「少々」「適量」は単位を前に付けた形
              （「大さじ2」「小さじ1」「少々」）で amount に入れ、unit は空文字にする
            - unit には「g」「ml」「本」「個」「束」のように、数量のうしろに付く単位だけを入れる
            - steps は1ステップ1文。5〜8ステップ程度にまとめる
            - pantryStaple には、一般的な家庭に常備されていて買い足す必要がないものは true を入れる。
              水・湯・塩・こしょう・砂糖・しょうゆ・みりん・料理酒・酢・サラダ油・ごま油・
              和風だしの素・コンソメ・片栗粉・小麦粉・米・ご飯 などが該当する。
              肉・魚・野菜・乳製品・卵・豆腐・麺・カレールーなど、買い物が必要なものは false にする
            - 常備品も材料からは省かず、pantryStaple: true を付けて必ず列挙する
            """;

    private final UserPreferenceRepository userPreferenceRepository;
    private final DishRepository dishRepository;
    private final RequestRepository requestRepository;
    private final AiGenerationLogger aiGenerationLogger;

    @Value("${ai.model}")
    private String model;

    @Value("${ai.api-key:}")
    private String apiKey;

    private volatile AnthropicClient client;

    /** 第1段階。主菜と副菜を1品ずつ提案する。DB には書き込まない。 */
    public SuggestResponse suggest(Long userId, SuggestRequest request) {
        String prompt = buildSuggestPrompt(userId, request);
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("prompt", prompt);

        SuggestResponse response = call(userId, AiPhase.SUGGEST, SUGGEST_SYSTEM_PROMPT, prompt,
                SuggestResponse.class, requestPayload);

        if (response.mainDish() == null || response.sideDish() == null) {
            throw new AiGenerationException("提案の生成に失敗しました。もう一度お試しください");
        }
        return response;
    }

    /** 第2段階。確定した2品について材料・分量・手順を生成する。 */
    public GeneratedMenuDetail generateDetail(Long userId, SuggestedDish mainDish, SuggestedDish sideDish) {
        String prompt = buildDetailPrompt(userId, mainDish, sideDish);
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("prompt", prompt);
        requestPayload.put("mainDish", mainDish.name());
        requestPayload.put("sideDish", sideDish.name());

        GeneratedMenuDetail detail = call(userId, AiPhase.DETAIL, DETAIL_SYSTEM_PROMPT, prompt,
                GeneratedMenuDetail.class, requestPayload);

        if (detail.mainDish() == null || detail.sideDish() == null) {
            throw new AiGenerationException("レシピの生成に失敗しました。もう一度お試しください");
        }
        return detail;
    }

    // ---- プロンプト組み立て ----

    private String buildSuggestPrompt(Long userId, SuggestRequest request) {
        StringBuilder sb = new StringBuilder();
        appendPreference(sb, userId);

        List<String> recentDishes =
                dishRepository.findRecentDishNames(userId, LocalDate.now().minusDays(HISTORY_DAYS));
        if (!recentDishes.isEmpty()) {
            sb.append("## 直近").append(HISTORY_DAYS).append("日間に作った料理（避けること）\n");
            recentDishes.forEach(name -> sb.append("- ").append(name).append('\n'));
            sb.append('\n');
        }

        List<String> openRequests = requestRepository
                .findByUserIdAndStatusOrderByCreatedAtDescIdDesc(userId, RequestStatus.OPEN)
                .stream().map(r -> r.getBody()).toList();
        if (!openRequests.isEmpty()) {
            sb.append("## 家族からのリクエスト（可能なら反映すること）\n");
            openRequests.forEach(body -> sb.append("- ").append(body).append('\n'));
            sb.append('\n');
        }

        if (request != null) {
            sb.append("## 今日の条件\n");
            if (request.maxCookingMinutes() != null) {
                sb.append("- 調理時間は合計").append(request.maxCookingMinutes()).append("分以内\n");
            }
            if (hasText(request.ingredients())) {
                sb.append("- 使いたい食材: ").append(request.ingredients()).append('\n');
            }
            if (hasText(request.mood())) {
                sb.append("- 気分・要望: ").append(request.mood()).append('\n');
            }
            if (request.excludeDishNames() != null && !request.excludeDishNames().isEmpty()) {
                sb.append("- 却下された料理（再提案しないこと）: ")
                        .append(String.join("、", request.excludeDishNames())).append('\n');
            }
            sb.append('\n');
        }

        sb.append("以上を踏まえて、今日の夕食の主菜と副菜を1品ずつ提案してください。");
        return sb.toString();
    }

    private String buildDetailPrompt(Long userId, SuggestedDish mainDish, SuggestedDish sideDish) {
        StringBuilder sb = new StringBuilder();
        appendPreference(sb, userId);

        sb.append("## 主菜\n")
                .append("- 料理名: ").append(mainDish.name()).append('\n');
        appendIfPresent(sb, "- 説明: ", mainDish.description());
        appendIngredientHint(sb, mainDish);

        sb.append("\n## 副菜\n")
                .append("- 料理名: ").append(sideDish.name()).append('\n');
        appendIfPresent(sb, "- 説明: ", sideDish.description());
        appendIngredientHint(sb, sideDish);

        sb.append("\nこの2品について、材料と分量、調理手順を作成してください。");
        return sb.toString();
    }

    private void appendPreference(StringBuilder sb, Long userId) {
        UserPreference preference = userPreferenceRepository.findByUserId(userId).orElse(null);
        sb.append("## 前提\n");
        if (preference == null) {
            sb.append("- 人数: 2人分\n\n");
            return;
        }
        sb.append("- 人数: ").append(preference.getHouseholdSize()).append("人分\n");
        appendIfPresent(sb, "- アレルギー（絶対に使わない）: ", preference.getAllergies());
        appendIfPresent(sb, "- 苦手な食材: ", preference.getDislikedFoods());
        appendIfPresent(sb, "- 好みの傾向: ", preference.getNote());
        sb.append('\n');
    }

    private void appendIngredientHint(StringBuilder sb, SuggestedDish dish) {
        if (dish.mainIngredients() != null && !dish.mainIngredients().isEmpty()) {
            sb.append("- 主な材料: ").append(String.join("、", dish.mainIngredients())).append('\n');
        }
    }

    private void appendIfPresent(StringBuilder sb, String label, String value) {
        if (hasText(value)) {
            sb.append(label).append(value).append('\n');
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // ---- API 呼び出し ----

    private <T> T call(Long userId, AiPhase phase, String systemPrompt, String userPrompt,
            Class<T> responseType, Map<String, Object> requestPayload) {

        long startedAt = System.currentTimeMillis();
        try {
            StructuredMessageCreateParams<T> params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(16000L)
                    .system(systemPrompt)
                    // outputConfig(OutputConfig) を併用してはいけない。
                    // 後勝ちでスキーマが消え、AI が Markdown を返して落ちる。
                    .outputConfig(responseType)
                    .addUserMessage(userPrompt)
                    .build();

            var message = anthropicClient().messages().create(params);

            T result = message.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(text -> text.text())
                    .findFirst()
                    .orElseThrow(() -> new AiGenerationException("AI から結果を取得できませんでした"));

            int latencyMs = (int) (System.currentTimeMillis() - startedAt);
            logGeneration(userId, phase, requestPayload, result,
                    (int) message.usage().inputTokens(),
                    (int) message.usage().outputTokens(),
                    latencyMs);
            return result;

        } catch (AiGenerationException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("AI 生成に失敗しました phase={}", phase, e);
            throw new AiGenerationException("AI の呼び出しに失敗しました。時間をおいて再度お試しください", e);
        }
    }

    /** ログの失敗で本体を落とさない。 */
    private <T> void logGeneration(Long userId, AiPhase phase, Map<String, Object> requestPayload,
            T result, Integer inputTokens, Integer outputTokens, int latencyMs) {
        try {
            Map<String, Object> responsePayload = new HashMap<>();
            responsePayload.put("result", result);
            aiGenerationLogger.log(userId, phase, requestPayload, responsePayload, model,
                    inputTokens, outputTokens, latencyMs);
        } catch (RuntimeException e) {
            log.warn("AI 呼び出しログの保存に失敗しました phase={}", phase, e);
        }
    }

    /**
     * API キーが無い環境でもアプリを起動できるよう、初回利用時に生成する。
     */
    private AnthropicClient anthropicClient() {
        AnthropicClient local = client;
        if (local == null) {
            synchronized (this) {
                local = client;
                if (local == null) {
                    if (!hasText(apiKey)) {
                        throw new AiGenerationException(
                                "ANTHROPIC_API_KEY が設定されていません。ルートの .env に設定してください");
                    }
                    local = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
                    client = local;
                }
            }
        }
        return local;
    }
}
