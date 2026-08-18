# AGENTS.md

Interactive Kotlin CLI: compares two texts for anagram equivalence and looks up anagrams among
texts entered earlier in the same run.

## Project stack

- Kotlin 2.4.10 on the JVM, compiled to Java 17 bytecode. A newer local JDK is fine.
- Maven via the wrapper (`./mvnw`). Never invoke a system `mvn`.
- JUnit 6 (Jupiter) for tests, surefire 3.5.6.
- Production code is **standard library only**. `kotlin-stdlib` is the single runtime dependency.
- No framework, no persistence, no I/O beyond stdin/stdout. History is in-memory, per run.

## Build and test commands

```bash
./mvnw verify                          # compile, all tests, shaded jar  <- use before every commit
./mvnw test                            # tests only
./mvnw test -Dtest=AnagramTextTest     # one test class (nested classes run too)
java -jar target/anagram-cli.jar       # run (requires verify/package first)
./mvnw compile exec:java               # run without packaging
```

Drive the CLI non-interactively by piping lines, one per prompt:

```bash
printf 'compare\nlisten\nsilent\nfind\nlisten\nquit\n' | java -jar target/anagram-cli.jar
```

## Code style

- `-Werror` is on. A Kotlin warning fails the build; fix it rather than suppressing it.
- Comments explain *why*, never *what*. Domain types carry KDoc stating their contract; that KDoc
  is the specification, so update it in the same change as the behaviour.
- Public domain API returns explicit types over booleans when there is a third case to report.
  `compareAndRecord` returns `ComparisonResult`, not `Boolean`, so `SAME_TEXT` stays distinct from
  `NOT_ANAGRAMS`.

**Write Unicode in tests as `\uXXXX` escapes, never as literal characters.** Editors and tooling
silently re-normalize source files, which once turned an NFC test into a tautology comparing two
identical strings:

```kotlin
val precomposed = "caf\u00E9"   // correct: intent survives a re-save
val decomposed = "cafe\u0301"
// wrong: "café" and "café" look different but can be saved byte-identical
```

## Architecture constraints

```
io.github.tihmels.anagram
├── domain   AnagramText · AnagramSignature · ComparisonResult · AnagramSession
└── cli      AnagramCli · AnagramApplication
```

- **The normalization contract lives only in `AnagramText.of`.** Never lowercase, trim, strip or
  compare strings anywhere else. If a rule needs changing, change it there and update the tests
  and the README table that pin it.
- **The CLI owns no anagram logic.** It may call `AnagramText.of` to validate input; it must not
  read `normalized` or touch `AnagramSignature`.
- **The domain owns no I/O.** `AnagramCli` takes a `Reader` and `Writer` by constructor — keep it
  that way so tests never need a terminal.
- `AnagramSignature` is `internal`. Keep it that way.
- Feature 1 records *both* inputs regardless of outcome; feature 2 is a pure read and must never
  record its query. These two rules are the whole point of the exercise.
- CLI output is asserted as complete transcripts. Changing any prompt or message means updating
  `AnagramCliTest` — that is intended friction, not a broken test.

## Boundaries

- Do not add dependencies, frameworks, DI containers, persistence, logging libraries or
  concurrency. This is a session-scoped CLI; extra machinery is a defect here.
- Do not add abstraction layers (repositories, use cases, ports/adapters) without a concrete
  problem they solve.
- Do not edit `target/` — it is generated and gitignored.
- Do not add a LICENSE without asking; the repo is deliberately unlicensed for now.
- Keep the README's normalization and history-semantics sections in sync with the code. They are
  the submission's contract, not decoration.

## Git workflow

- Conventional commits: `type(scope): imperative summary`. Atomic — one logical change each.
- Feature branches only. Never commit or push directly to `main`; never merge a PR.
- Stage named paths, not `git add -A`.
- No AI-authorship trailers in commit messages.

## Environment gotcha

A `GITHUB_TOKEN` in the environment overrides `gh`'s stored accounts and resolves this repo to the
**wrong GitHub account**. Prefix every `gh` and `git push` call:

```bash
env -u GITHUB_TOKEN -u GH_TOKEN gh <args>
```

A `pre-push` hook enforces this; if a push is refused, do not work around it by unsetting the hook.
