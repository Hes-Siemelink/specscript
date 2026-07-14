/**
 * Convert Markdown blocks to executable Scripts.
 *
 * Handles block-to-command conversion and section splitting by # headers.
 */

import type {MarkdownBlock} from './scanner.js'
import {
    Answers as AnswersBlock,
    Header,
    HiddenSpecScriptYaml,
    Output,
    Quote,
    scanMarkdown,
    ShellBlock,
    ShellCli,
    SpecScriptYaml,
    YamlFile,
} from './scanner.js'
import {Script} from '../language/script.js'
import type {Command, JsonValue} from '../language/types.js'
import {parseMarkdownYamlCommands, parseYaml} from '../util/yaml.js'

/**
 * Convert a list of MarkdownBlocks into a Script.
 *
 * Block types handled:
 * - SpecScriptYaml / HiddenSpecScriptYaml → parse as YAML command list
 * - Answers → Answers command
 * - Output → Expected console output command
 * - Quote → Print command
 * - Header → sets script title
 * - YamlFile → Temp file command
 * - ShellBlock → Shell command (skipped if 'ignore')
 * - ShellCli → Cli command (skipped if 'ignore')
 */
export function blocksToScript(blocks: MarkdownBlock[]): Script {
    const commands: Command[] = []
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
            const cd = block.getOption('cd')
            if (cd) {
                commands.push({name: 'Cd', data: cd})
            }
            const content = block.getContent()
            if (content.trim()) {
                commands.push(...parseMarkdownYamlCommands(content))
            }
            continue
        }

        if (block.type === AnswersBlock) {
            const content = block.getContent()
            if (content.trim()) {
                const data = parseYaml(content)
                commands.push({name: 'Answers', data})
            }
            continue
        }

        if (block.type === Quote) {
            commands.push({name: 'Print', data: block.getContent()})
            continue
        }

        if (block.type === Output) {
            commands.push({name: 'Expected console output', data: block.getContent()})
            continue
        }

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
            if (!cd) {
                data.cd = '${SCRIPT_HOME}'
            } else if (cd.startsWith('/') || cd.startsWith('${')) {
                data.cd = cd
            } else {
                data.cd = '${SCRIPT_HOME}/' + cd
            }
            commands.push({name: 'Shell', data})
            continue
        }

        if (block.type === ShellCli) {
            if (block.headerLine.includes('ignore')) {
                continue // Ignored blocks produce no commands
            }
            const content = block.getContent()
            const cd = block.getOption('cd')
            if (cd) {
                commands.push({name: 'Cli', data: {command: content, cd}})
            } else {
                commands.push({name: 'Cli', data: content})
            }
            continue
        }

        if (block.type === YamlFile) {
            // YamlFile → Temp file command with name from temp-file= option
            const name = block.getOption('temp-file')
            const resolveOpt = block.getOption('resolve')
            const content = block.getContent()
            // Markdown yaml file blocks default resolve to false (unlike the YAML command default of true)
            const resolveFlag = resolveOpt !== undefined ? resolveOpt === 'true' : false
            if (name) {
                commands.push({name: 'Temp file', data: {name, content, resolve: resolveFlag}})
            } else {
                commands.push({name: 'Temp file', data: {content, resolve: resolveFlag}})
            }

        }

        // Text and other block types: no commands
    }

    return new Script(commands, title)
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
 * Parse Markdown content into executable Scripts, one per # header section.
 * Sections with no executable commands are dropped.
 */
export function parseMarkdownScripts(content: string): Script[] {
    const blocks = scanMarkdown(content)
    return splitMarkdownSections(blocks).filter(script => script.commands.length > 0)
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
