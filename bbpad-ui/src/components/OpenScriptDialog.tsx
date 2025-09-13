import { useState, useEffect } from 'react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Badge } from '@/components/ui/badge'
import { FolderOpen, FileText, Clock, Hash, Trash2 } from 'lucide-react'
import { ToolbarButton } from '@/components/ui/toolbar'

interface Script {
  id: string
  name: string
  language: string
  tags?: string
  created_at: string
  updated_at: string
  last_run_at?: string
  run_count: number
}

interface OpenScriptDialogProps {
  onOpen: (scriptId: string) => Promise<void>
}

export function OpenScriptDialog({ onOpen }: OpenScriptDialogProps) {
  const [open, setOpen] = useState(false)
  const [scripts, setScripts] = useState<Script[]>([])
  const [selectedScript, setSelectedScript] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [isOpening, setIsOpening] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  useEffect(() => {
    if (open) {
      loadScripts()
    }
  }, [open])

  const loadScripts = async () => {
    setIsLoading(true)
    try {
      const response = await fetch('/api/scripts/list')
      const data = await response.json()
      if (data.success) {
        setScripts(data.scripts || [])
      }
    } catch (error) {
      console.error('Failed to load scripts:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const handleOpen = async () => {
    if (!selectedScript) return

    setIsOpening(true)
    try {
      await onOpen(selectedScript)
      setOpen(false)
      setSelectedScript(null)
    } catch (error) {
      console.error('Failed to open script:', error)
    } finally {
      setIsOpening(false)
    }
  }

  const handleDelete = async (scriptId: string, event: React.MouseEvent) => {
    event.stopPropagation() // Prevent selecting the script

    const script = scripts.find(s => s.id === scriptId)
    if (!script || !confirm(`Are you sure you want to delete "${script.name}"? This action cannot be undone.`)) {
      return
    }

    setIsDeleting(true)
    try {
      const response = await fetch('/api/scripts/delete', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ id: scriptId }),
      })

      const data = await response.json()
      if (data.success) {
        // Remove the script from the local state
        setScripts(prev => prev.filter(s => s.id !== scriptId))
        // Clear selection if the deleted script was selected
        if (selectedScript === scriptId) {
          setSelectedScript(null)
        }
      } else {
        throw new Error(data.error || 'Failed to delete script')
      }
    } catch (error) {
      console.error('Failed to delete script:', error)
      alert('Failed to delete script. Please try again.')
    } finally {
      setIsDeleting(false)
    }
  }

  const formatDate = (dateString: string) => {
    if (!dateString) return 'Never'
    const date = new Date(dateString)
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <ToolbarButton>
          <FolderOpen className="h-4 w-4 mr-1" />
          Open
        </ToolbarButton>
      </DialogTrigger>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>Open Script</DialogTitle>
          <DialogDescription>
            Select a script to open from your saved scripts library.
          </DialogDescription>
        </DialogHeader>
        
        <div className="py-4">
          {isLoading ? (
            <div className="flex items-center justify-center h-[300px]">
              <p className="text-muted-foreground">Loading scripts...</p>
            </div>
          ) : scripts.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-[300px] text-center">
              <FileText className="h-12 w-12 text-muted-foreground mb-3" />
              <p className="text-muted-foreground">No saved scripts found</p>
              <p className="text-sm text-muted-foreground mt-1">
                Save a script first to see it here
              </p>
            </div>
          ) : (
            <ScrollArea className="h-[400px] pr-4">
              <div className="space-y-2">
                {scripts.map((script) => (
                  <div
                    key={script.id}
                    className={`p-3 rounded-lg border cursor-pointer transition-colors ${
                      selectedScript === script.id
                        ? 'border-primary bg-primary/5'
                        : 'border-border hover:bg-accent/50'
                    }`}
                    onClick={() => setSelectedScript(script.id)}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-2">
                          <FileText className="h-4 w-4 text-muted-foreground" />
                          <h4 className="font-medium">{script.name}</h4>
                          <Badge variant="outline" className="text-xs">
                            {script.language}
                          </Badge>
                        </div>
                        
                        {script.tags && (
                          <div className="flex items-center gap-1 mt-1">
                            <Hash className="h-3 w-3 text-muted-foreground" />
                            <span className="text-xs text-muted-foreground">
                              {script.tags}
                            </span>
                          </div>
                        )}
                        
                        <div className="flex items-center gap-4 mt-2 text-xs text-muted-foreground">
                          <span>Created: {formatDate(script.created_at)}</span>
                          {script.last_run_at && (
                            <span className="flex items-center gap-1">
                              <Clock className="h-3 w-3" />
                              Last run: {formatDate(script.last_run_at)}
                            </span>
                          )}
                          {script.run_count > 0 && (
                            <span>Runs: {script.run_count}</span>
                          )}
                        </div>
                      </div>

                      {/* Delete button */}
                      <div className="flex-shrink-0 ml-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          className="h-8 w-8 p-0 text-muted-foreground hover:text-destructive hover:bg-destructive/10"
                          onClick={(e) => handleDelete(script.id, e)}
                          disabled={isDeleting}
                          title="Delete script"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </ScrollArea>
          )}
        </div>
        
        <DialogFooter>
          <Button variant="outline" onClick={() => setOpen(false)}>
            Cancel
          </Button>
          <Button 
            onClick={handleOpen} 
            disabled={!selectedScript || isOpening}
          >
            {isOpening ? 'Opening...' : 'Open Script'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}