# Goals MCP Server

An MCP server for managing goals.

## Overview

The Goals MCP Server provides tools for managing goals with CRUD operations and state transitions. It supports the basic
lifecycle: "todo" → "in_progress" → "completed".

To start the Goals MCP server:

```shell ignore
cd samples/goals-app
spec db create-db
spec goals-mcp-server.spec.md
```

The server can be reached on http://localhost:9041/mcp

## Server Configuration

```yaml specscript
Mcp server:
  name: goals-app
  version: 1.0.0
  transport: HTTP
  port: 9041
```

## Querying

Tools for retrieving and viewing goals

### List goals

Retrieves all goals with optional filtering by state, assignee, and parent.

```yaml specscript
Mcp tool:
  list_goals:
    description: List all goals with optional filtering
    script: goals/list.spec.yaml
```

### Get specific goal

Retrieves a single goal by its unique identifier.

```yaml specscript
Mcp tool:
  get_goal:
    description: Get a specific goal by ID
    script: goals/get.spec.yaml

```

## Goal CRUD

CRUD operations for creating, updating, and deleting goals.

### Create new goal

Creates a new goal with the provided details. New goals start in "todo" state. Optionally set a `parent_id` to create a
sub-goal.

```yaml specscript
Mcp tool:
  create_goal:
    description: Create a new goal
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

    script: goals/create-batch.spec.yaml
```

### Update existing goal

Updates an existing goal's properties including state transitions and parent assignment.

```yaml specscript
Mcp tool:
  update_goal:
    description: Update an existing goal
    script: goals/update.spec.yaml

```

### Delete goal

Permanently removes a goal. Deleting a parent goal also removes all its sub-goals.

```yaml specscript
Mcp tool:
  delete_goal:
    description: Delete a goal by ID
    script: goals/delete.spec.yaml
```

## Goal data model

Each goal contains the following properties:

- **id**: Unique goal identifier (integer, auto-generated)
- **title**: Brief goal summary (string, required)
- **description**: Detailed goal description (string, required)
- **state**: Current state - "todo", "in_progress", or "completed" (string)
- **priority**: Priority level - "low", "medium", or "high" (string)
- **assignee**: Person assigned to the goal (string, optional)
- **parent_id**: Parent goal ID for sub-goals (integer, optional, null for top-level goals)
- **created_at**: Timestamp when goal was created (ISO 8601 string)
- **updated_at**: Timestamp when goal was last modified (ISO 8601 string)

### State transitions

Goals follow this workflow:

1. **todo**: Goal is ready for work but not started
2. **in_progress**: Goal is currently being worked on
3. **completed**: Goal has been completed

State transitions can be made using the `update_goal` tool by changing the `state` property.
