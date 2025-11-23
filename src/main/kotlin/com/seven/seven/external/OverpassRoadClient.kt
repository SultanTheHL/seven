package com.seven.seven.external

import com.fasterxml.jackson.databind.ObjectMapper
import com.seven.seven.config.ExternalApiProperties
import com.seven.seven.shared.model.GeoPoint
import com.seven.seven.shared.model.RoadSegment
import com.seven.seven.shared.model.RoadType
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

@Component
class OverpassRoadClient(
    private val restClient: RestClient,
    private val properties: ExternalApiProperties,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(OverpassRoadClient::class.java)

    private val wayTagCache = ConcurrentHashMap<String, WayTags?>()

    fun classifyRoads(points: List<GeoPoint>): RoadSegmentsResult {
        val startTime = System.currentTimeMillis()
        logger.info("=== Overpass Road Classification Started ===")
        logger.info("Input points: ${points.size}")
        
        if (points.isEmpty()) {
            logger.info("No points provided, returning empty result")
            return RoadSegmentsResult(emptyMap(), emptyList())
        }
        
        val downSampleStart = System.currentTimeMillis()
        val sampledPoints = downSample(points)
        val downSampleDuration = System.currentTimeMillis() - downSampleStart
        logger.info("Downsampling: ${points.size} -> ${sampledPoints.size} points (took ${downSampleDuration}ms)")
        logger.info("Downsampling strategy: stride=$SAMPLE_STRIDE, max=$MAX_SAMPLE_POINTS")

        val totals = mutableMapOf<RoadType, Int>()
        val segments = mutableListOf<RoadSegment>()
        var apiCallsMade = 0
        var cacheHits = 0
        var apiCallFailures = 0
        var totalApiCallTime = 0L

        sampledPoints.forEachIndexed { index, point ->
            val cacheKey = bucketKey(point)
            val cacheStart = System.currentTimeMillis()
            val cached = wayTagCache[cacheKey]
            val cacheCheckTime = System.currentTimeMillis() - cacheStart
            
            val wayTags = if (cached != null) {
                cacheHits++
                logger.debug("Point $index (${point.lat}, ${point.lng}): Cache HIT (bucket: $cacheKey, check: ${cacheCheckTime}ms)")
                cached
            } else {
                apiCallsMade++
                logger.info("Point $index (${point.lat}, ${point.lng}): Cache MISS (bucket: $cacheKey), making API call #$apiCallsMade")
                val apiCallStart = System.currentTimeMillis()
                val result = queryWayTags(point)
                val apiCallDuration = System.currentTimeMillis() - apiCallStart
                totalApiCallTime += apiCallDuration
                
                if (result != null) {
                    logger.info("Point $index: API call SUCCESS (took ${apiCallDuration}ms) - highway: ${result.highway ?: "none"}, speed: ${result.speedKph ?: "none"} km/h")
                    wayTagCache[cacheKey] = result
                    result
                } else {
                    apiCallFailures++
                    logger.warn("Point $index: API call FAILED (took ${apiCallDuration}ms) - using fallback")
                    null
                }
            }
            
            val (roadType, speedKph) = when (wayTags) {
                null -> RoadType.UNKNOWN to DEFAULT_SPEED_KPH
                else -> {
                    val type = wayTags.highway?.let { highwayTagToRoadType(it) } ?: inferRoadType(wayTags.speedKph)
                    val speedValue = wayTags.speedKph ?: fallbackSpeed(type)
                    type to speedValue
                }
            }

            totals[roadType] = totals.getOrDefault(roadType, 0) + 1
            segments += RoadSegment(point, roadType, speedKph)
        }

        val sum = totals.values.sum()
        val breakdown = if (sum > 0) {
            totals.mapValues { it.value.toDouble() / sum }
        } else {
            defaultBreakdown()
        }

        val totalDuration = System.currentTimeMillis() - startTime
        val avgApiCallTime = if (apiCallsMade > 0) totalApiCallTime / apiCallsMade else 0L
        
        logger.info("=== Overpass Road Classification Summary ===")
        logger.info("Total duration: ${totalDuration}ms")
        logger.info("API calls made: $apiCallsMade")
        logger.info("Cache hits: $cacheHits")
        logger.info("API call failures: $apiCallFailures")
        logger.info("Total API call time: ${totalApiCallTime}ms")
        logger.info("Average API call time: ${avgApiCallTime}ms")
        logger.info("Cache hit rate: ${if (sampledPoints.isNotEmpty()) (cacheHits * 100.0 / sampledPoints.size).let { "%.1f".format(it) } else "0.0"}%")
        logger.info("Road type breakdown: ${breakdown.map { "${it.key}=${(it.value * 100).let { "%.1f".format(it) }}%" }.joinToString(", ")}")
        logger.info("Cache size: ${wayTagCache.size} entries")
        logger.info("=== End Overpass Classification ===")

        return RoadSegmentsResult(
            breakdown = breakdown,
            segments = segments
        )
    }

    private fun queryWayTags(point: GeoPoint): WayTags? {
        val query = """
            [out:json][timeout:25];
            way(around:$SEARCH_RADIUS_METERS,${point.lat},${point.lng})["highway"];
            out tags;
        """.trimIndent()

        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8)
        logger.debug("Overpass query for (${point.lat}, ${point.lng}): $query")
        logger.debug("Query length: ${encodedQuery.length} chars")

        var attempt = 0
        var delayMs = INITIAL_RETRY_DELAY_MS
        while (attempt < MAX_RETRIES) {
            val attemptStart = System.currentTimeMillis()
            try {
                logger.debug("Overpass API call attempt ${attempt + 1}/$MAX_RETRIES for (${point.lat}, ${point.lng})")
                val httpStart = System.currentTimeMillis()
                val responseBody = restClient.post()
                    .uri(properties.overpass.url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("data=$encodedQuery")
                    .retrieve()
                    .body(String::class.java)
                val httpDuration = System.currentTimeMillis() - httpStart
                logger.debug("HTTP request completed in ${httpDuration}ms, response length: ${responseBody?.length ?: 0} chars")
                
                if (responseBody == null) {
                    logger.warn("Overpass API returned null response for (${point.lat}, ${point.lng})")
                    return null
                }

                val parseStart = System.currentTimeMillis()
                val root = runCatching { objectMapper.readTree(responseBody) }.getOrNull()
                val parseDuration = System.currentTimeMillis() - parseStart
                logger.debug("JSON parsing took ${parseDuration}ms")
                
                if (root == null) {
                    logger.warn("Failed to parse Overpass JSON response for (${point.lat}, ${point.lng})")
                    return null
                }
                
                val elements = root.path("elements")
                logger.debug("Found ${elements.size()} elements in response")
                
                if (!elements.isArray || elements.isEmpty) {
                    logger.debug("No elements found in Overpass response for (${point.lat}, ${point.lng})")
                    return null
                }

                val preferred = elements
                    .firstOrNull { it.path("type").asText("") == "way" }
                    ?: run {
                        logger.debug("No 'way' element found in Overpass response for (${point.lat}, ${point.lng})")
                        return null
                    }

                val tags = preferred.path("tags")
                val highway = tags.path("highway").asText(null)
                val maxSpeedRaw = tags.path("maxspeed").asText(null)
                val parsedSpeed = maxSpeedRaw?.let { parseSpeedKph(it) }
                
                val totalAttemptDuration = System.currentTimeMillis() - attemptStart
                logger.debug("Successfully extracted tags: highway=$highway, maxspeed=$maxSpeedRaw -> ${parsedSpeed}km/h (total attempt: ${totalAttemptDuration}ms)")

                return WayTags(
                    highway = highway,
                    speedKph = parsedSpeed
                )
            } catch (ex: Exception) {
                val attemptDuration = System.currentTimeMillis() - attemptStart
                val isRetryable = shouldRetry(ex)
                val isLastAttempt = attempt == MAX_RETRIES - 1
                
                logger.warn("Overpass API call failed for (${point.lat}, ${point.lng}) on attempt ${attempt + 1}/$MAX_RETRIES (took ${attemptDuration}ms)")
                logger.warn("Exception type: ${ex.javaClass.simpleName}, message: ${ex.message}")
                
                if (ex is RestClientResponseException) {
                    logger.warn("HTTP status: ${ex.statusCode.value()}, response body: ${ex.responseBodyAsString?.take(200)}")
                }
                
                if (!isRetryable || isLastAttempt) {
                    logger.error("Giving up on Overpass API call for (${point.lat}, ${point.lng}) after ${attempt + 1} attempts")
                    return null
                }
                
                logger.info("Retrying Overpass API call for (${point.lat}, ${point.lng}) - attempt ${attempt + 2}/$MAX_RETRIES after ${delayMs}ms delay")
                Thread.sleep(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
                attempt++
            }
        }

        logger.error("Exhausted all retry attempts for Overpass API call at (${point.lat}, ${point.lng})")
        return null
    }

    private fun parseSpeedKph(value: String): Double? {
        val normalized = value.trim().lowercase()
        val numeric = normalized.replace("[^0-9.]".toRegex(), "")
        val number = numeric.toDoubleOrNull() ?: return null
        return if (normalized.contains("mph")) {
            number * MPH_TO_KPH
        } else {
            number
        }
    }

    private fun fallbackSpeed(roadType: RoadType): Double =
        ROAD_TYPE_SPEED_KPH[roadType] ?: DEFAULT_SPEED_KPH

    private fun inferRoadType(speed: Double?): RoadType {
        val value = speed ?: DEFAULT_SPEED_KPH
        return when {
            value >= 120 -> RoadType.MOTORWAY
            value >= 100 -> RoadType.TRUNK
            value >= 90 -> RoadType.PRIMARY
            value >= 80 -> RoadType.SECONDARY
            value >= 70 -> RoadType.TERTIARY
            value >= 50 -> RoadType.RESIDENTIAL
            value >= 30 -> RoadType.SERVICE
            else -> RoadType.UNKNOWN
        }
    }

    private fun downSample(points: List<GeoPoint>): List<GeoPoint> {
        if (points.isEmpty()) return emptyList()
        val strideSampled = points.filterIndexed { index, _ -> index % SAMPLE_STRIDE == 0 }.toMutableList()
        if (strideSampled.isEmpty()) strideSampled += points.first()
        if (strideSampled.last() != points.last()) {
            strideSampled += points.last()
        }
        if (strideSampled.size <= MAX_SAMPLE_POINTS) return strideSampled

        val step = strideSampled.size.toDouble() / MAX_SAMPLE_POINTS
        val capped = mutableListOf<GeoPoint>()
        var cursor = 0.0
        while (cursor < strideSampled.size) {
            capped += strideSampled[cursor.toInt()]
            cursor += step
        }
        if (capped.last() != strideSampled.last()) capped += strideSampled.last()
        return capped
    }

    private fun defaultBreakdown(): Map<RoadType, Double> = mapOf(RoadType.UNKNOWN to 1.0)

    private fun shouldRetry(ex: Exception): Boolean {
        return when (ex) {
            is RestClientResponseException -> ex.statusCode.value() in RETRY_STATUS_CODES
            else -> true
        }
    }

    private fun bucketKey(point: GeoPoint): String {
        val latBucket = roundToBucket(point.lat)
        val lngBucket = roundToBucket(point.lng)
        return "$latBucket:$lngBucket"
    }

    private fun roundToBucket(value: Double): Double {
        val scaled = value * CACHE_BUCKET_SCALE
        return (scaled.roundToInt() / CACHE_BUCKET_SCALE)
    }

    data class RoadSegmentsResult(
        val breakdown: Map<RoadType, Double>,
        val segments: List<RoadSegment>
    )

    private data class WayTags(
        val highway: String?,
        val speedKph: Double?
    )

    companion object {
        private const val SAMPLE_STRIDE = 5
        private const val MAX_SAMPLE_POINTS = 25
        private const val SEARCH_RADIUS_METERS = 10
        private const val MPH_TO_KPH = 1.60934
        private const val DEFAULT_SPEED_KPH = 50.0
        private const val CACHE_BUCKET_SCALE = 1000.0 // ~0.001 degree buckets (~100m)
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 500L
        private const val MAX_RETRY_DELAY_MS = 4000L
        private val RETRY_STATUS_CODES = setOf(429, 500, 502, 503, 504)

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

        private fun highwayTagToRoadType(value: String): RoadType = when (value.lowercase()) {
            "motorway" -> RoadType.MOTORWAY
            "trunk" -> RoadType.TRUNK
            "primary" -> RoadType.PRIMARY
            "secondary" -> RoadType.SECONDARY
            "tertiary" -> RoadType.TERTIARY
            "residential" -> RoadType.RESIDENTIAL
            "service" -> RoadType.SERVICE
            "ramp" -> RoadType.RAMP
            else -> RoadType.UNKNOWN
        }
    }
}

