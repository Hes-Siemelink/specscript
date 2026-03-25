import { registerCommand } from '../language/command-handler.js'
import { setDisplayFormatter } from '../language/types.js'
import { toDisplayYaml } from '../util/yaml.js'
import { Print } from './print.js'
import { Output, As } from './variables.js'
import { Do, Exit } from './control-flow.js'
import { ErrorCommand } from './error.js'
import { ScriptInfo, InputParameters, InputSchema } from './script-info.js'
import {
  AssertEquals, AssertThat,
  ExpectedOutput, ExpectedConsoleOutput, ExpectedError,
  TestCase, CodeExample, Answers,
  Tests, BeforeAllTests, AfterAllTests,
} from './testing.js'

/**
 * Register all Level 0 commands.
 */
export function registerLevel0Commands(): void {
  // Register YAML display formatter for variable interpolation
  setDisplayFormatter(toDisplayYaml)

  // Util
  registerCommand(Print)

  // Variables
  registerCommand(Output)
  registerCommand(As)

  // Control flow
  registerCommand(Do)
  registerCommand(Exit)

  // Errors
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
}
