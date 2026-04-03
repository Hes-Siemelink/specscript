# Command: SQLite

`SQLite` executes SQL statements against a SQLite database file.

| Input      | Supported     |
|------------|---------------|
| Scalar     | no            |
| List       | no            |
| Object     | yes           |

## Basic usage

Create a table, insert data, and query it:

```yaml specscript
Code example: Create and query a SQLite database

Shell: rm -f out/spec-sqlite.db

SQLite:
  file: out/spec-sqlite.db
  update:
    - create table users (id integer primary key, name text)
    - insert into users (name) values ('Alice')
  query: select * from users

Expected output:
  - id: 1
    name: Alice
```

## Update without query

When only `update` is provided, the command executes the statements and produces no output.

```yaml specscript
Code example: Update only

Shell: rm -f out/spec-sqlite.db

SQLite:
  file: out/spec-sqlite.db
  update:
    - create table users (id integer primary key, name text)
    - insert into users (name) values ('Alice')
    - insert into users (name) values ('Bob')

---

SQLite:
  file: out/spec-sqlite.db
  query: select name from users

Expected output:
  - name: Alice
  - name: Bob
```

## Prepared statement parameters

Variable references wrapped in single quotes are automatically converted to prepared statement parameters. This
prevents SQL injection without manual escaping.

```yaml specscript
Code example: Prepared statement parameters

Shell: rm -f out/spec-sqlite.db

${name}: O'Brien

SQLite:
  file: out/spec-sqlite.db
  update:
    - create table users (id integer primary key, name text)
    - insert into users (name) values ('${name}')
  query: select name from users where name = '${name}'

Expected output:
  - name: O'Brien
```

