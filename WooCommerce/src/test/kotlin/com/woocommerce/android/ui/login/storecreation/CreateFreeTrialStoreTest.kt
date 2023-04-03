package com.woocommerce.android.ui.login.storecreation

import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

internal class CreateFreeTrialStoreTest {
    private lateinit var sut: CreateFreeTrialStore
    private lateinit var storeCreationRepository: StoreCreationRepository

    @Before
    fun setUp() {
        createSut()
    }

    private fun createSut(
        siteDomain: String = "test domain",
        siteTitle: String = "test title",
        expectedCreationResult: StoreCreationResult<Long> = StoreCreationResult.Success(123)
    ) {
        val expectedSiteCreationData = StoreCreationRepository.SiteCreationData(
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
