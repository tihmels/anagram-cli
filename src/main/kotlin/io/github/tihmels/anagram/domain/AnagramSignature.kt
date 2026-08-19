package io.github.tihmels.anagram.domain

import java.text.BreakIterator
import java.util.Locale

/**
 * The canonical character multiset of a normalized text: its grapheme clusters (a base letter or
 * digit plus any combining marks attached to it), sorted.
 *
 * Equal signatures mean equal characters, not that the texts are anagrams — [AnagramSession]
 * additionally requires them to be different texts.
 */
@JvmInline
internal value class AnagramSignature private constructor(private val sortedClusters: String) {

    companion object {
        // Safe as a delimiter only because AnagramText.normalize strips all whitespace before a
        // signature is ever built, so no cluster can contain it and collide with a boundary.
        private const val CLUSTER_SEPARATOR = " "

        /** @param normalized a text already put through the [AnagramText] normalization contract. */
        fun of(normalized: String): AnagramSignature {
            val key = graphemeClusters(normalized).sorted().joinToString(CLUSTER_SEPARATOR)
            return AnagramSignature(key)
        }

        private fun graphemeClusters(text: String): List<String> = buildList {
            val boundaries = BreakIterator.getCharacterInstance(Locale.ROOT).apply { setText(text) }
            var start = boundaries.first()
            var end = boundaries.next()
            while (end != BreakIterator.DONE) {
                add(text.substring(start, end))
                start = end
                end = boundaries.next()
            }
        }
    }
}
