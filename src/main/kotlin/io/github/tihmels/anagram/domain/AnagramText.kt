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
 *  3. Only Unicode letters and digits are significant. Whitespace, punctuation, symbols and
 *     any combining marks that survive NFC composition are ignored.
 *  4. Iteration happens over code points, not UTF-16 chars, so characters outside the Basic
 *     Multilingual Plane are treated as single characters.
 *
 * Diacritics are deliberately **not** stripped: `é` and `e` are different characters. Folding
 * them together would be a language-dependent decision that this application does not make.
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
            // Lowercasing happens before filtering: some uppercase letters lowercase into a
            // letter plus a combining mark (U+0130 -> "i" + U+0307). Filtering afterwards
            // removes that mark, so "İSTANBUL" and "istanbul" normalize identically.
            val lowercased = Normalizer.normalize(raw, Normalizer.Form.NFC).lowercase(Locale.ROOT)
            val canonical = StringBuilder(lowercased.length)
            lowercased.codePoints()
                .filter(Character::isLetterOrDigit)
                .forEach(canonical::appendCodePoint)
            return canonical.toString()
        }
    }
}
