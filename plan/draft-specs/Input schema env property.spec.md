# Input Schema: `env` Property

The `env` property on Input schema parameters declares which environment variable provides the value. This creates a
clean resolution chain that works in both interactive and CI/CD environments.

## Resolution order

When an Input schema parameter is resolved, the following sources are checked in order:

1. **Existing value** -- passed as input to the script
2. **Environment variable** -- from `env:` property, if set
3. **Default value** -- from `default:` property
4. **Recorded test answer** -- from test fixtures
5. **Interactive prompt** -- if running in interactive mode
6. **Error** -- if the parameter is required and no value was found

## Basic usage

Declare an environment variable source with the `env` property.

<!-- yaml specscript
${input}: {}
-->

```yaml specscript
Code example: Input schema with env property

Input schema:
  type: object
  properties:
    home:
      description: Home directory
      env: HOME

Assert that:
  not:
    empty: ${home}
```

In this example, the `home` parameter reads from the `HOME` environment variable. Since `HOME` is typically set, the
parameter resolves without prompting.

## Env with default fallback

When the environment variable is not set, the default value is used.

```yaml specscript
Code example: Env with default fallback

Input schema:
  type: object
  properties:
    greeting:
      description: Greeting message
      env: SPECSCRIPT_TEST_GREETING_NOT_SET
      default: Hello

Expected output:
  greeting: Hello
```

## Env overrides default

When the environment variable IS set, it takes precedence over the default.

```yaml specscript
Code example: Env overrides default

Input schema:
  type: object
  properties:
    home:
      description: Home directory
      env: HOME
      default: /fallback

Assert that:
  not:
    item: ${home}
    equals: /fallback
```

## Explicit input overrides env

When a value is passed as input to the script, it takes precedence over the environment variable.

<!-- yaml specscript
${input}:
  name: Alice
-->

```yaml specscript
Code example: Explicit input overrides env

Input schema:
  type: object
  properties:
    name:
      description: Your name
      env: USER

Assert equals:
  actual: ${name}
  expected: Alice
```

## Real-world example: CI-friendly connection script

A connection script that works both interactively and in CI:

```yaml
# connect.spec.yaml -- not executable, illustrative
Input schema:
  type: object
  properties:
    url:
      description: API base URL
      env: DAI_PLATFORM_URL
      default: https://api.us.digitalai.cloud
    token:
      description: Bearer token
      env: DAI_PLATFORM_TOKEN
    username:
      description: Username
      env: DAI_PLATFORM_USERNAME
    password:
      description: Password
      env: DAI_PLATFORM_PASSWORD
      secret: true

Http request defaults:
  url: ${url}
  headers:
    Authorization: Token ${token}
```

Running locally (interactive, prompted for missing values):

```bash
spec deploy.spec.yaml
```

Running in CI (all values from env vars, no prompts):

```bash
DAI_PLATFORM_URL=https://api.staging.digitalai.cloud \
DAI_PLATFORM_TOKEN=xxx \
spec deploy.spec.yaml
```

## Relation to ${env}

The `env:` property on Input schema is a structured declaration. You can also access environment variables directly
via `${env.VAR_NAME}` in any context. The difference:

- `env:` on Input schema: declares a dependency, participates in the resolution chain, self-documents the script
- `${env.VAR_NAME}`: direct access, useful in string interpolation and ad-hoc usage

## Schema changes

The `ParameterData` schema adds the optional `env` property:

```yaml
# Addition to ParameterData.schema.yaml
"env":
  type: string
```
