package com.woocommerce.android.ui.common

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.User
import com.woocommerce.android.model.UserRole
import com.woocommerce.android.ui.common.UserEligibilityErrorViewModel.ViewState
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Logout
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class UserEligibilityErrorViewModelTest : BaseUnitTest() {
    private val testUser = User(
        id = 1L,
        firstName = "Anitaa",
        lastName = "Murthy",
        username = "murthyanitaa",
        email = "reallychumma1@gmail.com",
        roles = listOf(UserRole.Editor, UserRole.Author)
    )

    private val userEligibilityFetcher: UserEligibilityFetcher = mock {
        on { getUser() } doReturn testUser
    }
    private val accountRepository: AccountRepository = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    private lateinit var viewModel: UserEligibilityErrorViewModel

    private val viewState = ViewState()

    @Before
    fun setup() {
        viewModel = UserEligibilityErrorViewModel(
            savedState = SavedStateHandle(),
            accountRepository = accountRepository,
            userEligibilityFetcher = userEligibilityFetcher,
            analyticsTracker = analyticsTracker
        )
    }

    @Test
    fun `Displays the user eligibility error screen correctly`() = testBlocking {
        val expectedViewState = viewState.copy(user = testUser)

        var userData: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> userData = new }

        assertThat(userData).isEqualTo(expectedViewState)
    }

    @Test
    fun `Handles retry button correctly when user is not eligible`() =
        testBlocking {
            doReturn(testUser).whenever(userEligibilityFetcher).fetchUserInfo()

            val isProgressDialogShown = ArrayList<Boolean>()
            viewModel.viewStateData.observeForever { old, new ->
                new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) {
                    isProgressDialogShown.add(it)
                }
            }

            var snackbar: ShowSnackbar? = null
            viewModel.event.observeForever {
                if (it is ShowSnackbar) snackbar = it
            }

            viewModel.onRetryButtonClicked()

            verify(userEligibilityFetcher, times(1)).fetchUserInfo()

            assertThat(snackbar).isEqualTo(ShowSnackbar(string.user_role_access_error_retry))
            assertThat(isProgressDialogShown).containsExactly(true, false)
        }

    @Test
    fun `Handles retry button correctly when user is eligible`() = testBlocking {
        val user = testUser.copy(roles = listOf(UserRole.ShopManager))
        doReturn(user).whenever(userEligibilityFetcher).fetchUserInfo()

        val isProgressDialogShown = ArrayList<Boolean>()
        viewModel.viewStateData.observeForever { old, new ->
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) {
                isProgressDialogShown.add(it)
            }
        }

        var snackbar: ShowSnackbar? = null
        var exit: Exit? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
            if (it is Exit) exit = it
        }

        viewModel.onRetryButtonClicked()

        verify(userEligibilityFetcher, times(1)).fetchUserInfo()

        assertThat(snackbar).isNull()
        assertThat(exit).isNotNull
        assertThat(isProgressDialogShown).containsExactly(true, false)
    }

    @Test
    fun `Handles logout button click correctly`() = testBlocking {
        doReturn(true).whenever(accountRepository).logout()

        var logoutEvent: Logout? = null
        viewModel.event.observeForever {
            logoutEvent = it as? Logout
        }

        viewModel.onLogoutButtonClicked()

        verify(accountRepository).logout()
        assertThat(logoutEvent).isNotNull
    }
}
