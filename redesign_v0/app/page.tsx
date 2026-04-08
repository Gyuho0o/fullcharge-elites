"use client"

import { useState, useEffect, useCallback } from "react"
import { GatekeeperScreen } from "@/components/screens/gatekeeper-screen"
import { ChatScreen } from "@/components/screens/chat-screen"
import { OnboardingScreen } from "@/components/screens/onboarding-screen"
import { ExileScreen } from "@/components/screens/exile-screen"
import { useDemoBattery } from "@/hooks/use-battery"
import { getRankByTime, type Rank } from "@/lib/ranks"
import { Button } from "@/components/ui/button"
import { Slider } from "@/components/ui/slider"
import { Switch } from "@/components/ui/switch"
import { Label } from "@/components/ui/label"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet"
import { Settings, Battery, Zap } from "lucide-react"

type Screen = 'gatekeeper' | 'onboarding' | 'chat' | 'exile'

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

// Anonymous nickname generator
const NICKNAMES = [
  "익명의 전우", "무적의 소위", "철벽의 하사", "불굴의 병장",
  "충성의 이병", "용맹한 상병", "강철의 중사", "전설의 대위"
]

function generateNickname(): string {
  return `${NICKNAMES[Math.floor(Math.random() * NICKNAMES.length)]} ${Math.floor(Math.random() * 999)}`
}

// Sample messages for demo
const SAMPLE_MESSAGES: Message[] = [
  {
    id: '1',
    type: 'system',
    content: '완충 전우회에 오신 것을 환영합니다. 100% 완충 상태를 유지하십시오.',
    timestamp: new Date(Date.now() - 300000),
  },
  {
    id: '2',
    type: 'user',
    author: '익명의 전우 247',
    rank: getRankByTime(120),
    content: '동료 전우들, 오늘도 완충 상태를 유지합시다!',
    timestamp: new Date(Date.now() - 240000),
  },
  {
    id: '3',
    type: 'announcement',
    content: '김OO 전우가 배터리 관리 소홀로 전우회를 배신했습니다 (99% 추방)',
    timestamp: new Date(Date.now() - 180000),
  },
  {
    id: '4',
    type: 'user',
    author: '무적의 소위 512',
    rank: getRankByTime(1500),
    content: '24시간 생존 달성! 소위 진급했습니다!',
    timestamp: new Date(Date.now() - 120000),
  },
  {
    id: '5',
    type: 'system',
    content: '현재 접속 중인 전우: 347명',
    timestamp: new Date(Date.now() - 60000),
  },
]

// Sample live ranking data
const SAMPLE_LIVE_RANKING: RankingUser[] = [
  { id: 'live1', nickname: '철벽의 하사 777', rank: getRankByTime(2000), survivalMinutes: 2000 },
  { id: 'live2', nickname: '전설의 대위 333', rank: getRankByTime(1500), survivalMinutes: 1500 },
  { id: 'live3', nickname: '불굴의 병장 512', rank: getRankByTime(800), survivalMinutes: 800 },
  { id: 'live4', nickname: '강철의 중사 128', rank: getRankByTime(600), survivalMinutes: 600 },
  { id: 'live5', nickname: '용맹한 상병 456', rank: getRankByTime(400), survivalMinutes: 400 },
  { id: 'live6', nickname: '충성의 이병 999', rank: getRankByTime(200), survivalMinutes: 200 },
  { id: 'live7', nickname: '무적의 소위 888', rank: getRankByTime(100), survivalMinutes: 100 },
]

// Sample all-time ranking data
const SAMPLE_ALLTIME_RANKING: RankingUser[] = [
  { id: 'all1', nickname: '대장 김OO', rank: getRankByTime(50000), survivalMinutes: 50000 },
  { id: 'all2', nickname: '대령 이OO', rank: getRankByTime(25000), survivalMinutes: 25000 },
  { id: 'all3', nickname: '중령 박OO', rank: getRankByTime(15000), survivalMinutes: 15000 },
  { id: 'all4', nickname: '소령 최OO', rank: getRankByTime(8000), survivalMinutes: 8000 },
  { id: 'all5', nickname: '대위 정OO', rank: getRankByTime(5000), survivalMinutes: 5000 },
  { id: 'all6', nickname: '중위 강OO', rank: getRankByTime(3500), survivalMinutes: 3500 },
  { id: 'all7', nickname: '소위 조OO', rank: getRankByTime(2000), survivalMinutes: 2000 },
  { id: 'all8', nickname: '원사 한OO', rank: getRankByTime(1000), survivalMinutes: 1000 },
]

export default function EliteApp() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('gatekeeper')
  const [survivalMinutes, setSurvivalMinutes] = useState(0)
  const [messages, setMessages] = useState<Message[]>(SAMPLE_MESSAGES)
  const [crisisCountdown, setCrisisCountdown] = useState<number | null>(null)
  const [myNickname] = useState(generateNickname)
  const [liveRanking, setLiveRanking] = useState<RankingUser[]>(SAMPLE_LIVE_RANKING)
  const [allTimeRanking] = useState<RankingUser[]>(SAMPLE_ALLTIME_RANKING)
  const [hasSeenOnboarding, setHasSeenOnboarding] = useState(false)
  
  // Demo battery controls
  const battery = useDemoBattery()
  const { level: batteryLevel, charging, setLevel, setCharging } = battery

  // Get current rank
  const currentRank = getRankByTime(survivalMinutes)

  // Survival timer
  useEffect(() => {
    if (currentScreen !== 'chat') return

    const timer = setInterval(() => {
      setSurvivalMinutes(prev => prev + 1)
    }, 60000) // 1 minute = 60000ms (for demo, you can change to 1000ms for faster testing)

    return () => clearInterval(timer)
  }, [currentScreen])

  // Update live ranking with my current stats
  useEffect(() => {
    if (currentScreen !== 'chat') return

    const myRanking: RankingUser = {
      id: 'me',
      nickname: myNickname,
      rank: currentRank,
      survivalMinutes: survivalMinutes,
      isMe: true,
    }

    // Insert my ranking in the right position
    const updatedRanking = [...SAMPLE_LIVE_RANKING, myRanking]
      .sort((a, b) => b.survivalMinutes - a.survivalMinutes)
      .slice(0, 10)
    
    setLiveRanking(updatedRanking)
  }, [currentScreen, survivalMinutes, myNickname, currentRank])

  // Crisis mode detection
  useEffect(() => {
    if (currentScreen !== 'chat') return

    if (batteryLevel < 100 && crisisCountdown === null) {
      // Start crisis countdown
      setCrisisCountdown(10)
      
      // Add crisis announcement
      setMessages(prev => [...prev, {
        id: Date.now().toString(),
        type: 'announcement',
        content: '경고! 귀하의 배터리가 100% 미만으로 떨어졌습니다. 10초 내로 충전하십시오!',
        timestamp: new Date(),
      }])
    } else if (batteryLevel === 100 && crisisCountdown !== null) {
      // Crisis averted
      setCrisisCountdown(null)
      setMessages(prev => [...prev, {
        id: Date.now().toString(),
        type: 'system',
        content: '충전 확인. 위기 상황 해제. 계속 생존하십시오.',
        timestamp: new Date(),
      }])
    }
  }, [batteryLevel, crisisCountdown, currentScreen])

  // Crisis countdown timer
  useEffect(() => {
    if (crisisCountdown === null || crisisCountdown <= 0) return

    const timer = setInterval(() => {
      setCrisisCountdown(prev => {
        if (prev === null) return null
        if (prev <= 1) {
          // Exile the user
          setTimeout(() => setCurrentScreen('exile'), 100)
          return null
        }
        return prev - 1
      })
    }, 1000)

    return () => clearInterval(timer)
  }, [crisisCountdown])

  // Handle entering the chat
  const handleEnter = useCallback(() => {
    if (!hasSeenOnboarding) {
      setCurrentScreen('onboarding')
    } else {
      setCurrentScreen('chat')
    }
  }, [hasSeenOnboarding])

  // Handle onboarding complete
  const handleOnboardingComplete = useCallback(() => {
    setHasSeenOnboarding(true)
    setCurrentScreen('chat')
  }, [])

  // Handle sending a message
  const handleSendMessage = useCallback((content: string) => {
    const newMessage: Message = {
      id: Date.now().toString(),
      type: 'user',
      author: myNickname,
      rank: currentRank,
      content,
      timestamp: new Date(),
      isMine: true,
    }
    setMessages(prev => [...prev, newMessage])
  }, [myNickname, currentRank])

  // Handle retry from exile
  const handleRetry = useCallback(() => {
    if (batteryLevel === 100) {
      setSurvivalMinutes(0)
      setCrisisCountdown(null)
      setCurrentScreen('chat')
      setMessages([{
        id: Date.now().toString(),
        type: 'system',
        content: '재입대 완료. 새로운 시작입니다. 이번에는 끝까지 생존하십시오.',
        timestamp: new Date(),
      }])
    }
  }, [batteryLevel])

  return (
    <div className="relative max-w-md mx-auto min-h-screen">
      {/* Demo Controls Sheet */}
      <Sheet>
        <SheetTrigger asChild>
          <Button
            variant="outline"
            size="icon"
            className="fixed top-4 right-4 z-50 bg-card/80 backdrop-blur-sm border-border/50"
          >
            <Settings className="w-4 h-4" />
          </Button>
        </SheetTrigger>
        <SheetContent side="right" className="bg-card border-border">
          <SheetHeader>
            <SheetTitle className="text-foreground">데모 컨트롤</SheetTitle>
            <SheetDescription className="text-muted-foreground">배터리 및 화면 테스트 옵션</SheetDescription>
          </SheetHeader>
          <div className="mt-6 space-y-6">
            {/* Battery Level Control */}
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <Battery className="w-4 h-4 text-primary" />
                <Label className="text-foreground">배터리 레벨</Label>
              </div>
              <Slider
                value={[batteryLevel]}
                onValueChange={(value) => setLevel(value[0])}
                max={100}
                min={0}
                step={1}
                className="w-full"
              />
              <div className="text-center font-mono text-sm text-muted-foreground">
                {batteryLevel}%
              </div>
            </div>

            {/* Charging Toggle */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Zap className="w-4 h-4 text-primary" />
                <Label className="text-foreground">충전 중</Label>
              </div>
              <Switch
                checked={charging}
                onCheckedChange={setCharging}
              />
            </div>

            {/* Quick Actions */}
            <div className="space-y-2">
              <Label className="text-muted-foreground">빠른 테스트</Label>
              <div className="grid grid-cols-2 gap-2">
                <Button 
                  variant="outline" 
                  size="sm"
                  onClick={() => setLevel(100)}
                >
                  100% 설정
                </Button>
                <Button 
                  variant="outline" 
                  size="sm"
                  onClick={() => setLevel(99)}
                  className="border-destructive/50 text-destructive"
                >
                  99% 설정
                </Button>
              </div>
            </div>

            {/* Screen Navigation (for testing) */}
            <div className="space-y-2">
              <Label className="text-muted-foreground">화면 이동</Label>
              <div className="grid grid-cols-2 gap-2">
                <Button 
                  variant="outline" 
                  size="sm"
                  onClick={() => setCurrentScreen('gatekeeper')}
                >
                  검문소
                </Button>
                <Button 
                  variant="outline" 
                  size="sm"
                  onClick={() => setCurrentScreen('onboarding')}
                >
                  교육대
                </Button>
                <Button 
                  variant="outline" 
                  size="sm"
                  onClick={() => setCurrentScreen('chat')}
                >
                  채팅
                </Button>
                <Button 
                  variant="outline" 
                  size="sm"
                  onClick={() => setCurrentScreen('exile')}
                >
                  추방
                </Button>
              </div>
            </div>

            {/* Survival Time Control */}
            <div className="space-y-3">
              <Label className="text-muted-foreground">생존 시간 (분)</Label>
              <Slider
                value={[survivalMinutes]}
                onValueChange={(value) => setSurvivalMinutes(value[0])}
                max={50000}
                min={0}
                step={5}
                className="w-full"
              />
              <div className="text-center font-mono text-sm text-muted-foreground">
                {survivalMinutes}분 ({currentRank.name})
              </div>
            </div>
          </div>
        </SheetContent>
      </Sheet>

      {/* Screen Rendering */}
      {currentScreen === 'gatekeeper' && (
        <GatekeeperScreen
          batteryLevel={batteryLevel}
          onEnter={handleEnter}
          onShowOnboarding={() => setCurrentScreen('onboarding')}
        />
      )}

      {currentScreen === 'onboarding' && (
        <OnboardingScreen onComplete={handleOnboardingComplete} />
      )}

      {currentScreen === 'chat' && (
        <ChatScreen
          batteryLevel={batteryLevel}
          charging={charging}
          survivalMinutes={survivalMinutes}
          onlineCount={347}
          crisisCountdown={crisisCountdown}
          onSendMessage={handleSendMessage}
          messages={messages}
          myNickname={myNickname}
          liveRanking={liveRanking}
          allTimeRanking={allTimeRanking}
        />
      )}

      {currentScreen === 'exile' && (
        <ExileScreen
          finalRank={currentRank}
          survivalMinutes={survivalMinutes}
          batteryLevel={batteryLevel}
          onRetry={handleRetry}
        />
      )}
    </div>
  )
}
