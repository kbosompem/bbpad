import React, { useState, useEffect } from 'react'
import { Button } from '@/components/ui/button'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Badge } from '@/components/ui/badge'
import { 
  Database, 
  Plus, 
  Trash2, 
  TestTube,
  Circle,
  ChevronRight,
  ChevronDown,
  Table
} from 'lucide-react'
import { DatabaseConnectionsDialog } from './DatabaseConnectionsDialog'

interface Connection {
  id: string
  name?: string
  type: string
  host?: string
  port?: number
  database: string
  user?: string
  status: 'connected' | 'disconnected' | 'error'
}

interface Table {
  table_name: string
  table_type: string
}

interface ConnectionsPanelProps {
  className?: string
}

export function ConnectionsPanel({ className = "" }: ConnectionsPanelProps) {
  const [connections, setConnections] = useState<Connection[]>([])
  const [expandedConnections, setExpandedConnections] = useState<Set<string>>(new Set())
  const [connectionTables, setConnectionTables] = useState<Record<string, Table[]>>({})
  const [loading, setLoading] = useState(true)

  const loadConnections = async () => {
    try {
      const response = await fetch('/api/connections')
      const data = await response.json()
      if (data.success) {
        setConnections(data.connections || [])
      }
    } catch (error) {
      console.error('Failed to load connections:', error)
    } finally {
      setLoading(false)
    }
  }

  const loadTables = async (connectionId: string) => {
    try {
      const response = await fetch('/api/schema', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 'connection-id': connectionId })
      })
      const data = await response.json()
      console.log('Schema response for', connectionId, ':', data)

      if (data.success) {
        if (data.stats) {
          // Datalevin stats
          setConnectionTables(prev => ({
            ...prev,
            [connectionId]: [
              { table_name: `Attributes: ${data.stats.attributes}`, table_type: 'stat' },
              { table_name: `Entities: ${data.stats.entities}`, table_type: 'stat' },
              { table_name: `Values: ${data.stats.values}`, table_type: 'stat' }
            ]
          }))
        } else if (data.tables) {
          // Regular tables
          setConnectionTables(prev => ({
            ...prev,
            [connectionId]: data.tables
          }))
        }
      } else {
        console.error('Schema error:', data.error)
        setConnectionTables(prev => ({
          ...prev,
          [connectionId]: []
        }))
      }
    } catch (error) {
      console.error('Failed to load tables:', error)
      setConnectionTables(prev => ({
        ...prev,
        [connectionId]: []
      }))
    }
  }

  const toggleConnection = (connectionId: string) => {
    const newExpanded = new Set(expandedConnections)
    if (newExpanded.has(connectionId)) {
      newExpanded.delete(connectionId)
    } else {
      newExpanded.add(connectionId)
      if (!connectionTables[connectionId]) {
        loadTables(connectionId)
      }
    }
    setExpandedConnections(newExpanded)
  }

  const removeConnection = async (connectionId: string) => {
    try {
      const response = await fetch('/api/connections/remove', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: connectionId })
      })
      const data = await response.json()
      if (data.success) {
        loadConnections()
      }
    } catch (error) {
      console.error('Failed to remove connection:', error)
    }
  }

  useEffect(() => {
    loadConnections()
  }, [])

  if (loading) {
    return (
      <div className={`flex flex-col ${className}`}>
        <div className="p-4 border-b">
          <h3 className="text-sm font-semibold text-muted-foreground">Connections</h3>
        </div>
        <div className="flex-1 flex items-center justify-center">
          <div className="text-sm text-muted-foreground">Loading...</div>
        </div>
      </div>
    )
  }

  return (
    <div className={`flex flex-col ${className}`}>
      {/* Header */}
      <div className="p-4 border-b">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-muted-foreground">Connections</h3>
          <DatabaseConnectionsDialog
            trigger={
              <Button variant="ghost" size="sm" className="h-6 w-6 p-0">
                <Plus className="h-3 w-3" />
              </Button>
            }
            onConnectionAdded={loadConnections}
          />
        </div>
      </div>

      {/* Connections List */}
      <ScrollArea className="flex-1">
        <div className="p-2 space-y-1">
          {connections.length === 0 ? (
            <div className="p-4 text-center text-sm text-muted-foreground">
              <Database className="h-8 w-8 mx-auto mb-2 opacity-50" />
              <p>No connections</p>
              <p>Click + to add one</p>
            </div>
          ) : (
            connections.map((connection) => (
              <div key={connection.id} className="space-y-1">
                {/* Connection Item */}
                <div className="flex items-center gap-2 p-2 rounded-md hover:bg-muted/50 group">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-4 w-4 p-0 hover:bg-transparent"
                    onClick={() => toggleConnection(connection.id)}
                  >
                    {expandedConnections.has(connection.id) ? (
                      <ChevronDown className="h-3 w-3" />
                    ) : (
                      <ChevronRight className="h-3 w-3" />
                    )}
                  </Button>
                  
                  <Database className="h-4 w-4 text-muted-foreground" />
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium truncate">
                        {connection.name || connection.id}
                      </span>
                      <Circle 
                        className={`h-2 w-2 fill-current ${
                          connection.status === 'connected' ? 'text-green-500' :
                          connection.status === 'error' ? 'text-red-500' : 'text-yellow-500'
                        }`}
                      />
                    </div>
                    <div className="text-xs text-muted-foreground truncate">
                      {connection.type} • {connection.database}
                    </div>
                  </div>

                  <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-6 w-6 p-0"
                      onClick={() => removeConnection(connection.id)}
                    >
                      <Trash2 className="h-3 w-3" />
                    </Button>
                  </div>
                </div>

                {/* Tables List */}
                {expandedConnections.has(connection.id) && (
                  <div className="ml-6 space-y-1">
                    {connectionTables[connection.id]?.length > 0 ? (
                      connectionTables[connection.id].map((table) => (
                        <div 
                          key={table.table_name}
                          className="flex items-center gap-2 p-1.5 pl-4 rounded-sm hover:bg-muted/30 cursor-pointer"
                          onClick={() => {
                            // Insert table name into the current script
                            const event = new CustomEvent('insertText', {
                              detail: { text: table.table_name }
                            })
                            window.dispatchEvent(event)
                          }}
                        >
                          <Table className="h-3 w-3 text-muted-foreground" />
                          <span className="text-xs text-muted-foreground font-mono">
                            {table.table_name}
                          </span>
                          <Badge variant="outline" className="text-xs h-4 px-1">
                            {table.table_type === 'BASE TABLE' ? 'table' : table.table_type.toLowerCase()}
                          </Badge>
                        </div>
                      ))
                    ) : (
                      <div className="p-2 pl-4 text-xs text-muted-foreground">
                        Loading tables...
                      </div>
                    )}
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </ScrollArea>
    </div>
  )
}