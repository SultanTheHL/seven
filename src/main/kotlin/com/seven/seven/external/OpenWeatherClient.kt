package com.seven.seven.external

import com.fasterxml.jackson.databind.JsonNode
import com.seven.seven.config.ExternalApiProperties
import com.seven.seven.shared.model.GeoPoint
import com.seven.seven.shared.model.WeatherCondition
import com.seven.seven.shared.model.WeatherSeverity
import com.seven.seven.shared.model.WeatherSnapshot
import com.seven.seven.shared.model.WeatherType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.time.Duration
import java.time.Instant

@Component
class OpenWeatherClient(
    private val restClient: RestClient,
    private val properties: ExternalApiProperties
) {

    fun fetchSnapshots(points: List<GeoPoint>, travelInstant: Instant): List<WeatherSnapshot> {
        if (points.isEmpty()) return emptyList()
        val sampledPoints = pointsToCheck(points)
        return sampledPoints.mapNotNull { point ->
            runCatching { fetchSnapshot(point, travelInstant) }.getOrNull()
        }
    }

    private fun fetchSnapshot(point: GeoPoint, travelInstant: Instant): WeatherSnapshot? {
        val uri = UriComponentsBuilder.fromHttpUrl(properties.weather.forecastUrl)
            .queryParam("lat", point.lat)
            .queryParam("lon", point.lng)
            .queryParam("appid", properties.weather.apiKey)
            .queryParam("units", "metric")
            .build(true)
            .toUri()

        val response = restClient.get()
            .uri(uri)
            .retrieve()
            .body(JsonNode::class.java)
            ?: return null

        val listNode = response.path("list")
        if (!listNode.isArray || listNode.isEmpty) return null

        val closestNode = listNode.minByOrNull { forecast ->
            val timestamp = forecast.path("dt").asLong()
            val forecastInstant = Instant.ofEpochSecond(timestamp)
            Duration.between(travelInstant, forecastInstant).abs()
        } ?: listNode.first()

        val weatherNode = closestNode.path("weather").firstOrNull()
        val description = weatherNode?.path("description")?.asText()?.replaceFirstChar { it.uppercase() } ?: "N/A"
        val type = weatherNode?.path("main")?.asText()?.toWeatherType() ?: WeatherType.UNKNOWN

        val severity = computeSeverity(type, closestNode)

        return WeatherSnapshot(
            point = point,
            instant = Instant.ofEpochSecond(closestNode.path("dt").asLong()),
            condition = WeatherCondition(
                type = type,
                severity = severity,
                description = description
            )
        )
    }

    private fun computeSeverity(type: WeatherType, node: JsonNode): WeatherSeverity {
        val windSpeed = node.path("wind").path("speed").asDouble(0.0)
        val rain = node.path("rain").path("3h").asDouble(0.0)
        val snow = node.path("snow").path("3h").asDouble(0.0)

        val base = when (type) {
            WeatherType.CLEAR -> WeatherSeverity.LOW
            WeatherType.CLOUDS -> WeatherSeverity.LOW
            WeatherType.RAIN, WeatherType.WIND -> WeatherSeverity.MEDIUM
            WeatherType.SNOW, WeatherType.STORM, WeatherType.EXTREME -> WeatherSeverity.HIGH
            WeatherType.FOG -> WeatherSeverity.MEDIUM
            WeatherType.UNKNOWN -> WeatherSeverity.MEDIUM
        }

        val additionalRisk = when {
            windSpeed > 12 || rain > 10 || snow > 5 -> WeatherSeverity.HIGH
            windSpeed > 8 || rain > 5 || snow > 2 -> WeatherSeverity.MEDIUM
            else -> WeatherSeverity.LOW
        }

        return if (base.weight >= additionalRisk.weight) base else additionalRisk
    }

    private fun String.toWeatherType(): WeatherType = when (lowercase()) {
        "clear" -> WeatherType.CLEAR
        "clouds" -> WeatherType.CLOUDS
        "rain" -> WeatherType.RAIN
        "snow" -> WeatherType.SNOW
        "storm", "thunderstorm" -> WeatherType.STORM
        "drizzle" -> WeatherType.RAIN
        "mist", "fog" -> WeatherType.FOG
        "squall", "tornado" -> WeatherType.EXTREME
        else -> WeatherType.UNKNOWN
    }

    private fun pointsToCheck(points: List<GeoPoint>): List<GeoPoint> {
        val unique = points.distinct()
        if (unique.size <= 3) return unique
        val first = unique.first()
        val middle = unique[unique.size / 2]
        val last = unique.last()
        return listOf(first, middle, last)
    }
}

