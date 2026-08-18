package io.github.tihmels.anagram.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Executable form of the state semantics documented on [AnagramSession].
 */
class AnagramSessionTest {

    private val session = AnagramSession()

    private fun text(raw: String): AnagramText = AnagramText.of(raw) ?: error("unexpectedly rejected: '$raw'")

    private fun compare(first: String, second: String): Boolean =
        session.compareAndRecord(text(first), text(second))

    private fun find(query: String): List<String> =
        session.findAnagrams(text(query)).map { it.display }

    @Nested
    @DisplayName("feature #1 — comparing two texts")
    inner class Comparing {

        @Test
        fun `recognises a plain anagram`() {
            assertTrue(compare("listen", "silent"))
        }

        @Test
        fun `rejects texts that are not anagrams`() {
            assertFalse(compare("listen", "banana"))
        }

        @Test
        fun `ignores case, whitespace and punctuation`() {
            assertTrue(compare("Dormitory", "Dirty Room!"))
        }

        @Test
        fun `counts repeated characters rather than distinct ones`() {
            // "aab" and "abb" use the same letters but not the same number of them.
            assertFalse(compare("aab", "abb"))
        }

        @Test
        fun `a text is an anagram of itself`() {
            assertTrue(compare("listen", "Listen!"))
        }
    }

    @Nested
    @DisplayName("feature #2 — looking up anagrams among past inputs")
    inner class LookingUp {

        @Test
        fun `returns nothing when no text has been entered yet`() {
            assertEquals(emptyList<String>(), find("listen"))
        }

        @Test
        fun `never returns the query itself`() {
            compare("listen", "silent")
            assertEquals(listOf("silent"), find("listen"))
        }

        @Test
        fun `excludes the query by normalized form, not by spelling`() {
            compare("listen", "silent")
            assertEquals(listOf("silent"), find("  LISTEN!  "))
        }

        @Test
        fun `can be asked about a text that was never entered`() {
            compare("listen", "silent")
            assertEquals(listOf("listen", "silent"), find("enlist"))
        }

        @Test
        fun `does not record the query`() {
            // If a lookup recorded its query, "listen" would show up in the second lookup.
            find("listen")
            compare("silent", "banana")
            assertEquals(emptyList<String>(), find("silent"))
        }

        @Test
        fun `returns matches in first-seen order`() {
            compare("listen", "silent")
            compare("enlist", "tinsel")
            assertEquals(listOf("listen", "silent", "enlist", "tinsel"), find("inlets"))
        }
    }

    @Nested
    @DisplayName("both inputs of feature #1 enter the history")
    inner class Recording {

        @Test
        fun `records both texts even when they are not anagrams`() {
            compare("listen", "banana")
            // "banana" was on the losing side of the comparison and is still found later.
            assertEquals(listOf("banana"), find("nabana"))
        }

        @Test
        fun `associates texts that were never compared with each other`() {
            // The scenario from the assignment: A, B and D are anagrams, C is not.
            val a = "listen"
            val b = "silent"
            val c = "banana"
            val d = "enlist"

            compare(a, b)
            compare(a, c)
            compare(a, d)

            assertEquals(listOf(b, d), find(a))
            assertEquals(listOf(a, d), find(b))
            assertEquals(emptyList<String>(), find(c))
            // B and D were never compared directly, yet they find each other.
            assertEquals(listOf(a, b), find(d))
        }
    }

    @Nested
    @DisplayName("duplicates and idempotency")
    inner class Duplicates {

        @Test
        fun `repeating the same comparison does not multiply the results`() {
            repeat(3) { compare("listen", "silent") }
            assertEquals(listOf("silent"), find("listen"))
        }

        @Test
        fun `a text re-entered in a different spelling is not recorded twice`() {
            compare("listen", "silent")
            compare("LISTEN", "Silent!")
            assertEquals(listOf("silent"), find("listen"))
        }

        @Test
        fun `keeps the first-seen spelling of a repeated text`() {
            compare("Listen", "Silent")
            compare("listen", "silent")
            assertEquals(listOf("Silent"), find("listen"))
        }
    }

    @Test
    fun `sessions do not share history`() {
        compare("listen", "silent")
        assertEquals(emptyList<String>(), AnagramSession().findAnagrams(text("listen")).map { it.display })
    }
}
