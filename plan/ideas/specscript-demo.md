# Demo SpecScript to a wider audience

I want to give a 5-minute demo of SpecScript to wider technical / semi-technical audience.

Points to hit:

* AI wants Spec-first
* This means a lot of Markdown
* We also want it to be more formal
* SpecScript has both Markdown (informal) and Yaml (formal)
* Yaml is used because it is declarative and readable.
* Example: HTTP POST with custom headers vs curl syntax
* You are not supposed to 'code' in SpecScript, you are supposed to 'spec' it. Yaml is trying to prevent you from being
  too clever like Cobol.
* You can spin up a mini command and it's a CLI
* But it is an MCP tool right away
* Let's spin up an MCP server
* A Spec is worthless if it can't be tested.
* Let's add some test
* We can 'run' a script but also 'test' it (spec <file> vs spec --test <file>)
* Connect to anything using Shell or MCP (todo: create plumbing to run mcp tools directly from SpecScript)
* SpecScript would be ideal for LLMs: it should be their language. Because: it's wordy, it's formal but prevents you
  going down the rabbit hole; it's testable and repeatable; it's composable.
* Interesting observation: LLMs prefer to use CLI interaction over MCP tool calling wants they discover you can do that!

It can be fast-paced (I don't want to labor the points) but should be clear and understandable. Prefer a single
narrative thread than throwing in the kitchen. sink. But we can fold in some features in the examples, like using db
storage in a matter-of-fact way, or credential management using Connect to

Objective 1:

* Slide skeleton for fast-paced demo. 20s per slide? 10s?
* Propose 3 narrative threads. They may meander but should be linear. Avoid introduction → feature → introduction →
  feature. Keep piling up. More escalting "If we have this (hat) , what about this (rabbit!)" over stagnant "We are
  going to do X - we do X - We have done X"
* Find the right structure / balance between
    - Slogan slides
    - Static code examples
    - Live demo from CLI or Agent UI interacting with it
* It's a wow-presentation not a status report