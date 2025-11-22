package com.seven.seven.ml.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PersonalInfoPayload(
    @JsonProperty("people_count")
    val peopleCount: Int,

    @JsonProperty("laguage_big_count")
    val luggageBigCount: Int,

    @JsonProperty("laguage_small_count")
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
)

data class VehiclePayload(
    @JsonProperty("id")
    val id: String? = null,
    @JsonProperty("brand")
    val brand: String? = null,
    @JsonProperty("model")
    val model: String? = null,
    @JsonProperty("acriss_code")
    val acrissCode: String? = null,
    @JsonProperty("group_type")
    val groupType: String? = null,
    @JsonProperty("transmission_type")
    val transmissionType: String? = null,
    @JsonProperty("fuel_type")
    val fuelType: String? = null,
    @JsonProperty("passengers_count")
    val passengersCount: Int? = null,
    @JsonProperty("bags_count")
    val bagsCount: Int? = null,
    @JsonProperty("is_new_car")
    val isNewCar: Boolean? = null,
    @JsonProperty("is_recommended")
    val isRecommended: Boolean? = null,
    @JsonProperty("is_more_luxury")
    val isMoreLuxury: Boolean? = null,
    @JsonProperty("is_exciting_discount")
    val isExcitingDiscount: Boolean? = null,
    @JsonProperty("vehicle_cost_value_eur")
    val vehicleCostValueEur: Double? = null
)

data class MlRecommendationResponse(
    @JsonProperty("vehicle_id")
    val vehicleId: String,

    @JsonProperty("feedback")
    val feedback: String
)

