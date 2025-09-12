import { useState, useEffect } from 'react'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  DropdownMenuSeparator,
  DropdownMenuLabel,
} from '@/components/ui/dropdown-menu'
import { Button } from '@/components/ui/button'
import { Moon, Sun, Palette } from 'lucide-react'
import { themes, applyTheme, getStoredTheme, type ThemeKey } from '@/lib/themes'

export function ThemeSelector() {
  const [currentTheme, setCurrentTheme] = useState<ThemeKey>('default')
  const [currentMode, setCurrentMode] = useState<'light' | 'dark'>('dark')

  useEffect(() => {
    const { theme, mode } = getStoredTheme()
    setCurrentTheme(theme)
    setCurrentMode(mode)
    applyTheme(theme, mode)
  }, [])

  const handleThemeChange = (theme: ThemeKey) => {
    setCurrentTheme(theme)
    applyTheme(theme, currentMode)
  }

  const handleModeToggle = () => {
    const newMode = currentMode === 'light' ? 'dark' : 'light'
    setCurrentMode(newMode)
    applyTheme(currentTheme, newMode)
  }

  return (
    <div className="flex items-center gap-2">
      <Button
        variant="ghost"
        size="sm"
        onClick={handleModeToggle}
        className="h-8 w-8 p-0"
      >
        {currentMode === 'light' ? (
          <Sun className="h-4 w-4" />
        ) : (
          <Moon className="h-4 w-4" />
        )}
      </Button>
      
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="sm" className="h-8 px-2">
            <Palette className="h-4 w-4 mr-2" />
            <span className="text-xs">{themes[currentTheme].name}</span>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuLabel>Choose Theme</DropdownMenuLabel>
          <DropdownMenuSeparator />
          {Object.entries(themes).map(([key, theme]) => (
            <DropdownMenuItem
              key={key}
              onClick={() => handleThemeChange(key as ThemeKey)}
              className={currentTheme === key ? 'bg-accent' : ''}
            >
              <div className="flex items-center gap-2">
                {key === 'amber-minimal' && (
                  <div className="w-3 h-3 rounded-full bg-amber-500" />
                )}
                {key === 'ocean' && (
                  <div className="w-3 h-3 rounded-full bg-blue-500" />
                )}
                {key === 'forest' && (
                  <div className="w-3 h-3 rounded-full bg-green-500" />
                )}
                {key === 'purple' && (
                  <div className="w-3 h-3 rounded-full bg-purple-500" />
                )}
                {key === 'retro-arcade' && (
                  <div className="w-3 h-3 rounded-full bg-gradient-to-r from-pink-500 to-cyan-500" />
                )}
                {key === 'tangerine' && (
                  <div className="w-3 h-3 rounded-full bg-orange-500" />
                )}
                {key === 'violet-bloom' && (
                  <div className="w-3 h-3 rounded-full bg-gradient-to-r from-violet-500 to-pink-500" />
                )}
                {key === 'default' && (
                  <div className="w-3 h-3 rounded-full bg-gray-500" />
                )}
                <span>{theme.name}</span>
              </div>
            </DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  )
}