import { useState } from 'react'
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
import { Share, FileDown, Copy, Check } from 'lucide-react'
import { ToolbarButton } from '@/components/ui/toolbar'

interface ShareScriptDialogProps {
  currentScript: {
    id: string
    name: string
    content: string
  }
  trigger?: React.ReactNode
}

export function ShareScriptDialog({ currentScript, trigger }: ShareScriptDialogProps) {
  const [open, setOpen] = useState(false)
  const [fileName, setFileName] = useState(currentScript.name ? `${currentScript.name}.bb` : 'script.bb')
  const [copied, setCopied] = useState(false)

  const handleDownload = () => {
    try {
      const blob = new Blob([currentScript.content], { type: 'text/plain' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = fileName
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
      setOpen(false)
    } catch (error) {
      console.error('Failed to download script:', error)
    }
  }

  const handleCopyContent = async () => {
    try {
      await navigator.clipboard.writeText(currentScript.content)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch (error) {
      console.error('Failed to copy content:', error)
    }
  }

  const handleSaveAs = async () => {
    if ('showSaveFilePicker' in window) {
      try {
        // Use the modern File System Access API if available
        const fileHandle = await (window as any).showSaveFilePicker({
          suggestedName: fileName,
          types: [
            {
              description: 'Babashka script files',
              accept: {
                'text/plain': ['.bb', '.clj'],
              },
            },
          ],
        })

        const writable = await fileHandle.createWritable()
        await writable.write(currentScript.content)
        await writable.close()
        setOpen(false)
      } catch (error) {
        if ((error as Error).name !== 'AbortError') {
          console.error('Failed to save file:', error)
          // Fall back to download
          handleDownload()
        }
      }
    } else {
      // Fall back to download for browsers that don't support File System Access API
      handleDownload()
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        {trigger || (
          <ToolbarButton>
            <Share className="h-4 w-4 mr-1" />
            Share
          </ToolbarButton>
        )}
      </DialogTrigger>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Share className="h-5 w-5" />
            Share Script
          </DialogTitle>
          <DialogDescription>
            Export your script as a Babashka (.bb) file or copy it to clipboard.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          {/* File Name Input */}
          <div className="grid gap-2">
            <Label htmlFor="filename">File Name</Label>
            <Input
              id="filename"
              value={fileName}
              onChange={(e) => setFileName(e.target.value)}
              placeholder="script.bb"
            />
          </div>

          {/* Script Preview */}
          <div className="grid gap-2">
            <Label>Script Content</Label>
            <div className="max-h-32 overflow-y-auto rounded-md border bg-muted p-3">
              <pre className="text-xs text-muted-foreground whitespace-pre-wrap">
                {currentScript.content || 'No content'}
              </pre>
            </div>
          </div>
        </div>

        <DialogFooter className="flex-col sm:flex-row gap-2">
          <div className="flex gap-2">
            <Button
              variant="outline"
              onClick={handleCopyContent}
              className="flex items-center gap-2"
            >
              {copied ? (
                <Check className="h-4 w-4" />
              ) : (
                <Copy className="h-4 w-4" />
              )}
              {copied ? 'Copied!' : 'Copy'}
            </Button>

            <Button
              variant="outline"
              onClick={handleDownload}
              className="flex items-center gap-2"
            >
              <FileDown className="h-4 w-4" />
              Download
            </Button>
          </div>

          <div className="flex gap-2">
            <Button variant="outline" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleSaveAs} className="flex items-center gap-2">
              <Share className="h-4 w-4" />
              Save As...
            </Button>
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}