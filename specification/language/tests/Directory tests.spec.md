# Directory tests

This contains SpecScript behavior that is too boring for the main spec but should be tested.

## Empty directory

This is what happens when you run `cli` in an empty directory.

The `specscript-config.yaml` file for the `empty` directory:

```yaml file=empty/specscript-config.yaml
Script info: This is an example directory
```

There are no scripts in the `empty` directory.

```shell cli
spec --help empty
```

Will say:

```output
This is an example directory

No commands available.
```

## Imported helper scripts

Suppose you have a main script `main.spec.yaml` in the directory `main`:

```yaml file=main/main.spec.yaml
Script info: Main script

Say something: { }
```

And a helper script `helper.spec.yaml` in the `helper` directory:

```yaml file=helper/helper.spec.yaml
Output: Hello
```

You can import the say-something script by way of the `specscript-config.yaml` file in the `main` directory:

```yaml file=main/specscript-config.yaml
Script info: Main directory
imports:
  - ../helper/say-something.spec.yaml
```

Then the `helper` script will be available in the main directory, but will not show up when printing the contents

```shell cli
spec --help main
```

```output
Main directory

Available commands:
  main   Main script
```
