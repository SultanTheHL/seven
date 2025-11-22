package com.seven.seven.external

import com.fasterxml.jackson.databind.JsonNode
import com.seven.seven.config.ExternalApiProperties
import com.seven.seven.shared.model.ElevationSample
import com.seven.seven.shared.model.GeoPoint
import com.seven.seven.shared.util.GeoUtils
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder

@Component
class GoogleElevationClient(
    private val restClient: RestClient,
    private val properties: ExternalApiProperties
) {

    fun fetchSamples(points: List<GeoPoint>): List<ElevationSample> {
        if (points.isEmpty()) return emptyList()
        val sampledPoints = downSample(points, MAX_LOCATIONS_PER_REQUEST)
        val locations = sampledPoints.joinToString("|") { "${it.lat},${it.lng}" }

        val uri = UriComponentsBuilder.fromHttpUrl(properties.google.elevationUrl)
            .queryParam("locations", locations)
            .queryParam("key", properties.google.apiKey)
            .build(true)
            .toUri()

        val response = runCatching {
            restClient.get()
                .uri(uri)
                .retrieve()
                .body(JsonNode::class.java)
        }.getOrElse { throw ExternalApiException("Unable to call Google Elevation API", it) }

        val results = response?.path("results") ?: throw ExternalApiException("Elevation API returned no results")

        val distances = GeoUtils.cumulativeDistances(sampledPoints)

        return results.mapIndexed { index, node ->
            val elevation = node.path("elevation").asDouble(0.0)
            val locationNode = node.path("location")
            val lat = locationNode.path("lat").asDouble(sampledPoints[index].lat)
            val lng = locationNode.path("lng").asDouble(sampledPoints[index].lng)
            ElevationSample(
                point = GeoPoint(lat, lng),
                elevationMeters = elevation,
                cumulativeDistanceMeters = distances.getOrElse(index) { distances.lastOrNull() ?: 0.0 }
            )
        }
    }

    private fun downSample(points: List<GeoPoint>, max: Int): List<GeoPoint> {
        if (points.size <= max) return points
        val step = (points.size.toDouble() / max).coerceAtLeast(1.0)
        val sampled = mutableListOf<GeoPoint>()
        var index = 0.0
        while (index < points.size) {
            sampled += points[index.toInt()]
            index += step
        }
        if (sampled.last() != points.last()) sampled += points.last()
        return sampled
    }

    companion object {
        private const val MAX_LOCATIONS_PER_REQUEST = 250
    }
}

