package com.seven.seven.external

import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import com.seven.seven.config.ExternalApiProperties
import com.seven.seven.shared.model.ElevationSample
import com.seven.seven.shared.model.GeoPoint
import com.seven.seven.shared.util.GeoUtils
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class GoogleElevationClient(
    private val restClient: RestClient,
    private val properties: ExternalApiProperties,
    private val objectMapper: ObjectMapper
) {

    fun fetchSamples(points: List<GeoPoint>): List<ElevationSample> {
        if (points.isEmpty()) return emptyList()
        val sampledPoints = downSample(points, MAX_LOCATIONS_PER_REQUEST)
        val locations = sampledPoints.joinToString("|") { "${it.lat},${it.lng}" }

        // URL encode the locations parameter manually since it contains '|' characters
        val encodedLocations = URLEncoder.encode(locations, StandardCharsets.UTF_8)
        
        val uri = UriComponentsBuilder.fromUriString(properties.google.elevationUrl)
            .queryParam("locations", encodedLocations)
            .queryParam("key", properties.google.apiKey)
            .build(true)
            .toUri()

        val response = runCatching {
            val body = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String::class.java)
            objectMapper.readTree(body)
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

