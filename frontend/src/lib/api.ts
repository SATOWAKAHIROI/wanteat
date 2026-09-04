const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

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

  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "エラーが発生しました" }));
    throw new Error(error.message ?? "エラーが発生しました");
  }

  if (res.status === 204) return undefined as T;
  return res.json();
}
