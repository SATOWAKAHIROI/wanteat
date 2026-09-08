"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Loader2, RefreshCw, Sparkles } from "lucide-react";

import { apiFetch } from "@/lib/api";
import type { Menu, SuggestedDish, SuggestResponse } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

/** ローカル日付を YYYY-MM-DD で返す。UTC 変換で日付がずれないよう自前で組み立てる。 */
function today(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

type Pending = "suggest" | "confirm" | null;

export default function HomePage() {
  const router = useRouter();

  const [maxCookingMinutes, setMaxCookingMinutes] = useState("");
  const [ingredients, setIngredients] = useState("");
  const [mood, setMood] = useState("");

  const [suggestion, setSuggestion] = useState<SuggestResponse | null>(null);
  const [rejected, setRejected] = useState<string[]>([]);
  const [pending, setPending] = useState<Pending>(null);
  const [elapsed, setElapsed] = useState(0);
  const [error, setError] = useState<string | null>(null);

  // AI の応答は10秒以上かかる。無言で待たせると壊れて見えるので経過秒数を出す
  useEffect(() => {
    if (!pending) return;
    const id = setInterval(() => setElapsed((e) => e + 1), 1000);
    return () => clearInterval(id);
  }, [pending]);

  const suggest = async (excludeDishNames: string[]) => {
    setPending("suggest");
    setElapsed(0);
    setError(null);
    try {
      const minutes = Number(maxCookingMinutes);
      const result = await apiFetch<SuggestResponse>("/api/menus/suggest", {
        method: "POST",
        body: JSON.stringify({
          maxCookingMinutes: Number.isFinite(minutes) && minutes > 0 ? minutes : null,
          ingredients: ingredients.trim() || null,
          mood: mood.trim() || null,
          excludeDishNames,
        }),
      });
      setSuggestion(result);
      setRejected(excludeDishNames);
    } catch (e) {
      setError(e instanceof Error ? e.message : "提案の取得に失敗しました");
    } finally {
      setPending(null);
    }
  };

  /** 前の案は捨てて新しい1案に差し替える。並べて比較させない。 */
  const suggestAnother = () => {
    if (!suggestion) return;
    suggest([...rejected, suggestion.mainDish.name, suggestion.sideDish.name]);
  };

  const confirm = async () => {
    if (!suggestion) return;
    setPending("confirm");
    setElapsed(0);
    setError(null);
    try {
      const menu = await apiFetch<Menu>("/api/menus/confirm", {
        method: "POST",
        body: JSON.stringify({
          cookedOn: today(),
          mainDish: suggestion.mainDish,
          sideDish: suggestion.sideDish,
        }),
      });
      router.push(`/menus/${menu.id}`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "献立の確定に失敗しました");
      setPending(null);
    }
  };

  if (pending) {
    return <Waiting phase={pending} elapsed={elapsed} />;
  }

  return (
    <div className="space-y-5">
      <header>
        <h1 className="text-xl font-semibold tracking-tight">今日の献立</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          条件は空のままでも大丈夫です
        </p>
      </header>

      {error && (
        <p className="rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {error}
        </p>
      )}

      {suggestion ? (
        <div className="space-y-4">
          <SuggestionCard label="主菜" dish={suggestion.mainDish} />
          <SuggestionCard label="副菜" dish={suggestion.sideDish} />

          <div className="flex flex-col gap-2">
            <Button size="lg" className="h-12 w-full text-base" onClick={confirm}>
              これにする
            </Button>
            <Button
              variant="outline"
              size="lg"
              className="h-11 w-full"
              onClick={suggestAnother}
            >
              <RefreshCw aria-hidden />
              別の案
            </Button>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="minutes">調理時間（分・主菜と副菜の合計）</Label>
            <Input
              id="minutes"
              inputMode="numeric"
              placeholder="30"
              value={maxCookingMinutes}
              onChange={(e) => setMaxCookingMinutes(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="ingredients">使いたい食材</Label>
            <Input
              id="ingredients"
              placeholder="豚こま肉を使い切りたい"
              value={ingredients}
              onChange={(e) => setIngredients(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="mood">気分・要望</Label>
            <Input
              id="mood"
              placeholder="あっさりしたものがいい"
              value={mood}
              onChange={(e) => setMood(e.target.value)}
            />
          </div>

          <Button
            size="lg"
            className="h-12 w-full text-base"
            onClick={() => suggest([])}
          >
            <Sparkles aria-hidden />
            考えてもらう
          </Button>
        </div>
      )}
    </div>
  );
}

function SuggestionCard({ label, dish }: { label: string; dish: SuggestedDish }) {
  return (
    <Card>
      <CardHeader>
        <p className="text-xs font-medium text-muted-foreground">{label}</p>
        <CardTitle className="text-lg">{dish.name}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        {dish.description && (
          <p className="text-sm text-muted-foreground">{dish.description}</p>
        )}
        <div className="flex flex-wrap items-center gap-2 text-xs">
          {dish.cookingMinutes != null && (
            <span className="rounded-full bg-secondary px-2 py-0.5 text-secondary-foreground">
              約{dish.cookingMinutes}分
            </span>
          )}
          {dish.mainIngredients?.map((name) => (
            <span key={name} className="rounded-full bg-muted px-2 py-0.5 text-muted-foreground">
              {name}
            </span>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

function Waiting({ phase, elapsed }: { phase: "suggest" | "confirm"; elapsed: number }) {
  const suggesting = phase === "suggest";
  return (
    <div className="flex min-h-[60dvh] flex-col items-center justify-center gap-3 text-center">
      <Loader2 className="size-8 animate-spin text-primary" aria-hidden />
      <p className="text-base font-medium">
        {suggesting ? "献立を考えています" : "レシピを作っています"}
      </p>
      <p className="text-sm text-muted-foreground">
        {suggesting ? "10秒ほどかかります" : "20秒ほどかかります"}
      </p>
      <p className="text-sm tabular-nums text-muted-foreground">{elapsed} 秒</p>
    </div>
  );
}
