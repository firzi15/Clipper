package com.clipper.app.nlm

import com.clipper.app.nlm.models.CropMode
import com.clipper.app.nlm.models.ViralSegment
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object NlmParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Extracts and parses JSON array of viral segments from the LLM output text.
     */
    fun parseViralSegments(rawResponseText: String): List<ViralSegment> {
        // Strip markdown code fences if present (```json ... ``` or ``` ...)
        val cleanedText = extractJsonBlock(rawResponseText)

        return try {
            val rootElement = json.parseToJsonElement(cleanedText)
            val jsonArray = if (rootElement is kotlinx.serialization.json.JsonArray) {
                rootElement
            } else {
                rootElement.jsonObject["segments"]?.jsonArray
                    ?: rootElement.jsonObject["clips"]?.jsonArray
                    ?: return emptyList()
            }

            jsonArray.mapNotNull { element ->
                try {
                    val obj = element.jsonObject
                    val startStr = obj["start"]?.jsonPrimitive?.content ?: "00:00"
                    val endStr = obj["end"]?.jsonPrimitive?.content ?: "00:30"
                    val startSec = parseTimeToSeconds(startStr)
                    val endSec = parseTimeToSeconds(endStr)
                    val hook = obj["hook"]?.jsonPrimitive?.content 
                        ?: obj["headline"]?.jsonPrimitive?.content 
                        ?: "Viral Segment"
                    val score = obj["score"]?.jsonPrimitive?.content?.toIntOrNull() ?: 85
                    val reason = obj["reason"]?.jsonPrimitive?.content ?: "High emotional peak and strong hook."
                    val transcript = obj["transcript"]?.jsonPrimitive?.content ?: ""
                    val cropStr = obj["crop_mode"]?.jsonPrimitive?.content?.uppercase() ?: "AUTO"
                    
                    val cropMode = when (cropStr) {
                        "PODCAST", "SPLIT", "PODCAST_SPLIT" -> CropMode.PODCAST_SPLIT
                        "SINGLE", "SOLO" -> CropMode.SINGLE_SPEAKER
                        else -> CropMode.AUTO
                    }

                    ViralSegment(
                        startSeconds = startSec,
                        endSeconds = endSec,
                        startTimeFormatted = startStr,
                        endTimeFormatted = endStr,
                        hookHeadline = hook,
                        viralScore = score,
                        rationale = reason,
                        transcript = transcript,
                        recommendedCrop = cropMode
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            fallbackRegexParser(rawResponseText)
        }
    }

    private fun extractJsonBlock(text: String): String {
        val jsonFenceRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```", RegexOption.IGNORE_CASE)
        val match = jsonFenceRegex.find(text)
        if (match != null) {
            return match.groupValues[1].trim()
        }

        val firstBracket = text.indexOf('[').takeIf { it != -1 } ?: text.indexOf('{')
        val lastBracket = text.lastIndexOf(']').takeIf { it != -1 } ?: text.lastIndexOf('}')
        if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
            return text.substring(firstBracket, lastBracket + 1).trim()
        }

        return text.trim()
    }

    /**
     * Converts "01:25" or "00:01:25" or seconds string to Long seconds.
     */
    fun parseTimeToSeconds(timeStr: String): Long {
        val clean = timeStr.trim().replace(Regex("[^0-9:]"), "")
        val parts = clean.split(":").mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            2 -> parts[0] * 60 + parts[1]
            1 -> parts[0]
            else -> 0L
        }
    }

    private fun fallbackRegexParser(text: String): List<ViralSegment> {
        // Fallback in case JSON is corrupted or partial
        val list = mutableListOf<ViralSegment>()
        val timeRegex = Regex("(\\d{1,2}:\\d{2})\\s*[-–to]+\\s*(\\d{1,2}:\\d{2})", RegexOption.IGNORE_CASE)
        val matches = timeRegex.findAll(text).toList()

        for ((index, match) in matches.take(3).withIndex()) {
            val startStr = match.groupValues[1]
            val endStr = match.groupValues[2]
            val startSec = parseTimeToSeconds(startStr)
            val endSec = parseTimeToSeconds(endStr)

            list.add(
                ViralSegment(
                    startSeconds = startSec,
                    endSeconds = if (endSec > startSec) endSec else startSec + 45,
                    startTimeFormatted = startStr,
                    endTimeFormatted = endStr,
                    hookHeadline = "Viral Clip #${index + 1}",
                    viralScore = 80 + (index * 5),
                    rationale = "Auto-detected high interest timestamp from NotebookLM",
                    transcript = ""
                )
            )
        }
        return list
    }
}
