# Command: Script info

`Script info` contains the description of a script and the definition of the input.

| Content type | Supported |
|--------------|-----------|
| Value        | yes       |
| List         | no        |
| Object       | yes       |

[Script info.schema.yaml](schema/Script%20info.schema.yaml)

## Basic usage

With **Script info** you give a script a description.

There should be only one **Script info** command in a file, and it should be in top, so you can easily read it when
opening the file.

The simplest way of using **Script info** takes only text.

```yaml specscript
Code example: Basic Script info

Script info: A script containing a code example

# Do some stuff
```

When running SpecScript from the command line with the `cli` command, this is the description that is given. For
example, when listing the commands in a directory

```
$ cli script-info-samples 
Script info usage examples

* Available commands: 
 > basic   A script containing a code example
```

You can also put the description in the `description` property:

```yaml specscript
Code example: Script info with description property

Script info:
  description: A script containing a code example
```

You will need this when specifying input parameters, see below.

## Hidden commands

When invoking SpecScript interactively, `cli --help` will show the contents of the directory as commands. If you don't
want to expose a script this way, for example a helper script, then you can hide it with the `hide` property in **Script
info**.

For example, consider the file `helper.spec.yaml`:

```yaml file=helper.spec.yaml
Script info:
  description: Helper script
  hidden: true

Output: Something useful
```

It is not included in the directory listing:

```shell cli
spec --help .
```

```output
No commands available.
```

## SpecScript version

You can indicate the version of the SpecScript spec that the script is using.

```yaml specscript
Script info:
  specscript-version: 0.5.1
```
