package com.seven.seven.service

import com.seven.seven.api.dto.VehicleDto
import com.seven.seven.external.SixtBookingClient
import org.springframework.stereotype.Service

@Service
class SixtBookingService(
    private val sixtBookingClient: SixtBookingClient
) {
    fun fetchVehicles(bookingId: String): List<VehicleDto> =
        sixtBookingClient.fetchVehicles(bookingId)
}

