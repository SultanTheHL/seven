package com.seven.seven.ml

import com.seven.seven.config.ExternalApiProperties
import com.seven.seven.external.ExternalApiException
import com.seven.seven.ml.model.MlRecommendationResponse
import com.seven.seven.ml.model.PersonalInfoPayload
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI

@Component
class MlRecommendationClient(
    private val restClient: RestClient,
    private val properties: ExternalApiProperties
) {

    private val logger = LoggerFactory.getLogger(MlRecommendationClient::class.java)

    fun requestRecommendation(payload: PersonalInfoPayload): MlRecommendationResponse {
        val uri = URI.create(properties.ml.recommendationUrl)
        logger.info("Calling ML recommendation service at {}", uri)

        return runCatching {
            restClient.post()
                .uri(uri)
                .body(payload)
                .retrieve()
                .body(MlRecommendationResponse::class.java)
                ?: throw ExternalApiException("ML service returned empty response")
        }.getOrElse { throwable ->
            logger.error("ML recommendation service call failed: {}", throwable.message)
            throw ExternalApiException("Unable to call ML recommendation service", throwable)
        }
    }
}

