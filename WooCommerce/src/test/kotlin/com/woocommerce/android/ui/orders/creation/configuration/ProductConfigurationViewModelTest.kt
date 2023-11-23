package com.woocommerce.android.ui.orders.creation.configuration

import com.google.gson.Gson
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.ui.orders.creation.GetProductRules
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class ProductConfigurationViewModelTest : BaseUnitTest() {
    private val defaultProductId = 123L
    private val defaultProductRules = ProductRules.Builder().apply {
        productType = ProductType.BUNDLE
    }.build()

    private val tracker: AnalyticsTrackerWrapper = mock()
    private val getProductRules: GetProductRules = mock {
        onBlocking { getRules(defaultProductId) } doReturn defaultProductRules
    }
    private val getProductConfiguration: GetProductConfiguration = GetProductConfiguration(mock(), Gson())
    private val getChildrenProductInfo: GetChildrenProductInfo = mock {
        onBlocking { invoke(defaultProductId) } doReturn mock()
    }
    private val navArgs = ProductConfigurationFragmentArgs(flow = Flow.Selection(defaultProductId))

    private lateinit var sut: ProductConfigurationViewModel

    @Before
    fun setup() {
        sut = ProductConfigurationViewModel(
            savedState = navArgs.initSavedStateHandle(),
            getProductRules = getProductRules,
            resourceProvider = mock(),
            getProductConfiguration = getProductConfiguration,
            tracker = tracker,
            getChildrenProductInfo = getChildrenProductInfo
        )
    }

    @Test
    fun `when save configuration is pressed then send the track event`() {
        sut.onSaveConfiguration()
        verify(tracker).track(AnalyticsEvent.ORDER_FORM_BUNDLE_PRODUCT_CONFIGURATION_SAVE_TAPPED)
    }

    @Test
    fun `when an optional configuration changed then send the track event`() {
        sut.onUpdateChildrenConfiguration(2L, OptionalRule.KEY, true.toString())
        verify(tracker).track(
            AnalyticsEvent.ORDER_FORM_BUNDLE_PRODUCT_CONFIGURATION_CHANGED,
            mapOf(AnalyticsTracker.KEY_CHANGED_FIELD to AnalyticsTracker.VALUE_CHANGED_FIELD_OPTIONAL)
        )
    }

    @Test
    fun `when a variation configuration changed then send the track event`() {
        sut.onUpdateChildrenConfiguration(2L, VariableProductRule.KEY, "this is a Json value")
        verify(tracker).track(
            AnalyticsEvent.ORDER_FORM_BUNDLE_PRODUCT_CONFIGURATION_CHANGED,
            mapOf(AnalyticsTracker.KEY_CHANGED_FIELD to AnalyticsTracker.VALUE_CHANGED_FIELD_VARIATION)
        )
    }

    @Test
    fun `when a quantity configuration changed then send the track event`() {
        sut.onUpdateChildrenConfiguration(2L, QuantityRule.KEY, "3")
        verify(tracker).track(
            AnalyticsEvent.ORDER_FORM_BUNDLE_PRODUCT_CONFIGURATION_CHANGED,
            mapOf(AnalyticsTracker.KEY_CHANGED_FIELD to AnalyticsTracker.VALUE_CHANGED_FIELD_QUANTITY)
        )
    }
}
