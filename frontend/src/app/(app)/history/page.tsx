"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ChevronRight, Loader2 } from "lucide-react";

import { apiFetch } from "@/lib/api";
import type { Menu } from "@/lib/types";

export default function HistoryPage() {
  const [menus, setMenus] = useState<Menu[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        setMenus(await apiFetch<Menu[]>("/api/menus"));
      } catch (e) {
        setError(e instanceof Error ? e.message : "取得に失敗しました");
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  return (
    <div className="space-y-4">
      <header>
        <h1 className="text-xl font-semibold tracking-tight">履歴</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          直近の献立は提案時の重複回避に使われます
        </p>
      </header>

      {error && (
        <p className="rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {error}
        </p>
      )}

      {loading ? (
        <div className="flex justify-center py-10">
          <Loader2 className="size-6 animate-spin text-muted-foreground" aria-hidden />
        </div>
      ) : menus.length === 0 ? (
        <p className="py-10 text-center text-sm text-muted-foreground">
          まだ献立がありません
        </p>
      ) : (
        <ul className="divide-y divide-border">
          {menus.map((menu) => (
            <li key={menu.id}>
              <Link
                href={`/menus/${menu.id}`}
                className="flex items-center gap-3 py-3.5 transition-colors hover:text-primary"
              >
                <div className="min-w-0 flex-1">
                  <p className="flex items-center gap-2 text-xs text-muted-foreground">
                    {menu.cookedOn}
                    {menu.status === "COOKED" && (
                      <span className="rounded bg-muted px-1.5 py-px">調理済み</span>
                    )}
                  </p>
                  <p className="mt-0.5 truncate text-sm">
                    {menu.dishes.map((d) => d.name).join(" / ")}
                  </p>
                </div>
                <ChevronRight className="size-4 shrink-0 text-muted-foreground" aria-hidden />
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
