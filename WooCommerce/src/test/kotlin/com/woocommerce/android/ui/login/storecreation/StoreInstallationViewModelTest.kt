package com.woocommerce.android.ui.login.storecreation

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.STORE_LOADING_FAILED
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.STORE_NOT_READY
import com.woocommerce.android.ui.login.storecreation.installation.InstallationTransactionLauncher
import com.woocommerce.android.ui.login.storecreation.installation.ObserveSiteInstallation
import com.woocommerce.android.ui.login.storecreation.installation.StoreInstallationLoadingTimer
import com.woocommerce.android.ui.login.storecreation.installation.StoreInstallationViewModel
import com.woocommerce.android.ui.login.storecreation.installation.StoreInstallationViewModel.ViewState
import com.woocommerce.android.ui.login.storecreation.installation.StoreInstallationViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.login.storecreation.installation.StoreInstallationViewModel.ViewState.StoreCreationLoadingState
import com.woocommerce.android.ui.login.storecreation.installation.StoreInstallationViewModel.ViewState.SuccessState
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class StoreInstallationViewModelTest : BaseUnitTest() {
    private val savedState = SavedStateHandle()
    private val repository: StoreCreationRepository = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val selectedSite: SelectedSite = mock()
    private val storeInstallationLoadingTimer: StoreInstallationLoadingTimer = mock()
    private val installationTransactionLauncher: InstallationTransactionLauncher = mock()
    private val observeSiteInstallation: ObserveSiteInstallation = mock()

    private lateinit var viewModel: StoreInstallationViewModel

    private companion object {
        const val SITE_ID = 123L
        val INITIAL_LOADING_STATE = StoreCreationLoadingState(
            progress = 0F,
            title = R.string.store_creation_in_progress_title_1,
            description = R.string.store_creation_in_progress_description_1,
            image = R.drawable.store_creation_loading_almost_there
        )
    }

    private fun whenViewModelIsCreated() {
        viewModel = StoreInstallationViewModel(
            savedState,
            repository,
            newStore,
            analyticsTrackerWrapper,
            appPrefsWrapper,
            selectedSite,
            storeInstallationLoadingTimer,
            installationTransactionLauncher,
            observeSiteInstallation
        )
        viewModel.viewState.observeForever {}
    }

    private val newStore = NewStore().also {
        it.update(
            name = "eCommerce",
            domain = "woowoozela.com",
            siteId = SITE_ID,
            planPathSlug = "ecommerce_bundle",
            planProductId = 321
        )
    }

    @Before
    fun setup() {
        whenever(storeInstallationLoadingTimer.observe()).thenReturn(flowOf(INITIAL_LOADING_STATE))
    }

    @Test
    fun `when a Woo site is found after installation, a success state is displayed and loading canceled`() =
        testBlocking {
            whenever(selectedSite.get()).thenReturn(
                SiteModel().apply {
                    url = newStore.data.domain
                }
            )
            observeSiteInstallation.stub {
                onBlocking {
                    invoke(
                        any(),
                        any(),
                        any()
                    )
                }.thenReturn(flowOf(ObserveSiteInstallation.InstallationState.Success))
            }

            whenViewModelIsCreated()
            advanceUntilIdle()

            verify(storeInstallationLoadingTimer).resetTimer()
            val expectedState = SuccessState(newStore.data.domain!!.slashJoin("wp-admin/"))
            observeState { lastState ->
                assertEquals(expectedState, lastState)
            }
        }

    @Test
    fun `when a site is found but not ready after 10 tries, an error state is displayed`() =
        testBlocking {
            observeSiteInstallation.stub {
                onBlocking {
                    invoke(
                        any(),
                        any(),
                        any()
                    )
                }.thenReturn(
                    flowOf(
                        ObserveSiteInstallation.InstallationState.Failure(
                            STORE_NOT_READY
                        )
                    )
                )
            }

            whenViewModelIsCreated()
            advanceUntilIdle()

            val expectedState = ErrorState(STORE_NOT_READY)
            observeState { lastState ->
                assertEquals(expectedState, lastState)
            }
            verify(storeInstallationLoadingTimer).resetTimer()
            verify(analyticsTrackerWrapper).track(AnalyticsEvent.SITE_CREATION_TIMED_OUT)
        }

    @Test
    fun `when a site fetching returns an error, show error state`() = testBlocking {
        observeSiteInstallation.stub {
            onBlocking {
                invoke(
                    any(),
                    any(),
                    any()
                )
            }.thenReturn(
                flowOf(
                    ObserveSiteInstallation.InstallationState.Failure(
                        STORE_LOADING_FAILED
                    )
                )
            )
        }

        whenViewModelIsCreated()
        advanceUntilIdle()

        val expectedState = ErrorState(STORE_LOADING_FAILED)
        observeState { lastState ->
            assertEquals(expectedState, lastState)
        }
        verify(storeInstallationLoadingTimer).resetTimer()
    }

    @Test
    fun `when viewmodel is created, loading timer is initiated`() =
        testBlocking {
            whenViewModelIsCreated()

            verify(storeInstallationLoadingTimer).startTimer()
        }

    @Test
    fun `when a site is during installation, start measuring the transaction time`() =
        testBlocking {
            whenViewModelIsCreated()

            verify(installationTransactionLauncher).onStoreInstallationRequested()
        }

    @Test
    fun `when a site is after successful installation, finish measuring transaction time`() =
        testBlocking {
            // given
            observeSiteInstallation.stub {
                onBlocking {
                    invoke(
                        any(),
                        any(),
                        any()
                    )
                }.thenReturn(flowOf(ObserveSiteInstallation.InstallationState.Success))
            }
            whenever(selectedSite.get()).thenReturn(
                SiteModel().apply {
                    url = newStore.data.domain
                }
            )

            // when
            whenViewModelIsCreated()
            advanceUntilIdle()

            // then
            verify(installationTransactionLauncher).onStoreInstalled(
                mapOf(
                    AnalyticsTracker.KEY_SOURCE to null,
                    AnalyticsTracker.KEY_URL to newStore.data.domain,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_NATIVE,
                    AnalyticsTracker.KEY_IS_FREE_TRIAL to true
                )
            )
        }

    @Test
    fun `when a site is not successfully installed, abandon performance transaction`() =
        testBlocking {
            // given
            observeSiteInstallation.stub {
                onBlocking {
                    invoke(
                        any(),
                        any(),
                        any()
                    )
                }.thenReturn(
                    flowOf(
                        ObserveSiteInstallation.InstallationState.Failure(
                            STORE_NOT_READY
                        )
                    )
                )
            }

            // when
            whenViewModelIsCreated()
            advanceUntilIdle()

            // then
            verify(installationTransactionLauncher).onStoreInstallationFailed()
        }

    @Test
    fun `when a Woo site is found after installation, but has out-of-sync properties, report a tracks event and do it only once`() =
        testBlocking {
            val installationStateEmitter =
                MutableSharedFlow<ObserveSiteInstallation.InstallationState>()
            observeSiteInstallation.stub {
                onBlocking {
                    invoke(
                        any(),
                        any(),
                        any()
                    )
                }.thenReturn(installationStateEmitter)
            }

            whenViewModelIsCreated()
            advanceUntilIdle()
            repeat(2) {
                installationStateEmitter.emit(ObserveSiteInstallation.InstallationState.OutOfSync)
            }

            verify(analyticsTrackerWrapper).track(AnalyticsEvent.SITE_CREATION_PROPERTIES_OUT_OF_SYNC)
        }

    private fun observeState(check: (ViewState) -> Unit) =
        viewModel.viewState.observeForever { check(it) }
}
