package com.naslabs.yardscape.ui

import com.naslabs.yardscape.data.HostPhotoPicker
import com.naslabs.yardscape.data.SeededYardSaleData
import com.naslabs.yardscape.data.withMapSelectedLocation
import com.naslabs.yardscape.domain.EventPhoto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HostEditorFlowTest {
    @Test
    fun partialJourneyCanBeSavedAndResumedBeforeStepValidationPasses() {
        val appState = YardScapeAppState()
        val partial = appState.hostEditorState(null).copy(
            draft = appState.hostEditorState(null).draft.copy(title = "Early draft"),
        )

        val saved = appState.saveHostDraft(partial)
        val savedId = assertNotNull(saved.savedEventId)
        val resumed = appState.hostEditorState(savedId)

        assertTrue(saved.validationErrors.isEmpty())
        assertEquals("Early draft", resumed.draft.title)
        assertEquals(HostEditorStep.Basics, resumed.step)
    }

    @Test
    fun forwardNavigationStopsAtFirstInvalidStepWithRecoveryMessage() {
        val appState = YardScapeAppState()
        val blank = appState.hostEditorState(null)

        val missingBasics = appState.moveHostEditor(blank, HostEditorStep.Preview)
        assertEquals(HostEditorStep.Basics, missingBasics.step)
        assertTrue(missingBasics.validationErrors.any { it.contains("title") })

        val basicsComplete = missingBasics.copy(
            draft = missingBasics.draft.copy(
                title = "Saturday sale",
                description = "Furniture and garden supplies",
                categories = listOf("garden"),
            ),
            validationErrors = emptyList(),
        )
        val missingLocation = appState.moveHostEditor(basicsComplete, HostEditorStep.Preview)
        assertEquals(HostEditorStep.Location, missingLocation.step)
        assertTrue(missingLocation.validationErrors.any { it.contains("map location") })
    }

    @Test
    fun hostCanCompleteSaveLeaveAndResumeMockJourney() {
        val photo = EventPhoto("mock://test/photo", "Front tables")
        val appState = YardScapeAppState(hostPhotoPicker = HostPhotoPicker { listOf(photo) })
        val blank = appState.hostEditorState(null)
        val location = appState.searchHostLocations("hidden").first()
        val complete = blank.copy(
            draft = blank.draft.copy(
                title = "Saturday sale",
                description = "Furniture and garden supplies",
                categories = listOf("garden", "furniture"),
                photos = listOf(photo),
            ).withMapSelectedLocation(location),
            attendeeCapInput = "12",
            approvalMode = HostRsvpApprovalMode.ManualReview,
        )

        val preview = appState.moveHostEditor(complete, HostEditorStep.Preview)
        assertEquals(HostEditorStep.Preview, preview.step)
        assertTrue(preview.validationErrors.isEmpty())

        val saved = appState.saveHostDraft(preview)
        val savedId = assertNotNull(saved.savedEventId)
        val resumed = appState.hostEditorState(savedId)

        assertEquals(HostEditorStep.Preview, resumed.step)
        assertEquals(12, resumed.attendeeCap)
        assertEquals(HostRsvpApprovalMode.ManualReview, resumed.approvalMode)
        assertEquals(listOf(photo), resumed.draft.photos)
    }

    @Test
    fun publicPreviewOmitsProtectedLocationAndAccessInstructions() {
        val appState = YardScapeAppState()
        val seeded = appState.hostEditorState(SeededYardSaleData.DRAFT_EVENT_ID)
        val previewText = seeded.copy(
            draft = seeded.draft.copy(accessInstructions = "Use private gate 2468"),
        ).publicPreview().toString()

        assertFalse(previewText.contains(seeded.draft.exactStreetAddress))
        assertFalse(previewText.contains("private gate 2468"))
        assertFalse(previewText.contains(seeded.draft.exactLatitude.toString()))
        assertTrue(previewText.contains(seeded.draft.publicNeighborhood))
    }

    @Test
    fun photoPickerIsInjectableAndPublishedOrderAndCaptionsArePreserved() {
        val photos = listOf(
            EventPhoto("mock://photo/second", "Second caption"),
            EventPhoto("mock://photo/first", "First caption"),
        )
        val appState = YardScapeAppState(hostPhotoPicker = HostPhotoPicker { photos })
        assertEquals(photos, appState.availableHostPhotos())

        val seeded = appState.hostEditorState(SeededYardSaleData.DRAFT_EVENT_ID)
        val published = appState.publishHostEvent(
            seeded.copy(draft = seeded.draft.copy(photos = photos)),
        )

        assertTrue(published.validationErrors.isEmpty())
        assertEquals(photos, published.draft.photos)
        assertEquals(listOf("Second caption", "First caption"), published.publicPreview().photoCaptions)
        assertEquals(listOf("mock://photo/second", "mock://photo/first"), published.publicPreview().photoReferences)
    }

    @Test
    fun invalidRsvpPolicyCannotAdvanceAndDestructiveCopyRequiresExplicitChoice() {
        val appState = YardScapeAppState()
        val seeded = appState.hostEditorState(SeededYardSaleData.DRAFT_EVENT_ID).copy(
            step = HostEditorStep.RsvpSettings,
            attendeeCapInput = "zero",
        )

        val blocked = appState.moveHostEditor(seeded, HostEditorStep.Preview)
        assertEquals(HostEditorStep.RsvpSettings, blocked.step)
        assertTrue(blocked.validationErrors.any { it.contains("whole number") })

        assertEquals(null, seeded.pendingConfirmation)
        HostConfirmationAction.entries.forEach { action ->
            val requested = seeded.requestConfirmation(action)
            assertEquals(action, requested.pendingConfirmation)
            assertEquals(null, requested.dismissConfirmation().pendingConfirmation)
            if (action != HostConfirmationAction.Publish) {
                assertTrue(action.message.contains("revok"))
            }
        }
    }

    @Test
    fun previewCanReturnToAnEarlierStepAndExistingEventRestoresItsAttendancePolicy() {
        val appState = YardScapeAppState()
        assertTrue(
            appState.updateHostAttendancePolicy(
                SeededYardSaleData.FAMILY_GARAGE_EVENT_ID,
                HostAttendancePolicy(attendeeCap = 7, approvalMode = HostRsvpApprovalMode.ManualReview),
            ),
        )
        val editor = appState.hostEditorState(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID).copy(step = HostEditorStep.Preview)

        assertEquals("7", editor.attendeeCapInput)
        assertEquals(HostRsvpApprovalMode.ManualReview, editor.approvalMode)
        assertEquals(HostEditorStep.Basics, appState.moveHostEditor(editor, HostEditorStep.Basics).step)

        appState.publishHostEvent(editor)
        assertEquals(7, appState.hostAttendanceState(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID)?.policy?.attendeeCap)
        assertEquals(HostRsvpApprovalMode.ManualReview, appState.hostAttendanceState(SeededYardSaleData.FAMILY_GARAGE_EVENT_ID)?.policy?.approvalMode)
    }

    @Test
    fun newSaleReplacesTheSameRouteEditorSessionWithABlankDraft() {
        val appState = YardScapeAppState()
        val edited = appState.hostEditorState(null).copy(draft = appState.hostEditorState(null).draft.copy(title = "Stale draft"))
        appState.rememberHostEditorState(edited)
        val beforeReset = appState.hostEditorSessionSignal

        appState.openHostCreateEdit()

        assertTrue(appState.hostEditorSessionSignal > beforeReset)
        assertEquals("", appState.hostEditorState(null).draft.title)
    }
}
