@file:JsModule("maplibre-gl")
@file:JsNonModule

package com.naslabs.yardscape.map

import org.maplibre.kmp.js.map.Map
import org.w3c.dom.HTMLElement

internal external interface MarkerOptions {
    var element: HTMLElement
}

internal external class Marker(options: MarkerOptions) {
    fun setLngLat(coordinates: Array<Double>): Marker
    fun addTo(map: Map): Marker
    fun remove()
}
