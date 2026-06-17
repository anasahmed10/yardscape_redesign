package com.naslabs.yardscape.domain

data class YardSaleEvent(
    val id: String,
    val title: String,
    val description: String,
    val saleWindow: SaleWindow,
    val categories: List<String>,
    val photos: List<EventPhoto>,
    val acceptedPaymentTypes: List<String> = emptyList(),
    val accessibilityNotes: List<String> = emptyList(),
    val host: UserProfile,
    val status: EventStatus,
    val location: EventLocation,
)

data class SaleWindow(
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
) {
    init {
        require(startsAtEpochMillis < endsAtEpochMillis) {
            "Sale window must end after it starts."
        }
    }

    fun hasEnded(nowEpochMillis: Long): Boolean =
        nowEpochMillis >= endsAtEpochMillis
}

data class EventLocation(
    val publicLocation: PublicLocation,
    val exactAddress: ExactAddress,
)

data class PublicLocation(
    val neighborhood: String,
    val city: String,
    val areaDescription: String,
    val distanceLabel: String? = null,
)

data class ExactAddress(
    val streetAddress: String,
    val unit: String? = null,
    val city: String,
    val region: String,
    val postalCode: String,
    val latitude: Double,
    val longitude: Double,
    val accessInstructions: String? = null,
)

data class EventPhoto(
    val url: String,
    val description: String? = null,
)

data class Rsvp(
    val id: String,
    val eventId: String,
    val shopperId: String,
    val status: RsvpStatus,
    val locationVisibility: LocationVisibility,
)

data class UserProfile(
    val id: String,
    val displayName: String,
    val role: UserRole,
    val verificationState: VerificationState = VerificationState.UNVERIFIED,
    val trustSignals: List<String> = emptyList(),
)

data class PublicEventPreview(
    val id: String,
    val title: String,
    val description: String,
    val saleWindow: SaleWindow,
    val categories: List<String>,
    val photos: List<EventPhoto>,
    val acceptedPaymentTypes: List<String>,
    val accessibilityNotes: List<String>,
    val hostDisplayName: String,
    val hostTrustSignals: List<String>,
    val publicLocation: PublicLocation,
    val status: EventStatus,
)

enum class EventStatus {
    DRAFT,
    PUBLISHED,
    CANCELLED,
    COMPLETED,
    HIDDEN,
}

enum class LocationVisibility {
    PUBLIC_APPROXIMATION,
    RSVP_REQUESTED,
    RSVP_ACCEPTED,
    REVOKED,
    EXPIRED,
}

enum class RsvpStatus {
    REQUESTED,
    ACCEPTED,
    CANCELLED,
    DECLINED,
}

enum class UserRole {
    HOST,
    SHOPPER,
}

enum class VerificationState {
    UNVERIFIED,
    VERIFIED,
}

fun YardSaleEvent.toPublicPreview(): PublicEventPreview =
    PublicEventPreview(
        id = id,
        title = title,
        description = description,
        saleWindow = saleWindow,
        categories = categories,
        photos = photos,
        acceptedPaymentTypes = acceptedPaymentTypes,
        accessibilityNotes = accessibilityNotes,
        hostDisplayName = host.displayName,
        hostTrustSignals = host.trustSignals,
        publicLocation = location.publicLocation,
        status = status,
    )
