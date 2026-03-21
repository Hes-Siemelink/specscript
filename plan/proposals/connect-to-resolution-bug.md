# Connect to: connection resolution bug

## Summary

When a script is invoked via `Run script: resource:`, the `Connect to:` command inside that script resolves
the connection name using the **target script's** `.directory-info.yaml`, not the **calling script's** connection
definitions. This means a test directory cannot override a connection defined in the target script's directory.

## Reproduction

Given this directory structure:

```
goals-app/
  db/
    .directory-info.yaml       # Goal DB -> file: db/goals.db (production)
    create-db.spec.yaml        # Contains: Connect to: Goal DB
  tests/
    .directory-info.yaml       # Goal DB -> file: db/test-goals.db (test)
    create-tests.spec.yaml     # Calls: Run script: resource: ../db/create-db.spec.yaml
```

When `create-tests.spec.yaml` calls `Run script: resource: ../db/create-db.spec.yaml`:

1. The test defines `Goal DB` pointing to `db/test-goals.db`
2. `create-db.spec.yaml` runs and does `Connect to: Goal DB`
3. **Expected**: resolves `Goal DB` from the caller's context (test DB)
4. **Actual**: resolves `Goal DB` from `db/.directory-info.yaml` (production DB)

## Impact

- Tests cannot safely reuse shared DB setup scripts without risk of writing to the production database
- Workaround: duplicate the setup script in the test directory with its own `Connect to:` call

## Related path resolution issue

The `file:` path in `.directory-info.yaml` SQLite connection definitions is resolved relative to the **working
directory** (where `spec` was invoked), not the directory containing the `.directory-info.yaml` file. This is
consistent across `db/.directory-info.yaml` (which uses `file: db/goals.db` despite being in the `db/` directory
itself) but is unintuitive.

## Desired behavior

Two possible fixes (not mutually exclusive):

1. **Connection inheritance**: When calling a script via `Run script:`, the caller's connection definitions should
   take precedence over the target script's directory connections. This would allow test directories to override
   production connections.

2. **Relative path resolution**: The `file:` path in `.directory-info.yaml` should be resolved relative to the
   directory containing the `.directory-info.yaml` file, not the working directory.
