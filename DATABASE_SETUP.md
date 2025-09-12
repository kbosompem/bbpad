# Database Setup for BBPad

This document describes how to set up test databases for BBPad development and testing.

## Docker Test Databases

BBPad includes a Docker Compose configuration for running test databases locally. This setup includes:

- **PostgreSQL 15** (port 5432)
- **MySQL 8.0** (port 3306) 
- **MS SQL Server 2022 Express** (port 1434)

### Starting the Test Databases

```bash
# Start all test databases
docker-compose -f docker-compose.test.yml up -d

# Check status
docker-compose -f docker-compose.test.yml ps

# View logs
docker-compose -f docker-compose.test.yml logs

# Stop databases
docker-compose -f docker-compose.test.yml down
```

### Database Connection Details

#### PostgreSQL
- **Host**: localhost
- **Port**: 5432
- **Database**: bbpad_test
- **Username**: bbpad_user
- **Password**: bbpad_pass

#### MySQL
- **Host**: localhost
- **Port**: 3306
- **Database**: bbpad_test
- **Username**: bbpad_user
- **Password**: bbpad_pass

#### MS SQL Server
- **Host**: localhost
- **Port**: 1434 (mapped from container port 1433)
- **Database**: bbpad_test
- **Username**: sa
- **Password**: BBPad123!

### Sample Data

Each database includes sample tables with test data:

- `users` - 4 sample users
- `products` - 5 sample products (laptop, coffee mug, notebook, etc.)
- `orders` - 4 sample orders
- `order_items` - Order line items
- `order_summary` - View combining order and user data

### Database Initialization

The databases are automatically initialized with sample data using scripts in the `test-data/` directory:

- `test-data/postgres/01-init.sql` - PostgreSQL initialization
- `test-data/mysql/01-init.sql` - MySQL initialization  
- `test-data/mssql/01-init.sql` - MS SQL Server initialization

## Supported Database Types in BBPad

BBPad supports the following database types:

### SQL Databases
- **PostgreSQL** - Full support via JDBC
- **MySQL** - Full support via JDBC
- **MS SQL Server** - Full support via JDBC
- **SQLite** - File-based, no server required
- **H2** - In-memory or file-based
- **HSQLDB** - In-memory or file-based

### NoSQL/Graph Databases
- **Datalevin** - Local file-based datalog database
  - Shows special statistics: attributes, entities, values counts
  - No network connection required

## Connection Examples

### Creating Connections in BBPad

1. Click the "+" button in the Connections panel
2. Fill in the connection details
3. Test the connection
4. Save the connection

### Sample Connection Configurations

#### PostgreSQL (Docker)
```
Name: PostgreSQL Test
Type: postgresql
Host: localhost
Port: 5432
Database: bbpad_test
Username: bbpad_user
Password: bbpad_pass
SSL Mode: prefer
```

#### MySQL (Docker)
```
Name: MySQL Test  
Type: mysql
Host: localhost
Port: 3306
Database: bbpad_test
Username: bbpad_user
Password: bbpad_pass
```

#### MS SQL Server (Docker)
```
Name: SQL Server Test
Type: mssql
Host: localhost
Port: 1434
Database: bbpad_test
Username: sa
Password: BBPad123!
```

#### Datalevin (Local)
```
Name: Local Datalevin
Type: datalevin
Database: /path/to/datalevin-db
```

#### SQLite (Local)
```
Name: Local SQLite
Type: sqlite
Database: /path/to/database.db
```

#### H2 (Local)
```
Name: Local H2
Type: h2
Database: ~/test-db
```

## Querying Data

Once connected, you can:

1. Expand connections to see tables/schema
2. Click table names to insert them into your script
3. Write and execute SQL queries
4. View results in the Results panel

### Example Queries

```sql
-- List all users
SELECT * FROM users;

-- Get orders with user details
SELECT u.username, o.total_amount, o.status 
FROM orders o 
JOIN users u ON o.user_id = u.id;

-- Product inventory
SELECT name, category, stock_quantity 
FROM products 
ORDER BY stock_quantity DESC;
```

## Troubleshooting

### Port Conflicts
If you get port binding errors, check what's using the ports:
```bash
lsof -i :5432  # PostgreSQL
lsof -i :3306  # MySQL  
lsof -i :1434  # MS SQL Server
```

### Container Issues
```bash
# View detailed logs
docker logs bbpad-postgres-1
docker logs bbpad-mysql-1
docker logs bbpad-mssql-1

# Restart a specific database
docker-compose -f docker-compose.test.yml restart postgres
```

### Connection Issues
1. Ensure the Docker containers are running and healthy
2. Check that ports are not blocked by firewall
3. Verify connection credentials match the docker-compose.test.yml configuration
4. For MS SQL Server, wait a minute after startup for initialization to complete