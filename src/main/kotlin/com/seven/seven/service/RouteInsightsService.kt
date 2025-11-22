package com.seven.seven.service

import com.seven.seven.external.GoogleDirectionsClient
import com.seven.seven.external.GoogleElevationClient
import com.seven.seven.external.GoogleRoadsClient
import com.seven.seven.external.OpenWeatherClient
import com.seven.seven.shared.model.GeoPoint
import com.seven.seven.shared.model.RoadType
import com.seven.seven.shared.model.RouteProfile
import com.seven.seven.shared.model.WeatherCondition
import com.seven.seven.shared.model.WeatherSeverity
import com.seven.seven.shared.model.WeatherSnapshot
import com.seven.seven.shared.model.WeatherType
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RouteInsightsService(
    private val directionsClient: GoogleDirectionsClient,
    private val elevationClient: GoogleElevationClient,
    private val roadsClient: GoogleRoadsClient,
    private val weatherClient: OpenWeatherClient
) {

    fun buildProfile(request: RouteInsightsRequest): RouteProfile {
        val directions = directionsClient.fetchRoute(request.origin, request.destination, request.waypoints)
        val weatherPoints = listOf(request.origin) + request.waypoints + request.destination

        val elevationSamples = elevationClient.fetchSamples(directions.points)
        val roadBreakdown = roadsClient.classifyRoads(directions.points).takeUnless { it.isEmpty() }
            ?: mapOf(RoadType.UNKNOWN to 1.0)
        val weatherSnapshots = weatherClient.fetchSnapshots(weatherPoints, request.travelInstant)

        return RouteProfile(
            totalDistanceMeters = directions.totalDistanceMeters,
            elevationSamples = elevationSamples,
            roadBreakdown = roadBreakdown,
            weatherSnapshots = ensureWeatherSnapshots(weatherSnapshots, request.travelInstant, weatherPoints)
        )
    }

    private fun ensureWeatherSnapshots(
        snapshots: List<WeatherSnapshot>,
        travelInstant: Instant,
        points: List<GeoPoint>
    ): List<WeatherSnapshot> {
        if (snapshots.isNotEmpty()) return snapshots
        // fallback minimal stub to keep downstream logic working
        return points.take(1).map {
            WeatherSnapshot(
                point = it,
                instant = travelInstant,
                condition = WeatherSnapshotFallback.condition
            )
        }
    }

    data class RouteInsightsRequest(
        val origin: GeoPoint,
        val destination: GeoPoint,
        val waypoints: List<GeoPoint>,
        val travelInstant: Instant
    )

    private object WeatherSnapshotFallback {
        val condition = WeatherCondition(
            type = WeatherType.CLEAR,
            severity = WeatherSeverity.LOW,
            description = "Clear (fallback)"
        )
    }
}

