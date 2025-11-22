package com.seven.seven.external

import com.fasterxml.jackson.databind.JsonNode
import com.seven.seven.config.ExternalApiProperties
import com.seven.seven.shared.model.GeoPoint
import com.seven.seven.shared.model.RoadType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder

@Component
class GoogleRoadsClient(
    private val restClient: RestClient,
    private val properties: ExternalApiProperties
) {

    fun classifyRoads(points: List<GeoPoint>): Map<RoadType, Double> {
        if (points.isEmpty()) return emptyMap()
        val sampledPoints = downSample(points, MAX_PATH_POINTS)
        val path = sampledPoints.joinToString("|") { "${it.lat},${it.lng}" }

        val uri = UriComponentsBuilder.fromHttpUrl(properties.google.roadsUrl)
            .queryParam("path", path)
            .queryParam("interpolate", true)
            .queryParam("key", properties.google.apiKey)
            .build(true)
            .toUri()

        val response = runCatching {
            restClient.get()
                .uri(uri)
                .retrieve()
                .body(JsonNode::class.java)
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
        private const val MAX_PATH_POINTS = 100
    }
}

