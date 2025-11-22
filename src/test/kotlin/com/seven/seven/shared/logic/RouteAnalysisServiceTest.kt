package com.seven.seven.shared.logic

import com.seven.seven.shared.model.ElevationSample
import com.seven.seven.shared.model.GeoPoint
import com.seven.seven.shared.model.RoadType
import com.seven.seven.shared.model.RouteProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RouteAnalysisServiceTest {

    private val service = RouteAnalysisService()

    @Test
    fun `calculates slope metrics`() {
        val samples = listOf(
            ElevationSample(GeoPoint(0.0, 0.0), 100.0, 0.0),
            ElevationSample(GeoPoint(0.0, 0.1), 150.0, 1000.0),
            ElevationSample(GeoPoint(0.0, 0.2), 130.0, 2000.0)
        )

        val metrics = service.calculateSlopeMetrics(samples)

        assertEquals(5.0, metrics.maxSlopePercent, 0.1)
        assertEquals(50.0, metrics.totalAscentMeters, 0.1)
        assertEquals(20.0, metrics.totalDescentMeters, 0.1)
    }

    @Test
    fun `computes overall analysis`() {
        val samples = listOf(
            ElevationSample(GeoPoint(0.0, 0.0), 100.0, 0.0),
            ElevationSample(GeoPoint(0.0, 0.1), 150.0, 500.0)
        )
        val profile = RouteProfile(
            totalDistanceMeters = 5000.0,
            elevationSamples = samples,
            roadBreakdown = mapOf(RoadType.RESIDENTIAL to 0.6, RoadType.MOTORWAY to 0.4),
            weatherSnapshots = emptyList()
        )

        val analysis = service.analyze(profile)

        assertEquals(0.0, analysis.weatherSeverityScore, 0.01)
        assertEquals(true, analysis.roadDifficultyScore > 0)
        assertEquals(true, analysis.overallDifficultyScore > 0)
    }
}

