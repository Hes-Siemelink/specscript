# Encode vars

Encodes incoming JSON or other input that may happen to contain the SpecScript variable syntax `${varName}`. This
prevents accidental variable substitution during processing. Or in other words, it prevents your script from blowing up
on input.

## Specification

```yaml specscript
Script info: Encode variables

Input parameters:
  text: The text to sanitize
```

We use a simple encoding scheme where `${` is replaced with `💰{` to escape the variable syntax.

```yaml specscript
Replace:
  text: ${
  in: ${text}
  with: 💰{
```

## Code examples

## Tests
