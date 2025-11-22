package com.seven.seven.api.dto

import com.seven.seven.shared.model.ConfidenceLevel
import com.seven.seven.shared.model.GeoPoint
import com.seven.seven.shared.model.PreferenceFocus
import com.seven.seven.shared.model.ProtectionRecommendation
import com.seven.seven.shared.model.RecommendedVehicle
import com.seven.seven.shared.model.UserPreferences
import com.seven.seven.shared.model.VehicleRecommendation
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

data class RecommendationRequestDto(
    @field:NotNull
    @field:Valid
    val origin: CoordinateDto,
    @field:NotNull
    @field:Valid
    val destination: CoordinateDto,
    @field:Size(max = 25)
    val waypoints: List<@Valid CoordinateDto> = emptyList(),
    @field:NotNull
    val travelDate: Instant,
    @field:Min(1)
    val rentalDays: Int,
    @field:NotNull
    @field:Valid
    val preferences: PreferencesDto
) {
    fun originPoint(): GeoPoint = origin.toGeoPoint()
    fun destinationPoint(): GeoPoint = destination.toGeoPoint()
    fun waypointPoints(): List<GeoPoint> = waypoints.map { it.toGeoPoint() }
    fun toPreferences(): UserPreferences = preferences.toModel()
}

data class CoordinateDto(
    val lat: Double,
    val lng: Double
) {
    fun toGeoPoint(): GeoPoint = GeoPoint(lat, lng)
}

data class PreferencesDto(
    val numTravellers: Int,
    val numBags: Int,
    val focus: String,
    val kids: Boolean = false,
    val confidenceInBadWeather: String = "medium",
    val automaticPreferred: Boolean = true
) {
    fun toModel(): UserPreferences = UserPreferences(
        numTravellers = numTravellers,
        numBags = numBags,
        focus = focus.toPreferenceFocus(),
        travelingWithKids = kids,
        confidenceInBadWeather = confidenceInBadWeather.toConfidence(),
        automaticPreferred = automaticPreferred
    )
}

fun String.toPreferenceFocus(): PreferenceFocus = PreferenceFocus.entries
    .firstOrNull { it.name.equals(this, ignoreCase = true) }
    ?: PreferenceFocus.COMFORT

fun String.toConfidence(): ConfidenceLevel = ConfidenceLevel.entries
    .firstOrNull { it.name.equals(this, ignoreCase = true) }
    ?: ConfidenceLevel.MEDIUM

data class RecommendationResponseDto(
    val recommendedVehicle: RecommendedVehicleDto,
    val protectionRecommendation: ProtectionRecommendationDto?
) {
    companion object {
        fun from(recommendation: VehicleRecommendation) = RecommendationResponseDto(
            recommendedVehicle = RecommendedVehicleDto.from(recommendation.recommendedVehicle),
            protectionRecommendation = recommendation.protectionRecommendation?.let {
                ProtectionRecommendationDto.from(it)
            }
        )
    }
}

data class RecommendedVehicleDto(
    val group: String,
    val modelExample: String,
    val reasons: List<String>,
    val upgradeOptions: List<UpgradeOptionDto>
) {
    companion object {
        fun from(vehicle: RecommendedVehicle) = RecommendedVehicleDto(
            group = vehicle.group,
            modelExample = vehicle.modelExample,
            reasons = vehicle.reasons,
            upgradeOptions = vehicle.upgradeOptions.map { UpgradeOptionDto(it.group, it.modelExample, it.priceDelta) }
        )
    }
}

data class UpgradeOptionDto(
    val group: String,
    val modelExample: String,
    val priceDelta: String
)

data class ProtectionRecommendationDto(
    val packageName: String,
    val reason: String
) {
    companion object {
        fun from(recommendation: ProtectionRecommendation) = ProtectionRecommendationDto(
            packageName = recommendation.packageName,
            reason = recommendation.reason
        )
    }
}

