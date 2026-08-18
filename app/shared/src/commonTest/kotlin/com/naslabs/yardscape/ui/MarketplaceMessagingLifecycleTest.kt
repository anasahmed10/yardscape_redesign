package com.naslabs.yardscape.ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MarketplaceMessagingLifecycleTest {
    @Test
    fun initialLoadIsNotCancelledOrDuplicatedWhenTheSuspendingCallbackChangesInboxState() = runTest {
        val enteredLoad = CompletableDeferred<Unit>()
        val releaseLoad = CompletableDeferred<Unit>()
        var loadCount = 0
        val lifecycle = MarketplaceMessagingLifecycle(
            onLoadInbox = {
                loadCount++
                enteredLoad.complete(Unit)
                releaseLoad.await()
                true
            },
            onResumePendingThread = { false },
        )

        val first = async { lifecycle.handleAuthorizationSignal(0L, false, MessagingInboxUiState.Idle) }
        enteredLoad.await()
        runCurrent()
        assertFalse(lifecycle.handleAuthorizationSignal(0L, false, MessagingInboxUiState.Loading))
        assertEquals(1, loadCount)

        releaseLoad.complete(Unit)
        assertTrue(first.await())
        assertEquals(1, loadCount)
    }

    @Test
    fun queuedThreadSignalsResumeSequentiallyAndSameRouteSignalsDoNotDuplicateWork() = runTest {
        var resumeCount = 0
        var loadCount = 0
        val lifecycle = MarketplaceMessagingLifecycle(
            onLoadInbox = { loadCount++; true },
            onResumePendingThread = { resumeCount++; true },
        )

        assertTrue(lifecycle.handleAuthorizationSignal(4L, true, MessagingInboxUiState.Idle))
        assertFalse(lifecycle.handleAuthorizationSignal(4L, true, MessagingInboxUiState.Idle))
        assertTrue(lifecycle.handleAuthorizationSignal(5L, true, MessagingInboxUiState.Idle))

        assertEquals(2, resumeCount)
        assertEquals(0, loadCount)
    }
}
