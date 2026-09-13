# Trust the Spec, Not the Code

*Who's on top or do we play together? Making Code, Tests and Docs work as a team.*

Open almost any software project and you will find it describing itself three times. There is the documentation, which
tells you what the program is supposed to do. There are the tests, which insist on a narrower set of things it must
never stop doing. And there is the code, which is what the machine actually runs. Three accounts of one system, written
in three registers, maintained by three different impulses.

Each owns a piece of the truth. The documentation holds the intent — why the thing exists, what it promises. The tests
hold the guarantees — the corners that have burned someone before. The code holds the behavior as it will really happen,
down to the edge cases nobody wrote down. The trouble is that the boundaries between these three are unmarked, and they
move. A promise in the documentation quietly stops being true. A test passes while asserting something nobody cares
about anymore. The code grows a behavior that was never anyone's intent. No line divides what each is responsible for,
so when they disagree — and given enough time they always disagree — there is no principled way to say which one is
right.

In practice we resolve the argument the same way every time. We trust the code. The code wins because the code is what
runs; everything else is commentary. This is a strange thing to have settled on, because the code is also the least
legible of the three. It is the hardest artifact to read intent from, the one where a human is least able to glance and
confirm *yes, that is what we meant*. We have agreed to place our trust in precisely the description we can least
verify.

## The idea

Suppose the three accounts could not drift apart, because they were the same artifact.

Imagine a single file that reads like documentation — a person can look at it and confirm it says what they intended —
and that also runs, so it is not a description of the system but the system itself, and that also fails loudly the
moment reality diverges from it, the way a test does. One artifact, legible and executable and self-checking at once.
There is no gap between the intent, the guarantee, and the behavior, because there is no longer any space between them
for a gap to form.

Call that artifact a specification, in the plain sense: a statement of what should be true. Notice what has happened to
the three witnesses. They have stopped competing to be the source of truth. Code, tests, and docs are no longer three
players elbowing for the top chair — they are a band, and the tune they play together is the spec. The proposition is
that you should put your trust in the music, and not in any one musician. Trust the spec, not the code.

This sounds like a slogan until you notice what it does to the code. If the specification is where the truth lives, then
the implementation underneath it becomes *replaceable* — in the way that, on a healthy team, no single person is
irreplaceable. Bus factor, but for software. You want code that can be rewritten, ported, regenerated, or handed to
someone new without ceremony, because nothing precious is stored in it that isn't also stated, plainly and verifiably,
somewhere more legible. Code that hoards intent inside its mechanism is a liability, however clever. Code you can throw
away and recreate is a sign the intent was captured somewhere safer.

That is the deeper move behind the slogan: stop describing *how* the machine should proceed, step by step, and start
stating *what* should be true. Develop in a declarative style. Say the outcome; let the mechanism be a detail that any
competent hand — or, increasingly, any competent model — can supply and resupply.

## An example of the principle

None of this is hypothetical. There is a small language, SpecScript, built entirely around the wager.

A SpecScript file is written in YAML and Markdown. Here is one, complete:

```yaml
GET: https://api.example.com/hello?name=Ada
Expected output: Hi Ada!
```

Two lines. The first states an intention — make this request. The second states what must be true of the result. Run the
file and it performs the request. Run it in test mode and it checks the promise and fails if the world has changed.
Publish the same file inside a Markdown document and the prose around it becomes documentation while the block stays
executable, so the documentation cannot lie without breaking the build. The document, the test, and the program are not
three files kept laboriously in agreement. They are one file that cannot disagree with itself.

From there the same artifact projects into whatever shape you need. The identical file can be a command-line tool, with
help text and prompts generated from its inputs; an HTTP endpoint; or a tool an AI agent can call. This is usually
described as the language doing an alarming amount, which invites the obvious worry about focus. But the breadth is the
dividend, not the design. When there is one artifact you actually trust, you want it to appear everywhere you work
without being rewritten. The many roles are surfaces of a single verified intent.

The language even makes the wager on itself. Its own building blocks — the `GET` above, and the rest — are defined by
specifications, and those specifications are satisfied by two independent implementations, one in Kotlin and one in
TypeScript, each checked against the same several hundred executable examples. Neither implementation is the real one.
Either could be deleted and rebuilt against the spec, and nothing anyone relies on would move. The specification is what
endures. The code is, deliberately, replaceable.

## Where it stops

A declarative style does not absorb everything, and the honest version of this argument says so. Some work is wiring:
routing a value from one step to the next, making a request, exposing a result. A specification expresses that kind of
glue naturally, because glue is mostly a statement of what connects to what. But other work is genuine computation — the
hard interior logic of a real application — and stating *what should be true* is no substitute for the algorithm that
makes it so. You can write a web server in a shell script, too. You will regret it. Declarative languages are at their
best when they orchestrate and verify, and reach out to something purpose-built for the heavy lifting in the middle.

So the file you keep holds the intent, the orchestration, and the checks; the machinery it calls out to is the part you
were always willing to replace. The boundary is not a weakness in the idea. It is the idea drawn to its natural edge.

## The Agent Bet

The most interesting claim is also the least settled. If intent lives in a legible, checkable specification and the
implementation is replaceable, then the natural author of that replaceable implementation is increasingly a machine —
and the specification becomes the contract between a person and an agent, a thing both can read and one can verify. In
practice this works better in theory than on the screen. Models today produce specifications that are correct more often
than they are graceful. Whether that gap closes, and how, is genuinely open.

But the direction is not in much doubt. For decades we have trusted the artifact we could run over the artifact we could
read, and paid for it in drift, in documentation nobody believes, in tests that guard the wrong things. The alternative
is to make the thing you can read the same as the thing you can run, and to move your trust there. A musician can leave
the band; the tune is what you keep. Say what should be true. Burn the code, keep the spec.
