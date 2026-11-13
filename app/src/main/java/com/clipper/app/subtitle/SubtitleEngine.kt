package com.clipper.app.subtitle

import android.graphics.Color

data class WordToken(
    val word: String,
    val startMs: Long,
    val endMs: Long
)

data class SubtitlePhrase(
    val words: List<WordToken>,
    val startMs: Long,
    val endMs: Long,
    val text: String = words.joinToString(" ") { it.word }
)

enum class SubtitlePreset {
    HORMOZI_YELLOW,   // Bold uppercase, neon yellow active highlight, black outline
    MODERN_CLEAN,     // Clean white with subtle rounded background pill
    CYBER_GREEN       // Matrix/Tech green pop
}

object SubtitleEngine {

    /**
     * Splits a raw transcript text into rhythmic 3-5 word subtitle chunks
     * with calculated relative timestamps for the video segment.
     */
    fun generatePhrases(
        transcript: String,
        segmentDurationSeconds: Long,
        wordsPerPhrase: Int = 3
    ): List<SubtitlePhrase> {
        val rawWords = transcript.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (rawWords.isEmpty()) return emptyList()

        val totalMs = segmentDurationSeconds * 1000L
        val msPerWord = if (rawWords.isNotEmpty()) totalMs / rawWords.size else 500L

        val tokenList = rawWords.mapIndexed { index, word ->
            val start = index * msPerWord
            val end = start + msPerWord
            WordToken(word = word.uppercase(), startMs = start, endMs = end)
        }

        val phrases = mutableListOf<SubtitlePhrase>()
        var chunkIndex = 0

        while (chunkIndex < tokenList.size) {
            val chunk = tokenList.drop(chunkIndex).take(wordsPerPhrase)
            if (chunk.isNotEmpty()) {
                phrases.add(
                    SubtitlePhrase(
                        words = chunk,
                        startMs = chunk.first().startMs,
                        endMs = chunk.last().endMs
                    )
                )
            }
            chunkIndex += wordsPerPhrase
        }

        return phrases
    }

    /**
     * Finds active phrase and active word index at the given playback position.
     */
    fun getActiveSubtitleAt(
        phrases: List<SubtitlePhrase>,
        positionMs: Long
    ): Pair<SubtitlePhrase?, Int> {
        val activePhrase = phrases.firstOrNull { positionMs in it.startMs..it.endMs }
            ?: phrases.firstOrNull { it.startMs > positionMs }

        val activeWordIndex = activePhrase?.words?.indexOfFirst { positionMs in it.startMs..it.endMs } ?: 0
        return Pair(activePhrase, activeWordIndex)
    }
}
