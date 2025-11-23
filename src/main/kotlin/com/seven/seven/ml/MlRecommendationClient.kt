package com.seven.seven.ml

import com.seven.seven.config.ExternalApiProperties
import com.seven.seven.external.ExternalApiException
import com.seven.seven.ml.model.MlRecommendationResponse
import com.seven.seven.ml.model.MlVehicleCandidate
import com.seven.seven.ml.model.PersonalInfoPayload
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class MlRecommendationClient(
    private val restClient: RestClient,
    private val properties: ExternalApiProperties
) {

    private val logger = LoggerFactory.getLogger(MlRecommendationClient::class.java)

    fun requestRecommendation(payload: PersonalInfoPayload, bookingId: String): MlRecommendationResponse {
        val uri = UriComponentsBuilder.fromUriString(properties.ml.recommendationUrl)
            .queryParam("booking_id", bookingId)
            .build()
            .toUri()
        
        logger.info("Calling ML recommendation service at {} with booking_id={}", uri, bookingId)

        val wirePayload = payload.toWirePayload()

        return runCatching {
            val response = restClient.post()
                .uri(uri)
                .body(wirePayload)
                .retrieve()
                .body(MlRecommendationResponse::class.java)
                ?: throw ExternalApiException("ML service returned empty response")
            
            // Log all response fields
            logger.info("=== ML Recommendation Response ===")
            logger.info("highway_percent: {}", response.highwayPercent)
            logger.info("max_slope: {}", response.maxSlope)
            logger.info("total_ascent: {}", response.totalAscent)
            logger.info("total_descent: {}", response.totalDescent)
            logger.info("average_slope: {}", response.averageSlope)
            logger.info("risk_score: {}", response.riskScore)
            logger.info("vehicles count: {}", response.vehicles.size)
            
            // Log each vehicle candidate
            response.vehicles.forEachIndexed { index, vehicle ->
                logger.info("Vehicle[{}]: id={}, rank={}", index, vehicle.id, vehicle.rank)
            }
            logger.info("=== End ML Recommendation Response ===")
            
            response
        }.getOrElse { throwable ->
            logger.error("ML recommendation service call failed: {}", throwable.message)
            throw ExternalApiException("Unable to call ML recommendation service", throwable)
        }
//        logger.info("Returning mocked ML recommendation response")
//        return MlRecommendationResponse(
//            highwayPercent = 0.6,
//            maxSlope = 5.2,
//            totalAscent = 150.0,
//            totalDescent = 120.0,
//            averageSlope = 2.1,
//            riskScore = 0.35,
//            vehicles = listOf(
//                MlVehicleCandidate(id = "vehicle-1", rank = 1),
//                MlVehicleCandidate(id = "vehicle-2", rank = 2),
//                MlVehicleCandidate(id = "vehicle-3", rank = 3)
//            )
//        )
    }
}

