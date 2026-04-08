"use client"

import { BatteryIcon } from "@/components/battery-icon"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { Shield, Lock, Unlock } from "lucide-react"

interface GatekeeperScreenProps {
  batteryLevel: number
  onEnter: () => void
  onShowOnboarding: () => void
}

export function GatekeeperScreen({ 
  batteryLevel, 
  onEnter, 
  onShowOnboarding 
}: GatekeeperScreenProps) {
  const isFullCharge = batteryLevel === 100

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Header */}
      <header className="p-4 border-b border-border/50">
        <div className="flex items-center justify-center gap-2">
          <Shield className="w-5 h-5 text-primary" />
          <span className="font-mono text-xs tracking-[0.3em] text-muted-foreground">
            검문소 | CHECKPOINT
          </span>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 flex flex-col items-center justify-center p-6 gap-8">
        {/* Security Clearance Badge */}
        <div className="relative">
          <div className={cn(
            "absolute -inset-8 rounded-full blur-3xl opacity-30",
            isFullCharge ? "bg-primary" : "bg-destructive"
          )} />
          
          <div className={cn(
            "relative w-32 h-32 rounded-full border-4 flex items-center justify-center",
            isFullCharge 
              ? "border-primary bg-primary/10" 
              : "border-destructive bg-destructive/10"
          )}>
            {isFullCharge ? (
              <Unlock className="w-16 h-16 text-primary animate-pulse" />
            ) : (
              <Lock className="w-16 h-16 text-destructive" />
            )}
          </div>
        </div>

        {/* Battery Status */}
        <BatteryIcon 
          level={batteryLevel} 
          size="xl" 
          animated 
        />

        {/* Status Text */}
        <div className="text-center space-y-4 max-w-sm">
          {isFullCharge ? (
            <>
              <h1 className="text-2xl font-bold text-primary">
                입장 허가
              </h1>
              <p className="text-muted-foreground leading-relaxed">
                귀하는 <span className="text-primary font-bold">완충 전우회</span>의 
                자격을 갖추었습니다.
              </p>
              <div className="font-mono text-xs text-primary/70 tracking-wider">
                SECURITY CLEARANCE: GRANTED
              </div>
            </>
          ) : (
            <>
              <h1 className="text-2xl font-bold text-destructive">
                입장 거부
              </h1>
              <p className="text-muted-foreground leading-relaxed">
                배터리 충전 상태가 <span className="text-destructive font-bold">불량</span>합니다.
              </p>
              <p className="text-sm text-muted-foreground">
                100% 완충 후 재투입하십시오.
              </p>
              <div className="font-mono text-xs text-destructive/70 tracking-wider">
                SECURITY CLEARANCE: DENIED
              </div>
            </>
          )}
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col gap-3 w-full max-w-xs">
          <Button
            size="lg"
            disabled={!isFullCharge}
            onClick={onEnter}
            className={cn(
              "relative overflow-hidden font-bold tracking-wider",
              isFullCharge && "bg-primary hover:bg-primary/90 text-primary-foreground shadow-[0_0_20px_rgba(0,255,0,0.3)]"
            )}
          >
            <span className="relative z-10">전선 투입</span>
            {isFullCharge && (
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent -translate-x-full animate-[shimmer_2s_infinite]" />
            )}
          </Button>

          <Button
            variant="outline"
            size="sm"
            onClick={onShowOnboarding}
            className="text-muted-foreground border-border/50"
          >
            신병 교육 안내
          </Button>
        </div>

        {/* Warning Notice */}
        <div className="mt-auto pt-8 text-center">
          <p className="text-xs text-muted-foreground/60 font-mono tracking-wide">
            경고: 99% 이하로 떨어지면 10초 내 추방됩니다
          </p>
        </div>
      </main>

      {/* Footer */}
      <footer className="p-4 border-t border-border/30">
        <div className="flex items-center justify-center gap-2">
          <div className={cn(
            "w-2 h-2 rounded-full",
            isFullCharge ? "bg-primary animate-pulse" : "bg-destructive"
          )} />
          <span className="font-mono text-xs text-muted-foreground">
            완충 전우회 | THE 100% ELITES
          </span>
        </div>
      </footer>
    </div>
  )
}
