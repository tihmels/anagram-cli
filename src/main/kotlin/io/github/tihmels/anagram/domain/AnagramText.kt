package io.github.tihmels.anagram.domain

import java.text.Normalizer
import java.util.Locale

/**
 * A text that has passed the normalization contract and is therefore usable as domain input.
 *
 * The contract is applied exactly once, in [of], and this is the only place in the codebase that
 * applies string rules: NFC composition, [Locale.ROOT] lowercasing, letters, digits and combining
 * marks kept while everything else is ignored, iterated per code point. Diacritics and marks are
 * deliberately preserved; folding them would be a language-dependent decision this application
 * does not make. The README states each rule and its consequences, and a test pins each one.
 */
class AnagramText private constructor(
    /** The text as first entered, whitespace-trimmed. Used for display only. */
    val display: String,
    // A representation rather than public identity: callers compare AnagramText values, so no
    // second notion of "same text" can grow outside this class.
    internal val normalized: String,
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
            // Not merely an emptiness check: marks are significant, so a text of marks alone
            // normalizes to something non-empty while having nothing of its own to modify.
            if (normalized.codePoints().noneMatch(Character::isLetterOrDigit)) return null
            return AnagramText(raw.trim(), normalized)
        }

        private fun normalize(raw: String): String {
            // NFC runs twice on purpose: case mapping can decompose what the first pass composed,
            // since some characters lowercase into a letter plus a mark (U+0130 -> "i" + U+0307).
            val composed = Normalizer.normalize(raw, Normalizer.Form.NFC)
            val cased = Normalizer.normalize(composed.lowercase(Locale.ROOT), Normalizer.Form.NFC)

            val canonical = StringBuilder(cased.length)
            cased.codePoints()
                .filter(::isSignificant)
                .forEach(canonical::appendCodePoint)

            // A third pass: discarding punctuation can leave a base character and a mark that
            // would compose adjacent for the first time, e.g. "cafe,́" only becomes "café"
            // once the comma between the "e" and the accent is gone. Without this, that mark is
            // stuck uncomposed while the same accent typed with no comma in the way is not.
            return Normalizer.normalize(canonical.toString(), Normalizer.Form.NFC)
        }

        // Marks are kept although they are not letters: in many scripts they are part of the
        // character, and dropping U+093E would make the Devanagari "का" equal to "क".
        private fun isSignificant(codePoint: Int): Boolean =
            Character.isLetterOrDigit(codePoint) || Character.getType(codePoint) in COMBINING_MARK_TYPES

        private val COMBINING_MARK_TYPES = setOf(
            Character.NON_SPACING_MARK.toInt(),
            Character.COMBINING_SPACING_MARK.toInt(),
            Character.ENCLOSING_MARK.toInt(),
        )
    }
}
