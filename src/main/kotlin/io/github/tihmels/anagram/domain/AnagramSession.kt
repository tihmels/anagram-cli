package io.github.tihmels.anagram.domain

/**
 * The in-memory history of one program run, and the two operations the assignment asks for.
 *
 * State semantics, all of them deliberate rather than inherited from a collection type:
 *
 *  - **What is stored:** every text handed to [compareAndRecord], whether or not the two texts
 *    turned out to be anagrams. That is what makes the transitive case work: two texts that were
 *    never directly compared still find each other through their shared signature.
 *  - **When it is stored:** only [compareAndRecord] writes. [findAnagrams] is a pure read, so a
 *    lookup can be repeated or run against a text that was never entered without changing the
 *    history.
 *  - **Duplicates:** a text is recorded once per normalized form. Re-entering the same text —
 *    or the same text in different spelling or casing — does not multiply later results, and the
 *    first-seen spelling is the one that is reported back.
 *  - **Self-exclusion:** [findAnagrams] never returns the query itself, compared by normalized
 *    form rather than by the entered spelling.
 *  - **Ordering:** matches come back in the order they were first recorded, so the same sequence
 *    of commands always produces the same output.
 *
 * Not thread-safe: one session belongs to one interactive run.
 */
class AnagramSession {

    private val recordedForms = HashSet<String>()
    private val bySignature = HashMap<AnagramSignature, MutableList<AnagramText>>()

    /**
     * Feature #1 — compares [first] and [second] and records both texts in the session history,
     * independently of the answer.
     *
     * Two texts that normalize to the same value are reported as [ComparisonResult.SAME_TEXT]
     * rather than as anagrams: an anagram rearranges the letters of a *different* text, and
     * nothing was rearranged here. This is the same rule [findAnagrams] applies when it excludes
     * the query from its own results.
     */
    fun compareAndRecord(first: AnagramText, second: AnagramText): ComparisonResult {
        record(first)
        record(second)
        return when {
            first == second -> ComparisonResult.SAME_TEXT
            first.signature == second.signature -> ComparisonResult.ANAGRAMS
            else -> ComparisonResult.NOT_ANAGRAMS
        }
    }

    /**
     * Feature #2 — all previously recorded texts that are anagrams of [query], excluding [query]
     * itself, in first-seen order. Does not record [query].
     */
    fun findAnagrams(query: AnagramText): List<AnagramText> =
        bySignature[query.signature]
            .orEmpty()
            .filter { it != query }

    private fun record(text: AnagramText) {
        if (recordedForms.add(text.normalized)) {
            bySignature.getOrPut(text.signature) { mutableListOf() }.add(text)
        }
    }
}
