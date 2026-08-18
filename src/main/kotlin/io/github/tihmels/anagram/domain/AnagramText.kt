package io.github.tihmels.anagram.domain

import java.text.Normalizer
import java.util.Locale

/**
 * A text that has passed the normalization contract in [of] and is safe to use as domain input.
 *
 * Normalization: NFC composition, [Locale.ROOT] lowercasing, keeping letters, digits and combining
 * marks while discarding everything else, iterated per code point rather than per UTF-16 char. See
 * the README for the full contract and the reasoning behind each rule.
 */
class AnagramText private constructor(
    /** The text as first entered, whitespace-trimmed. Used for display only. */
    val display: String,
    internal val normalized: String,
) {
    internal val signature: AnagramSignature = AnagramSignature.of(normalized)

    override fun equals(other: Any?): Boolean = other is AnagramText && normalized == other.normalized

    override fun hashCode(): Int = normalized.hashCode()

    override fun toString(): String = display

    companion object {
        /** @return the validated text, or `null` if [raw] contains no letter or digit. */
        fun of(raw: String): AnagramText? {
            val normalized = normalize(raw)
            if (normalized.codePoints().noneMatch(Character::isLetterOrDigit)) return null
            return AnagramText(raw.trim(), normalized)
        }

        private fun normalize(raw: String): String {
            val composed = Normalizer.normalize(raw, Normalizer.Form.NFC)
            val cased = Normalizer.normalize(composed.lowercase(Locale.ROOT), Normalizer.Form.NFC)

            val canonical = StringBuilder(cased.length)
            cased.codePoints()
                .filter(::isSignificant)
                .forEach(canonical::appendCodePoint)

            // Discarding punctuation can newly place a base character and a mark next to
            // each other, so composition needs a pass after the filter too, not just before it.
            return Normalizer.normalize(canonical.toString(), Normalizer.Form.NFC)
        }

        private fun isSignificant(codePoint: Int): Boolean =
            Character.isLetterOrDigit(codePoint) || Character.getType(codePoint) in COMBINING_MARK_TYPES

        private val COMBINING_MARK_TYPES = setOf(
            Character.NON_SPACING_MARK.toInt(),
            Character.COMBINING_SPACING_MARK.toInt(),
            Character.ENCLOSING_MARK.toInt(),
        )
    }
}
