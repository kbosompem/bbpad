import { useState, useEffect } from 'react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Database, Plus, Trash2, TestTube, CheckCircle, XCircle, Edit2 } from 'lucide-react'

interface DatabaseConnection {
  id: string
  name: string
  type: 'postgresql' | 'mysql' | 'mssql' | 'sqlite' | 'hsqldb' | 'h2' | 'datalevin'
  host?: string
  port?: number
  database: string
  user?: string
  status: 'connected' | 'disconnected'
}

interface DatabaseConnectionsDialogProps {
  trigger?: React.ReactNode
  onConnectionAdded?: () => void
}

const defaultConnection = {
  name: '',
  type: 'postgresql' as const,
  host: 'localhost',
  port: 5432,
  database: '',
  user: '',
  password: '',
  'ssl-mode': 'prefer'
}

export function DatabaseConnectionsDialog({ trigger, onConnectionAdded }: DatabaseConnectionsDialogProps) {
  const [connections, setConnections] = useState<DatabaseConnection[]>([])
  const [isOpen, setIsOpen] = useState(false)
  const [activeTab, setActiveTab] = useState('list')
  const [newConnection, setNewConnection] = useState(defaultConnection)
  const [editingConnection, setEditingConnection] = useState<DatabaseConnection | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [testResult, setTestResult] = useState<{ success?: boolean; error?: string } | null>(null)

  useEffect(() => {
    if (isOpen) {
      loadConnections()
    }
  }, [isOpen])

  const loadConnections = async () => {
    try {
      const response = await fetch('/api/connections')
      const data = await response.json()
      if (data.success) {
        setConnections(data.connections)
      }
    } catch (error) {
      console.error('Failed to load connections:', error)
    }
  }

  const testConnection = async () => {
    setIsLoading(true)
    setTestResult(null)
    
    try {
      const response = await fetch('/api/connections/test', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ config: newConnection }),
      })
      
      const data = await response.json()
      setTestResult(data)
    } catch (error) {
      setTestResult({ error: `Failed to test connection: ${error}` })
    } finally {
      setIsLoading(false)
    }
  }

  const saveConnection = async () => {
    const connection = editingConnection || newConnection
    if (!connection.name || !connection.database) {
      return
    }

    setIsLoading(true)
    
    try {
      const url = editingConnection ? 
        '/api/connections/update' : 
        '/api/connections'
      
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(editingConnection ? 
          { id: editingConnection.id, config: connection } :
          { config: connection }
        ),
      })
      
      const data = await response.json()
      if (data.success) {
        setNewConnection(defaultConnection)
        setEditingConnection(null)
        setTestResult(null)
        setActiveTab('list')
        loadConnections()
        onConnectionAdded?.()
      }
    } catch (error) {
      console.error('Failed to save connection:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const editConnection = (connection: DatabaseConnection) => {
    setEditingConnection(connection)
    setNewConnection({
      name: connection.name,
      type: connection.type,
      host: connection.host || 'localhost',
      port: connection.port,
      database: connection.database,
      user: connection.user || '',
      password: '',
      'ssl-mode': 'prefer'
    })
    setTestResult(null)
    setActiveTab('new')
  }

  const removeConnection = async (id: string) => {
    try {
      const response = await fetch('/api/connections/remove', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ id }),
      })
      
      if (response.ok) {
        loadConnections()
      }
    } catch (error) {
      console.error('Failed to remove connection:', error)
    }
  }

  const getDefaultPort = (type: string) => {
    switch (type) {
      case 'postgresql': return 5432
      case 'mysql': return 3306
      case 'mssql': return 1433
      case 'sqlite': return undefined
      case 'hsqldb': return undefined
      case 'h2': return undefined
      case 'datalevin': return undefined
      default: return 5432
    }
  }

  const handleTypeChange = (type: string) => {
    setNewConnection({
      ...newConnection,
      type: type as any,
      port: getDefaultPort(type)
    })
  }

  return (
    <Dialog open={isOpen} onOpenChange={setIsOpen}>
      <DialogTrigger asChild>
        {trigger || (
          <Button variant="outline" size="sm">
            <Database className="h-4 w-4 mr-2" />
            Connections
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Database Connections</DialogTitle>
        </DialogHeader>

        <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="list">Connections</TabsTrigger>
            <TabsTrigger value="new">Add New</TabsTrigger>
          </TabsList>
          
          <TabsContent value="list" className="space-y-4">
            {connections.length === 0 ? (
              <div className="text-center py-8 text-muted-foreground">
                <Database className="h-12 w-12 mx-auto mb-2 opacity-50" />
                <p>No database connections configured</p>
                <Button
                  variant="outline"
                  onClick={() => setActiveTab('new')}
                  className="mt-4"
                >
                  <Plus className="h-4 w-4 mr-2" />
                  Add First Connection
                </Button>
              </div>
            ) : (
              <div className="space-y-2">
                {connections.map((conn) => (
                  <div
                    key={conn.id}
                    className="flex items-center justify-between p-3 border rounded-lg hover:bg-accent"
                  >
                    <div className="flex items-center space-x-3">
                      <div className={`w-2 h-2 rounded-full ${
                        conn.status === 'connected' ? 'bg-green-500' : 'bg-red-500'
                      }`} />
                      <div>
                        <p className="font-medium">{conn.name}</p>
                        <p className="text-sm text-muted-foreground">
                          {conn.type} • {conn.database}
                          {conn.host && ` • ${conn.host}:${conn.port}`}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-1">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => editConnection(conn)}
                      >
                        <Edit2 className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => removeConnection(conn.id)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </TabsContent>

          <TabsContent value="new" className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="name">Connection Name</Label>
                <Input
                  id="name"
                  value={newConnection.name}
                  onChange={(e) => setNewConnection({ ...newConnection, name: e.target.value })}
                  placeholder="My Database"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="type">Database Type</Label>
                <select
                  id="type"
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                  value={newConnection.type}
                  onChange={(e) => handleTypeChange(e.target.value)}
                >
                  <option value="postgresql">PostgreSQL</option>
                  <option value="mysql">MySQL</option>
                  <option value="mssql">MS SQL Server</option>
                  <option value="sqlite">SQLite</option>
                  <option value="hsqldb">HSQLDB</option>
                  <option value="h2">H2</option>
                  <option value="datalevin">Datalevin</option>
                </select>
              </div>
            </div>

            {!['sqlite', 'hsqldb', 'h2', 'datalevin'].includes(newConnection.type) && (
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="host">Host</Label>
                  <Input
                    id="host"
                    value={newConnection.host}
                    onChange={(e) => setNewConnection({ ...newConnection, host: e.target.value })}
                    placeholder="localhost"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="port">Port</Label>
                  <Input
                    id="port"
                    type="number"
                    value={newConnection.port || ''}
                    onChange={(e) => setNewConnection({ ...newConnection, port: parseInt(e.target.value) || undefined })}
                    placeholder="5432"
                  />
                </div>
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="database">Database</Label>
              <Input
                id="database"
                value={newConnection.database}
                onChange={(e) => setNewConnection({ ...newConnection, database: e.target.value })}
                placeholder={
                  newConnection.type === 'sqlite' ? 'path/to/database.db' : 
                  newConnection.type === 'datalevin' ? 'path/to/datalevin-db' :
                  'database_name'
                }
              />
            </div>

            {!['sqlite', 'hsqldb', 'h2', 'datalevin'].includes(newConnection.type) && (
              <>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="user">Username</Label>
                    <Input
                      id="user"
                      value={newConnection.user}
                      onChange={(e) => setNewConnection({ ...newConnection, user: e.target.value })}
                      placeholder="username"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="password">Password</Label>
                    <Input
                      id="password"
                      type="password"
                      value={newConnection.password}
                      onChange={(e) => setNewConnection({ ...newConnection, password: e.target.value })}
                      placeholder="password"
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="ssl-mode">SSL Mode</Label>
                  <select
                    id="ssl-mode"
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={newConnection['ssl-mode']}
                    onChange={(e) => setNewConnection({ ...newConnection, 'ssl-mode': e.target.value })}
                  >
                    <option value="prefer">Prefer</option>
                    <option value="require">Require</option>
                    <option value="disable">Disable</option>
                  </select>
                </div>
              </>
            )}

            {testResult && (
              <div className={`flex items-center space-x-2 p-3 rounded-md ${
                testResult.success ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'
              }`}>
                {testResult.success ? (
                  <CheckCircle className="h-4 w-4" />
                ) : (
                  <XCircle className="h-4 w-4" />
                )}
                <span className="text-sm">
                  {testResult.success ? 'Connection successful!' : testResult.error}
                </span>
              </div>
            )}

            <div className="flex justify-between pt-4">
              <Button
                variant="outline"
                onClick={testConnection}
                disabled={isLoading || !newConnection.database}
              >
                <TestTube className="h-4 w-4 mr-2" />
                Test Connection
              </Button>
              
              <div className="space-x-2">
                <Button
                  variant="outline"
                  onClick={() => {
                    setNewConnection(defaultConnection)
                    setTestResult(null)
                    setActiveTab('list')
                  }}
                >
                  Cancel
                </Button>
                <Button
                  onClick={saveConnection}
                  disabled={isLoading || !newConnection.name || !newConnection.database || !testResult?.success}
                >
                  Save Connection
                </Button>
              </div>
            </div>
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  )
}