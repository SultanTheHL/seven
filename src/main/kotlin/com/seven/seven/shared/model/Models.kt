package com.seven.seven.shared.model

import java.time.Instant

data class GeoPoint(
    val lat: Double,
    val lng: Double
)

data class UserPreferences(
    val numTravellers: Int,
    val numBags: Int,
    val focus: PreferenceFocus,
    val travelingWithKids: Boolean,
    val confidenceInBadWeather: ConfidenceLevel,
    val automaticPreferred: Boolean
)

enum class PreferenceFocus {
    COMFORT, SAFETY, PRICE
}

enum class ConfidenceLevel {
    LOW, MEDIUM, HIGH
}

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
    val condition: WeatherCondition
)

data class WeatherCondition(
    val type: WeatherType,
    val severity: WeatherSeverity,
    val description: String
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

data class RouteProfile(
    val totalDistanceMeters: Double,
    val elevationSamples: List<ElevationSample>,
    val roadBreakdown: Map<RoadType, Double>,
    val weatherSnapshots: List<WeatherSnapshot>
)

data class SlopeMetrics(
    val maxSlopePercent: Double,
    val totalAscentMeters: Double,
    val totalDescentMeters: Double,
    val averageSlopePercent: Double
)

data class RouteAnalysisResult(
    val slopeMetrics: SlopeMetrics,
    val roadDifficultyScore: Double,
    val weatherSeverityScore: Double,
    val overallDifficultyScore: Double
)

data class RecommendedVehicle(
    val group: String,
    val modelExample: String,
    val reasons: List<String>,
    val upgradeOptions: List<UpgradeOption>
)

data class UpgradeOption(
    val group: String,
    val modelExample: String,
    val priceDelta: String
)

data class ProtectionRecommendation(
    val packageName: String,
    val reason: String
)

data class VehicleRecommendation(
    val recommendedVehicle: RecommendedVehicle,
    val protectionRecommendation: ProtectionRecommendation?
)

data class RecommendationInput(
    val routeProfile: RouteProfile,
    val analysis: RouteAnalysisResult,
    val preferences: UserPreferences,
    val travelDate: Instant,
    val rentalDays: Int
)

