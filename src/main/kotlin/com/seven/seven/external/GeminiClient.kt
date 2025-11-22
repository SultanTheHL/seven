package com.seven.seven.external

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.seven.seven.config.ExternalApiProperties
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.util.UriComponentsBuilder

@Component
class GeminiClient(
    private val restClient: RestClient,
    private val properties: ExternalApiProperties,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(GeminiClient::class.java)

    fun generateFeedback(selectedVehicleId: String): GeminiResult {
        if (properties.gemini.apiKey.isBlank()) {
            logger.warn("Gemini API key is missing. Falling back to default feedback.")
            return GeminiResult(
                id = selectedVehicleId,
                feedback = DEFAULT_FEEDBACK
            )
        }

        val uri = UriComponentsBuilder
            .fromUriString("$GEMINI_BASE_URL/${properties.gemini.model}:generateContent")
            .queryParam("key", properties.gemini.apiKey)
            .build(true)
            .toUri()

        val requestBody = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to "Hello")
                    )
                )
            )
        )

        val responseText = try {
            val raw = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String::class.java)
            extractText(raw)
        } catch (ex: RestClientResponseException) {
            logger.error("Gemini API call failed with status ${ex.statusCode}: ${ex.responseBodyAsString}")
            null
        } catch (ex: Exception) {
            logger.error("Gemini API call failed: ${ex.message}")
            null
        }

        return GeminiResult(
            id = selectedVehicleId,
            feedback = responseText ?: DEFAULT_FEEDBACK
        )
    }

    private fun extractText(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            val root: JsonNode = objectMapper.readTree(body)
            root.path("candidates")
                .firstOrNull()
                ?.path("content")
                ?.path("parts")
                ?.firstOrNull()
                ?.path("text")
                ?.asText()
        } catch (ex: Exception) {
            logger.warn("Failed to parse Gemini response: ${ex.message}")
            null
        }
    }

    data class GeminiResult(
        val id: String,
        val feedback: String
    )

    companion object {
        private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val DEFAULT_FEEDBACK = "No feedback provided."
    }
}

