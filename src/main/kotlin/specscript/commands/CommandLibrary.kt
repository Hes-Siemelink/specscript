package specscript.commands

import specscript.commands.connections.*
import specscript.commands.controlflow.*
import specscript.commands.datamanipulation.*
import specscript.commands.db.SQLite
import specscript.commands.db.Store
import specscript.commands.errors.ErrorCommand
import specscript.commands.errors.OnError
import specscript.commands.errors.OnErrorType
import specscript.commands.files.ReadFile
import specscript.commands.files.RunScript
import specscript.commands.files.TempFile
import specscript.commands.files.WriteFile
import specscript.commands.http.*
import specscript.commands.mcp.*
import specscript.commands.schema.CheckType
import specscript.commands.schema.ValidateSchema
import specscript.commands.scriptinfo.InputParameters
import specscript.commands.scriptinfo.ScriptInfo
import specscript.commands.shell.Cli
import specscript.commands.shell.Shell
import specscript.commands.testing.*
import specscript.commands.userinteraction.Confirm
import specscript.commands.userinteraction.Prompt
import specscript.commands.userinteraction.PromptObject
import specscript.commands.util.*
import specscript.commands.variables.As
import specscript.commands.variables.Output
import specscript.language.CommandHandler

object CommandLibrary {

    val commands = commandMap(

        // Script definition
        InputParameters,
        ScriptInfo,

        // Variables
        As,
        Output,

        // Testing
        TestCase,
        CodeExample,
        AssertEquals,
        AssertThat,
        ExpectedConsoleOutput,
        ExpectedOutput,
        ExpectedError,
        Answers,

        // Control flow
        Do,
        Exit,
        If,
        When,
        ForEach,
        Repeat,
        Find,

        // Error handling
        ErrorCommand,
        OnError,
        OnErrorType,

        // User interaction
        Confirm,
        Prompt,
        PromptObject,

        // Data manipulation
        Add,
        AddTo,
        Append,
        Fields,
        JsonPatch,
        Replace,
        Size,
        Sort,
        Values,

        // Util
        Print,
        PrintJson,
        ToJson,
        Wait,
        Base64Encode,
        Base64Decode,

        // Files
        ReadFile,
        WriteFile,
        TempFile,

        // Call other scripts
        Cli,
        RunScript,
        Shell,

        // HTTP client
        Get,
        Post,
        Put,
        Patch,
        Delete,
        HttpRequestDefaults,

        // Http server
        HttpServer,

        // Account connections
        ConnectTo,
        CreateCredentials,
        Credentials,
        DeleteCredentials,
        GetAllCredentials,
        GetCredentials,
        SetDefaultCredentials,

        // JSON Schema
        ValidateSchema,

        // Types
        CheckType,

        // Database
        SQLite,
        Store,

        // AI
        CallMcpTool,
        McpPrompt,
        McpResource,
        McpServer,
        McpTool
    )

    // TODO Store commands in canonical form: all lower case and spaces
    private fun commandMap(vararg commands: CommandHandler): Map<String, CommandHandler> {
        return commands.associateBy { it.name }
    }
}