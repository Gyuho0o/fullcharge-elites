// 완충 전우회 계급 시스템 (Military Ranking System)

export interface Rank {
  id: string
  name: string
  englishName: string
  category: 'enlisted' | 'nco' | 'officer' | 'general'
  minSurvivalMinutes: number
  icon: string
  description: string
}

export const RANKS: Rank[] = [
  // 병 (Enlisted)
  { id: 'trainee', name: '훈련병', englishName: 'Trainee', category: 'enlisted', minSurvivalMinutes: 0, icon: '⬜', description: '첫 입대' },
  { id: 'private', name: '이등병', englishName: 'Private', category: 'enlisted', minSurvivalMinutes: 5, icon: '▫️', description: '5분 생존' },
  { id: 'pfc', name: '일등병', englishName: 'PFC', category: 'enlisted', minSurvivalMinutes: 15, icon: '◽', description: '15분 생존' },
  { id: 'corporal', name: '상등병', englishName: 'Corporal', category: 'enlisted', minSurvivalMinutes: 30, icon: '◻️', description: '30분 생존' },
  { id: 'sergeant', name: '병장', englishName: 'Sergeant', category: 'enlisted', minSurvivalMinutes: 60, icon: '🔲', description: '1시간 생존' },
  
  // 부사관 (NCO)
  { id: 'staffsergeant', name: '하사', englishName: 'Staff Sergeant', category: 'nco', minSurvivalMinutes: 120, icon: '🔷', description: '2시간 생존' },
  { id: 'sergeant1st', name: '중사', englishName: 'Sergeant 1st Class', category: 'nco', minSurvivalMinutes: 240, icon: '🔶', description: '4시간 생존' },
  { id: 'mastersergeant', name: '상사', englishName: 'Master Sergeant', category: 'nco', minSurvivalMinutes: 480, icon: '💠', description: '8시간 생존' },
  { id: 'sergeantmajor', name: '원사', englishName: 'Sergeant Major', category: 'nco', minSurvivalMinutes: 720, icon: '🔹', description: '12시간 생존' },
  
  // 위관 (Officer)
  { id: 'secondlt', name: '소위', englishName: '2nd Lieutenant', category: 'officer', minSurvivalMinutes: 1440, icon: '⭐', description: '24시간 생존' },
  { id: 'firstlt', name: '중위', englishName: '1st Lieutenant', category: 'officer', minSurvivalMinutes: 2880, icon: '🌟', description: '48시간 생존' },
  { id: 'captain', name: '대위', englishName: 'Captain', category: 'officer', minSurvivalMinutes: 4320, icon: '✨', description: '72시간 생존' },
  
  // 영관/장성 (General)
  { id: 'major', name: '소령', englishName: 'Major', category: 'general', minSurvivalMinutes: 7200, icon: '🎖️', description: '5일 생존' },
  { id: 'ltcolonel', name: '중령', englishName: 'Lieutenant Colonel', category: 'general', minSurvivalMinutes: 10080, icon: '🏅', description: '7일 생존' },
  { id: 'colonel', name: '대령', englishName: 'Colonel', category: 'general', minSurvivalMinutes: 20160, icon: '🥇', description: '14일 생존' },
  { id: 'general', name: '대장', englishName: 'General', category: 'general', minSurvivalMinutes: 43200, icon: '⭐⭐⭐⭐', description: '30일 생존' },
]

export function getRankByTime(survivalMinutes: number): Rank {
  let currentRank = RANKS[0]
  for (const rank of RANKS) {
    if (survivalMinutes >= rank.minSurvivalMinutes) {
      currentRank = rank
    } else {
      break
    }
  }
  return currentRank
}

export function formatSurvivalTime(minutes: number): string {
  if (minutes < 60) {
    return `${minutes}분`
  }
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  if (hours < 24) {
    return mins > 0 ? `${hours}시간 ${mins}분` : `${hours}시간`
  }
  const days = Math.floor(hours / 24)
  const remainingHours = hours % 24
  if (remainingHours > 0) {
    return `${days}일 ${remainingHours}시간`
  }
  return `${days}일`
}

export function getNextRank(currentRank: Rank): Rank | null {
  const currentIndex = RANKS.findIndex(r => r.id === currentRank.id)
  if (currentIndex < RANKS.length - 1) {
    return RANKS[currentIndex + 1]
  }
  return null
}

export function getRankProgress(survivalMinutes: number, currentRank: Rank): number {
  const nextRank = getNextRank(currentRank)
  if (!nextRank) return 100
  
  const currentMin = currentRank.minSurvivalMinutes
  const nextMin = nextRank.minSurvivalMinutes
  const progress = ((survivalMinutes - currentMin) / (nextMin - currentMin)) * 100
  return Math.min(Math.max(progress, 0), 100)
}
