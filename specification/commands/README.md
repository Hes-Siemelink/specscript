# SpecScript Command Reference

Overview of standard commands in SpecScript.

## Core commands

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
* [Find](core/data-manipulation/Find.spec.md) - Retrieves a snippet from a larger object
* [Replace](core/data-manipulation/Replace.spec.md) - Does a text-based find&replace
* [Size](core/data-manipulation/Size.spec.md) - Gives you the size of things
* [Sort](core/data-manipulation/Sort.spec.md) - Sorts an array

### Control flow

* [Do](core/control-flow/Do.spec.md) - Executes one or more commands
* [Exit](core/control-flow/Exit.spec.md) - Stops running the current script
* [For each](core/control-flow/For%20each.spec.md) - Loops over or transforms a list or object
* [Repeat](core/control-flow/Repeat.spec.md) - Executes a block of code until a condition is satisfied
* [If](core/control-flow/If.spec.md) - exectues commands if a condition holds
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

### User interaction

* [Confirm](core/user-interaction/Confirm.spec.md) - Asks the user for confirmation
* [Prompt](core/user-interaction/Prompt.spec.md) - Asks the user for input with an interactive prompt
* [Prompt object](core/user-interaction/Prompt%20object.spec.md) - Asks multiple questions and stores the answers into
  one object

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

* [Read file](core/files/Read%20file.spec.md) - Loads Yaml or Json from a file
* [Write file](core/files/Write%20file.spec.md) - Saves content to a file as Yaml
* [Temp file](core/files/Temp%20file.spec.md) - Saves content to a temporary file
* [Run script](core/files/Run%20script.spec.md) - Runs another SpecScript file
* [SpecScript files as commands](core/files/SpecScript%20files%20as%20commands.spec.md) - To run any SpecScript file in
  the same directory as a regular command

### Shell

* [Cli](core/shell/Cli.spec.md) - Executes the SpecScript cli command
* [Shell](core/shell/Shell.spec.md) - Executes a shell command

## REST API interaction

### Http client

* [GET](core/http/GET.spec.md) - Sends a GET request to an HTTP endpoint
* [POST](core/http/POST.spec.md) - Sends a POST request to an HTTP endpoint
* [PUT](core/http/PUT.spec.md) - Sends a PUT request to an HTTP endpoint
* [PATCH](core/http/PATCH.spec.md) - Sends a PATCH request to an HTTP endpoint
* [DELETE](core/http/DELETE.spec.md) - Sends a DELETE request to an HTTP endpoint
* [Http request defaults](core/http/Http%20request%20defaults.spec.md) - Sets the default parameters for all subsequent
  HTTP commands.

### Http server

* [Http server](core/http/Http%20server.spec.md) - Starts an embedded HTTP server, based on an OpenAPI-flavored spec and
  backed by SpecScript commands.

### Manage credentials

* [Connect to](core/connections/Connect%20to.spec.md) - Sets up a connection to a named endpoint
* [Create credentials](core/connections/Create%20credentials.spec.md) - Configures a credentials for an endpoint and
  saves it in the user's preferences.
* [Credentials](core/connections/Credentials.spec.md) - Use a different credentials file than the default one from the
  home directory
* [Get credentials](core/connections/Get%20credentials.spec.md) - Gets the default credentials for an endpoint
* [Get all credentials](core/connections/Get%20all%20credentials.spec.md) - Gets all credential for an endpoint
* [Set default credentials](core/connections/Set%20default%20credentials.spec.md) - Sets the default credentials for an
  endpoint
* [Delete credentials](core/connections/Delete%20credentials.spec.md) - Deletes credentials for an endpoint

## AI

### Model Context Protocol (MCP)

* [Mcp tool call](ai/mcp/Mcp%20tool%20call.spec.md) - Executes tools on MCP servers via various transports
* [Mcp server](ai/mcp/Mcp%20server.spec.md) - Starts an MCP server with tools, resources, and prompts
* [Mcp tool](ai/mcp/Mcp%20tool.spec.md) - Defines tools for an MCP server
* [Mcp prompt](ai/mcp/Mcp%20prompt.spec.md) - Defines prompts for an MCP server
* [Mcp resource](ai/mcp/Mcp%20resource.spec.md) - Defines resources for an MCP server

