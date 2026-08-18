package io.github.tihmels.anagram.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Executable form of the normalization contract documented on [AnagramText].
 *
 * These tests are the reason the contract lives in one place: changing any rule below has to be a
 * deliberate act that breaks a named test, not a side effect of an edit somewhere in the CLI.
 *
 * The Unicode under test is written with explicit escapes, so that the intent of a test cannot be
 * altered by an editor silently re-normalizing the source file.
 */
class AnagramTextTest {

    private fun normalize(raw: String): String =
        AnagramText.of(raw)?.normalized ?: error("unexpectedly rejected: '$raw'")

    @Nested
    @DisplayName("rejects text without significant characters")
    inner class Rejection {

        @ParameterizedTest(name = "rejects [{0}]")
        @ValueSource(strings = ["", "   ", "\t\n", "!?...", "--- ---", "€ £ $"])
        fun `rejects input that normalizes to nothing`(raw: String) {
            assertNull(AnagramText.of(raw))
        }

        @Test
        fun `accepts input that keeps at least one significant character`() {
            assertEquals("a", normalize("...a..."))
            assertEquals("7", normalize("(7)"))
        }
    }

    @Nested
    @DisplayName("normalization rules")
    inner class Rules {

        @Test
        fun `lowercases independently of the platform locale`() {
            // Locale.ROOT rather than the default locale: under a Turkish default locale a
            // locale-sensitive lowercase would turn "I" into a dotless "ı".
            assertEquals("i", normalize("I"))
            assertEquals("listen", normalize("LISTEN"))
        }

        @Test
        fun `ignores whitespace`() {
            assertEquals("listen", normalize("  li s\tt\ne n  "))
        }

        @Test
        fun `ignores punctuation and symbols`() {
            assertEquals("listen", normalize("l-i,s.t!e?n"))
        }

        @Test
        fun `keeps digits significant`() {
            assertEquals("abc123", normalize("abc 123"))
            assertNotEquals(normalize("abc12"), normalize("abc123"))
        }

        @Test
        fun `composes decomposed characters to NFC`() {
            val precomposed = "caf\u00E9"      // e-acute as a single code point
            val decomposed = "cafe\u0301"     // e followed by a combining acute accent
            assertEquals(normalize(precomposed), normalize(decomposed))
            assertEquals(4, normalize(decomposed).length, "the accent must compose, not be dropped")
        }

        @Test
        fun `keeps accented letters distinct from their unaccented counterparts`() {
            // A documented decision, not an oversight: folding "é" onto "e" is language dependent.
            assertNotEquals(normalize("café"), normalize("cafe"))
        }

        @Test
        fun `keeps the first-seen spelling for display while normalizing identity`() {
            val text = AnagramText.of("  Silent, Listen!  ")!!
            assertEquals("Silent, Listen!", text.display)
            assertEquals("silentlisten", text.normalized)
        }
    }

    @Nested
    @DisplayName("Unicode is handled per code point")
    inner class CodePoints {

        // A cased Deseret letter pair outside the Basic Multilingual Plane: one code point each,
        // but two UTF-16 chars, so char-wise handling would be observably wrong.
        private val deseretLongICapital = "\uD801\uDC00"  // U+10400
        private val deseretLongISmall = "\uD801\uDC28"    // U+10428
        private val deseretLongESmall = "\uD801\uDC29"    // U+10429

        @Test
        fun `a supplementary plane letter counts as one character`() {
            val normalized = normalize(deseretLongICapital)
            assertEquals(1, normalized.codePointCount(0, normalized.length))
            assertEquals(2, normalized.length, "still two UTF-16 chars")
        }

        @Test
        fun `lowercases supplementary plane letters`() {
            assertEquals(deseretLongISmall, normalize(deseretLongICapital))
        }

        @Test
        fun `treats distinct supplementary letters as distinct characters`() {
            assertNotEquals(normalize(deseretLongISmall), normalize(deseretLongESmall))
        }

        @Test
        fun `orders characters by code point, not by UTF-16 char`() {
            // U+FF41 sorts after U+10428 by code point, but before its high surrogate U+D801 by
            // UTF-16 char. A char-wise canonical form would therefore mis-order one of these.
            val fullwidthA = "\uFF41"  // U+FF41
            val first = AnagramText.of(fullwidthA + deseretLongISmall)!!
            val second = AnagramText.of(deseretLongISmall + fullwidthA)!!
            assertEquals(
                ComparisonResult.ANAGRAMS,
                AnagramSession().compareAndRecord(first, second),
                "reordered code points must still be anagrams",
            )
        }
    }

    @Nested
    @DisplayName("identity")
    inner class Identity {

        @Test
        fun `texts with the same normalized form are the same input`() {
            assertEquals(AnagramText.of("Listen!"), AnagramText.of("  listen  "))
            assertEquals(AnagramText.of("Listen!").hashCode(), AnagramText.of("  listen  ").hashCode())
        }

        @Test
        fun `texts with different normalized forms are different inputs`() {
            assertNotEquals(AnagramText.of("listen"), AnagramText.of("silent"))
        }
    }
}
