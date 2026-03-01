# Goals App MCP Server

A Model Context Protocol (MCP) server for Goals App, a tool for managing personal and professional goals.

## Overview

The Goals App MCP Server provides tools for managing goals with CRUD operations and state transitions. It supports the
basic lifecycle: "todo" → "in_progress" → "completed".

## Server Configuration

```yaml specscript
Mcp server:
  name: goals-app
  version: 1.0.0
  transport: SSE
```

## Listing Tools

Tools for retrieving and viewing goals

### List goals

Retrieves all goals with optional filtering by state and assignee.

```yaml specscript
Mcp tool:
  list_goals:
    description: List all goals with optional filtering
    inputSchema:
      type: object
      properties:
        state:
          type: string
          enum: [ all, todo, in_progress, completed ]
          description: Filter goals by state (use 'all' or omit for no filtering)
          default: all
        assignee:
          type: string
          description: Filter goals by assignee (use 'all' or omit for no filtering)
          default: all
    script: goals/list.spec.yaml
```

### Get specific goal

Retrieves a single goal by its unique identifier.

```yaml specscript
Mcp tool:
  get_goal:
    description: Get a specific goal by ID
    inputSchema:
      type: object
      properties:
        id:
          type: integer
          description: Goal ID
      required: [ id ]
    script: goals/get.spec.yaml

```

## Goal CRUD

CRUD operations for creating, updating, and deleting goals.

### Create new goal

Creates a new goal with the provided details. New goals start in "todo" state.

```yaml specscript
Mcp tool:
  create_goal:
    description: Create a new goal
    inputSchema:
      type: object
      properties:
        title:
          type: string
          description: Goal title
        description:
          type: string
          description: Goal description
        priority:
          type: string
          enum: [ low, medium, high ]
          description: Goal priority level
          default: medium
        assignee:
          type: string
          description: Person assigned to this goal
      required: [ title, description ]
    script: goals/create.spec.yaml
```

### Create a batch of goals

You can create multiple goals at once using the **create_goal_batch** tool. Simply provide a list of goals according to
the schema defined in the `create_goal` tool.

```yaml specscript
Mcp tool:
  create_goal_batch:
    description: |
      You can create multiple goals at once.

      Important: don't pass an array directly, since it is probable that it will confuse either agent client or server. Instead, wrap the array in an object with a property named `batch`.

      The items of the array in the `batch` property must follow the same schema as defined in the `create_goal` tool.

    inputSchema:
      type: object
      properties:
        batch:
          type: array
          items:
            type: object
          description: Goal title
      required:
        - batch
    script: goals/create-batch.spec.yaml
```

### Update existing goal

Updates an existing goal's properties including state transitions.

```yaml specscript
Mcp tool:
  update_goal:
    description: Update an existing goal
    inputSchema:
      type: object
      properties:
        id:
          type: integer
          description: Goal ID to update
        title:
          type: string
          description: New goal title
        description:
          type: string
          description: New goal description
        state:
          type: string
          enum: [ todo, in_progress, completed ]
          description: New goal state
        priority:
          type: string
          enum: [ low, medium, high ]
          description: New priority level
        assignee:
          type: string
          description: New assignee
      required: [ id ]
    script: goals/update.spec.yaml

```

### Delete goal

Permanently removes a goal.

```yaml specscript
Mcp tool:
  delete_goal:
    description: Delete a goal by ID
    inputSchema:
      type: object
      properties:
        id:
          type: integer
          description: Goal identifier
      required: [ id ]
    script: goals/delete.spec.yaml
```

## Usage Examples

### Starting the server

To start the Goals App MCP server:

```shell ignore
spec samples/goals-app/mcp-server.spec.md
```

The server will start and wait for MCP client connections.

### Goal data model

Each goal contains the following properties:

- **id**: Unique goal identifier (integer, auto-generated)
- **title**: Brief goal summary (string, required)
- **description**: Detailed goal description (string, required)
- **state**: Current state - "todo", "in_progress", or "completed" (string)
- **priority**: Priority level - "low", "medium", or "high" (string)
- **assignee**: Person assigned to the goal (string, optional)
- **created_at**: Timestamp when goal was created (ISO 8601 string)
- **updated_at**: Timestamp when goal was last modified (ISO 8601 string)

### State transitions

Goals follow this workflow:

1. **todo**: Goal is ready for work but not started
2. **in_progress**: Goal is currently being worked on
3. **completed**: Goal has been completed

State transitions can be made using the `update_goal` tool by changing the `state` property.

