package org.wordpress.android.fluxc.persistence.dao

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.wc.shippinglabels.WCShippingLabelTestUtils

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ShippingLabelDaoTest {

    private lateinit var shippingLabelDao: ShippingLabelDao

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    private val defaultSiteId = LocalId(6)
    private val defaultOrderId = RemoteId(12L)

    @Before
    fun setUp() {
        shippingLabelDao = databaseRule.db.shippingLabelDao
    }

    @Test
    fun `when shipping labels are upserted, then they can be retrieved`() = runTest {
        // given
        val shippingLabels = List(3) { id ->
            WCShippingLabelTestUtils.generateSampleShippingLabel(
                remoteId = id.toLong(),
                siteId = defaultSiteId.value,
                orderId = defaultOrderId.value
            )
        }
        shippingLabelDao.upsertShippingLabels(shippingLabels)

        // when
        val result = shippingLabelDao.getShippingLabels(defaultSiteId, defaultOrderId)

        // then
        assertThat(result).containsExactlyInAnyOrderElementsOf(shippingLabels)
    }

    @Test
    fun `when shipping label exists, then it can be retrieved by id`() = runTest {
        // given
        val labelId = 123L
        val shippingLabel = WCShippingLabelTestUtils.generateSampleShippingLabel(
            remoteId = labelId,
            siteId = defaultSiteId.value,
            orderId = defaultOrderId.value
        )
        shippingLabelDao.upsertShippingLabels(listOf(shippingLabel))

        // when
        val retrievedLabel = shippingLabelDao.getShippingLabel(
            defaultSiteId,
            defaultOrderId,
            RemoteId(labelId)
        )
        val nonExistentLabel = shippingLabelDao.getShippingLabel(
            defaultSiteId,
            defaultOrderId,
            RemoteId(999L)
        )

        // then
        assertThat(retrievedLabel).isEqualTo(shippingLabel)
        assertThat(nonExistentLabel).isNull()
    }

    @Test
    fun `when shipping labels are updated, then changes are persisted`() = runTest {
        // given
        val shippingLabels = List(3) { id ->
            WCShippingLabelTestUtils.generateSampleShippingLabel(
                remoteId = id.toLong(),
                siteId = defaultSiteId.value,
                orderId = defaultOrderId.value
            )
        }
        shippingLabelDao.upsertShippingLabels(shippingLabels)

        // when
        val updatedLabels = shippingLabels.map { label ->
            WCShippingLabelTestUtils.generateSampleShippingLabel(
                remoteId = label.remoteShippingLabelId.value,
                siteId = label.localSiteId.value,
                orderId = label.remoteOrderId.value,
                status = "UPDATED_STATUS",
                serviceName = "UPDATED_SERVICE"
            )
        }
        shippingLabelDao.upsertShippingLabels(updatedLabels)
        val retrievedLabels = shippingLabelDao.getShippingLabels(defaultSiteId, defaultOrderId)

        // then
        assertThat(retrievedLabels).containsExactlyElementsOf(updatedLabels)
    }

    @Test
    fun `when shipping labels are deleted, then they are removed from database`() = runTest {
        // given
        val shippingLabels = List(3) { id ->
            WCShippingLabelTestUtils.generateSampleShippingLabel(
                remoteId = id.toLong(),
                siteId = defaultSiteId.value,
                orderId = defaultOrderId.value
            )
        }
        shippingLabelDao.upsertShippingLabels(shippingLabels)

        // when
        shippingLabelDao.deleteShippingLabels(defaultOrderId)
        val retrievedLabels = shippingLabelDao.getShippingLabels(defaultSiteId, defaultOrderId)

        // then
        assertThat(retrievedLabels).isEmpty()
    }
}
