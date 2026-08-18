package com.naslabs.yardscape.map

import com.naslabs.yardscape.domain.MapViewport
import com.naslabs.yardscape.domain.ViewportCenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapMarkerProjectionTest {
    @Test
    fun publicCenterProjectsToTheCenterOfTheMapSurface() {
        val offset = projectMapPoint(
            latitude = 47.61,
            longitude = -122.20,
            viewport = MapViewport(ViewportCenter(47.61, -122.20), 12.0),
            widthPx = 390.0,
            heightPx = 480.0,
        )

        assertEquals(195.0, offset.xPx, absoluteTolerance = 0.001)
        assertEquals(240.0, offset.yPx, absoluteTolerance = 0.001)
    }

    @Test
    fun northeastPublicCenterProjectsNortheastOnScreen() {
        val offset = projectMapPoint(
            latitude = 47.62,
            longitude = -122.19,
            viewport = MapViewport(ViewportCenter(47.61, -122.20), 12.0),
            widthPx = 390.0,
            heightPx = 480.0,
        )

        assertTrue(offset.xPx > 195.0)
        assertTrue(offset.yPx < 240.0)
    }

    @Test
    fun projectionUsesMapLibreNative512PixelWorldSize() {
        val offset = projectMapPoint(
            latitude = 0.0,
            longitude = 90.0,
            viewport = MapViewport(ViewportCenter(0.0, 0.0), 1.0),
            widthPx = 800.0,
            heightPx = 600.0,
        )

        assertEquals(656.0, offset.xPx, absoluteTolerance = 0.001)
    }
}
