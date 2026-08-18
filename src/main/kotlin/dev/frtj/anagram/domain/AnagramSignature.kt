package dev.frtj.anagram.domain

/**
 * The canonical character multiset of a normalized text.
 *
 * Two texts are anagrams of each other exactly when their signatures are equal, which makes the
 * signature usable as a lookup key: grouping by it answers "which texts are anagrams of this
 * one" without comparing the query against every stored text.
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
