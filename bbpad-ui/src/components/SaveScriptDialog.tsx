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
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Save } from 'lucide-react'
import { ToolbarButton } from '@/components/ui/toolbar'

interface SaveScriptDialogProps {
  currentScript: {
    id: string
    name: string
    content: string
  }
  onSave: (name: string, tags?: string) => Promise<void>
  open?: boolean
  onOpenChange?: (open: boolean) => void
}

export function SaveScriptDialog({ currentScript, onSave, open: externalOpen, onOpenChange }: SaveScriptDialogProps) {
  const [internalOpen, setInternalOpen] = useState(false)
  const open = externalOpen !== undefined ? externalOpen : internalOpen
  const setOpen = onOpenChange || setInternalOpen
  const [scriptName, setScriptName] = useState(currentScript.name || '')
  const [tags, setTags] = useState('')
  const [isSaving, setIsSaving] = useState(false)

  // Update form when current script changes
  useEffect(() => {
    setScriptName(currentScript.name || '')
  }, [currentScript.name])

  const handleSave = async () => {
    if (!scriptName.trim()) return
    
    setIsSaving(true)
    try {
      await onSave(scriptName, tags)
      setOpen(false)
      setScriptName('')
      setTags('')
    } catch (error) {
      console.error('Failed to save script:', error)
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <ToolbarButton>
          <Save className="h-4 w-4 mr-1" />
          Save
        </ToolbarButton>
      </DialogTrigger>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>Save Script</DialogTitle>
          <DialogDescription>
            Save your script with a name and optional tags for easy organization.
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="name" className="text-right">
              Name
            </Label>
            <Input
              id="name"
              value={scriptName}
              onChange={(e) => setScriptName(e.target.value)}
              className="col-span-3"
              placeholder="My Script"
              autoFocus
            />
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="tags" className="text-right">
              Tags
            </Label>
            <Input
              id="tags"
              value={tags}
              onChange={(e) => setTags(e.target.value)}
              className="col-span-3"
              placeholder="database, query, utils (optional)"
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => setOpen(false)}>
            Cancel
          </Button>
          <Button 
            onClick={handleSave} 
            disabled={!scriptName.trim() || isSaving}
          >
            {isSaving ? 'Saving...' : 'Save Script'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}