package com.seven.seven.shared.logic

import com.seven.seven.shared.model.ElevationSample
import com.seven.seven.shared.model.RoadType
import com.seven.seven.shared.model.RouteAnalysisResult
import com.seven.seven.shared.model.RouteProfile
import com.seven.seven.shared.model.SlopeMetrics
import com.seven.seven.shared.model.WeatherSeverity
import org.springframework.stereotype.Component
import kotlin.math.abs
import kotlin.math.min

@Component
class RouteAnalysisService {

    fun analyze(profile: RouteProfile): RouteAnalysisResult {
        val slopeMetrics = calculateSlopeMetrics(profile.elevationSamples)
        val roadDifficultyScore = computeRoadDifficultyScore(profile.roadBreakdown)
        val weatherSeverityScore = computeWeatherSeverityScore(profile)
        val slopeScore = min(slopeMetrics.maxSlopePercent / SLOPE_REFERENCE_PERCENT, 1.0)
        val overall = (slopeScore * 0.4) + (roadDifficultyScore * 0.3) + (weatherSeverityScore * 0.3)
        return RouteAnalysisResult(
            slopeMetrics = slopeMetrics,
            roadDifficultyScore = roadDifficultyScore,
            weatherSeverityScore = weatherSeverityScore,
            overallDifficultyScore = overall
        )
    }

    fun calculateSlopeMetrics(samples: List<ElevationSample>): SlopeMetrics {
        if (samples.size < 2) {
            return SlopeMetrics(
                maxSlopePercent = 0.0,
                totalAscentMeters = 0.0,
                totalDescentMeters = 0.0,
                averageSlopePercent = 0.0
            )
        }

        var maxSlope = 0.0
        var ascent = 0.0
        var descent = 0.0
        var slopeSum = 0.0
        var slopeCount = 0

        samples.zipWithNext().forEach { (previous, current) ->
            val deltaElevation = current.elevationMeters - previous.elevationMeters
            val deltaDistance = current.cumulativeDistanceMeters - previous.cumulativeDistanceMeters
            if (deltaDistance <= 0.0) return@forEach

            if (deltaElevation > 0) {
                ascent += deltaElevation
            } else {
                descent += abs(deltaElevation)
            }

            val slopePercent = (deltaElevation / deltaDistance) * 100
            maxSlope = maxOf(maxSlope, abs(slopePercent))
            slopeSum += abs(slopePercent)
            slopeCount++
        }

        val averageSlope = if (slopeCount == 0) 0.0 else slopeSum / slopeCount

        return SlopeMetrics(
            maxSlopePercent = maxSlope,
            totalAscentMeters = ascent,
            totalDescentMeters = descent,
            averageSlopePercent = averageSlope
        )
    }

    fun computeRoadDifficultyScore(breakdown: Map<RoadType, Double>): Double {
        if (breakdown.isEmpty()) return 0.0
        val weighted = breakdown.entries.sumOf { (type, fraction) ->
            fraction * (ROAD_TYPE_WEIGHTS[type] ?: DEFAULT_ROAD_WEIGHT)
        }
        return min(weighted, 1.0)
    }

    fun computeWeatherSeverityScore(profile: RouteProfile): Double {
        if (profile.weatherSnapshots.isEmpty()) return 0.0
        val severityAverage = profile.weatherSnapshots
            .map { it.condition.severity.weight }
            .average()
        return min(severityAverage, 1.0)
    }

    companion object {
        private const val SLOPE_REFERENCE_PERCENT = 18.0

        private val ROAD_TYPE_WEIGHTS = mapOf(
            RoadType.MOTORWAY to 0.1,
            RoadType.TRUNK to 0.2,
            RoadType.PRIMARY to 0.3,
            RoadType.SECONDARY to 0.4,
            RoadType.TERTIARY to 0.5,
            RoadType.RESIDENTIAL to 0.7,
            RoadType.SERVICE to 0.8,
            RoadType.RAMP to 0.6,
            RoadType.UNKNOWN to 0.5
        )

        private const val DEFAULT_ROAD_WEIGHT = 0.5
    }
}

