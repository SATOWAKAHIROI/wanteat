"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { CalendarDays, ChefHat, Heart, Settings, ShoppingCart } from "lucide-react";

import { cn } from "@/lib/utils";

const TABS = [
  { href: "/", label: "今日の献立", Icon: ChefHat },
  { href: "/shopping", label: "買い物", Icon: ShoppingCart },
  { href: "/requests", label: "リクエスト", Icon: Heart },
  { href: "/history", label: "履歴", Icon: CalendarDays },
  { href: "/settings", label: "設定", Icon: Settings },
] as const;

export function BottomNav() {
  const pathname = usePathname();

  return (
    <nav className="fixed inset-x-0 bottom-0 z-10 border-t border-border bg-background/95 backdrop-blur">
      <ul className="mx-auto flex max-w-md">
        {TABS.map(({ href, label, Icon }) => {
          const active = href === "/" ? pathname === "/" : pathname.startsWith(href);
          return (
            <li key={href} className="flex-1">
              <Link
                href={href}
                aria-current={active ? "page" : undefined}
                className={cn(
                  // スーパーで片手操作するため、タップ領域を広めに取る
                  "flex flex-col items-center gap-1 py-2.5 text-[11px] transition-colors",
                  active ? "text-primary" : "text-muted-foreground hover:text-foreground"
                )}
              >
                <Icon className="size-5" aria-hidden />
                {label}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
