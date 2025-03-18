package com.woocommerce.android.ui.login.jetpack.sitecredentials

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.JetpackConnectionStatus
import com.woocommerce.android.model.JetpackSiteRegistrationStatus
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class JetpackActivationSiteCredentialsViewModelTest : BaseUnitTest() {
    private val siteUrl = "https://woocommerce.com/"
    private val wpApiSiteRepository: WPApiSiteRepository = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val fetchJetpackStatus: FetchJetpackStatus = mock()
    private val appPrefs: AppPrefsWrapper = mock()

    private lateinit var viewModel: JetpackActivationSiteCredentialsViewModel

    suspend fun setUp(isJetpackInstalled: Boolean, prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()

        viewModel = JetpackActivationSiteCredentialsViewModel(
            JetpackActivationSiteCredentialsFragmentArgs(
                siteUrl = siteUrl,
                jetpackStatus = JetpackStatus(
                    isJetpackInstalled, JetpackConnectionStatus.AccountNotConnected(
                        siteRegistrationStatus = JetpackSiteRegistrationStatus.UNKNOWN,
                        blogId = null
                    )
                )
            ).toSavedStateHandle(),
            wpApiSiteRepository,
            analyticsTrackerWrapper,
            fetchJetpackStatus,
            appPrefs
        )
    }

    @Test
    fun `given successfull login, when Jetpack Status fetching succeeds, then start Jetpack Activation`() =
        testBlocking {
            val isJetpackInstalled = true
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = isJetpackInstalled,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                    siteRegistrationStatus = JetpackSiteRegistrationStatus.NOT_REGISTERED,
                    blogId = null
                )
            )
            setUp(isJetpackInstalled = isJetpackInstalled) {
                givenLoginResult(Result.success(SiteModel()))

                givenJetpackFetchResult(
                    Result.success(FetchJetpackStatus.JetpackStatusFetchResponse.Success(jetpackStatus))
                )
            }

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onUsernameChanged("username")
                viewModel.onPasswordChanged("password")
                viewModel.onContinueClick()
            }.last()

            assertThat(event).isEqualTo(
                JetpackActivationSiteCredentialsViewModel.NavigateToJetpackActivationSteps(
                    siteUrl,
                    jetpackStatus
                )
            )
        }

    @Test
    fun `given successfull login, when Jetpack Status fetching fails, then show error message`() =
        testBlocking {
            val isJetpackInstalled = true
            setUp(isJetpackInstalled = isJetpackInstalled) {
                givenLoginResult(Result.success(SiteModel()))

                givenJetpackFetchResult(Result.failure(Exception()))
            }

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onUsernameChanged("username")
                viewModel.onPasswordChanged("password")
                viewModel.onContinueClick()
            }.last()

            assertThat(event)
                .isEqualTo(MultiLiveEvent.Event.ShowUiStringSnackbar(UiStringRes(R.string.error_generic)))
        }

    @Test
    fun `given successful login, when Jetpack Status fetching fails with forbidden, then continue with default value`() =
        testBlocking {
            val isJetpackInstalled = true
            setUp(isJetpackInstalled = isJetpackInstalled) {
                givenLoginResult(Result.success(SiteModel()))

                givenJetpackFetchResult(
                    Result.success(FetchJetpackStatus.JetpackStatusFetchResponse.ConnectionForbidden)
                )
            }

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onUsernameChanged("username")
                viewModel.onPasswordChanged("password")
                viewModel.onContinueClick()
            }.last()

            assertThat(event).isEqualTo(
                JetpackActivationSiteCredentialsViewModel.NavigateToJetpackActivationSteps(
                    siteUrl,
                    JetpackStatus(
                        isJetpackInstalled = isJetpackInstalled,
                        jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                            siteRegistrationStatus = JetpackSiteRegistrationStatus.NOT_REGISTERED,
                            blogId = null
                        )
                    )
                )
            )
        }

    @Test
    fun `given successful login, when Jetpack is already connected, then show an alert`() = testBlocking {
        val isJetpackInstalled = true
        setUp(isJetpackInstalled = isJetpackInstalled) {
            givenLoginResult(Result.success(SiteModel()))

            givenJetpackFetchResult(
                Result.success(
                    FetchJetpackStatus.JetpackStatusFetchResponse.Success(
                        JetpackStatus(
                            isJetpackInstalled = isJetpackInstalled,
                            jetpackConnectionStatus = JetpackConnectionStatus.AccountConnected("email")
                        )
                    )
                )
            )
        }

        val dialogState = viewModel.dialogState.runAndCaptureValues {
            viewModel.onUsernameChanged("username")
            viewModel.onPasswordChanged("password")
            viewModel.onContinueClick()
        }.last()

        assertThat(dialogState).matches {
            it?.title == UiStringRes(R.string.login_jetpack_user_already_connected_dialog_title) &&
                it.message == UiStringRes(R.string.login_jetpack_user_already_connected_dialog_message) &&
                it.positiveButton?.text == UiStringRes(R.string.login_jetpack_user_already_connected_dialog_proceed_button) &&
                it.negativeButton?.text == UiStringRes(R.string.login_jetpack_user_already_connected_dialog_cancel_button)
        }
    }

    @Test
    fun `given account connection alert is shown, when user confirms, then proceed to the WordPress_com login`() =
        testBlocking {
            val isJetpackInstalled = true
            setUp(isJetpackInstalled = isJetpackInstalled) {
                givenLoginResult(Result.success(SiteModel()))

                givenJetpackFetchResult(
                    Result.success(
                        FetchJetpackStatus.JetpackStatusFetchResponse.Success(
                            JetpackStatus(
                                isJetpackInstalled = isJetpackInstalled,
                                jetpackConnectionStatus = JetpackConnectionStatus.AccountConnected("email")
                            )
                        )
                    )
                )
            }

            val dialogState = viewModel.dialogState.runAndCaptureValues {
                viewModel.onUsernameChanged("username")
                viewModel.onPasswordChanged("password")
                viewModel.onContinueClick()
            }.last()

            val event = viewModel.event.runAndCaptureValues {
                dialogState?.positiveButton?.onClick?.invoke()
            }.last()

            assertThat(event).isEqualTo(
                JetpackActivationSiteCredentialsViewModel.OpenWordPressComLogin("email")
            )
            verify(appPrefs).setLoginSiteAddress(siteUrl)
        }

    @Test
    fun `given account connection alert is shown, when user cancels, then clear the username and password`() =
        testBlocking {
            val isJetpackInstalled = true
            setUp(isJetpackInstalled = isJetpackInstalled) {
                givenLoginResult(Result.success(SiteModel()))

                givenJetpackFetchResult(
                    Result.success(
                        FetchJetpackStatus.JetpackStatusFetchResponse.Success(
                            JetpackStatus(
                                isJetpackInstalled = isJetpackInstalled,
                                jetpackConnectionStatus = JetpackConnectionStatus.AccountConnected("email")
                            )
                        )
                    )
                )
            }

            val dialogState = viewModel.dialogState.runAndCaptureValues {
                viewModel.onUsernameChanged("username")
                viewModel.onPasswordChanged("password")
                viewModel.onContinueClick()
            }.last()

            val viewState = viewModel.viewState.runAndCaptureValues {
                dialogState?.negativeButton?.onClick?.invoke()
            }.last()

            assertThat(viewState.username).isEmpty()
            assertThat(viewState.password).isEmpty()
            assertThat(viewModel.dialogState.getOrAwaitValue()).isNull()
        }

    private suspend fun givenLoginResult(result: Result<SiteModel>) {
        given(wpApiSiteRepository.loginAndFetchSite(any(), any(), any())).willReturn(result)
    }

    private suspend fun givenJetpackFetchResult(
        result: Result<FetchJetpackStatus.JetpackStatusFetchResponse>
    ) {
        given(fetchJetpackStatus.invoke(any(), any(), any())).willReturn(result)
    }
}
