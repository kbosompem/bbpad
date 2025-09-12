import React from 'react'

interface BabashkaLogoProps {
  className?: string
  size?: number
}

export function BabashkaLogo({ className = "", size = 24 }: BabashkaLogoProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
    >
      {/* Babashka-inspired design with script/terminal theme */}
      <g>
        {/* Main circular background */}
        <circle
          cx="12"
          cy="12"
          r="10"
          fill="currentColor"
          className="text-primary"
          opacity="0.1"
        />
        
        {/* Terminal window frame */}
        <rect
          x="4"
          y="6"
          width="16"
          height="12"
          rx="2"
          stroke="currentColor"
          strokeWidth="1.5"
          fill="none"
          className="text-primary"
        />
        
        {/* Terminal header bar */}
        <rect
          x="4"
          y="6"
          width="16"
          height="3"
          rx="2"
          fill="currentColor"
          className="text-primary"
          opacity="0.2"
        />
        
        {/* Terminal dots */}
        <circle cx="6.5" cy="7.5" r="0.5" fill="currentColor" className="text-destructive" />
        <circle cx="8" cy="7.5" r="0.5" fill="currentColor" className="text-yellow-500" />
        <circle cx="9.5" cy="7.5" r="0.5" fill="currentColor" className="text-green-500" />
        
        {/* Code brackets/parens representing Clojure */}
        <text
          x="7"
          y="15"
          fontSize="8"
          fontFamily="monospace"
          fill="currentColor"
          className="text-primary font-bold"
        >
          ()
        </text>
        
        {/* Lightning bolt for Babashka speed */}
        <path
          d="M14 11l-2 3h1.5l-1.5 3 2-3H13l1-3z"
          fill="currentColor"
          className="text-yellow-500"
          strokeWidth="0.5"
        />
      </g>
    </svg>
  )
}