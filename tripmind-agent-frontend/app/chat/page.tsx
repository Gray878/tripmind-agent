"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import {
  Send,
  ArrowLeft,
  Sparkles,
  Brain,
  Search,
  FileText,
  ChevronDown,
  Eye,
  EyeOff,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { ThemeSwitcher } from "@/components/theme-switcher";
import { apiClient, SSEMessage } from "@/lib/api";

interface Message {
  id: string;
  role: "user" | "assistant";
  content: string;
}

interface ParsedAssistantSections {
  thinking: string;
  searchLogs: string[];
  finalAnswer: string;
}

const THINKING_RE = /^(?:[^A-Za-z0-9\u4e00-\u9fff]*)?(?:\u601d\u8003|think)\s*[:\uFF1A]\s*(.*)$/i;
const ACTION_RE = /^(?:[^A-Za-z0-9\u4e00-\u9fff]*)?(?:\u884c\u52a8|action)\s*[:\uFF1A]\s*(.*)$/i;
const OBSERVATION_RE = /^(?:[^A-Za-z0-9\u4e00-\u9fff]*)?(?:\u89c2\u5bdf|observation|observe)\s*[:\uFF1A]\s*(.*)$/i;
const STEP_RE = /^(?:[^A-Za-z0-9\u4e00-\u9fff]*)?step\s*\d+/i;
const EVENT_DATA_RE = /^(?:event|data)\s*:/i;
const TERMINAL_RE = /^(?:\u4efb\u52a1\u7ed3\u675f|\u4efb\u52a1\u5b8c\u6210|task\s*finished|finished|done)$/i;

const createMessageId = (role: Message["role"]): string => {
  return `${role}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
};

const normalizeBlock = (text: string): string => {
  return text.replace(/\r/g, "").replace(/\n{3,}/g, "\n\n").trim();
};

const cleanQuotedText = (text: string): string => {
  return text.replace(/^["'\u201C\u201D]+|["'\u201C\u201D]+$/g, "").trim();
};

const looksLikeAnswerStart = (line: string): boolean => {
  const trimmed = line.trim();
  if (!trimmed) return false;

  return (
    /^#{1,6}\s/.test(trimmed) ||
    /^[-*+]\s/.test(trimmed) ||
    /^\d+\.\s/.test(trimmed) ||
    /^(\u65b9\u6848|\u603b\u7ed3|\u5efa\u8bae|\u63a8\u8350|\u884c\u7a0b)/.test(trimmed)
  );
};

const isTraceLine = (line: string): boolean => {
  const trimmed = line.trim();
  if (!trimmed) return false;

  return (
    THINKING_RE.test(trimmed) ||
    ACTION_RE.test(trimmed) ||
    OBSERVATION_RE.test(trimmed) ||
    STEP_RE.test(trimmed) ||
    EVENT_DATA_RE.test(trimmed) ||
    TERMINAL_RE.test(trimmed) ||
    /^tool_result\[[^\]]+\]\s*=\s*/i.test(trimmed) ||
    /\btool\s*=\s*[a-z0-9_]+/i.test(trimmed)
  );
};

const parseSearchLogFromAction = (actionText: string): string | null => {
  const toolMatch = actionText.match(/tool\s*=\s*([a-z0-9_]+)/i);
  if (!toolMatch) return null;

  const toolName = toolMatch[1];
  if (/doterminate/i.test(toolName)) return null;
  if (/search/i.test(toolName)) return `\u8c03\u7528\u641c\u7d22\u5de5\u5177\uff1a${toolName}`;
  return `\u8c03\u7528\u5de5\u5177\uff1a${toolName}`;
};

const parseSearchLogFromObservation = (observationText: string): string | null => {
  const resultMatch = observationText.match(/tool_result\[[^\]]+\]\s*=\s*(.+)$/i);
  const raw = resultMatch ? resultMatch[1] : observationText;
  const cleaned = cleanQuotedText(raw);

  if (!cleaned || TERMINAL_RE.test(cleaned)) {
    return null;
  }

  return cleaned;
};

const parseAssistantSections = (rawContent: string): ParsedAssistantSections => {
  const lines = rawContent.replace(/\r/g, "").split("\n");
  const thinkingLines: string[] = [];
  const answerLines: string[] = [];
  const searchLogs: string[] = [];

  let mode: "none" | "thinking" | "answer" = "none";

  for (const originalLine of lines) {
    const line = originalLine.trimEnd();
    const trimmed = line.trim();

    if (!trimmed) {
      if (mode === "thinking" && thinkingLines[thinkingLines.length - 1] !== "") {
        thinkingLines.push("");
      }
      if (mode === "answer" && answerLines[answerLines.length - 1] !== "") {
        answerLines.push("");
      }
      continue;
    }

    const thinkingMatch = trimmed.match(THINKING_RE);
    if (thinkingMatch) {
      mode = "thinking";
      const firstLine = thinkingMatch[1]?.trim();
      if (firstLine) thinkingLines.push(firstLine);
      continue;
    }

    const actionMatch = trimmed.match(ACTION_RE);
    if (actionMatch) {
      mode = "none";
      const actionLog = parseSearchLogFromAction(actionMatch[1] || "");
      if (actionLog) searchLogs.push(actionLog);
      continue;
    }

    const observationMatch = trimmed.match(OBSERVATION_RE);
    if (observationMatch) {
      mode = "none";
      const observationLog = parseSearchLogFromObservation(observationMatch[1] || "");
      if (observationLog) searchLogs.push(observationLog);
      continue;
    }

    if (STEP_RE.test(trimmed)) {
      searchLogs.push(trimmed);
      mode = "none";
      continue;
    }

    if (EVENT_DATA_RE.test(trimmed) || TERMINAL_RE.test(trimmed)) {
      mode = "none";
      continue;
    }

    if (mode === "thinking") {
      if (looksLikeAnswerStart(trimmed)) {
        mode = "answer";
        answerLines.push(line);
      } else {
        thinkingLines.push(line);
      }
      continue;
    }

    mode = "answer";
    answerLines.push(line);
  }

  const finalAnswer = normalizeBlock(answerLines.join("\n"));
  const fallbackAnswer = normalizeBlock(lines.filter((line) => !isTraceLine(line)).join("\n"));

  const dedupSearchLogs: string[] = [];
  for (const log of searchLogs) {
    if (!log) continue;
    if (dedupSearchLogs[dedupSearchLogs.length - 1] === log) continue;
    dedupSearchLogs.push(log);
  }

  return {
    thinking: normalizeBlock(thinkingLines.join("\n")),
    searchLogs: dedupSearchLogs,
    finalAnswer: finalAnswer || fallbackAnswer,
  };
};

const MarkdownContent = ({ content }: { content: string }) => {
  return (
    <ReactMarkdown
      remarkPlugins={[remarkGfm]}
      components={{
        h1: ({ children }) => <h1 className="mb-3 mt-2 text-xl font-bold">{children}</h1>,
        h2: ({ children }) => <h2 className="mb-2 mt-4 text-lg font-semibold">{children}</h2>,
        h3: ({ children }) => <h3 className="mb-2 mt-3 text-base font-semibold">{children}</h3>,
        p: ({ children }) => <p className="mb-2 last:mb-0">{children}</p>,
        ul: ({ children }) => <ul className="mb-2 list-disc space-y-1 pl-5">{children}</ul>,
        ol: ({ children }) => <ol className="mb-2 list-decimal space-y-1 pl-5">{children}</ol>,
        li: ({ children }) => <li>{children}</li>,
        strong: ({ children }) => <strong className="font-semibold">{children}</strong>,
        code: ({ children, className }) => {
          const text = String(children ?? "");
          const isInline = !className && !text.includes("\n");

          if (isInline) {
            return <code className="rounded bg-muted px-1 py-0.5 text-[13px]">{children}</code>;
          }

          return (
            <code className="block overflow-x-auto rounded-lg bg-muted p-3 text-[13px]">{children}</code>
          );
        },
        blockquote: ({ children }) => (
          <blockquote className="my-3 border-l-2 border-border pl-3 text-muted-foreground">{children}</blockquote>
        ),
        a: ({ href, children }) => (
          <a href={href} target="_blank" rel="noreferrer" className="text-primary underline underline-offset-2">
            {children}
          </a>
        ),
      }}
    >
      {content}
    </ReactMarkdown>
  );
};

const AssistantDocument = ({
  content,
  isStreaming,
  showThinking,
  showSearch,
  finalOnly,
}: {
  content: string;
  isStreaming: boolean;
  showThinking: boolean;
  showSearch: boolean;
  finalOnly: boolean;
}) => {
  const sections = parseAssistantSections(content);
  const hasThinking = Boolean(sections.thinking);
  const hasSearch = sections.searchLogs.length > 0;
  const hasFinal = Boolean(sections.finalAnswer);
  const hasAnySection = hasThinking || hasSearch || hasFinal;

  if (!hasAnySection && isStreaming) {
    return (
      <section className="mx-auto w-full max-w-4xl rounded-2xl border bg-card/80 p-4 shadow-sm sm:p-6">
        <div className="flex items-center justify-center gap-3 py-10 text-muted-foreground">
          <div className="h-2.5 w-2.5 animate-bounce rounded-full bg-foreground/50" />
          <div className="h-2.5 w-2.5 animate-bounce rounded-full bg-foreground/50 [animation-delay:0.2s]" />
          <div className="h-2.5 w-2.5 animate-bounce rounded-full bg-foreground/50 [animation-delay:0.4s]" />
          <span className="text-sm">{"\u6b63\u5728\u751f\u6210\u5185\u5bb9..."}</span>
        </div>
      </section>
    );
  }

  if (!hasAnySection) {
    return null;
  }

  return (
    <section className="mx-auto w-full max-w-4xl rounded-2xl border bg-card/80 p-4 shadow-sm sm:p-6">
      {!finalOnly && hasThinking && (
        <>
          <div className="rounded-xl border bg-muted/30">
            <div className="flex items-center justify-between px-4 py-3 text-sm font-medium text-muted-foreground">
              <span className="inline-flex items-center gap-2">
                <Brain className="h-4 w-4" />
                {"\u601d\u8003\u8fc7\u7a0b"}
              </span>
              <ChevronDown className={`h-4 w-4 transition-transform ${showThinking ? "rotate-180" : ""}`} />
            </div>

            {showThinking && (
              <div className="border-t px-4 py-3 text-[15px] leading-relaxed text-foreground">
                <MarkdownContent content={sections.thinking} />
              </div>
            )}
          </div>
        </>
      )}

      {!finalOnly && hasSearch && (
        <>
          <div className="mt-4 rounded-xl border bg-muted/20 p-4">
            <div className="flex items-center justify-between">
              <h3 className="inline-flex items-center gap-2 text-sm font-medium text-muted-foreground">
                <Search className="h-4 w-4" />
                {"\u641c\u7d22\u8fc7\u7a0b"}
              </h3>
              <ChevronDown className={`h-4 w-4 transition-transform ${showSearch ? "rotate-180" : ""}`} />
            </div>

            {showSearch && (
              <div className="mt-2">
                <ul className="space-y-2 text-sm text-foreground">
                  {sections.searchLogs.map((log, index) => (
                    <li key={`${index}-${log}`} className="rounded-lg bg-background/70 px-3 py-2">
                      {log}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </>
      )}

      {hasFinal && (
        <div className={`${finalOnly ? "" : "mt-4"} rounded-xl border bg-background p-4 sm:p-5`}>
          <h3 className="mb-3 inline-flex items-center gap-2 text-sm font-medium text-muted-foreground">
            <FileText className="h-4 w-4" />
            {"\u6700\u7ec8\u56de\u7b54"}
          </h3>

          <div className="text-[15px] leading-relaxed text-foreground">
            <MarkdownContent content={sections.finalAnswer} />
          </div>
        </div>
      )}

      {!hasFinal && isStreaming && (
        <div className={`${finalOnly || (!hasThinking && !hasSearch) ? "" : "mt-4"} rounded-xl border bg-background p-4 sm:p-5`}>
          <div className="flex items-center gap-3 py-2 text-sm text-muted-foreground">
            <div className="h-2 w-2 animate-bounce rounded-full bg-foreground/50" />
            <div className="h-2 w-2 animate-bounce rounded-full bg-foreground/50 [animation-delay:0.2s]" />
            <div className="h-2 w-2 animate-bounce rounded-full bg-foreground/50 [animation-delay:0.4s]" />
            <span>{"\u6b63\u5728\u751f\u6210\u6700\u7ec8\u56de\u7b54..."}</span>
          </div>
        </div>
      )}
    </section>
  );
};

export default function ChatPage() {
  const searchParams = useSearchParams();
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [showThinking, setShowThinking] = useState(false);
  const [showSearch, setShowSearch] = useState(true);
  const [finalOnly, setFinalOnly] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const hasInitialized = useRef(false);

  const fetchAIResponse = useCallback(
    async (message: string) => {
      if (isLoading) return;

      const assistantMessageId = createMessageId("assistant");
      setIsLoading(true);

      setMessages((prev) => [...prev, { id: assistantMessageId, role: "assistant", content: "" }]);

      const updateAssistantMessage = (updater: (current: string) => string) => {
        setMessages((prev) =>
          prev.map((msg) => {
            if (msg.id !== assistantMessageId || msg.role !== "assistant") return msg;
            return { ...msg, content: updater(msg.content) };
          }),
        );
      };

      try {
        await apiClient.createPlanStream(message, "guest_user", (data: SSEMessage) => {
          switch (data.type) {
            case "step":
            case "progress": {
              // Keep loading state only; avoid polluting final answer with internal step text.
              break;
            }

            case "result": {
              const nextChunk = data.content;
              if (!nextChunk) return;
              updateAssistantMessage((current) => `${current}${current ? "\n\n" : ""}${nextChunk}`);
              break;
            }

            case "complete": {
              const finalContent = data.result || data.content;
              if (finalContent && !TERMINAL_RE.test(finalContent.trim())) {
                updateAssistantMessage((current) => {
                  const trimmedCurrent = current.trim();
                  const trimmedFinal = finalContent.trim();
                  if (trimmedCurrent.includes(trimmedFinal)) return current;
                  return `${current}${current ? "\n\n" : ""}${finalContent}`;
                });
              }
              setIsLoading(false);
              break;
            }

            case "error": {
              const errorText = data.content || data.errorMessage || "Unknown error";
              updateAssistantMessage(
                () => `\u62b1\u6b49\uff0c\u5904\u7406\u60a8\u7684\u8bf7\u6c42\u65f6\u51fa\u73b0\u9519\u8bef\uff1a\n\n${errorText}`,
              );
              setIsLoading(false);
              break;
            }
          }
        });

        setIsLoading(false);
      } catch (error) {
        console.error("API call failed:", error);
        const errorText = error instanceof Error ? error.message : "Unknown error";
        updateAssistantMessage(
          () =>
            `\u62b1\u6b49\uff0c\u670d\u52a1\u6682\u65f6\u4e0d\u53ef\u7528\u3002\u8bf7\u68c0\u67e5\u540e\u7aef\u670d\u52a1\u662f\u5426\u542f\u52a8\u3002\n\n\u9519\u8bef\u4fe1\u606f\uff1a${errorText}\n\n\u8bf7\u786e\u8ba4\u540e\u7aef\u670d\u52a1\u8fd0\u884c\u5728 http://localhost:8123`,
        );
        setIsLoading(false);
      }
    },
    [isLoading],
  );

  useEffect(() => {
    const query = searchParams.get("q");
    if (query && !hasInitialized.current) {
      hasInitialized.current = true;
      const userMessage: Message = { id: createMessageId("user"), role: "user", content: query };
      setMessages([userMessage]);
      fetchAIResponse(query);
    }
  }, [searchParams, fetchAIResponse]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isLoading) return;

    const userMessage: Message = { id: createMessageId("user"), role: "user", content: input };
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

  const lastMessageId = messages[messages.length - 1]?.id;
  const hasAssistantMessage = messages.some((msg) => msg.role === "assistant");
  const hasStructuredAssistantContent = messages.some((msg) => {
    if (msg.role !== "assistant") return false;
    const sections = parseAssistantSections(msg.content);
    return Boolean(sections.thinking || sections.searchLogs.length || sections.finalAnswer);
  });

  return (
    <div className="relative flex h-screen flex-col">
      <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
        <div className="absolute -left-40 -top-40 h-[300px] w-[300px] rounded-full bg-cyan-400/15 blur-[128px]" />
        <div className="absolute -right-40 -top-40 h-[300px] w-[300px] rounded-full bg-teal-400/15 blur-[128px]" />
      </div>

      <nav className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-6">
          <div className="flex items-center gap-6">
            <Link href="/" className="group">
              <Button
                variant="ghost"
                size="sm"
                className="gap-2 text-muted-foreground transition-colors hover:text-foreground"
                aria-label={"\u8fd4\u56de\u9996\u9875"}
              >
                <ArrowLeft className="h-4 w-4 transition-transform group-hover:-translate-x-0.5" />
                <span className="hidden sm:inline">{"\u8fd4\u56de\u9996\u9875"}</span>
              </Button>
            </Link>
            <div className="flex items-center gap-2">
              <Sparkles className="h-5 w-5 text-primary" aria-hidden="true" />
              <span className="text-lg font-semibold">TripMind</span>
            </div>
          </div>
          <ThemeSwitcher />
        </div>
      </nav>

      <div className="flex-1 overflow-y-auto">
        <div className="mx-auto w-full max-w-5xl px-4 py-8">
          {messages.length === 0 ? (
            <div className="flex h-full flex-col items-center justify-center py-20 text-center">
              <Sparkles className="mb-4 h-12 w-12 text-muted-foreground" />
              <p className="text-lg text-muted-foreground">{"\u5f00\u59cb\u60a8\u7684\u65c5\u884c\u89c4\u5212\u5bf9\u8bdd..."}</p>
            </div>
          ) : (
            <div className="space-y-8">
              {hasAssistantMessage && hasStructuredAssistantContent && (
                <div className="flex flex-wrap justify-center gap-2">
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    className="gap-2 rounded-full px-4"
                    onClick={() => setShowThinking((prev) => !prev)}
                    disabled={finalOnly}
                  >
                    <Brain className="h-4 w-4" />
                    {showThinking
                      ? "\u9690\u85cf\u601d\u8003\u8fc7\u7a0b"
                      : "\u663e\u793a\u601d\u8003\u8fc7\u7a0b"}
                    <ChevronDown className={`h-4 w-4 transition-transform ${showThinking ? "rotate-180" : ""}`} />
                  </Button>

                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    className="gap-2 rounded-full px-4"
                    onClick={() => setShowSearch((prev) => !prev)}
                    disabled={finalOnly}
                  >
                    <Search className="h-4 w-4" />
                    {showSearch
                      ? "\u9690\u85cf\u641c\u7d22\u8fc7\u7a0b"
                      : "\u663e\u793a\u641c\u7d22\u8fc7\u7a0b"}
                    <ChevronDown className={`h-4 w-4 transition-transform ${showSearch ? "rotate-180" : ""}`} />
                  </Button>

                  <Button
                    type="button"
                    variant={finalOnly ? "default" : "outline"}
                    size="sm"
                    className="gap-2 rounded-full px-4"
                    onClick={() => setFinalOnly((prev) => !prev)}
                  >
                    {finalOnly ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    {finalOnly
                      ? "\u663e\u793a\u5168\u90e8\u5206\u533a"
                      : "\u4ec5\u663e\u793a\u6700\u7ec8\u56de\u7b54"}
                  </Button>
                </div>
              )}

              {messages.map((message) => {
                if (message.role === "user") {
                  return (
                    <div key={message.id} className="flex justify-end">
                      <div className="max-w-[75%] rounded-xl border border-cyan-200 bg-cyan-50/80 px-4 py-2 text-[15px] text-slate-800 shadow-sm">
                        {message.content}
                      </div>
                    </div>
                  );
                }

                return (
                  <AssistantDocument
                    key={message.id}
                    content={message.content}
                    isStreaming={isLoading && message.id === lastMessageId}
                    showThinking={showThinking}
                    showSearch={showSearch}
                    finalOnly={finalOnly}
                  />
                );
              })}
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>
      </div>

      <div className="border-t bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="mx-auto max-w-4xl px-4 py-4">
          <form onSubmit={handleSubmit}>
            <div className="relative flex items-end gap-2 rounded-2xl border bg-background p-2 shadow-sm focus-within:ring-2 focus-within:ring-ring">
              <Textarea
                placeholder={"\u63cf\u8ff0\u60a8\u7684\u65c5\u884c\u60f3\u6cd5\uff0c\u4f8b\u5982\uff1a\u6211\u60f3\u8981\u4e0a\u6d77\u5468\u8fb9\u4e24\u65e5\u6e38\uff0c\u9884\u7b975000\u5143"}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                disabled={isLoading}
                className="min-h-[60px] max-h-[200px] flex-1 resize-none border-0 bg-transparent text-sm focus-visible:ring-0 focus-visible:ring-offset-0"
                rows={1}
              />
              <Button
                type="submit"
                disabled={isLoading || !input.trim()}
                size="icon"
                className="h-10 w-10 flex-shrink-0 rounded-xl"
              >
                <Send className="h-4 w-4" />
              </Button>
            </div>
          </form>
          <p className="mt-3 text-center text-xs text-muted-foreground">
            {"\u6309 Enter \u53d1\u9001\uff0cShift + Enter \u6362\u884c"}
          </p>
        </div>
      </div>
    </div>
  );
}
