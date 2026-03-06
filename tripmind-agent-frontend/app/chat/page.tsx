"use client";

import { useState, useEffect, useRef } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { ThemeSwitcher } from "@/components/theme-switcher";
import { Send, ArrowLeft, Sparkles } from "lucide-react";
import Link from "next/link";

interface Message {
  role: "user" | "assistant";
  content: string;
}

export default function ChatPage() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const hasInitialized = useRef(false);

  useEffect(() => {
    const query = searchParams.get("q");
    if (query && !hasInitialized.current) {
      hasInitialized.current = true;
      // 添加用户的初始问题并获取回复
      const userMessage: Message = { role: "user", content: query };
      setMessages([userMessage]);
      fetchAIResponse(query);
    }
  }, [searchParams]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const fetchAIResponse = async (message: string) => {
    setIsLoading(true);
    
    // TODO: 这里调用后端API
    // 模拟API响应
    setTimeout(() => {
      const response: Message = {
        role: "assistant",
        content: `我已经收到您的问题："${message}"。\n\n让我为您规划一个详细的旅行方案...\n\n**行程概览**\n根据您的需求，我建议以下行程安排：\n\n1. 目的地分析\n2. 预算规划\n3. 行程安排\n4. 天气情况\n\n请稍等，我正在为您生成详细方案...`,
      };
      setMessages((prev) => [...prev, response]);
      setIsLoading(false);
    }, 1000);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isLoading) return;

    const userMessage: Message = { role: "user", content: input };
    setMessages((prev) => [...prev, userMessage]);
    fetchAIResponse(input);
    setInput("");
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  return (
    <div className="flex flex-col h-screen relative">
      {/* 装饰性渐变背景 */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none" aria-hidden="true">
        <div className="absolute -top-40 -left-40 w-[300px] h-[300px] bg-cyan-400/15 rounded-full blur-[128px]" />
        <div className="absolute -top-40 -right-40 w-[300px] h-[300px] bg-teal-400/15 rounded-full blur-[128px]" />
      </div>

      {/* 顶部导航 */}
      <nav className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="max-w-7xl mx-auto flex justify-between items-center px-6 h-16">
          <div className="flex items-center gap-6">
            <Link href="/" className="group">
              <Button 
                variant="ghost" 
                size="sm" 
                className="gap-2 text-muted-foreground hover:text-foreground transition-colors"
                aria-label="返回首页"
              >
                <ArrowLeft className="w-4 h-4 transition-transform group-hover:-translate-x-0.5" />
                <span className="hidden sm:inline">返回首页</span>
              </Button>
            </Link>
            <div className="flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-primary" aria-hidden="true" />
              <span className="text-lg font-semibold">TripMind</span>
            </div>
          </div>
          <ThemeSwitcher />
        </div>
      </nav>

      {/* 对话区域 */}
      <div className="flex-1 overflow-y-auto">
        <div className="max-w-3xl mx-auto px-4 py-8">
          {messages.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-center py-20">
              <Sparkles className="w-12 h-12 text-muted-foreground mb-4" />
              <p className="text-lg text-muted-foreground">开始您的旅行规划对话...</p>
            </div>
          ) : (
            <div className="space-y-6">
              {messages.map((message, index) => (
                <div
                  key={index}
                  className={`flex gap-3 ${
                    message.role === "user" ? "justify-end" : "justify-start"
                  }`}
                >
                  {message.role === "assistant" && (
                    <div className="flex-shrink-0 w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center">
                      <Sparkles className="w-4 h-4 text-primary" />
                    </div>
                  )}
                  <div
                    className={`max-w-[85%] rounded-2xl px-4 py-3 ${
                      message.role === "user"
                        ? "bg-primary text-primary-foreground"
                        : "bg-muted"
                    }`}
                  >
                    <div className="text-sm leading-relaxed whitespace-pre-wrap">
                      {message.content}
                    </div>
                  </div>
                  {message.role === "user" && (
                    <div className="flex-shrink-0 w-8 h-8 rounded-full bg-muted flex items-center justify-center">
                      <span className="text-sm">👤</span>
                    </div>
                  )}
                </div>
              ))}
              {isLoading && (
                <div className="flex gap-3 justify-start">
                  <div className="flex-shrink-0 w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center">
                    <Sparkles className="w-4 h-4 text-primary" />
                  </div>
                  <div className="max-w-[85%] rounded-2xl px-4 py-3 bg-muted">
                    <div className="flex items-center gap-1">
                      <div className="w-2 h-2 bg-foreground/40 rounded-full animate-bounce" />
                      <div className="w-2 h-2 bg-foreground/40 rounded-full animate-bounce [animation-delay:0.2s]" />
                      <div className="w-2 h-2 bg-foreground/40 rounded-full animate-bounce [animation-delay:0.4s]" />
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>
      </div>

      {/* 输入区域 */}
      <div className="border-t bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="max-w-3xl mx-auto px-4 py-4">
          <form onSubmit={handleSubmit}>
            <div className="relative flex items-end gap-2 rounded-2xl border bg-background shadow-sm focus-within:ring-2 focus-within:ring-ring p-2">
              <Textarea
                placeholder="描述您的旅行想法，例如：我想去成都玩5天，预算5000元..."
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                disabled={isLoading}
                className="flex-1 min-h-[60px] max-h-[200px] resize-none border-0 bg-transparent focus-visible:ring-0 focus-visible:ring-offset-0 text-sm"
                rows={1}
              />
              <Button
                type="submit"
                disabled={isLoading || !input.trim()}
                size="icon"
                className="flex-shrink-0 rounded-xl h-10 w-10"
              >
                <Send className="w-4 h-4" />
              </Button>
            </div>
          </form>
          <p className="text-xs text-muted-foreground text-center mt-3">
            按 Enter 发送，Shift + Enter 换行
          </p>
        </div>
      </div>
    </div>
  );
}
