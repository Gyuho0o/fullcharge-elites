"use client"

import { cn } from "@/lib/utils"

interface BatteryIconProps {
  level: number
  charging?: boolean
  size?: "sm" | "md" | "lg" | "xl"
  animated?: boolean
  className?: string
}

export function BatteryIcon({ 
  level, 
  charging = false, 
  size = "md",
  animated = true,
  className 
}: BatteryIconProps) {
  const isFullCharge = level === 100
  const isCritical = level < 100

  const sizeClasses = {
    sm: "w-12 h-6",
    md: "w-20 h-10",
    lg: "w-32 h-16",
    xl: "w-48 h-24",
  }

  const tipSizes = {
    sm: "w-1 h-3",
    md: "w-1.5 h-5",
    lg: "w-2 h-8",
    xl: "w-3 h-12",
  }

  return (
    <div className={cn("relative flex items-center", className)}>
      {/* Battery Body */}
      <div
        className={cn(
          "relative border-2 rounded-sm overflow-hidden",
          sizeClasses[size],
          isFullCharge 
            ? "border-primary shadow-[0_0_20px_rgba(0,255,0,0.5)]" 
            : "border-destructive shadow-[0_0_20px_rgba(255,0,0,0.5)]",
          animated && isFullCharge && "animate-pulse"
        )}
      >
        {/* Fill Level */}
        <div
          className={cn(
            "absolute inset-0 transition-all duration-500",
            isFullCharge 
              ? "bg-primary" 
              : "bg-destructive"
          )}
          style={{ width: `${level}%` }}
        />
        
        {/* Scan Lines Effect */}
        <div className="absolute inset-0 bg-[linear-gradient(transparent_50%,rgba(0,0,0,0.3)_50%)] bg-[length:100%_4px] pointer-events-none" />
        
        {/* Level Text */}
        <div className={cn(
          "absolute inset-0 flex items-center justify-center font-mono font-bold text-background",
          size === "sm" && "text-xs",
          size === "md" && "text-sm",
          size === "lg" && "text-xl",
          size === "xl" && "text-3xl",
        )}>
          {level}%
        </div>

        {/* Charging Icon */}
        {charging && (
          <div className="absolute inset-0 flex items-center justify-center">
            <svg 
              viewBox="0 0 24 24" 
              className={cn(
                "fill-background/80",
                size === "sm" && "w-3 h-3",
                size === "md" && "w-5 h-5",
                size === "lg" && "w-8 h-8",
                size === "xl" && "w-12 h-12",
                animated && "animate-pulse"
              )}
            >
              <path d="M11 21h-1l1-7H7.5c-.58 0-.57-.32-.38-.66.19-.34.05-.08.07-.12C8.48 10.94 10.42 7.54 13 3h1l-1 7h3.5c.49 0 .56.33.47.51l-.07.15C12.96 17.55 11 21 11 21z" />
            </svg>
          </div>
        )}
      </div>

      {/* Battery Tip */}
      <div 
        className={cn(
          "rounded-r-sm",
          tipSizes[size],
          isFullCharge ? "bg-primary" : "bg-destructive"
        )} 
      />

      {/* Glow Effect for Full Charge */}
      {isFullCharge && animated && (
        <div className="absolute inset-0 -z-10">
          <div className="absolute inset-0 bg-primary/20 blur-xl rounded-full animate-pulse" />
        </div>
      )}

      {/* Crisis Effect */}
      {isCritical && animated && (
        <div className="absolute inset-0 -z-10">
          <div className="absolute inset-0 bg-destructive/30 blur-xl rounded-full animate-pulse" />
        </div>
      )}
    </div>
  )
}
