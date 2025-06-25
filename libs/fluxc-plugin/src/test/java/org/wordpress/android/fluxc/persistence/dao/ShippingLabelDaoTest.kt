package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.wc.shippinglabels.WCShippingLabelTestUtils

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ShippingLabelDaoTest {
    private lateinit var shippingLabelDao: ShippingLabelDao
    private lateinit var db: WCAndroidDatabase

    private val defaultSiteId = LocalId(6)
    private val defaultOrderId = RemoteId(12L)

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        shippingLabelDao = db.shippingLabelDao
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `test upsert and get shipping labels`() = runTest {
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
    fun `test get shipping label by id`() = runTest {
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
    fun `test update shipping labels`() = runTest {
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
    fun `test delete shipping labels`() = runTest {
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
