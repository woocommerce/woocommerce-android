package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.persistence.entity.FilterHistoryEntity

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class FilterHistoryDaoTest {
    private lateinit var sut: FilterHistoryDao

    private val siteId = LocalId(1)
    private val orders = "ORDERS"
    private val products = "PRODUCTS"

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext<Application>())

    @Before
    fun setup() {
        sut = databaseRule.db.filterHistoryDao
    }

    @Test
    fun `given no history, when observed, then empty list is emitted`() = runTest {
        val result = sut.observeForSite(siteId, orders).first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `given a saved filter, when observed, then it is emitted`() = runTest {
        sut.insertOrReplace(entity(payload = "status=processing", readableString = "Processing", dateModified = 1))

        val result = sut.observeForSite(siteId, orders).first()

        assertThat(result).hasSize(1)
        assertThat(result.first().payload).isEqualTo("status=processing")
        assertThat(result.first().readableString).isEqualTo("Processing")
    }

    @Test
    fun `given multiple filters, when observed, then newest modified is first`() = runTest {
        sut.insertOrReplace(entity(payload = "a", readableString = "A", dateModified = 1))
        sut.insertOrReplace(entity(payload = "b", readableString = "B", dateModified = 2))
        sut.insertOrReplace(entity(payload = "c", readableString = "C", dateModified = 3))

        val result = sut.observeForSite(siteId, orders).first()

        assertThat(result.map { it.payload }).containsExactly("c", "b", "a")
    }

    @Test
    fun `given an identical payload, when saved again, then it is deduped and bumped to the top`() = runTest {
        sut.insertOrReplace(entity(payload = "a", readableString = "A", dateModified = 1))
        sut.insertOrReplace(entity(payload = "b", readableString = "B", dateModified = 2))

        // Re-save "a" with a newer timestamp and updated readable string.
        sut.insertOrReplace(entity(payload = "a", readableString = "A updated", dateModified = 3))

        val result = sut.observeForSite(siteId, orders).first()

        assertThat(result.map { it.payload }).containsExactly("a", "b")
        assertThat(result.first().readableString).isEqualTo("A updated")
    }

    @Test
    fun `given a saved filter, when deleted by id, then it is removed`() = runTest {
        sut.insertOrReplace(entity(payload = "a", readableString = "A", dateModified = 1))
        val saved = sut.observeForSite(siteId, orders).first().first()

        sut.delete(saved.id)

        assertThat(sut.observeForSite(siteId, orders).first()).isEmpty()
    }

    @Test
    fun `given filters for a site and type, when cleared, then only that site and type is emptied`() = runTest {
        sut.insertOrReplace(entity(payload = "a", readableString = "A", dateModified = 1))
        sut.insertOrReplace(entity(payload = "b", readableString = "B", dateModified = 2))
        sut.insertOrReplace(entity(filterType = products, payload = "p", readableString = "P", dateModified = 1))
        sut.insertOrReplace(entity(localSiteId = LocalId(2), payload = "o", readableString = "O", dateModified = 1))

        sut.clear(siteId, orders)

        assertThat(sut.observeForSite(siteId, orders).first()).isEmpty()
        assertThat(sut.observeForSite(siteId, products).first()).hasSize(1)
        assertThat(sut.observeForSite(LocalId(2), orders).first()).hasSize(1)
    }

    @Test
    fun `given filters for multiple sites, when observed, then each site is isolated`() = runTest {
        sut.insertOrReplace(entity(payload = "a", readableString = "A", dateModified = 1))
        sut.insertOrReplace(entity(localSiteId = LocalId(2), payload = "b", readableString = "B", dateModified = 1))

        assertThat(sut.observeForSite(siteId, orders).first().first().payload).isEqualTo("a")
        assertThat(sut.observeForSite(LocalId(2), orders).first().first().payload).isEqualTo("b")
    }

    private fun entity(
        localSiteId: LocalId = siteId,
        filterType: String = orders,
        payload: String,
        readableString: String,
        dateModified: Long
    ) = FilterHistoryEntity(
        localSiteId = localSiteId,
        filterType = filterType,
        payload = payload,
        readableString = readableString,
        dateModified = dateModified
    )
}
