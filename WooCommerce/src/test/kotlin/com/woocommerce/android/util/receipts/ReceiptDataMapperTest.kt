package com.woocommerce.android.util.receipts

import com.nhaarman.mockitokotlin2.mock
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import org.junit.Before
import org.junit.Test

class ReceiptDataMapperTest : BaseUnitTest() {
    private lateinit var receiptDataMapper: ReceiptDataMapper
    private val resourceProvider: ResourceProvider = mock()
    private val selectedSite: SelectedSite = mock()

    @Before
    fun setUp() {
        receiptDataMapper = ReceiptDataMapper(resourceProvider, selectedSite)
    }

    @Test
    fun foo() {
    }
}
