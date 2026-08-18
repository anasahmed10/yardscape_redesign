package com.naslabs.yardscape.map

import com.naslabs.yardscape.domain.MapViewport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin

internal data class MapScreenOffset(val xPx: Double, val yPx: Double)

internal fun projectMapPoint(
    latitude: Double,
    longitude: Double,
    viewport: MapViewport,
    widthPx: Double,
    heightPx: Double,
): MapScreenOffset {
    val worldSize = 512.0 * 2.0.pow(viewport.zoomLevel)
    val deltaX = (mercatorX(longitude) - mercatorX(viewport.center.longitude)).let { delta ->
        when {
            delta > 0.5 -> delta - 1.0
            delta < -0.5 -> delta + 1.0
            else -> delta
        }
    }
    return MapScreenOffset(
        xPx = widthPx / 2.0 + deltaX * worldSize,
        yPx = heightPx / 2.0 +
            (mercatorY(latitude) - mercatorY(viewport.center.latitude)) * worldSize,
    )
}

private fun mercatorX(longitude: Double): Double = (longitude + 180.0) / 360.0

private fun mercatorY(latitude: Double): Double {
    val radians = latitude.coerceIn(-85.05112878, 85.05112878) * PI / 180.0
    return (1.0 - ln((1.0 + sin(radians)) / cos(radians)) / PI) / 2.0
}
