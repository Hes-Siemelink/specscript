# Talk Deck — "Trust the Spec, Not the Code"

TED-style skeleton. Conversational, visual-led, provocative but not cheesy. Per slide: **Title** / **Main idea** (what
you say or what the slide asserts) / **Visual** (the image or motion behind it). Where the visual is strong enough, it
carries the point and the text shrinks to a few words or disappears.

This is over-built on purpose. More slides than a single talk needs — shuffle, skip, and use some as 5-second beats you
walk past without speaking.

---

## Act 1 — The uncomfortable truth

### Slide 1 — Cold open
- **Title:** *(none — black slide)*
- **Main idea:** One line, white on black: "Every program describes itself three times."
- **Visual:** Pure black. Just the sentence. Let it sit.

### Slide 2 — The three witnesses
- **Title:** Three descriptions, one program
- **Main idea:** Docs, Tests, Code — three accounts of the same system.
- **Visual:** Three musicians on stage, each reading from a *different* sheet of music. Same band, three scores.

### Slide 3 — Each owns a piece of the truth
- **Title:** *(none)*
- **Main idea:** Docs = intent. Tests = guarantees. Code = what actually happens.
- **Visual:** A Venn diagram of three circles — but the overlaps are smeared, edges blurred, not clean. The truth is spread across all three and no one owns the middle.

### Slide 4 — They drift
- **Title:** Given enough time, they disagree
- **Main idea:** The doc promises something no longer true. The test guards something nobody wants. The code grew a behavior nobody meant.
- **Visual:** The three musicians drifting out of sync — three tempos, cacophony lines rising off each. Motion or across clicks.

### Slide 5 — The question
- **Title:** *(none)*
- **Main idea:** "When they disagree — which one do you believe?"
- **Visual:** Three doors. No labels yet.

### Slide 6 — We always pick the same one
- **Title:** We trust the code
- **Main idea:** The code wins. It's what runs. Everything else is commentary.
- **Visual:** The band freezes; a spotlight picks out one musician — CODE — while the other two go dark. The soloist nobody can actually read.

### Slide 7 — The scandal
- **Title:** *(none)*
- **Main idea:** "We trust the description we can *least* read."
- **Visual:** A wall of dense, unreadable code — tiny, monospace, endless. A magnifying glass finds nothing legible.

### Slide 8 — Beat
- **Title:** *(none)*
- **Main idea:** "The one thing a human can't verify — is the one thing we decided to trust."
- **Visual:** Black. Single line. 5-second pause slide.

---

## Act 2 — The reframe

### Slide 9 — What if
- **Title:** What if they couldn't drift apart?
- **Main idea:** Because they stopped playing from three sheets.
- **Visual:** The three drifting musicians snap into rhythm — three scores merge into one shared sheet on a single stand.

### Slide 10 — One artifact, three jobs
- **Title:** Readable. Runnable. Self-checking.
- **Main idea:** A file you can read like a doc, that runs like a program, that fails like a test.
- **Visual:** One sheet of music with three glowing badges: an eye, a play button, a checkmark.

### Slide 11 — The name for it
- **Title:** The tune is the spec
- **Main idea:** When the band plays in tune, the music they make together has a name: the specification. A plain statement of what should be true.
- **Visual:** The word SPECIFICATION forming out of musical notation. Ghosted academic baggage nearby ("formal", "proof", "PhD") struck through.

### Slide 12 — The line
- **Title:** Trust the spec, not the code
- **Main idea:** Trust the music, not any one musician. *(say it, then stop talking)*
- **Visual:** Big. Centered. Nothing else.

### Slide 13 — What it does to the code
- **Title:** *(none)*
- **Main idea:** If the truth lives in the tune, any one player becomes replaceable.
- **Visual:** A musician walks offstage mid-song; a stand-in slides in; the music never stops.

### Slide 14 — Bus factor, for software
- **Title:** Nothing precious inside the mechanism
- **Main idea:** Healthy bands survive a member leaving. Healthy systems survive their code being rewritten.
- **Visual:** A touring van pulling away. (The bus-factor bus, on tour.) Understated, a little wry.

### Slide 15 — Say what, not how
- **Title:** Stop describing *how*. State *what*.
- **Main idea:** Develop in a declarative style. Say the outcome; let the mechanism be a detail.
- **Visual:** Left: a tangle of imperative arrows/steps. Right: a single clean statement. Split screen.

---

## Act 3 — Show it

### Slide 16 — It exists
- **Title:** This isn't hypothetical
- **Main idea:** A small language built entirely on the wager. SpecScript.
- **Visual:** Understated logo / plain title card. No fireworks.

### Slide 17 — The whole program
- **Title:** *(none)*
- **Main idea:** Show the two-line file. GET + Expected output.
- **Visual:** Just the code, large, syntax-highlighted, centered. Let them read it.

### Slide 18 — Read it three ways
- **Title:** Same file. Three jobs.
- **Main idea:** Run it → it does the thing. Test it → it checks the promise. Document with it → the doc can't lie.
- **Visual:** The same two-line file, three times across the slide, each with a different badge lighting up (play / check / eye).

### Slide 19 — Documentation that can't lie
- **Title:** *(none)*
- **Main idea:** If the doc drifts from reality, the build breaks.
- **Visual:** A page of prose with a live code block in it; a red "BUILD FAILED" stamp when the prose disagrees.

### Slide 20 — One artifact, many surfaces
- **Title:** The same file is also…
- **Main idea:** A CLI tool. An HTTP endpoint. A tool an AI agent can call.
- **Visual:** One file at the center, projecting into four screens: terminal, browser, API, robot. Rays from a single source.

### Slide 21 — "Isn't that doing too much?"
- **Title:** The breadth is the dividend
- **Main idea:** When you trust one artifact, you want it everywhere — without rewriting it.
- **Visual:** The objection as a speech bubble, then popped.

### Slide 22 — It bets on itself
- **Title:** Even the language is replaceable
- **Main idea:** SpecScript's own commands are specs — satisfied by two implementations (Kotlin, TypeScript), same tests. Delete either. Nothing moves.
- **Visual:** Two engines under one chassis; one gets pulled out, the car keeps driving.

---

## Act 4 — The honest edge

### Slide 23 — Where it stops
- **Title:** A spec is not an algorithm
- **Main idea:** Glue — wiring, routing, checking — states naturally. Hard computation does not.
- **Visual:** A clean pipe network (glue) meeting a dense black box (domain). The box stays a box.

### Slide 24 — You *can*, but…
- **Title:** You can write a web server in a shell script
- **Main idea:** You will regret it. Declarative languages orchestrate and verify; they delegate the heavy lifting.
- **Visual:** A shell prompt duct-taped to a server rack. Slightly absurd.

### Slide 25 — The natural edge
- **Title:** *(none)*
- **Main idea:** Keep the intent, the orchestration, the checks. Replace the machinery in the middle.
- **Visual:** The one durable file, with a swappable module slotted into its center.

---

## Act 5 — The bet

### Slide 26 — Who writes the replaceable part?
- **Title:** *(none)*
- **Main idea:** If the code is replaceable, the natural author is increasingly a machine.
- **Visual:** A human hand and a robot hand reaching toward the same file — the spec between them as contract.

### Slide 27 — The Agent Bet
- **Title:** The Agent Bet
- **Main idea:** The spec becomes the contract between a person and an agent — one both can read, one can verify.
- **Visual:** A handshake rendered as a signed document.

### Slide 28 — The honest part
- **Title:** Not settled
- **Main idea:** Models write specs that are correct more often than graceful. Whether that gap closes is genuinely open.
- **Visual:** A dashed line into fog. Don't over-illustrate. Let it be unfinished.

### Slide 29 — But the direction isn't in doubt
- **Title:** *(none)*
- **Main idea:** For decades we trusted what we could run over what we could read. We paid in drift.
- **Visual:** The out-of-sync musicians from Slide 4 return — as the "before."

### Slide 30 — Close
- **Title:** Burn the code, not the spec
- **Main idea:** A musician can leave the band. The tune is what you keep.
- **Visual:** A page of code catching fire — and one sheet of music, the spec, untouched beside it.

### Slide 31 — End card
- **Title:** Trust the spec, not the code
- **Main idea:** *(name, repo, one QR code)*
- **Visual:** Black. The line. Nothing else.

---

## Spare / alternate beats (pull in as needed)

- **"You wouldn't trust a map you can't read."** — analogy for trusting illegible code. Visual: a map in a language you don't speak.
- **"The doc was true. In 2021."** — drift, made concrete. Visual: a doc with a dust layer / expiry date.
- **"Three sources of truth is zero sources of truth."** — punchy restatement of Act 1. Visual: three clocks showing three times.
- **"Nobody's on top. They're a band."** — the dek, as a slide. Visual: three players, one sheet, no conductor.
- **"Legible to a human. Executable by a machine. Pick both."** — the fusion, restated.
- **"Code is cheap now. Intent isn't."** — the economic turn behind the Agent Bet.
- **Live demo slide** — edit the spec, watch the CLI, the test, and the endpoint all change at once. Visual: split screen, one edit ripples.
