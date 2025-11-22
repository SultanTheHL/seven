package com.seven.seven.service

import com.seven.seven.shared.logic.RecommendationEngine
import com.seven.seven.shared.logic.RouteAnalysisService
import com.seven.seven.shared.model.GeoPoint
import com.seven.seven.shared.model.RecommendationInput
import com.seven.seven.shared.model.UserPreferences
import com.seven.seven.shared.model.VehicleRecommendation
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RecommendationService(
    private val routeInsightsService: RouteInsightsService,
    private val routeAnalysisService: RouteAnalysisService,
    private val recommendationEngine: RecommendationEngine
) {

    fun generate(command: RecommendationCommand): VehicleRecommendation {
        val profile = routeInsightsService.buildProfile(
            RouteInsightsService.RouteInsightsRequest(
                origin = command.origin,
                destination = command.destination,
                waypoints = command.waypoints,
                travelInstant = command.travelInstant
            )
        )

        val analysis = routeAnalysisService.analyze(profile)

        val input = RecommendationInput(
            routeProfile = profile,
            analysis = analysis,
            preferences = command.preferences,
            travelDate = command.travelInstant,
            rentalDays = command.rentalDays
        )

        return recommendationEngine.recommend(input)
    }

    data class RecommendationCommand(
        val origin: GeoPoint,
        val destination: GeoPoint,
        val waypoints: List<GeoPoint>,
        val travelInstant: Instant,
        val preferences: UserPreferences,
        val rentalDays: Int
    )
}

