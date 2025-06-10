package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class CustomerDaoTest {
    private lateinit var sut: CustomerDao
    private lateinit var database: WCAndroidDatabase
    private val site = SiteModel().apply {
        email = "test@example.org"
        name = "Test Site"
        id = 24
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sut = database.customerDao
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun `get customer by remote id returns null when there is no customer stored`() = runTest {
        // given
        val remoteCustomerId = 1L

        // when & then
        assertNull(sut.getCustomerByRemoteId(LocalId(site.id), RemoteId(remoteCustomerId)))
    }

    @Test
    fun `get customer by remote id returns customer when there its stored`() = runTest {
        // given
        val remoteCustomerId = 1L
        val username = "userName"
        val customer = WCCustomerModel(
            remoteCustomerId = RemoteId(remoteCustomerId),
            localSiteId = LocalId(site.id),
            username = username
        )

        // when
        sut.upsertCustomer(customer)

        // then
        val storedCustomer = sut.getCustomerByRemoteId(LocalId(site.id), RemoteId(remoteCustomerId))
        assertEquals(storedCustomer!!.remoteCustomerId.value, remoteCustomerId)
        assertEquals(storedCustomer.localSiteId.value, site.id)
        assertEquals(storedCustomer.username, username)
    }

    @Test
    fun `get customer by remote id returns null when there another customer stored`() = runTest {
        // given
        val remoteCustomerId = 1L
        val username = "userName"
        val customer = WCCustomerModel(
            remoteCustomerId = RemoteId(remoteCustomerId),
            localSiteId = LocalId(3),
            username = username
        )

        // when
        sut.upsertCustomer(customer)

        // then
        val storedCustomer = sut.getCustomerByRemoteId(LocalId(site.id), RemoteId(remoteCustomerId))
        assertNull(storedCustomer)
    }

    @Test
    fun `get customers by site returns null when no customers stored`() = runTest {
        // when & then
        assertTrue(sut.getCustomersForSite(LocalId(site.id)).isEmpty())
    }

    @Test
    fun `get customer by site returns customer with site id provided`() = runTest {
        // given
        val usernameOne = "userNameOne"
        val usernameTwo = "userNameTwo"
        val customerOne = WCCustomerModel(
            remoteCustomerId = RemoteId(1L),
            localSiteId = LocalId(3),
            username = usernameOne
        )
        val customerTwo = WCCustomerModel(
            remoteCustomerId = RemoteId(2L),
            localSiteId = LocalId(site.id),
            username = usernameTwo
        )

        // when
        sut.upsertCustomer(customerOne)
        sut.upsertCustomer(customerTwo)

        // then
        val storedCustomers = sut.getCustomersForSite(LocalId(site.id))
        assertEquals(1, storedCustomers.size)
        assertEquals(usernameTwo, storedCustomers[0].username)
        assertEquals(24, storedCustomers[0].localSiteId.value)
        assertEquals(2L, storedCustomers[0].remoteCustomerId.value)
    }

    @Test
    fun `delete customers for site deletes all customers for the site`() = runTest {
        // given
        val usernameOne = "userNameOne"
        val usernameTwo = "userNameTwo"
        val customerOne = WCCustomerModel(
            remoteCustomerId = RemoteId(1L),
            localSiteId = LocalId(3),
            username = usernameOne
        )
        val customerTwo = WCCustomerModel(
            remoteCustomerId = RemoteId(2L),
            localSiteId = LocalId(site.id),
            username = usernameTwo
        )

        // when
        sut.upsertCustomer(customerOne)
        sut.upsertCustomer(customerTwo)
        sut.deleteCustomersForSite(LocalId(site.id))

        // then
        val storedCustomers = sut.getCustomersForSite(LocalId(customerOne.localSiteId.value))
        assertEquals(1, storedCustomers.size)
        assertEquals(usernameOne, storedCustomers[0].username)
        assertEquals(3, storedCustomers[0].localSiteId.value)
        assertEquals(1L, storedCustomers[0].remoteCustomerId.value)
    }
}
