package io.github.tihmels.anagram.cli

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.io.StringWriter

/**
 * Drives the CLI end to end through in-memory streams, so the interaction is verified without a
 * terminal and without a human typing.
 *
 * Several tests assert the complete transcript rather than a substring. That is deliberate: the
 * program's promise is that the same sequence of commands always produces the same output, and a
 * substring check would not notice a duplicated, reordered or missing line.
 */
class AnagramCliTest {

    /** Feeds [commands] as successive lines and returns the transcript with normalized newlines. */
    private fun transcript(vararg commands: String): String {
        val output = StringWriter()
        AnagramCli(StringReader(commands.joinToString("\n")), output).run()
        return output.toString().replace(System.lineSeparator(), "\n")
    }

    private val banner = "Anagram tool. Type 'help' for the available commands, 'quit' to exit.\n"

    @Test
    fun `runs both features in one session`() {
        val transcript = transcript(
            "compare", "listen", "silent",
            "find", "listen",
            "quit",
        )

        assertEquals(
            banner +
                "> First text:  Second text: Anagrams: yes\n" +
                "> Text: 1 match(es):\n" +
                "  - silent\n" +
                "> Bye.\n",
            transcript,
        )
    }

    @Test
    @DisplayName("reproduces the scenario from the assignment")
    fun `associates texts that were never compared with each other`() {
        // f1(A, B), f1(A, C), f1(A, D) with A, B, D anagrams and C unrelated.
        val transcript = transcript(
            "compare", "listen", "silent",
            "compare", "listen", "banana",
            "compare", "listen", "enlist",
            "find", "listen",
            "find", "silent",
            "find", "banana",
            "quit",
        )

        assertEquals(
            banner +
                "> First text:  Second text: Anagrams: yes\n" +
                "> First text:  Second text: Anagrams: no\n" +
                "> First text:  Second text: Anagrams: yes\n" +
                // f2(A) -> [B, D]
                "> Text: 2 match(es):\n" +
                "  - silent\n" +
                "  - enlist\n" +
                // f2(B) -> [A, D], although B and D were never compared directly
                "> Text: 2 match(es):\n" +
                "  - listen\n" +
                "  - enlist\n" +
                // f2(C) -> []
                "> Text: No matches.\n" +
                "> Bye.\n",
            transcript,
        )
    }

    @Test
    fun `accepts the numeric aliases from the assignment`() {
        val transcript = transcript("1", "listen", "silent", "2", "enlist", "quit")

        assertEquals(
            banner +
                "> First text:  Second text: Anagrams: yes\n" +
                "> Text: 2 match(es):\n" +
                "  - listen\n" +
                "  - silent\n" +
                "> Bye.\n",
            transcript,
        )
    }

    @Test
    fun `distinguishes the same text from a genuine mismatch`() {
        val transcript = transcript("compare", "listen", "Listen!", "compare", "listen", "banana", "quit")

        assertEquals(
            banner +
                "> First text:  Second text: Anagrams: no \u2014 both inputs are the same text\n" +
                "> First text:  Second text: Anagrams: no\n" +
                "> Bye.\n",
            transcript,
        )
    }

    @Test
    fun `reports rejected input and keeps running`() {
        val transcript = transcript("compare", "!!!", "find", "   ", "quit")

        assertEquals(
            banner +
                // The second text is not even asked for once the first one is rejected.
                "> First text:  Rejected: a text must contain at least one letter or digit.\n" +
                "> Text: Rejected: a text must contain at least one letter or digit.\n" +
                "> Bye.\n",
            transcript,
        )
    }

    @Test
    fun `reports an unknown command without ending the session`() {
        val transcript = transcript("frobnicate", "quit")

        assertEquals(
            banner +
                "> Unknown command 'frobnicate'. Type 'help' to see the available commands.\n" +
                "> Bye.\n",
            transcript,
        )
    }

    @Test
    fun `ignores an empty command line`() {
        assertEquals(banner + "> > > Bye.\n", transcript("", "   ", "quit"))
    }

    @Test
    fun `stops cleanly when the input ends without a quit command`() {
        val transcript = transcript("compare", "listen", "silent")

        assertEquals(
            banner +
                "> First text:  Second text: Anagrams: yes\n" +
                "> Bye.\n",
            transcript,
        )
    }

    @Test
    fun `stops cleanly when the input ends in the middle of a command`() {
        assertEquals(banner + "> First text:  Second text: Bye.\n", transcript("compare", "listen"))
    }

    @Test
    fun `describes both features in the help text`() {
        val transcript = transcript("help", "quit")

        assertTrue(transcript.contains("compare (1)"), transcript)
        assertTrue(transcript.contains("find    (2)"), transcript)
        assertTrue(transcript.contains("Both texts are remembered"), transcript)
    }
}
