"use client"

import { cn } from "@/lib/utils"
import { RANKS, type Rank } from "@/lib/ranks"
import { ScrollArea } from "@/components/ui/scroll-area"

interface RankDisplayProps {
  currentRank?: Rank
  className?: string
}

export function RankDisplay({ currentRank, className }: RankDisplayProps) {
  const groupedRanks = {
    enlisted: RANKS.filter(r => r.category === 'enlisted'),
    nco: RANKS.filter(r => r.category === 'nco'),
    officer: RANKS.filter(r => r.category === 'officer'),
    general: RANKS.filter(r => r.category === 'general'),
  }

  const categoryLabels = {
    enlisted: { ko: '병', en: 'ENLISTED' },
    nco: { ko: '부사관', en: 'NCO' },
    officer: { ko: '위관', en: 'OFFICER' },
    general: { ko: '영관/장성', en: 'GENERAL' },
  }

  return (
    <ScrollArea className={cn("h-full", className)}>
      <div className="p-4 space-y-6">
        {(Object.keys(groupedRanks) as Array<keyof typeof groupedRanks>).map((category) => (
          <div key={category}>
            {/* Category Header */}
            <div className="flex items-center gap-2 mb-3">
              <div className="h-px flex-1 bg-border/50" />
              <span className="font-mono text-xs text-muted-foreground tracking-wider">
                {categoryLabels[category].ko} | {categoryLabels[category].en}
              </span>
              <div className="h-px flex-1 bg-border/50" />
            </div>

            {/* Ranks in Category */}
            <div className="grid grid-cols-2 gap-2">
              {groupedRanks[category].map((rank) => {
                const isCurrentRank = currentRank?.id === rank.id
                const isAchieved = currentRank && RANKS.indexOf(currentRank) >= RANKS.indexOf(rank)

                return (
                  <div
                    key={rank.id}
                    className={cn(
                      "p-3 rounded-lg border transition-all",
                      isCurrentRank && "border-primary bg-primary/10 shadow-[0_0_10px_rgba(0,255,0,0.2)]",
                      !isCurrentRank && isAchieved && "border-border/50 bg-card/50",
                      !isAchieved && "border-border/30 bg-muted/30 opacity-50"
                    )}
                  >
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-lg">{rank.icon}</span>
                      <span className={cn(
                        "font-bold text-sm",
                        isCurrentRank ? "text-primary" : "text-foreground"
                      )}>
                        {rank.name}
                      </span>
                    </div>
                    <div className="font-mono text-xs text-muted-foreground">
                      {rank.description}
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        ))}
      </div>
    </ScrollArea>
  )
}
