"use client"

import { useState, useRef } from "react"
import { ScrollArea } from "@/components/ui/scroll-area"
import { cn } from "@/lib/utils"
import { formatSurvivalTime, type Rank } from "@/lib/ranks"
import { Trophy, Clock, ChevronLeft, ChevronRight } from "lucide-react"

interface RankingUser {
  id: string
  nickname: string
  rank: Rank
  survivalMinutes: number
  isMe?: boolean
}

interface RankingBoardProps {
  liveRanking: RankingUser[]
  allTimeRanking: RankingUser[]
  myNickname: string
}

export function RankingBoard({ liveRanking, allTimeRanking, myNickname }: RankingBoardProps) {
  const [activeTab, setActiveTab] = useState<'live' | 'alltime'>('live')
  const containerRef = useRef<HTMLDivElement>(null)
  const touchStartX = useRef<number | null>(null)

  const handleTouchStart = (e: React.TouchEvent) => {
    touchStartX.current = e.touches[0].clientX
  }

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (touchStartX.current === null) return
    
    const touchEndX = e.changedTouches[0].clientX
    const diff = touchStartX.current - touchEndX

    if (Math.abs(diff) > 50) {
      if (diff > 0 && activeTab === 'live') {
        setActiveTab('alltime')
      } else if (diff < 0 && activeTab === 'alltime') {
        setActiveTab('live')
      }
    }
    touchStartX.current = null
  }

  const currentRanking = activeTab === 'live' ? liveRanking : allTimeRanking

  const getMedalIcon = (position: number) => {
    switch (position) {
      case 0: return <span className="text-lg">🥇</span>
      case 1: return <span className="text-lg">🥈</span>
      case 2: return <span className="text-lg">🥉</span>
      default: return <span className="text-sm text-muted-foreground font-mono w-5 text-center">{position + 1}</span>
    }
  }

  return (
    <div 
      ref={containerRef}
      className="bg-card/80 border border-border/50 rounded-lg overflow-hidden"
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
    >
      {/* Tab Header */}
      <div className="flex border-b border-border/50">
        <button
          onClick={() => setActiveTab('live')}
          className={cn(
            "flex-1 py-2 px-3 flex items-center justify-center gap-2 text-xs font-bold transition-colors",
            activeTab === 'live' 
              ? "bg-primary/10 text-primary border-b-2 border-primary" 
              : "text-muted-foreground hover:text-foreground"
          )}
        >
          <Clock className="w-3 h-3" />
          실시간 랭킹
        </button>
        <button
          onClick={() => setActiveTab('alltime')}
          className={cn(
            "flex-1 py-2 px-3 flex items-center justify-center gap-2 text-xs font-bold transition-colors",
            activeTab === 'alltime' 
              ? "bg-primary/10 text-primary border-b-2 border-primary" 
              : "text-muted-foreground hover:text-foreground"
          )}
        >
          <Trophy className="w-3 h-3" />
          역대 랭킹
        </button>
      </div>

      {/* Swipe Indicator */}
      <div className="flex items-center justify-center gap-2 py-1.5 border-b border-border/30">
        <ChevronLeft className={cn(
          "w-3 h-3 transition-opacity",
          activeTab === 'live' ? "opacity-20" : "opacity-60 text-primary"
        )} />
        <div className="flex gap-1">
          <div className={cn(
            "w-1.5 h-1.5 rounded-full transition-colors",
            activeTab === 'live' ? "bg-primary" : "bg-muted"
          )} />
          <div className={cn(
            "w-1.5 h-1.5 rounded-full transition-colors",
            activeTab === 'alltime' ? "bg-primary" : "bg-muted"
          )} />
        </div>
        <ChevronRight className={cn(
          "w-3 h-3 transition-opacity",
          activeTab === 'alltime' ? "opacity-20" : "opacity-60 text-primary"
        )} />
      </div>

      {/* Ranking List */}
      <ScrollArea className="h-48">
        <div className="p-2 space-y-1">
          {currentRanking.length === 0 ? (
            <div className="flex items-center justify-center h-32 text-muted-foreground text-sm">
              랭킹 데이터가 없습니다
            </div>
          ) : (
            currentRanking.map((user, index) => {
              const isMe = user.nickname === myNickname || user.isMe
              return (
                <div 
                  key={user.id}
                  className={cn(
                    "flex items-center gap-3 px-2 py-2 rounded-md transition-colors",
                    isMe && "bg-primary/15 border border-primary/30",
                    index < 3 && !isMe && "bg-muted/30"
                  )}
                >
                  {/* Position */}
                  <div className="w-6 flex justify-center">
                    {getMedalIcon(index)}
                  </div>

                  {/* Rank Icon */}
                  <span className="text-base">{user.rank.icon}</span>

                  {/* User Info */}
                  <div className="flex-1 min-w-0">
                    <div className={cn(
                      "text-sm font-bold truncate",
                      isMe ? "text-primary" : "text-foreground"
                    )}>
                      {isMe ? "나" : user.nickname}
                    </div>
                    <div className="text-xs text-muted-foreground">
                      {user.rank.name}
                    </div>
                  </div>

                  {/* Survival Time */}
                  <div className="text-right">
                    <div className={cn(
                      "font-mono text-xs",
                      isMe ? "text-primary" : "text-foreground"
                    )}>
                      {formatSurvivalTime(user.survivalMinutes)}
                    </div>
                    {activeTab === 'alltime' && (
                      <div className="text-xs text-muted-foreground">
                        최고 기록
                      </div>
                    )}
                  </div>
                </div>
              )
            })
          )}
        </div>
      </ScrollArea>

      {/* Footer Hint */}
      <div className="px-3 py-1.5 border-t border-border/30 bg-muted/20">
        <p className="text-center text-xs text-muted-foreground">
          {activeTab === 'live' 
            ? "현재 접속 중인 전우들의 실시간 순위" 
            : "완충 전우회 역대 명예의 전당"}
        </p>
      </div>
    </div>
  )
}
