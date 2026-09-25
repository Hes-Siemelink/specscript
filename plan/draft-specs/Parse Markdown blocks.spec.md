# Parse Markdown blocks

**Parse Markdown blocks** will get the structural overview of a Markdown file, as SpecScript sees it.

(preamble)

## Basic usage

Feed the **Parse Markdown blocks** a Markdown file to get its structural overview.

```yaml specscript
Code example: Markdown structural overview

Parse Markdown blocks: |
  # Title

  Some text.

  ## Section 1

  More text.

Expected output:
  - type: heading
    level: 1
    text: Title
  - type: paragraph
    text: Some text.
  - type: heading
    level: 2
    text: Section 1
  - type: paragraph
    text: More text.
```

## Code blocks

Code blocks are returned as a single item.

~~~yaml specscript
Code example: Code block

Parse Markdown blocks: |
  ```yaml specscript
  Print: Hello, world!
  ```
Expected output:
  - type: code block
    info: yaml
    info tokens: specscript
    text: |
      Print: Hello, world!
~~~

## Block comments

Html comments are returned as a single item.

```yaml specscript
Code example: Block comment

Parse Markdown blocks: |
  <!-- multiline comment 
  This is a comment 
  -->

Expected output:
  - type: comment
    info: multiline
    info tokens: comment
    text: This is a comment
```

## Quotes

A quote block is returned as a single item.

```yaml specscript
Code example: Quote block
Parse Markdown blocks: |
  Here is some text

  > This is a quote
  > It has multiple lines

  Text after the quote.

Expected output:
  - type: paragraph
    text: Here is some text
  - type: quote
    text: |
      This is a quote
      It has multiple lines
  - type: paragraph
    text: Text after the quote.
```

## Lists

Lists are parsed as an array of text.

```yaml specscript
Code example: Bullet list

Parse Markdown blocks: |
  - Item 1
  - Item 2
  - Item 3

Expected output:
  - type: list
    items:
      - Item 1
      - Item 2
      - Item 3
```

For numbered list, 'ordered: true' is added.

```yaml specscript
Code example: Numbered list

Parse Markdown blocks: |
  1. Item 1
  2. Item 2
  3. Item 3

Expected output:
  - type: list
    ordered: true
    items:
      - Item 1
      - Item 2
      - Item 3
```

## Sublists

Sublists are not supported. The parser is not recursive. If you need the contents of a sublist, parse again.

```yaml specscript
Code example: Sublists

Parse Markdown blocks: |
  - Item 1
    - Subitem 1.1
    - Subitem 1.2

Expected output:
  - type: list
    items:
      - |
        Item 1
          - Subitem 1.1
          - Subitem 1.2
---
Parse Markdown blocks: ${output[0].items[0]}

Expected output:
  - type: list
    items:
      - Subitem 1.1
      - Subitem 1.2
```

Would this work

