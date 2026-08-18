package io.github.tihmels.anagram.domain

/**
 * The outcome of comparing two texts.
 *
 * An anagram rearranges the letters of a *different* word or phrase, so "same characters" and
 * "is an anagram" are not the same statement. Reporting all three cases separately keeps that
 * distinction in the type system instead of collapsing [SAME_TEXT] into a bare `false` that the
 * caller cannot tell apart from a genuine mismatch.
 */
enum class ComparisonResult {
    /** Same characters in a different arrangement. */
    ANAGRAMS,

    /** The same text entered twice — equal characters, but nothing was rearranged. */
    SAME_TEXT,

    /** Different characters altogether. */
    NOT_ANAGRAMS,
}
