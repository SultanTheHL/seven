package com.seven.seven.ml.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonalInfoPayload(
    @JsonProperty("people_count")
    val peopleCount: Int,

    @JsonProperty("language_big_count")
    val luggageBigCount: Int,

    @JsonProperty("language_small_count")
    val luggageSmallCount: Int,

    @JsonProperty("road_coordinates")
    val roadCoordinates: List<RoadCoordinatePayload>,

    @JsonProperty("trip_length_km")
    val tripLengthKm: Double,

    @JsonProperty("trip_length_hours")
    val tripLengthHours: Int,

    @JsonProperty("condition_id")
    val conditionId: Int,

    @JsonProperty("temperature_c")
    val temperatureCelsius: Double,

    @JsonProperty("wind_speed_mps")
    val windSpeedMetersPerSecond: Double,

    @JsonProperty("rain_volume_1h")
    val rainVolumeLastHour: Double,

    @JsonProperty("snow_volume_1h")
    val snowVolumeLastHour: Double,

    @JsonProperty("visibility_m")
    val visibilityMeters: Int,

    @JsonProperty("current_vehicle")
    val currentVehicle: VehiclePayload?,

    @JsonProperty("preference")
    val preference: String,

    @JsonProperty("automatic")
    val automaticPreference: Int,

    @JsonProperty("driving_skills")
    val drivingSkills: String,

    @JsonProperty("parking_difficulty")
    val parkingDifficulty: Int
) {
    fun toWirePayload(): PersonalInfoWirePayload = PersonalInfoWirePayload(
        peopleCount = peopleCount,
        luggageBigCount = luggageBigCount,
        luggageSmallCount = luggageSmallCount,
        roadCoordinates = roadCoordinates.map { it.toTuple() },
        tripLengthKm = tripLengthKm,
        tripLengthHours = tripLengthHours,
        conditionId = conditionId,
        temperatureCelsius = temperatureCelsius,
        windSpeedMetersPerSecond = windSpeedMetersPerSecond,
        rainVolumeLastHour = rainVolumeLastHour,
        snowVolumeLastHour = snowVolumeLastHour,
        visibilityMeters = visibilityMeters,
        currentVehicle = currentVehicle ?: VehiclePayload(),
        preference = preference,
        automaticPreference = automaticPreference,
        drivingSkills = drivingSkills,
        parkingDifficulty = parkingDifficulty
    )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonalInfoWirePayload(
    @JsonProperty("people_count")
    val peopleCount: Int,

    @JsonProperty("language_big_count")
    val luggageBigCount: Int,

    @JsonProperty("language_small_count")
    val luggageSmallCount: Int,

    @JsonProperty("road_coordinates")
    val roadCoordinates: List<List<Double>>,

    @JsonProperty("trip_length_km")
    val tripLengthKm: Double,

    @JsonProperty("trip_length_hours")
    val tripLengthHours: Int,

    @JsonProperty("condition_id")
    val conditionId: Int,

    @JsonProperty("temperature_c")
    val temperatureCelsius: Double,

    @JsonProperty("wind_speed_mps")
    val windSpeedMetersPerSecond: Double,

    @JsonProperty("rain_volume_1h")
    val rainVolumeLastHour: Double,

    @JsonProperty("snow_volume_1h")
    val snowVolumeLastHour: Double,

    @JsonProperty("visibility_m")
    val visibilityMeters: Int,

    @JsonProperty("current_vehicle")
    val currentVehicle: VehiclePayload,

    @JsonProperty("preference")
    val preference: String,

    @JsonProperty("automatic")
    val automaticPreference: Int,

    @JsonProperty("driving_skills")
    val drivingSkills: String,

    @JsonProperty("parking_difficulty")
    val parkingDifficulty: Int
)

data class RoadCoordinatePayload(
    @JsonProperty("lon")
    val longitude: Double,

    @JsonProperty("lat")
    val latitude: Double,

    @JsonProperty("elevation")
    val elevationMeters: Double,

    @JsonProperty("speed")
    val speedKph: Double
) {
    fun toTuple(): List<Double> = listOf(longitude, latitude, elevationMeters, speedKph)
}

data class VehiclePayload(
    @JsonProperty("id")
    val id: String = ZERO_UUID,
    @JsonProperty("brand")
    val brand: String = "",
    @JsonProperty("model")
    val model: String = "",
    @JsonProperty("acriss_code")
    val acrissCode: String = "",
    @JsonProperty("group_type")
    val groupType: String = "",
    @JsonProperty("transmission_type")
    val transmissionType: String = "",
    @JsonProperty("fuel_type")
    val fuelType: String = "",
    @JsonProperty("passengers_count")
    val passengersCount: Int = 0,
    @JsonProperty("bags_count")
    val bagsCount: Int = 0,
    @JsonProperty("is_new_car")
    val isNewCar: Boolean = false,
    @JsonProperty("is_recommended")
    val isRecommended: Boolean = false,
    @JsonProperty("is_more_luxury")
    val isMoreLuxury: Boolean = false,
    @JsonProperty("is_exciting_discount")
    val isExcitingDiscount: Boolean = false,
    @JsonProperty("vehicle_cost_value_eur")
    val vehicleCostValueEur: Double = 0.0,
    @JsonProperty("model_annex")
    val modelAnnex: String = "",
    @JsonProperty("images")
    val images: List<String> = emptyList(),
    @JsonProperty("tyre_type")
    val tyreType: String = "",
    @JsonProperty("attributes")
    val attributes: List<VehicleAttributePayload> = emptyList(),
    @JsonProperty("vehicle_status")
    val vehicleStatus: String = "",
    @JsonProperty("vehicle_cost")
    val vehicleCost: VehicleCostPayload? = null,
    @JsonProperty("upsell_reasons")
    val upsellReasons: List<UpsellReasonPayload> = emptyList()
) {
    companion object {
        const val ZERO_UUID = "00000000-0000-0000-0000-000000000000"
    }
}

data class VehicleAttributePayload(
    @JsonProperty("key")
    val key: String = "",
    @JsonProperty("title")
    val title: String = "",
    @JsonProperty("value")
    val value: String = "",
    @JsonProperty("attribute_type")
    val attributeType: String = "",
    @JsonProperty("icon_url")
    val iconUrl: String = ""
)

data class VehicleCostPayload(
    @JsonProperty("currency")
    val currency: String = "",
    @JsonProperty("value")
    val value: Double = 0.0
)

data class UpsellReasonPayload(
    @JsonProperty("title")
    val title: String = "",
    @JsonProperty("description")
    val description: String = ""
)

data class MlRecommendationResponse(
    @JsonProperty("highway_percent")
    val highwayPercent: Double = 0.0,
    @JsonProperty("max_slope")
    val maxSlope: Double = 0.0,
    @JsonProperty("total_ascent")
    val totalAscent: Double = 0.0,
    @JsonProperty("total_descent")
    val totalDescent: Double = 0.0,
    @JsonProperty("average_slope")
    val averageSlope: Double = 0.0,
    @JsonProperty("risk_score")
    val riskScore: Double = 0.0,
    @JsonProperty("vehicles")
    val vehicles: List<MlVehicleCandidate> = emptyList()
)

data class MlVehicleCandidate(
    @JsonProperty("id")
    val id: String,
    @JsonProperty("rank")
    val rank: Int
)

