# MCP Backlog Server

A Model Context Protocol (MCP) server for managing ticket backlogs and sprint planning.

## Overview

The MCP Backlog server provides tools for managing software development tickets with full CRUD operations, state
transitions, and backlog ordering. It supports the standard ticket lifecycle: "todo" → "doing" → "done".

## Server Configuration

```yaml specscript
Mcp server:
  name: mcp-backlog
  version: 1.0.0
  transport: HTTP
```

## Ticket Listing Tools

Tools for retrieving and viewing tickets with filtering and backlog management capabilities.

### List tickets

Retrieves all tickets with optional filtering by state and assignee.

```yaml specscript
Mcp tool:
  list_tickets:
    description: List all tickets with optional filtering
    inputSchema:
      type: object
      properties:
        state:
          type: string
          enum: [ all, todo, doing, done ]
          description: Filter tickets by state (use 'all' or omit for no filtering)
          default: all
        assignee:
          type: string
          description: Filter tickets by assignee (use 'all' or omit for no filtering)
          default: all
    script: list-tickets.cli
```

### Get specific ticket

Retrieves a single ticket by its unique identifier.

```yaml specscript
Mcp tool:
  get_ticket:
    description: Get a specific ticket by ID
    inputSchema:
      type: object
      properties:
        id:
          type: string
          description: Ticket ID
      required: [ id ]
    script: get-ticket.cli

```

## Ticket Management Tools

CRUD operations for creating, updating, and deleting tickets.

### Create new ticket

Creates a new ticket with the provided details. New tickets start in "todo" state.

```yaml specscript
Mcp tool:
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
          enum: [ low, medium, high ]
          description: Ticket priority level
          default: medium
        assignee:
          type: string
          description: Person assigned to this ticket
      required: [ title, description ]
    script: create-ticket.cli

```

### Update existing ticket

Updates an existing ticket's properties including state transitions.

```yaml specscript
Mcp tool:
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
          enum: [ todo, doing, done ]
          description: New ticket state
        priority:
          type: string
          enum: [ low, medium, high ]
          description: New priority level
        assignee:
          type: string
          description: New assignee
      required: [ id ]
    script: update-ticket.cli

```

### Delete ticket

Permanently removes a ticket from the backlog.

```yaml specscript
Mcp tool:
  delete_ticket:
    description: Delete a ticket by full ticket ID
    inputSchema:
      type: object
      properties:
        id:
          type: string
          description: Full ticket identifier (e.g. TICKET-007)
      required: [ id ]
    script: delete-ticket.cli
```

## Backlog Organization Tools

Tools for organizing and prioritizing tickets within the backlog.

### Move ticket position

Changes the order of a ticket in the backlog for priority management.

```yaml specscript
Mcp tool:
  move_ticket:
    description: Move a ticket in the backlog (up, down, top, bottom)
    inputSchema:
      type: object
      properties:
        id:
          type: string
          description: Ticket ID to move
        action:
          type: string
          enum: [ up, down, top, bottom ]
          description: Move action (relative or absolute positioning)
          default: up
      required: [ id ]
    script: move-ticket.cli

```

## Usage Examples

### Starting the Server

To start the MCP backlog server:

```shell ignore
cli samples/mcp-backlog/mcp-backlog-server.spec.md
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

The backlog maintains an ordered list of tickets using the `order` property. Use the `move_ticket` tool to reorder
tickets based on changing priorities or sprint planning needs.
