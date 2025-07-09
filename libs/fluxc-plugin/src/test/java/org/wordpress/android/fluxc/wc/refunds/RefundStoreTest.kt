package org.wordpress.android.fluxc.wc.refunds

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.RefundMapper
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.converters.CurrencyPositionConverter
import org.wordpress.android.fluxc.persistence.dao.RefundDao
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WCRefundStore.Companion.DEFAULT_PAGE
import org.wordpress.android.fluxc.store.WCRefundStore.Companion.DEFAULT_PAGE_SIZE
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import java.io.IOException

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class RefundStoreTest {
    private val restClient = mock<RefundRestClient>()
    private val site = SiteModel()
    private val mapper = RefundMapper()
    private lateinit var store: WCRefundStore
    private lateinit var db: WCAndroidDatabase
    private lateinit var refundDao: RefundDao

    private val orderId = 1L
    private val refundId = REFUND_RESPONSE.refundId
    private val error = WooError(
        WooErrorType.INVALID_ID,
        BaseRequest.GenericErrorType.NOT_FOUND,
        "Invalid order ID"
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().context
        db = Room.inMemoryDatabaseBuilder(
            context, WCAndroidDatabase::class.java
        ).addTypeConverter(CurrencyPositionConverter(Mockito.mock()))
            .allowMainThreadQueries().build()
        refundDao = db.refundDao

        store = WCRefundStore(
            restClient,
            initCoroutineEngine(),
            mapper,
            refundDao,
            Gson()
        )
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `fetch all refunds of an order`() = test {
        val data = arrayOf(REFUND_RESPONSE, REFUND_RESPONSE)
        val result = fetchAllTestRefunds()

        assertThat(result.model?.size).isEqualTo(data.size)
        assertThat(result.model?.first()).isEqualTo(mapper.map(data.first()))

        val invalidRequestResult = store.fetchAllRefunds(site, 2)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    @Test
    fun `get all refunds of an order`() = test {
        fetchAllTestRefunds()

        val refunds = store.getAllRefunds(site, orderId)

        assertThat(refunds.size).isEqualTo(1)
        assertThat(refunds.first()).isEqualTo(mapper.map(REFUND_RESPONSE))

        val invalidRequestResult = store.getAllRefunds(site, 2)
        assertThat(invalidRequestResult.size).isEqualTo(0)
    }

    @Test
    fun `fetch specific refund`() = test {
        val refund = fetchSpecificTestRefund()

        assertThat(refund.model).isEqualTo(mapper.map(REFUND_RESPONSE))
    }

    @Test
    fun `get specific refund`() = test {
        fetchSpecificTestRefund()
        val refund = store.getRefund(site, orderId, refundId)

        assertThat(refund).isEqualTo(mapper.map(REFUND_RESPONSE))
    }

    private suspend fun fetchSpecificTestRefund(): WooResult<WCRefundModel> {
        val fetchRefundsPayload = WooPayload(
            REFUND_RESPONSE
        )
        whenever(restClient.fetchRefund(site, orderId, refundId)).thenReturn(
                fetchRefundsPayload
        )

        whenever(restClient.fetchRefund(site, 2, refundId)).thenReturn(
            WooPayload(error)
        )
        return store.fetchRefund(site, orderId, refundId)
    }

    private suspend fun fetchAllTestRefunds(): WooResult<List<WCRefundModel>> {
        val data = arrayOf(REFUND_RESPONSE, REFUND_RESPONSE)
        val fetchRefundsPayload = WooPayload(
            data
        )
        whenever(
            restClient.fetchAllRefunds(
                site,
                orderId,
                DEFAULT_PAGE,
                DEFAULT_PAGE_SIZE
            )
        ).thenReturn(
                fetchRefundsPayload
        )
        whenever(
            restClient.fetchAllRefunds(
                site,
                2,
                DEFAULT_PAGE,
                DEFAULT_PAGE_SIZE
            )
        ).thenReturn(
            WooPayload(error)
        )
        return store.fetchAllRefunds(site, orderId)
    }
}
