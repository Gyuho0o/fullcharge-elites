"use client"

import { useState, useRef, useEffect } from "react"
import { BatteryIcon } from "@/components/battery-icon"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { ScrollArea } from "@/components/ui/scroll-area"
import { cn } from "@/lib/utils"
import { getRankByTime, formatSurvivalTime, getRankProgress, type Rank } from "@/lib/ranks"
import { Send, Users, Clock, Zap, AlertTriangle, Trophy } from "lucide-react"
import { RankingBoard } from "@/components/ranking-board"

interface Message {
  id: string
  type: 'user' | 'system' | 'announcement'
  author?: string
  rank?: Rank
  content: string
  timestamp: Date
  isMine?: boolean
}

interface RankingUser {
  id: string
  nickname: string
  rank: Rank
  survivalMinutes: number
  isMe?: boolean
}

interface ChatScreenProps {
  batteryLevel: number
  charging: boolean
  survivalMinutes: number
  onlineCount: number
  crisisCountdown: number | null
  onSendMessage: (message: string) => void
  messages: Message[]
  myNickname: string
  liveRanking: RankingUser[]
  allTimeRanking: RankingUser[]
}

export function ChatScreen({
  batteryLevel,
  charging,
  survivalMinutes,
  onlineCount,
  crisisCountdown,
  onSendMessage,
  messages,
  myNickname,
  liveRanking,
  allTimeRanking,
}: ChatScreenProps) {
  const [inputValue, setInputValue] = useState("")
  const [showRanking, setShowRanking] = useState(false)
  const scrollRef = useRef<HTMLDivElement>(null)
  const currentRank = getRankByTime(survivalMinutes)
  const rankProgress = getRankProgress(survivalMinutes, currentRank)
  const isCrisis = crisisCountdown !== null

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [messages])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (inputValue.trim()) {
      onSendMessage(inputValue.trim())
      setInputValue("")
    }
  }

  return (
    <div className="min-h-screen bg-background flex flex-col relative">
      {/* Crisis Overlay */}
      {isCrisis && (
        <div className="absolute inset-0 z-50 bg-destructive/90 flex flex-col items-center justify-center animate-pulse">
          <AlertTriangle className="w-24 h-24 text-destructive-foreground mb-4" />
          <h1 className="text-4xl font-black text-destructive-foreground mb-2">
            비상 상황!
          </h1>
          <p className="text-xl text-destructive-foreground/90 mb-8 text-center px-4">
            {crisisCountdown}초 내로 충전기를 연결하십시오!
          </p>
          <div className="text-8xl font-mono font-black text-destructive-foreground">
            {crisisCountdown}
          </div>
          <p className="mt-8 text-sm text-destructive-foreground/70 font-mono">
            BATTERY CRITICAL: {batteryLevel}%
          </p>
        </div>
      )}

      {/* HUD Header */}
      <header className="border-b border-border/50 bg-card/50 backdrop-blur-sm sticky top-0 z-40">
        {/* Top Status Bar */}
        <div className="flex items-center justify-between px-3 py-2 border-b border-border/30">
          <div className="flex items-center gap-2">
            <BatteryIcon level={batteryLevel} charging={charging} size="sm" />
            <span className={cn(
              "font-mono text-xs",
              batteryLevel === 100 ? "text-primary" : "text-destructive"
            )}>
              {batteryLevel}%
            </span>
          </div>
          
          <div className="flex items-center gap-3">
            <button 
              onClick={() => setShowRanking(!showRanking)}
              className={cn(
                "flex items-center gap-1 px-2 py-1 rounded-md transition-colors",
                showRanking 
                  ? "bg-primary/20 text-primary" 
                  : "text-muted-foreground hover:text-primary"
              )}
            >
              <Trophy className="w-3 h-3" />
              <span className="font-mono text-xs">랭킹</span>
            </button>
            <div className="flex items-center gap-1 text-muted-foreground">
              <Users className="w-3 h-3" />
              <span className="font-mono text-xs">{onlineCount}</span>
            </div>
          </div>
        </div>

        {/* Rank & Stats Bar */}
        <div className="px-3 py-2">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center gap-2">
              <span className="text-lg">{currentRank.icon}</span>
              <div>
                <div className="font-bold text-sm text-foreground">{currentRank.name}</div>
                <div className="font-mono text-xs text-muted-foreground">{currentRank.englishName}</div>
              </div>
            </div>
            
            <div className="flex items-center gap-1 text-primary">
              <Clock className="w-3 h-3" />
              <span className="font-mono text-xs">{formatSurvivalTime(survivalMinutes)}</span>
            </div>
          </div>

          {/* Rank Progress Bar */}
          <div className="h-1 bg-muted rounded-full overflow-hidden">
            <div 
              className="h-full bg-primary transition-all duration-500"
              style={{ width: `${rankProgress}%` }}
            />
          </div>
        </div>
      </header>

      {/* Ranking Board Panel */}
      {showRanking && (
        <div className="p-3 border-b border-border/30 bg-background/50">
          <RankingBoard 
            liveRanking={liveRanking}
            allTimeRanking={allTimeRanking}
            myNickname={myNickname}
          />
        </div>
      )}

      {/* Chat Messages */}
      <ScrollArea className="flex-1" ref={scrollRef}>
        <div className="p-3 space-y-3">
          {messages.map((msg) => (
            <div key={msg.id}>
              {/* System Message - Centered, minimal style */}
              {msg.type === 'system' && (
                <div className="flex justify-center py-2">
                  <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-muted/30 border border-border/20">
                    <Zap className="w-3 h-3 text-primary/70" />
                    <span className="text-xs text-muted-foreground">
                      {msg.content}
                    </span>
                  </div>
                </div>
              )}

              {/* Announcement - Full width, prominent red style */}
              {msg.type === 'announcement' && (
                <div className="mx-2 my-3 p-3 rounded-lg bg-destructive/15 border-l-4 border-destructive">
                  <div className="flex items-center gap-2 mb-1.5">
                    <AlertTriangle className="w-4 h-4 text-destructive" />
                    <span className="font-mono text-xs font-bold text-destructive uppercase tracking-wider">
                      긴급 알림
                    </span>
                    <span className="font-mono text-xs text-destructive/60">
                      {msg.timestamp.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                  <p className="text-sm text-destructive font-medium leading-relaxed pl-6">
                    {msg.content}
                  </p>
                </div>
              )}

              {/* User Message - Chat bubble style */}
              {msg.type === 'user' && (
                <div className={cn(
                  "flex flex-col gap-1",
                  msg.isMine && "items-end"
                )}>
                  {/* Author info */}
                  <div className={cn(
                    "flex items-center gap-2 px-1",
                    msg.isMine && "flex-row-reverse"
                  )}>
                    <span className="text-sm">{msg.rank?.icon}</span>
                    <span className={cn(
                      "font-bold text-sm",
                      msg.isMine ? "text-accent-foreground" : "text-primary"
                    )}>
                      {msg.isMine ? "나" : msg.author}
                    </span>
                    <span className="font-mono text-xs text-muted-foreground">
                      {msg.timestamp.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                  {/* Message bubble */}
                  <div className={cn(
                    "max-w-[85%] p-3 rounded-2xl shadow-sm",
                    msg.isMine 
                      ? "mr-1 ml-8 rounded-tr-sm bg-primary text-primary-foreground shadow-primary/20" 
                      : "ml-6 mr-8 rounded-tl-sm bg-card border border-primary/20 shadow-primary/5"
                  )}>
                    <p className={cn(
                      "text-sm leading-relaxed",
                      msg.isMine ? "text-primary-foreground" : "text-foreground"
                    )}>
                      {msg.content}
                    </p>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </ScrollArea>

      {/* Message Input */}
      <div className="border-t border-border/50 bg-card/50 backdrop-blur-sm p-3">
        <form onSubmit={handleSubmit} className="flex gap-2">
          <Input
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            placeholder="메시지를 입력하십시오..."
            className="flex-1 bg-input border-border/50 text-foreground placeholder:text-muted-foreground"
          />
          <Button 
            type="submit" 
            size="icon"
            className="bg-primary hover:bg-primary/90 text-primary-foreground"
          >
            <Send className="w-4 h-4" />
          </Button>
        </form>
        <div className="flex items-center justify-center gap-2 mt-2">
          <div className="w-2 h-2 rounded-full bg-primary animate-pulse" />
          <span className="font-mono text-xs text-muted-foreground">
            전우회 통신 활성화
          </span>
        </div>
      </div>
    </div>
  )
}
