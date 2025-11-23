package com.seven.seven.api.dto

import com.fasterxml.jackson.annotation.JsonAlias
import com.seven.seven.external.GeminiClient
import com.seven.seven.ml.model.VehiclePayload
import com.seven.seven.ml.model.VehicleAttributePayload
import com.seven.seven.ml.model.VehicleCostPayload
import com.seven.seven.ml.model.UpsellReasonPayload
import com.seven.seven.service.RecommendationService
import com.seven.seven.shared.model.LocationInput
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
    val origin: LocationDto,
    @field:NotNull
    @field:Valid
    val destination: LocationDto,
    @field:Size(max = 25)
    val waypoints: List<@Valid LocationDto> = emptyList(),
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
    fun originLocation(): LocationInput = origin.toLocationInput()
    fun destinationLocation(): LocationInput = destination.toLocationInput()
    fun waypointLocations(): List<LocationInput> = waypoints.map { it.toLocationInput() }
}

data class LocationDto(
    val lat: Double? = null,
    val lng: Double? = null,
    val place: String? = null
) {
    fun validate() {
        require((lat != null && lng != null) || !place.isNullOrBlank()) {
            "Location requires latitude/longitude or a place query"
        }
    }

    fun toLocationInput(): LocationInput {
        validate()
        return LocationInput.from(lat, lng, place)
    }
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
    val vehicleCostValueEur: Double? = null,
    @JsonAlias("model_annex")
    val modelAnnex: String? = null,
    @JsonAlias("images")
    val images: List<String> = emptyList(),
    @JsonAlias("tyre_type")
    val tyreType: String? = null,
    @JsonAlias("attributes")
    val attributes: List<VehicleAttributeDto> = emptyList(),
    @JsonAlias("vehicle_status")
    val vehicleStatus: String? = null,
    @JsonAlias("vehicle_cost")
    val vehicleCost: VehicleCostDto? = null,
    @JsonAlias("upsell_reasons")
    val upsellReasons: List<UpsellReasonDto> = emptyList()
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
        vehicleCostValueEur = vehicleCostValueEur ?: vehicleCost?.value ?: 0.0,
        modelAnnex = modelAnnex.orEmpty(),
        images = images,
        tyreType = tyreType.orEmpty(),
        attributes = attributes.map { it.toPayload() },
        vehicleStatus = vehicleStatus.orEmpty(),
        vehicleCost = vehicleCost?.toPayload(),
        upsellReasons = upsellReasons.map { it.toPayload() }
    )
}

data class VehicleAttributeDto(
    @JsonAlias("key")
    val key: String = "",
    @JsonAlias("title")
    val title: String = "",
    @JsonAlias("value")
    val value: String = "",
    @JsonAlias("attribute_type")
    val attributeType: String = "",
    @JsonAlias("icon_url")
    val iconUrl: String? = null
) {
    fun toPayload(): VehicleAttributePayload = VehicleAttributePayload(
        key = key,
        title = title,
        value = value,
        attributeType = attributeType,
        iconUrl = iconUrl.orEmpty()
    )
}

data class VehicleCostDto(
    @JsonAlias("currency")
    val currency: String = "",
    @JsonAlias("value")
    val value: Double = 0.0
) {
    fun toPayload(): VehicleCostPayload = VehicleCostPayload(
        currency = currency,
        value = value
    )
}

data class UpsellReasonDto(
    @JsonAlias("title")
    val title: String = "",
    @JsonAlias("description")
    val description: String = ""
) {
    fun toPayload(): UpsellReasonPayload = UpsellReasonPayload(
        title = title,
        description = description
    )
}

data class RecommendationResponseDto(
    val vehicles: List<VehicleFeedbackDto>,
    val feedbackProtection: List<ProtectionFeedbackDto>
) {
    companion object {
        fun from(result: RecommendationService.RecommendationResult) = RecommendationResponseDto(
            vehicles = result.vehicles.map { vehicle ->
                VehicleFeedbackDto(
                    vehicleId = vehicle.vehicleId,
                    rank = vehicle.rank,
                    feedback = vehicle.feedback.map { FeedbackTextDto(it) }
                )
            },
            feedbackProtection = listOf(
                ProtectionFeedbackDto(
                    protectionAll = result.protectionFeedback.protectionAll.map { it.toDto() },
                    protectionSmart = result.protectionFeedback.protectionSmart.map { it.toDto() },
                    protectionBasic = result.protectionFeedback.protectionBasic.map { it.toDto() },
                    protectionNone = result.protectionFeedback.protectionNone.map { it.toDto() }
                )
            )
        )
    }
}

data class VehicleFeedbackDto(
    val vehicleId: String,
    val rank: Int,
    val feedback: List<FeedbackTextDto>
)

data class FeedbackTextDto(
    val feedbackText: String
)

data class ProtectionFeedbackDto(
    val protectionAll: List<ProtectionFeedbackEntryDto>,
    val protectionSmart: List<ProtectionFeedbackEntryDto>,
    val protectionBasic: List<ProtectionFeedbackEntryDto>,
    val protectionNone: List<ProtectionFeedbackEntryDto>
)

data class ProtectionFeedbackEntryDto(
    val feedbackText: String,
    val feedbackType: String
)

private fun GeminiClient.ProtectionFeedbackEntry.toDto() =
    ProtectionFeedbackEntryDto(feedbackText = feedbackText, feedbackType = feedbackType)
