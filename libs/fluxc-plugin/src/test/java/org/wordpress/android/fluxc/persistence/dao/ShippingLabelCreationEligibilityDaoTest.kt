package org.wordpress.android.fluxc.persistence.dao

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelCreationEligibility
import org.wordpress.android.fluxc.persistence.DatabaseTestRule

@RunWith(RobolectricTestRunner::class)
class ShippingLabelCreationEligibilityDaoTest {

    private lateinit var dao: ShippingLabelCreationEligibilityDao

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    private val defaultSiteId = LocalId(6)
    private val defaultOrderId = RemoteId(12L)

    @Before
    fun setUp() {
        dao = databaseRule.db.shippingLabelCreationEligibilityDao
    }

    @Test
    fun `when eligibility is upserted, then it can be retrieved`() = runTest {
        val eligibility = generateEligibility(
            siteId = defaultSiteId,
            orderId = defaultOrderId,
            isEligible = true
        )
        dao.upsertShippingLabelCreationEligibility(eligibility)

        val result = dao.getShippingLabelCreationEligibility(defaultSiteId, defaultOrderId)

        assertThat(result).isEqualTo(eligibility)
    }

    @Test
    fun `when eligibility does not exist, then null is returned`() = runTest {
        val result = dao.getShippingLabelCreationEligibility(defaultSiteId, defaultOrderId)

        assertThat(result).isNull()
    }

    @Test
    fun `when eligibility is updated, then changes are persisted`() = runTest {
        val eligibility = generateEligibility(
            siteId = defaultSiteId,
            orderId = defaultOrderId,
            isEligible = false,
            canCreatePackage = false
        )
        dao.upsertShippingLabelCreationEligibility(eligibility)

        val updatedEligibility = eligibility.copy(
            isEligible = true,
            canCreatePackage = true,
            reason = null
        )
        dao.upsertShippingLabelCreationEligibility(updatedEligibility)
        val result = dao.getShippingLabelCreationEligibility(defaultSiteId, defaultOrderId)

        assertThat(result).isEqualTo(updatedEligibility)
    }

    @Test
    fun `when multiple eligibilities exist for different sites, then they can be retrieved separately`() = runTest {
        val siteId1 = LocalId(1)
        val siteId2 = LocalId(2)
        val orderId = RemoteId(100L)

        val eligibility1 = generateEligibility(siteId = siteId1, orderId = orderId, isEligible = true)
        val eligibility2 = generateEligibility(siteId = siteId2, orderId = orderId, isEligible = false)

        dao.upsertShippingLabelCreationEligibility(eligibility1)
        dao.upsertShippingLabelCreationEligibility(eligibility2)

        val result1 = dao.getShippingLabelCreationEligibility(siteId1, orderId)
        val result2 = dao.getShippingLabelCreationEligibility(siteId2, orderId)

        assertThat(result1).isEqualTo(eligibility1)
        assertThat(result2).isEqualTo(eligibility2)
    }

    @Test
    fun `when multiple eligibilities exist for different orders, then they can be retrieved separately`() = runTest {
        val orderId1 = RemoteId(100L)
        val orderId2 = RemoteId(200L)

        val eligibility1 = generateEligibility(siteId = defaultSiteId, orderId = orderId1, isEligible = true)
        val eligibility2 = generateEligibility(siteId = defaultSiteId, orderId = orderId2, isEligible = false)

        dao.upsertShippingLabelCreationEligibility(eligibility1)
        dao.upsertShippingLabelCreationEligibility(eligibility2)

        val result1 = dao.getShippingLabelCreationEligibility(defaultSiteId, orderId1)
        val result2 = dao.getShippingLabelCreationEligibility(defaultSiteId, orderId2)

        assertThat(result1).isEqualTo(eligibility1)
        assertThat(result2).isEqualTo(eligibility2)
    }

    private fun generateEligibility(
        siteId: LocalId,
        orderId: RemoteId,
        canCreatePackage: Boolean = false,
        canCreatePaymentMethod: Boolean = false,
        canCreateCustomsForm: Boolean = false,
        isEligible: Boolean = false,
        reason: String? = "test reason"
    ) = WCShippingLabelCreationEligibility(
        siteId = siteId,
        orderId = orderId,
        canCreatePackage = canCreatePackage,
        canCreatePaymentMethod = canCreatePaymentMethod,
        canCreateCustomsForm = canCreateCustomsForm,
        isEligible = isEligible,
        reason = reason
    )
}
