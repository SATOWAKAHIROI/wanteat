"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Loader2, LogOut } from "lucide-react";

import { apiFetch } from "@/lib/api";
import type { Preference } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

export default function SettingsPage() {
  const router = useRouter();

  const [householdSize, setHouseholdSize] = useState("2");
  const [allergies, setAllergies] = useState("");
  const [dislikedFoods, setDislikedFoods] = useState("");
  const [note, setNote] = useState("");

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const preference = await apiFetch<Preference>("/api/preferences");
        setHouseholdSize(String(preference.householdSize));
        setAllergies(preference.allergies ?? "");
        setDislikedFoods(preference.dislikedFoods ?? "");
        setNote(preference.note ?? "");
      } catch {
        // 未登録なら 404。初期値のままフォームを表示する
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const save = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setSaved(false);
    setError(null);
    try {
      await apiFetch<Preference>("/api/preferences", {
        method: "PUT",
        body: JSON.stringify({
          householdSize: Number(householdSize),
          allergies: allergies.trim() || null,
          dislikedFoods: dislikedFoods.trim() || null,
          note: note.trim() || null,
        }),
      });
      setSaved(true);
    } catch (e) {
      setError(e instanceof Error ? e.message : "保存に失敗しました");
    } finally {
      setSaving(false);
    }
  };

  const logout = async () => {
    await apiFetch<void>("/api/auth/logout", { method: "POST" }).catch(() => {});
    router.push("/login");
  };

  if (loading) {
    return (
      <div className="flex min-h-[50dvh] items-center justify-center">
        <Loader2 className="size-6 animate-spin text-muted-foreground" aria-hidden />
      </div>
    );
  }

  return (
    <div className="space-y-5">
      <header>
        <h1 className="text-xl font-semibold tracking-tight">設定</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          提案のたびに AI へ渡されます
        </p>
      </header>

      {error && (
        <p className="rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {error}
        </p>
      )}

      <form onSubmit={save} className="space-y-4">
        <div className="space-y-1.5">
          <Label htmlFor="household">人数</Label>
          <Input
            id="household"
            inputMode="numeric"
            required
            value={householdSize}
            onChange={(e) => setHouseholdSize(e.target.value)}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="allergies">アレルギー</Label>
          <Input
            id="allergies"
            placeholder="そば, えび"
            value={allergies}
            onChange={(e) => setAllergies(e.target.value)}
          />
          <p className="text-xs text-muted-foreground">
            ここに書いたものは提案から必ず除外されます
          </p>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="disliked">苦手な食材</Label>
          <Input
            id="disliked"
            placeholder="パクチー"
            value={dislikedFoods}
            onChange={(e) => setDislikedFoods(e.target.value)}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="note">好みの傾向</Label>
          <Textarea
            id="note"
            placeholder="和食多め、揚げ物は控えめ"
            value={note}
            onChange={(e) => setNote(e.target.value)}
          />
        </div>

        <Button type="submit" size="lg" className="h-11 w-full" disabled={saving}>
          {saving ? "保存中..." : saved ? "保存しました" : "保存"}
        </Button>
      </form>

      <Button variant="outline" className="w-full" onClick={logout}>
        <LogOut aria-hidden />
        ログアウト
      </Button>
    </div>
  );
}
