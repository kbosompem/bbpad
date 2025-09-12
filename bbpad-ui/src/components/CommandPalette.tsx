import React, { useState, useEffect, useCallback } from 'react'
import { Dialog, DialogContent } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { ScrollArea } from '@/components/ui/scroll-area'
import { 
  Play, 
  Save, 
  FolderOpen, 
  Database, 
  Settings, 
  Moon, 
  Sun,
  Plus,
  FileText,
  Search,
  Command
} from 'lucide-react'

interface Command {
  id: string
  title: string
  description?: string
  icon: React.ReactNode
  action: () => void
  keywords?: string[]
}

interface CommandPaletteProps {
  isOpen: boolean
  onClose: () => void
  commands: Command[]
}

export function CommandPalette({ isOpen, onClose, commands }: CommandPaletteProps) {
  const [query, setQuery] = useState('')
  const [selectedIndex, setSelectedIndex] = useState(0)
  
  const filteredCommands = commands.filter(command => {
    const searchText = query.toLowerCase()
    const titleMatch = command.title.toLowerCase().includes(searchText)
    const descriptionMatch = command.description?.toLowerCase().includes(searchText)
    const keywordsMatch = command.keywords?.some(keyword => 
      keyword.toLowerCase().includes(searchText)
    )
    return titleMatch || descriptionMatch || keywordsMatch
  })

  useEffect(() => {
    if (isOpen) {
      setQuery('')
      setSelectedIndex(0)
    }
  }, [isOpen])

  useEffect(() => {
    setSelectedIndex(0)
  }, [query])

  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    if (!isOpen) return

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        setSelectedIndex(prev => 
          prev < filteredCommands.length - 1 ? prev + 1 : prev
        )
        break
      case 'ArrowUp':
        e.preventDefault()
        setSelectedIndex(prev => prev > 0 ? prev - 1 : prev)
        break
      case 'Enter':
        e.preventDefault()
        if (filteredCommands[selectedIndex]) {
          filteredCommands[selectedIndex].action()
          onClose()
        }
        break
      case 'Escape':
        e.preventDefault()
        onClose()
        break
    }
  }, [isOpen, filteredCommands, selectedIndex, onClose])

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [handleKeyDown])

  const executeCommand = (command: Command) => {
    command.action()
    onClose()
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-2xl max-h-[80vh] p-0 overflow-hidden">
        <div className="flex flex-col h-full">
          {/* Search Input */}
          <div className="flex items-center border-b px-4 py-3">
            <Search className="h-4 w-4 text-muted-foreground mr-3" />
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Type a command or search..."
              className="border-0 focus-visible:ring-0 text-base"
              autoFocus
            />
            <div className="flex items-center gap-1 ml-3 text-xs text-muted-foreground">
              <kbd className="pointer-events-none inline-flex h-5 select-none items-center gap-1 rounded border bg-muted px-1.5 font-mono text-[10px] font-medium text-muted-foreground">
                <Command className="h-3 w-3" />
              </kbd>
              <kbd className="pointer-events-none inline-flex h-5 select-none items-center gap-1 rounded border bg-muted px-1.5 font-mono text-[10px] font-medium text-muted-foreground">
                K
              </kbd>
            </div>
          </div>

          {/* Commands List */}
          <ScrollArea className="flex-1 max-h-96">
            <div className="p-2">
              {filteredCommands.length === 0 ? (
                <div className="py-8 text-center text-muted-foreground">
                  <Search className="h-8 w-8 mx-auto mb-2 opacity-50" />
                  <p>No commands found</p>
                  <p className="text-xs">Try a different search term</p>
                </div>
              ) : (
                filteredCommands.map((command, index) => (
                  <div
                    key={command.id}
                    className={`flex items-center gap-3 px-3 py-2 rounded-md cursor-pointer transition-colors ${
                      index === selectedIndex
                        ? 'bg-accent text-accent-foreground'
                        : 'hover:bg-muted/50'
                    }`}
                    onClick={() => executeCommand(command)}
                  >
                    <div className="flex-shrink-0">
                      {command.icon}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="font-medium text-sm">{command.title}</div>
                      {command.description && (
                        <div className="text-xs text-muted-foreground truncate">
                          {command.description}
                        </div>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          </ScrollArea>

          {/* Footer */}
          {filteredCommands.length > 0 && (
            <div className="border-t px-4 py-2 text-xs text-muted-foreground flex items-center justify-between">
              <div className="flex items-center gap-4">
                <div className="flex items-center gap-1">
                  <kbd className="inline-flex h-4 w-4 items-center justify-center rounded bg-muted text-[10px]">↑</kbd>
                  <kbd className="inline-flex h-4 w-4 items-center justify-center rounded bg-muted text-[10px]">↓</kbd>
                  <span className="ml-1">Navigate</span>
                </div>
                <div className="flex items-center gap-1">
                  <kbd className="inline-flex h-4 px-1 items-center justify-center rounded bg-muted text-[10px]">Enter</kbd>
                  <span className="ml-1">Execute</span>
                </div>
                <div className="flex items-center gap-1">
                  <kbd className="inline-flex h-4 px-1 items-center justify-center rounded bg-muted text-[10px]">Esc</kbd>
                  <span className="ml-1">Close</span>
                </div>
              </div>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}

export function useCommandPalette(commands: Command[]) {
  const [isOpen, setIsOpen] = useState(false)

  const open = useCallback(() => setIsOpen(true), [])
  const close = useCallback(() => setIsOpen(false), [])

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.shiftKey && e.key === 'P') {
        e.preventDefault()
        setIsOpen(true)
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [])

  return {
    isOpen,
    open,
    close,
    CommandPalette: ({ additionalCommands = [] }: { additionalCommands?: Command[] }) => (
      <CommandPalette
        isOpen={isOpen}
        onClose={close}
        commands={[...commands, ...additionalCommands]}
      />
    )
  }
}