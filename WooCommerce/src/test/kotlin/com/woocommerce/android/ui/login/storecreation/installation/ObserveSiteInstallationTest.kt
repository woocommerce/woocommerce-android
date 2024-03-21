package com.woocommerce.android.ui.login.storecreation.installation

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
import kotlin.test.assertTrue

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
    fun `given site where site is ready to use and properties are in sync, when observation starts, then emit success`() =
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
            assertThat(installationStates.last()).isEqualTo(Success)
        }

    @Test
    fun `given site where site is not ready to use, when defined number of repetition is reached, then emit failure`() =
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
            assertThat(installationStates.last()).isEqualTo(
                Failure(
                    StoreCreationErrorType.STORE_NOT_READY
                )
            )
        }

    @Test
    fun `given site where site is ready to use but properties are out of sync, when observation starts, then emit out of sync event`() =
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
            assertTrue(installationStates.contains(OutOfSync))
        }

    @Test
    fun `given api that returns a failure, when observation starts, then return failure`() = testBlocking {
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
        assertThat(installationStates.last()).isEqualTo(
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
