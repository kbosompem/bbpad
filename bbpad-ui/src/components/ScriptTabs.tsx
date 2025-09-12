import React, { useState, useCallback } from 'react'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Button } from '@/components/ui/button'
import { X, Plus, FileText } from 'lucide-react'

export interface ScriptTab {
  id: string
  title: string
  content: string
  modified: boolean
  language?: string
}

interface ScriptTabsProps {
  tabs: ScriptTab[]
  activeTab: string
  onTabChange: (tabId: string) => void
  onTabClose: (tabId: string) => void
  onTabAdd: () => void
  onTabContentChange: (tabId: string, content: string) => void
  children: (tab: ScriptTab) => React.ReactNode
  className?: string
}

export function ScriptTabs({
  tabs,
  activeTab,
  onTabChange,
  onTabClose,
  onTabAdd,
  onTabContentChange,
  children,
  className = ""
}: ScriptTabsProps) {
  
  const handleTabClose = useCallback((e: React.MouseEvent, tabId: string) => {
    e.stopPropagation()
    onTabClose(tabId)
  }, [onTabClose])

  const getTabTitle = (tab: ScriptTab) => {
    const baseTitle = tab.title || 'Untitled'
    return tab.modified ? `${baseTitle} •` : baseTitle
  }

  return (
    <div className={`flex flex-col h-full ${className}`}>
      {/* Tab Strip */}
      <div className="flex items-center border-b bg-muted/30">
        <Tabs value={activeTab} onValueChange={onTabChange} className="flex-1">
          <TabsList className="h-9 bg-transparent justify-start rounded-none border-none p-0">
            {tabs.map((tab) => (
              <TabsTrigger
                key={tab.id}
                value={tab.id}
                className="group relative h-9 px-3 py-1.5 text-sm bg-transparent data-[state=active]:bg-background data-[state=active]:border-b-2 data-[state=active]:border-primary data-[state=active]:shadow-none hover:bg-muted/50 rounded-none border-r border-border/50 last:border-r-0"
              >
                <div className="flex items-center gap-2">
                  <FileText className="h-3 w-3" />
                  <span className="max-w-[120px] truncate">
                    {getTabTitle(tab)}
                  </span>
                  {tabs.length > 1 && (
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-4 w-4 p-0 hover:bg-muted-foreground/20 opacity-0 group-hover:opacity-100 transition-opacity"
                      onClick={(e) => handleTabClose(e, tab.id)}
                    >
                      <X className="h-3 w-3" />
                    </Button>
                  )}
                </div>
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>
        
        {/* Add Tab Button */}
        <div className="px-2 py-1">
          <Button
            variant="ghost"
            size="sm"
            onClick={onTabAdd}
            className="h-7 w-7 p-0 hover:bg-muted"
          >
            <Plus className="h-3 w-3" />
          </Button>
        </div>
      </div>

      {/* Tab Content */}
      <div className="flex-1 overflow-hidden">
        <Tabs value={activeTab} onValueChange={onTabChange} className="h-full">
          {tabs.map((tab) => (
            <TabsContent
              key={tab.id}
              value={tab.id}
              className="h-full m-0 p-0 data-[state=inactive]:hidden"
            >
              {children(tab)}
            </TabsContent>
          ))}
        </Tabs>
      </div>
    </div>
  )
}

export function useScriptTabs(initialTab?: Partial<ScriptTab>) {
  const defaultTab: ScriptTab = {
    id: 'tab-1',
    title: 'Script 1',
    content: '; Welcome to BBPad - Babashka Script Runner\n; A LINQPad-inspired tool for Clojure development\n\n; Try these examples:\n(println "Hello, BBPad!")\n\n; Create a map\n{:name "Alice" :age 30 :role "Developer"}\n\n; Work with collections\n(->> (range 1 11)\n     (filter odd?)\n     (map #(* % %))\n     (reduce +))',
    modified: false,
    language: 'clojure'
  }

  const [tabs, setTabs] = useState<ScriptTab[]>([
    { ...defaultTab, ...initialTab }
  ])
  const [activeTab, setActiveTab] = useState(tabs[0].id)

  const addTab = useCallback(() => {
    const newId = `tab-${Date.now()}`
    const newTab: ScriptTab = {
      id: newId,
      title: `Script ${tabs.length + 1}`,
      content: '; New script\n',
      modified: false,
      language: 'clojure'
    }
    setTabs(prev => [...prev, newTab])
    setActiveTab(newId)
  }, [tabs.length])

  const closeTab = useCallback((tabId: string) => {
    if (tabs.length <= 1) return // Don't close the last tab
    
    setTabs(prev => {
      const newTabs = prev.filter(tab => tab.id !== tabId)
      if (activeTab === tabId && newTabs.length > 0) {
        setActiveTab(newTabs[0].id)
      }
      return newTabs
    })
  }, [tabs.length, activeTab])

  const updateTabContent = useCallback((tabId: string, content: string) => {
    setTabs(prev => prev.map(tab => 
      tab.id === tabId 
        ? { ...tab, content, modified: content !== defaultTab.content }
        : tab
    ))
  }, [])

  const updateTabTitle = useCallback((tabId: string, title: string) => {
    setTabs(prev => prev.map(tab => 
      tab.id === tabId ? { ...tab, title } : tab
    ))
  }, [])

  const getCurrentTab = useCallback(() => {
    return tabs.find(tab => tab.id === activeTab) || tabs[0]
  }, [tabs, activeTab])

  return {
    tabs,
    activeTab,
    setActiveTab,
    addTab,
    closeTab,
    updateTabContent,
    updateTabTitle,
    getCurrentTab
  }
}