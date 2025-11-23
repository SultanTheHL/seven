package com.seven.seven.shared.model

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class LocationInput(
    val geoPoint: GeoPoint? = null,
    val query: String? = null
) {
    init {
        require(geoPoint != null || !query.isNullOrBlank()) {
            "LocationInput requires either geoPoint or query"
        }
    }

    fun asQueryParam(): String = when {
        !query.isNullOrBlank() -> URLEncoder.encode(query, StandardCharsets.UTF_8)
        geoPoint != null -> "${geoPoint.lat},${geoPoint.lng}"
        else -> error("Invalid LocationInput state")
    }

    companion object {
        fun from(lat: Double?, lng: Double?, place: String?): LocationInput {
            val trimmedPlace = place?.takeIf { it.isNotBlank() }
            val geoPoint = if (lat != null && lng != null) GeoPoint(lat, lng) else null
            return LocationInput(geoPoint = geoPoint, query = trimmedPlace)
        }
    }
}

