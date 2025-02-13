package org.wordpress.android.fluxc.persistence

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class CustomerSqlUtilsTest {
    val site = SiteModel().apply {
        email = "test@example.org"
        name = "Test Site"
        id = 24
    }

    @Before
    fun setUp() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(WCCustomerModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun `get customer by remote id returns null when there is no customer stored`() {
        // given
        val remoteCustomerId = 1L

        // when & then
        assertNull(CustomerSqlUtils.getCustomerByRemoteId(site, remoteCustomerId))
    }

    @Test
    fun `get customer by remote id returns customer when there its stored`() {
        // given
        val remoteCustomerId = 1L
        val username = "userName"
        val customer = WCCustomerModel().apply {
            this.remoteCustomerId = remoteCustomerId
            this.localSiteId = site.id
            this.username = username
        }

        // when
        CustomerSqlUtils.insertOrUpdateCustomer(customer)

        // then
        val storedCustomer = CustomerSqlUtils.getCustomerByRemoteId(site, remoteCustomerId)
        assertEquals(storedCustomer!!.remoteCustomerId, remoteCustomerId)
        assertEquals(storedCustomer.localSiteId, site.id)
        assertEquals(storedCustomer.username, username)
    }

    @Test
    fun `get customer by remote id returns null when there another customer stored`() {
        // given
        val remoteCustomerId = 1L
        val username = "userName"
        val customer = WCCustomerModel().apply {
            this.remoteCustomerId = remoteCustomerId
            this.localSiteId = 3
            this.username = username
        }

        // when
        CustomerSqlUtils.insertOrUpdateCustomer(customer)

        // then
        val storedCustomer = CustomerSqlUtils.getCustomerByRemoteId(site, remoteCustomerId)
        assertNull(storedCustomer)
    }

    @Test
    fun `get customers by site returns null when no customers stored`() {
        // when & then
        assertTrue(CustomerSqlUtils.getCustomersForSite(site).isEmpty())
    }

    @Test
    fun `get customer by site returns customer with site id provided`() {
        // given
        val usernameOne = "userNameOne"
        val usernameTwo = "userNameTwo"
        val customerOne = WCCustomerModel().apply {
            this.remoteCustomerId = 1L
            this.localSiteId = 3
            this.username = usernameOne
        }
        val customerTwo = WCCustomerModel().apply {
            this.remoteCustomerId = 2L
            this.localSiteId = site.id
            this.username = usernameTwo
        }

        // when
        CustomerSqlUtils.insertOrUpdateCustomer(customerOne)
        CustomerSqlUtils.insertOrUpdateCustomer(customerTwo)

        // then
        val storedCustomers = CustomerSqlUtils.getCustomersForSite(site)
        assertEquals(1, storedCustomers.size)
        assertEquals(usernameTwo, storedCustomers[0].username)
        assertEquals(24, storedCustomers[0].localSiteId)
        assertEquals(2L, storedCustomers[0].remoteCustomerId)
    }

    @Test
    fun `get customers by remote ids returns null when no customers stored`() {
        // when & then
        assertTrue(CustomerSqlUtils.getCustomerByRemoteIds(site, listOf(1)).isEmpty())
    }

    @Test
    fun `get customers by remote ids returns customers with remote ids`() {
        // given
        val usernameOne = "userNameOne"
        val usernameTwo = "userNameTwo"
        val customerOne = WCCustomerModel().apply {
            this.remoteCustomerId = 1L
            this.localSiteId = site.id
            this.username = usernameOne
        }
        val customerTwo = WCCustomerModel().apply {
            this.remoteCustomerId = 2L
            this.localSiteId = site.id
            this.username = usernameTwo
        }

        // when
        CustomerSqlUtils.insertOrUpdateCustomer(customerOne)
        CustomerSqlUtils.insertOrUpdateCustomer(customerTwo)

        // then
        val customerByRemoteIds = CustomerSqlUtils.getCustomerByRemoteIds(site, listOf(1, 2, 3))
        assertEquals(customerByRemoteIds.size, 2)
        assertEquals(customerByRemoteIds[0], customerOne)
        assertEquals(customerByRemoteIds[1], customerTwo)
    }

    @Test
    fun `delete customers for site deletes all customers for the site`() {
        // given
        val usernameOne = "userNameOne"
        val usernameTwo = "userNameTwo"
        val customerOne = WCCustomerModel().apply {
            this.remoteCustomerId = 1L
            this.localSiteId = 3
            this.username = usernameOne
        }
        val customerTwo = WCCustomerModel().apply {
            this.remoteCustomerId = 2L
            this.localSiteId = site.id
            this.username = usernameTwo
        }

        // when
        CustomerSqlUtils.insertOrUpdateCustomer(customerOne)
        CustomerSqlUtils.insertOrUpdateCustomer(customerTwo)
        CustomerSqlUtils.deleteCustomersForSite(site)

        // then
        val storedCustomers = CustomerSqlUtils.getCustomersForSite(SiteModel().apply { id = customerOne.localSiteId })
        assertEquals(1, storedCustomers.size)
        assertEquals(usernameOne, storedCustomers[0].username)
        assertEquals(3, storedCustomers[0].localSiteId)
        assertEquals(1L, storedCustomers[0].remoteCustomerId)
    }
}
