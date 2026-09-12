# Contributing to Tallybook

Bug reports and pull requests are welcome in the [issue tracker](https://github.com/herrerad85/tallybook/issues). Current direction and open work live in the pinned [roadmap issue](https://github.com/herrerad85/tallybook/issues/15).

People keep years of their own financial records in this app, and it runs on phones going back to Android 7. A change that works once, on one modern phone, in portrait, has not been tested yet.

## Using AI to write a change

AI written pull requests are welcome here. There is nothing to hide and nothing to announce. The bar is the same as for any other patch, and the work of getting a change over that bar is yours, not the model's.

- Read every line you are about to submit and be able to explain why it is there. If you cannot explain a line, delete it and find out what it was for.
- A model states a cause with the same confidence whether it traced one or not. Trace the mechanism yourself before you write it into a pull request body, a commit message or a code comment. A commit message is a claim, and a wrong one sends the next person down the wrong path.
- Watch for the things models invent: a file path that is not in the tree, a method nobody calls, an API that does not exist on API 24, a test that passes because it asserts the code's current behavior instead of the correct one.
- "It should be a small fix" is a prediction, not a finding. When the change looks smallest, check it hardest.

## Cold read your own diff

Getting a change to work only answers the easy question. Before you open the pull request, read the complete diff again as a stranger would, someone with no memory of how the code got that way and no investment in the design being right. That is the cold read.

The question to ask is not whether the change is correct.

- **What did the change make newly possible?** A new nullable is a new crash path. A new early return skips cleanup that used to run. Work moved off the main thread opens a lifecycle race that could not exist before.
- **Does the codebase already do this?** Look for the existing helper before writing a second one a few files over.
- **Every caller, not just the one in the issue.** Grep the function you touched.

A second model that did not write the code finds things the one that wrote it cannot see, so give the diff to a fresh session with no history of the work and tell it to break the change. What it reports is a candidate, not an order. Verify each finding in the code yourself, because a confident wrong finding produces a fix for a defect that was never there. Fixes written in response to a cold read get their own cold read, and you stop when a pass finds nothing or changes nothing.

## Testing

Build and run the unit tests:

```
./gradlew clean testFlossOsmDebugUnitTest assembleFlossOsmDebug lintFlossOsmDebug
```

Pass `clean`, the way CI does. An up to date task is skipped, so a green test count can be one that predates your edit.

The same command runs in CI on every pull request. The instrumented tests are compiled there but only run once a week, on one emulator at API 35, so a change touching the database or the UI needs you to run it on a device yourself.

Run the app, on an emulator or a phone, on both ends of the range:

- **API 24, Android 7.0.** This is `minSdk`, and it is a real floor, not a formality. Newer APIs, newer library behavior and newer syntax silently compile and then fail there.
- **A current Android.** The app targets API 35, where permissions, storage and edge to edge behavior all differ from the floor.

Rotation and theming have both broken screens in this app before.

- **Landscape.** Rotate every screen your change touches, including with a dialog open and with half typed text in a field. A rotation destroys and recreates the activity, so anything held only in memory is gone unless it was saved.
- **Dark theme**, and a couple of the accent themes if you changed anything visual.
- **The reported input.** Reproduce the original bug first, so you know it is real and you know the steps. Then run those same steps on your build. A passing unit test is not the same evidence as the bug being gone.

Screenshots help on anything visual, before and after. Use invented amounts and category names, never a real ledger.

## Lint

Lint runs in CI with `abortOnError true`, so an error fails the build. Warnings do not fail it, and warnings your change introduces are still yours to fix.

`app/lint-baseline.xml` records what was already there before a check was turned on. It is not permission to add more. Do not regenerate it to make a new warning disappear.

## The change itself

- **Fix it where every caller goes through.** A report names one path. Patching only that path leaves every sibling caller broken, and one guard in the shared function is a smaller change than a guard in each caller.
- **Read the history of a line before you change it.** `git blame` and `git log -L <line>,<line>:<path>` take seconds and name the commit and the issue that put the line there. Code that looks arbitrary, a guard, a disabled default, a magic number, usually has a crash behind it. Layouts and resource files carry decisions the same way source does.
- **One concern per pull request.** Unrelated cleanup makes a change harder to review and harder to revert.
- **No new dependency for what a few lines do.** The `floss` flavor ships no proprietary services and F-Droid builds it from source, so anything added has to be free software and resolvable in that build.
- **Comment sparingly.** Explain why at the definition, not what at every call site.
- **License.** Tallybook is GPLv3 or later. Code adapted from another project has to be compatible with that, which GPLv2 only is not, and the pull request needs to say where it came from.

## The pull request

Say what was broken and how you know it is fixed, in plain words, with steps someone else can run. Every claim in the body should be one you checked. If something is untested, name it. "I did not test X" is a fine sentence, and a confident wrong one is not.

## Translating

Translating needs no tooling and no build. Copy `app/src/main/res/values/strings.xml` into a folder named for the language, such as `values-de`, translate the entries and open a pull request. Leave any entry you are unsure about in English, and check that longer translations do not clip the layout.
