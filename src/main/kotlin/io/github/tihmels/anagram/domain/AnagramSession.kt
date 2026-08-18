package io.github.tihmels.anagram.domain

/**
 * The in-memory history of one program run, and the two operations the assignment asks for.
 *
 * Not thread-safe: one session belongs to one interactive run.
 */
class AnagramSession {

    // Equal normalized forms always produce equal signatures, so a repeated text can never land in
    // a different group and slip past the set. LinkedHashSet keeps the first-seen instance instead
    // of replacing it, which is what preserves the first-seen spelling and the result order.
    private val bySignature = HashMap<AnagramSignature, LinkedHashSet<AnagramText>>()

    /**
     * Feature #1 — records both texts, whatever the answer, and reports how they relate.
     *
     * Texts that normalize to the same value are [ComparisonResult.SAME_TEXT] rather than
     * anagrams, since an anagram rearranges a *different* text. [findAnagrams] applies the same
     * rule when it excludes the query from its own results.
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
     * Feature #2 — recorded texts that are anagrams of [query], in first-seen order, excluding
     * [query] itself. A pure read: it does not record [query].
     */
    fun findAnagrams(query: AnagramText): List<AnagramText> =
        bySignature[query.signature]
            .orEmpty()
            .filter { it != query }

    private fun record(text: AnagramText) {
        bySignature.getOrPut(text.signature) { LinkedHashSet() }.add(text)
    }
}
