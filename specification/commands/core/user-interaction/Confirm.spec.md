# Command: Confirm

`Confirm` asks the user for confirmation on a single topic

| Input  | Supported |
|--------|-----------|
| Value  | yes       |
| List   | no        |
| Object | no        |

[Confirm.schema.yaml](schema/Confirm.schema.yaml)

## Basic usage

With **Confirmation**, you can ask the user a yes/no question.

<!-- answers
Do you want to continue?: "Yes"
-->

Suppose you have the file `confirm-this.spec.yaml`:

```yaml temp-file=confirm-this.spec.yaml
Code example: Simple confirmation message

Confirm: Do you want to continue?

Print: Thank you for confirming!
```

You have to run it in interactive mode to get the confirmation prompt. Use the `--interactive` flag or `-i` to enable
interactive mode:

```FIXME shell cli cd=${SCRIPT_TEMP_DIR} # hangs
spec --interactive confirm-this.spec.yaml
````

This will ask for user input on the command line:

```FIXME output
? Do you want to continue? 
 ❯ ◉ Yes
   ◯ No

Thank you for confirming!
```

## Handling rejection

When a user says no, the **Confirm** command will raise on error. You can catch this error with an `On error` block.

<!-- answers
Are you sure?: "No"
-->

Suppose you have the file `not-so-sure.spec.yaml`:

```yaml temp-file=not-so-sure.spec.yaml
Code example: Not confirmed

Confirm: Are you sure?

On error:
  Exit: Not confirmed

Expected output: Script will not reach this point
```

Here's how to run it:

```FIXME shell cli cd=${SCRIPT_TEMP_DIR}
spec --interactive not-so-sure.spec.yaml
``` 

And the output will be

```FIXME output
Not confirmed
```

# Non-interactive mode

When running in non-interactive mode (the default), **Confirm** commands are skipped silently.

Suppose you have the file `headless.cli`:

```yaml temp-file=headless.cli
Confirm: Are you there?

Print: Thank you for NOT confirming!
```

If you run it headless you will not get the confirmation prompt.

```FIXME shell cli cd=${SCRIPT_TEMP_DIR}
spec headless.cli
```

will print

```FIXME output
Thank you for NOT confirming!
```
