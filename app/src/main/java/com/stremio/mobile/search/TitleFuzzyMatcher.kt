package com.stremio.mobile.search

import java.text.Normalizer
import java.util.Locale

/**
 * Finds a likely intended title among locally cached catalog titles.
 *
 * This is deliberately a suggestion-only matcher: callers must still run the normal Stremio
 * search with the selected title instead of treating a local match as a search result.
 */
internal object TitleFuzzyMatcher {
    private const val MIN_QUERY_LENGTH = 4
    private const val MIN_SIMILARITY = 0.72

    fun bestMatch(query: String, candidates: Iterable<String>): String? {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.length < MIN_QUERY_LENGTH) return null

        val queryTokens = normalizedQuery.split(' ').filter(String::isNotBlank).toSet()
        return candidates.asSequence()
            .mapNotNull { title ->
                val normalizedTitle = normalize(title)
                if (normalizedTitle.isBlank() || normalizedTitle == normalizedQuery) {
                    return@mapNotNull null
                }

                val characterSimilarity = characterSimilarity(normalizedQuery, normalizedTitle)
                val titleTokens = normalizedTitle.split(' ').filter(String::isNotBlank).toSet()
                val unionSize = (queryTokens + titleTokens).size
                val tokenSimilarity = if (unionSize == 0) {
                    0.0
                } else {
                    queryTokens.intersect(titleTokens).size.toDouble() / unionSize
                }
                val score = maxOf(characterSimilarity, tokenSimilarity * 0.96)

                if (score >= MIN_SIMILARITY) Match(title, score) else null
            }
            .sortedWith(compareByDescending<Match> { it.score }.thenBy { it.title.length })
            .firstOrNull()
            ?.title
    }

    private fun normalize(value: String): String {
        val withoutMarks = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("""\p{M}+"""), "")
        return withoutMarks.lowercase(Locale.ROOT)
            .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
            .trim()
    }

    private fun characterSimilarity(left: String, right: String): Double {
        val longestLength = maxOf(left.length, right.length)
        if (longestLength == 0) return 1.0
        return 1.0 - levenshteinDistance(left, right).toDouble() / longestLength
    }

    private fun levenshteinDistance(left: String, right: String): Int {
        var previous = IntArray(right.length + 1) { it }
        var current = IntArray(right.length + 1)

        for (leftIndex in left.indices) {
            current[0] = leftIndex + 1
            for (rightIndex in right.indices) {
                val substitutionCost = if (left[leftIndex] == right[rightIndex]) 0 else 1
                current[rightIndex + 1] = minOf(
                    current[rightIndex] + 1,
                    previous[rightIndex + 1] + 1,
                    previous[rightIndex] + substitutionCost,
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[right.length]
    }

    private data class Match(val title: String, val score: Double)
}
