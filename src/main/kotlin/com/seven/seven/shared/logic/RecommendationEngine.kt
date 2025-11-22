package com.seven.seven.shared.logic

import com.seven.seven.shared.model.ConfidenceLevel
import com.seven.seven.shared.model.PreferenceFocus
import com.seven.seven.shared.model.ProtectionRecommendation
import com.seven.seven.shared.model.RecommendationInput
import com.seven.seven.shared.model.RoadType
import com.seven.seven.shared.model.UpgradeOption
import com.seven.seven.shared.model.VehicleRecommendation
import com.seven.seven.shared.model.RecommendedVehicle
import org.springframework.stereotype.Component
import kotlin.math.roundToInt

@Component
class RecommendationEngine {

    fun recommend(input: RecommendationInput): VehicleRecommendation {
        val preferences = input.preferences
        val analysis = input.analysis

        val slope = analysis.slopeMetrics.maxSlopePercent
        val roadDifficulty = analysis.roadDifficultyScore
        val weatherSeverity = analysis.weatherSeverityScore
        val tightRoadsShare = input.routeProfile.roadBreakdown
            .filterKeys { it == RoadType.RESIDENTIAL || it == RoadType.SERVICE }
            .values
            .sum()

        val reasons = mutableListOf<String>()
        var recommendedGroup: String
        var modelExample: String

        val travelingHeavy = preferences.numTravellers >= 4 || preferences.numBags >= 3
        val steepRoute = slope >= 8
        val harshWeather = weatherSeverity >= 0.5
        val narrowRoads = tightRoadsShare >= 0.3 && !travelingHeavy

        if (steepRoute || travelingHeavy || harshWeather) {
            recommendedGroup = if (preferences.focus == PreferenceFocus.COMFORT) {
                "SUV Premium"
            } else {
                "SUV Standard"
            }
            modelExample = if (preferences.focus == PreferenceFocus.COMFORT) "BMW X3" else "Toyota RAV4"
            reasons += buildSteepReason(steepRoute, weatherSeverity)
            if (travelingHeavy) {
                reasons += "You travel with ${preferences.numTravellers} people & ${preferences.numBags} bags → mid-sized SUV provides room."
            }
            if (preferences.focus == PreferenceFocus.COMFORT) {
                reasons += "You prioritise comfort → premium SUV trim adds quieter cabin and seats."
            }
        } else if (narrowRoads) {
            recommendedGroup = "Compact Sedan"
            modelExample = "Audi A3"
            reasons += "Route has ${(tightRoadsShare * 100).roundToInt()}% narrow or residential roads → compact car handles tight turns."
            if (preferences.focus == PreferenceFocus.PRICE) {
                reasons += "Focus on price → compact sedans keep rental cost low."
            }
        } else {
            recommendedGroup = "Crossover Comfort"
            modelExample = "Volvo XC40"
            reasons += "Balanced route difficulty → crossover gives higher seating without big footprint."
        }

        if (harshWeather) {
            reasons += "Forecast indicates challenging weather → AWD & safety aids recommended."
            if (preferences.confidenceInBadWeather == ConfidenceLevel.LOW) {
                reasons += "You reported low confidence in bad weather → driver assistance prioritized."
            }
        }

        if (preferences.travelingWithKids) {
            reasons += "Travelling with kids → ISOFIX mounts & driver assists add safety."
        }

        val upgradeOptions = buildUpgradeOptions(recommendedGroup)
        val protectionRecommendation = buildProtectionRecommendation(roadDifficulty, weatherSeverity, tightRoadsShare)

        val recommendation = RecommendedVehicle(
            group = recommendedGroup,
            modelExample = modelExample,
            reasons = reasons.distinct(),
            upgradeOptions = upgradeOptions
        )

        return VehicleRecommendation(
            recommendedVehicle = recommendation,
            protectionRecommendation = protectionRecommendation
        )
    }

    private fun buildSteepReason(steepRoute: Boolean, weatherSeverity: Double): String =
        when {
            steepRoute && weatherSeverity >= 0.5 ->
                "Steep climbs plus harsh weather expected → AWD SUV for grip & torque."
            steepRoute ->
                "Route features notable elevation changes → SUV handles slopes better."
            else ->
                "Harsh weather expected along the route → SUV traction & stability helpful."
        }

    private fun buildUpgradeOptions(group: String): List<UpgradeOption> = when (group) {
        "SUV Premium" -> listOf(
            UpgradeOption("SUV Luxury", "BMW X5", "+€40/day"),
            UpgradeOption("Sedan Compact", "Audi A3", "-€10/day")
        )
        "SUV Standard" -> listOf(
            UpgradeOption("SUV Premium", "BMW X3", "+€25/day"),
            UpgradeOption("Crossover Comfort", "Volvo XC40", "+€10/day")
        )
        "Compact Sedan" -> listOf(
            UpgradeOption("Crossover Comfort", "Volvo XC40", "+€8/day"),
            UpgradeOption("EV City", "VW ID.3", "+€5/day")
        )
        else -> listOf(
            UpgradeOption("SUV Premium", "BMW X3", "+€20/day"),
            UpgradeOption("Compact Sedan", "Audi A3", "same-day upgrade")
        )
    }

    private fun buildProtectionRecommendation(
        roadDifficulty: Double,
        weatherSeverity: Double,
        tightRoadsShare: Double
    ): ProtectionRecommendation? {
        val needsExtraCover = roadDifficulty >= 0.5 || weatherSeverity >= 0.5 || tightRoadsShare >= 0.3
        if (!needsExtraCover) return null

        val reasonParts = mutableListOf<String>()
        if (tightRoadsShare >= 0.3) reasonParts += "narrow residential streets increase parking scuff risk"
        if (roadDifficulty >= 0.5) reasonParts += "mixed road surfaces add chance of wheel/tire damage"
        if (weatherSeverity >= 0.5) reasonParts += "bad weather raises accident probability"

        return ProtectionRecommendation(
            packageName = "Smart Protection Plus",
            reason = reasonParts.joinToString(" & ")
        )
    }
}

