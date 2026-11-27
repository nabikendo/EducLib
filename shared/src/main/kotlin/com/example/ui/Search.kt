package com.example.ui

import java.text.Normalizer
import java.util.regex.Pattern

// Busca tolerante a acentos e pequenos erros de digitação, compartilhada entre Android e desktop.

private val DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
internal val WHITESPACE_REGEX = Regex("\\s+")

fun String.normalizeForSearch(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return DIACRITICS_PATTERN.matcher(temp).replaceAll("").lowercase().trim()
}

fun calculateLevenshteinDistance(s: String, t: String): Int {
    if (s == t) return 0
    if (s.isEmpty()) return t.length
    if (t.isEmpty()) return s.length
    val d = IntArray(t.length + 1) { it }
    for (i in 1..s.length) {
        var prev = i
        var old = d[0]
        d[0] = i
        for (j in 1..t.length) {
            val cost = if (s[i - 1] == t[j - 1]) 0 else 1
            val temp = d[j]
            d[j] = minOf(d[j] + 1, prev + 1, old + cost)
            old = temp
            prev = d[j]
        }
    }
    return d[t.length]
}

fun isWordFuzzyMatch(qWord: String, tWord: String): Boolean {
    if (tWord.contains(qWord) || qWord.contains(tWord)) return true
    val lenDiff = Math.abs(qWord.length - tWord.length)
    val maxAllowed = when {
        qWord.length <= 3 -> 0
        qWord.length <= 6 -> 1
        else -> 2
    }
    if (lenDiff > maxAllowed) return false
    val dist = calculateLevenshteinDistance(qWord, tWord)
    return dist <= maxAllowed
}

fun isFuzzyMatch(query: String, target: String): Boolean {
    val qNorm = query.normalizeForSearch()
    if (qNorm.isEmpty()) return true
    val targetNorm = target.normalizeForSearch()
    if (targetNorm.contains(qNorm)) return true
    
    val queryWords = qNorm.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
    if (queryWords.isEmpty()) return true
    val targetWords = targetNorm.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
    
    // Each query word must fuzzy-match at least one target word
    return queryWords.all { qWord ->
        targetWords.any { tWord ->
            isWordFuzzyMatch(qWord, tWord)
        }
    }
}
