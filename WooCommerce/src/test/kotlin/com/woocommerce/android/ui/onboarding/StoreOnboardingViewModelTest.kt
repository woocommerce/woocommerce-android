package com.woocommerce.android.ui.onboarding

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent.STORE_ONBOARDING_TASK_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.ONBOARDING_TASK_KEY
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTask
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class StoreOnboardingViewModelTest : BaseUnitTest() {
    private companion object {
        val ONBOARDING_TASK_INCOMPLETED_LIST = listOf(
            OnboardingTask(
                type = OnboardingTaskType.ABOUT_YOUR_STORE,
                isComplete = false,
                isVisible = true,
                isVisited = false
            ),
            OnboardingTask(
                type = OnboardingTaskType.ADD_FIRST_PRODUCT,
                isComplete = false,
                isVisible = true,
                isVisited = false
            ),
            OnboardingTask(
                type = OnboardingTaskType.CUSTOMIZE_DOMAIN,
                isComplete = false,
                isVisible = true,
                isVisited = false
            )
        )
        val ONBOARDING_TASK_COMPLETED_LIST = listOf(
            OnboardingTask(
                type = OnboardingTaskType.ABOUT_YOUR_STORE,
                isComplete = true,
                isVisible = true,
                isVisited = false
            ),
            OnboardingTask(
                type = OnboardingTaskType.ADD_FIRST_PRODUCT,
                isComplete = true,
                isVisible = true,
                isVisited = false
            )
        )
    }

    private val onboardingTasksCacheFlow: MutableStateFlow<List<OnboardingTask>> = MutableStateFlow(emptyList())

    private val lifecycleOwner: LifecycleOwner = mock()
    private val savedState: SavedStateHandle = SavedStateHandle()
    private val onboardingRepository: StoreOnboardingRepository = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val shouldShowOnboarding: ShouldShowOnboarding = mock()

    private lateinit var viewModel: StoreOnboardingViewModel

    @Before
    fun setup() {
        whenever(onboardingRepository.observeOnboardingTasks()).thenReturn(
            onboardingTasksCacheFlow
        )
    }

    @Test
    fun `given all onboarding tasks are completed, when view model is created, onboarding is not shown`() =
        testBlocking {
            onboardingTasksCacheFlow.tryEmit(ONBOARDING_TASK_COMPLETED_LIST)

            whenViewModelIsCreated()

            Assertions.assertThat(viewModel.viewState.value?.show).isFalse
        }

    @Test
    fun `given a list of incomplete tasks , when view model is created, onboarding is shown`() =
        testBlocking {
            onboardingTasksCacheFlow.tryEmit(ONBOARDING_TASK_INCOMPLETED_LIST)
            whenever(shouldShowOnboarding.showForTasks(ONBOARDING_TASK_INCOMPLETED_LIST)).thenReturn(true)

            whenViewModelIsCreated()

            Assertions.assertThat(viewModel.viewState.value?.show).isTrue
        }

    @Test
    fun `given onboarding is incomplete, when on resume called, onboarding tasks are fetched`() = testBlocking {
        whenever(!shouldShowOnboarding.isOnboardingMarkedAsCompleted()).thenReturn(false)

        whenViewModelIsCreated()
        viewModel.onResume(lifecycleOwner)

        verify(onboardingRepository).fetchOnboardingTasks()
    }

    @Test
    fun `given onboarding is incomplete, when pull to refresh, onboarding tasks are fetched`() = testBlocking {
        whenever(!shouldShowOnboarding.isOnboardingMarkedAsCompleted()).thenReturn(false)

        whenViewModelIsCreated()
        viewModel.onPullToRefresh()

        verify(onboardingRepository).fetchOnboardingTasks()
    }

    @Test
    fun `given onboarding is completed, when on resume called, onboarding list is hidden and task are not fetched`() =
        testBlocking {
            whenever(!shouldShowOnboarding.isOnboardingMarkedAsCompleted()).thenReturn(true)

            whenViewModelIsCreated()
            viewModel.onResume(lifecycleOwner)

            Assertions.assertThat(viewModel.viewState.value?.show).isFalse
            verify(onboardingRepository, never()).fetchOnboardingTasks()
        }

    @Test
    fun `given onboarding is completed, when pull to refresh, onboarding list is hidden and task are not fetched`() =
        testBlocking {
            whenever(!shouldShowOnboarding.isOnboardingMarkedAsCompleted()).thenReturn(true)

            whenViewModelIsCreated()
            viewModel.onResume(lifecycleOwner)

            Assertions.assertThat(viewModel.viewState.value?.show).isFalse
            verify(onboardingRepository, never()).fetchOnboardingTasks()
        }

    @Test
    fun `when any task is clicked, track task clicked event`() = testBlocking {
        whenViewModelIsCreated()
        viewModel.onTaskClicked(
            OnboardingTaskUi(
                taskUiResources = AboutYourStoreTaskRes,
                isCompleted = false
            )
        )

        verify(analyticsTrackerWrapper).track(
            stat = STORE_ONBOARDING_TASK_TAPPED,
            properties = mapOf(ONBOARDING_TASK_KEY to AnalyticsTracker.VALUE_STORE_DETAILS)
        )
    }

    private fun whenViewModelIsCreated() {
        viewModel = StoreOnboardingViewModel(
            savedState,
            onboardingRepository,
            analyticsTrackerWrapper,
            shouldShowOnboarding,
        )
    }
}
