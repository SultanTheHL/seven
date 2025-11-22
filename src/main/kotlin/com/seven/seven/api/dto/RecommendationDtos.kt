package com.seven.seven.api.dto

import com.seven.seven.ml.model.MlRecommendationResponse
import com.seven.seven.ml.model.VehiclePayload
import com.seven.seven.shared.model.GeoPoint
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
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
}

data class CoordinateDto(
    val lat: Double,
    val lng: Double
) {
    fun toGeoPoint(): GeoPoint = GeoPoint(lat, lng)
}

data class PreferencesDto(
    @field:Min(1)
    val peopleCount: Int,
    @field:Min(0)
    val luggageBigCount: Int,
    @field:Min(0)
    val luggageSmallCount: Int,
    @field:NotBlank
    val preference: String,
    @field:Min(0)
    @field:Max(2)
    val automatic: Int,
    @field:NotBlank
    val drivingSkills: String,
    @field:Valid
    val currentVehicle: VehicleDto? = null
) {
    fun toVehiclePayload(): VehiclePayload? = currentVehicle?.toPayload()
}

data class VehicleDto(
    val id: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val acrissCode: String? = null,
    val groupType: String? = null,
    val transmissionType: String? = null,
    val fuelType: String? = null,
    val passengersCount: Int? = null,
    val bagsCount: Int? = null,
    val isNewCar: Boolean? = null,
    val isRecommended: Boolean? = null,
    val isMoreLuxury: Boolean? = null,
    val isExcitingDiscount: Boolean? = null,
    val vehicleCostValueEur: Double? = null
) {
    fun toPayload(): VehiclePayload = VehiclePayload(
        id = id,
        brand = brand,
        model = model,
        acrissCode = acrissCode,
        groupType = groupType,
        transmissionType = transmissionType,
        fuelType = fuelType,
        passengersCount = passengersCount,
        bagsCount = bagsCount,
        isNewCar = isNewCar,
        isRecommended = isRecommended,
        isMoreLuxury = isMoreLuxury,
        isExcitingDiscount = isExcitingDiscount,
        vehicleCostValueEur = vehicleCostValueEur
    )
}

data class RecommendationResponseDto(
    val vehicleId: String,
    val feedback: String
) {
    companion object {
        fun from(response: MlRecommendationResponse) = RecommendationResponseDto(
            vehicleId = response.vehicleId,
            feedback = response.feedback
        )
    }
}
