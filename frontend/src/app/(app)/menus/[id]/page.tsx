"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Check, Loader2 } from "lucide-react";

import { apiFetch } from "@/lib/api";
import type { Dish, Menu } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function MenuDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();

  const [menu, setMenu] = useState<Menu | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const result = await apiFetch<Menu>(`/api/menus/${params.id}`);
        if (!cancelled) setMenu(result);
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : "献立の取得に失敗しました");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [params.id]);

  const markCooked = async () => {
    try {
      setMenu(await apiFetch<Menu>(`/api/menus/${params.id}/cooked`, { method: "PATCH" }));
    } catch (e) {
      setError(e instanceof Error ? e.message : "更新に失敗しました");
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-[50dvh] items-center justify-center">
        <Loader2 className="size-6 animate-spin text-muted-foreground" aria-hidden />
      </div>
    );
  }

  if (error || !menu) {
    return <p className="text-sm text-destructive">{error ?? "献立が見つかりません"}</p>;
  }

  const cooked = menu.status === "COOKED";

  return (
    <div className="space-y-5">
      <div className="flex items-center gap-2">
        <Button variant="ghost" size="icon-sm" onClick={() => router.back()} aria-label="戻る">
          <ArrowLeft aria-hidden />
        </Button>
        <div>
          <h1 className="text-lg font-semibold tracking-tight">{menu.cookedOn}</h1>
          {cooked && <p className="text-xs text-muted-foreground">調理済み</p>}
        </div>
      </div>

      {menu.dishes.map((dish) => (
        <DishSection key={dish.id} dish={dish} />
      ))}

      {!cooked && (
        <Button size="lg" className="h-12 w-full text-base" onClick={markCooked}>
          <Check aria-hidden />
          作った
        </Button>
      )}
    </div>
  );
}

function DishSection({ dish }: { dish: Dish }) {
  return (
    <Card>
      <CardHeader>
        <p className="text-xs font-medium text-muted-foreground">
          {dish.category === "MAIN" ? "主菜" : "副菜"}
          {dish.cookingMinutes != null && ` ・ 約${dish.cookingMinutes}分`}
        </p>
        <CardTitle className="text-lg">{dish.name}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-5">
        {dish.description && (
          <p className="text-sm text-muted-foreground">{dish.description}</p>
        )}

        <section>
          <h2 className="mb-2 text-sm font-medium">材料</h2>
          <ul className="divide-y divide-border text-sm">
            {dish.ingredients.map((ingredient) => (
              <li
                key={ingredient.id}
                className={`flex items-center justify-between gap-3 py-1.5 ${
                  // 常備品は「買わなくていいもの」なので控えめに見せる
                  ingredient.pantryStaple ? "text-muted-foreground" : ""
                }`}
              >
                <span className="flex items-center gap-1.5">
                  {ingredient.name}
                  {ingredient.pantryStaple && (
                    <span className="rounded bg-muted px-1 py-px text-[10px]">常備</span>
                  )}
                </span>
                <span className="shrink-0 tabular-nums">
                  {ingredient.amount ?? ""}
                  {ingredient.unit ?? ""}
                </span>
              </li>
            ))}
          </ul>
        </section>

        <section>
          <h2 className="mb-2 text-sm font-medium">作り方</h2>
          <ol className="space-y-2 text-sm">
            {dish.steps.map((step, index) => (
              <li key={index} className="flex gap-2.5">
                <span className="mt-px flex size-5 shrink-0 items-center justify-center rounded-full bg-secondary text-xs font-medium text-secondary-foreground">
                  {index + 1}
                </span>
                <span className="leading-relaxed">{step}</span>
              </li>
            ))}
          </ol>
        </section>
      </CardContent>
    </Card>
  );
}
