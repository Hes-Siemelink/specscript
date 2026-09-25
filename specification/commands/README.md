# SpecScript Command Reference

Overview of standard commands in SpecScript.

## Core commands

Everything you need to write down a spec and run it.

### Script definition

* [Script info](core/script-info/Script%20info.spec.md) - Contains script description and input parameter definitions.

### Variables

* [${..} assignment](core/variables/Assignment.spec.md) - Sets a variable value
* [As](core/variables/As.spec.md) - Sets a variable to the contents of the output variable
* [Output](core/variables/Output.spec.md) - Sets the output variable

### Data manipulation

* [Add](core/data-manipulation/Add.spec.md) - Adds items
* [Add to](core/data-manipulation/Add%20to.spec.md) - Adds items to existing variables
* [Append](core/data-manipulation/Append.spec.md) - Adds items to the output variable
* [Fields](core/data-manipulation/Fields.spec.md) - Returns the list of fields in an object
* [Find](core/data-manipulation/Find.spec.md) - Retrieves a snippet from a larger object
* [Json patch](core/data-manipulation/Json%20patch.spec.md) - Applies JSON Patch operations to a document
* [Replace](core/data-manipulation/Replace.spec.md) - Does a text-based find&replace
* [Size](core/data-manipulation/Size.spec.md) - Gives you the size of things
* [Sort](core/data-manipulation/Sort.spec.md) - Sorts an array
* [Values](core/data-manipulation/Values.spec.md) - Returns the list of values in an object

### Control flow

* [Do](core/control-flow/Do.spec.md) - Executes one or more commands
* [Exit](core/control-flow/Exit.spec.md) - Stops running the current script
* [For each](core/control-flow/For%20each.spec.md) - Loops over or transforms a list or object
* [If](core/control-flow/If.spec.md) - Executes commands if a condition holds
* [Repeat](core/control-flow/Repeat.spec.md) - Executes a block of code until a condition is satisfied
* [When](core/control-flow/When.spec.md) - Executes a single command from a list of conditions

### Error handling

* [Error](core/errors/Error.spec.md) - Raises an error
* [On error](core/errors/On%20error.spec.md) - Handles any error
* [On error type](core/errors/On%20error%20type.spec.md) - Handles an error of a specific type

### Testing

* [Test case](core/testing/Test%20case.spec.md) - Marks a test case
* [Assert that](core/testing/Assert%20that.spec.md) - Executes a condition
* [Assert equals](core/testing/Assert%20equals.spec.md) - Tests two objects for equality
* [Code example](core/testing/Code%20example.spec.md) - Marks example code
* [Expected output](core/testing/Expected%20output.spec.md) - Tests the output variable against a given value
* [Expected error](core/testing/Expected%20error.spec.md) - Tests if an error was raised
* [Answers](core/testing/Answers.spec.md) - Prerecords answers for prompts, so they can pass automated tests.

### Types and validation

* [Check type](core/types/Types.spec.md) - Checks if data is valid according to a type definition
* [Validate schema](core/types/Validate%20schema.spec.md) - Validates data against a JSON Schema

### User interaction

* [Confirm](core/user-interaction/Confirm.spec.md) - Asks the user for confirmation
* [Prompt](core/user-interaction/Prompt.spec.md) - Asks the user for input with an interactive prompt

### Util

* [Base64 decode](core/util/Base64%20decode.spec.md) - Decodes a Base64 encoded text
* [Base64 encode](core/util/Base64%20encode.spec.md) - Does a Base64 encoding
* [Json](core/util/Json.spec.md) - Converts structured data into a JSON string
* [Parse Yaml](core/util/Parse%20Yaml.spec.md) - Parses a Yaml or Json string into structured data
* [Print](core/util/Print.spec.md) - Prints to the console
* [Print Json](core/util/Print%20Json.spec.md) - Prints the output variable as JSON
* [Text](core/util/Text.spec.md) - Converts structured data into a string
* [Wait](core/util/Wait.spec.md) - Waits a while

## Local IO

### Files

* [Cd](core/files/Cd.spec.md) - Changes the working directory
* [Read file](core/files/Read%20file.spec.md) - Loads Yaml or Json from a file
* [Write file](core/files/Write%20file.spec.md) - Saves content to a file as Yaml
* [Temp file](core/files/Temp%20file.spec.md) - Saves content to a temporary file

### Run

* [Run](core/run/Run.spec.md) - Runs another SpecScript file or inline script
* [Cli](core/run/Cli.spec.md) - Executes the SpecScript cli command
* [SpecScript files as commands](core/run/SpecScript%20files%20as%20commands.spec.md) - To run any SpecScript file in
  the same directory as a regular command

### Shell

* [Shell](shell/Shell.spec.md) - Executes a shell command

## Integrations

Commands that hand off to something outside the SpecScript world.

### Connections

* [Connect to](connections/Connect%20to.spec.md) - Sets up a connection to a named endpoint
* [Create credentials](connections/Create%20credentials.spec.md) - Configures a credentials for an endpoint and
  saves it in the user's preferences.
* [Credentials](connections/Credentials.spec.md) - Use a different credentials file than the default one from the
  home directory
* [Get credentials](connections/Get%20credentials.spec.md) - Gets the default credentials for an endpoint
* [Get all credentials](connections/Get%20all%20credentials.spec.md) - Gets all credential for an endpoint
* [Set default credentials](connections/Set%20default%20credentials.spec.md) - Sets the default credentials for an
  endpoint
* [Delete credentials](connections/Delete%20credentials.spec.md) - Deletes credentials for an endpoint

### Database

* [SQLite](db/SQLite.spec.md) - Executes SQL statements against a SQLite database file
* [SQLite defaults](db/SQLite%20defaults.spec.md) - Sets default parameters for subsequent SQLite commands
* [Store](db/Store.spec.md) - Stores and queries JSON documents in SQLite

### Http client

* [GET](http/client/GET.spec.md) - Sends a GET request to an HTTP endpoint
* [POST](http/client/POST.spec.md) - Sends a POST request to an HTTP endpoint
* [PUT](http/client/PUT.spec.md) - Sends a PUT request to an HTTP endpoint
* [PATCH](http/client/PATCH.spec.md) - Sends a PATCH request to an HTTP endpoint
* [DELETE](http/client/DELETE.spec.md) - Sends a DELETE request to an HTTP endpoint
* [Http session](http/client/Http%20session.spec.md) - Opens a session that provides default parameters for subsequent
  HTTP commands
* [Http close session](http/client/Http%20close%20session.spec.md) - Closes an open Http session

### Http server

* [Http server](http/server/Http%20server.spec.md) - Starts an embedded HTTP server, based on an OpenAPI-flavored spec and
  backed by SpecScript commands.

### MCP client

* [Mcp call tool](mcp/client/Mcp%20call%20tool.spec.md) - Executes tools on MCP servers
* [Mcp read resource](mcp/client/Mcp%20read%20resource.spec.md) - Reads resources from MCP servers
* [Mcp get prompt](mcp/client/Mcp%20get%20prompt.spec.md) - Gets prompts from MCP servers
* [Mcp session](mcp/client/Mcp%20session.spec.md) - Opens a connection to an MCP server that stays alive across calls
* [Mcp close session](mcp/client/Mcp%20close%20session.spec.md) - Closes an open Mcp session

### MCP server

* [Mcp server](mcp/server/Mcp%20server.spec.md) - Starts an MCP server with tools, resources, and prompts
* [Mcp tool](mcp/server/Mcp%20tool.spec.md) - Defines tools for an MCP server
* [Mcp prompt](mcp/server/Mcp%20prompt.spec.md) - Defines prompts for an MCP server
* [Mcp resource](mcp/server/Mcp%20resource.spec.md) - Defines resources for an MCP server