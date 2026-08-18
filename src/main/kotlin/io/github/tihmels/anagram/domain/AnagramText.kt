package io.github.tihmels.anagram.domain

import java.text.Normalizer
import java.util.Locale

/**
 * A text that has passed the normalization contract and is therefore usable as domain input.
 *
 * The contract is applied exactly once, in [of]. Everything downstream works on [normalized]
 * and never re-derives its own rules from [display].
 *
 * Normalization contract:
 *  1. Unicode NFC composition, so that decomposed and precomposed spellings of the same
 *     character compare equal (`"café"` and `"café"` are the same text).
 *  2. Locale-independent lowercasing via [Locale.ROOT], so that the result never depends on
 *     the machine's default locale.
 *  3. Letters, digits and combining marks are significant. Whitespace, punctuation and
 *     symbols are ignored.
 *  4. Iteration happens over code points, not UTF-16 chars, so characters outside the Basic
 *     Multilingual Plane are treated as single characters.
 *
 * Diacritics are deliberately **not** stripped: `é` and `e` are different characters. Folding
 * them together would be a language-dependent decision that this application does not make.
 * Combining marks are kept for the same reason - see [isSignificant].
 *
 * One consequence worth knowing: U+0130 lowercases to `i` plus a combining dot that has no
 * precomposed form, so it is not the same character as a plain `i`. That falls out of keeping
 * marks rather than discarding them, and is covered by a test.
 *
 * A text whose normalized form is empty carries no anagram information and is rejected.
 */
class AnagramText private constructor(
    /** The text as first entered, whitespace-trimmed. Used for display only. */
    val display: String,
    /** The canonical form. Two texts are considered the same input when these are equal. */
    val normalized: String,
) {
    internal val signature: AnagramSignature = AnagramSignature.of(normalized)

    /** Identity follows the normalization contract, not the entered spelling. */
    override fun equals(other: Any?): Boolean = other is AnagramText && normalized == other.normalized

    override fun hashCode(): Int = normalized.hashCode()

    override fun toString(): String = display

    companion object {
        /**
         * Applies the normalization contract to [raw].
         *
         * @return the validated text, or `null` if [raw] contains no letter or digit.
         */
        fun of(raw: String): AnagramText? {
            val normalized = normalize(raw)
            return if (normalized.isEmpty()) null else AnagramText(raw.trim(), normalized)
        }

        private fun normalize(raw: String): String {
            // NFC runs twice, on purpose. The first pass gives the case mapping composed
            // characters to work on; the second recomposes what case mapping decomposed, since
            // some characters lowercase into a letter plus a combining mark
            // (U+0130 -> "i" + U+0307). NFC also puts combining marks into canonical order, so
            // the same character always yields the same code points regardless of typing order.
            val composed = Normalizer.normalize(raw, Normalizer.Form.NFC)
            val cased = Normalizer.normalize(composed.lowercase(Locale.ROOT), Normalizer.Form.NFC)

            val canonical = StringBuilder(cased.length)
            cased.codePoints()
                .filter(::isSignificant)
                .forEach(canonical::appendCodePoint)
            return canonical.toString()
        }

        /**
         * Letters, digits and combining marks carry meaning; everything else is layout.
         *
         * Combining marks have to be kept even though they are not letters. In many scripts they
         * are not decoration but part of the character: dropping U+093E would make the Devanagari
         * "का" indistinguishable from "क", and dropping a non-composable accent would silently
         * fold "ạ̈" onto "ạ".
         */
        private fun isSignificant(codePoint: Int): Boolean =
            Character.isLetterOrDigit(codePoint) || Character.getType(codePoint) in COMBINING_MARK_TYPES

        private val COMBINING_MARK_TYPES = setOf(
            Character.NON_SPACING_MARK.toInt(),
            Character.COMBINING_SPACING_MARK.toInt(),
            Character.ENCLOSING_MARK.toInt(),
        )
    }
}
