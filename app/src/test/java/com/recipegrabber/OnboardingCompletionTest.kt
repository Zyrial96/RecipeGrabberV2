package com.recipegrabber

import com.recipegrabber.data.repository.PreferencesRepository
import com.recipegrabber.presentation.ui.screens.onboarding.OnboardingViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingCompletionTest {
    @Test fun `navigation waits until onboarding preference is persisted`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val preferences = mockk<PreferencesRepository>()
            val saved = CompletableDeferred<Unit>()
            coEvery { preferences.setOnboardingCompleted(true) } coAnswers { saved.await() }
            var navigated = false
            OnboardingViewModel(preferences).completeOnboarding { navigated = true }
            runCurrent()
            assertFalse(navigated)
            saved.complete(Unit)
            runCurrent()
            assertTrue(navigated)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
