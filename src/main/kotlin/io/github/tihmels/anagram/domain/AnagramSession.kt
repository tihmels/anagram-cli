package io.github.tihmels.anagram.domain

class AnagramSession {

    private val bySignature = HashMap<AnagramSignature, LinkedHashSet<AnagramText>>()

    /** Records both [first] and [second], regardless of whether they compare as anagrams. */
    fun compareAndRecord(first: AnagramText, second: AnagramText): ComparisonResult {
        record(first)
        record(second)
        return when {
            first == second -> ComparisonResult.SAME_TEXT
            first.signature == second.signature -> ComparisonResult.ANAGRAMS
            else -> ComparisonResult.NOT_ANAGRAMS
        }
    }

    /** Recorded texts that are anagrams of [query], in first-seen order, excluding [query] itself. */
    fun findAnagrams(query: AnagramText): List<AnagramText> =
        bySignature[query.signature]
            .orEmpty()
            .filter { it != query }

    private fun record(text: AnagramText) {
        bySignature.getOrPut(text.signature) { LinkedHashSet() }.add(text)
    }
}
