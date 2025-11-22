package com.seven.seven.external

import com.fasterxml.jackson.databind.ObjectMapper
import com.seven.seven.config.ExternalApiProperties
import com.seven.seven.shared.model.GeoPoint
import com.seven.seven.shared.model.RoadSegment
import com.seven.seven.shared.model.RoadType
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class OverpassRoadClient(
    private val restClient: RestClient,
    private val properties: ExternalApiProperties,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(OverpassRoadClient::class.java)

    fun classifyRoads(points: List<GeoPoint>): RoadSegmentsResult {
        if (points.isEmpty()) return RoadSegmentsResult(emptyMap(), emptyList())
        val sampledPoints = downSample(points, MAX_SAMPLE_POINTS)

        val totals = mutableMapOf<RoadType, Int>()
        val segments = mutableListOf<RoadSegment>()

        sampledPoints.forEach { point ->
            val wayTags = queryWayTags(point)
            val (roadType, speedKph) = when (wayTags) {
                null -> RoadType.UNKNOWN to DEFAULT_SPEED_KPH
                else -> {
                    val type = wayTags.highway?.let { highwayTagToRoadType(it) } ?: inferRoadType(wayTags.speedKph)
                    val speedValue = wayTags.speedKph ?: fallbackSpeed(type)
                    type to speedValue
                }
            }

            totals[roadType] = totals.getOrDefault(roadType, 0) + 1
            segments += RoadSegment(point, roadType, speedKph)
        }

        val sum = totals.values.sum()
        val breakdown = if (sum > 0) {
            totals.mapValues { it.value.toDouble() / sum }
        } else {
            defaultBreakdown()
        }

        return RoadSegmentsResult(
            breakdown = breakdown,
            segments = segments
        )
    }

    private fun queryWayTags(point: GeoPoint): WayTags? {
        val query = """
            [out:json][timeout:25];
            way(around:$SEARCH_RADIUS_METERS,${point.lat},${point.lng})["highway"];
            out tags;
        """.trimIndent()

        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8)

        val responseBody = runCatching {
            restClient.post()
                .uri(properties.overpass.url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("data=$encodedQuery")
                .retrieve()
                .body(String::class.java)
        }.getOrElse { throwable ->
            logger.warn("Overpass API call failed near (${point.lat}, ${point.lng}): ${throwable.message}")
            return null
        } ?: return null

        val root = runCatching { objectMapper.readTree(responseBody) }.getOrNull() ?: return null
        val elements = root.path("elements")
        if (!elements.isArray || elements.isEmpty) return null

        val preferred = elements
            .firstOrNull { it.path("type").asText("") == "way" }
            ?: return null

        val tags = preferred.path("tags")
        val highway = tags.path("highway").asText(null)
        val maxSpeedRaw = tags.path("maxspeed").asText(null)
        val parsedSpeed = maxSpeedRaw?.let { parseSpeedKph(it) }

        return WayTags(
            highway = highway,
            speedKph = parsedSpeed
        )
    }

    private fun parseSpeedKph(value: String): Double? {
        val normalized = value.trim().lowercase()
        val numeric = normalized.replace("[^0-9.]".toRegex(), "")
        val number = numeric.toDoubleOrNull() ?: return null
        return if (normalized.contains("mph")) {
            number * MPH_TO_KPH
        } else {
            number
        }
    }

    private fun fallbackSpeed(roadType: RoadType): Double =
        ROAD_TYPE_SPEED_KPH[roadType] ?: DEFAULT_SPEED_KPH

    private fun inferRoadType(speed: Double?): RoadType {
        val value = speed ?: DEFAULT_SPEED_KPH
        return when {
            value >= 120 -> RoadType.MOTORWAY
            value >= 100 -> RoadType.TRUNK
            value >= 90 -> RoadType.PRIMARY
            value >= 80 -> RoadType.SECONDARY
            value >= 70 -> RoadType.TERTIARY
            value >= 50 -> RoadType.RESIDENTIAL
            value >= 30 -> RoadType.SERVICE
            else -> RoadType.UNKNOWN
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

    private fun defaultBreakdown(): Map<RoadType, Double> = mapOf(RoadType.UNKNOWN to 1.0)

    data class RoadSegmentsResult(
        val breakdown: Map<RoadType, Double>,
        val segments: List<RoadSegment>
    )

    private data class WayTags(
        val highway: String?,
        val speedKph: Double?
    )

    companion object {
        private const val MAX_SAMPLE_POINTS = 25
        private const val SEARCH_RADIUS_METERS = 10
        private const val MPH_TO_KPH = 1.60934
        private const val DEFAULT_SPEED_KPH = 50.0

        private val ROAD_TYPE_SPEED_KPH = mapOf(
            RoadType.MOTORWAY to 120.0,
            RoadType.TRUNK to 100.0,
            RoadType.PRIMARY to 90.0,
            RoadType.SECONDARY to 80.0,
            RoadType.TERTIARY to 70.0,
            RoadType.RESIDENTIAL to 50.0,
            RoadType.SERVICE to 30.0,
            RoadType.RAMP to 60.0,
            RoadType.UNKNOWN to DEFAULT_SPEED_KPH
        )

        private fun highwayTagToRoadType(value: String): RoadType = when (value.lowercase()) {
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
    }
}

