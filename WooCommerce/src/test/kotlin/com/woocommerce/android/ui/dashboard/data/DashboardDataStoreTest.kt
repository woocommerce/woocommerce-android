package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DashboardDataStoreTest {
    private val selectedSite: SelectedSite = mock()

    @Test
    fun `given site is null, when widgets observed, then default widgets are emitted`() = runBlocking {
        // Given
        whenever(selectedSite.siteComponent).thenReturn(null)
        whenever(selectedSite.observe()).thenReturn(flowOf(null))

        val dashboardDataStore = DashboardDataStore(selectedSite)

        // When
        val widgets = dashboardDataStore.widgets.first()

        // Then
        assertEquals(dashboardDataStore.getDefaultWidgets(), widgets)
    }
}
