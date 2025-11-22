package com.seven.seven.shared.util

import com.seven.seven.shared.model.GeoPoint

object PolylineDecoder {

    fun decode(polyline: String): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        var index = 0
        val length = polyline.length
        var lat = 0
        var lng = 0

        while (index < length) {
            val resultLat = decodeChunk(polyline, index)
            lat += resultLat.value
            index = resultLat.nextIndex

            val resultLng = decodeChunk(polyline, index)
            lng += resultLng.value
            index = resultLng.nextIndex

            points += GeoPoint(lat / 1e5, lng / 1e5)
        }

        return points
    }

    private fun decodeChunk(encoded: String, startIndex: Int): ChunkResult {
        var index = startIndex
        var shift = 0
        var result = 0
        var byte: Int
        do {
            byte = encoded[index++].code - 63
            result = result or ((byte and 0x1f) shl shift)
            shift += 5
        } while (byte >= 0x20)

        val delta = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
        return ChunkResult(delta, index)
    }

    private data class ChunkResult(
        val value: Int,
        val nextIndex: Int
    )
}

