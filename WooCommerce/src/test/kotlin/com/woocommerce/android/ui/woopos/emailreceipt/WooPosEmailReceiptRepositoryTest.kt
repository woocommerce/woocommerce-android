package com.woocommerce.android.ui.woopos.emailreceipt

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import java.util.regex.Pattern

class WooPosEmailReceiptRepositoryTest {
    private val siteModel: SiteModel = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(siteModel)
    }
    private val orderStore: WCOrderStore = mock()
    private val getWooCoreVersion: GetWooCorePluginCachedVersion = mock()
    private val provideEmailPattern: WooPosEmailReceiptRepository.WooPosProvideEmailPattern = mock {
        on { invoke() }.thenReturn(
            Pattern.compile(
                "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
            )
        )
    }

    private val repository = WooPosEmailReceiptRepository(
        selectedSite,
        orderStore,
        provideEmailPattern,
        getWooCoreVersion
    )

    @Test
    fun `given valid email, when isEmailValid, then return true`() {
        // GIVEN
        val validEmail = "test@example.com"

        // WHEN
        val result = repository.isEmailValid(validEmail)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given invalid email, when isEmailValid, then return false`() {
        // GIVEN
        val invalidEmail = "invalid-email"

        // WHEN
        val result = repository.isEmailValid(invalidEmail)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given valid orderId and email, when WC plugin version is lower than 10, then sendReceiptByEmail returns success`() = runTest {
        // GIVEN
        val orderId = 1L
        val email = "test@example.com"

        whenever(getWooCoreVersion.invoke()).thenReturn("9.9.0")
        whenever(orderStore.updateOrderBillingEmail(siteModel, orderId, email)).thenReturn(WooPayload(Unit))
        whenever(orderStore.sendOrderReceipt(siteModel, orderId)).thenReturn(WooPayload(Unit))

        // WHEN
        val result = repository.sendReceiptByEmail(orderId, email)

        // THEN
        assertThat(result.isSuccess).isTrue()
        verify(orderStore).updateOrderBillingEmail(siteModel, orderId, email)
        verify(orderStore).sendOrderReceipt(siteModel, orderId)
    }

    @Test
    fun `given WC version 10 or higher, when sendReceiptByEmail, then sends POS receipt with explicit templateId`() = runTest {
        // GIVEN
        val orderId = 1L
        val email = "test@example.com"

        whenever(getWooCoreVersion.invoke()).thenReturn("10.0.0")
        whenever(
            orderStore.sendOrderPOSSpecificReceipt(siteModel, orderId, email, true, "customer_pos_completed_order")
        ).thenReturn(WooPayload(Unit))

        // WHEN
        val result = repository.sendReceiptByEmail(orderId, email)

        // THEN
        assertThat(result.isSuccess).isTrue()
        verify(orderStore).sendOrderPOSSpecificReceipt(siteModel, orderId, email, true, "customer_pos_completed_order")
        verify(orderStore, never()).updateOrderBillingEmail(siteModel, orderId, email)
    }

    @Test
    fun `given legacy WC version and email update fails, when sendReceiptByEmail, then return failure`() = runTest {
        // GIVEN
        val email = "test@example.com"
        val orderId = 1L

        whenever(getWooCoreVersion.invoke()).thenReturn("9.9.0")
        whenever(orderStore.updateOrderBillingEmail(siteModel, orderId, email)).thenReturn(
            WooPayload(WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.NETWORK_ERROR))
        )

        // WHEN
        val result = repository.sendReceiptByEmail(orderId, email)

        // THEN
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `given WC 10 or higher and POS receipt fails, when sendReceiptByEmail, then return failure`() = runTest {
        // GIVEN
        val orderId = 1L
        val email = "test@example.com"

        whenever(getWooCoreVersion.invoke()).thenReturn("10.0.0")
        whenever(
            orderStore.sendOrderPOSSpecificReceipt(siteModel, orderId, email, true, "customer_pos_completed_order")
        ).thenReturn(
            WooPayload(WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.TIMEOUT))
        )

        // WHEN
        val result = repository.sendReceiptByEmail(orderId, email)

        // THEN
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `given receipt sending fails, when sendReceiptByEmail, then return failure`() = runTest {
        // GIVEN
        val orderId = 1L
        val email = "test@example.com"

        whenever(getWooCoreVersion.invoke()).thenReturn("9.9.0")
        whenever(orderStore.updateOrderBillingEmail(siteModel, orderId, email)).thenReturn(WooPayload(Unit))
        whenever(orderStore.sendOrderReceipt(siteModel, orderId)).thenReturn(
            WooPayload(WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.TIMEOUT))
        )

        // WHEN
        val result = repository.sendReceiptByEmail(orderId, email)

        // THEN
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `given WC 10_7 or higher, when sendReceiptByEmail, then sends POS receipt without templateId`() = runTest {
        // GIVEN
        val orderId = 1L
        val email = "test@example.com"

        whenever(getWooCoreVersion.invoke()).thenReturn("10.7.0")
        whenever(orderStore.sendOrderPOSSpecificReceipt(siteModel, orderId, email, true, null)).thenReturn(
            WooPayload(Unit)
        )

        // WHEN
        val result = repository.sendReceiptByEmail(orderId, email)

        // THEN
        assertThat(result.isSuccess).isTrue()
        verify(orderStore).sendOrderPOSSpecificReceipt(siteModel, orderId, email, true, null)
    }

    @Test
    fun `given WC 11_0, when sendReceiptByEmail, then sends POS receipt without templateId`() = runTest {
        // GIVEN
        val orderId = 1L
        val email = "test@example.com"

        whenever(getWooCoreVersion.invoke()).thenReturn("11.0.0")
        whenever(orderStore.sendOrderPOSSpecificReceipt(siteModel, orderId, email, true, null)).thenReturn(
            WooPayload(Unit)
        )

        // WHEN
        val result = repository.sendReceiptByEmail(orderId, email)

        // THEN
        assertThat(result.isSuccess).isTrue()
        verify(orderStore).sendOrderPOSSpecificReceipt(siteModel, orderId, email, true, null)
    }

    @Test
    fun `given order with billing email, when getBillingEmail, then return email`() = runTest {
        // GIVEN
        val orderId = 1L
        val orderEntity: OrderEntity = mock {
            on { billingEmail }.thenReturn("customer@example.com")
        }
        whenever(orderStore.getOrderByIdAndSite(orderId, siteModel)).thenReturn(orderEntity)

        // WHEN
        val result = repository.getBillingEmail(orderId)

        // THEN
        assertThat(result).isEqualTo("customer@example.com")
    }

    @Test
    fun `given order with blank billing email, when getBillingEmail, then return null`() = runTest {
        // GIVEN
        val orderId = 1L
        val orderEntity: OrderEntity = mock {
            on { billingEmail }.thenReturn("")
        }
        whenever(orderStore.getOrderByIdAndSite(orderId, siteModel)).thenReturn(orderEntity)

        // WHEN
        val result = repository.getBillingEmail(orderId)

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `given no order found, when getBillingEmail, then return null`() = runTest {
        // GIVEN
        val orderId = 1L
        whenever(orderStore.getOrderByIdAndSite(orderId, siteModel)).thenReturn(null)

        // WHEN
        val result = repository.getBillingEmail(orderId)

        // THEN
        assertThat(result).isNull()
    }
}
