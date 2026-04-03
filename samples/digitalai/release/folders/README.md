# Digital.ai Release — Folder Management

Manage folders in Digital.ai Release: list, move interactively, or move by ID.

## Setup

These commands require a connection to the Release server. Use the [credentials](../credentials/README.md) commands
to log in and set default credentials.

## List Folders

Show all folders with full paths:

```shell ignore
spec list
```

Output:

```
Digital.ai - Official
Digital.ai - Official/Workflows
Digital.ai - Official/Templates
...
Samples & Tutorials
```

## Move a Folder (Interactive)

Interactively select a source folder and a new parent:

```shell ignore
spec -i move
```

SpecScript will prompt you to pick the folder to move and the target parent:

```
? Select the folder you want to move Engineering/Frontend
? Select the new parent folder Operations
```

After the move, the updated folder listing is printed.

To move multiple folders in a row, use `move-forever` which loops until you exit:

```shell ignore
spec -i move-forever
```

## Move by ID (Non-interactive)

For scripting, use `move-by-id` with explicit folder IDs:

```shell ignore
spec move-by-id --source Applications/Folder123 --target Applications/Folder456
```

## Testing

The `tests/` directory contains a mock Release server and automated tests. Run them with:

```shell ignore
spec -t -p samples tests
```

The mock server replays recorded API responses from `tests/recorded-data/`, so no live Release server is needed.

## Files

| File                | Description                                           |
|---------------------|-------------------------------------------------------|
| `list.spec.yaml`    | Lists all folders with flattened paths                 |
| `move.spec.yaml`    | Interactive folder move using prompts                  |
| `move-by-id.spec.yaml` | Non-interactive move with explicit source/target IDs |
| `move-forever.spec.yaml` | Loops `move` until you exit                        |
| `flat-folder-list.spec.yaml` | Utility: recursively flattens nested folder tree |
| `raw-list.spec.yaml` | Gets the raw folder data from the API                 |
