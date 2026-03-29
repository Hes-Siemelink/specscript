import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs'
import { join, resolve, dirname } from 'node:path'
import { homedir } from 'node:os'
import { parseYaml } from '../util/yaml.js'
import { canonicalName, type CommandHandler } from './command-handler.js'
import { createFileCommandHandler, fileToCommandName } from './context.js'
import type { ImportItem } from './package-import.js'
import type { JsonObject } from './types.js'

const YAML_SPEC_EXTENSION = '.spec.yaml'
const MARKDOWN_SPEC_EXTENSION = '.spec.md'

export let packagePath: string | undefined
export let autoPackagePath: string | undefined

export function setPackagePath(path: string | undefined): void {
  packagePath = path
}

export function setAutoPackagePath(path: string | undefined): void {
  autoPackagePath = path
}

export function findEnclosingPackageLibrary(startDir: string): string | undefined {
  let dir: string | undefined = resolve(startDir)
  while (dir) {
    const config = readConfig(dir)
    if (config && 'Package info' in config) {
      return dirname(dir)
    }
    const parent = dirname(dir)
    if (parent === dir) break
    dir = parent
  }
  return undefined
}

export function findPackage(name: string): string | undefined {
  for (const libDir of searchPath()) {
    const candidate = join(libDir, name)
    try {
      if (statSync(candidate).isDirectory() && isPackage(candidate, name)) {
        return candidate
      }
    } catch {
      // Directory not accessible
    }
  }
  return undefined
}

function searchPath(): string[] {
  const paths: string[] = []

  if (autoPackagePath) {
    paths.push(autoPackagePath)
  }

  if (packagePath) {
    paths.push(packagePath)
  }

  const envPath = process.env['SPECSCRIPT_PACKAGE_PATH']
  if (envPath) {
    for (const p of envPath.split(':')) {
      if (p.trim()) paths.push(p.trim())
    }
  }

  const defaultDir = join(homedir(), '.specscript', 'packages')
  if (existsSync(defaultDir)) {
    paths.push(defaultDir)
  }

  return paths
}

function isPackage(dir: string, expectedName: string): boolean {
  const config = readConfig(dir)
  if (!config) return false
  return 'Package info' in config
}

function readConfig(dir: string): JsonObject | undefined {
  const configPath = join(dir, 'specscript-config.yaml')
  try {
    const content = readFileSync(configPath, 'utf-8')
    const parsed = parseYaml(content)
    if (parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed)) {
      return parsed as JsonObject
    }
  } catch {
    // No config or not readable
  }
  return undefined
}

export function scanCommands(
  packageDir: string,
  items: ImportItem[],
): Map<string, CommandHandler> {
  const commands = new Map<string, CommandHandler>()

  for (const item of items) {
    switch (item.kind) {
      case 'command': {
        const file = join(packageDir, `${item.path}${YAML_SPEC_EXTENSION}`)
        if (existsSync(file)) {
          const baseName = fileToCommandName(`${item.name}${YAML_SPEC_EXTENSION}`)
          const name = item.alias ?? baseName
          if (name) commands.set(canonicalName(name), createFileCommandHandler(name, file))
        }
        break
      }

      case 'name':
        addNameImport(commands, packageDir, item.value, item.alias)
        break

      case 'directory': {
        const dir = join(packageDir, item.path)
        if (isDirectory(dir)) {
          addDirectoryCommands(commands, dir)
        }
        break
      }

      case 'wildcard': {
        const dir = item.path ? join(packageDir, item.path) : packageDir
        if (isDirectory(dir)) {
          if (item.recursive) {
            addRecursiveCommands(commands, dir)
          } else {
            addDirectoryCommands(commands, dir)
          }
        }
        break
      }
    }
  }

  return commands
}

export function scanLocalCommands(
  configDir: string,
  localPath: string,
  items: ImportItem[],
): Map<string, CommandHandler> {
  const resolvedPath = localPath.replace(/^\.\//, '')
  const dir = resolve(configDir, resolvedPath)
  const commands = new Map<string, CommandHandler>()

  for (const item of items) {
    switch (item.kind) {
      case 'command': {
        const file = join(dir, `${item.name}${YAML_SPEC_EXTENSION}`)
        if (existsSync(file)) {
          const baseName = fileToCommandName(`${item.name}${YAML_SPEC_EXTENSION}`)
          const name = item.alias ?? baseName
          if (name) commands.set(canonicalName(name), createFileCommandHandler(name, file))
        }
        break
      }

      case 'name':
        addNameImport(commands, dir, item.value, item.alias)
        break

      case 'wildcard':
        if (item.recursive) {
          addRecursiveCommands(commands, dir)
        } else {
          addDirectoryCommands(commands, dir)
        }
        break

      case 'directory': {
        const subDir = join(dir, item.path)
        if (isDirectory(subDir)) {
          addDirectoryCommands(commands, subDir)
        }
        break
      }
    }
  }

  return commands
}

function addNameImport(
  commands: Map<string, CommandHandler>,
  baseDir: string,
  name: string,
  alias?: string,
): void {
  const asFile = join(baseDir, `${name}${YAML_SPEC_EXTENSION}`)
  if (existsSync(asFile)) {
    const baseName = fileToCommandName(`${name}${YAML_SPEC_EXTENSION}`)
    const commandName = alias ?? baseName
    if (commandName) {
      commands.set(canonicalName(commandName), createFileCommandHandler(commandName, asFile))
    }
    return
  }

  const asDir = join(baseDir, name)
  if (isDirectory(asDir)) {
    addDirectoryCommands(commands, asDir)
  }
}

function addDirectoryCommands(
  commands: Map<string, CommandHandler>,
  dir: string,
): void {
  if (!isDirectory(dir)) return

  try {
    for (const entry of readdirSync(dir)) {
      if (!entry.endsWith(YAML_SPEC_EXTENSION) && !entry.endsWith(MARKDOWN_SPEC_EXTENSION)) continue
      const filePath = join(dir, entry)
      if (statSync(filePath).isDirectory()) continue

      const commandName = fileToCommandName(entry)
      if (!commandName) continue
      commands.set(canonicalName(commandName), createFileCommandHandler(commandName, filePath))
    }
  } catch {
    // Directory not readable
  }
}

function addRecursiveCommands(commands: Map<string, CommandHandler>, dir: string): void {
  if (!isDirectory(dir)) return

  addDirectoryCommands(commands, dir, undefined)

  try {
    for (const entry of readdirSync(dir)) {
      const fullPath = join(dir, entry)
      if (isDirectory(fullPath) && !isExcluded(entry)) {
        addRecursiveCommands(commands, fullPath)
      }
    }
  } catch {
    // Directory not readable
  }
}

function isExcluded(dirName: string): boolean {
  if (dirName === 'tests') return true
  // Check for hidden flag in config
  const config = readConfig(dirName)
  return config?.['hidden'] === true
}

function isDirectory(path: string): boolean {
  try {
    return statSync(path).isDirectory()
  } catch {
    return false
  }
}
