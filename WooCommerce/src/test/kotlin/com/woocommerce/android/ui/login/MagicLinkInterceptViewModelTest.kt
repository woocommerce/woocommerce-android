package com.woocommerce.android.ui.login

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.JetpackConnectionStatus
import com.woocommerce.android.model.JetpackSiteRegistrationStatus
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class MagicLinkInterceptViewModelTest : BaseUnitTest() {
    private val site = SiteModel().apply {
        url = "https://woocommerce.com/"
    }
    private val magicLinkInterceptRepository: MagicLinkInterceptRepository = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn site
    }
    private val fetchJetpackStatus: FetchJetpackStatus = mock()

    private lateinit var viewModel: MagicLinkInterceptViewModel

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()

        viewModel = MagicLinkInterceptViewModel(
            savedState = SavedStateHandle(),
            magicLinkInterceptRepository = magicLinkInterceptRepository,
            selectedSite = selectedSite,
            fetchJetpackStatus = fetchJetpackStatus
        )
    }

    @Test
    fun `given jetpack connection, when account info is fetched, then fetch jetpack status`() = testBlocking {
        setup {
            givenAuthTokenUpdateResult(RequestResult.SUCCESS)
            given(selectedSite.connectionType).willReturn(SiteConnectionType.ApplicationPasswords)
            givenJetpackStatusFetchResult(
                Result.success(
                    FetchJetpackStatus.JetpackStatusFetchResponse.Success(
                        JetpackStatus(
                            isJetpackInstalled = true,
                            jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                                siteRegistrationStatus = JetpackSiteRegistrationStatus.UNKNOWN,
                                blogId = null
                            )
                        )
                    )
                )
            )
        }

        viewModel.handleMagicLink("authToken", MagicLinkFlow.JetpackConnection)

        verify(fetchJetpackStatus).invoke(site, useApplicationPasswords = true)
    }

    @Test
    fun `given jetpack connection, when fetching jetpack status succeeds, then continue jetpack setup`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = true,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                    siteRegistrationStatus = JetpackSiteRegistrationStatus.REGISTERED,
                    blogId = null
                )
            )
            setup {
                givenAuthTokenUpdateResult(RequestResult.SUCCESS)
                given(selectedSite.connectionType).willReturn(SiteConnectionType.ApplicationPasswords)
                givenJetpackStatusFetchResult(
                    Result.success(FetchJetpackStatus.JetpackStatusFetchResponse.Success(jetpackStatus))
                )
            }

            val event = viewModel.event.runAndCaptureValues {
                viewModel.handleMagicLink("authToken", MagicLinkFlow.JetpackConnection)
            }.last()

            assertThat(event).isEqualTo(MagicLinkInterceptViewModel.ContinueJetpackActivation(jetpackStatus, site.url))
        }

    @Test
    fun `given jetpack connection, when fetching jetpack status fails, then show error message`() =
        testBlocking {
            setup {
                givenAuthTokenUpdateResult(RequestResult.SUCCESS)
                given(selectedSite.connectionType).willReturn(SiteConnectionType.ApplicationPasswords)
                givenJetpackStatusFetchResult(Result.failure(Exception()))
            }

            val event = viewModel.event.runAndCaptureValues {
                viewModel.handleMagicLink("authToken", MagicLinkFlow.JetpackConnection)
            }.last()

            assertThat(event).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.magic_link_fetch_account_error))
        }

    @Test
    fun `given jetpack connection, when fetching jetpack status is forbidden, then use default value for jetpack setup`() =
        testBlocking {
            setup {
                givenAuthTokenUpdateResult(RequestResult.SUCCESS)
                given(selectedSite.connectionType).willReturn(SiteConnectionType.ApplicationPasswords)
                givenJetpackStatusFetchResult(
                    Result.success(FetchJetpackStatus.JetpackStatusFetchResponse.ConnectionForbidden)
                )
            }

            val event = viewModel.event.runAndCaptureValues {
                viewModel.handleMagicLink("authToken", MagicLinkFlow.JetpackConnection)
            }.last()

            assertThat(event).isEqualTo(
                MagicLinkInterceptViewModel.ContinueJetpackActivation(
                    JetpackStatus(
                        isJetpackInstalled = false,
                        jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                            siteRegistrationStatus = JetpackSiteRegistrationStatus.NOT_REGISTERED,
                            blogId = null
                        )
                    ),
                    site.url
                )
            )
        }

    private suspend fun givenAuthTokenUpdateResult(result: RequestResult) {
        given(magicLinkInterceptRepository.updateMagicLinkAuthToken("authToken")).willReturn(result)
    }

    private suspend fun givenJetpackStatusFetchResult(result: Result<FetchJetpackStatus.JetpackStatusFetchResponse>) {
        given(fetchJetpackStatus.invoke(site, useApplicationPasswords = true)).willReturn(result)
    }
}
