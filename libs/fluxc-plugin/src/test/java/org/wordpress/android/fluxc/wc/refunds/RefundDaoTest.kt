package org.wordpress.android.fluxc.wc.refunds

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.persistence.dao.RefundDao

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class RefundDaoTest {
    private val orderId = 1L
    private lateinit var sut: RefundDao

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    @Before
    fun setUp() {
        sut = databaseRule.db.refundDao
    }

    @Test
    fun `test refund insert`(): Unit = runBlocking {
        val entity = REFUND_ENTITY.copy(data = "test-refund-data")
        sut.upsertRefund(entity)

        val entities = sut.getRefundsForOrder(TEST_SITE.localId(), RemoteId(orderId))
        assertThat(entities.first())
            .extracting({ it.data }, { it.refundId.value })
            .containsExactly("test-refund-data", 1L)
    }

    @Test
    fun `test refund update`(): Unit = runBlocking {
        val originalData = "original-data"
        val updatedData = "updated-data"

        val entity = REFUND_ENTITY.copy(data = originalData)
        sut.upsertRefund(entity)

        val retrievedEntity = sut.getRefund(TEST_SITE.localId(), RemoteId(orderId), RemoteId(1L))
        assertThat(retrievedEntity?.data).isEqualTo(originalData)

        val updatedEntity = REFUND_ENTITY.copy(data = updatedData)
        sut.upsertRefund(updatedEntity)

        val updatedRetrievedEntity = sut.getRefund(TEST_SITE.localId(), RemoteId(orderId), RemoteId(1L))
        assertThat(updatedRetrievedEntity?.data).isEqualTo(updatedData)
    }

    @Test
    fun `test select`(): Unit = runBlocking {
        val entity1 = REFUND_ENTITY.copy(data = "data-1")
        val entity2 = REFUND_ENTITY.copy(refundId = RemoteId(2L), data = "data-2")
        val entities = listOf(entity1, entity2)

        sut.upsertRefunds(entities)

        val retrievedEntities = sut.getRefundsForOrder(TEST_SITE.localId(), RemoteId(orderId))
        assertThat(retrievedEntities)
            .extracting<String> { it.data }
            .containsExactlyInAnyOrder("data-1", "data-2")

        val specificEntity = sut.getRefund(TEST_SITE.localId(), RemoteId(orderId), RemoteId(2L))
        assertThat(specificEntity?.data).isEqualTo("data-2")
    }

    @Test
    fun `test select empty result`(): Unit = runBlocking {
        val entity1 = REFUND_ENTITY.copy(data = "data-1")
        val entity2 = REFUND_ENTITY.copy(refundId = RemoteId(2L), data = "data-2")
        val entities = listOf(entity1, entity2)

        sut.upsertRefunds(entities)

        val emptyResults = sut.getRefundsForOrder(TEST_SITE.localId(), RemoteId(2L))
        assertThat(emptyResults).isEmpty()

        val nullResult = sut.getRefund(TEST_SITE.localId(), RemoteId(orderId), RemoteId(3L))
        assertThat(nullResult).isNull()
    }
}
