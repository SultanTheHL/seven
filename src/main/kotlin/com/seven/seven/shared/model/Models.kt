package com.seven.seven.shared.model

import java.time.Instant

data class GeoPoint(
    val lat: Double,
    val lng: Double
)

data class ElevationSample(
    val point: GeoPoint,
    val elevationMeters: Double,
    val cumulativeDistanceMeters: Double
)

enum class RoadType {
    MOTORWAY,
    TRUNK,
    PRIMARY,
    SECONDARY,
    TERTIARY,
    RESIDENTIAL,
    SERVICE,
    RAMP,
    UNKNOWN
}

data class WeatherSnapshot(
    val point: GeoPoint,
    val instant: Instant,
    val condition: WeatherCondition,
    val metrics: WeatherMetrics
)

data class WeatherCondition(
    val type: WeatherType,
    val severity: WeatherSeverity,
    val description: String
)

data class WeatherMetrics(
    val conditionId: Int,
    val temperatureCelsius: Double,
    val windSpeedMetersPerSecond: Double,
    val rainVolumeLastHour: Double,
    val snowVolumeLastHour: Double,
    val visibilityMeters: Int
)

enum class WeatherType {
    CLEAR,
    CLOUDS,
    RAIN,
    SNOW,
    STORM,
    FOG,
    EXTREME,
    WIND,
    UNKNOWN
}

enum class WeatherSeverity(val weight: Double) {
    LOW(0.2),
    MEDIUM(0.6),
    HIGH(1.0)
}

data class RoadSegment(
    val point: GeoPoint,
    val roadType: RoadType,
    val speedKph: Double
)


