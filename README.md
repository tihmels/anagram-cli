# anagram-cli

An interactive Kotlin command line application that answers two questions:

1. **Are these two texts anagrams of each other?**
2. **Which of the texts I entered earlier are anagrams of this one?**

Every text handed to feature 1 enters an in-memory history, whether or not the comparison
succeeded. Feature 2 searches that whole history, which is what makes texts find each other even
when they were never compared directly:

```
compare(listen, silent)     ->  yes
compare(listen, banana)     ->  no
compare(listen, enlist)     ->  yes

find(listen)                ->  [silent, enlist]
find(silent)                ->  [listen, enlist]      # never compared with enlist
find(banana)                ->  []
```

The history lives in memory for the duration of one run and is deliberately not persisted.

## Requirements

A JDK 17 or newer. Nothing else — the Maven Wrapper downloads its own Maven.

## Build, test, run

```bash
./mvnw verify                       # compile, run all tests, package
java -jar target/anagram-cli.jar    # run
```

`verify` produces a self-contained jar, so running it needs only a JVM. If you prefer not to
package first:

```bash
./mvnw compile exec:java
```

To run only the tests: `./mvnw test`.

## Example session

```
$ java -jar target/anagram-cli.jar
Anagram tool. Type 'help' for the available commands, 'quit' to exit.
> compare
First text:  Dormitory
Second text: Dirty Room!
Anagrams: yes
> compare
First text:  listen
Second text: banana
Anagrams: no
> compare
First text:  listen
Second text: Enlist!
Anagrams: yes
> find
Text: silent
2 match(es):
  - listen
  - Enlist!
> find
Text: banana
No matches.
> quit
Bye.
```

Commands are `compare` (or `1`), `find` (or `2`), `help` and `quit` (or `exit`). Each command asks for its
texts on separate lines rather than parsing them off one line, because texts may contain spaces
and there is no unambiguous separator that a text could not itself contain.

## The text contract

"Anagram" is only well defined once you say which characters matter and when two characters count
as the same. That decision is a contract, so it lives in exactly one place — `AnagramText.of` —
and every rule below is pinned by a named test. Nothing else in the codebase applies string rules
of its own.

A raw text is normalized by:

1. **Unicode NFC composition**, so a decomposed and a precomposed spelling of the same character
   compare equal (`cafe` + combining acute equals `café`). NFC also puts combining marks into
   canonical order, so the order they were typed in does not matter.
2. **Lowercasing with `Locale.ROOT`**, so results never depend on the machine's default locale.
   A locale-sensitive lowercase would turn `I` into a dotless `ı` under a Turkish locale. NFC is
   applied a second time afterwards, because case mapping can decompose what the first pass
   composed.
3. **Keeping letters, digits and combining marks.** Whitespace, punctuation and symbols are
   ignored, and NFC composes once more after they are discarded — a mark separated from its base
   by punctuation (`cafe,´`) has to end up composed the same as one with nothing in between
   (`café`), since the punctuation between them was never meant to matter.
4. **Iterating over code points, not UTF-16 chars**, so characters outside the Basic Multilingual
   Plane count as one character and never get split.

Three consequences worth stating explicitly, because all three are choices rather than facts:

- **Diacritics are preserved.** `café` and `cafe` are *not* anagrams. Folding `é` onto `e` is a
  language-dependent decision this application does not make on the user's behalf.
- **Combining marks are preserved.** In many scripts a mark is not decoration but part of the
  character: dropping U+093E would make the Devanagari `का` indistinguishable from `क`, and an
  accent with no precomposed form would silently fold `ạ̈` onto `ạ`. The cost of this choice is
  that `İ` (U+0130) lowercases to `i` plus a combining dot that has no precomposed form, so `İ`
  and `i` are different characters here.
- **Digits are significant.** `abc12` and `abc123` are not anagrams.

A text that contains no letter or digit at all carries no anagram information and is **rejected at
the boundary** — including one made only of combining marks, which normalizes to something
non-empty but has nothing of its own to modify. The CLI reports it and carries on; the domain therefore only ever holds values
that are already valid and never has to defend itself against impossible input.

Two texts are anagrams when their normalized characters, sorted, are equal — where a character is
a base letter or digit together with any combining marks attached to it, not a bare code point.
Sorting code points individually would let a mark drift from one base character onto another and
call that a mere reordering, when the mark moving is what actually changed the text. That sorted
form is the *signature*, and grouping the history by it is what turns feature 2 into a lookup
rather than a scan over every stored text.

### A text is not an anagram of itself

An anagram rearranges the letters of a *different* word or phrase, so equal characters alone are
not enough — something has to have been rearranged. `compare` therefore reports three outcomes
rather than a yes/no:

```
compare(listen, silent)   ->  Anagrams: yes
compare(listen, Listen!)  ->  Anagrams: no — both inputs are the same text
compare(listen, banana)   ->  Anagrams: no
```

Sameness is judged on the normalized form, so `listen` and `LISTEN!` count as the same text. This
is the same rule feature 2 applies when it excludes the query from its own results; a `compare`
that answered "yes" here while `find` refused to list the text would be contradicting itself.
Keeping the "same text" case as its own outcome rather than folding it into `false` means the
caller can say *why* the answer was no.

## History semantics

The assignment leaves duplicates and ordering open. The choices made here, all of them covered by
tests:

| Question | Behaviour |
| --- | --- |
| What gets recorded? | Both texts of every `compare`, regardless of the outcome. |
| Does `find` record its query? | No. `find` is a pure read and can be asked about a text that was never entered. |
| Repeated inputs | A text is recorded once per normalized form, so repeating a comparison never multiplies later results. |
| Which spelling comes back? | The first one seen. Entering `Listen` and later `listen` reports `Listen`. |
| Does a query match itself? | No. Self-exclusion compares normalized forms, so `LISTEN!` will not return `listen`. |
| Is a text an anagram of itself? | No. `compare` reports it as the same text, matching feature 2's self-exclusion. |
| Result order | First-seen order, so the same sequence of commands always produces the same output. |

## Architecture

```
io.github.tihmels.anagram
├── domain
│   ├── AnagramText       validated input: the normalization contract, applied once
│   ├── AnagramSignature  canonical character multiset; the lookup key
│   ├── ComparisonResult  the three outcomes of comparing two texts
│   └── AnagramSession    the history and the two features
└── cli
    ├── AnagramCli        command loop, prompts, rendering
    └── AnagramApplication  wires stdin/stdout to the CLI
```

Six small types split across two packages, and the boundary between them is the point:

- The **CLI owns no anagram logic.** It never sees a signature and never inspects a normalized
  form. Its single domain responsibility is running raw input through `AnagramText.of`, which is
  where invalid text is turned away.
- The **domain owns no I/O.** `AnagramSession` can be tested completely without a terminal, and
  the CLI can be tested completely without one either, because its streams are constructor
  arguments rather than `System.in` and `System.out`.
- `AnagramSignature` is `internal`: it is an implementation detail of how the domain finds
  matches, and nothing outside has a reason to construct or read one.

There is no repository, no use-case layer, no dependency-injection container and no framework.
None of them would solve a problem this program has.

## Trade-offs

- **Sorted characters over a frequency map.** Both are valid canonical forms. Sorting is the one
  whose correctness is obvious at a glance, and a frequency map would need a defined iteration
  order before it could serve as a reliable map key. For texts of realistic length the difference
  in cost is not worth the loss of clarity.
- **The whole history is held in memory**, keyed by signature. Lookup is a hash lookup rather than
  a scan, but memory grows with the number of distinct texts entered. That is the right trade for
  a session-scoped interactive tool; a long-lived service would need eviction or storage.
- **No framework and one runtime dependency** (the Kotlin standard library). A framework would add
  startup cost, configuration and a dependency surface to a program whose entire domain is four
  types.
- **`AnagramText.of` returns `null` for invalid input** rather than throwing. There is exactly one
  failure mode — no letters or digits — so a nullable return states it in the type system without
  a result type or exception control flow in a loop that must not crash.
- **`AnagramSession` is not thread-safe.** One session belongs to one interactive run. Making it
  concurrent would be unused machinery.

## Tests

```bash
./mvnw test
```

Split by what they protect:

- `AnagramTextTest` — the normalization contract, rule by rule: casing, whitespace, punctuation,
  digits, NFC composition, the diacritics and combining-mark decisions, rejection of empty input,
  and Unicode handled per code point (including a supplementary-plane letter and an ordering case
  that a char-wise implementation would get wrong).
- `AnagramSessionTest` — the history semantics: both inputs recorded, transitive association, the
  scenario from the assignment, duplicates, self-exclusion, ordering and session isolation — plus a
  set of anagram pairs quoted directly from the
  [Wikipedia definition](https://en.wikipedia.org/wiki/Anagram) this program follows, covering
  multi-word phrases and punctuation the normalization contract has to see through.
- `AnagramCliTest` — the CLI driven end to end over in-memory streams. Most of these assert the
  **complete transcript** rather than a substring, because the program's promise is deterministic
  output and a substring check would not notice a duplicated, reordered or missing line.
