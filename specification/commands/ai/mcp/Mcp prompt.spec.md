# Command: Mcp prompt

`Mcp prompt` defines prompts for an MCP server.

| Content type | Supported |
|--------------|-----------|
| Value        | no        |
| List         | implicit  |
| Object       | yes       |

[Mcp prompt.schema.yaml](schema/Mcp%20prompt.schema.yaml)

## Basic usage

Use **Mcp prompt** to define prompts that can be added to an MCP server. This command is typically used in conjunction
with `Mcp server` to modularize prompt definitions.

Let's start with a basic MCP server definition:

```yaml specscript
Code example: MCP server without definitions

Mcp server:
  name: test-server
  version: "1.0.0"
```

Now that we have a server running, we can add prompts to it:

```yaml specscript
Code example: Adding a prompt to an MCP server

Mcp prompt:
  code-review:
    name: Code Review Assistant
    description: Helps review code for best practices and potential issues
    arguments:
      - name: code
        description: The code to review
        required: true
      - name: language
        description: Programming language of the code
        required: false
    script:
      Output: |
        Reviewing ${input.language} code:

        Code Analysis:
        ${input.code}

        Recommendations:
        - Consider adding error handling
        - Add documentation comments
        - Follow naming conventions
```

## Multiple prompts

Add multiple prompts in one command:

```yaml specscript
Code example: Multiple MCP prompts

Mcp prompt:
  explain-concept:
    name: Concept Explainer
    description: Explains technical concepts in simple terms
    arguments:
      - name: concept
        description: The concept to explain
        required: true
      - name: audience
        description: Target audience level (beginner, intermediate, advanced)
        required: false
    script:
      Output: |
        Explaining "${input.concept}" for ${input.audience} audience:

        Simple explanation coming soon...

  generate-tests:
    name: Test Generator
    description: Generates unit tests for given code
    arguments:
      - name: function_code
        description: The function code to generate tests for
        required: true
      - name: test_framework
        description: Testing framework to use
        required: false
    script:
      Output: |
        Generating tests using ${input.test_framework} framework:

        Test cases for:
        ${input.function_code}

        // Test implementation would go here
```

### External script files

You can reference external SpecScript files in the `script` property by providing a filename:

```yaml specscript
Code example: Prompt backed by external script

Mcp prompt:
  documentation-writer:
    name: Documentation Writer
    description: Generates comprehensive documentation
    arguments:
      - name: code_snippet
        description: Code to document
        required: true
      - name: format
        description: Documentation format (markdown, rst, etc.)
        required: false
    script: generate-docs.spec.yaml
```

The external script file should contain the SpecScript commands to execute when the prompt is invoked.

<!-- yaml specscript
Mcp server:
  name: test-server
  version: "1.0.0"
  stop: true
-->