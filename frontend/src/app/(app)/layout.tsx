import type { ReactNode } from "react";

import { BottomNav } from "@/components/BottomNav";

/** ログイン以外の画面に共通のレイアウト。下部にタブバーを置く。 */
export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-md flex-col">
      {/* タブバーに隠れないよう下側に余白を確保する */}
      <main className="flex-1 px-4 pt-5 pb-24">{children}</main>
      <BottomNav />
    </div>
  );
}
