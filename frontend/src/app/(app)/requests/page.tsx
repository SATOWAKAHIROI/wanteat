"use client";

import { useEffect, useState } from "react";
import { Check, Loader2, Plus, Trash2 } from "lucide-react";

import { apiFetch } from "@/lib/api";
import type { MealRequest, RequestedBy } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export default function RequestsPage() {
  const [requests, setRequests] = useState<MealRequest[]>([]);
  const [body, setBody] = useState("");
  const [requestedBy, setRequestedBy] = useState<RequestedBy>("PARTNER");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const result = await apiFetch<MealRequest[]>("/api/requests");
        if (!cancelled) setRequests(result);
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : "取得に失敗しました");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const add = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!body.trim()) return;
    try {
      const created = await apiFetch<MealRequest>("/api/requests", {
        method: "POST",
        body: JSON.stringify({ requestedBy, body: body.trim() }),
      });
      setRequests((prev) => [created, ...prev]);
      setBody("");
    } catch (e) {
      setError(e instanceof Error ? e.message : "追加に失敗しました");
    }
  };

  const fulfill = async (id: number) => {
    try {
      const updated = await apiFetch<MealRequest>(`/api/requests/${id}`, { method: "PATCH" });
      setRequests((prev) => prev.map((r) => (r.id === id ? updated : r)));
    } catch (e) {
      setError(e instanceof Error ? e.message : "更新に失敗しました");
    }
  };

  const remove = async (id: number) => {
    try {
      await apiFetch<void>(`/api/requests/${id}`, { method: "DELETE" });
      setRequests((prev) => prev.filter((r) => r.id !== id));
    } catch (e) {
      setError(e instanceof Error ? e.message : "削除に失敗しました");
    }
  };

  const open = requests.filter((r) => r.status === "OPEN");
  const fulfilled = requests.filter((r) => r.status === "FULFILLED");

  return (
    <div className="space-y-4">
      <header>
        <h1 className="text-xl font-semibold tracking-tight">リクエスト</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          未消化のものは提案時に考慮されます
        </p>
      </header>

      {error && (
        <p className="rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {error}
        </p>
      )}

      <form onSubmit={add} className="space-y-2">
        <div className="flex gap-2">
          <Input
            placeholder="カレーが食べたい"
            value={body}
            onChange={(e) => setBody(e.target.value)}
          />
          <Button type="submit" size="icon" aria-label="追加">
            <Plus aria-hidden />
          </Button>
        </div>
        <div className="flex gap-2">
          {(["PARTNER", "SELF"] as const).map((who) => (
            <Button
              key={who}
              type="button"
              variant={requestedBy === who ? "default" : "outline"}
              size="sm"
              onClick={() => setRequestedBy(who)}
            >
              {who === "PARTNER" ? "パートナー" : "自分"}
            </Button>
          ))}
        </div>
      </form>

      {loading ? (
        <div className="flex justify-center py-10">
          <Loader2 className="size-6 animate-spin text-muted-foreground" aria-hidden />
        </div>
      ) : (
        <div className="space-y-6">
          <section>
            <h2 className="mb-1 text-sm font-medium">未消化（{open.length}）</h2>
            {open.length === 0 ? (
              <p className="py-4 text-sm text-muted-foreground">ありません</p>
            ) : (
              <ul className="divide-y divide-border">
                {open.map((request) => (
                  <li key={request.id} className="flex items-center gap-2 py-3">
                    <span className="shrink-0 rounded bg-muted px-1.5 py-0.5 text-[10px] text-muted-foreground">
                      {request.requestedBy === "PARTNER" ? "パートナー" : "自分"}
                    </span>
                    <span className="flex-1 text-sm">{request.body}</span>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => fulfill(request.id)}
                      aria-label="消化済みにする"
                    >
                      <Check aria-hidden />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => remove(request.id)}
                      aria-label="削除"
                    >
                      <Trash2 aria-hidden />
                    </Button>
                  </li>
                ))}
              </ul>
            )}
          </section>

          {fulfilled.length > 0 && (
            <section>
              <h2 className="mb-1 text-sm font-medium text-muted-foreground">
                消化済み（{fulfilled.length}）
              </h2>
              <ul className="divide-y divide-border">
                {fulfilled.map((request) => (
                  <li key={request.id} className="flex items-center gap-2 py-2.5">
                    <span className="flex-1 text-sm text-muted-foreground line-through">
                      {request.body}
                    </span>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => remove(request.id)}
                      aria-label="削除"
                    >
                      <Trash2 aria-hidden />
                    </Button>
                  </li>
                ))}
              </ul>
            </section>
          )}
        </div>
      )}
    </div>
  );
}
