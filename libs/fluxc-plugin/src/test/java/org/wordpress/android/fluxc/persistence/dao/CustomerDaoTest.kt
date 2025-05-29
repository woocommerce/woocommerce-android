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
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class CustomerDaoTest {
    private lateinit var sut: CustomerDao
    private lateinit var database: WCAndroidDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sut = database.customerDao
    }

    @Test
    fun testInsertOrUpdateCustomer() = runTest {
        val customer = generateSampleCustomer(1, 100)
        val site = SiteModel().apply { id = customer.localSiteId.value }

        // Test inserting customer
        sut.upsertCustomer(customer)
        val storedCustomersCount = sut.getCustomersForSite(site.localId()).count()
        assertEquals(1, storedCustomersCount)

        // Test updating customer
        val updatedCustomer = customer.copy(
            firstName = "Updated",
            lastName = "Customer"
        )
        sut.upsertCustomer(updatedCustomer)

        val updatedCustomersCount = sut.getCustomersForSite(site.localId()).count()
        assertEquals(1, updatedCustomersCount)

        val retrievedCustomer = sut.getCustomerByRemoteId(site.localId(), customer.remoteCustomerId)
        assertEquals(updatedCustomer.firstName, retrievedCustomer?.firstName)
        assertEquals(updatedCustomer.lastName, retrievedCustomer?.lastName)
    }

    @Test
    fun testInsertOrUpdateCustomers() = runTest {
        val site = SiteModel().apply { id = 2 }
        val customers = listOf(
            generateSampleCustomer(site.id, 100),
            generateSampleCustomer(site.id, 101),
            generateSampleCustomer(site.id, 102)
        )

        // Delete all customers for this site, then test inserting the above customers
        sut.deleteCustomersForSite(site.localId())
        sut.upsertCustomers(customers)
        val storedCustomers = sut.getCustomersForSite(site.localId())
        assertEquals(customers.size, storedCustomers.size)
    }

    @Test
    fun testGetCustomersForSite() = runTest {
        // insert customers for one site
        val site1 = SiteModel().apply { id = 2 }
        val customer1 = generateSampleCustomer(site1.id, 100)
        sut.upsertCustomer(customer1)

        // verify that it is stored
        val storedCustomer = sut.getCustomerByRemoteId(site1.localId(), customer1.remoteCustomerId)
        assertEquals(customer1.remoteCustomerId, storedCustomer?.remoteCustomerId)
        assertEquals(customer1.firstName, storedCustomer?.firstName)
        assertEquals(customer1.lastName, storedCustomer?.lastName)

        // insert customers for another site
        val site2 = SiteModel().apply { id = 10 }
        val customer2 = generateSampleCustomer(site2.id, 200)
        sut.upsertCustomer(customer2)

        // verify that it is stored
        val storedCustomer2 = sut.getCustomerByRemoteId(site2.localId(), customer2.remoteCustomerId)
        assertEquals(customer2.remoteCustomerId, storedCustomer2?.remoteCustomerId)
        assertEquals(customer2.firstName, storedCustomer2?.firstName)
        assertEquals(customer2.lastName, storedCustomer2?.lastName)

        // add another customer for site 1
        val customer3 = generateSampleCustomer(site1.id, 101)
        sut.upsertCustomer(customer3)

        // verify that the site 2 customer size is still the same
        val storedCustomerForSite2Count = sut.getCustomersForSite(site2.localId()).count()
        assertEquals(1, storedCustomerForSite2Count)

        // verify that the site 1 customer is increases by 1
        val storedCustomerForSite1Count = sut.getCustomersForSite(site1.localId()).count()
        assertEquals(2, storedCustomerForSite1Count)
    }

    @Test
    fun testGetCustomerByRemoteIds() = runTest {
        val customerIds = listOf<Long>(100, 101, 102)

        val site = SiteModel().apply { id = 2 }
        val customers = listOf(
            generateSampleCustomer(site.id, 100),
            generateSampleCustomer(site.id, 101),
            generateSampleCustomer(site.id, 103)
        )

        sut.upsertCustomers(customers)

        val retrievedCustomers = sut.getCustomerByRemoteIds(site.localId(), customerIds)
        assertEquals(2, retrievedCustomers.size)

        // insert customers with the same customerIds but for a different site
        val differentSiteCustomers = listOf(
            generateSampleCustomer(10, 100),
            generateSampleCustomer(10, 101),
            generateSampleCustomer(10, 102)
        )

        sut.upsertCustomers(differentSiteCustomers)

        // verify that the customers for the first site is still 2
        assertEquals(2, sut.getCustomerByRemoteIds(site.localId(), customerIds).size)

        // verify that the customers for the second site is 3
        val site2 = SiteModel().apply { id = 10 }
        val differentSiteRetrievedCustomers = sut.getCustomerByRemoteIds(site2.localId(), customerIds)
        assertEquals(3, differentSiteRetrievedCustomers.size)
    }

    @Test
    fun testDeleteCustomersForSite() = runTest {
        val site1 = SiteModel().apply { id = 2 }
        val site2 = SiteModel().apply { id = 3 }

        val customers1 = listOf(
            generateSampleCustomer(site1.id, 100),
            generateSampleCustomer(site1.id, 101)
        )

        val customers2 = listOf(
            generateSampleCustomer(site2.id, 200),
            generateSampleCustomer(site2.id, 201)
        )

        sut.upsertCustomers(customers1)
        sut.upsertCustomers(customers2)

        // Verify both sites have customers
        assertEquals(2, sut.getCustomersForSite(site1.localId()).size)
        assertEquals(2, sut.getCustomersForSite(site2.localId()).size)

        // Delete customers for site1
        sut.deleteCustomersForSite(site1.localId())

        // Verify site1 has no customers but site2 still has customers
        assertEquals(0, sut.getCustomersForSite(site1.localId()).size)
        assertEquals(2, sut.getCustomersForSite(site2.localId()).size)
    }

    private fun generateSampleCustomer(siteId: Int, remoteId: Long): WCCustomerModel {
        return WCCustomerModel(
            localSiteId = LocalOrRemoteId.LocalId(siteId),
            remoteCustomerId = remoteId,
            firstName = "Test",
            lastName = "Customer",
            email = "test$remoteId@example.com",
            username = "testcustomer$remoteId",
            avatarUrl = "",
            dateCreated = "2023-01-01T00:00:00",
            dateCreatedGmt = "2023-01-01T00:00:00",
            dateModified = "2023-01-01T00:00:00",
            dateModifiedGmt = "2023-01-01T00:00:00",
            isPayingCustomer = false,
            role = "customer",
            billingAddress1 = "123 Test St",
            billingAddress2 = "",
            billingCity = "Test City",
            billingCompany = "",
            billingCountry = "US",
            billingEmail = "test$remoteId@example.com",
            billingFirstName = "Test",
            billingLastName = "Customer",
            billingPhone = "555-555-5555",
            billingPostcode = "12345",
            billingState = "CA",
            shippingAddress1 = "123 Test St",
            shippingAddress2 = "",
            shippingCity = "Test City",
            shippingCompany = "",
            shippingCountry = "US",
            shippingFirstName = "Test",
            shippingLastName = "Customer",
            shippingPostcode = "12345",
            shippingState = "CA"
        )
    }

    private fun SiteModel.localId() = LocalOrRemoteId.LocalId(id)

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }
}
