# Command: Http server

`Http server` starts an embedded HTTP server, based on an OpenAPI-flavored spec and backed by SpecScript scripts. Use
**Http server** to quickly prototype an API.

| Input     | Supported    |
|-----------|--------------|
| Scalar    | no           |
| List      | auto-iterate |
| Object    | yes          |
| Raw input | yes          |

[Http server.schema.yaml](schema/Http%20server.schema.yaml)

## Basic usage

Set up an HTTP server by defining a name and endpoints.

The following example defines an HTTP `GET` request on path `/hello` to return the text "Hello World!".

```yaml specscript
Code example: Http server setup

Http server:
  name: hello-server
  port: 25001
  endpoints:
    /hello:
      get:
        output: Hello World!

Expected console output: Starting SpecScript Http Server 'hello-server' on port 25001

# Test the server
GET: http://localhost:25001/hello

Expected output: Hello World!
```

Let's break this down,

### Define endpoints and start server

The **name** field is the server's identifier, used to reference and stop it.

The **port** field defines the listening port, in this case 25001. If omitted, the port defaults to 3000.

You can run multiple servers simultaneously on different ports. If you call **Http server** multiple times with the same
name, the endpoints you define are added to the existing server. You can also add endpoints separately using the
**[Http endpoint](Http%20endpoint.spec.md)** command.

Then you define the **endpoints**. The format is inspired by OpenAPI definitions.

First, you define a path, in this case `/hello`. On the path, you need a define a method handler, one of `get`, `post`,
`put`, `patch` or `delete`. In this case we defined a `get`.

Then you define a handler. There are two handler types:

* `output`: returns the content below it
* `script`: runs a SpecScript script — either inline or from a file

In the above example we have a static Hello World greeting being returned by `output: Hello World!`

The server is started right away, and we can test it with a simple GET request:

```yaml specscript
GET: http://localhost:25001/hello
```

### Stop the server

Stop and remove a server by name with the `Stop http server` command:

```yaml specscript
Code example: Stop server

Stop http server: hello-server
```

This will only stop the named server, other servers will continue operating.

If the server is still running at the end of the script, SpecScript will not exit and keep serving requests until you
stop the process -- for example by pressing `^C` from the command line.

## Supplied variables

The following variables can be used in `output` and `script`

* `${input}`: - Body or query parameters. If there is a body in the request, query parameters are ignored.
* `${request.headers}`: - Request headers
* `${request.path}`: - Full path of the request
* `${request.pathParameters}`: - Map of parameters defined in the path
* `${request.query}`: - Query string
* `${request.queryParameters}`: - Map of query parameters
* `${request.body}`: - Request body
* `${request.cookies}`: - Cookies sent by the client

## Path patterns

### Path parameters

Paths can contain parameters using `{name}` syntax. The parameter values are available via `${request.pathParameters}`.

```yaml specscript
Code example: Path parameters

Http server:
  name: param-server
  port: 25005
  endpoints:
    /users/{id}:
      get:
        output: User ${request.pathParameters.id}

GET: http://localhost:25005/users/42

Expected output: User 42

Stop http server: param-server
```

### Wildcard paths

Use `"*"` to match any path. This is useful for proxy servers or mock servers that handle all incoming requests.

```yaml specscript
Code example: Wildcard path

Http server:
  name: wildcard-server
  port: 25006
  endpoints:
    "*":
      get:
        output: You requested ${request.path}

GET: http://localhost:25006/any/path/here

Expected output: You requested /any/path/here

Stop http server: wildcard-server
```

### Using variables with `output`

Variables are resolved in the `output` handler, making it easy to echo certain parts of the request without processing

```yaml specscript
Code example: Variables in output

Http server:
  name: echo-server
  port: 25002
  endpoints:
    /echo/headers:
      get:
        output: ${request.headers}
    /greeting:
      get:
        output: Hello ${input.name}!

GET: http://localhost:25002/greeting?name=Alice

Expected output: Hello Alice!

Stop http server: echo-server
```

## Running an inline script

You can define an inline SpecScript script in the handler using the `script` property:

```yaml specscript
Code example: SpecScript script handler

Http server:
  name: greet-server
  port: 25003
  endpoints:
    /greet-all:
      post:
        script: # Inline SpecScript
          For each:
            ${name} in: ${input.names}
            Output:
              Hello ${name}!

POST:
  url: http://localhost:25003/greet-all
  body:
    names:
      - Alice
      - Bob
      - Carol

Expected output:
  - Hello Alice!
  - Hello Bob!
  - Hello Carol!

Stop http server: greet-server
```

## Running a script file

you can also reference to a SpecScript file, as an alternative to an inline script,

Suppose you have a file `greet.spec.yaml`

```yaml temp-file=greet.spec.yaml
Script info: Creates a greeting

Input schema:
  type: object
  properties:
    name:
      description: Your name

Output: Hello ${input.name}!
```

You can call this script directly from the http endpoint definition by passing to the `script` property. Just pass it as
a string, and SpecScript will resolve it as a path relative to the file where the server is defined. Query and body
parameters are passed as input to the script.

```yaml specscript
Code example: Script file handler

Http server:
  name: file-server
  port: 25004
  endpoints:
    /greet:
      get:
        script: greet.spec.yaml   # Call script in the same directory

GET: http://localhost:25004/greet?name=Alice

Expected output: Hello Alice!

Stop http server: file-server
```
