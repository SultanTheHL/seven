package com.seven.seven.api

import com.seven.seven.api.dto.BookingVehiclesResponseDto
import com.seven.seven.service.SixtBookingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/booking")
class BookingController(
    private val sixtBookingService: SixtBookingService
) {

    @GetMapping("/{bookingId}/vehicles")
    fun bookingVehicles(@PathVariable bookingId: String): BookingVehiclesResponseDto {
        val vehicles = sixtBookingService.fetchVehicles(bookingId)
        return BookingVehiclesResponseDto(
            bookingId = bookingId,
            vehicles = vehicles
        )
    }
}

