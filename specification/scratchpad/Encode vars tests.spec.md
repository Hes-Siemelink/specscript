# Encode vars

Encodes incoming JSON or other input that may happen to contain the SpecScript variable syntax `${varName}`. This
prevents accidental variable substitution during processing. Or in other words, it prevents your script from blowing up
on input.

## Specification

Suppose we have this file `input.yaml`:

```yaml file=input.yaml
key: value with ${not a variable}
```

```yaml specscript
Code example: Encode variables from file

Read file: input.yaml

Encode vars:
  text: ${output}

Expected output:
  key: value with 💰{not a variable}
```

