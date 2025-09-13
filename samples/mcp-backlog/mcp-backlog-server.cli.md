# MCP Backlog Server

A Model Context Protocol (MCP) server for managing ticket backlogs and sprint planning.

## Overview

The MCP Backlog server provides tools for managing software development tickets with full CRUD operations, state transitions, and backlog ordering. It supports the standard ticket lifecycle: "todo" → "doing" → "done".

## Server Configuration

The backlog server runs as an MCP server with the following tools:

```yaml specscript
Mcp server:
  name: mcp-backlog
  version: 1.0.0
  tools:
    list_tickets:
      description: List all tickets with optional filtering
      inputSchema:
        type: object
        properties:
          state:
            type: string
            enum: [todo, doing, done]
            description: Filter tickets by state
          assignee:
            type: string
            description: Filter tickets by assignee
        additionalProperties: false
      script:
        Output:
          - id: "TICKET-001"
            title: "Implement user authentication"
            description: "Add login and registration functionality"
            state: "todo"
            priority: "high"
            assignee: "alice"
            created_at: "2025-01-01T10:00:00Z"
            updated_at: "2025-01-01T10:00:00Z"
            order: 1
          - id: "TICKET-002"
            title: "Design dashboard mockups"
            description: "Create wireframes for main dashboard"
            state: "doing"
            priority: "medium"
            assignee: "bob"
            created_at: "2025-01-01T11:00:00Z"
            updated_at: "2025-01-02T09:00:00Z"
            order: 2

    get_ticket:
      description: Get a specific ticket by ID
      inputSchema:
        type: object
        properties:
          id:
            type: string
            description: Ticket ID
        required: [id]
        additionalProperties: false
      script:
        Output:
          id: "TICKET-001"
          title: "Implement user authentication"
          description: "Add login and registration functionality"
          state: "todo"
          priority: "high"
          assignee: "alice"
          created_at: "2025-01-01T10:00:00Z"
          updated_at: "2025-01-01T10:00:00Z"
          order: 1

    create_ticket:
      description: Create a new ticket
      inputSchema:
        type: object
        properties:
          title:
            type: string
            description: Ticket title
          description:
            type: string
            description: Detailed ticket description
          priority:
            type: string
            enum: [low, medium, high]
            description: Ticket priority level
            default: medium
          assignee:
            type: string
            description: Person assigned to this ticket
        required: [title, description]
        additionalProperties: false
      script:
        Output:
          id: "TICKET-003"
          title: "${input.title}"
          description: "${input.description}"
          state: "todo"
          priority: "${input.priority}"
          assignee: "${input.assignee}"
          created_at: "2025-01-13T09:22:00Z"
          updated_at: "2025-01-13T09:22:00Z"
          order: 3

    update_ticket:
      description: Update an existing ticket
      inputSchema:
        type: object
        properties:
          id:
            type: string
            description: Ticket ID to update
          title:
            type: string
            description: New ticket title
          description:
            type: string
            description: New ticket description
          state:
            type: string
            enum: [todo, doing, done]
            description: New ticket state
          priority:
            type: string
            enum: [low, medium, high]
            description: New priority level
          assignee:
            type: string
            description: New assignee
        required: [id]
        additionalProperties: false
      script:
        Output:
          id: "${input.id}"
          title: "${input.title}"
          description: "${input.description}"
          state: "${input.state}"
          priority: "${input.priority}"
          assignee: "${input.assignee}"
          updated_at: "2025-01-13T09:22:00Z"

    delete_ticket:
      description: Delete a ticket by ID
      inputSchema:
        type: object
        properties:
          id:
            type: string
            description: Ticket ID to delete
        required: [id]
        additionalProperties: false
      script:
        Output:
          success: true
          message: "Ticket ${input.id} deleted successfully"

    move_ticket:
      description: Change the order of a ticket in the backlog
      inputSchema:
        type: object
        properties:
          id:
            type: string
            description: Ticket ID to move
          new_order:
            type: integer
            description: New position in backlog (1-based)
        required: [id, new_order]
        additionalProperties: false
      script:
        Output:
          id: "${input.id}"
          old_order: 2
          new_order: "${input.new_order}"
          message: "Ticket moved successfully"

    get_backlog:
      description: Get the complete backlog ordered by priority
      inputSchema:
        type: object
        properties:
          limit:
            type: integer
            description: Maximum number of tickets to return
            default: 50
        additionalProperties: false
      script:
        Output:
          total_tickets: 2
          tickets:
            - id: "TICKET-001"
              title: "Implement user authentication"
              state: "todo"
              priority: "high"
              assignee: "alice"
              order: 1
            - id: "TICKET-002"
              title: "Design dashboard mockups"
              state: "doing"
              priority: "medium"
              assignee: "bob"
              order: 2
```

## Usage Examples

### Starting the Server

To start the MCP backlog server:

```shell cli
cli samples/mcp-backlog/mcp-backlog-server.cli.md
```

The server will start and wait for MCP client connections.

### Ticket Data Model

Each ticket contains the following properties:

- **id**: Unique ticket identifier (string)
- **title**: Brief ticket summary (string, required)
- **description**: Detailed ticket description (string, required)
- **state**: Current state - "todo", "doing", or "done" (string)
- **priority**: Priority level - "low", "medium", or "high" (string)
- **assignee**: Person assigned to the ticket (string, optional)
- **created_at**: Timestamp when ticket was created (ISO 8601 string)
- **updated_at**: Timestamp when ticket was last modified (ISO 8601 string)
- **order**: Position in backlog for ordering (integer)

### State Transitions

Tickets follow the standard development workflow:

1. **todo**: Ticket is ready for work but not started
2. **doing**: Ticket is currently being worked on
3. **done**: Ticket has been completed

State transitions can be made using the `update_ticket` tool by changing the `state` property.

### Backlog Management

The backlog maintains an ordered list of tickets using the `order` property. Use the `move_ticket` tool to reorder tickets based on changing priorities or sprint planning needs.