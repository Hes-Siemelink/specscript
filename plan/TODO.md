# On my mind

* Stop running tests in the temp directory
* Move some commands out of core (db, http)

# Spotlight

* Introduce tutorials
* Document .spec wrapper file
* LLM commands and Agent definitions
* Bundled scripts
    * `spec serve http .`  to serve the current directory with SpecScript-defined endpoints
    * `spec serve mcp .`  to serve the current directory with MCP tools
    * `spec -i doc` browse the documentation in an interactive way.
    * `spec agent prime` to give a how-to for agents like 'beans prime'
    * `spec agent skills` lists agent skills
* Replace specscript-config.yaml with README and put stuff in Markdown front matter.

## In general

* Notebook type interaction to write scripts
* Tutorial / guide documents: "Spec documents are written spec-first — they define behavior before implementation
  exists. This drives a minimalist style: precise, executable, and cheap to change." Create a separate set of
  friendlier, tutorial-style "getting started" and "how-to" guides.

# Naming alternatives

* Spec.it

# SpecScript language

## Core

* `spec --version` CLI flag (needs version baked into jar at build time)
* Define commands in SpecScript itself
* Allow list on top level
* Secrets
* Properly handle: null, empty, boolean, int
* Stream and pipe output as Yaml array of lines
* Find a way to confirm default input parameters vs. just taking them for granted. Interactive mode would trigger
  confirmation?
* Use Markdown front matter for SpecScript files to define metadata

## User interaction

* Use Input schema for Prompt
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

* Support text parameters in commands that are SpecScript scripts
* Support 'list execution' for commands that are SpecScript scripts
* Multi-line shell support
* Support stdin:
  if (System.`in`.available() != 0) { val input = Yaml.mapper.readTree(System.`in`)}

## AI agents

* Langchain
* https://github.com/JetBrains/koog

## Code organization

* Versioning and auto-upgrading of commands
* Review Script info on directories
* Support modules

# Bugs

* BUG: First line of output is not captured if shell script asks for user input => command appears to hang

# Where to take it

* Plaxolotl - cli scripts are just an interface to a portable execution format. This format has all the metadata defined
  explicitly. For example: content type, variable replacement yes/no, etc.
* HouseApp
* Run Release templates
* SpecScript as bash replacement
    * Shell and pipe support
    * https://www.gnu.org/software/bash/manual/bash.html

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
* Clean spec allows AI to create and maintain TypeScript implementation without human intervention
