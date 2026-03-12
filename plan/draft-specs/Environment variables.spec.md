# Environment Variables

SpecScript exposes operating system environment variables through the `${env}` namespace. This allows scripts to
read configuration from the environment, which is the standard way to inject configuration in CI/CD, Docker, and
Kubernetes environments.

## Basic usage

Access environment variables using `${env.VARIABLE_NAME}` syntax.

```yaml specscript
Code example: Read an environment variable

Assert that:
  not:
    empty: ${env.HOME}
```

## Using env in string interpolation

Environment variables can be used in string interpolation just like regular variables.

```yaml specscript
Code example: Environment variable in string

Print: Home directory is ${env.HOME}
```

## Using env in HTTP requests

Environment variables are useful for injecting API URLs and tokens in CI/CD environments.

```yaml
# Not executable -- illustrative example
Http request defaults:
  url: ${env.API_URL}
  headers:
    Authorization: Token ${env.API_TOKEN}
```

## Using env with Shell

Previously, environment variables were only accessible through Shell commands. Now you can use them directly.

Before:

```yaml
# Workaround: use Shell to read env vars
Shell: echo $API_URL
As: ${api_url}

Http request defaults:
  url: ${api_url}
```

After:

```yaml
# Direct access
Http request defaults:
  url: ${env.API_URL}
```

## Missing environment variables

Accessing an unset environment variable produces an empty value.

```yaml specscript
Code example: Unset environment variable is empty

Assert that:
  empty: ${env.THIS_VAR_CERTAINLY_DOES_NOT_EXIST_12345}
```

## Env is read-only

The `${env}` namespace is read-only. You cannot assign values to it. Use regular SpecScript variables for mutable
state.

```yaml
# This does NOT set an environment variable -- it creates a regular SpecScript variable
${env}: something
```

## Relation to Input schema

The `env:` property on Input schema parameters provides a structured way to declare environment variable dependencies.
See [Input schema](../../specification/commands/core/script-info/Input%20schema.spec.md) for details.
