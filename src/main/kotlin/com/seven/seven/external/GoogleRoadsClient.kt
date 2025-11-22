package com.seven.seven.external

import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import com.seven.seven.config.ExternalApiProperties
import com.seven.seven.shared.model.GeoPoint
import com.seven.seven.shared.model.RoadType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class GoogleRoadsClient(
    private val restClient: RestClient,
    private val properties: ExternalApiProperties,
    private val objectMapper: ObjectMapper
) {

    fun classifyRoads(points: List<GeoPoint>): Map<RoadType, Double> {
        if (points.isEmpty()) return emptyMap()
        val sampledPoints = downSample(points, MAX_PATH_POINTS)
        val path = sampledPoints.joinToString("|") { "${it.lat},${it.lng}" }

        // URL encode the path parameter manually since it contains '|' characters
        val encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8)
        
        val uri = UriComponentsBuilder.fromUriString(properties.google.roadsUrl)
            .queryParam("path", encodedPath)
            .queryParam("interpolate", true)
            .queryParam("key", properties.google.apiKey)
            .build(true)
            .toUri()

        val response = runCatching {
            val body = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String::class.java)
            objectMapper.readTree(body)
        }.getOrElse { throw ExternalApiException("Unable to call Google Roads API", it) }

        val snappedPoints = response?.path("snappedPoints") ?: return defaultBreakdown()

        val totals = mutableMapOf<RoadType, Int>()
        snappedPoints.forEach { node ->
            val roadClass = node.path("roadClass").asText("")
            val type = roadClass.toRoadType()
            totals[type] = totals.getOrDefault(type, 0) + 1
        }

        val sum = totals.values.sum().takeIf { it > 0 } ?: return defaultBreakdown()

        return totals.mapValues { it.value.toDouble() / sum }
    }

    private fun String.toRoadType(): RoadType = when (lowercase()) {
        "motorway" -> RoadType.MOTORWAY
        "trunk" -> RoadType.TRUNK
        "primary" -> RoadType.PRIMARY
        "secondary" -> RoadType.SECONDARY
        "tertiary" -> RoadType.TERTIARY
        "residential" -> RoadType.RESIDENTIAL
        "service" -> RoadType.SERVICE
        "ramp" -> RoadType.RAMP
        else -> RoadType.UNKNOWN
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

    private fun defaultBreakdown(): Map<RoadType, Double> = mapOf(RoadType.UNKNOWN to 1.0)

    companion object {
        private const val MAX_PATH_POINTS = 25
    }
}

