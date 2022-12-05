package com.woocommerce.android.ui.login.storecreation.webview

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository
import com.woocommerce.android.ui.login.storecreation.plans.BillingPeriod.ECOMMERCE_MONTHLY
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.PlanInfo
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.LoadingState
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.PlanState
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.plans.full.Plan
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class PlansViewModelTest : BaseUnitTest() {
    private val savedState = SavedStateHandle()
    private val repository: StoreCreationRepository = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private lateinit var viewModel: PlansViewModel

    private var observer: Observer<ViewState> = mock()
    private val captor = argumentCaptor<ViewState>()

    private val newStore = NewStore()
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

    private fun whenViewModelIsCreated() {
        viewModel = PlansViewModel(
            savedState,
            analyticsTrackerWrapper,
            newStore,
            repository
        )
    }

    @Test
    fun `when view model is created, eCommerce plan is fetched and information displayed`() = testBlocking {
        whenever(repository.fetchPlan(ECOMMERCE_MONTHLY)).thenReturn(plan)

        whenViewModelIsCreated()

        viewModel.viewState.observeForever(observer)
        verify(observer).onChanged(captor.capture())

        val observedPlan = captor.firstValue as PlanState
        assertNotNull(observedPlan)

        assertEquals(planInfo.name, observedPlan.plan.name)
        assertEquals(planInfo.formattedPrice, observedPlan.plan.formattedPrice)
        assertEquals(planInfo.billingPeriod, observedPlan.plan.billingPeriod)
    }
}
