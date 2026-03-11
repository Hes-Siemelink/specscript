# Change Task in Digital.ai Release

Change a task in Digital.ai Release. Use this tool to change the type of a task and provide it with new properties. For
example, you can change a manual task to a Jenkins build task and provide the job name and other details.

## MCP Tool Definition

```yaml specscript

Mcp tool:
  name: Change Task

  inputSchema:
    properties:

    task_id:
      type: string
      description: Full ID of the task to change. For example Applications/FolderSamplesAndTutorials/Release56424c393cb541cc92bb6eb41119f05c/Phase2437552/Task7479519

    target_type:
      type: string
      description: New task type. For example, xlrelease.Task or jenkins.Build

    task_properties:
      type: object
      description: |
        Task details as JSON object.
        The properties are defined in the synthetic.xml of the target type.

        For example, if you are changing to a Jenkins build task, you can provide the job name: 

          "task_properties": {
            "jobName": "My Job" 
          }
```

## Script

### Connect to Digital.ai Release

Our target system is Digital.ai Release.

```yaml specscript
Connect to: Digital.ai Release
```

There are two REST calls in this script. The first one changes the task type, and the second one updates the task
properties.

### Change task type

```yaml specscript
POST: /api/v1/tasks/${input.task_id}/changeType?targetType=${input.target_type}
As: ${task}
```

### Update properties

We assume we are updating a Jython plugin task and create the necessary snippet for the `pythonScript` field.

```yaml specscript
Add:
  - id: ${task.pythonScript.id}
    type: ${input.target_type}
  - ${input.task_properties}
As: ${script}
```

### Send properties

```yaml specscript
PUT:
  path: /api/v1/tasks/${input.task_id}
  body:
    id: ${input.task_id}
    type: xlrelease.CustomScriptTask
    title: ${task.title}
    pythonScript: ${script}
```