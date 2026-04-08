"use client"

import { useState, useEffect, useCallback } from 'react'

interface BatteryManager extends EventTarget {
  charging: boolean
  chargingTime: number
  dischargingTime: number
  level: number
}

interface BatteryState {
  level: number
  charging: boolean
  supported: boolean
  isFullCharge: boolean
}

declare global {
  interface Navigator {
    getBattery?: () => Promise<BatteryManager>
  }
}

export function useBattery() {
  const [battery, setBattery] = useState<BatteryState>({
    level: 100,
    charging: true,
    supported: true,
    isFullCharge: true,
  })

  const updateBattery = useCallback((batteryManager: BatteryManager) => {
    const level = Math.round(batteryManager.level * 100)
    setBattery({
      level,
      charging: batteryManager.charging,
      supported: true,
      isFullCharge: level === 100,
    })
  }, [])

  useEffect(() => {
    let batteryManager: BatteryManager | null = null

    const initBattery = async () => {
      if (typeof navigator !== 'undefined' && navigator.getBattery) {
        try {
          batteryManager = await navigator.getBattery()
          updateBattery(batteryManager)

          const handleChange = () => {
            if (batteryManager) {
              updateBattery(batteryManager)
            }
          }

          batteryManager.addEventListener('levelchange', handleChange)
          batteryManager.addEventListener('chargingchange', handleChange)

          return () => {
            if (batteryManager) {
              batteryManager.removeEventListener('levelchange', handleChange)
              batteryManager.removeEventListener('chargingchange', handleChange)
            }
          }
        } catch {
          // Battery API not supported or permission denied
          setBattery(prev => ({ ...prev, supported: false }))
        }
      } else {
        // For demo purposes, simulate battery at 100%
        setBattery({
          level: 100,
          charging: true,
          supported: false,
          isFullCharge: true,
        })
      }
    }

    initBattery()
  }, [updateBattery])

  return battery
}

// Demo mode hook for testing different battery states
export function useDemoBattery() {
  const [demoLevel, setDemoLevel] = useState(100)
  const [demoCharging, setDemoCharging] = useState(true)

  return {
    level: demoLevel,
    charging: demoCharging,
    supported: true,
    isFullCharge: demoLevel === 100,
    setLevel: setDemoLevel,
    setCharging: setDemoCharging,
  }
}
