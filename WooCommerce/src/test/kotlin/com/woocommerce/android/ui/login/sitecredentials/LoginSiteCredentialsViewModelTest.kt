package com.woocommerce.android.ui.login.sitecredentials

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.applicationpasswords.ApplicationPasswordsNotifier
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.ui.login.sitecredentials.LoginSiteCredentialsViewModel.LoggedIn
import com.woocommerce.android.ui.login.sitecredentials.LoginSiteCredentialsViewModel.ShowApplicationPasswordsUnavailableScreen
import com.woocommerce.android.ui.login.sitecredentials.LoginSiteCredentialsViewModel.ShowNonWooErrorScreen
import com.woocommerce.android.util.observeForTesting
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.user.WCUserModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationError
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.INCORRECT_USERNAME_OR_PASSWORD
import org.wordpress.android.login.LoginAnalyticsListener

@ExperimentalCoroutinesApi
class LoginSiteCredentialsViewModelTest : BaseUnitTest() {
    private val testUsername = "username"
    private val testPassword = "password"
    private val siteAddress: String = "http://site.com"
    private val testSite = SiteModel()
    private val testUser = WCUserModel()
    private val applicationPasswordsUnavailableEvents = MutableSharedFlow<WPAPINetworkError>(extraBufferCapacity = 1)

    private val wpApiSiteRepository: WPApiSiteRepository = mock {
        onBlocking { login(eq(siteAddress), any(), any()) } doReturn Result.success(testSite)
        onBlocking { checkWooStatus(testSite) } doReturn Result.success(true)
        onBlocking { getSiteByUrl(siteAddress) } doReturn testSite
    }
    private var isJetpackConnected: Boolean = false
    private val selectedSite: SelectedSite = mock {
        on { exists() } doReturn false
    }
    private val applicationPasswordsNotifier: ApplicationPasswordsNotifier = mock {
        on { featureUnavailableEvents } doReturn applicationPasswordsUnavailableEvents
    }
    private val userEligibilityFetcher: UserEligibilityFetcher = mock {
        onBlocking { fetchUserInfo() } doReturn testUser
    }
    private val loginAnalyticsListener: LoginAnalyticsListener = mock()
    private val resourceProvider: ResourceProvider = mock()

    private lateinit var viewModel: LoginSiteCredentialsViewModel

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()

        viewModel = LoginSiteCredentialsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    LoginSiteCredentialsViewModel.SITE_ADDRESS_KEY to siteAddress,
                    LoginSiteCredentialsViewModel.IS_JETPACK_CONNECTED_KEY to isJetpackConnected
                )
            ),
            wpApiSiteRepository = wpApiSiteRepository,
            selectedSite = selectedSite,
            loginAnalyticsListener = loginAnalyticsListener,
            resourceProvider = resourceProvider,
            applicationPasswordsNotifier = applicationPasswordsNotifier,
            userEligibilityFetcher = userEligibilityFetcher
        )
    }

    @Test
    fun `when changing username, then update state`() = testBlocking {
        setup()

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onUsernameChanged(testUsername)
        }.last()

        assertThat(state.username).isEqualTo(testUsername)
    }

    @Test
    fun `when changing password, then update state`() = testBlocking {
        setup()

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onUsernameChanged(testPassword)
        }.last()

        assertThat(state.username).isEqualTo(testPassword)
    }

    @Test
    fun `when username is empty, then mark input as invalid`() = testBlocking {
        setup()

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onUsernameChanged("")
        }.last()

        assertThat(state.isValid).isFalse()
    }

    @Test
    fun `when password is empty, then mark input as invalid`() = testBlocking {
        setup()

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onPasswordChanged("")
        }.last()

        assertThat(state.isValid).isFalse()
    }

    @Test
    fun `given login successful, when submitting login, then log the user successfully`() = testBlocking {
        setup()

        viewModel.state.observeForTesting {
            viewModel.onUsernameChanged(testUsername)
            viewModel.onPasswordChanged(testPassword)
            viewModel.onContinueClick()
        }

        assertThat(viewModel.event.value).isEqualTo(LoggedIn(testSite.id))
    }

    @Test
    fun `given incorrect credentials, when submitting, then show error`() = testBlocking {
        setup {
            whenever(wpApiSiteRepository.login(siteAddress, testUsername, testPassword)).thenReturn(
                Result.failure(OnChangedException(AuthenticationError(INCORRECT_USERNAME_OR_PASSWORD, "")))
            )
        }

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onUsernameChanged(testUsername)
            viewModel.onPasswordChanged(testPassword)
            viewModel.onContinueClick()
        }.last()

        assertThat(state.errorMessage).isEqualTo(R.string.username_or_password_incorrect)
    }

    @Test
    fun `given site without Woo, when submitting, then show error screen`() = testBlocking {
        setup {
            whenever(wpApiSiteRepository.checkWooStatus(testSite)).thenReturn(Result.success(false))
        }

        viewModel.state.observeForTesting {
            viewModel.onUsernameChanged(testUsername)
            viewModel.onPasswordChanged(testPassword)
            viewModel.onContinueClick()
        }

        assertThat(viewModel.event.value).isEqualTo(ShowNonWooErrorScreen(siteAddress))
    }

    @Test
    fun `given Woo check fails, when submitting, then show snackbar`() = testBlocking {
        setup {
            whenever(wpApiSiteRepository.checkWooStatus(testSite)).thenReturn(Result.failure(Exception()))
        }

        viewModel.state.observeForTesting {
            viewModel.onUsernameChanged(testUsername)
            viewModel.onPasswordChanged(testPassword)
            viewModel.onContinueClick()
        }

        assertThat(viewModel.event.value).isEqualTo(ShowSnackbar(R.string.error_generic))
    }

    @Test
    fun `given site without Woo, when attempting Woo installation, then recheck woo status`() = testBlocking {
        setup {
            whenever(wpApiSiteRepository.getSiteByUrl(siteAddress)).thenReturn(testSite)
        }

        viewModel.state.observeForTesting {
            viewModel.onWooInstallationAttempted()
        }

        verify(wpApiSiteRepository).checkWooStatus(testSite)
    }

    @Test
    fun `given application passwords disabled, when submitting login, then show error screen`() = testBlocking {
        setup {
            whenever(wpApiSiteRepository.checkWooStatus(testSite)).thenReturn(Result.failure(Exception()))
        }

        viewModel.state.observeForTesting {
            viewModel.onUsernameChanged(testUsername)
            viewModel.onPasswordChanged(testPassword)
            viewModel.onContinueClick()
            applicationPasswordsUnavailableEvents.tryEmit(mock())
        }

        assertThat(viewModel.event.value)
            .isEqualTo(ShowApplicationPasswordsUnavailableScreen(siteAddress, isJetpackConnected))
    }

    @Test
    fun `give user role fetch fails, when submitting login, then show a snackbar`() = testBlocking {
        setup {
            whenever(userEligibilityFetcher.fetchUserInfo()).thenReturn(null)
        }

        viewModel.state.observeForTesting {
            viewModel.onUsernameChanged(testUsername)
            viewModel.onPasswordChanged(testPassword)
            viewModel.onContinueClick()
        }

        assertThat(viewModel.event.value).isEqualTo(ShowSnackbar(R.string.error_generic))
    }
}
