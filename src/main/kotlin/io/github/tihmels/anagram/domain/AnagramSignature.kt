package io.github.tihmels.anagram.domain

/**
 * The canonical character multiset of a normalized text.
 *
 * Two texts share a signature exactly when they use the same characters the same number of times.
 * That is a statement about characters, not a verdict: whether the texts are *anagrams* is a
 * domain question, and [AnagramSession] additionally requires them to be different texts. Equal
 * signatures make the value usable as a lookup key, so grouping by it answers "which texts use
 * these characters" without comparing the query against every stored text.
 *
 * The representation is the normalized code points in ascending order. Sorting is the simplest
 * canonical form whose correctness is self-evident; a frequency map would need a defined
 * iteration order to be equally reliable as a key.
 *
 * Internal on purpose: signatures are a domain implementation detail and nothing outside the
 * domain has a reason to construct or read one.
 */
@JvmInline
internal value class AnagramSignature private constructor(private val sortedCodePoints: String) {

    companion object {
        /** @param normalized a text already put through the [AnagramText] normalization contract. */
        fun of(normalized: String): AnagramSignature {
            val canonical = StringBuilder(normalized.length)
            normalized.codePoints()
                .sorted()
                .forEach(canonical::appendCodePoint)
            return AnagramSignature(canonical.toString())
        }
    }
}
