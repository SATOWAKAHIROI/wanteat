const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

/** 認証切れで投げられるエラー。画面側で握りつぶしてよい。 */
export class UnauthorizedError extends Error {
  constructor() {
    super("ログインが必要です");
    this.name = "UnauthorizedError";
  }
}

export async function apiFetch<T>(
  path: string,
  options?: RequestInit
): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    credentials: "include", // HTTP-only Cookie（JWTトークン）を自動送信
    headers: {
      "Content-Type": "application/json",
      ...options?.headers,
    },
    ...options,
  });

  // Cookie の期限切れ。ログイン画面自身での 401 は無限ループになるため除外する
  if (
    res.status === 401 &&
    typeof window !== "undefined" &&
    !window.location.pathname.startsWith("/login")
  ) {
    // セッション切れでは画面遷移ではなく全体リロードさせる。
    // 各画面がメモリに持っている取得済みデータを確実に捨てるため。
    // eslint-disable-next-line @next/next/no-location-assign-relative-destination
    window.location.href = "/login";
    throw new UnauthorizedError();
  }

  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "エラーが発生しました" }));
    throw new Error(error.message ?? "エラーが発生しました");
  }

  if (res.status === 204) return undefined as T;

  // login / logout は 200 でも本文が空（トークンは Set-Cookie ヘッダで返る）。
  // ステータスではなく本文の有無で判断する。
  const text = await res.text();
  if (!text) return undefined as T;
  return JSON.parse(text) as T;
}
