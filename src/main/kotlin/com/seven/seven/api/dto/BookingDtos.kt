package com.seven.seven.api.dto

data class BookingVehiclesResponseDto(
    val bookingId: String,
    val vehicles: List<VehicleDto>
)

