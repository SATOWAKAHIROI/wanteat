import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // 本番イメージを小さくするため、必要な依存だけを含む server.js を出力する
  output: "standalone",
};

export default nextConfig;
