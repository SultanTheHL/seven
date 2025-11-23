package com.seven.seven.api.dto

import com.fasterxml.jackson.annotation.JsonAlias
import com.seven.seven.ml.model.VehiclePayload
import com.seven.seven.service.RecommendationService
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
    val preferences: PreferencesDto,
    @JsonAlias("booking_id")
    val bookingId: String? = null
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
    @JsonAlias("people_count")
    val peopleCount: Int,
    @field:Min(0)
    @JsonAlias("luggage_big_count")
    val luggageBigCount: Int,
    @field:Min(0)
    @JsonAlias("luggage_small_count")
    val luggageSmallCount: Int,
    @field:NotBlank
    val preference: String,
    @field:Min(0)
    @field:Max(2)
    val automatic: Int,
    @field:NotBlank
    @JsonAlias("driving_skills")
    val drivingSkills: String,
    @field:Valid
    @JsonAlias("current_vehicle")
    val currentVehicle: VehicleDto? = null
) {
    fun toVehiclePayload(): VehiclePayload? = currentVehicle?.toPayload()
}

data class VehicleDto(
    @JsonAlias("id")
    val id: String? = null,
    @JsonAlias("brand")
    val brand: String? = null,
    @JsonAlias("model")
    val model: String? = null,
    @JsonAlias("acriss_code")
    val acrissCode: String? = null,
    @JsonAlias("group_type")
    val groupType: String? = null,
    @JsonAlias("transmission_type")
    val transmissionType: String? = null,
    @JsonAlias("fuel_type")
    val fuelType: String? = null,
    @JsonAlias("passengers_count")
    val passengersCount: Int? = null,
    @JsonAlias("bags_count")
    val bagsCount: Int? = null,
    @JsonAlias("is_new_car")
    val isNewCar: Boolean? = null,
    @JsonAlias("is_recommended")
    val isRecommended: Boolean? = null,
    @JsonAlias("is_more_luxury")
    val isMoreLuxury: Boolean? = null,
    @JsonAlias("is_exciting_discount")
    val isExcitingDiscount: Boolean? = null,
    @JsonAlias("vehicle_cost_value_eur")
    val vehicleCostValueEur: Double? = null
) {
    fun toPayload(): VehiclePayload = VehiclePayload(
        id = id ?: VehiclePayload.ZERO_UUID,
        brand = brand.orEmpty(),
        model = model.orEmpty(),
        acrissCode = acrissCode.orEmpty(),
        groupType = groupType.orEmpty(),
        transmissionType = transmissionType.orEmpty(),
        fuelType = fuelType.orEmpty(),
        passengersCount = passengersCount ?: 0,
        bagsCount = bagsCount ?: 0,
        isNewCar = isNewCar ?: false,
        isRecommended = isRecommended ?: false,
        isMoreLuxury = isMoreLuxury ?: false,
        isExcitingDiscount = isExcitingDiscount ?: false,
        vehicleCostValueEur = vehicleCostValueEur ?: 0.0
    )
}

data class RecommendationResponseDto(
    val vehicleId: String,
    val feedback: String
) {
    companion object {
        fun from(result: RecommendationService.RecommendationResult) = RecommendationResponseDto(
            vehicleId = result.id,
            feedback = result.feedback
        )
    }
}
