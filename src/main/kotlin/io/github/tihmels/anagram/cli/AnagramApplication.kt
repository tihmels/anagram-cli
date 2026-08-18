@file:JvmName("AnagramApplication")

package io.github.tihmels.anagram.cli

import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

fun main() {
    // Pinned to UTF-8 rather than the platform default, since normalization is defined over
    // Unicode code points.
    val output = OutputStreamWriter(System.out, StandardCharsets.UTF_8)
    AnagramCli(InputStreamReader(System.`in`, StandardCharsets.UTF_8), output).run()
    output.flush()
}
