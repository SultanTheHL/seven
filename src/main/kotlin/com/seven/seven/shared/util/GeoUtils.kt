package com.seven.seven.shared.util

import com.seven.seven.shared.model.GeoPoint
import kotlin.math.*

object GeoUtils {

    private const val EARTH_RADIUS_METERS = 6371000.0

    fun haversineDistanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val latDistance = Math.toRadians(b.lat - a.lat)
        val lonDistance = Math.toRadians(b.lng - a.lng)
        val originLatRad = Math.toRadians(a.lat)
        val destLatRad = Math.toRadians(b.lat)

        val calculation = sin(latDistance / 2).pow(2.0) +
            sin(lonDistance / 2).pow(2.0) * cos(originLatRad) * cos(destLatRad)

        return 2 * EARTH_RADIUS_METERS * asin(sqrt(calculation))
    }

    fun cumulativeDistances(points: List<GeoPoint>): List<Double> {
        if (points.isEmpty()) return emptyList()
        val result = mutableListOf(0.0)
        points.zipWithNext().forEach { (prev, current) ->
            val distance = haversineDistanceMeters(prev, current)
            result += result.last() + distance
        }
        return result
    }
}

