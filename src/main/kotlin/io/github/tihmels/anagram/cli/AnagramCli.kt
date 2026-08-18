package io.github.tihmels.anagram.cli

import io.github.tihmels.anagram.domain.AnagramSession
import io.github.tihmels.anagram.domain.AnagramText
import java.io.Reader
import java.io.Writer
import java.util.Locale

/**
 * The interactive adapter: it reads commands, renders results and reports rejected input.
 *
 * It owns no anagram logic — it never sees a signature and never inspects a normalized form.
 * Its only domain responsibility is to run raw console input through [AnagramText.of] so that
 * invalid text is turned away at the boundary and the session only ever holds valid values.
 *
 * The streams are constructor arguments rather than [System.in] and [System.out] so the whole
 * interaction can be driven from a test without a terminal.
 */
class AnagramCli(
    input: Reader,
    private val output: Writer,
    private val session: AnagramSession = AnagramSession(),
) {
    private val input = input.buffered()

    /** Runs the command loop until the user quits or the input stream ends. */
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
        val firstLine = prompt("First text:  ") ?: return false
        val first = AnagramText.of(firstLine)
        if (first == null) {
            emit(REJECTED)
            return true
        }

        val secondLine = prompt("Second text: ") ?: return false
        val second = AnagramText.of(secondLine)
        if (second == null) {
            emit(REJECTED)
            return true
        }

        emit(if (session.compareAndRecord(first, second)) "Anagrams: yes" else "Anagrams: no")
        return true
    }

    /** @return `false` when the input stream ended and the loop should stop. */
    private fun find(): Boolean {
        val queryLine = prompt("Text: ") ?: return false
        val query = AnagramText.of(queryLine)
        if (query == null) {
            emit(REJECTED)
            return true
        }

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
