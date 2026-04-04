# Command: Connect to

By using `Connect to`, setting up a connection to an Http Endpoint is simplified by moving the connection logic behind
the scenes.

| Input  | Supported    |
|--------|--------------|
| Value  | yes          |
| List   | auto-iterate |
| Object | no           |

[Connect to.schema.yaml](schema/Connect%20to.schema.yaml)

## Basic usage

**Connect to** takes a symbolic name and usually
configures [Http request defaults](../http/Http%20request%20defaults.spec.md) for subsequent REST API calls.

A script `get-items.spec.yaml` would look like this:

<!-- yaml specscript
Http request defaults:
  url: http://localhost:2525
-->

```yaml temp-file=get-items.spec.yaml
Code example: Use a connection

Connect to: SpecScript Samples

GET: /items

Expected output:
  - 1
  - 2
  - 3
```

In order for this to work, you need to configure a _connection script_ for the **SpecScript Samples** endpoint. You do
this in the `specscript-config.yaml` file in the same directory:

```yaml temp-file=specscript-config.yaml
connections:
  SpecScript Samples: connect.spec.yaml
```

The connect script `connect.spec.yaml` will be responsible for selecting the account. This way the main script does not
need to know the user credentials and other connection logic.

Here's an example connection script:

```yaml temp-file=connect.spec.yaml
# Set up endpoint for subsequent HTTP calls
Http request defaults:
  url: http://localhost:2525
```

Now we can run it

```cli cd=${SCRIPT_TEMP_DIR}
spec get-items
```

and it will output:

```output
- 1
- 2
- 3
```

## More examples

This is a very simple example, but you can put more in this script. For example, managing user credentials, obtaining a
session token, etc. See the [samples](../../../../samples) directory for some real world examples, for example on how to
connect to Spotify.

## Upward directory search

When a connection name is not found in the script's own directory, SpecScript searches `specscript-config.yaml` files in
parent directories until a match is found. The closest definition wins — a subdirectory can override a parent's
connection.

This means you only need one `specscript-config.yaml` at the project root instead of duplicating it in every
subdirectory.

## Connection inheritance

When a script runs another script, connections from the outer script are available in the inner script. If both define
the same connection name, the first one wins — the outer script's definition takes precedence.

Given a script `inner-script.spec.yaml` that connects to an endpoint:

```yaml temp-file=inner-script.spec.yaml
Connect to: SpecScript Samples

GET: /items

Output: ${output}
```

```yaml specscript
Code example: Connection inheritance when calling a script

Connect to: SpecScript Samples

Run: inner-script.spec.yaml

Expected output:
  - 1
  - 2
  - 3
```
