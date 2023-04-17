package com.woocommerce.android.ui.login.storecreation.installation

import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult
import com.woocommerce.android.ui.login.storecreation.installation.InstallationConst.INITIAL_STORE_CREATION_DELAY
import com.woocommerce.android.ui.login.storecreation.installation.InstallationConst.SITE_CHECK_DEBOUNCE
import com.woocommerce.android.ui.login.storecreation.installation.InstallationConst.STORE_LOAD_RETRIES_LIMIT
import com.woocommerce.android.ui.login.storecreation.installation.ObserveSiteInstallation.InstallationState
import com.woocommerce.android.ui.login.storecreation.installation.ObserveSiteInstallation.InstallationState.Failure
import com.woocommerce.android.ui.login.storecreation.installation.ObserveSiteInstallation.InstallationState.OutOfSync
import com.woocommerce.android.ui.login.storecreation.installation.ObserveSiteInstallation.InstallationState.Success
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveSiteInstallationTest : BaseUnitTest(StandardTestDispatcher()) {

    lateinit var sut: ObserveSiteInstallation

    private val storeCreationRepository: StoreCreationRepository = mock()
    private val siteStore: SiteStore = mock()

    @Before
    fun setUp() {
        sut = ObserveSiteInstallation(
            storeCreationRepository,
            siteStore,
            coroutinesTestRule.testDispatchers,
        )
    }

    @Test
    fun `when jetpack is operational and properties are in sync, emit success`() =
        testBlocking {
            // given
            val installationStates = mutableListOf<InstallationState>()
            storeCreationRepository.stub {
                onBlocking { fetchSite(siteId) } doReturn StoreCreationResult.Success(Unit)
            }
            siteStore.stub {
                on { getSiteBySiteId(siteId) } doReturn validSite
            }

            // when
            backgroundScope.launch(StandardTestDispatcher()) {
                sut.invoke(siteId, expectedName).toList(installationStates)
            }
            advanceTimeBy(INITIAL_STORE_CREATION_DELAY)
            advanceUntilIdle()
            runCurrent()

            // then
            assertThat(installationStates.first()).isEqualTo(Success)
        }

    @Test
    fun `when jetpack is not operational after defined number of repeats, emit failure`() =
        testBlocking {
            // given
            val installationStates = mutableListOf<InstallationState>()
            storeCreationRepository.stub {
                onBlocking { fetchSite(siteId) } doReturn StoreCreationResult.Success(Unit)
            }
            siteStore.stub {
                on { getSiteBySiteId(siteId) } doReturn noJetpackSite
            }

            // when
            backgroundScope.launch(StandardTestDispatcher()) {
                sut.invoke(siteId, expectedName).toList(installationStates)
            }
            advanceTimeBy(INITIAL_STORE_CREATION_DELAY)
            advanceUntilIdle()
            runCurrent()

            repeat(STORE_LOAD_RETRIES_LIMIT) {
                advanceTimeBy(SITE_CHECK_DEBOUNCE)
                advanceUntilIdle()
                runCurrent()
            }

            // then
            assertThat(installationStates.first()).isEqualTo(
                Failure(
                    StoreCreationErrorType.STORE_NOT_READY
                )
            )
        }

    @Test
    fun `when jetpack is operational but properties are out of sync, emit out of sync event`() =
        testBlocking {
            // given
            val installationStates = mutableListOf<InstallationState>()
            storeCreationRepository.stub {
                onBlocking { fetchSite(siteId) } doReturn StoreCreationResult.Success(Unit)
            }
            siteStore.stub {
                on { getSiteBySiteId(siteId) } doReturn propertiesOutOfSyncSite
            }

            // when
            backgroundScope.launch(StandardTestDispatcher()) {
                sut.invoke(siteId, expectedName).toList(installationStates)
            }
            advanceTimeBy(INITIAL_STORE_CREATION_DELAY)
            advanceUntilIdle()
            runCurrent()

            // then
            assertThat(installationStates.first()).isEqualTo(
                OutOfSync
            )
        }

    @Test
    fun `when api returns a failure, return failure as well`() = testBlocking {
        // given
        val installationStates = mutableListOf<InstallationState>()
        storeCreationRepository.stub {
            onBlocking { fetchSite(siteId) } doReturn StoreCreationResult.Failure(
                StoreCreationErrorType.STORE_LOADING_FAILED
            )
        }

        // when
        backgroundScope.launch(StandardTestDispatcher()) {
            sut.invoke(siteId, expectedName).toList(installationStates)
        }
        advanceTimeBy(INITIAL_STORE_CREATION_DELAY)
        advanceUntilIdle()
        runCurrent()

        // then
        assertThat(installationStates.first()).isEqualTo(
            Failure(StoreCreationErrorType.STORE_LOADING_FAILED)
        )
    }

    companion object {
        const val siteId = 123L
        const val expectedName = "expected name"
        val validSite = SiteModel().apply {
            setIsJetpackInstalled(true)
            setIsJetpackConnected(true)
            setIsWpComStore(true)
            hasWooCommerce = true
            name = expectedName
        }
        val noJetpackSite = SiteModel().apply {
            setIsJetpackInstalled(false)
            setIsJetpackConnected(false)
        }
        val propertiesOutOfSyncSite = SiteModel().apply {
            setIsJetpackInstalled(true)
            setIsJetpackConnected(true)
            setIsWpComStore(false)
            hasWooCommerce = false
            name = expectedName + "out of sync"
        }
    }
}
