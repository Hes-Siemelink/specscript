# Connection Inheritance

When a script calls another script, the caller's connection definitions take precedence over the callee's. This
allows test directories to override production connections without duplicating scripts.

## The problem

Consider the following directory structure:

```
goals-app/
  db/
    .directory-info.yaml       # Goal DB -> file: db/goals.db
    create-db.spec.yaml        # Contains: Connect to: Goal DB
  tests/
    .directory-info.yaml       # Goal DB -> file: db/test-goals.db
    create-tests.spec.yaml     # Calls: Run script: resource: ../db/create-db.spec.yaml
```

Without connection inheritance, when `create-tests.spec.yaml` calls `create-db.spec.yaml`, the `Connect to: Goal DB`
inside resolves from `db/.directory-info.yaml` -- the production database. The test override is ignored.

## Caller wins

With connection inheritance, the caller's connection definitions are passed to the callee. If both define the same
connection name, the caller's definition wins.

```yaml file=.directory-info.yaml
connections:
  SpecScript Sample Server:
    Http request defaults:
      url: http://localhost:2525
```

```yaml file=get-items.spec.yaml
Connect to: SpecScript Sample Server

GET: /items

Output: ${output}
```

```yaml specscript
Code example: Connection inheritance when calling a script

Connect to: SpecScript Sample Server

Run script: get-items.spec.yaml

Expected output:
  - 1
  - 2
  - 3
```

## Test override pattern

The main use case is test directories overriding production connections. The test directory defines the same connection
name pointing to a test resource:

```yaml
# Production: goals-app/db/.directory-info.yaml
connections:
  Goal DB:
    SQLite defaults:
      file: db/goals.db
```

```yaml
# Test: goals-app/tests/.directory-info.yaml
connections:
  Goal DB:
    SQLite defaults:
      file: db/test-goals.db
```

When the test script calls a production script via `Run script:`, the test connection overrides the production one:

```yaml
# tests/create-tests.spec.yaml
# "Goal DB" here resolves to test-goals.db

Create test db: {}
# Inside create-db.spec.yaml, "Connect to: Goal DB" resolves
# from the caller's context -> test-goals.db
```

## HTTP connection override

The same pattern works for HTTP connections, useful for running against a mock server in tests:

```yaml
# Production: cloud-connector/.directory-info.yaml
connections:
  Digital.ai Platform: ../login/connect.spec.yaml

# Test: cloud-connector/tests/.directory-info.yaml
connections:
  Digital.ai Platform:
    Http request defaults:
      url: http://localhost:25101
```

Test scripts call the same production scripts, but HTTP requests go to the mock server.

## Override only applies to named connections

Connection inheritance only affects `Connect to:` resolution. It does not affect `Http request defaults:` set directly
in a script. Only connections looked up by name through `.directory-info.yaml` participate in the override chain.

## Nested calls

If script A calls script B, and B calls script C, A's connection overrides apply to both B and C. The override is
propagated through the session, not through the directory hierarchy.

```
A (defines "My DB" -> test.db)
  -> B (defines "My DB" -> prod.db, does "Connect to: My DB")
       -> resolves to test.db (A wins)
       -> C (defines "My DB" -> other.db, does "Connect to: My DB")
            -> resolves to test.db (A still wins)
```
