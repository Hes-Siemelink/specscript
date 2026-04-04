# Command: SQLite defaults

`SQLite defaults` sets default parameters for subsequent `SQLite` commands. This avoids repeating the `file` parameter
in every command.

| Input  | Supported |
|--------|-----------|
| Value  | no        |
| List   | no        |
| Object | yes       |

## Basic usage

Set the database file once and use `SQLite` without specifying it:

```yaml specscript
Code example: SQLite defaults

Shell: rm -f out/spec-sqlite-defaults.db

SQLite defaults:
  file: out/spec-sqlite-defaults.db

SQLite:
  update:
    - create table users (id integer primary key, name text)
    - insert into users (name) values ('Alice')
  query: select * from users

Expected output:
  - id: 1
    name: Alice
```
