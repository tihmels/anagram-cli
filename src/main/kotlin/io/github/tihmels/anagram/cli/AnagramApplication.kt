@file:JvmName("AnagramApplication")

package io.github.tihmels.anagram.cli

import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

fun main() {
    // Both streams are pinned to UTF-8. The normalization contract is defined over Unicode code
    // points, so a platform default encoding must not be allowed to mangle input on its way in
    // or matches on their way out.
    val output = OutputStreamWriter(System.out, StandardCharsets.UTF_8)
    AnagramCli(InputStreamReader(System.`in`, StandardCharsets.UTF_8), output).run()
    output.flush()
}
