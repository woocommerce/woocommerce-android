package com.woocommerce.android.ui.common

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.ui.common.UserEligibilityErrorViewModel.ViewState
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction.SIGN_OUT
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.user.WCUserModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class UserEligibilityErrorViewModelTest : BaseUnitTest() {
    private val appPrefsWrapper: AppPrefs = mock()
    private val dispatcher: Dispatcher = mock()
    private val accountStore: AccountStore = mock()
    private val userEligibilityFetcher: UserEligibilityFetcher = mock()

    private lateinit var viewModel: UserEligibilityErrorViewModel
    private lateinit var actionCaptor: KArgumentCaptor<Action<*>>

    private val testUser = WCUserModel().apply {
        remoteUserId = 1L
        firstName = "Anitaa"
        lastName = "Murthy"
        username = "murthyanitaa"
        roles = "[author, editor]"
        email = "reallychumma1@gmail.com"
    }

    private val viewState = ViewState()

    @Before
    fun setup() {
        actionCaptor = argumentCaptor()

        viewModel = spy(
            UserEligibilityErrorViewModel(
                SavedStateHandle(),
                appPrefsWrapper,
                dispatcher,
                accountStore,
                userEligibilityFetcher
            )
        )

        clearInvocations(
            viewModel,
            userEligibilityFetcher,
            appPrefsWrapper
        )
    }

    @Test
    fun `Displays the user eligibility error screen correctly`() = testBlocking {
        doReturn(testUser).whenever(userEligibilityFetcher).getUserByEmail(any())
        whenever(appPrefsWrapper.getUserEmail()).thenReturn(testUser.email)

        val expectedViewState = viewState.copy(user = testUser.toAppModel())

        var userData: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> userData = new }

        viewModel.start()
        assertThat(userData).isEqualTo(expectedViewState)
    }

    @Test
    fun `Handles retry button correctly when user is not eligible`() =
        testBlocking {
            doReturn(testUser).whenever(userEligibilityFetcher).fetchUserInfo()
            doReturn(false).whenever(appPrefsWrapper).isUserEligible()

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
            verify(userEligibilityFetcher, times(1)).updateUserInfo(any())

            assertFalse(appPrefsWrapper.isUserEligible())
            assertThat(snackbar).isEqualTo(ShowSnackbar(string.user_role_access_error_retry))
            assertThat(isProgressDialogShown).containsExactly(true, false)
        }

    @Test
    fun `Handles retry button correctly when user is eligible`() = testBlocking {
        testUser.roles = "[\"shop_manager\"]"
        doReturn(testUser).whenever(userEligibilityFetcher).fetchUserInfo()
        doReturn(true).whenever(appPrefsWrapper).isUserEligible()

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
        verify(userEligibilityFetcher, times(1)).updateUserInfo(any())

        assertTrue(appPrefsWrapper.isUserEligible())
        assertThat(snackbar).isNull()
        assertThat(exit).isNotNull
        assertThat(isProgressDialogShown).containsExactly(true, false)
    }

    @Test
    fun `Handles logout button click correctly`() {
        doReturn(false).whenever(accountStore).hasAccessToken()

        var logoutEvent: Logout? = null
        viewModel.event.observeForever {
            if (it is Logout) logoutEvent = it
        }

        viewModel.onLogoutButtonClicked()

        // note that we expect two dispatches because there's one to sign out the user and
        // the other to remove WPcom and Jetpack sites from local db
        verify(dispatcher, times(2)).dispatch(actionCaptor.capture())
        assertEquals(SIGN_OUT, actionCaptor.firstValue.type)
        assertEquals(SiteAction.REMOVE_WPCOM_AND_JETPACK_SITES, actionCaptor.secondValue.type)

        viewModel.onAccountChanged(OnAccountChanged())
        assertThat(logoutEvent).isNotNull
    }
}
