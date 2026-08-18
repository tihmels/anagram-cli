package io.github.tihmels.anagram.domain

/**
 * How two texts relate.
 *
 * An anagram rearranges a *different* text, so "same characters" and "is an anagram" are separate
 * statements. [SAME_TEXT] stays its own outcome rather than collapsing into a bare `false` that a
 * caller could not tell apart from a genuine mismatch.
 */
enum class ComparisonResult {
    ANAGRAMS,
    SAME_TEXT,
    NOT_ANAGRAMS,
}
