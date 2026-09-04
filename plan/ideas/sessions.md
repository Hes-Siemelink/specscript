# Sessions for Http and MCP

We want to introduce sessions for the HTTP and MCP clients. We want the same approach for both HTTP and MCP.

Currently, a new session is opened for each call.

We have some support for default parameters in **Http requests defaults**, but there is no session management.

In some use cases it is essential to have session management. For example to maintain OAUTH login token or when
interacting with Playwright MCP.

## References

* specscript/specification/commands/core/http/Http request defaults.spec.md
* specscript/specification/commands/core/http/GET.spec.md
* specscript/specification/commands/ai/mcp/Mcp call tool.spec.md

## Sketch

Sessions are started with **Http session** and **MCP session**. The verb 'start' is missing here to make it easier to
read, the code will look more declarative. You do not need to explicitly close the session, it will be closed when the
script ends. You can also use **Http close session** and **MCP close session** if needed.

Here's what this would look like:

```yaml specscript
Code example: Http and MCP sessions

# Http session
Http session:
  name: http-session-001
  url: http://localhost:2525
  path: /items

POST:
  body:
    1: One
    2: Two
    3: Three

Http close session: http-session-001

---
# MCP session
Mcp session:
  name: worker-a
  url: http://localhost:8091/mcp
As: ${mcp_session}

Mcp call tool:
  tool: remember
  input:
    value: 42

Expected output: 42

Mcp close session: ${mcp_session}
```

Note: sessions have a name that serves as an id. If not specified, one will be generated. The result of the session
command is the used parameters. You can close the session with either the full session object or just the name.

```yaml specscript
Code example: Session objects

# Http session
Http session:
  url: http://localhost:2525
  path: /items

Expected output:
  name: http-session-001 # Note: this will be difficult to test, as the name is generated
  url: http://localhost:2525
  path: /items

---
Mcp session:
  name: worker-a
  url: http://localhost:8091/mcp

Expected output:
  name: worker-a
  url: http://localhost:8091/mcp

```

Note: small discrepancy MCP tool call takes the contents in of the session in the `server` property, while Http commands
take the contents in the root of the session object. This is because MCP tool calls is modeled like that now. Not sure
if we want to make those two commands consistent -- punt it for now and we will pick it up in second review.

## Approach

* **Http session** will supersede **Http request defaults**. It is allowed to remove support for **Http request defaults
  ** right away. Assess impact for current implementation, spec and tests to do it in one go or deprecate first and then
  remove.
* New specs for **Http session** and **MCP session**, **Http close session** and **MCP close session**
* Superficial human review
* Make everything work in Kotlin and Typescript
* Human inspects results and rewrites specs to taste
* Make everything work again

## Notes

As always, remember that the specs are the unit tests! The spec is the doc is the test. Do not forget to read relevant
skills in specscript/.agents/skills

We are in the specscript repository itself! The entire language is defined in the specification directory. I am the
author of SpecScript, so feel free to ask anything.

Also: the spec is guiding but not exhaustive. Corner cases are put in the tests directory. See
specscript/specification/commands/core/http/tests/Http client tests.spec.yaml

Also: it is crucial that MCP session maintains the mcp session id so tool calls will work with Playwright.