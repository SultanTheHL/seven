package com.seven.seven.external

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.seven.seven.config.ExternalApiProperties
import com.seven.seven.ml.model.MlRecommendationResponse
import com.seven.seven.ml.model.PersonalInfoPayload
import com.seven.seven.ml.model.VehiclePayload
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

    fun generateAdvantages(context: PromptContext): List<VehicleAdvantage> {
        if (context.superiorVehicles.isEmpty()) {
            logger.warn("No superior vehicles provided to Gemini prompt")
            return emptyList()
        }

        if (properties.gemini.apiKey.isBlank()) {
            logger.warn("Gemini API key is missing. Falling back to heuristic advantages.")
            return fallbackAdvantages(context)
        }

        val prompt = buildPrompt(context)
        val uri = UriComponentsBuilder
            .fromUriString("$GEMINI_BASE_URL/${properties.gemini.model}:generateContent")
            .queryParam("key", properties.gemini.apiKey)
            .build(true)
            .toUri()

        val requestBody = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
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

        val parsed = responseText?.let { parseAdvantages(it) }
        return parsed ?: fallbackAdvantages(context)
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

    private fun parseAdvantages(payload: String): List<VehicleAdvantage>? {
        val listType = objectMapper.typeFactory.constructCollectionType(List::class.java, VehicleAdvantage::class.java)
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            objectMapper.readValue(payload, listType) as List<VehicleAdvantage>
        }.getOrElse {
            runCatching {
                objectMapper.readValue(payload, GeminiTextWrapper::class.java).recommendations
            }.getOrElse { ex ->
                logger.warn("Unable to parse Gemini JSON payload: ${ex.message}")
                null
            }
        }
    }

    private fun fallbackAdvantages(context: PromptContext): List<VehicleAdvantage> {
        val standard = context.standardVehicle
        return context.superiorVehicles.map { vehicle ->
            val advantages = mutableListOf<String>()
            if (vehicle.passengersCount > standard.passengersCount) {
                advantages += "${vehicle.brand} ${vehicle.model} seats ${vehicle.passengersCount - standard.passengersCount} more passengers than the currently booked vehicle."
            }
            if (vehicle.bagsCount > standard.bagsCount) {
                advantages += "Provides ${vehicle.bagsCount - standard.bagsCount} additional luggage slots."
            }
            if (vehicle.isMoreLuxury && !standard.isMoreLuxury) {
                advantages += "Includes luxury package upgrades absent on the standard model."
            }
            if (vehicle.vehicleCostValueEur > standard.vehicleCostValueEur) {
                advantages += "Higher list price indicates a more premium equipment level."
            }
            if (advantages.isEmpty()) {
                advantages += "Offers enhanced comfort and technology compared to the standard vehicle."
            }
            VehicleAdvantage(vehicleId = vehicle.id, advantages = advantages.take(3))
        }
    }

    private fun buildPrompt(context: PromptContext): String {
        val standardJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.standardVehicle)
        val superiorJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.superiorVehicles)
        val personalJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.personalInfo)
        val mlJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.mlResponse)

        return """
You are a knowledgeable automotive consultant with extensive experience in evaluating and comparing vehicle performance, features, and overall value. Your expertise lies in identifying specific advantages that higher-end vehicles have over standard models.

Your task is to analyze a standard car and up to three superior models, then determine three factual advantages of each superior model compared to the standard vehicle. The vehicle schema is:
id, brand, model, acriss_code, group_type, transmission_type, fuel_type, passengers_count, bags_count, is_new_car, is_recommended, is_more_luxury, is_exciting_discount, vehicle_cost_value_eur, model_annex, images, tyre_type, attributes, vehicle_status, vehicle_cost (currency/value), upsell_reasons.

Standard vehicle JSON:
$standardJson

Superior vehicles JSON:
$superiorJson

PersonalInfoPayload (route context):
$personalJson

MlRecommendationResponse (metrics):
$mlJson

Return ONLY a JSON array. Each array entry must contain:
{
  "vehicle_id": "<UUID>",
  "advantages": [
    "Advantage 1 text",
    "Advantage 2 text",
    "Advantage 3 text"
  ]
}

Every advantage must compare the superior vehicle to the standard vehicle using the provided data (performance, seating, luggage, luxury package, cost, safety tech, efficiency, etc.). Avoid subjective language and do not add explanations outside the JSON array.
""".trimIndent()
    }

    data class PromptContext(
        val standardVehicle: VehiclePayload,
        val superiorVehicles: List<VehiclePayload>,
        val personalInfo: PersonalInfoPayload,
        val mlResponse: MlRecommendationResponse
    )

    data class VehicleAdvantage(
        @JsonProperty("vehicle_id")
        val vehicleId: String,
        @JsonProperty("advantages")
        val advantages: List<String>
    )

    private data class GeminiTextWrapper(
        val recommendations: List<VehicleAdvantage>
    )

    companion object {
        private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }
}

