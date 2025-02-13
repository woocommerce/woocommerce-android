package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.entity.ShippingMethodEntity
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ShippingMethodDaoTest {
    private lateinit var shippingMethodDao: ShippingMethodDao
    private lateinit var db: WCAndroidDatabase

    private val defaultSiteId = LocalId(1)

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        shippingMethodDao = db.shippingMethodDao
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `test insert shipping methods`(): Unit = runBlocking {
        // when
        val shippingMethods = List(3) { id -> generateShippingMethodEntity("method$id") }
        shippingMethodDao.insertShippingMethods(shippingMethods)
        val observedShippingMethods =
            shippingMethodDao.observeShippingMethods(defaultSiteId).first()

        // then
        Assertions.assertThat(shippingMethods).isEqualTo(observedShippingMethods)
    }

    @Test
    fun `test get shipping method by id`(): Unit = runBlocking {
        // when
        val shippingMethodId = "successId"
        val notFoundId = "notFoundId"
        val shippingMethods = listOf(generateShippingMethodEntity(id = shippingMethodId))
        shippingMethodDao.insertShippingMethods(shippingMethods)
        val success = shippingMethodDao.getShippingMethodById(defaultSiteId, shippingMethodId)
        val fail = shippingMethodDao.getShippingMethodById(defaultSiteId, notFoundId)

        // then
        Assertions.assertThat(success).isNotNull
        Assertions.assertThat(success!!.id).isEqualTo(shippingMethodId)
        Assertions.assertThat(fail).isNull()
    }

    @Test
    fun `test update shipping methods`(): Unit = runBlocking {
        // when
        val startShippingMethods = List(3) { id ->
            generateShippingMethodEntity("method$id")
        }
        val updatedShippingMethods = List(3) { id ->
            generateShippingMethodEntity("updatedMethod$id")
        }
        shippingMethodDao.insertShippingMethods(startShippingMethods)
        shippingMethodDao.updateShippingMethods(updatedShippingMethods)
        val observedShippingMethods =
            shippingMethodDao.observeShippingMethods(defaultSiteId).first()
        // then
        Assertions.assertThat(updatedShippingMethods).isEqualTo(observedShippingMethods)
    }

    private fun generateShippingMethodEntity(
        id: String,
        siteId: LocalId = defaultSiteId,
        title: String = "title for $id"
    ): ShippingMethodEntity = ShippingMethodEntity(
        localSiteId = siteId,
        id = id,
        title = title
    )
}
