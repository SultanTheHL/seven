package com.seven.seven.service

import com.seven.seven.api.dto.VehicleDto
import com.seven.seven.external.GeminiClient
import com.seven.seven.ml.MlRecommendationClient
import com.seven.seven.ml.model.MlRecommendationResponse
import com.seven.seven.ml.model.PersonalInfoPayload
import com.seven.seven.ml.model.RoadCoordinatePayload
import com.seven.seven.ml.model.VehiclePayload
import com.seven.seven.shared.model.ElevationSample
import com.seven.seven.shared.model.GeoPoint
import com.seven.seven.shared.model.RoadSegment
import com.seven.seven.shared.model.RoadType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.math.ceil

@Service
class RecommendationService(
    private val routeInsightsService: RouteInsightsService,
    private val mlRecommendationClient: MlRecommendationClient,
    private val geminiClient: GeminiClient,
    private val sixtBookingService: SixtBookingService
) {
    private val logger = LoggerFactory.getLogger(RecommendationService::class.java)

    fun generate(command: RecommendationCommand): RecommendationResult {
        val context = routeInsightsService.collectRouteContext(
            RouteInsightsService.RouteInsightsRequest(
                origin = command.origin,
                destination = command.destination,
                waypoints = command.waypoints,
                travelInstant = command.travelInstant
            )
        )

        val payload: PersonalInfoPayload = buildPayload(command, context.elevationSamples, context)

        // Log all payload attributes for debugging
        logger.info("=== ML Payload ===")
        logger.info("peopleCount: ${payload.peopleCount}")
        logger.info("luggageBigCount: ${payload.luggageBigCount}")
        logger.info("luggageSmallCount: ${payload.luggageSmallCount}")
        logger.info("roadCoordinates: ${payload.roadCoordinates.size} points")
        payload.roadCoordinates.take(3).forEachIndexed { index, coord ->
            logger.info("  roadCoordinate[$index]: lat=${coord.latitude}, lng=${coord.longitude}, elevation=${coord.elevationMeters}m, speed=${coord.speedKph}km/h")
        }
        logger.info("tripLengthKm: ${payload.tripLengthKm}")
        logger.info("tripLengthHours: ${payload.tripLengthHours}")
        logger.info("conditionId: ${payload.conditionId}")
        logger.info("temperatureCelsius: ${payload.temperatureCelsius}")
        logger.info("windSpeedMetersPerSecond: ${payload.windSpeedMetersPerSecond}")
        logger.info("rainVolumeLastHour: ${payload.rainVolumeLastHour}")
        logger.info("snowVolumeLastHour: ${payload.snowVolumeLastHour}")
        logger.info("visibilityMeters: ${payload.visibilityMeters}")
        logger.info("currentVehicle: ${payload.currentVehicle}")
        logger.info("preference: ${payload.preference}")
        logger.info("automaticPreference: ${payload.automaticPreference}")
        logger.info("drivingSkills: ${payload.drivingSkills}")
        logger.info("parkingDifficulty: ${payload.parkingDifficulty}")
        logger.info("=== End ML Payload ===")

        val bookingId = command.bookingId ?: throw IllegalArgumentException("booking_id is required")

        val mlResponse = mlRecommendationClient.requestRecommendation(payload, bookingId)

        val bookingVehicles = loadBookingVehicles(bookingId)
        val standardVehicle = resolveStandardVehicle(command.currentVehicle, bookingVehicles)
        val superiorVehicles = resolveSuperiorVehicles(mlResponse, bookingVehicles, standardVehicle.id)
            .distinctBy { it.id }

        val promptContext = GeminiClient.PromptContext(
            standardVehicle = standardVehicle,
            superiorVehicles = superiorVehicles.take(TOP_SUPERIOR_VEHICLES),
            personalInfo = payload,
            mlResponse = mlResponse
        )

        val advantages = geminiClient.generateAdvantages(promptContext)
        return RecommendationResult(
            recommendations = advantages
        )
    }

    private fun loadBookingVehicles(bookingId: String): List<VehicleDto> {
        val vehicles = sixtBookingService.fetchVehicles(bookingId)
        logger.info("Loaded ${vehicles.size} vehicles for booking $bookingId")
        return vehicles
    }

    private fun resolveStandardVehicle(
        currentVehicle: VehiclePayload?,
        bookingVehicles: List<VehicleDto>
    ): VehiclePayload {
        if (currentVehicle != null) {
            logger.info("Using current vehicle from request as standard: ${currentVehicle.id}")
            return currentVehicle
        }

        val fallback = bookingVehicles.firstOrNull()?.toPayload()
        if (fallback != null) {
            logger.info("No current vehicle provided. Using first booking vehicle ${fallback.id} as standard.")
            return fallback
        }

        logger.warn("No vehicles available to determine standard vehicle. Using default placeholder.")
        return VehiclePayload()
    }

    private fun resolveSuperiorVehicles(
        mlResponse: MlRecommendationResponse,
        bookingVehicles: List<VehicleDto>,
        standardVehicleId: String
    ): List<VehiclePayload> {
        val vehicleMap = bookingVehicles.mapNotNull { dto ->
            dto.id?.let { id -> id to dto.toPayload() }
        }.toMap()

        val resolved = mlResponse.vehicles.mapNotNull { candidate ->
            val payload = vehicleMap[candidate.id] ?: VehiclePayload(id = candidate.id)
            payload.takeIf { it.id != standardVehicleId }
        }

        if (resolved.isEmpty()) {
            logger.warn("No superior vehicles matched booking inventory; using placeholders from ML response.")
            return mlResponse.vehicles.map { VehiclePayload(id = it.id) }
                .filter { it.id != standardVehicleId }
        }

        return resolved
    }

    private fun buildPayload(
        command: RecommendationCommand,
        elevationSamples: List<ElevationSample>,
        context: RouteInsightsService.RouteContext
    ): PersonalInfoPayload {
        val totalDistanceMeters = context.directions.totalDistanceMeters
        val totalDurationSeconds = context.directions.totalDurationSeconds
        val tripLengthKm = totalDistanceMeters / 1000.0
        val tripLengthHours = ceil(totalDurationSeconds / 3600.0).toInt().coerceAtLeast(1)

        val averageSpeedKph = when {
            totalDurationSeconds > 0 -> (totalDistanceMeters / totalDurationSeconds) * MPS_TO_KPH
            else -> estimateSpeedFromRoadTypes(context.roadBreakdown)
        }

        val roadCoordinates = buildRoadCoordinates(
            elevationSamples = elevationSamples,
            fallbackPoints = context.directions.points,
            roadSegments = context.roadSegments,
            defaultSpeedKph = averageSpeedKph
        )

        val weather = context.representativeWeather
        val metrics = weather.metrics

        return PersonalInfoPayload(
            peopleCount = command.peopleCount,
            luggageBigCount = command.luggageBigCount,
            luggageSmallCount = command.luggageSmallCount,
            roadCoordinates = roadCoordinates,
            tripLengthKm = tripLengthKm,
            tripLengthHours = tripLengthHours,
            conditionId = metrics.conditionId,
            temperatureCelsius = metrics.temperatureCelsius,
            windSpeedMetersPerSecond = metrics.windSpeedMetersPerSecond,
            rainVolumeLastHour = metrics.rainVolumeLastHour,
            snowVolumeLastHour = metrics.snowVolumeLastHour,
            visibilityMeters = metrics.visibilityMeters,
            currentVehicle = command.currentVehicle,
            preference = command.preference,
            automaticPreference = command.automaticPreference,
            drivingSkills = command.drivingSkills,
            parkingDifficulty = 0
        )
    }

    private fun buildRoadCoordinates(
        elevationSamples: List<ElevationSample>,
        fallbackPoints: List<GeoPoint>,
        roadSegments: List<RoadSegment>,
        defaultSpeedKph: Double
    ): List<RoadCoordinatePayload> {
        val sanitizedDefaultSpeed = defaultSpeedKph.takeIf { it.isFinite() && it >= 0 } ?: DEFAULT_SPEED_KPH
        val speedLookup = buildSpeedLookup(roadSegments, sanitizedDefaultSpeed)
        val samples = when {
            elevationSamples.isNotEmpty() -> elevationSamples
            else -> fallbackPoints.mapIndexed { index, point ->
                ElevationSample(
                    point = point,
                    elevationMeters = 0.0,
                    cumulativeDistanceMeters = index.toDouble()
                )
            }
        }

        return samples.take(MAX_ROAD_COORDINATES).map {
            RoadCoordinatePayload(
                longitude = it.point.lng,
                latitude = it.point.lat,
                elevationMeters = it.elevationMeters,
                speedKph = speedLookup[coordinateKey(it.point)] ?: sanitizedDefaultSpeed
            )
        }
    }

    private fun buildSpeedLookup(
        roadSegments: List<RoadSegment>,
        defaultSpeed: Double
    ): Map<String, Double> {
        return roadSegments.associate { segment ->
            val speed = if (segment.speedKph.isFinite() && segment.speedKph > 0) {
                segment.speedKph
            } else {
                ROAD_TYPE_SPEED_KPH[segment.roadType] ?: defaultSpeed
            }
            coordinateKey(segment.point) to speed
        }
    }

    private fun coordinateKey(point: GeoPoint): String =
        "%.5f:%.5f".format(point.lat, point.lng)

    private fun estimateSpeedFromRoadTypes(roadBreakdown: Map<RoadType, Double>): Double {
        if (roadBreakdown.isEmpty()) return DEFAULT_SPEED_KPH
        var total = 0.0
        roadBreakdown.forEach { (type, share) ->
            val segmentSpeed = ROAD_TYPE_SPEED_KPH[type] ?: DEFAULT_SPEED_KPH
            total += share * segmentSpeed
        }
        return total.takeIf { it > 0 } ?: DEFAULT_SPEED_KPH
    }

    data class RecommendationCommand(
        val origin: GeoPoint,
        val destination: GeoPoint,
        val waypoints: List<GeoPoint>,
        val travelInstant: Instant,
        val peopleCount: Int,
        val luggageBigCount: Int,
        val luggageSmallCount: Int,
        val preference: String,
        val automaticPreference: Int,
        val drivingSkills: String,
        val currentVehicle: VehiclePayload?,
        val rentalDays: Int,
        val bookingId: String?
    )

    data class RecommendationResult(
        val recommendations: List<GeminiClient.VehicleAdvantage>
    )

    companion object {
        private const val MAX_ROAD_COORDINATES = 50
        private const val DEFAULT_SPEED_KPH = 50.0
        private const val MPS_TO_KPH = 3.6
        private const val TOP_SUPERIOR_VEHICLES = 3
        private val ROAD_TYPE_SPEED_KPH = mapOf(
            RoadType.MOTORWAY to 120.0,
            RoadType.TRUNK to 100.0,
            RoadType.PRIMARY to 90.0,
            RoadType.SECONDARY to 80.0,
            RoadType.TERTIARY to 70.0,
            RoadType.RESIDENTIAL to 50.0,
            RoadType.SERVICE to 30.0,
            RoadType.RAMP to 60.0,
            RoadType.UNKNOWN to DEFAULT_SPEED_KPH
        )
    }
}
