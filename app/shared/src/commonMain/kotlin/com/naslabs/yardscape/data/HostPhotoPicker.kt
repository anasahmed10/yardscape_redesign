package com.naslabs.yardscape.data

import com.naslabs.yardscape.domain.EventPhoto

fun interface HostPhotoPicker {
    fun availablePhotos(): List<EventPhoto>
}

class SeededHostPhotoPicker : HostPhotoPicker {
    override fun availablePhotos(): List<EventPhoto> = listOf(
        EventPhoto("mock://host-photo/driveway", "Driveway overview"),
        EventPhoto("mock://host-photo/furniture", "Furniture and home goods"),
        EventPhoto("mock://host-photo/tools", "Tools table"),
        EventPhoto("mock://host-photo/kids", "Kids items"),
    )
}
