# Command: Store

`Store` provides a higher-level interface for storing and querying JSON documents in SQLite. Each row stores a JSON
object in a `json` column, and queries use `json_extract()` for field access.

| Input      | Supported     |
|------------|---------------|
| Scalar     | no            |
| List       | no            |
| Object     | yes           |

## Basic usage

Insert JSON objects and query them:

```yaml specscript
Code example: Store and query JSON documents

Shell: rm -f out/spec-store.db

Store:
  file: out/spec-store.db
  insert:
    - greeting: Hello
      language: English
    - greeting: Hola
      language: Spanish
  query:
    where: $.language = 'Spanish'

Expected output:
  - greeting: Hola
    language: Spanish
```

## Selecting specific fields

Use `select` to return only specific fields from the JSON documents:

```yaml specscript
Code example: Select specific fields

Shell: rm -f out/spec-store.db

Store:
  file: out/spec-store.db
  insert:
    - name: Alice
      age: 16
    - name: Bob
      age: 17
    - name: Charlie
      age: 18
  query:
    select:
      - name
    where: $.age < 18

Expected output:
  - name: Alice
  - name: Bob
```

## Custom table name

By default, data is stored in a table called `json_data`. Use `table` to specify a different name:

```yaml specscript
Code example: Custom table name

Shell: rm -f out/spec-store.db

Store:
  file: out/spec-store.db
  table: recipes
  insert:
    - name: Pancakes
      servings: 4

---

Store:
  file: out/spec-store.db
  table: recipes
  query:
    select:
      - name

Expected output:
  - name: Pancakes
```
