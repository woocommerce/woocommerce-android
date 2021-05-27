package com.woocommerce.android.util.receipts

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import org.junit.Before
import org.mockito.ArgumentMatchers.anyInt
import org.wordpress.android.fluxc.model.SiteModel

class ReceiptDataMapperTest : BaseUnitTest() {
    private lateinit var receiptDataMapper: ReceiptDataMapper
    private val resourceProvider: ResourceProvider = mock()
    private val selectedSite: SelectedSite = mock()
    private val mockedSite: SiteModel = mock()

    @Before
    fun setUp() {
        receiptDataMapper = ReceiptDataMapper(resourceProvider, selectedSite)
        whenever(resourceProvider.getString(anyInt())).thenReturn("test")
        whenever(selectedSite.get()).thenReturn(mockedSite)
    }
}
