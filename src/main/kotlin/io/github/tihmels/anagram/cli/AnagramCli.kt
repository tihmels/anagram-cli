package io.github.tihmels.anagram.cli

import io.github.tihmels.anagram.domain.AnagramSession
import io.github.tihmels.anagram.domain.AnagramText
import io.github.tihmels.anagram.domain.ComparisonResult
import java.io.Reader
import java.io.Writer
import java.util.Locale

class AnagramCli(
    input: Reader,
    private val output: Writer,
    private val session: AnagramSession = AnagramSession(),
) {
    private val input = input.buffered()

    fun run() {
        emit(BANNER)
        var running = true
        while (running) {
            val command = (prompt(COMMAND_PROMPT) ?: break).trim()
            running = when (command.lowercase(Locale.ROOT)) {
                "" -> true
                "1", "compare" -> compare()
                "2", "find" -> find()
                "help" -> {
                    emit(HELP)
                    true
                }
                "quit", "exit" -> false
                else -> {
                    emit("Unknown command '$command'. Type 'help' to see the available commands.")
                    true
                }
            }
        }
        emit("Bye.")
    }

    /** @return `false` when the input stream ended and the loop should stop. */
    private fun compare(): Boolean {
        val first = readOrReject(prompt("First text:  ") ?: return false) ?: return true
        val second = readOrReject(prompt("Second text: ") ?: return false) ?: return true

        emit(
            when (session.compareAndRecord(first, second)) {
                ComparisonResult.ANAGRAMS -> "Anagrams: yes"
                ComparisonResult.SAME_TEXT -> "Anagrams: no — both inputs are the same text"
                ComparisonResult.NOT_ANAGRAMS -> "Anagrams: no"
            },
        )
        return true
    }

    /** @return `false` when the input stream ended and the loop should stop. */
    private fun find(): Boolean {
        val query = readOrReject(prompt("Text: ") ?: return false) ?: return true

        val matches = session.findAnagrams(query)
        if (matches.isEmpty()) {
            emit("No matches.")
        } else {
            emit("${matches.size} match(es):")
            matches.forEach { emit("  - ${it.display}") }
        }
        return true
    }

    /** @return the entered line, or `null` at end of input. */
    private fun prompt(text: String): String? {
        output.write(text)
        output.flush()
        return input.readLine()
    }

    /** @return the parsed text, or `null` after emitting [REJECTED]. */
    private fun readOrReject(line: String): AnagramText? {
        val text = AnagramText.of(line)
        if (text == null) emit(REJECTED)
        return text
    }

    private fun emit(text: String) {
        output.write(text)
        output.write(System.lineSeparator())
        output.flush()
    }

    private companion object {
        const val COMMAND_PROMPT = "> "

        const val REJECTED = "Rejected: a text must contain at least one letter or digit."

        const val BANNER = "Anagram tool. Type 'help' for the available commands, 'quit' to exit."

        val HELP = """
            compare (1)  Check whether two texts are anagrams. Both texts are remembered,
                         whether or not they turned out to be anagrams.
            find    (2)  List the remembered texts that are anagrams of a query. The query
                         itself is not remembered and never appears in its own results.
            help         Show this text.
            quit         Exit. The history lives in memory only and is lost on exit.

            Comparison ignores case, whitespace and punctuation; accented letters are kept
            distinct from their unaccented counterparts.
        """.trimIndent()
    }
}
