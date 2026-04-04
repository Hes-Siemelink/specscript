# Properly document one example -- set the template for all

_As a SpecScript language maintainer, I want to provide fully documented examples that also can be tested with the
built-in test runner as a best practices template for users, so that specscript users know how to write their SpecScript
implementations._

## Starting small: release list

The `release list` command is a good candidate for a fully documented example, since it's relatively simple scope but
has some real world challenges, like interacting with a third party server.

- Location: /samples/digitalai/release/folders

The 'folders' sample is a part of the examples that show how to interact with the Digital.ai Release API.

It has simple commands to list folders and an interactive one to move folders.

## The main README

Currently, it has a README that explains how to use it.

* Goal 1: Check if the README is complete for all commands in the directory.
* Pattern: Use a main README as the 'executable spec' of a directory.

## Running tests for a 3rd party server

Problem with current README is that we can't reliably add code examples that test, because we rely on a third party
server with unpredictable data.

* Goal 2: Start a mock server in the background that helps us test the examples
* Pattern: Show how to set up a mock server

Hints: you can use the recording proxy pattern to record interactions with the real server and then use those recordings
to create a mock server that responds with the same data. This way, you can have predictable responses for your tests
while still using real data from the third party server. See samples/http-server/simple-proxy.

There is a real Release server running at http://localhost:5516 that can be used for experiments.

Use samples/digitalai/release/credentials to set up a proxy account and use it as default credentials for the tests.
This way, we can have a stable set of data to work with for our examples and tests.

Suggestion: place mock server code in the 'tests' directory Decision point: Use an all-in-one server defintion, or split
API definition from mock output data files.

## Interactive examples

Current 'move' examples are untestable because they interactive and play with a real server. With the mock server in
place, input becomes predictable and we can provide 'Answers' for the interactive examples.

* Goal 3: make interactive examples testable
* Pattern: Use mock server and Answers

## General requirements

* The README should read as a friendly tutorial.
* Use comment blocks for set up in tear down in README. Keep it simple
* Additional tests (like for edge cases) should go into the 'tests' directory (if they are needed).
* The SpecScript user that wants to set up README for their own projects should get a good understanding of how
  everything hangs together by reading the markdown of this README and looking at the code examples.

## Final outcomes

* A reference-grade README for the 'folders' sample that can be used as a template for other samples.
* A generalized Agent Skill for writing a README with testable examples and mock server setup, that can be reused by
  other sample projects.