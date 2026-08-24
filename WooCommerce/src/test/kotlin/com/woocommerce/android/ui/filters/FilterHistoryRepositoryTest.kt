package com.woocommerce.android.ui.filters

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.dao.FilterHistoryDao
import org.wordpress.android.fluxc.persistence.entity.FilterHistoryEntity
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class FilterHistoryRepositoryTest : BaseUnitTest() {
    private val filterHistoryDao: FilterHistoryDao = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel().apply { id = SITE_ID }
    }
    private val currentTimeProvider: CurrentTimeProvider = mock {
        on { currentDate() } doReturn Date(NOW)
    }

    private val sut = FilterHistoryRepository(filterHistoryDao, selectedSite, currentTimeProvider)

    @Test
    fun `given stored entries for the current site, when observing, then they are mapped to saved filters`() =
        testBlocking {
            whenever(filterHistoryDao.observeForSite(LocalId(SITE_ID), "ORDERS")).thenReturn(
                flowOf(
                    listOf(
                        FilterHistoryEntity(
                            localSiteId = LocalId(SITE_ID),
                            filterType = "ORDERS",
                            payload = "payload",
                            readableString = "Processing",
                            dateModified = 10
                        )
                    )
                )
            )

            val result = sut.observeHistory(FilterHistoryType.ORDERS).first()

            assertThat(result).isEqualTo(
                listOf(SavedFilter(readableString = "Processing", payload = "payload"))
            )
        }

    @Test
    fun `when saving, then it is inserted for the current site with the current time`() = testBlocking {
        sut.save(FilterHistoryType.ORDERS, payload = "payload", readableString = "Processing")

        verify(filterHistoryDao).insertOrReplace(
            FilterHistoryEntity(
                localSiteId = LocalId(SITE_ID),
                filterType = "ORDERS",
                payload = "payload",
                readableString = "Processing",
                dateModified = NOW
            )
        )
    }

    @Test
    fun `when removing a filter, then it is deleted by its site, type and payload`() = testBlocking {
        sut.remove(FilterHistoryType.ORDERS, SavedFilter(readableString = "Processing", payload = "payload"))

        verify(filterHistoryDao).delete(LocalId(SITE_ID), "ORDERS", "payload")
    }

    @Test
    fun `when clearing, then the current site and type are cleared`() = testBlocking {
        sut.clear(FilterHistoryType.PRODUCTS)

        verify(filterHistoryDao).clear(LocalId(SITE_ID), "PRODUCTS")
    }

    private companion object {
        const val SITE_ID = 123
        const val NOW = 1_000L
    }
}
