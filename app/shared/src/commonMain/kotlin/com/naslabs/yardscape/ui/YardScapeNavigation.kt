package com.naslabs.yardscape.ui

import com.naslabs.yardscape.data.PublicEventDetail
import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.data.SeededYardSaleEventRepository
import com.naslabs.yardscape.data.YardSaleEventRepository
import com.naslabs.yardscape.domain.PublicEventPreview

sealed interface YardScapeRoute {
    data object Browse : YardScapeRoute
    data class EventDetail(val eventId: String) : YardScapeRoute
    data class Rsvp(val eventId: String) : YardScapeRoute
    data class HostCreateEdit(val eventId: String? = null) : YardScapeRoute
}

class YardScapeAppState(
    private val repository: YardSaleEventRepository = SeededYardSaleEventRepository(),
    private val nowEpochMillis: Long = SeededYardSaleData.BASE_NOW_EPOCH_MILLIS,
) {
    var route: YardScapeRoute = YardScapeRoute.Browse
        private set

    fun browseItems(): List<BrowseEventItem> =
        repository.publicPreviews(nowEpochMillis).map { it.toBrowseEventItem(nowEpochMillis) }

    fun selectedPublicDetail(): PublicEventDetail? {
        val detailRoute = route as? YardScapeRoute.EventDetail ?: return null
        return repository.publicEventDetail(detailRoute.eventId)
    }

    fun openEvent(eventId: String) {
        route = YardScapeRoute.EventDetail(eventId)
    }

    fun openRsvp(eventId: String) {
        route = YardScapeRoute.Rsvp(eventId)
    }

    fun openHostCreateEdit(eventId: String? = null) {
        route = YardScapeRoute.HostCreateEdit(eventId)
    }

    fun returnToBrowse() {
        route = YardScapeRoute.Browse
    }
}

data class BrowseEventItem(
    val id: String,
    val title: String,
    val dateLabel: String,
    val locationLabel: String,
    val categoryLabels: List<String>,
    val statusLabel: String,
)

fun PublicEventPreview.toBrowseEventItem(nowEpochMillis: Long): BrowseEventItem =
    BrowseEventItem(
        id = id,
        title = title,
        dateLabel = saleWindow.toBrowseDateLabel(nowEpochMillis),
        locationLabel = listOfNotNull(
            publicLocation.neighborhood,
            publicLocation.distanceLabel ?: publicLocation.areaDescription,
        ).joinToString(" - "),
        categoryLabels = categories,
        statusLabel = status.name.lowercase().replaceFirstChar { it.uppercase() },
    )

fun PublicEventDetail.toDetailSections(nowEpochMillis: Long): List<Pair<String, String>> =
    listOf(
        "When" to saleWindow.toBrowseDateLabel(nowEpochMillis),
        "Area" to listOfNotNull(
            publicLocation.neighborhood,
            publicLocation.city,
            publicLocation.distanceLabel ?: publicLocation.areaDescription,
        ).joinToString(" - "),
        "Categories" to categories.joinToString(", "),
        "Host" to listOf(hostDisplayName, hostTrustSignals.firstOrNull()).joinToString(" - "),
    )

private fun com.naslabs.yardscape.domain.SaleWindow.toBrowseDateLabel(
    nowEpochMillis: Long,
): String {
    val dayOffset = (startsAtEpochMillis - nowEpochMillis).floorDiv(MILLIS_PER_DAY)
    val dayLabel = when (dayOffset) {
        0L -> "Today"
        1L -> "Tomorrow"
        else -> "In $dayOffset days"
    }
    return "$dayLabel, ${startsAtEpochMillis.toHourLabel()}-${endsAtEpochMillis.toHourLabel()}"
}

private fun Long.toHourLabel(): String {
    val hour24 = (floorMod(MILLIS_PER_DAY) / MILLIS_PER_HOUR).toInt()
    val hour12 = when (val normalized = hour24 % 12) {
        0 -> 12
        else -> normalized
    }
    val meridiem = if (hour24 < 12) "AM" else "PM"
    return "$hour12 $meridiem"
}

private fun Long.floorDiv(other: Long): Long {
    val quotient = this / other
    val remainder = this % other
    return if (remainder != 0L && (this xor other) < 0L) quotient - 1L else quotient
}

private fun Long.floorMod(other: Long): Long =
    this - floorDiv(other) * other

private const val MILLIS_PER_HOUR = 60L * 60L * 1_000L
private const val MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR
