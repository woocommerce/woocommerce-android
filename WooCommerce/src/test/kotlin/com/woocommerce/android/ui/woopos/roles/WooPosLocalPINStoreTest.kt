package com.woocommerce.android.ui.woopos.roles

import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosLocalPINStoreTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()
    private val storage = MemoryStorage()
    private val store = WooPosLocalPINStore(storage, coroutineRule.testDispatchers)
    private val authenticator = WooPosPINAuthenticator(store, coroutineRule.testDispatchers)

    @Test
    fun `given no Admin PIN, when reading credentials, then protection is off`() = runTest {
        assertThat(store.credentials(1).getOrThrow()).isEmpty()
        assertThat(authenticator.authenticate(1, "1234").getOrThrow()).isNull()
        WooPosRole.entries.filter { it != WooPosRole.ADMIN }.forEach { role ->
            assertThat(store.setPIN(1, role, "5678").exceptionOrNull())
                .isEqualTo(WooPosPINError.AdminRequired)
        }
        assertThat(storage.writtenSiteIds).isEmpty()
    }

    @Test
    fun `given Admin and Cashier PINs, when authenticating, then each has its permission set`() = runTest {
        // GIVEN
        store.setPIN(1, WooPosRole.ADMIN, "1234").getOrThrow()
        store.setPIN(1, WooPosRole.CASHIER, "5678").getOrThrow()

        // THEN
        assertThat(authenticator.authenticate(1, "1234").getOrThrow()?.permissions)
            .containsExactlyInAnyOrderElementsOf(WooPosPermission.entries)
        assertThat(authenticator.authenticate(1, "5678").getOrThrow()?.permissions)
            .containsExactlyInAnyOrder(
                WooPosPermission.PROCESS_SALES,
                WooPosPermission.VIEW_ORDERS,
                WooPosPermission.APPLY_COUPONS
            )
        assertThat(authenticator.authenticate(1, "9999").getOrThrow()).isNull()
    }

    @Test
    fun `given same PIN for both roles, when setting either role, then the existing PINs remain`() = runTest {
        // GIVEN
        store.setPIN(1, WooPosRole.ADMIN, "1234").getOrThrow()
        store.setPIN(1, WooPosRole.CASHIER, "5678").getOrThrow()

        // THEN
        assertThat(store.setPIN(1, WooPosRole.CASHIER, "1234").exceptionOrNull())
            .isEqualTo(WooPosPINError.MatchesOtherRole)
        assertThat(store.setPIN(1, WooPosRole.ADMIN, "5678").exceptionOrNull())
            .isEqualTo(WooPosPINError.MatchesOtherRole)
        assertThat(authenticator.authenticate(1, "1234").getOrThrow()).isNotNull()
        assertThat(authenticator.authenticate(1, "5678").getOrThrow()).isNotNull()
    }

    @Test
    fun `given a configured store, when switching stores and recreating repository, then PINs stay separate`() = runTest {
        // GIVEN
        store.setPIN(1, WooPosRole.ADMIN, "1234").getOrThrow()
        store.setPIN(2, WooPosRole.ADMIN, "5678").getOrThrow()
        val recreated = WooPosPINAuthenticator(
            WooPosLocalPINStore(storage, coroutineRule.testDispatchers),
            coroutineRule.testDispatchers
        )

        // THEN
        assertThat(recreated.authenticate(1, "1234").getOrThrow()).isNotNull()
        assertThat(recreated.authenticate(2, "1234").getOrThrow()).isNull()
        assertThat(recreated.authenticate(2, "5678").getOrThrow()).isNotNull()
    }

    @Test
    fun `given a non-Admin PIN, when it is removed, then only Admin remains`() = runTest {
        // GIVEN
        store.setPIN(1, WooPosRole.ADMIN, "1234").getOrThrow()
        WooPosRole.entries.filter { it != WooPosRole.ADMIN }.forEach { role ->
            store.setPIN(1, role, "5678").getOrThrow()

            // WHEN
            store.removePIN(1, role).getOrThrow()

            // THEN
            assertThat(authenticator.authenticate(1, "1234").getOrThrow()).isNotNull()
            assertThat(authenticator.authenticate(1, "5678").getOrThrow()).isNull()
            assertThat(store.credentials(1).getOrThrow()).hasSize(1)
        }
    }

    @Test
    fun `given both PINs, when Admin PIN is removed, then both PINs are removed only from that store`() = runTest {
        // GIVEN
        store.setPIN(1, WooPosRole.ADMIN, "1234").getOrThrow()
        store.setPIN(1, WooPosRole.CASHIER, "5678").getOrThrow()
        store.setPIN(2, WooPosRole.ADMIN, "0000").getOrThrow()

        // WHEN
        store.removePIN(1, WooPosRole.ADMIN).getOrThrow()

        // THEN
        assertThat(store.credentials(1).getOrThrow()).isEmpty()
        assertThat(store.credentials(2).getOrThrow()).hasSize(1)
    }

    @Test
    fun `given multiple stores, when clearing for logout, then all local credentials are removed`() = runTest {
        // GIVEN
        store.setPIN(1, WooPosRole.ADMIN, "1234").getOrThrow()
        store.setPIN(2, WooPosRole.ADMIN, "5678").getOrThrow()

        // WHEN
        store.clearAll().getOrThrow()

        // THEN
        assertThat(store.credentials(1).getOrThrow()).isEmpty()
        assertThat(store.credentials(2).getOrThrow()).isEmpty()
    }

    @Test
    fun `given invalid PIN formats, when setting PINs, then each is rejected without storage writes`() = runTest {
        listOf("", "123", "12345", "12a4", "１２３４", "١٢٣٤", " 123", "123\n").forEach { pin ->
            assertThat(store.setPIN(1, WooPosRole.ADMIN, pin).exceptionOrNull())
                .isEqualTo(WooPosPINError.InvalidFormat)
        }
        assertThat(storage.entries).isEmpty()
    }

    @Test
    fun `given invalid site identity, when changing PINs, then storage is unchanged`() = runTest {
        // GIVEN
        val records = mapOf(0 to byteArrayOf(0), -1 to byteArrayOf(-1), 1 to byteArrayOf(1))
        storage.entries.putAll(records)

        // WHEN
        listOf(0, -1).forEach { siteId ->
            WooPosRole.entries.forEach { role ->
                assertThat(store.setPIN(siteId, role, "1234").exceptionOrNull())
                    .isEqualTo(WooPosPINError.StorageFailure)
                assertThat(store.removePIN(siteId, role).exceptionOrNull())
                    .isEqualTo(WooPosPINError.StorageFailure)
            }
        }

        // THEN
        assertThat(storage.writtenSiteIds).isEmpty()
        assertThat(storage.entries).isEqualTo(records)
    }

    @Test
    fun `given malformed records, when reading or replacing PINs, then storage fails and records are preserved`() = runTest {
        // GIVEN
        storage.entries[1] = byteArrayOf(1, 2, 3)

        // THEN
        assertThat(store.credentials(1).exceptionOrNull()).isEqualTo(WooPosPINError.StorageFailure)
        assertThat(authenticator.authenticate(1, "1234").exceptionOrNull()).isEqualTo(WooPosPINError.StorageFailure)
        assertThat(store.setPIN(1, WooPosRole.ADMIN, "1234").exceptionOrNull())
            .isEqualTo(WooPosPINError.StorageFailure)
        assertThat(storage.entries[1]).containsExactly(1, 2, 3)
    }

    @Test
    fun `given unreadable secure storage, when reading credentials, then failure is not protection off`() = runTest {
        // GIVEN
        val failingStorage: WooPosPINStorage = mock {
            on { read(any()) } doThrow IllegalStateException("Keystore unavailable")
        }
        val failingStore = WooPosLocalPINStore(failingStorage, coroutineRule.testDispatchers)

        // THEN
        assertThat(failingStore.credentials(1).exceptionOrNull()).isEqualTo(WooPosPINError.StorageFailure)
    }

    @Test
    fun `given cancelled storage operation, when reading credentials, then cancellation propagates`() = runTest {
        // GIVEN
        val cancelledStorage: WooPosPINStorage = mock()
        whenever(cancelledStorage.read(1)).thenThrow(CancellationException())
        val cancelledStore = WooPosLocalPINStore(cancelledStorage, coroutineRule.testDispatchers)

        // WHEN
        val failure = runCatching { cancelledStore.credentials(1) }.exceptionOrNull()

        // THEN
        assertThat(failure).isInstanceOf(CancellationException::class.java)
    }

    private class MemoryStorage : WooPosPINStorage {
        val entries = mutableMapOf<Int, ByteArray>()
        val writtenSiteIds = mutableListOf<Int>()
        override fun read(localSiteId: Int): ByteArray? = entries[localSiteId]
        override fun write(localSiteId: Int, bytes: ByteArray?) {
            writtenSiteIds.add(localSiteId)
            if (bytes == null) entries.remove(localSiteId) else entries[localSiteId] = bytes
        }
        override fun clearAll() = entries.clear()
    }
}
