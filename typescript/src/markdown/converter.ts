/**
 * Convert Markdown blocks to executable Scripts.
 *
 * Handles block-to-command conversion and section splitting by # headers.
 */

import type { MarkdownBlock } from './scanner.js'
import {
  Header, SpecScriptYaml, HiddenSpecScriptYaml,
  Answers as AnswersBlock, Output, Quote,
  YamlFile, ShellBlock, ShellCli,
} from './scanner.js'
import { Script, toCommandList } from '../language/script.js'
import type { Command, JsonValue } from '../language/types.js'
import { parseYamlCommands } from '../util/yaml.js'
import { parseYaml } from '../util/yaml.js'

/**
 * Convert a list of MarkdownBlocks into a Script.
 *
 * Block types handled at Level 2:
 * - SpecScriptYaml / HiddenSpecScriptYaml → parse as YAML command list
 * - Answers → Answers command
 * - Output → Expected console output command
 * - Quote → Print command
 * - Header → sets script title
 *
 * Block types recognized but not converted (Level 3+):
 * - YamlFile → skipped (needs Temp file command)
 * - ShellBlock → skipped unless 'ignore' (needs Shell command)
 * - ShellCli → skipped unless 'ignore' (needs Cli command)
 */
export function blocksToScript(blocks: MarkdownBlock[]): Script {
  const commands: Command[] = []
  const skippedBlocks: string[] = []
  let title: string | undefined

  for (const block of blocks) {
    if (block.type === Header) {
      // Extract title from header line: "## Some Title" → "Some Title"
      const spaceIndex = block.headerLine.indexOf(' ')
      if (spaceIndex >= 0) {
        title = block.headerLine.substring(spaceIndex).trim()
      }
      continue
    }

    if (block.type === SpecScriptYaml || block.type === HiddenSpecScriptYaml) {
      const content = block.getContent()
      if (content.trim()) {
        // getContent() never ends with \n, so block scalars need trailing \n stripped
        // to match Jackson's behavior (which only adds \n when source ends with \n)
        commands.push(...parseYamlCommands(content, true))
      }
      continue
    }

    if (block.type === AnswersBlock) {
      const content = block.getContent()
      if (content.trim()) {
        const data = parseYaml(content)
        commands.push({ name: 'Answers', data })
      }
      continue
    }

    if (block.type === Quote) {
      commands.push({ name: 'Print', data: block.getContent() })
      continue
    }

    if (block.type === Output) {
      commands.push({ name: 'Expected console output', data: block.getContent() })
      continue
    }

    // Level 3+ block types: skip unless 'ignore'
    if (block.type === ShellBlock) {
      if (block.headerLine.includes('ignore')) {
        continue // Ignored blocks produce no commands
      }
      const content = block.getContent()
      const cd = block.getOption('cd')
      const showOutput = block.getOption('show_output')
      const showCommand = block.getOption('show_command')
      // Markdown shell blocks default to show output: true (unlike the YAML command default)
      const data: Record<string, JsonValue> = {
        command: content,
        'show output': showOutput !== undefined ? showOutput === 'true' : true,
        'show command': showCommand === 'true',
      }
      if (cd) data.cd = cd
      commands.push({ name: 'Shell', data })
      continue
    }

    if (block.type === ShellCli) {
      if (block.headerLine.includes('ignore')) {
        continue // Ignored blocks produce no commands
      }
      const content = block.getContent()
      const cd = block.getOption('cd')
      if (cd) {
        commands.push({ name: 'Cli', data: { command: content, cd } })
      } else {
        commands.push({ name: 'Cli', data: content })
      }
      continue
    }

    if (block.type === YamlFile) {
      // YamlFile → Temp file command with filename from file= option
      const filename = block.getOption('file')
      const resolveOpt = block.getOption('resolve')
      const content = block.getContent()
      // Markdown yaml file blocks default resolve to false (unlike the YAML command default of true)
      const resolveFlag = resolveOpt !== undefined ? resolveOpt === 'true' : false
      if (filename) {
        commands.push({ name: 'Temp file', data: { filename, content, resolve: resolveFlag } })
      } else {
        commands.push({ name: 'Temp file', data: { content, resolve: resolveFlag } })
      }
      continue
    }

    // Text and other block types: no commands
  }

  return new Script(commands, title, skippedBlocks)
}

/**
 * Split Markdown blocks into sections by # headers.
 * Each section becomes a separate Script (test case).
 * Sections with no executable commands are skipped.
 */
export function splitMarkdownSections(blocks: MarkdownBlock[]): Script[] {
  const sections: MarkdownBlock[][] = []
  let current: MarkdownBlock[] = []

  for (const block of blocks) {
    if (block.type === Header) {
      if (current.length > 0) {
        sections.push(current)
      }
      current = [block]
    } else {
      current.push(block)
    }
  }

  // Add the last section
  if (current.length > 0) {
    sections.push(current)
  }

  return sections.map(blocksToScript)
}

/**
 * Get the test title for a Script parsed from Markdown.
 * Prefers the section title (from # header), then falls back to the first
 * Code example command's value, then "Untitled".
 */
export function getTestTitle(script: Script): string {
  if (script.title) return script.title

  const codeExample = script.commands.find(
    c => c.name.toLowerCase() === 'code example'
  )
  if (codeExample && typeof codeExample.data === 'string') {
    return codeExample.data
  }

  return 'Untitled'
}
