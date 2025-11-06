# On my mind

* Rewrite README
* Test mode
    * Merge tests into CLI so you can run cli --test <comething>
* Define commands in Markdown and publish thenm as library functions
* Use JSON schema for input. Or Typescript syntax?

## In general

* AI to generate scripts: notebook type interaction to write scripts
* Document Json Patch (uses RFC 6902)

# Actually build something

* Home Recipes
    * Store stuff on file system with a construct like Connect to
    * Store stuff on sqlite using same construct
    * Edit lists

# Installable artifact

* Produce a clean, lightweight, native library
* Node / TypeScript?
* Compile to WASM
* Kotlin native? => Use Pure Kotlin libraries
* Go?

# SpecScript

* Naming alternatives
    * Spec.it
* Add test support in cli command
    * `spec --run` vs `spec --test`

# SpecScript language

## Core

* Define commands in SpecScript itself
* Secrets
* Properly handle: null, empty, boolean, int
* Stream and pipe output as Yaml array of lines
* Find a way to confirm default input parameters vs. just taking them for granted. Interactive mode would trigger
  confirmation?

## Types

* Document types
* Define array and string types in type system
* Define 'output type' on Script info
* Turn types into JSON schema
* Refactor types and directory info
* Base on TypeScript types

## User interaction

* Resolve difference between Input parameters and Prompt for default values. Use two terms:
    - 'default' for Input parameters only -- no confirmation
    - 'placeholder' for both that will display the value for confirmation. (Alternative names: 'suggestion', '
      example', 'hint')
    - When putting 'default' on Prompt it will act as a placeholder
* Check command browser on Windows
* Command browser should support ESC character to go a level up or exit

## Http

* Configuring connections out of the box with packaged scripts
* Clean up Connect to: be smart about multiple connections and tokens. Currently `connect-to` script in Digital.ai only
  checks if something has been set as Http defaults
* Built-in OAuth. It's kinda cool that you can do it in SpecScript but not that you should... Makes the script 'turn
  into code'.

## Shell & Files

* Support 'default' parameter to support text parameters in commands that are SpecScript scripts
* Support 'list execution' for commands that are SpecScript scripts
* Multi-line shell support
* Support stdin:
  if (System.`in`.available() != 0) { val input = Yaml.mapper.readTree(System.`in`)}

## MCP

* TODOs in MCP server.
* Process MCP result as list

## AI agents

* Langchain
* https://github.com/JetBrains/koog

## Code organization

* Versioning and auto-upgrading of commands
* Review Script info on directories
* Support modules

## Improve slow startup

* Jackson is slow on Kotlin: https://github.com/FasterXML/jackson-module-kotlin/issues/69
* Jackson Jr?
* Alternative Yaml lib: https://github.com/pwall567/kjson-yaml
* Or rewrite as Go or TypeScript

# Bugs

* BUG: First line of output is not captured if shell script asks for user input => command appears to hang
* BUG: Create connection doesn't work

```shell
hes@Mac samples % cli digitalai/release/login 
Login to Digital.ai Release

* Available commands: connect                     Logs in to Digital.ai Release

Instacli scripting error

Unknown command: Create Release connection

In login:

  If:
    empty: "${endpoint}"
    then:
      Create Release connection: true
      As: "${endpoint}"

```

* BUG: in select connection

```shell
Login to Digital.ai Release

* Available commands: select-release-connection   Select the default connection
? Select account to log in with * Create new account
Please configure a connection to Digital.ai Release

Instacli scripting error

Caused by: java.lang.ClassCastException: class com.fasterxml.jackson.databind.node.TextNode cannot be cast to class com.fasterxml.jackson.databind.node.ObjectNode (com.fasterxml.jackson.databind.node.TextNode and com.fasterxml.jackson.databind.node.ObjectNode are in unnamed module of loader 'app')

In create-release-connection.cli:

  If:
    empty: "${account.url}"
    then:
      Create release connection: ""
      As: "${account}"

```

# Where to take it

* Plaxolotl - cli scripts are just an interface to a portable execution format. This format has all the metadata defined
  explicitly. For example: content type, variable replacement yes/no, etc.
* HouseApp
* Run Release templates
* SpecScript as bash replacement
    * Shell and pipe support
    * https://www.gnu.org/software/bash/manual/bash.html
    * Database support (SQLite or something)

# Blog topics

* How to Design a Language Without Writing a Parser
* How Complexity Creeps in
* Keep it Flat, SIlly (KIFS)
* How (Not) to Create a Lisp
* The Zen of Frictionlessity: On Avoiding Surprises, Humor and Being Clever
    * "Http server" was "Http endpoint" was: "Http serve"
    * Cheesy test data
* Liberation of Code: Say What, Not How
    * Sample Server in SpecScript vs. Javalin/Kotlin
* Coding is the Unhappy Path
    * 80% of code is about exceptions to the rule
    * 80% of coding goes into the happy path
    * Typical scenario
        1. Struggle to get the happy path working
        2. Don't apply learnings to code base
        3. Over 50% (or more) of the code is about the exceptions
        4. Exception handling / alternative paths are implemented on top of a shaky code base as an afterthought
    * Why not try "Exception Driven Programming". But: you can't _start_ with the exceptions.
    * Define the happy path as declarative as possible. Build the exception flow around it?
    * Mold the language to have fewer exceptions: be declarative, idempotent

# Nerd badges 🦡

* Code as-code
* Towers of Hanoi
* Made it a Lisp by adding 30 lines of code.
* Define SpecScript in SpecScript itself