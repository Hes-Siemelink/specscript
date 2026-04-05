import { registerCommand } from '../language/command-handler.js'
import { setDisplayFormatter } from '../language/types.js'
import { toDisplayYaml } from '../util/yaml.js'
import { Print } from './print.js'
import { Output, As } from './variables.js'
import { Do, Exit, If, When, ForEach, Repeat } from './control-flow.js'
import { ErrorCommand, OnError, OnErrorType } from './error.js'
import { ScriptInfo, InputParameters, InputSchema } from './script-info.js'
import {
  AssertEquals, AssertThat,
  ExpectedOutput, ExpectedConsoleOutput, ExpectedError,
  TestCase, CodeExample, Answers,
  Tests, BeforeAllTests, AfterAllTests,
} from './testing.js'
import {
  Json, Text, PrintJson, ParseYamlCommand,
  Base64Encode, Base64Decode, WaitCommand,
} from './util.js'
import {
  Add, AddTo, Append, Fields, Values, Size, Sort, Find, Replace,
} from './data-manipulation.js'
import { JsonPatchCommand } from './json-patch.js'
import { CdCommand, TempFileCommand, ReadFileCommand, WriteFileCommand } from './files.js'
import { ShellCommand } from './shell.js'
import { RunCommand } from './run.js'
import { CliCommand } from './cli-command.js'
import { HttpServerCommand, HttpEndpointCommand, StopHttpServerCommand } from './http-server.js'
import { GetCommand, PostCommand, PutCommand, PatchCommand, DeleteCommand, HttpRequestDefaultsCommand } from './http.js'
import { CheckTypeCommand } from './check-type.js'
import { PromptCommand } from './prompt.js'
import { PromptObjectCommand } from './prompt-object.js'
import { ConfirmCommand } from './confirm.js'
import {
  CredentialsCommand, GetAllCredentialsCommand, GetCredentialsCommand,
  SetDefaultCredentialsCommand, CreateCredentialsCommand, DeleteCredentialsCommand,
} from './credentials.js'
import { ConnectToCommand } from './connect-to.js'
import {
  McpServerCommand, McpToolCommand, McpResourceCommand,
  McpPromptCommand, McpCallToolCommand, McpReadResourceCommand,
  McpGetPromptCommand, StopMcpServerCommand,
} from './mcp-server.js'
import { SQLiteCommand, SQLiteDefaultsCommand, StoreCommand } from './sqlite.js'

/**
 * Register all commands (Level 0 + Level 1 + Level 3 + Level 4).
 */
export function registerAllCommands(): void {
  // Register YAML display formatter for variable interpolation
  setDisplayFormatter(toDisplayYaml)

  // --- Level 0 ---

  // Util
  registerCommand(Print)

  // Variables
  registerCommand(Output)
  registerCommand(As)

  // Control flow (Level 0)
  registerCommand(Do)
  registerCommand(Exit)

  // Errors (Level 0)
  registerCommand(ErrorCommand)

  // Script info
  registerCommand(ScriptInfo)
  registerCommand(InputParameters)
  registerCommand(InputSchema)

  // Testing
  registerCommand(AssertEquals)
  registerCommand(AssertThat)
  registerCommand(ExpectedOutput)
  registerCommand(ExpectedConsoleOutput)
  registerCommand(ExpectedError)
  registerCommand(TestCase)
  registerCommand(CodeExample)
  registerCommand(Answers)
  registerCommand(Tests)
  registerCommand(BeforeAllTests)
  registerCommand(AfterAllTests)

  // --- Level 1 ---

  // Control flow
  registerCommand(If)
  registerCommand(When)
  registerCommand(ForEach)
  registerCommand(Repeat)

  // Error handling
  registerCommand(OnError)
  registerCommand(OnErrorType)

  // Data manipulation
  registerCommand(Add)
  registerCommand(AddTo)
  registerCommand(Append)
  registerCommand(Fields)
  registerCommand(Values)
  registerCommand(Size)
  registerCommand(Sort)
  registerCommand(Find)
  registerCommand(Replace)
  registerCommand(JsonPatchCommand)

  // Utility
  registerCommand(Json)
  registerCommand(Text)
  registerCommand(PrintJson)
  registerCommand(ParseYamlCommand)
  registerCommand(Base64Encode)
  registerCommand(Base64Decode)
  registerCommand(WaitCommand)

  // --- Level 3 ---

  // File commands
  registerCommand(CdCommand)
  registerCommand(TempFileCommand)
  registerCommand(ReadFileCommand)
  registerCommand(WriteFileCommand)

  // Shell
  registerCommand(ShellCommand)

  // Script composition
  registerCommand(RunCommand)

  // CLI
  registerCommand(CliCommand)

  // Database
  registerCommand(SQLiteCommand)
  registerCommand(SQLiteDefaultsCommand)
  registerCommand(StoreCommand)

  // --- Level 4 ---

  // HTTP server
  registerCommand(HttpServerCommand)
  registerCommand(HttpEndpointCommand)
  registerCommand(StopHttpServerCommand)

  // HTTP client
  registerCommand(GetCommand)
  registerCommand(PostCommand)
  registerCommand(PutCommand)
  registerCommand(PatchCommand)
  registerCommand(DeleteCommand)
  registerCommand(HttpRequestDefaultsCommand)

  // --- Level 5 ---

  // Schema / Types
  registerCommand(CheckTypeCommand)

  // User interaction
  registerCommand(PromptCommand)
  registerCommand(PromptObjectCommand)
  registerCommand(ConfirmCommand)

  // Connections
  registerCommand(ConnectToCommand)
  registerCommand(CredentialsCommand)
  registerCommand(GetAllCredentialsCommand)
  registerCommand(GetCredentialsCommand)
  registerCommand(SetDefaultCredentialsCommand)
  registerCommand(CreateCredentialsCommand)
  registerCommand(DeleteCredentialsCommand)

  // --- Level 6 ---

  // MCP server
  registerCommand(McpServerCommand)
  registerCommand(McpToolCommand)
  registerCommand(McpResourceCommand)
  registerCommand(McpPromptCommand)
  registerCommand(McpCallToolCommand)
  registerCommand(McpReadResourceCommand)
  registerCommand(McpGetPromptCommand)
  registerCommand(StopMcpServerCommand)
}

/**
 * Register Level 0 commands only (backward compatibility).
 * @deprecated Use registerAllCommands() instead.
 */
export function registerLevel0Commands(): void {
  registerAllCommands()
}
