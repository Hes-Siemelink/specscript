# Command: Mcp resource

`Mcp resource` defines resources for an MCP server.

| Content type | Supported |
|--------------|-----------|
| Value        | no        |
| List         | implicit  |
| Object       | yes       |

[McpResource.schema.yaml](schema/McpResource.schema.yaml)

## Basic usage

Use **Mcp resource** to define resources that can be added to an MCP server. This command is typically used in conjunction with `Mcp server` to modularize resource definitions.

```yaml specscript
Code example: MCP server without definitions

Mcp server:
  name: test-server
  version: "1.0.0"
```

Now that we have a server running, we can add resources to it:

```yaml specscript
Code example: Adding a configuration resource to an MCP server

Mcp resource:
  file-content:
    name: Configuration File
    description: Application configuration in YAML format
    script:
      Output:
        config:
          database:
            host: localhost
            port: 5432
          app:
            name: My Application
```

## Multiple resources

Add multiple resources in one command:

```yaml specscript
Code example: Multiple MCP resources

Mcp resource:
  user-data:
    name: "User Data"
    description: "Current user information"
    script:
      Output:
        id: 12345
        name: "John Doe"
        email: "john@example.com"
  system-info:
    name: "System Information"
    description: "Current system status"
    mimeType: "application/json"
    script:
      Output:
        cpu_usage: 45.2
        memory_usage: 67.8
        disk_space: 234.5
```

## Different MIME types

Resources support different content types through the optional `mimeType` property:

```yaml specscript
Code example: MCP Resources with custom MIME types

Mcp resource:
  csv-report:
    name: Sales Report
    description: Monthly sales data in CSV format
    mimeType: text/csv
    script:
      Output: |
        Date,Product,Sales
        2024-01-01,Widget A,100
        2024-01-02,Widget B,150
  xml-config:
    name: Service Configuration
    description: Service configuration in XML format
    mimeType: application/xml
    script:
      Output: |
        <?xml version="1.0"?>
        <config>
          <service>web-api</service>
          <port>8080</port>
        </config>
```

### External script files

You can reference external SpecScript files in the `script` property by providing a filename:

```yaml specscript
Code example: MCP resource referencing external script 

Mcp resource:
  database-backup:
    name: Database Backup
    description: Latest database backup information
    script: backup-info.cli
```

The external script file should contain the SpecScript commands to execute when the resource is accessed.

<!-- yaml specscript
Mcp server:
  name: test-server
  version: "1.0.0"
  stop: true
-->