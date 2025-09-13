import React from 'react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Separator } from '@/components/ui/separator'
import { Badge } from '@/components/ui/badge'
import {
  Keyboard,
  Play,
  Save,
  FolderOpen,
  Database,
  Share,
  Settings,
  Plus,
  HelpCircle
} from 'lucide-react'

interface HelpDialogProps {
  trigger?: React.ReactNode
}

export function HelpDialog({ trigger }: HelpDialogProps) {
  const shortcuts = [
    { key: 'F5', action: 'Execute Script', icon: <Play className="h-4 w-4" /> },
    { key: 'Ctrl/Cmd + S', action: 'Save Script', icon: <Save className="h-4 w-4" /> },
    { key: 'Ctrl/Cmd + O', action: 'Open Script', icon: <FolderOpen className="h-4 w-4" /> },
    { key: 'Ctrl/Cmd + K', action: 'Command Palette', icon: <Keyboard className="h-4 w-4" /> },
  ]

  const features = [
    {
      title: 'Script Execution',
      description: 'Execute Babashka scripts with full Clojure syntax support',
      icon: <Play className="h-4 w-4" />
    },
    {
      title: 'Database Connections',
      description: 'Connect to PostgreSQL, MySQL, SQLite, and more',
      icon: <Database className="h-4 w-4" />
    },
    {
      title: 'Script Management',
      description: 'Save, organize, and share your scripts',
      icon: <FolderOpen className="h-4 w-4" />
    },
    {
      title: 'Themes & Customization',
      description: 'Multiple themes with complete editor integration',
      icon: <Settings className="h-4 w-4" />
    }
  ]

  return (
    <Dialog>
      <DialogTrigger asChild>
        {trigger || (
          <Button variant="ghost">
            <HelpCircle className="h-4 w-4 mr-2" />
            Help
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <HelpCircle className="h-5 w-5" />
            BBPad Help
          </DialogTitle>
          <DialogDescription>
            Learn how to use BBPad's features and keyboard shortcuts
          </DialogDescription>
        </DialogHeader>

        <ScrollArea className="flex-1 pr-4">
          <div className="space-y-6">
            {/* Quick Start */}
            <section>
              <h3 className="text-lg font-semibold mb-3">Quick Start</h3>
              <div className="space-y-2 text-sm text-muted-foreground">
                <p>1. <strong>Write Code:</strong> Use the editor to write Babashka/Clojure scripts</p>
                <p>2. <strong>Execute:</strong> Press F5 or click the Execute button to run your script</p>
                <p>3. <strong>Save:</strong> Save your scripts to the database for later use</p>
                <p>4. <strong>Connect:</strong> Add database connections to query data from your scripts</p>
              </div>
            </section>

            <Separator />

            {/* Keyboard Shortcuts */}
            <section>
              <h3 className="text-lg font-semibold mb-3">Keyboard Shortcuts</h3>
              <div className="space-y-2">
                {shortcuts.map((shortcut, index) => (
                  <div key={index} className="flex items-center justify-between p-2 rounded-md bg-muted/30">
                    <div className="flex items-center gap-2">
                      {shortcut.icon}
                      <span className="text-sm">{shortcut.action}</span>
                    </div>
                    <Badge variant="outline" className="font-mono text-xs">
                      {shortcut.key}
                    </Badge>
                  </div>
                ))}
              </div>
            </section>

            <Separator />

            {/* Features */}
            <section>
              <h3 className="text-lg font-semibold mb-3">Features</h3>
              <div className="grid gap-3">
                {features.map((feature, index) => (
                  <div key={index} className="p-3 rounded-md border">
                    <div className="flex items-start gap-3">
                      <div className="mt-0.5">{feature.icon}</div>
                      <div>
                        <h4 className="font-medium text-sm">{feature.title}</h4>
                        <p className="text-xs text-muted-foreground mt-1">{feature.description}</p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>

            <Separator />

            {/* Database Path & Version Info */}
            <section>
              <h3 className="text-lg font-semibold mb-3">Application Info</h3>
              <div className="space-y-2 text-sm">
                <div className="flex items-center justify-between p-2 rounded-md bg-muted/30">
                  <span className="text-muted-foreground">Version</span>
                  <Badge variant="outline">0.1.0</Badge>
                </div>
                <div className="flex items-center justify-between p-2 rounded-md bg-muted/30">
                  <span className="text-muted-foreground">Engine</span>
                  <Badge variant="outline">Babashka</Badge>
                </div>
                <div className="flex items-center justify-between p-2 rounded-md bg-muted/30">
                  <span className="text-muted-foreground">Database</span>
                  <span className="font-mono text-xs">~/Library/Application Support/BBPad/bbpad.db</span>
                </div>
              </div>
            </section>

            <Separator />

            {/* Support */}
            <section>
              <h3 className="text-lg font-semibold mb-3">Support</h3>
              <div className="text-sm text-muted-foreground space-y-2">
                <p>For help and documentation, visit:</p>
                <div className="p-2 rounded-md bg-muted/30 font-mono text-xs">
                  https://github.com/kbosompem/bbpad
                </div>
              </div>
            </section>
          </div>
        </ScrollArea>
      </DialogContent>
    </Dialog>
  )
}