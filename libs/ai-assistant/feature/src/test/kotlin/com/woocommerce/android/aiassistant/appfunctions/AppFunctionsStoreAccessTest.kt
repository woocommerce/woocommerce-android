package com.woocommerce.android.aiassistant.appfunctions

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

class AppFunctionsStoreAccessTest {
    @Test
    fun `given access is not allowed, when a store is checked, then there is no access`() {
        val openStore = store()
        val access = storeAccess(openStore) { false }

        assertThat(access.check(openStore.identity())).isEqualTo(StoreCheck.NO_ACCESS)
    }

    @Test
    fun `given no store is open, when a store is checked, then there is no access`() {
        val access = AppFunctionsStoreAccess(
            selectedSite = mock { on { getIfExists() } doReturn null },
            accessPolicy = AppFunctionsAccessPolicy { true },
        )

        assertThat(access.check(store().identity())).isEqualTo(StoreCheck.NO_ACCESS)
    }

    @Test
    fun `given the same store is open, when it is checked, then it is the same store`() {
        val current = store()
        val access = storeAccess(current) { true }

        assertThat(access.check(current.identity())).isEqualTo(StoreCheck.SAME_STORE)
    }

    @Test
    fun `given another application password store has remote id zero, when the first is checked, then it differs`() {
        val current = store(id = OTHER_LOCAL_ID, url = OTHER_STORE_URL, remoteId = 0L)
        val access = storeAccess(current) { true }

        assertThat(access.check(store(remoteId = 0L).identity())).isEqualTo(StoreCheck.DIFFERENT_STORE)
    }

    @Test
    fun `given a reused local id has a different url, when the first store is checked, then it differs`() {
        val current = store(url = OTHER_STORE_URL)
        val access = storeAccess(current) { true }

        assertThat(access.check(store().identity())).isEqualTo(StoreCheck.DIFFERENT_STORE)
    }

    private fun storeAccess(openStore: SiteModel, allowed: () -> Boolean) = AppFunctionsStoreAccess(
        selectedSite = mock { on { getIfExists() } doReturn openStore },
        accessPolicy = AppFunctionsAccessPolicy { allowed() },
    )

    private fun store(id: Int = LOCAL_ID, url: String = STORE_URL, remoteId: Long = 1L) = SiteModel().apply {
        this.id = id
        this.url = url
        siteId = remoteId
    }

    private companion object {
        const val LOCAL_ID = 7
        const val OTHER_LOCAL_ID = 8
        const val STORE_URL = "https://bean.example"
        const val OTHER_STORE_URL = "https://other.example"
    }
}
