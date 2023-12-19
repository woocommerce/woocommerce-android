package com.woocommerce.android.ui.login.storecreation

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.iap.pub.IAPActivityWrapper
import com.woocommerce.android.iap.pub.PurchaseWPComPlanActions
import com.woocommerce.android.iap.pub.model.WPComPlanProduct
import com.woocommerce.android.iap.pub.model.WPComProductResult
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.SITE_ADDRESS_ALREADY_EXISTS
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository.SiteCreationData
import com.woocommerce.android.ui.login.storecreation.iap.IsIAPEnabled
import com.woocommerce.android.ui.login.storecreation.plans.BillingPeriod.ECOMMERCE_MONTHLY
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.PlanInfo
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.CheckoutState
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.PlanState
import com.woocommerce.android.util.SiteIndependentCurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plans.full.Plan
import java.util.TimeZone
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class PlansViewModelTest : BaseUnitTest() {
    private val savedState = SavedStateHandle()
    private val repository: StoreCreationRepository = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val iapManager: PurchaseWPComPlanActions = mock()
    private val isIAPEnabled: IsIAPEnabled = mock()
    private val iapActivityWrapper: IAPActivityWrapper = mock()
    private val siteIndependentCurrencyFormatter: SiteIndependentCurrencyFormatter = mock()

    private lateinit var viewModel: PlansViewModel

    private var observer: Observer<ViewState> = mock()
    private val captor = argumentCaptor<ViewState>()

    companion object {
        private const val SITE_ID = 123L
        private const val EXPECTED_FORMATTED_PRICE = "$69.99"
    }

    private val siteData = SiteCreationData(
        segmentId = null,
        domain = "woocommerce.com",
        title = "WooCommerce"
    )

    private val plan = Plan(
        productId = 123,
        productName = "eCommerce",
        productShortName = "eCommerce short",
        productSlug = "ecommerce_slug",
        pathSlug = "ecommerce_path",
        billPeriod = 31,
        formattedPrice = "$99"
    )

    private val planInfo = PlanInfo(
        name = "eCommerce short",
        formattedPrice = "$99",
        billingPeriod = ECOMMERCE_MONTHLY,
        features = emptyList()
    )

    private val iapProduct = WPComPlanProduct(
        localizedTitle = "eCommerce",
        localizedDescription = "Description",
        price = 69.99,
        currency = "USD",
    )

    private fun whenViewModelIsCreated() {
        viewModel = PlansViewModel(
            savedState,
            analyticsTrackerWrapper,
            newStore,
            repository,
            iapManager,
            isIAPEnabled,
            siteIndependentCurrencyFormatter
        )
    }

    private val newStore = NewStore().also {
        it.update(
            name = siteData.title,
            domain = siteData.domain,
            siteId = SITE_ID,
            planPathSlug = plan.pathSlug,
            planProductId = plan.productId
        )
    }

    private val checkoutState = CheckoutState(
        startUrl = "${PlansViewModel.CART_URL}/${newStore.data.domain ?: newStore.data.siteId}",
        successTriggerKeyword = PlansViewModel.WEBVIEW_SUCCESS_TRIGGER_KEYWORD,
        exitTriggerKeyword = PlansViewModel.WEBVIEW_EXIT_TRIGGER_KEYWORD
    )

    @Test
    fun `when view model is created, eCommerce plan is fetched and information displayed`() = testBlocking {
        whenever(repository.fetchPlan(ECOMMERCE_MONTHLY)).thenReturn(plan)
        whenViewModelIsCreated()

        viewModel.viewState.observeForever(observer)
        verify(observer).onChanged(captor.capture())

        val observedPlan = captor.firstValue as? PlanState
        assertNotNull(observedPlan)

        assertEquals(planInfo.name, observedPlan.plan.name)
        assertEquals(planInfo.formattedPrice, observedPlan.plan.formattedPrice)
        assertEquals(planInfo.billingPeriod, observedPlan.plan.billingPeriod)
    }

    @Test
    fun `given IAP enabled, when view model is created, eCommerce plan is updated with IAP product info`() =
        testBlocking {
            givenIsIAPEnabled()
            whenever(
                siteIndependentCurrencyFormatter.formatAmountWithCurrency(
                    iapProduct.price,
                    iapProduct.currency
                )
            ).thenReturn(EXPECTED_FORMATTED_PRICE)
            whenever(repository.fetchPlan(ECOMMERCE_MONTHLY)).thenReturn(plan)
            whenever(iapManager.fetchWPComPlanProduct()).thenReturn(WPComProductResult.Success(iapProduct))

            whenViewModelIsCreated()

            viewModel.viewState.observeForever(observer)
            verify(observer).onChanged(captor.capture())

            val observedPlan = captor.firstValue as? PlanState
            assertNotNull(observedPlan)
            assertEquals(iapProduct.localizedTitle, observedPlan.plan.name)
            assertEquals(EXPECTED_FORMATTED_PRICE, observedPlan.plan.formattedPrice)
        }

    @Test
    fun `when a site is created, a plan is added to site's checkout cart and a WebView is launched`() =
        testBlocking {
            whenever(
                repository.createNewSite(
                    siteData = siteData,
                    languageWordPressId = PlansViewModel.NEW_SITE_LANGUAGE_ID,
                    timeZoneId = TimeZone.getDefault().id,
                    siteCreationFlow = newStore.data.planPathSlug
                )
            ).thenReturn(StoreCreationResult.Success(SITE_ID))

            whenever(repository.addPlanToCart(newStore.data.planProductId, newStore.data.planPathSlug, SITE_ID))
                .thenReturn(StoreCreationResult.Success(Unit))

            whenViewModelIsCreated()

            viewModel.onConfirmClicked(iapActivityWrapper)

            viewModel.viewState.observeForever(observer)
            verify(observer).onChanged(captor.capture())

            val observedState = captor.firstValue as? CheckoutState
            assertNotNull(observedState)

            assertEquals(observedState, checkoutState)
        }

    @Test
    fun `given IAP enabled, when a site is created, IAP flow is triggered and button set to loading`() =
        testBlocking {
            givenIsIAPEnabled()
            whenever(repository.fetchPlan(ECOMMERCE_MONTHLY)).thenReturn(plan)
            whenever(
                repository.createNewSite(
                    siteData = siteData,
                    languageWordPressId = PlansViewModel.NEW_SITE_LANGUAGE_ID,
                    timeZoneId = TimeZone.getDefault().id,
                    siteCreationFlow = newStore.data.planPathSlug
                )
            ).thenReturn(StoreCreationResult.Success(SITE_ID))

            whenViewModelIsCreated()
            viewModel.onConfirmClicked(iapActivityWrapper)
            viewModel.viewState.observeForever(observer)
            verify(observer).onChanged(captor.capture())

            val observedState = captor.firstValue as? PlanState
            assertNotNull(observedState)
            assertTrue(observedState.showMainButtonLoading)
            verify(iapManager).getPurchaseWpComPlanResult(SITE_ID)
            verify(iapManager).purchaseWPComPlan(iapActivityWrapper, SITE_ID)
        }

    @Test
    fun `when a site already exists and belongs to the user, the flow is successfully recovered`() =
        testBlocking {
            whenever(
                repository.createNewSite(
                    siteData = siteData,
                    languageWordPressId = PlansViewModel.NEW_SITE_LANGUAGE_ID,
                    timeZoneId = TimeZone.getDefault().id,
                    siteCreationFlow = newStore.data.planPathSlug
                )
            ).thenReturn(StoreCreationResult.Failure(SITE_ADDRESS_ALREADY_EXISTS))

            whenever(repository.getSiteByUrl(newStore.data.domain!!))
                .thenReturn(SiteModel().apply { siteId = SITE_ID })

            whenever(repository.addPlanToCart(newStore.data.planProductId, newStore.data.planPathSlug, SITE_ID))
                .thenReturn(StoreCreationResult.Success(Unit))

            whenViewModelIsCreated()

            viewModel.onConfirmClicked(iapActivityWrapper)

            viewModel.viewState.observeForever(observer)
            verify(observer).onChanged(captor.capture())

            val observedState = captor.firstValue as? CheckoutState
            assertNotNull(observedState)

            assertEquals(observedState, checkoutState)
        }

    @Test
    fun `when a site already exists and and it doesn't belong to the user, the flow fails`() =
        testBlocking {
            whenever(
                repository.createNewSite(
                    siteData = siteData,
                    languageWordPressId = PlansViewModel.NEW_SITE_LANGUAGE_ID,
                    timeZoneId = TimeZone.getDefault().id,
                    siteCreationFlow = newStore.data.planPathSlug
                )
            ).thenReturn(StoreCreationResult.Failure(SITE_ADDRESS_ALREADY_EXISTS))

            whenever(repository.getSiteByUrl(newStore.data.domain!!))
                .thenReturn(null)

            whenViewModelIsCreated()

            viewModel.onConfirmClicked(iapActivityWrapper)

            viewModel.viewState.observeForever(observer)
            verify(observer).onChanged(captor.capture())

            val observedState = captor.firstValue as? ErrorState
            assertNotNull(observedState)

            assertEquals(observedState.errorType, SITE_ADDRESS_ALREADY_EXISTS)
        }

    private fun givenIsIAPEnabled() {
        whenever(isIAPEnabled.invoke()).thenReturn(true)
    }
}
