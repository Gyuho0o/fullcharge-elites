"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { 
  Battery, 
  AlertTriangle, 
  Plug, 
  Star,
  ChevronRight,
  ChevronLeft,
  Shield
} from "lucide-react"

interface OnboardingScreenProps {
  onComplete: () => void
}

const STEPS = [
  {
    icon: Battery,
    title: "100% 완충만이 살 길이다",
    subtitle: "RULE #1: FULL CHARGE ONLY",
    description: "완충 전우회는 오직 배터리 100%인 전우만 입장할 수 있습니다. 이것이 진정한 전우의 자격입니다.",
    highlight: "100%",
    color: "text-primary",
    bgColor: "bg-primary/10",
    borderColor: "border-primary/30",
  },
  {
    icon: AlertTriangle,
    title: "99%가 되는 순간, 배신자로 간주된다",
    subtitle: "RULE #2: NO TOLERANCE",
    description: "배터리가 99%로 떨어지는 순간, 귀하는 전우회를 배신한 것으로 간주됩니다. 용서는 없습니다.",
    highlight: "99%",
    color: "text-destructive",
    bgColor: "bg-destructive/10",
    borderColor: "border-destructive/30",
  },
  {
    icon: Plug,
    title: "10초의 기회 안에 충전기를 연결하라",
    subtitle: "RULE #3: LAST CHANCE",
    description: "99%가 되면 10초의 유예시간이 주어집니다. 이 안에 충전기를 연결하면 복귀할 수 있습니다.",
    highlight: "10초",
    color: "text-amber-500",
    bgColor: "bg-amber-500/10",
    borderColor: "border-amber-500/30",
  },
  {
    icon: Star,
    title: "오래 생존하여 별(장군)을 달아라",
    subtitle: "RULE #4: RISE IN RANKS",
    description: "생존 시간에 따라 계급이 올라갑니다. 훈련병부터 시작하여 대장까지. 당신의 명예를 증명하십시오.",
    highlight: "대장",
    color: "text-yellow-500",
    bgColor: "bg-yellow-500/10",
    borderColor: "border-yellow-500/30",
  },
]

export function OnboardingScreen({ onComplete }: OnboardingScreenProps) {
  const [currentStep, setCurrentStep] = useState(0)
  const step = STEPS[currentStep]
  const Icon = step.icon
  const isLastStep = currentStep === STEPS.length - 1

  const handleNext = () => {
    if (isLastStep) {
      onComplete()
    } else {
      setCurrentStep(prev => prev + 1)
    }
  }

  const handlePrev = () => {
    if (currentStep > 0) {
      setCurrentStep(prev => prev - 1)
    }
  }

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Header */}
      <header className="p-4 border-b border-border/50">
        <div className="flex items-center justify-center gap-2">
          <Shield className="w-5 h-5 text-primary" />
          <span className="font-mono text-xs tracking-[0.3em] text-muted-foreground">
            신병 교육대 | BOOT CAMP
          </span>
        </div>
      </header>

      {/* Progress Indicator */}
      <div className="px-6 py-4">
        <div className="flex gap-2">
          {STEPS.map((_, index) => (
            <div
              key={index}
              className={cn(
                "h-1 flex-1 rounded-full transition-all duration-300",
                index <= currentStep ? "bg-primary" : "bg-muted"
              )}
            />
          ))}
        </div>
        <div className="flex justify-between mt-2">
          <span className="font-mono text-xs text-muted-foreground">
            STEP {currentStep + 1}/{STEPS.length}
          </span>
          <span className="font-mono text-xs text-primary">
            {Math.round(((currentStep + 1) / STEPS.length) * 100)}% 완료
          </span>
        </div>
      </div>

      {/* Content */}
      <main className="flex-1 flex flex-col items-center justify-center p-6">
        {/* Icon */}
        <div className={cn(
          "w-24 h-24 rounded-2xl flex items-center justify-center mb-8 border-2",
          step.bgColor,
          step.borderColor
        )}>
          <Icon className={cn("w-12 h-12", step.color)} />
        </div>

        {/* Text Content */}
        <div className="text-center space-y-4 max-w-sm">
          <div className="font-mono text-xs tracking-wider text-muted-foreground">
            {step.subtitle}
          </div>
          
          <h1 className="text-2xl font-bold text-foreground leading-tight text-balance">
            {step.title}
          </h1>
          
          <p className="text-muted-foreground leading-relaxed">
            {step.description}
          </p>

          {/* Highlight Badge */}
          <div className={cn(
            "inline-flex items-center gap-2 px-4 py-2 rounded-lg border",
            step.bgColor,
            step.borderColor
          )}>
            <span className={cn("font-mono font-bold text-lg", step.color)}>
              {step.highlight}
            </span>
          </div>
        </div>
      </main>

      {/* Navigation */}
      <div className="p-6 border-t border-border/30">
        <div className="flex gap-3">
          <Button
            variant="outline"
            onClick={handlePrev}
            disabled={currentStep === 0}
            className="flex-1"
          >
            <ChevronLeft className="w-4 h-4 mr-1" />
            이전
          </Button>
          
          <Button
            onClick={handleNext}
            className={cn(
              "flex-1",
              isLastStep && "bg-primary hover:bg-primary/90 text-primary-foreground"
            )}
          >
            {isLastStep ? "교육 완료" : "다음"}
            <ChevronRight className="w-4 h-4 ml-1" />
          </Button>
        </div>

        {/* Skip Button */}
        <Button
          variant="ghost"
          onClick={onComplete}
          className="w-full mt-3 text-muted-foreground text-sm"
        >
          교육 건너뛰기
        </Button>
      </div>
    </div>
  )
}
