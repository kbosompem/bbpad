import { Editor } from '@monaco-editor/react'
import { useTheme } from 'next-themes'
import { cn } from '@/lib/utils'
import { useEffect, useState } from 'react'

interface CodeEditorProps {
  value: string
  onChange: (value: string | undefined) => void
  className?: string
  language?: string
  readOnly?: boolean
}

export function CodeEditor({
  value,
  onChange,
  className,
  language = 'clojure',
  readOnly = false
}: CodeEditorProps) {
  const { theme, resolvedTheme } = useTheme()
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  const isDark = mounted && (resolvedTheme === 'dark' || theme === 'dark')

  const handleEditorMount = (editor: any, monaco: any) => {
    const customThemeName = isDark ? 'bbpad-dark' : 'bbpad-light'

    // Get CSS custom property value
    const rootStyles = getComputedStyle(document.documentElement)
    const bgHsl = rootStyles.getPropertyValue('--background').trim()

    // Convert HSL to RGB
    let backgroundColor = '#ffffff'
    if (bgHsl) {
      const [h, s, l] = bgHsl.split(' ').map(v => parseFloat(v.replace('%', '')))
      const hslToRgb = (h: number, s: number, l: number) => {
        s /= 100
        l /= 100
        const c = (1 - Math.abs(2 * l - 1)) * s
        const x = c * (1 - Math.abs(((h / 60) % 2) - 1))
        const m = l - c / 2
        let r = 0, g = 0, b = 0

        if (0 <= h && h < 60) {
          r = c; g = x; b = 0
        } else if (60 <= h && h < 120) {
          r = x; g = c; b = 0
        } else if (120 <= h && h < 180) {
          r = 0; g = c; b = x
        } else if (180 <= h && h < 240) {
          r = 0; g = x; b = c
        } else if (240 <= h && h < 300) {
          r = x; g = 0; b = c
        } else if (300 <= h && h < 360) {
          r = c; g = 0; b = x
        }

        r = Math.round((r + m) * 255)
        g = Math.round((g + m) * 255)
        b = Math.round((b + m) * 255)

        return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`
      }
      backgroundColor = hslToRgb(h, s, l)
    }

    monaco.editor.defineTheme(customThemeName, {
      base: isDark ? 'vs-dark' : 'vs',
      inherit: true,
      rules: [],
      colors: {
        'editor.background': backgroundColor,
      }
    })

    monaco.editor.setTheme(customThemeName)
  }

  return (
    <div className={cn("border rounded-md overflow-hidden", className)}>
      <Editor
        height="100%"
        defaultLanguage={language}
        value={value}
        onChange={onChange}
        theme={isDark ? 'vs-dark' : 'vs'}
        onMount={handleEditorMount}
        options={{
          minimap: { enabled: false },
          fontSize: 14,
          fontFamily: 'Consolas, Monaco, monospace',
          lineNumbers: 'on',
          scrollBeyondLastLine: false,
          automaticLayout: true,
          tabSize: 2,
          insertSpaces: true,
          wordWrap: 'on',
          readOnly,
          bracketPairColorization: { enabled: true },
          folding: true,
          showFoldingControls: 'always',
          matchBrackets: 'always',
        }}
      />
    </div>
  )
}