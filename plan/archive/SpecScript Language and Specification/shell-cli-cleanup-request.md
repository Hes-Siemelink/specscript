Couple of things to clean up in the are of running cli scripts from within markdown files.

## 1. SCRIPT_HOME or PWD?

Currently ```shell cli runs from the current directory. This seems wrong, because you typically run known cli files
relative to the current script, or ones that you have just created through a temp file. So it would make more sense to
run from the directory of the current script, which is what SCRIPT_HOME is for. So I propose to change the default
working directory of cli to SCRIPT_HOME.

The reason why it's not done is that it's ```shell cli in yaml, although it runs specscript.commands.shell.Cli and not
Shell. But the shell command may make more sense to run stuff in the current working directory, as a sort of bash
replacement working on local files.

But count the scripts that need to set cd=${SCRIPT_HOME} to run, how it is incongruent with how links work in markdown.

That takes me to the second point:

## 2. Rename ```shell cli to just ```cli or ```spec cli

The idea was to have 'shell syntax highlighting' for the markdown render and
```shell cli  would be a special case of just ```shell.

I guess ```cli would render just fine without the special shell syntax, just as 'code block'.

## 3. Introduce ${PWD} variable

Would make sense to introduce a PWD variable that always points to the current working directory, so you can make it
explicit where to run, ${SCRIPT_HOME} or ${PWD}.

## 4. ```shell should default to SCRIPT_HOME as well?

This is a bit more controversial, because it would make it less of a bash replacement, but it would be more consistent
with how links work in markdown. If you want bash-style behavio,r you can use cd=${PWD} which is excplicit and easy to
remember.