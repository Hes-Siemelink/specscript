# Spec right with Playwright

Do SpecScript on websites using Playwright.

The idea is to write SpecScript files, a mix between markdown and declarative commands in Yaml, that connect to a
website.

It's an alternative to write browser test files in Typescript using Playwright, but with a more declarative approach.

This could be useful in some use cases where conversational flow is more important than testing to the dot.

The idea is to skip Page Object Model and sit on the level of Playwright MCP, like LLMs do.

The question is to figure out what that would look like.

## Different levels

SpecScript is designed to be as close to "say what you want to do" as possible, it's a declarative approach. Then you
can process or test the result, also in a declarative way.

For example, to do an HTTP request and check the result, you can write:

```yaml specscript
GET: https://example.com

Expected output: Hello world!
```

This is valid SpecScript and it runs. (no LLM required)

For websites, I honestly don't know what this would look like.

But we can start with natural language:

```markdown
Navigate to http://release.digital.ai.local:5516, log in as user "admin" with password "admin", click on the "Releases"
link, and check the list of releases contains "Welcome Release Administrator".
```

My assumption is that LLMs can figure this out using Playwright MCP. (Try it!)
But this consumes tokens and there is a lot of back and forth to figure out the page, etc.

On the other end of the spectrum, we have the Playwright API and Page Object model, but then you are knee-deep in
browser elements and anchors and selectors. (Imagine the code yourself)

Now SpecScript would sit somewhere in the middle, where you have some structure, but are not too far down the rabbit
hole of selectors and page objects.

```yaml specscript
Navigate to: http://release.digital.ai.local:5516
Submit form:
  id: login-form
  properties:
    User: admin
    Password: admin
  button: Login
---
Click button: Releases

Find element:
  selector: "h1"
  text: "Welcome Release Administrator"
```

This is absolutely not the final syntax, but it gives an idea of what it could look like.

Questions:

* As an LLM, please replay the above scenario with Playwright MCP and record all steps
* Playwright serves the Yaml pages in some way. It contains accessibility references, like ` [ref=e33] `. Where do they
  come from? Are they static? Does Playwright just invent them on the fly?
* There is a challenge maybe that Playwright MCP is designed to be efemeral for LLM sessions; where SpecScript files
  have a longer lifecycle. Tell me more about Playwright MCP philosophy, it looks a bit more fluid than your typical
  API-exposed-as-MCP but I don';'t know
* Suggest a syntax for SpecScript that is more declarative than Playwright API, but still allows to do the same things.
  It should be easy to read and write, and not too verbose. It should be able to handle navigation, form submission,
  clicking buttons, finding elements, and checking their properties. Give three simple examples, don't overthink it.


