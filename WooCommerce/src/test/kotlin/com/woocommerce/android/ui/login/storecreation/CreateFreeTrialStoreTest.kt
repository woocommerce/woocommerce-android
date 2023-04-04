package com.woocommerce.android.ui.login.storecreation

import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Failed
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository.SiteCreationData
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
internal class CreateFreeTrialStoreTest : BaseUnitTest() {
    private lateinit var sut: CreateFreeTrialStore
    private lateinit var storeCreationRepository: StoreCreationRepository

    @Test
    fun `when createFreeTrialSite is called correctly, then free trial store creation starts`() = testBlocking {
        // Given
        val jobs = mutableListOf<Job>()
        var lastCreationState: StoreCreationState? = null
        var receivedSiteID: Long? = null

        val siteDomain = "test domain"
        val siteTitle = "test title"
        val expectedCreationResult = StoreCreationResult.Success(123L)
        val expectedSiteCreationData = SiteCreationData(
            siteDesign = PlansViewModel.NEW_SITE_THEME,
            domain = siteDomain,
            title = siteTitle,
            segmentId = null
        )
        createSut(siteDomain, siteTitle, expectedCreationResult)
        sut.state
            .onEach { lastCreationState = it }
            .launchIn(this)
            .also { jobs.add(it) }

        // When
        sut(siteDomain, siteTitle)
            .onEach { receivedSiteID = it }
            .launchIn(this)
            .also { jobs.add(it) }

        advanceUntilIdle()

        // Then
        verify(storeCreationRepository).createNewFreeTrialSite(
            eq(expectedSiteCreationData),
            eq(PlansViewModel.NEW_SITE_LANGUAGE_ID),
            any()
        )
        assertThat(lastCreationState).isEqualTo(StoreCreationState.Finished)
        assertThat(receivedSiteID).isEqualTo(123L)

        jobs.forEach { it.cancel() }
    }

    @Test
    fun `when createFreeTrialSite fails, then the error is returned`() = testBlocking {
        // Given
        val jobs = mutableListOf<Job>()
        var lastCreationState: StoreCreationState? = null
        var receivedSiteID: Long? = null

        val siteDomain = "test domain"
        val siteTitle = "test title"
        val expectedCreationResult = StoreCreationResult.Failure<Long>(
            StoreCreationErrorType.FREE_TRIAL_ASSIGNMENT_FAILED
        )
        val expectedSiteCreationData = SiteCreationData(
            siteDesign = PlansViewModel.NEW_SITE_THEME,
            domain = siteDomain,
            title = siteTitle,
            segmentId = null
        )

        createSut(siteDomain, siteTitle, expectedCreationResult)
        sut.state
            .onEach { lastCreationState = it }
            .launchIn(this)
            .also { jobs.add(it) }

        // When
        sut(siteDomain, siteTitle)
            .onEach { receivedSiteID = it }
            .launchIn(this)
            .also { jobs.add(it) }

        advanceUntilIdle()

        // Then
        verify(storeCreationRepository).createNewFreeTrialSite(
            eq(expectedSiteCreationData),
            eq(PlansViewModel.NEW_SITE_LANGUAGE_ID),
            any()
        )
        assertThat(lastCreationState).isEqualTo(
            Failed(StoreCreationErrorType.FREE_TRIAL_ASSIGNMENT_FAILED)
        )
        assertThat(receivedSiteID).isNull()

        jobs.forEach { it.cancel() }
    }

    @Test
    fun `when createFreeTrialSite fails with SITE_ADDRESS_ALREADY_EXISTS, then the site is retrieved`() = testBlocking {
        // Given
        val jobs = mutableListOf<Job>()
        var lastCreationState: StoreCreationState? = null
        var receivedSiteID: Long? = null

        val siteDomain = "test existent domain"
        val siteTitle = "test existent title"
        val expectedSiteCreationData = SiteCreationData(
            siteDesign = PlansViewModel.NEW_SITE_THEME,
            domain = siteDomain,
            title = siteTitle,
            segmentId = null
        )

        createSut(
            siteDomain,
            siteTitle,
            StoreCreationResult.Failure(StoreCreationErrorType.SITE_ADDRESS_ALREADY_EXISTS)
        )
        sut.state
            .onEach { lastCreationState = it }
            .launchIn(this)
            .also { jobs.add(it) }

        // When
        sut(siteDomain, siteTitle)
            .onEach { receivedSiteID = it }
            .launchIn(this)
            .also { jobs.add(it) }

        advanceUntilIdle()

        // Then
        verify(storeCreationRepository).createNewFreeTrialSite(
            eq(expectedSiteCreationData),
            eq(PlansViewModel.NEW_SITE_LANGUAGE_ID),
            any()
        )
        verify(storeCreationRepository).getSiteByUrl(siteDomain)
        assertThat(lastCreationState).isEqualTo(StoreCreationState.Finished)
        assertThat(receivedSiteID).isEqualTo(123L)

        jobs.forEach { it.cancel() }
    }

    private fun createSut(
        siteDomain: String,
        siteTitle: String,
        expectedCreationResult: StoreCreationResult<Long> = StoreCreationResult.Success(123)
    ) {
        val expectedSiteCreationData = SiteCreationData(
            siteDesign = PlansViewModel.NEW_SITE_THEME,
            domain = siteDomain,
            title = siteTitle,
            segmentId = null
        )

        val siteModel = SiteModel().apply { siteId = 123 }

        storeCreationRepository = mock {
            onBlocking {
                createNewFreeTrialSite(
                    eq(expectedSiteCreationData),
                    eq(PlansViewModel.NEW_SITE_LANGUAGE_ID),
                    any()
                )
            } doReturn expectedCreationResult

            onBlocking { getSiteByUrl(expectedSiteCreationData.domain) } doReturn siteModel
        }

        sut = CreateFreeTrialStore(storeCreationRepository)
    }
}
