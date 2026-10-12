package com.glassroom.music.data.util

import com.glassroom.music.domain.model.LyricLine

object LyricsParser {

    private val TIMESTAMP_REGEX = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]""")

    fun parse(lrcContent: String?): List<LyricLine> {
        if (lrcContent.isNullOrBlank()) return emptyList()

        val lines = mutableListOf<LyricLine>()

        lrcContent.lines().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) return@forEach

            // Check if line contains one or more timestamps
            val matches = TIMESTAMP_REGEX.findAll(trimmed).toList()
            if (matches.isNotEmpty()) {
                val text = trimmed.replace(TIMESTAMP_REGEX, "").trim()

                for (match in matches) {
                    val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                    val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                    val fractionStr = match.groupValues.getOrNull(3).orEmpty()
                    val fractionMs = when (fractionStr.length) {
                        1 -> (fractionStr.toLongOrNull() ?: 0L) * 100L
                        2 -> (fractionStr.toLongOrNull() ?: 0L) * 10L
                        3 -> fractionStr.toLongOrNull() ?: 0L
                        else -> 0L
                    }

                    val timestampMs = (minutes * 60 + seconds) * 1000L + fractionMs
                    if (text.isNotBlank()) {
                        lines.add(LyricLine(timestampMs = timestampMs, text = text))
                    }
                }
            }
        }

        return lines.sortedBy { it.timestampMs }
    }
}
