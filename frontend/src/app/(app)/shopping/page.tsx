"use client";

import { useEffect, useState } from "react";
import { Loader2, Plus, Trash2 } from "lucide-react";

import { apiFetch } from "@/lib/api";
import type { ShoppingItem } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export default function ShoppingPage() {
  const [items, setItems] = useState<ShoppingItem[]>([]);
  const [name, setName] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const result = await apiFetch<ShoppingItem[]>("/api/shopping-items");
        if (!cancelled) setItems(result);
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

  const toggle = async (item: ShoppingItem) => {
    // スーパーで連打されるので、往復を待たずに見た目を先に変える
    setItems((prev) =>
      prev.map((i) => (i.id === item.id ? { ...i, checked: !i.checked } : i))
    );
    try {
      const updated = await apiFetch<ShoppingItem>(`/api/shopping-items/${item.id}`, {
        method: "PATCH",
      });
      setItems((prev) => prev.map((i) => (i.id === updated.id ? updated : i)));
    } catch {
      setItems((prev) =>
        prev.map((i) => (i.id === item.id ? { ...i, checked: item.checked } : i))
      );
    }
  };

  const add = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    try {
      const created = await apiFetch<ShoppingItem>("/api/shopping-items", {
        method: "POST",
        body: JSON.stringify({ name: name.trim() }),
      });
      setItems((prev) => [...prev, created]);
      setName("");
    } catch (e) {
      setError(e instanceof Error ? e.message : "追加に失敗しました");
    }
  };

  const remove = async (id: number) => {
    try {
      await apiFetch<void>(`/api/shopping-items/${id}`, { method: "DELETE" });
      setItems((prev) => prev.filter((i) => i.id !== id));
    } catch (e) {
      setError(e instanceof Error ? e.message : "削除に失敗しました");
    }
  };

  const clearChecked = async () => {
    try {
      await apiFetch<void>("/api/shopping-items/checked", { method: "DELETE" });
      setItems((prev) => prev.filter((i) => !i.checked));
    } catch (e) {
      setError(e instanceof Error ? e.message : "削除に失敗しました");
    }
  };

  const checkedCount = items.filter((i) => i.checked).length;

  return (
    <div className="space-y-4">
      <header className="flex items-baseline justify-between">
        <h1 className="text-xl font-semibold tracking-tight">買い物リスト</h1>
        <span className="text-sm text-muted-foreground">
          {items.length - checkedCount} / {items.length}
        </span>
      </header>

      {error && (
        <p className="rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {error}
        </p>
      )}

      <form onSubmit={add} className="flex gap-2">
        <Input
          placeholder="買い足すものを追加"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <Button type="submit" size="icon" aria-label="追加">
          <Plus aria-hidden />
        </Button>
      </form>

      {loading ? (
        <div className="flex justify-center py-10">
          <Loader2 className="size-6 animate-spin text-muted-foreground" aria-hidden />
        </div>
      ) : items.length === 0 ? (
        <p className="py-10 text-center text-sm text-muted-foreground">
          献立を確定すると材料がここに並びます
        </p>
      ) : (
        <ul className="divide-y divide-border">
          {items.map((item) => (
            <li key={item.id} className="flex items-center gap-3">
              {/* タップ領域を行全体に広げ、片手で操作できるようにする */}
              <label className="flex flex-1 cursor-pointer items-center gap-3 py-3">
                <input
                  type="checkbox"
                  checked={item.checked}
                  onChange={() => toggle(item)}
                  className="size-5 shrink-0 accent-primary"
                />
                <span
                  className={`flex-1 text-sm ${
                    item.checked ? "text-muted-foreground line-through" : ""
                  }`}
                >
                  {item.name}
                </span>
                <span className="shrink-0 text-sm tabular-nums text-muted-foreground">
                  {item.amount ?? ""}
                  {item.unit ?? ""}
                </span>
              </label>
              <Button
                variant="ghost"
                size="icon-sm"
                onClick={() => remove(item.id)}
                aria-label={`${item.name} を削除`}
              >
                <Trash2 aria-hidden />
              </Button>
            </li>
          ))}
        </ul>
      )}

      {checkedCount > 0 && (
        <Button variant="outline" className="w-full" onClick={clearChecked}>
          購入済みの {checkedCount} 件を消す
        </Button>
      )}
    </div>
  );
}
