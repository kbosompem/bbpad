import { useState, useEffect } from 'react'
import { ThemeProvider } from '@/components/ThemeProvider'
import { Toolbar, ToolbarButton, ToolbarSeparator } from '@/components/ui/toolbar'
import { ResizablePanelGroup, ResizablePanel, ResizableHandle } from '@/components/ui/resizable'
import { Button } from '@/components/ui/button'
import { CodeEditor } from '@/components/CodeEditor'
import { ResultsPanel } from '@/components/ResultsPanel'
import { DatabaseConnectionsDialog } from '@/components/DatabaseConnectionsDialog'
import { ConnectionsPanel } from '@/components/ConnectionsPanel'
import { ScriptTabs, useScriptTabs } from '@/components/ScriptTabs'
import { useCommandPalette } from '@/components/CommandPalette'
import { BabashkaLogo } from '@/components/BabashkaLogo'
import { SaveScriptDialog } from '@/components/SaveScriptDialog'
import { OpenScriptDialog } from '@/components/OpenScriptDialog'
import {
  Play,
  Square,
  Save,
  FolderOpen,
  Settings,
  Moon,
  Sun,
  Database,
  Share,
  HelpCircle,
  Plus
} from 'lucide-react'
import { useTheme } from 'next-themes'
import { ThemeSelector } from '@/components/ThemeSelector'
import { initializeTheme } from '@/lib/themes'

interface ApiResponse {
  success: boolean
  result?: {
    type: string
    content: string
    data?: any
    'data-type'?: string
  }
  output?: string
  error?: string
  'execution-time'?: number
}

// ThemeToggle replaced by ThemeSelector component

function AppContent() {
  const scriptTabs = useScriptTabs()
  const [result, setResult] = useState<string>('')
  const [isExecuting, setIsExecuting] = useState(false)
  const { theme, setTheme } = useTheme()

  const executeCode = async () => {
    const currentTab = scriptTabs.getCurrentTab()
    setIsExecuting(true)
    setResult('Executing...')

    try {
      const response = await fetch('http://localhost:8082/api/execute', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ code: currentTab.content }),
      })

      const data: ApiResponse = await response.json()

      if (data.success) {
        let resultText = ''

        if (data.output) {
          resultText += `Output:\n${data.output}\n\n`
        }

        if (data.result) {
          resultText += `Result (${data.result.type}):\n${data.result.content}`
        }

        if (data['execution-time']) {
          resultText += `\n\n⚡ Executed in ${data['execution-time']}ms`
        }

        setResult(resultText)
      } else {
        setResult(`❌ Error: ${data.error || 'Unknown error occurred'}`)
      }
    } catch (error) {
      setResult(`❌ Failed to execute: ${error}`)
    } finally {
      setIsExecuting(false)
    }
  }

  const stopExecution = () => {
    // TODO: Implement execution cancellation
    setIsExecuting(false)
  }

  const saveScript = async (name: string, tags?: string) => {
    const currentTab = scriptTabs.getCurrentTab()

    try {
      const response = await fetch('http://localhost:8082/api/scripts/save', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          id: currentTab.id.startsWith('script-') ? currentTab.id : undefined,
          name: name,
          content: currentTab.content,
          language: 'clojure',
          tags: tags || ''
        }),
      })

      const data = await response.json()
      if (data.success) {
        // Update the tab with the saved script info
        scriptTabs.updateTab(currentTab.id, {
          id: data.id, // Use the script ID returned from the server
          title: name,
          modified: false // Mark as saved
        })
      } else {
        throw new Error(data.error || 'Failed to save script')
      }
    } catch (error) {
      throw error
    }
  }

  const openScript = async (scriptId: string) => {
    try {
      const scriptResponse = await fetch(`http://localhost:8082/api/scripts/get/${scriptId}`)
      const scriptData = await scriptResponse.json()

      if (scriptData.success && scriptData.script) {
        const script = scriptData.script
        scriptTabs.addTab({
          id: script.id,
          title: script.name,
          content: script.content,
          modified: false,
          language: script.language || 'clojure'
        })
      } else {
        throw new Error('Failed to load script')
      }
    } catch (error) {
      throw error
    }
  }

  // Command Palette Setup
  const commands = [
    {
      id: 'execute',
      title: 'Execute Script',
      description: 'Run the current script (F5)',
      icon: <Play className="h-4 w-4" />,
      action: executeCode,
      keywords: ['run', 'execute', 'play', 'f5']
    },
    {
      id: 'stop',
      title: 'Stop Execution',
      description: 'Stop the running script',
      icon: <Square className="h-4 w-4" />,
      action: stopExecution,
      keywords: ['stop', 'cancel', 'abort']
    },
    {
      id: 'new-tab',
      title: 'New Script Tab',
      description: 'Create a new script tab',
      icon: <Plus className="h-4 w-4" />,
      action: scriptTabs.addTab,
      keywords: ['new', 'tab', 'script', 'create']
    },
    {
      id: 'toggle-theme',
      title: 'Toggle Theme',
      description: `Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`,
      icon: theme === 'dark' ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />,
      action: () => setTheme(theme === 'dark' ? 'light' : 'dark'),
      keywords: ['theme', 'dark', 'light', 'mode']
    }
  ]

  const commandPalette = useCommandPalette(commands)

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'F5' && !isExecuting) {
        event.preventDefault()
        executeCode()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [executeCode, isExecuting])

  return (
    <div className="h-screen flex flex-col bg-background">
      {/* Title Bar */}
      <div className="shrink-0 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="flex items-center justify-between px-4 py-2">
          <div className="flex items-center gap-3">
            <BabashkaLogo size={28} className="text-primary" />
            <div className="flex flex-col">
              <h1 className="text-lg font-bold text-foreground leading-none">BBPad</h1>
              <p className="text-xs text-muted-foreground leading-none">Babashka Script Runner</p>
            </div>
          </div>

          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <span className="hidden sm:inline">Babashka-powered Desktop App</span>
            <div className="h-4 w-px bg-border hidden sm:block" />
            <span>v0.1.0</span>
          </div>
        </div>
      </div>

      {/* Main Toolbar */}
      <Toolbar className="shrink-0">
        <div className="flex items-center gap-1">
          <ToolbarButton onClick={isExecuting ? stopExecution : executeCode} disabled={false}>
            {isExecuting ? (
              <Square className="h-4 w-4 mr-1 fill-current" />
            ) : (
              <Play className="h-4 w-4 mr-1 fill-current" />
            )}
            {isExecuting ? 'Stop (F5)' : 'Execute (F5)'}
          </ToolbarButton>
        </div>

        <ToolbarSeparator />

        <OpenScriptDialog onOpen={openScript} />

        <SaveScriptDialog
          currentScript={{
            id: scriptTabs.getCurrentTab().id,
            name: scriptTabs.getCurrentTab().name || '',
            content: scriptTabs.getCurrentTab().content
          }}
          onSave={saveScript}
        />

        <ToolbarSeparator />

        <DatabaseConnectionsDialog
          trigger={
            <ToolbarButton>
              <Database className="h-4 w-4 mr-1" />
              Connections
            </ToolbarButton>
          }
        />

        <ToolbarButton>
          <Share className="h-4 w-4 mr-1" />
          Share
        </ToolbarButton>

        <div className="flex-1" />

        <ToolbarButton>
          <HelpCircle className="h-4 w-4 mr-1" />
          Help
        </ToolbarButton>

        <ToolbarButton>
          <Settings className="h-4 w-4 mr-1" />
          Settings
        </ToolbarButton>

        <ThemeSelector />
      </Toolbar>

      {/* Main Content Area */}
      <div className="flex-1 overflow-hidden">
        <ResizablePanelGroup direction="horizontal" className="h-full">
          {/* Left Panel - Code Editor and Results */}
          <ResizablePanel defaultSize={75} minSize={50}>
            <ResizablePanelGroup direction="vertical" className="h-full">
              {/* Code Editor Panel */}
              <ResizablePanel defaultSize={60} minSize={30}>
                <ScriptTabs
                  tabs={scriptTabs.tabs}
                  activeTab={scriptTabs.activeTab}
                  onTabChange={scriptTabs.setActiveTab}
                  onTabClose={scriptTabs.closeTab}
                  onTabAdd={scriptTabs.addTab}
                  onTabContentChange={scriptTabs.updateTabContent}
                  className="h-full"
                >
                  {(tab) => (
                    <div className="h-full p-4">
                      <div className="flex items-center justify-between mb-2">
                        <h2 className="text-sm font-semibold text-muted-foreground">Script Editor</h2>
                        <div className="text-xs text-muted-foreground">
                          {tab.language || 'Clojure'} • {tab.content.split('\n').length} lines
                        </div>
                      </div>
                      <CodeEditor
                        value={tab.content}
                        onChange={(value) => scriptTabs.updateTabContent(tab.id, value || '')}
                        className="h-[calc(100%-2rem)]"
                      />
                    </div>
                  )}
                </ScriptTabs>
              </ResizablePanel>

              <ResizableHandle withHandle />

              {/* Results Panel */}
              <ResizablePanel defaultSize={40} minSize={20}>
                <ResultsPanel
                  result={result}
                  isExecuting={isExecuting}
                  className="h-full"
                />
              </ResizablePanel>
            </ResizablePanelGroup>
          </ResizablePanel>

          <ResizableHandle withHandle />

          {/* Right Panel - Connections */}
          <ResizablePanel defaultSize={25} minSize={20} maxSize={40}>
            <ConnectionsPanel className="h-full border-l" />
          </ResizablePanel>
        </ResizablePanelGroup>
      </div>

      {/* Status Bar */}
      <div className="border-t px-4 py-1 text-xs text-muted-foreground bg-muted/30 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <span>Ready</span>
          <span>•</span>
          <span>localhost:8082</span>
        </div>
        <div className="flex items-center gap-2">
          <span>BBPad v0.1.0</span>
        </div>
      </div>

      {/* Command Palette */}
      <commandPalette.CommandPalette />
    </div>
  )
}

function App() {
  return (
    <ThemeProvider
      attribute="class"
      defaultTheme="system"
      enableSystem
      disableTransitionOnChange
    >
      <AppContent />
    </ThemeProvider>
  )
}

export default App