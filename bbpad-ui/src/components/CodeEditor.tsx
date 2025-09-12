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

  const editorTheme = mounted && (resolvedTheme === 'dark' || theme === 'dark') ? 'vs-dark' : 'vs'

  return (
    <div className={cn("border rounded-md overflow-hidden", className)}>
      <Editor
        height="100%"
        defaultLanguage={language}
        value={value}
        onChange={onChange}
        theme={editorTheme}
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