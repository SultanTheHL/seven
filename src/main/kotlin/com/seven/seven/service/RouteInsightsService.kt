package com.seven.seven.service

import com.seven.seven.external.GoogleDirectionsClient
import com.seven.seven.external.GoogleElevationClient
import com.seven.seven.external.OpenWeatherClient
import com.seven.seven.external.OverpassRoadClient
import com.seven.seven.shared.model.ElevationSample
import com.seven.seven.shared.model.GeoPoint
import com.seven.seven.shared.model.RoadSegment
import com.seven.seven.shared.model.RoadType
import com.seven.seven.shared.model.WeatherCondition
import com.seven.seven.shared.model.WeatherMetrics
import com.seven.seven.shared.model.WeatherSeverity
import com.seven.seven.shared.model.WeatherSnapshot
import com.seven.seven.shared.model.WeatherType
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RouteInsightsService(
    private val directionsClient: GoogleDirectionsClient,
    private val elevationClient: GoogleElevationClient,
    private val roadsClient: OverpassRoadClient,
    private val weatherClient: OpenWeatherClient
) {

    fun collectRouteContext(request: RouteInsightsRequest): RouteContext {
        val directions = directionsClient.fetchRoute(request.origin, request.destination, request.waypoints)
        val weatherPoints = listOf(request.origin) + request.waypoints + request.destination

        val elevationSamples = elevationClient.fetchSamples(directions.points)
        val roadData = roadsClient.classifyRoads(directions.points)
        val roadBreakdown = roadData.breakdown.takeUnless { it.isEmpty() }
            ?: mapOf(RoadType.UNKNOWN to 1.0)
        val roadSegments = roadData.segments.takeUnless { it.isEmpty() }
            ?: directions.points.map { RoadSegment(it, RoadType.UNKNOWN, DEFAULT_SPEED_KPH) }
        val weatherSnapshots = weatherClient.fetchSnapshots(weatherPoints, request.travelInstant)

        return RouteContext(
            directions = directions,
            elevationSamples = elevationSamples,
            roadBreakdown = roadBreakdown,
            representativeWeather = ensureWeatherSnapshots(weatherSnapshots, request.travelInstant, weatherPoints),
            roadSegments = roadSegments
        )
    }

    private fun ensureWeatherSnapshots(
        snapshots: List<WeatherSnapshot>,
        travelInstant: Instant,
        points: List<GeoPoint>
    ): WeatherSnapshot {
        val worstSnapshot = snapshots.maxByOrNull { calculateRiskScore(it) }
        return worstSnapshot ?: WeatherSnapshot(
            point = points.first(),
            instant = travelInstant,
            condition = WeatherSnapshotFallback.condition,
            metrics = WeatherSnapshotFallback.metrics
        )
    }

    private fun calculateRiskScore(snapshot: WeatherSnapshot): Double {
        val metrics = snapshot.metrics
        val severityScore = snapshot.condition.severity.weight * 100
        val precipitationScore = (metrics.rainVolumeLastHour + metrics.snowVolumeLastHour) * 10
        val windScore = metrics.windSpeedMetersPerSecond
        val visibilityPenalty = (MAX_VISIBILITY_METERS - metrics.visibilityMeters)
            .coerceAtLeast(0) / 100.0
        return severityScore + precipitationScore + windScore + visibilityPenalty
    }

    data class RouteInsightsRequest(
        val origin: GeoPoint,
        val destination: GeoPoint,
        val waypoints: List<GeoPoint>,
        val travelInstant: Instant
    )

    data class RouteContext(
        val directions: GoogleDirectionsClient.DirectionsData,
        val elevationSamples: List<ElevationSample>,
        val roadBreakdown: Map<RoadType, Double>,
        val representativeWeather: WeatherSnapshot,
        val roadSegments: List<RoadSegment>
    )

    private object WeatherSnapshotFallback {
        val condition = WeatherCondition(
            type = WeatherType.CLEAR,
            severity = WeatherSeverity.LOW,
            description = "Clear (fallback)"
        )

        val metrics = WeatherMetrics(
            conditionId = 800,
            temperatureCelsius = 20.0,
            windSpeedMetersPerSecond = 2.0,
            rainVolumeLastHour = 0.0,
            snowVolumeLastHour = 0.0,
            visibilityMeters = MAX_VISIBILITY_METERS
        )
    }
    companion object {
        private const val DEFAULT_SPEED_KPH = 50.0
        private const val MAX_VISIBILITY_METERS = 10000
    }
}

