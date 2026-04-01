/**
 * Markdown scanner for .spec.md files.
 *
 * Parses a Markdown document into a list of typed blocks. The scanner is a line-by-line
 * state machine that classifies code blocks by their opening directive.
 */

// --- Block types ---

export interface BlockType {
  readonly name: string
  readonly firstLinePrefix: string
  readonly lastLinePrefix: string
}

export const Text: BlockType = { name: 'Text', firstLinePrefix: '', lastLinePrefix: '```' }
export const Header: BlockType = { name: 'Header', firstLinePrefix: '#', lastLinePrefix: '' }
export const HiddenSpecScriptYaml: BlockType = { name: 'HiddenSpecScriptYaml', firstLinePrefix: '<!-- yaml specscript', lastLinePrefix: '-->' }
export const SpecScriptYaml: BlockType = { name: 'SpecScriptYaml', firstLinePrefix: '```yaml specscript', lastLinePrefix: '```' }
export const YamlFile: BlockType = { name: 'YamlFile', firstLinePrefix: '```yaml temp-file', lastLinePrefix: '```' }
export const ShellCli: BlockType = { name: 'ShellCli', firstLinePrefix: '```cli', lastLinePrefix: '```' }
export const ShellBlock: BlockType = { name: 'ShellBlock', firstLinePrefix: '```shell', lastLinePrefix: '```' }
export const Answers: BlockType = { name: 'Answers', firstLinePrefix: '<!-- answers', lastLinePrefix: '-->' }
export const Output: BlockType = { name: 'Output', firstLinePrefix: '```output', lastLinePrefix: '```' }
export const Quote: BlockType = { name: 'Quote', firstLinePrefix: '> ', lastLinePrefix: '' }

/**
 * Priority order for block detection. YamlFile must come before SpecScriptYaml
 * (both start with ```yaml), ShellCli (```cli) must come before other blocks.
 */
const BLOCK_TYPES: BlockType[] = [
  YamlFile,
  HiddenSpecScriptYaml,
  SpecScriptYaml,
  ShellCli,
  ShellBlock,
  Answers,
  Output,
  Header,
  Text, // last / fallback
]

// --- Markdown block ---

export class MarkdownBlock {
  readonly type: BlockType
  readonly headerLine: string
  readonly lines: string[] = []

  constructor(type: BlockType, headerLine: string = '') {
    this.type = type
    this.headerLine = headerLine
  }

  /** Extract an option value from the header line, e.g. file=data.yaml → "data.yaml" */
  getOption(option: string): string | undefined {
    const match = new RegExp(`${option}=(\\S+)`).exec(this.headerLine)
    return match ? match[1] : undefined
  }

  /** Get the content of the block as a single string. */
  getContent(): string {
    return this.lines.join('\n')
  }
}

// --- Scanner ---

/**
 * Scan a Markdown document (as a string) into a list of typed blocks.
 * Matches the Kotlin SpecScriptMarkdown.scan() algorithm exactly.
 */
export function scanMarkdown(content: string): MarkdownBlock[] {
  const blocks: MarkdownBlock[] = []
  let currentBlock = addBlock(blocks, Text)

  const lines = content.split('\n')

  for (const line of lines) {
    // Quote is a special case: indicated by a prefix on every line
    if (line.startsWith(Quote.firstLinePrefix)) {
      if (currentBlock.type === Text) {
        currentBlock = addBlock(blocks, Quote)
      }
      if (currentBlock.type === Quote) {
        currentBlock.lines.push(line.substring(Quote.firstLinePrefix.length))
      }
      continue
    }

    // Text is the container type for other block types
    if (currentBlock.type === Text) {
      const startBlockType = BLOCK_TYPES.find(bt => line.startsWith(bt.firstLinePrefix))

      if (!startBlockType || startBlockType === Text) {
        currentBlock.lines.push(line)
      } else {
        currentBlock = addBlock(blocks, startBlockType, line)
      }
      continue
    }

    // When a block ends, return to Text
    if (line.startsWith(currentBlock.type.lastLinePrefix)) {
      currentBlock = addBlock(blocks, Text)
      continue
    }

    // Otherwise, add the line to the current block
    currentBlock.lines.push(line)
  }

  return blocks
}

function addBlock(blocks: MarkdownBlock[], type: BlockType, headerLine: string = ''): MarkdownBlock {
  const block = new MarkdownBlock(type, headerLine)
  blocks.push(block)
  return block
}
