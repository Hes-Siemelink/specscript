# Command: Stop http server

`Stop http server` stops a running HTTP server by name.

| Input      | Supported     |
|------------|---------------|
| Scalar     | yes           |
| List       | no            |
| Object     | no            |

[Stop http server.schema.yaml](schema/Stop%20http%20server.schema.yaml)

## Usage

Pass the server name as a string value to stop and remove the server:

```yaml specscript
Code example: Start and stop HTTP server

Http server:
  name: stop-demo
  port: 25001
  endpoints:
    /ping:
      get:
        output: pong

GET: http://localhost:25001/ping

Expected output: pong

Stop http server: stop-demo
```

Other servers continue operating; only the named server is stopped.

If the server is still running at the end of the script, SpecScript will not exit and keep serving requests until you
stop the process — for example by pressing `^C` from the command line.
