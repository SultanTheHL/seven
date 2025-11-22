package com.seven.seven.external

import com.fasterxml.jackson.databind.JsonNode
import com.seven.seven.config.ExternalApiProperties
import com.seven.seven.shared.model.GeoPoint
import com.seven.seven.shared.util.PolylineDecoder
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder

@Component
class GoogleDirectionsClient(
    private val restClient: RestClient,
    private val properties: ExternalApiProperties
) {

    fun fetchRoute(origin: GeoPoint, destination: GeoPoint, waypoints: List<GeoPoint>): DirectionsData {
        val uri = UriComponentsBuilder.fromHttpUrl(properties.google.directionsUrl)
            .queryParam("origin", origin.asQueryParam())
            .queryParam("destination", destination.asQueryParam())
            .apply {
                if (waypoints.isNotEmpty()) {
                    queryParam("waypoints", waypoints.joinToString("|") { it.asQueryParam() })
                }
            }
            .queryParam("key", properties.google.apiKey)
            .build(true)
            .toUri()

        val response = runCatching {
            restClient.get()
                .uri(uri)
                .retrieve()
                .body(JsonNode::class.java)
        }.getOrElse { throw ExternalApiException("Unable to call Google Directions API", it) }

        val route = response?.path("routes")?.firstOrNull()
            ?: throw ExternalApiException("Google Directions API returned no routes")

        val overviewPolyline = route.path("overview_polyline").path("points").asText("")
        if (overviewPolyline.isBlank()) {
            throw ExternalApiException("Directions API response missing overview polyline")
        }

        val decodedPoints = PolylineDecoder.decode(overviewPolyline)
        val totalDistance = route.path("legs")
            .sumOf { leg -> leg.path("distance").path("value").asDouble(0.0) }

        return DirectionsData(
            polyline = overviewPolyline,
            points = decodedPoints,
            totalDistanceMeters = totalDistance
        )
    }

    data class DirectionsData(
        val polyline: String,
        val points: List<GeoPoint>,
        val totalDistanceMeters: Double
    )

    private fun GeoPoint.asQueryParam(): String = "${this.lat},${this.lng}"
}

