package com.woocommerce.android.ui.woopos.settings.details.security

import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSession
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSessionState
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState.Status
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosSettingsSecurityViewModelTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()
    private val sessionState = MutableStateFlow<WooPosAccessSessionState>(WooPosAccessSessionState.Unprotected)
    private val session: WooPosAccessSession = mock { on { state }.thenReturn(sessionState) }

    @Test
    fun `given no PINs, when Security opens, then it offers access control setup`() {
        assertThat(WooPosSettingsSecurityViewModel(session).isProtectionEnabled.value).isFalse()
    }

    @Test
    fun `when the first Admin PIN unlocks the session, then Security stops offering setup`() {
        // GIVEN
        val viewModel = WooPosSettingsSecurityViewModel(session)

        // WHEN
        sessionState.value = WooPosAccessSessionState.Unlocked(WooPosLocalRole.ADMIN)

        // THEN
        assertThat(viewModel.isProtectionEnabled.value).isTrue()
    }

    @Test
    fun `given unreadable PIN storage, when Security opens, then it never offers setup`() {
        // GIVEN
        sessionState.value = WooPosAccessSessionState.Locked(Status.StorageUnavailable)

        // THEN
        assertThat(WooPosSettingsSecurityViewModel(session).isProtectionEnabled.value).isTrue()
    }
}
