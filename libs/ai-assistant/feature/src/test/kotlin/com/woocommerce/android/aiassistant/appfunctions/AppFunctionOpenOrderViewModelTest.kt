package com.woocommerce.android.aiassistant.appfunctions

import androidx.lifecycle.SavedStateHandle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

class AppFunctionOpenOrderViewModelTest {
    @Test
    fun `given the order store is still open, when the handoff opens, then the order id is ready`() {
        val viewModel = AppFunctionOpenOrderViewModel(savedState(), storeAccess(store()))

        assertThat(viewModel.state).isEqualTo(AppFunctionOpenOrderState.Open(ORDER_ID))
    }

    @Test
    fun `given another store is open, when the handoff opens, then the store is named`() {
        val viewModel = AppFunctionOpenOrderViewModel(
            savedState(),
            storeAccess(store(id = OTHER_LOCAL_ID, url = OTHER_STORE_URL, siteId = 0L)),
        )

        assertThat(viewModel.state).isEqualTo(AppFunctionOpenOrderState.WrongStore(STORE_NAME))
    }

    @Test
    fun `given access is not allowed, when the handoff opens, then it is unavailable`() {
        val viewModel = AppFunctionOpenOrderViewModel(savedState(), storeAccess(store(), allowed = false))

        assertThat(viewModel.state).isEqualTo(AppFunctionOpenOrderState.Unavailable)
    }

    @Test
    fun `given request extras are missing, when the handoff opens, then it is unavailable`() {
        val viewModel = AppFunctionOpenOrderViewModel(SavedStateHandle(), storeAccess(store()))

        assertThat(viewModel.state).isEqualTo(AppFunctionOpenOrderState.Unavailable)
    }

    private fun storeAccess(openStore: SiteModel, allowed: Boolean = true) = AppFunctionsStoreAccess(
        selectedSite = mock { on { getIfExists() } doReturn openStore },
        accessPolicy = AppFunctionsAccessPolicy { allowed },
    )

    private fun savedState() = SavedStateHandle(
        mapOf(
            EXTRA_ORDER_ID to ORDER_ID,
            EXTRA_SITE_LOCAL_ID to LOCAL_ID,
            EXTRA_STORE_URL to STORE_URL,
            EXTRA_STORE_NAME to STORE_NAME,
        ),
    )

    private fun store(id: Int = LOCAL_ID, url: String = STORE_URL, siteId: Long = 1L) = SiteModel().apply {
        this.id = id
        this.url = url
        this.siteId = siteId
    }

    private companion object {
        const val ORDER_ID = 42L
        const val LOCAL_ID = 7
        const val OTHER_LOCAL_ID = 8
        const val STORE_URL = "https://bean.example"
        const val OTHER_STORE_URL = "https://other.example"
        const val STORE_NAME = "Bean Shop"
    }
}
