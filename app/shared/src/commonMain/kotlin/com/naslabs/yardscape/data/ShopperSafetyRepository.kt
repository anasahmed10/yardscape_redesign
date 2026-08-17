package com.naslabs.yardscape.data

import com.naslabs.yardscape.domain.SafetyReportDraft
import com.naslabs.yardscape.domain.ShopperSafetyPolicy
import com.naslabs.yardscape.domain.YardSaleEvent

interface ShopperSafetyRepository {
    fun submitReport(draft: SafetyReportDraft): SafetyRepositoryResult<ReportReceipt>

    fun blockHostForEvent(eventId: String): SafetyRepositoryResult<BlockedHostUpdate>

    fun unblockHostForEvent(eventId: String): SafetyRepositoryResult<BlockedHostUpdate>
}

sealed interface SafetyRepositoryResult<out T> {
    data class Success<T>(val value: T) : SafetyRepositoryResult<T>

    data class ValidationFailure(val messages: List<String>) : SafetyRepositoryResult<Nothing>

    data class Offline(
        val message: String = "You're offline. This safety action was not completed.",
    ) : SafetyRepositoryResult<Nothing>

    data class ServerError(
        val message: String = "The safety service could not complete this action. Try again.",
    ) : SafetyRepositoryResult<Nothing>
}

data class ReportReceipt(val id: String)

data class BlockedHostUpdate(
    val hostId: String,
    val affectedEventIds: Set<String>,
    val isBlocked: Boolean,
)

data class SubmittedSafetyReport(
    val receipt: ReportReceipt,
    val draft: SafetyReportDraft,
)

enum class SeededSafetyOutcome {
    Success,
    Offline,
    ServerError,
}

data class SeededSafetyBehavior(
    val reportOutcome: SeededSafetyOutcome = SeededSafetyOutcome.Success,
    val blockOutcome: SeededSafetyOutcome = SeededSafetyOutcome.Success,
    val unblockOutcome: SeededSafetyOutcome = SeededSafetyOutcome.Success,
)

class SeededShopperSafetyRepository(
    events: List<YardSaleEvent> = SeededYardSaleData.events,
    private val behavior: SeededSafetyBehavior = SeededSafetyBehavior(),
) : ShopperSafetyRepository {
    private val hostIdByEventId: Map<String, String> = events.associate { it.id to it.host.id }
    private val eventIdsByHostId: Map<String, Set<String>> = events
        .groupBy { it.host.id }
        .mapValues { (_, hostEvents) -> hostEvents.mapTo(linkedSetOf()) { it.id } }
    private val reports = mutableListOf<SubmittedSafetyReport>()
    private val blockedHostIds = mutableSetOf<String>()

    val submittedReports: List<SubmittedSafetyReport>
        get() = reports.toList()

    val blockedHosts: Set<String>
        get() = blockedHostIds.toSet()

    override fun submitReport(draft: SafetyReportDraft): SafetyRepositoryResult<ReportReceipt> {
        val validationMessages = ShopperSafetyPolicy.validateReport(draft).map { it.message }
        if (validationMessages.isNotEmpty()) {
            return SafetyRepositoryResult.ValidationFailure(validationMessages)
        }
        val normalized = ShopperSafetyPolicy.normalizedReport(draft)
        return when (behavior.reportOutcome) {
            SeededSafetyOutcome.Offline -> SafetyRepositoryResult.Offline()
            SeededSafetyOutcome.ServerError -> SafetyRepositoryResult.ServerError()
            SeededSafetyOutcome.Success -> {
                val receipt = ReportReceipt(id = "mock-report-${reports.size + 1}")
                reports += SubmittedSafetyReport(receipt = receipt, draft = normalized)
                SafetyRepositoryResult.Success(receipt)
            }
        }
    }

    override fun blockHostForEvent(eventId: String): SafetyRepositoryResult<BlockedHostUpdate> =
        updateBlock(eventId = eventId, shouldBlock = true, outcome = behavior.blockOutcome)

    override fun unblockHostForEvent(eventId: String): SafetyRepositoryResult<BlockedHostUpdate> =
        updateBlock(eventId = eventId, shouldBlock = false, outcome = behavior.unblockOutcome)

    private fun updateBlock(
        eventId: String,
        shouldBlock: Boolean,
        outcome: SeededSafetyOutcome,
    ): SafetyRepositoryResult<BlockedHostUpdate> {
        val hostId = hostIdByEventId[eventId]
            ?: return SafetyRepositoryResult.ValidationFailure(listOf("This sale is unavailable."))
        return when (outcome) {
            SeededSafetyOutcome.Offline -> SafetyRepositoryResult.Offline()
            SeededSafetyOutcome.ServerError -> SafetyRepositoryResult.ServerError()
            SeededSafetyOutcome.Success -> {
                if (shouldBlock) blockedHostIds += hostId else blockedHostIds -= hostId
                SafetyRepositoryResult.Success(
                    BlockedHostUpdate(
                        hostId = hostId,
                        affectedEventIds = eventIdsByHostId.getValue(hostId),
                        isBlocked = shouldBlock,
                    ),
                )
            }
        }
    }
}
