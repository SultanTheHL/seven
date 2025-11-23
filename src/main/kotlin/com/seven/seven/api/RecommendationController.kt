package com.seven.seven.api

import com.seven.seven.api.dto.RecommendationRequestDto
import com.seven.seven.api.dto.RecommendationResponseDto
import com.seven.seven.service.RecommendationService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/recommendation")
class RecommendationController(
    private val recommendationService: RecommendationService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    fun recommend(
        @Valid @RequestBody request: RecommendationRequestDto,
        @RequestParam(name = "booking_id", required = false) bookingIdParam: String?
    ): RecommendationResponseDto {
        val preferences = request.preferences
        val bookingId = bookingIdParam ?: request.bookingId
        val recommendation = recommendationService.generate(
            RecommendationService.RecommendationCommand(
                origin = request.originLocation(),
                destination = request.destinationLocation(),
                waypoints = request.waypointLocations(),
                travelInstant = request.travelDate,
                peopleCount = preferences.peopleCount,
                luggageBigCount = preferences.luggageBigCount,
                luggageSmallCount = preferences.luggageSmallCount,
                preference = preferences.preference,
                automaticPreference = preferences.automatic,
                drivingSkills = preferences.drivingSkills,
                currentVehicle = preferences.toVehiclePayload(),
                rentalDays = request.rentalDays,
                bookingId = bookingId
            )
        )
        return RecommendationResponseDto.from(recommendation)
    }
}

