import { ThemeSwitcher } from "@/components/theme-switcher";
import { Hero } from "@/components/hero";
import { EnvVarWarning } from "@/components/env-var-warning";
import { AuthButton } from "@/components/auth-button";
import { hasEnvVars } from "@/lib/utils";
import Link from "next/link";
import { Suspense } from "react";

export default function Home() {
  return (
    <main className="min-h-screen flex flex-col relative">
      {/* 装饰性渐变背景 */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none" aria-hidden="true">
        <div className="absolute -top-40 -left-40 w-[600px] h-[600px] bg-cyan-400/15 rounded-full blur-[128px]" />
        <div className="absolute -top-40 -right-40 w-[600px] h-[600px] bg-teal-400/15 rounded-full blur-[128px]" />
      </div>

      {/* 导航栏 */}
      <nav className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="max-w-7xl mx-auto flex justify-between items-center px-6 h-16">
          <div className="flex items-center gap-2">
            <Link href={"/"} className="flex items-center gap-2 hover:opacity-80 transition-opacity">
              <span className="text-lg font-semibold">TripMind</span>
            </Link>
          </div>
          <div className="flex items-center gap-3">
            <ThemeSwitcher />
            {!hasEnvVars ? (
              <EnvVarWarning />
            ) : (
              <Suspense>
                <AuthButton />
              </Suspense>
            )}
          </div>
        </div>
      </nav>

      {/* Hero 区域 */}
      <Hero />

      {/* 功能特性区域 */}
      <section id="features" className="w-full py-12 bg-muted/30">
        <div className="max-w-7xl mx-auto px-6">
          <div className="text-center mb-8">
            <h2 className="text-3xl font-bold mb-2">核心功能</h2>
            <p className="text-muted-foreground">基于多智能体协作的智能旅行规划系统</p>
          </div>
          <div className="grid md:grid-cols-3 gap-6">
            <div className="group relative p-6 rounded-xl bg-background border hover:border-foreground/20 transition-all overflow-hidden">
              <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-cyan-500 to-blue-500" aria-hidden="true" />
              <div className="flex items-center gap-3 mb-3">
                <div className="flex-shrink-0 w-10 h-10 rounded-lg bg-cyan-500/10 flex items-center justify-center">
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-cyan-600 dark:text-cyan-400" aria-hidden="true">
                    <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/>
                    <circle cx="12" cy="10" r="3"/>
                  </svg>
                </div>
                <h3 className="text-xl font-semibold">智能行程规划</h3>
              </div>
              <p className="text-sm text-muted-foreground leading-relaxed">
                根据您的偏好和时间，自动生成最优旅行路线和景点安排
              </p>
            </div>
            <div className="group relative p-6 rounded-xl bg-background border hover:border-foreground/20 transition-all overflow-hidden">
              <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-emerald-500 to-teal-500" aria-hidden="true" />
              <div className="flex items-center gap-3 mb-3">
                <div className="flex-shrink-0 w-10 h-10 rounded-lg bg-emerald-500/10 flex items-center justify-center">
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-emerald-600 dark:text-emerald-400" aria-hidden="true">
                    <line x1="12" x2="12" y1="2" y2="22"/>
                    <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
                  </svg>
                </div>
                <h3 className="text-xl font-semibold">预算管理</h3>
              </div>
              <p className="text-sm text-muted-foreground leading-relaxed">
                智能分析旅行成本，提供详细的预算建议和费用优化方案
              </p>
            </div>
            <div className="group relative p-6 rounded-xl bg-background border hover:border-foreground/20 transition-all overflow-hidden">
              <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-amber-500 to-orange-500" aria-hidden="true" />
              <div className="flex items-center gap-3 mb-3">
                <div className="flex-shrink-0 w-10 h-10 rounded-lg bg-amber-500/10 flex items-center justify-center">
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-amber-600 dark:text-amber-400" aria-hidden="true">
                    <path d="M12 2v10"/>
                    <path d="M18.4 6.6a9 9 0 1 1-12.77.04"/>
                  </svg>
                </div>
                <h3 className="text-xl font-semibold">天气预报</h3>
              </div>
              <p className="text-sm text-muted-foreground leading-relaxed">
                实时获取目的地天气信息，帮助您做出更好的出行决策
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 使用案例区域 */}
      <section id="examples" className="w-full py-20">
        <div className="max-w-7xl mx-auto px-6">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold mb-4">使用案例</h2>
            <p className="text-muted-foreground">看看 TripMind 如何帮助用户规划完美旅程</p>
          </div>
          <div className="grid md:grid-cols-3 gap-6">
            <Link href="#" className="group block p-6 rounded-lg border hover:border-foreground/20 transition-all">
              <div className="text-xs text-muted-foreground mb-2">周末游</div>
              <h3 className="text-lg font-semibold mb-2 group-hover:text-foreground/80">
                上海周边两日游规划
              </h3>
              <p className="text-sm text-muted-foreground mb-4">
                预算3000元，包含苏州园林和杭州西湖的完整行程安排
              </p>
              <div className="text-xs text-muted-foreground">2024-03-15</div>
            </Link>
            <Link href="#" className="group block p-6 rounded-lg border hover:border-foreground/20 transition-all">
              <div className="text-xs text-muted-foreground mb-2">国内游</div>
              <h3 className="text-lg font-semibold mb-2 group-hover:text-foreground/80">
                成都美食文化深度游
              </h3>
              <p className="text-sm text-muted-foreground mb-4">
                5天4夜，探索成都的美食、文化和自然风光
              </p>
              <div className="text-xs text-muted-foreground">2024-03-10</div>
            </Link>
            <Link href="#" className="group block p-6 rounded-lg border hover:border-foreground/20 transition-all">
              <div className="text-xs text-muted-foreground mb-2">海外游</div>
              <h3 className="text-lg font-semibold mb-2 group-hover:text-foreground/80">
                日本东京大阪7日游
              </h3>
              <p className="text-sm text-muted-foreground mb-4">
                包含交通、住宿、景点门票的详细预算和行程规划
              </p>
              <div className="text-xs text-muted-foreground">2024-03-05</div>
            </Link>
            <Link href="#" className="group block p-6 rounded-lg border hover:border-foreground/20 transition-all">
              <div className="text-xs text-muted-foreground mb-2">亲子游</div>
              <h3 className="text-lg font-semibold mb-2 group-hover:text-foreground/80">
                北京亲子文化之旅
              </h3>
              <p className="text-sm text-muted-foreground mb-4">
                适合带孩子的博物馆、公园和历史景点推荐
              </p>
              <div className="text-xs text-muted-foreground">2024-03-01</div>
            </Link>
            <Link href="#" className="group block p-6 rounded-lg border hover:border-foreground/20 transition-all">
              <div className="text-xs text-muted-foreground mb-2">自驾游</div>
              <h3 className="text-lg font-semibold mb-2 group-hover:text-foreground/80">
                川藏线自驾攻略
              </h3>
              <p className="text-sm text-muted-foreground mb-4">
                15天川藏线自驾路线规划，包含住宿和注意事项
              </p>
              <div className="text-xs text-muted-foreground">2024-02-28</div>
            </Link>
            <Link href="#" className="group block p-6 rounded-lg border hover:border-foreground/20 transition-all">
              <div className="text-xs text-muted-foreground mb-2">海岛游</div>
              <h3 className="text-lg font-semibold mb-2 group-hover:text-foreground/80">
                三亚海岛度假方案
              </h3>
              <p className="text-sm text-muted-foreground mb-4">
                5天4夜海岛度假，包含潜水、海鲜和沙滩活动
              </p>
              <div className="text-xs text-muted-foreground">2024-02-25</div>
            </Link>
          </div>
        </div>
      </section>

      {/* 页脚 */}
      <footer className="w-full border-t py-8 mt-auto">
        <div className="max-w-7xl mx-auto px-6 text-center text-sm text-muted-foreground">
          <p>Powered by TripMind AI Agent System</p>
        </div>
      </footer>
    </main>
  );
}
