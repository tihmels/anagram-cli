package io.github.tihmels.anagram.domain

/** How two texts compared by [AnagramSession.compareAndRecord] relate to each other. */
enum class ComparisonResult {
    ANAGRAMS,
    SAME_TEXT,
    NOT_ANAGRAMS,
}
