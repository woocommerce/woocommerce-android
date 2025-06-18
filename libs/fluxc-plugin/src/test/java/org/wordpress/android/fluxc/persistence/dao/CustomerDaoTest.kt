package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
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
        val storedCustomer = sut.getCustomerByRemoteId(LocalId(site.id), RemoteId(remoteCustomerId))
        assertThat(storedCustomer).isNull()
    }

    @Test
    fun `get customer by remote id returns customer when there its stored`() = runTest {
        // given
        val remoteCustomerId = 1L
        val username = "userName"
        val expectedCustomer = WCCustomerModel(
            remoteCustomerId = RemoteId(remoteCustomerId),
            localSiteId = LocalId(site.id),
            username = username
        )

        // when
        sut.upsertCustomer(expectedCustomer)

        // then
        val storedCustomer = sut.getCustomerByRemoteId(LocalId(site.id), RemoteId(remoteCustomerId))
        assertThat(storedCustomer).isEqualTo(expectedCustomer)
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
        assertThat(storedCustomer).isNull()
    }

    @Test
    fun `get customers by site returns null when no customers stored`() = runTest {
        // when & then
        assertThat(sut.getCustomersForSite(LocalId(site.id)).isEmpty())
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
        assertThat(storedCustomers).containsExactly(customerTwo)
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
        assertThat(storedCustomers).containsExactly(customerOne)
    }
}
