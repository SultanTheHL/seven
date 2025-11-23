package com.seven.seven.external

import com.seven.seven.config.ExternalApiProperties
import com.seven.seven.shared.model.GeoPoint
import com.seven.seven.shared.model.LocationInput
import com.seven.seven.shared.util.PolylineDecoder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import tools.jackson.databind.ObjectMapper

@Component
class GoogleDirectionsClient(
    private val restClient: RestClient,
    private val properties: ExternalApiProperties,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(GoogleDirectionsClient::class.java)

    fun fetchRoute(origin: LocationInput, destination: LocationInput, waypoints: List<LocationInput>): DirectionsData {
        val apiKey = properties.google.apiKey
        logger.info("Making Google Directions API request with API key: ${if (apiKey.isNotBlank()) "SET (length: ${apiKey.length}, last 4: ${apiKey.takeLast(4)})" else "NOT SET - REQUEST WILL FAIL!"}")

        val uri = UriComponentsBuilder.fromUriString(properties.google.directionsUrl)
            .queryParam("origin", origin.asQueryParam())
            .queryParam("destination", destination.asQueryParam())
            .apply {
                if (waypoints.isNotEmpty()) {
                    queryParam("waypoints", waypoints.joinToString("|") { it.asQueryParam() })
                }
            }
            .queryParam("key", apiKey)
            .build(true)
            .toUri()

        logger.debug("Google Directions API URL (key masked): ${uri.toString().replace(apiKey, "***")}")

        val response = runCatching {
            val body = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String::class.java)
            if (body.isNullOrBlank()) {
                throw ExternalApiException("Google Directions API returned empty response")
            }
            objectMapper.readTree(body)
        }.getOrElse { throw ExternalApiException("Unable to call Google Directions API", it) }

        if (response == null) {
            throw ExternalApiException("Google Directions API returned null response")
        }

        // Check for API errors first
        val status = response.path("status")?.asText("")
        if (status != null && status != "OK") {
            val errorMessage = response.path("error_message")?.asText("")
            throw ExternalApiException("Google Directions API error: $status${if (errorMessage != null) " - $errorMessage" else ""}")
        }

        val routes = response.path("routes")
        if (routes == null || !routes.isArray || routes.isEmpty) {
            throw ExternalApiException("Google Directions API returned no routes. Status: $status")
        }

        val route = routes.firstOrNull()
            ?: throw ExternalApiException("Google Directions API returned empty routes array")

        val overviewPolyline = route.path("overview_polyline").path("points").asText("")
        if (overviewPolyline.isBlank()) {
            throw ExternalApiException("Directions API response missing overview polyline")
        }

        val decodedPoints = PolylineDecoder.decode(overviewPolyline)
        val legs = route.path("legs")
        val totalDistance = legs.sumOf { leg -> leg.path("distance").path("value").asDouble(0.0) }
        val totalDurationSeconds = legs.sumOf { leg -> leg.path("duration").path("value").asDouble(0.0) }
        val startLocation = legs.firstOrNull()?.let { leg ->
            val node = leg.path("start_location")
            if (node.has("lat") && node.has("lng")) GeoPoint(node.path("lat").asDouble(), node.path("lng").asDouble()) else null
        }
        val endLocation = legs.lastOrNull()?.let { leg ->
            val node = leg.path("end_location")
            if (node.has("lat") && node.has("lng")) GeoPoint(node.path("lat").asDouble(), node.path("lng").asDouble()) else null
        }

        return DirectionsData(
            polyline = overviewPolyline,
            points = decodedPoints,
            totalDistanceMeters = totalDistance,
            totalDurationSeconds = totalDurationSeconds,
            startLocation = startLocation,
            endLocation = endLocation
        )
    }

    data class DirectionsData(
        val polyline: String,
        val points: List<GeoPoint>,
        val totalDistanceMeters: Double,
        val totalDurationSeconds: Double,
        val startLocation: GeoPoint?,
        val endLocation: GeoPoint?
    )

}

