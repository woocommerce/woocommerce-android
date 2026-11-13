package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WooPosLockSessionTest {
    private val featureFlagRepository: FeatureFlagRepository = mock()
    private val session = WooPosLockSession(featureFlagRepository)

    @Test
    fun `given flag off, when lock requested, then POS stays unlocked`() {
        // WHEN
        session.lock()

        // THEN
        assertThat(session.state.value.isLocked).isFalse()
        assertThat(FeatureFlag.WOO_POS_ROLES.localValue).isFalse()
    }

    @Test
    fun `given flag on, when lock requested, then POS locks with empty entry`() {
        // GIVEN
        enableLock()

        // WHEN
        session.lock()

        // THEN
        assertThat(session.state.value).isEqualTo(WooPosLockScreenState(isLocked = true))
    }

    @Test
    fun `given locked, when three digits entered, then POS stays locked`() {
        // GIVEN
        enableLock()
        session.lock()

        // WHEN
        listOf(9, 0, 7).forEach(session::enterDigit)

        // THEN
        assertThat(session.state.value).isEqualTo(WooPosLockScreenState(isLocked = true, enteredDigits = 3))
    }

    @Test
    fun `given locked, when any four digits entered, then POS unlocks and clears entry`() {
        // GIVEN
        enableLock()

        // WHEN
        listOf(listOf(0, 0, 0, 0), listOf(9, 7, 2, 8)).forEach { digits ->
            session.lock()
            digits.forEach(session::enterDigit)

            // THEN
            assertThat(session.state.value).isEqualTo(WooPosLockScreenState())
        }
    }

    @Test
    fun `given partial entry, when last digit deleted, then four new digits are needed to unlock`() {
        // GIVEN
        enableLock()
        session.lock()
        session.enterDigit(1)

        // WHEN
        session.deleteDigit()
        listOf(2, 3, 4).forEach(session::enterDigit)

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        session.enterDigit(5)
        assertThat(session.state.value).isEqualTo(WooPosLockScreenState())
    }

    @Test
    fun `given empty entry, when delete or invalid digits received, then entry stays empty`() {
        // GIVEN
        enableLock()
        session.lock()

        // WHEN
        session.deleteDigit()
        session.enterDigit(-1)
        session.enterDigit(10)

        // THEN
        assertThat(session.state.value).isEqualTo(WooPosLockScreenState(isLocked = true))
    }

    @Test
    fun `given unlocked, when digit entered, then entry stays empty`() {
        // WHEN
        session.enterDigit(1)

        // THEN
        assertThat(session.state.value).isEqualTo(WooPosLockScreenState())
    }

    @Test
    fun `given partial entry, when lock requested again, then entry clears`() {
        // GIVEN
        enableLock()
        session.lock()
        session.enterDigit(1)

        // WHEN
        session.lock()

        // THEN
        assertThat(session.state.value).isEqualTo(WooPosLockScreenState(isLocked = true))
    }

    private fun enableLock() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ROLES)).thenReturn(true)
    }
}
