import { cn } from '@/lib/utils'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Button } from '@/components/ui/button'
import { Copy, Download, Table, FileJson, Database } from 'lucide-react'
import { CljDump } from '@/components/CljDump'

interface ResultsPanelProps {
  result: string
  isExecuting: boolean
  className?: string
  structuredData?: any
}

export function ResultsPanel({ result, isExecuting, className, structuredData }: ResultsPanelProps) {
  const hasError = result.startsWith('❌')
  const isEmpty = !result || result === 'Press F5 or click Execute to run your Clojure script...'

  const handleCopyResult = async () => {
    try {
      await navigator.clipboard.writeText(result)
    } catch (err) {
      console.error('Failed to copy result:', err)
    }
  }

  const handleExportResult = () => {
    const blob = new Blob([result], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'bbpad-result.txt'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }

  return (
    <div className={cn("flex flex-col h-full", className)}>
      {/* Results Header */}
      <div className="flex items-center justify-between border-b px-4 py-2 bg-muted/30">
        <div className="flex items-center gap-2">
          <h3 className="font-semibold text-sm">Results</h3>
          {isExecuting && (
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <div className="w-2 h-2 bg-orange-500 rounded-full animate-pulse" />
              Executing...
            </div>
          )}
        </div>
        
        {!isEmpty && !isExecuting && (
          <div className="flex items-center gap-1">
            <Button
              variant="ghost"
              size="sm"
              onClick={handleCopyResult}
              className="h-7 px-2"
            >
              <Copy className="h-3 w-3" />
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleExportResult}
              className="h-7 px-2"
            >
              <Download className="h-3 w-3" />
            </Button>
          </div>
        )}
      </div>

      {/* Results Content */}
      <div className="flex-1 overflow-hidden">
        <Tabs defaultValue="formatted" className="h-full flex flex-col">
          <TabsList className="mx-4 mt-2 w-fit">
            <TabsTrigger value="formatted" className="text-xs">
              <Table className="h-3 w-3 mr-1" />
              Formatted
            </TabsTrigger>
            <TabsTrigger value="raw" className="text-xs">
              <FileJson className="h-3 w-3 mr-1" />
              Raw
            </TabsTrigger>
            <TabsTrigger value="cljdump" className="text-xs">
              <Database className="h-3 w-3 mr-1" />
              cljdump
            </TabsTrigger>
          </TabsList>
          
          <TabsContent value="formatted" className="flex-1 mt-2 mx-4 mb-4 overflow-hidden">
            <div className={cn(
              "h-full rounded-md border overflow-y-auto text-sm font-mono",
              hasError
                ? "bg-red-50 border-red-200 text-red-800 dark:bg-red-950/20 dark:border-red-800 dark:text-red-300"
                : isEmpty
                ? "bg-muted/30 text-muted-foreground"
                : "bg-background"
            )}>
              <pre className="p-4 whitespace-pre-wrap leading-relaxed min-h-full">
                {result || 'Press F5 or click Execute to run your Clojure script...'}
              </pre>
            </div>
          </TabsContent>

          <TabsContent value="raw" className="flex-1 mt-2 mx-4 mb-4 overflow-hidden">
            <div className="h-full rounded-md border bg-background overflow-y-auto text-sm font-mono">
              <pre className="p-4 whitespace-pre-wrap leading-relaxed text-muted-foreground min-h-full">
                {result || 'No output'}
              </pre>
            </div>
          </TabsContent>

          <TabsContent value="cljdump" className="flex-1 mt-2 mx-4 mb-4 overflow-hidden">
            <div className="h-full rounded-md border bg-background overflow-hidden">
              {structuredData ? (
                <CljDump data={structuredData} className="h-full" />
              ) : (
                <div className="p-4 text-sm text-muted-foreground">
                  No structured data available. Execute a script that returns data structures to see them here.
                </div>
              )}
            </div>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  )
}