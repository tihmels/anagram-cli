package io.github.tihmels.anagram.domain

/**
 * The canonical character multiset of a normalized text: its code points, sorted.
 *
 * Equal signatures mean equal characters, not that the texts are anagrams — [AnagramSession]
 * additionally requires them to be different texts. Grouping by signature is what turns feature #2
 * into a lookup instead of a scan over everything stored.
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
