package com.seven.seven.external

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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

    fun generateVehicleAdvantages(context: PromptContext): List<VehicleAdvantage> {
        if (context.superiorVehicles.isEmpty()) {
            logger.warn("No superior vehicles provided to Gemini prompt")
            return emptyList()
        }

        if (properties.gemini.apiKey.isBlank()) {
            logger.warn("Gemini API key is missing. Falling back to heuristic advantages.")
            return fallbackAdvantages(context)
        }

        val prompt = buildVehiclePrompt(context)
        val responseText = executeGeminiCall(prompt)

        val parsed = responseText?.let { parseAdvantages(it) }
        return parsed ?: fallbackAdvantages(context)
    }

    fun generateProtectionFeedback(context: PromptContext): ProtectionFeedback {
        if (properties.gemini.apiKey.isBlank()) {
            logger.warn("Gemini API key missing. Using fallback protection feedback.")
            return fallbackProtectionFeedback(context)
        }

        val prompt = buildProtectionPrompt(context)
        val responseText = executeGeminiCall(prompt)
        val parsed = responseText?.let { parseProtectionFeedback(it) }
        return parsed ?: fallbackProtectionFeedback(context)
    }

    private fun executeGeminiCall(prompt: String): String? {
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

        return try {
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

    private fun parseProtectionFeedback(payload: String): ProtectionFeedback? =
        runCatching {
            objectMapper.readValue(payload, ProtectionFeedback::class.java)
        }.getOrElse {
            logger.warn("Unable to parse protection feedback payload: ${it.message}")
            null
        }

    private fun fallbackAdvantages(context: PromptContext): List<VehicleAdvantage> {
        val standard = context.standardVehicle
        val passengerPhrase = passengerNeedPhrase(context.personalInfo)
        val luggagePhrase = luggageNeedPhrase(context.personalInfo)
        val journeyPhrase = distancePhrase(context.personalInfo)
        val hazard = hazardSnippet(context)

        return context.superiorVehicles.map { vehicle ->
            val advantages = mutableListOf<String>()
            if (vehicle.passengersCount > standard.passengersCount) {
                advantages += "Based on your travel party needs, ${vehicle.brand} ${vehicle.model} offers extra seating flexibility."
            }
            if (vehicle.bagsCount > standard.bagsCount) {
                advantages += "Provides additional cargo space to handle ${luggagePhrase} without compromising cabin comfort."
            }
            if (vehicle.isMoreLuxury && !standard.isMoreLuxury) {
                advantages += "Delivers a higher-comfort trim that keeps ${journeyPhrase} relaxing for everyone onboard."
            }
            if (vehicle.vehicleCostValueEur > standard.vehicleCostValueEur) {
                advantages += "Includes premium equipment that better protects occupants when facing $hazard."
            }
            if (advantages.isEmpty()) {
                advantages += "Enhances your trip with greater comfort and stability tailored to $journeyPhrase."
            }
            VehicleAdvantage(vehicleId = vehicle.id, advantages = advantages.take(3))
        }
    }

    private fun fallbackProtectionFeedback(context: PromptContext): ProtectionFeedback {
        val hazard = hazardSnippet(context)
        val journeyPhrase = distancePhrase(context.personalInfo)

        return ProtectionFeedback(
            protectionAll = listOf(
                ProtectionFeedbackEntry("Based on your route analysis, this plan keeps every premium component covered so surprise repairs never hit your wallet.", POSITIVE),
                ProtectionFeedbackEntry("Zero deductible preserves your budget even if steep terrain or dense traffic causes a claim.", POSITIVE),
                ProtectionFeedbackEntry("Includes tire, glass, interior, mobility, and passenger accident coverage—ideal for conditions like $hazard.", POSITIVE)
            ),
            protectionSmart = listOf(
                ProtectionFeedbackEntry("Balances cost and peace of mind by pairing a zero deductible with collision, theft, tire, and glass protection.", POSITIVE),
                ProtectionFeedbackEntry("Keeps essentials protected when $hazard puts additional stress on wheels and windshield.", POSITIVE)
            ),
            protectionBasic = listOf(
                ProtectionFeedbackEntry("Appeals if you only need core collision/theft coverage at the lowest paid tier.", POSITIVE),
                ProtectionFeedbackEntry("Leaves a high four-figure deductible and no tire/glass cover, which is risky for $journeyPhrase.", NEGATIVE)
            ),
            protectionNone = listOf(
                ProtectionFeedbackEntry("Avoids an upfront fee.", POSITIVE),
                ProtectionFeedbackEntry("Any incident bills the full vehicle value and nothing shields you from $hazard.", NEGATIVE),
                ProtectionFeedbackEntry("No tire, glass, or interior protection despite the conditions on your trip.", NEGATIVE)
            )
        )
    }

    private fun hazardSnippet(context: PromptContext): String {
        val info = context.personalInfo
        return when {
            info.snowVolumeLastHour > 0.0 -> "snow and slush along the route"
            info.rainVolumeLastHour > 0.5 -> "rain-soaked highways"
            info.windSpeedMetersPerSecond > 8 -> "strong crosswinds"
            context.mlResponse.maxSlope > 4 -> "steep gradients"
            else -> "long-distance driving exposure"
        }
    }

    private fun passengerNeedPhrase(info: PersonalInfoPayload): String =
        if (info.peopleCount >= 5) "your larger travel party" else "your passengers"

    private fun luggageNeedPhrase(info: PersonalInfoPayload): String {
        val totalBags = info.luggageBigCount + info.luggageSmallCount
        return if (totalBags >= 3) "multiple suitcases" else "your luggage"
    }

    private fun distancePhrase(info: PersonalInfoPayload): String =
        if (info.tripLengthKm >= 200) "your long-distance itinerary" else "your city-focused journey"

    private fun buildVehiclePrompt(context: PromptContext): String {
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

WRITING RULES:
1. Start every advantage with a contextual hook such as "Based on your luggage requirements" or "Given the steep terrain on this trip".
2. Explicitly tie each advantage to the renter's needs (party size, luggage, weather, slopes, trip length, vehicle preferences).
3. Never quote raw numeric measurements from the input (e.g., do not mention exact degrees, m/s, or kilometers). Use qualitative phrases like "strong winds", "steep gradients", or "long-distance drive".
4. Do not merely restate a spec; explain why the feature helps this specific journey.

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

    private fun buildProtectionPrompt(context: PromptContext): String {
        val personalInfo = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.personalInfo)
        val mlJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.mlResponse)
        val contextSummary = summarizeContext(context)

        return """
ROLE:
You are a highly intelligent car rental recommendation engine. Your goal is to generate persuasive, context-aware feedback for car rental protection packages based on specific driving conditions (weather, terrain, trip duration).

USER CONTEXT SUMMARY:
$contextSummary

PersonalInfoPayload JSON:
$personalInfo

MlRecommendationResponse JSON:
$mlJson

PROTECTION PACKAGES:
[
  {
    "name": "All Inclusive-Schutz",
    "price_per_day": 44.82,
    "features": [
      "$0 Deductible",
      "Collision",
      "Theft",
      "Tires & Glass",
      "Interior coverage",
      "Mobility coverage",
      "Passenger accident coverage"
    ]
  },
  {
    "name": "Smart-Schutz",
    "price_per_day": 26.43,
    "features": [
      "$0 Deductible",
      "Collision",
      "Theft",
      "Tires & Glass"
    ]
  },
  {
    "name": "Basic-Schutz",
    "price_per_day": 6.56,
    "features": [
      "1.100 € Deductible",
      "Collision",
      "Theft"
    ]
  },
  {
    "name": "Kein Extra-Schutz",
    "price_per_day": 0.0,
    "features": [
      "Deductible equals full vehicle value",
      "No coverage"
    ]
  }
]

LOGIC RULES:
1. "All Inclusive-Schutz": sentiment 100% positive, exactly 3 reasons, highlight exclusive features and 0 deductible.
2. "Smart-Schutz": sentiment 100% positive, exactly 2 reasons (< All Inclusive), focus on price + essential coverage (tires/glass, 0 deductible).
3. "Basic-Schutz": mixed sentiment. Provide exactly 1 positive (usually price) AND up to 1 negative highlighting high deductible (1.100 €) or missing tire/glass.
4. "Kein Extra-Schutz": predominantly negative/risky. Max 1 positive (price). Warn that deductible equals vehicle value and no coverage for tires/glass.

TAILORING:
Reference the route context. Mention wind/rain/snow/steep slopes when relevant (e.g., wind -> windshield/tire risk, steep slopes -> accident cost). No generic statements.

STYLE RULES:
- Every sentence should begin with a hook such as "Based on your route analysis" or "Given the winds on your journey".
- Do NOT repeat raw numeric measurements from the input (describe as "strong winds", "steep gradients", "long itinerary", etc.).
- Keep reasons concise, factual, and tied to the packages' features.

OUTPUT FORMAT:
Return ONLY a JSON object:
{
  "protectionAll": [ { "feedbackText": "...", "feedbackType": "POSITIVE" }, ... ],
  "protectionSmart": [ ... ],
  "protectionBasic": [ ... ],
  "protectionNone": [ ... ]
}

feedbackType must be either "POSITIVE" or "NEGATIVE".
""".trimIndent()
    }

    private fun summarizeContext(context: PromptContext): String {
        val info = context.personalInfo
        val ml = context.mlResponse
        return """
Trip length: ${"%.1f".format(info.tripLengthKm)} km (~${info.tripLengthHours} h)
Weather: condition ${info.conditionId}, temperature ${"%.1f".format(info.temperatureCelsius)}°C, wind ${"%.1f".format(info.windSpeedMetersPerSecond)} m/s, rain ${"%.2f".format(info.rainVolumeLastHour)} mm/h, snow ${"%.2f".format(info.snowVolumeLastHour)} mm/h, visibility ${info.visibilityMeters} m
Terrain: max slope ${"%.1f".format(ml.maxSlope)}°, total ascent ${"%.1f".format(ml.totalAscent)} m, descent ${"%.1f".format(ml.totalDescent)} m, risk_score ${"%.2f".format(ml.riskScore)}
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

    data class ProtectionFeedback(
        @JsonProperty("protectionAll")
        val protectionAll: List<ProtectionFeedbackEntry> = emptyList(),
        @JsonProperty("protectionSmart")
        val protectionSmart: List<ProtectionFeedbackEntry> = emptyList(),
        @JsonProperty("protectionBasic")
        val protectionBasic: List<ProtectionFeedbackEntry> = emptyList(),
        @JsonProperty("protectionNone")
        val protectionNone: List<ProtectionFeedbackEntry> = emptyList()
    )

    data class ProtectionFeedbackEntry(
        @JsonProperty("feedbackText")
        val feedbackText: String,
        @JsonProperty("feedbackType")
        val feedbackType: String
    )

    companion object {
        private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val POSITIVE = "POSITIVE"
        private const val NEGATIVE = "NEGATIVE"
    }
}

