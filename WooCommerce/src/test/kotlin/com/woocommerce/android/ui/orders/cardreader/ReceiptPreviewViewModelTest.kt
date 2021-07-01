package com.woocommerce.android.ui.orders.cardreader

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.junit.Before
import org.wordpress.android.fluxc.model.SiteModel

class ReceiptPreviewViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ReceiptPreviewViewModel

    private val selectedSite: SelectedSite = mock()
    private val tracker: AnalyticsTrackerWrapper = mock()

    private val savedState: SavedStateHandle = ReceiptPreviewFragmentArgs(
        receiptUrl = "testing url",
        billingEmail = "testing email",
        orderId = 999L
    ).initSavedStateHandle()

    @Before
    fun setUp() {
        viewModel = ReceiptPreviewViewModel(savedState, tracker, selectedSite)
        whenever(selectedSite.get()).thenReturn(SiteModel().apply { name = "testName" })
    }
}
