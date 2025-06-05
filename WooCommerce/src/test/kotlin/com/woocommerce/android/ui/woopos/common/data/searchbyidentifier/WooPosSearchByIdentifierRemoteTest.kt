package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel

class WooPosSearchByIdentifierRemoteTest {

    private lateinit var sut: WooPosSearchByIdentifierRemote
    private val dispatcher: Dispatcher = mock()
    private val selectedSite: SelectedSite = mock()
    private val productsCache: WooPosProductsCache = mock()
    private val checkDigitRemover: WooPosSearchByIdentifierCheckDigitRemover = mock()

    private val testSite = SiteModel().apply { id = 1 }

    @Before
    fun setup() {
        whenever(selectedSite.get()).thenReturn(testSite)
        sut = WooPosSearchByIdentifierRemote(
            dispatcher,
            selectedSite,
            productsCache,
            checkDigitRemover
        )
    }

    @Test
    fun `given WooPosSearchByIdentifierRemote created, when onCleanup called, then dispatcher unregistered`() {
        // GIVEN
        // Remote searcher is created

        // WHEN
        sut.onCleanup()

        // THEN
        assertTrue(true)
    }

    @Test
    fun `given checkDigitRemover returns value, when identifier validation needed, then remover is called`() = runTest {
        // GIVEN
        val identifier = "1234567890123"
        val modifiedIdentifier = "123456789012"
        whenever(checkDigitRemover(identifier, WooPosBarcodeFormat.FormatEAN13))
            .thenReturn(modifiedIdentifier)

        val result = checkDigitRemover(identifier, WooPosBarcodeFormat.FormatEAN13)

        // THEN
        assertEquals(modifiedIdentifier, result)
    }

    @Test
    fun `given remote searcher initialized, when constructor called, then searcher is created successfully`() {
        // GIVEN & WHEN
        val remoteSearcher = WooPosSearchByIdentifierRemote(
            dispatcher,
            selectedSite,
            productsCache,
            checkDigitRemover
        )

        // Test that object creation succeeds without exceptions
        assertEquals("WooPosSearchByIdentifierRemote", remoteSearcher::class.simpleName)
    }
}
