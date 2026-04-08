"use client"

import { BatteryIcon } from "@/components/battery-icon"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { type Rank, formatSurvivalTime, getNextRank } from "@/lib/ranks"
import { Skull, Clock, Award, AlertOctagon, RotateCcw } from "lucide-react"

interface ExileScreenProps {
  finalRank: Rank
  survivalMinutes: number
  batteryLevel: number
  onRetry: () => void
}

export function ExileScreen({
  finalRank,
  survivalMinutes,
  batteryLevel,
  onRetry,
}: ExileScreenProps) {
  const isFullCharge = batteryLevel === 100
  const nextRank = getNextRank(finalRank)
  
  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Header with Warning */}
      <header className="p-4 border-b border-destructive/30 bg-destructive/5">
        <div className="flex items-center justify-center gap-2">
          <AlertOctagon className="w-5 h-5 text-destructive" />
          <span className="font-mono text-xs tracking-[0.3em] text-destructive">
            불명예 제대 | DISHONORABLE DISCHARGE
          </span>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 flex flex-col items-center justify-center p-6 gap-6">
        {/* Skull Icon */}
        <div className="relative">
          <div className="absolute -inset-8 rounded-full bg-destructive/20 blur-3xl" />
          <div className="relative w-28 h-28 rounded-full border-4 border-destructive bg-destructive/10 flex items-center justify-center">
            <Skull className="w-16 h-16 text-destructive" />
          </div>
        </div>

        {/* Title */}
        <div className="text-center space-y-2">
          <h1 className="text-3xl font-black text-destructive">
            불명예 퇴장
          </h1>
          <p className="font-mono text-sm text-destructive/70">
            EXILED FROM THE ELITE
          </p>
        </div>

        {/* Mission Report Card */}
        <div className="w-full max-w-sm bg-card border border-border/50 rounded-lg overflow-hidden">
          <div className="bg-muted/50 px-4 py-2 border-b border-border/30">
            <span className="font-mono text-xs text-muted-foreground tracking-wider">
              임무 보고서 | MISSION REPORT
            </span>
          </div>
          
          <div className="p-4 space-y-4">
            {/* Final Rank */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-muted-foreground">
                <Award className="w-4 h-4" />
                <span className="text-sm">최종 계급</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-lg">{finalRank.icon}</span>
                <span className="font-bold text-foreground">{finalRank.name}</span>
              </div>
            </div>

            {/* Survival Time */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-muted-foreground">
                <Clock className="w-4 h-4" />
                <span className="text-sm">생존 시간</span>
              </div>
              <span className="font-mono font-bold text-primary">
                {formatSurvivalTime(survivalMinutes)}
              </span>
            </div>

            {/* Divider */}
            <div className="border-t border-border/30" />

            {/* Next Rank Info */}
            {nextRank && (
              <div className="text-center text-sm text-muted-foreground">
                <span>다음 계급 </span>
                <span className="text-foreground font-bold">{nextRank.name}</span>
                <span>까지 </span>
                <span className="text-primary font-mono">
                  {formatSurvivalTime(nextRank.minSurvivalMinutes - survivalMinutes)}
                </span>
                <span> 남았습니다</span>
              </div>
            )}
          </div>
        </div>

        {/* Shame Message */}
        <div className="text-center max-w-sm px-4">
          <p className="text-muted-foreground leading-relaxed">
            귀하는 전우들과의 약속을 저버렸습니다.
          </p>
          <p className="text-muted-foreground leading-relaxed mt-2">
            다시 충전하고 <span className="text-primary font-bold">복귀</span>하십시오.
          </p>
        </div>

        {/* Current Battery Status */}
        <div className="flex flex-col items-center gap-2">
          <BatteryIcon level={batteryLevel} size="lg" />
          <span className={cn(
            "font-mono text-sm",
            isFullCharge ? "text-primary" : "text-destructive"
          )}>
            현재 배터리: {batteryLevel}%
          </span>
        </div>

        {/* Retry Button */}
        <Button
          size="lg"
          disabled={!isFullCharge}
          onClick={onRetry}
          className={cn(
            "w-full max-w-xs relative overflow-hidden",
            isFullCharge 
              ? "bg-primary hover:bg-primary/90 text-primary-foreground shadow-[0_0_20px_rgba(0,255,0,0.3)]"
              : "bg-muted text-muted-foreground"
          )}
        >
          <RotateCcw className="w-4 h-4 mr-2" />
          {isFullCharge ? "재입대 준비 완료" : "재입대 준비 (완충 시 활성화)"}
          {isFullCharge && (
            <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent -translate-x-full animate-[shimmer_2s_infinite]" />
          )}
        </Button>
      </main>

      {/* Footer */}
      <footer className="p-4 border-t border-border/30">
        <div className="text-center">
          <p className="text-xs text-muted-foreground/60 font-mono">
            "패배는 일시적이다. 포기만이 영원하다."
          </p>
        </div>
      </footer>
    </div>
  )
}
