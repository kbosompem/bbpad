import React, { useState, useMemo } from 'react'
import { cn } from '@/lib/utils'
import { ChevronRight, ChevronDown, Copy, Maximize2, Minimize2 } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface CljDumpProps {
  data: any
  className?: string
}

type DataType = 'map' | 'vector' | 'set' | 'list' | 'string' | 'number' | 'boolean' | 'nil' | 'keyword' | 'symbol' | 'unknown'

interface ParsedData {
  type: DataType
  value: any
  size?: number
  items?: Array<{ key?: any; value: any }>
}

function detectType(value: any): DataType {
  if (value === null || value === undefined) return 'nil'
  if (typeof value === 'string') {
    if (value.startsWith(':')) return 'keyword'
    if (value.startsWith("'")) return 'symbol'
    return 'string'
  }
  if (typeof value === 'number') return 'number'
  if (typeof value === 'boolean') return 'boolean'
  if (Array.isArray(value)) {
    // Check if it's a vector of uniform maps (table data)
    if (value.length > 0 && value.every((item: any) => typeof item === 'object' && !Array.isArray(item))) {
      const firstKeys = Object.keys(value[0]).sort().join(',')
      const isTable = value.every((item: any) =>
        Object.keys(item).sort().join(',') === firstKeys
      )
      if (isTable) return 'vector' // We'll handle table rendering in the component
    }
    return 'vector'
  }
  if (typeof value === 'object') {
    if (value._type === 'set') return 'set'
    if (value._type === 'list') return 'list'
    return 'map'
  }
  return 'unknown'
}

function parseValue(value: any): ParsedData {
  const type = detectType(value)

  switch (type) {
    case 'map':
      const entries = Object.entries(value).filter(([k]) => !k.startsWith('_'))
      return {
        type,
        value,
        size: entries.length,
        items: entries.map(([k, v]) => ({ key: k, value: v }))
      }
    case 'vector':
    case 'list':
      return {
        type,
        value,
        size: value.length,
        items: value.map((v: any, i: number) => ({ key: i, value: v }))
      }
    case 'set':
      const setItems = value.items || value
      return {
        type,
        value,
        size: Array.isArray(setItems) ? setItems.length : 0,
        items: Array.isArray(setItems) ? setItems.map((v: any) => ({ value: v })) : []
      }
    default:
      return { type, value }
  }
}

const typeColors = {
  map: 'bg-blue-500/10 border-blue-500/20',
  vector: 'bg-green-500/10 border-green-500/20',
  set: 'bg-yellow-500/10 border-yellow-500/20',
  list: 'bg-purple-500/10 border-purple-500/20',
  string: 'bg-background',
  number: 'bg-muted/30',
  boolean: 'text-orange-600 dark:text-orange-400',
  nil: 'text-destructive italic',
  keyword: 'text-purple-600 dark:text-purple-400',
  symbol: 'text-blue-600 dark:text-blue-400',
  unknown: 'bg-muted/50'
}

const typeLabels = {
  map: 'Map',
  vector: 'Vector',
  set: 'Set',
  list: 'List',
  string: 'String',
  number: 'Number',
  boolean: 'Boolean',
  nil: 'nil',
  keyword: 'Keyword',
  symbol: 'Symbol',
  unknown: 'Unknown'
}

interface DumpNodeProps {
  data: ParsedData
  depth?: number
  path?: string
  expandAll?: boolean
}

function DumpNode({ data, depth = 0, path = '', expandAll = false }: DumpNodeProps) {
  const [isExpanded, setIsExpanded] = useState(expandAll || depth === 0)

  const handleCopy = async (value: any) => {
    try {
      const text = typeof value === 'object' ? JSON.stringify(value, null, 2) : String(value)
      await navigator.clipboard.writeText(text)
    } catch (err) {
      console.error('Failed to copy:', err)
    }
  }

  const renderPrimitive = (value: any, type: DataType) => {
    const displayValue = type === 'string' ? `"${value}"` :
                        type === 'nil' ? 'nil' :
                        String(value)

    return (
      <span className={cn('font-mono text-sm', typeColors[type])}>
        {displayValue}
      </span>
    )
  }

  // Check if this is a table (vector of uniform maps)
  const isTableData = () => {
    if (data.type !== 'vector' || !data.items || data.items.length === 0) return false

    const firstItem = data.items[0].value
    if (typeof firstItem !== 'object' || Array.isArray(firstItem)) return false

    const firstKeys = Object.keys(firstItem).sort().join(',')
    return data.items.every((item) => {
      const val = item.value
      return typeof val === 'object' && !Array.isArray(val) &&
             Object.keys(val).sort().join(',') === firstKeys
    })
  }

  const renderTable = () => {
    if (!data.items || data.items.length === 0) return null

    const columns = Object.keys(data.items[0].value)
    const typeLabel = `Table (${data.size || 0} rows)`

    return (
      <div className="w-full">
        <button
          onClick={() => setIsExpanded(!isExpanded)}
          className={cn(
            "flex items-center gap-1 px-2 py-1 w-full text-left rounded-t border",
            "bg-primary/10 border-primary/20",
            "hover:bg-primary/15 transition-colors"
          )}
        >
          {isExpanded ? <ChevronDown className="h-3 w-3" /> : <ChevronRight className="h-3 w-3" />}
          <span className="font-semibold text-xs">{typeLabel}</span>
          {path && (
            <span className="ml-auto text-xs text-muted-foreground">{path}</span>
          )}
        </button>

        {isExpanded && (
          <div className="border border-t-0 rounded-b overflow-auto">
            <table className="w-full text-sm">
              <thead className="bg-muted/50 border-b">
                <tr>
                  <th className="px-3 py-2 text-left text-xs font-semibold text-muted-foreground">#</th>
                  {columns.map((col) => (
                    <th key={col} className="px-3 py-2 text-left text-xs font-semibold text-muted-foreground">
                      {col}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {data.items.map((item, index) => {
                  const rowData = item.value
                  const isEven = index % 2 === 0
                  return (
                    <tr
                      key={index}
                      className={cn(
                        "border-b last:border-b-0",
                        isEven ? "bg-background" : "bg-muted/20"
                      )}
                    >
                      <td className="px-3 py-2 text-xs text-muted-foreground">{index}</td>
                      {columns.map((col) => (
                        <td key={col} className="px-3 py-2">
                          {(() => {
                            const val = rowData[col]
                            const parsed = parseValue(val)
                            if (['map', 'vector', 'set', 'list'].includes(parsed.type)) {
                              return <DumpNode data={parsed} depth={depth + 1} path={`${path}[${index}].${col}`} expandAll={expandAll} />
                            }
                            return renderPrimitive(parsed.value, parsed.type)
                          })()}
                        </td>
                      ))}
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    )
  }

  const renderComplexType = () => {
    const hasItems = data.items && data.items.length > 0
    const typeLabel = `${typeLabels[data.type]} (${data.size || 0} items)`

    return (
      <div className="w-full">
        <button
          onClick={() => setIsExpanded(!isExpanded)}
          className={cn(
            "flex items-center gap-1 px-2 py-1 w-full text-left rounded-t border",
            typeColors[data.type],
            "hover:bg-accent/50 transition-colors"
          )}
        >
          {hasItems && (
            isExpanded ? <ChevronDown className="h-3 w-3" /> : <ChevronRight className="h-3 w-3" />
          )}
          <span className="font-semibold text-xs">{typeLabel}</span>
          {path && (
            <span className="ml-auto text-xs text-muted-foreground">{path}</span>
          )}
        </button>

        {isExpanded && hasItems && (
          <div className="border border-t-0 rounded-b overflow-hidden">
            <table className="w-full">
              <tbody>
                {data.items!.map((item, index) => {
                  const isEven = index % 2 === 0
                  const itemPath = data.type === 'map' && item.key
                    ? `${path}${path ? '.' : ''}${item.key}`
                    : `${path}[${index}]`

                  return (
                    <tr
                      key={index}
                      className={cn(
                        "border-b last:border-b-0",
                        isEven ? "bg-muted/20" : "bg-background"
                      )}
                    >
                      {data.type === 'map' && (
                        <td className="px-3 py-2 font-mono text-sm font-semibold text-purple-600 dark:text-purple-400 w-1/3">
                          {String(item.key)}
                        </td>
                      )}
                      {(data.type === 'vector' || data.type === 'list') && (
                        <td className="px-3 py-2 text-xs text-muted-foreground w-12 text-center">
                          {index}
                        </td>
                      )}
                      <td className="px-3 py-2">
                        <div className="flex items-start gap-2">
                          <div className="flex-1">
                            {(() => {
                              const parsed = parseValue(item.value)
                              if (['map', 'vector', 'set', 'list'].includes(parsed.type)) {
                                return <DumpNode data={parsed} depth={depth + 1} path={itemPath} expandAll={expandAll} />
                              }
                              return renderPrimitive(parsed.value, parsed.type)
                            })()}
                          </div>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleCopy(item.value)}
                            className="h-6 w-6 p-0 opacity-0 hover:opacity-100 transition-opacity"
                          >
                            <Copy className="h-3 w-3" />
                          </Button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    )
  }

  // Check if this should be rendered as a table
  if (data.type === 'vector' && isTableData()) {
    return renderTable()
  }

  if (['map', 'vector', 'set', 'list'].includes(data.type)) {
    return renderComplexType()
  }

  return renderPrimitive(data.value, data.type)
}

export function CljDump({ data, className }: CljDumpProps) {
  const [expandAll, setExpandAll] = useState(false)

  const parsedData = useMemo(() => {
    try {
      // Try to parse if it's a string
      let parsed = data
      if (typeof data === 'string') {
        try {
          parsed = JSON.parse(data)
        } catch {
          // If JSON parse fails, treat as raw string
          parsed = data
        }
      }
      return parseValue(parsed)
    } catch (err) {
      console.error('Failed to parse data:', err)
      return parseValue(data)
    }
  }, [data])

  const handleCopyAll = async () => {
    try {
      const text = typeof data === 'object' ? JSON.stringify(data, null, 2) : String(data)
      await navigator.clipboard.writeText(text)
    } catch (err) {
      console.error('Failed to copy:', err)
    }
  }

  const toggleExpandAll = () => {
    setExpandAll(!expandAll)
    // Force re-render with new expand state
    window.dispatchEvent(new Event('cljdump-toggle-expand'))
  }

  return (
    <div className={cn("flex flex-col gap-2 h-full", className)}>
      <div className="flex items-center gap-2 px-2">
        <Button
          variant="outline"
          size="sm"
          onClick={toggleExpandAll}
          className="h-7 text-xs"
        >
          {expandAll ? (
            <>
              <Minimize2 className="h-3 w-3 mr-1" />
              Collapse All
            </>
          ) : (
            <>
              <Maximize2 className="h-3 w-3 mr-1" />
              Expand All
            </>
          )}
        </Button>
        <Button
          variant="outline"
          size="sm"
          onClick={handleCopyAll}
          className="h-7 text-xs"
        >
          <Copy className="h-3 w-3 mr-1" />
          Copy All
        </Button>
      </div>

      <div className="flex-1 overflow-auto px-2 pb-2">
        <div className="min-w-0">
          <DumpNode data={parsedData} expandAll={expandAll} />
        </div>
      </div>
    </div>
  )
}