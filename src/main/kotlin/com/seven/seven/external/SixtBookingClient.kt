package com.seven.seven.external

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.seven.seven.api.dto.UpsellReasonDto
import com.seven.seven.api.dto.VehicleAttributeDto
import com.seven.seven.api.dto.VehicleCostDto
import com.seven.seven.api.dto.VehicleDto
import com.seven.seven.config.ExternalApiProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder

@Component
class SixtBookingClient(
    private val restClient: RestClient,
    private val properties: ExternalApiProperties,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(SixtBookingClient::class.java)

    fun fetchVehicles(bookingId: String): List<VehicleDto> {
        val uri = UriComponentsBuilder
            .fromUriString("${properties.sixt.bookingBaseUrl}/booking/{bookingId}/vehicles")
            .buildAndExpand(bookingId)
            .toUri()

        logger.info("Fetching vehicles for booking {}", bookingId)
        val body = restClient.get()
            .uri(uri)
            .retrieve()
            .body(String::class.java)
            ?: return emptyList()

        val root = runCatching { objectMapper.readTree(body) }.getOrElse {
            logger.error("Failed to parse vehicles response: {}", it.message)
            return emptyList()
        }

        val deals = root.path("deals")
        if (!deals.isArray || deals.isEmpty) {
            logger.warn("No deals returned for booking {}", bookingId)
            return emptyList()
        }

        return deals.mapNotNull { dealNode ->
            val vehicleNode = dealNode.path("vehicle")
            if (vehicleNode.isMissingNode || vehicleNode.isNull) {
                logger.warn("Deal entry missing 'vehicle' node, skipping")
                null
            } else {
                parseVehicle(vehicleNode)
            }
        }
    }

    private fun parseVehicle(node: JsonNode): VehicleDto {
        val attributes = node.path("attributes").takeIf { it.isArray }?.map {
            VehicleAttributeDto(
                key = it.path("key").asText(""),
                title = it.path("title").asText(""),
                value = it.path("value").asText(""),
                attributeType = it.path("attributeType").asText(""),
                iconUrl = it.path("iconUrl").asText(null)
            )
        } ?: emptyList()

        val upsellReasons = node.path("upsellReasons").takeIf { it.isArray }?.map {
            UpsellReasonDto(
                title = it.path("title").asText(""),
                description = it.path("description").asText("")
            )
        } ?: emptyList()

        val vehicleCost = node.path("vehicleCost").takeIf { it.isObject }?.let {
            VehicleCostDto(
                currency = it.path("currency").asText(""),
                value = it.path("value").asDouble(0.0)
            )
        }

        val images = node.path("images").takeIf { it.isArray }?.map { it.asText("") }?.filter { it.isNotBlank() }
            ?: emptyList()

        return VehicleDto(
            id = node.path("id").asText(null),
            brand = node.path("brand").asText(null),
            model = node.path("model").asText(null),
            acrissCode = node.path("acrissCode").asText(null),
            groupType = node.path("groupType").asText(null),
            transmissionType = node.path("transmissionType").asText(null),
            fuelType = node.path("fuelType").asText(null),
            passengersCount = node.path("passengersCount").takeIf { it.isNumber }?.asInt(),
            bagsCount = node.path("bagsCount").takeIf { it.isNumber }?.asInt(),
            isNewCar = node.path("isNewCar").asBoolean(false),
            isRecommended = node.path("isRecommended").asBoolean(false),
            isMoreLuxury = node.path("isMoreLuxury").asBoolean(false),
            isExcitingDiscount = node.path("isExcitingDiscount").asBoolean(false),
            vehicleCostValueEur = vehicleCost?.value,
            modelAnnex = node.path("modelAnnex").asText(null),
            images = images,
            tyreType = node.path("tyreType").asText(null),
            attributes = attributes,
            vehicleStatus = node.path("vehicleStatus").asText(null),
            vehicleCost = vehicleCost,
            upsellReasons = upsellReasons
        )
    }
}

