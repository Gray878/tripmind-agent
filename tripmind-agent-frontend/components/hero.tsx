"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ArrowRight } from "lucide-react";

export function Hero() {
  const [query, setQuery] = useState("");
  const router = useRouter();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (query.trim()) {
      // 跳转到对话页面，并传递查询参数
      router.push(`/chat?q=${encodeURIComponent(query)}`);
    }
  };

  const handleQuickQuery = (text: string) => {
    router.push(`/chat?q=${encodeURIComponent(text)}`);
  };

  return (
    <section className="w-full py-16 md:py-24 bg-gradient-to-b from-background to-muted/30">
      <div className="max-w-5xl mx-auto px-6 text-center">
        <h1 className="text-4xl md:text-6xl font-bold mb-4 bg-gradient-to-r from-foreground to-foreground/70 bg-clip-text text-transparent">
          AI 驱动的智能旅行规划
        </h1>
        <p className="text-base md:text-lg text-muted-foreground mb-10 max-w-2xl mx-auto">
          不只是简单的行程推荐，而是基于深度理解的个性化旅行方案
        </p>
        
        {/* 搜索框 */}
        <form onSubmit={handleSubmit} className="max-w-3xl mx-auto mb-6">
          <div className="flex gap-3 p-3 rounded-xl border-2 bg-background shadow-xl hover:shadow-2xl transition-shadow">
            <Input
              type="text"
              placeholder="描述您的旅行想法，例如：我想去成都玩5天，预算5000元..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="flex-1 border-0 focus-visible:ring-0 text-base h-12"
            />
            <Button type="submit" size="lg" className="gap-2 px-6 h-12">
              开始规划
              <ArrowRight className="w-4 h-4" />
            </Button>
          </div>
        </form>

        {/* 快速标签 */}
        <div className="flex flex-wrap justify-center items-center gap-2 text-sm">
          <span className="text-muted-foreground">试试：</span>
          <button
            onClick={() => handleQuickQuery("上海周边两日游")}
            className="px-3 py-1 rounded-full bg-muted hover:bg-muted/80 transition-colors"
          >
            周末游
          </button>
          <button
            onClick={() => handleQuickQuery("成都美食文化深度游")}
            className="px-3 py-1 rounded-full bg-muted hover:bg-muted/80 transition-colors"
          >
            美食之旅
          </button>
          <button
            onClick={() => handleQuickQuery("三亚海岛度假")}
            className="px-3 py-1 rounded-full bg-muted hover:bg-muted/80 transition-colors"
          >
            海岛度假
          </button>
          <button
            onClick={() => handleQuickQuery("川藏线自驾游")}
            className="px-3 py-1 rounded-full bg-muted hover:bg-muted/80 transition-colors"
          >
            自驾游
          </button>
        </div>
      </div>
    </section>
  );
}
