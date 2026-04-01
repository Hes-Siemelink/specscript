# Digital.ai Releaser Folder commands

Moving folders within Digital.ai Release

## Connecting

These commands require a connection to the Release server. Use the commands in the [**credentials
**](../credentials/README.md) folder to login and set default credentials.

## List folders

List all folders in with the following command

```spec cli 
spec list
```

This will show the list of all folders with full paths.

```
- Digital.ai - Official
- Digital.ai - Official/Workflows
...
- Samples & Tutorials
```

## Move folder

This is an interactive command that allows you to move a folder to a new location. It will get the list of folders and
ask you to select the folder you want to move ("source") and the new parent folder ("target")

```shell ignore
spec -i move
```

Will show something like this:

```
? Select the folder you want to move Digital.ai - Official/Workflows
? Select the new parent folder Samples & Tutorials
New folder structure:

- Digital.ai - Official
- Digital.ai - Official/Templates
- Digital.ai - Official/Templates/Governance Pipeline
- Digital.ai - Official/Workflow Executions
- Samples & Tutorials
- Samples & Tutorials/Workflows
```

If you want to move several folder, use 'move-forever', which will call 'move' in a loop until you exit.

```shell ignore
spec -i move-forever
```

## Move folder by ID

Use this if you know the folder IDs and don't want an interactive prompt.

Example:

```shell ignore
spec move-by-id --source-id Applications/Folderfa48c0bf5a6c4aeda74d73b50db3bbf7 --target-id Applications/Folder46601b6f698447d380fc7541a6e14b59
```