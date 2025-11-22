package com.seven.seven.api

import com.seven.seven.api.dto.RecommendationRequestDto
import com.seven.seven.api.dto.RecommendationResponseDto
import com.seven.seven.service.RecommendationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/recommendation")
class RecommendationController(
    private val recommendationService: RecommendationService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    fun recommend(@Valid @RequestBody request: RecommendationRequestDto): RecommendationResponseDto {
        val recommendation = recommendationService.generate(
            RecommendationService.RecommendationCommand(
                origin = request.originPoint(),
                destination = request.destinationPoint(),
                waypoints = request.waypointPoints(),
                travelInstant = request.travelDate,
                preferences = request.toPreferences(),
                rentalDays = request.rentalDays
            )
        )
        return RecommendationResponseDto.from(recommendation)
    }
}

