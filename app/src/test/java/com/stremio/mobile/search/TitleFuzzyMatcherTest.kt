package com.stremio.mobile.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TitleFuzzyMatcherTest {
    @Test
    fun \`suggests the closest cached title for a small typo\`() {
        assertEquals(
            "Breaking Bad",
            TitleFuzzyMatcher.bestMatch("Breakng Bad", listOf("The Office", "Breaking Bad", "Dark")),
        )
    }

    @Test
    fun \`does not suggest a title when the query is already exact\`() {
        assertNull(TitleFuzzyMatcher.bestMatch("Breaking Bad", listOf("Breaking Bad", "The Office")))
    }

    @Test
    fun \`does not suggest a title for unrelated words\`() {
        assertNull(TitleFuzzyMatcher.bestMatch("spaceship", listOf("The Office", "Breaking Bad", "Dark")))
    }

    @Test
    fun \`ignores queries that are too short\`() {
        assertNull(TitleFuzzyMatcher.bestMatch("abc", listOf("ABC", "The Office")))
    }

    @Test
    fun \`normalizes punctuation and diacritics\`() {
        assertNull(TitleFuzzyMatcher.bestMatch("Amelie", listOf("Amélie")))
        assertEquals(
            "Spider-Man: Across the Spider-Verse",
            TitleFuzzyMatcher.bestMatch(
                "Spider Man Across the Spider Verse",
                listOf("Spider-Man: Across the Spider-Verse", "The Office"),
            ),
        )
    }
}
