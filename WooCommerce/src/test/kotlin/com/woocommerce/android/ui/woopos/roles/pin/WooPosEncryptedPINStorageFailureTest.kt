package com.woocommerce.android.ui.woopos.roles.pin

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.security.ProviderException
import javax.crypto.AEADBadTagException

class WooPosEncryptedPINStorageFailureTest {
    @Test
    fun `given a tampered record, when checking the failure, then it is not retried`() {
        assertThat(WooPosEncryptedPINStorage.isTransientFailure(AEADBadTagException())).isFalse()
    }

    @Test
    fun `given Android before 13, when a Keystore operation fails, then it is retried`() {
        assertThat(WooPosEncryptedPINStorage.isTransientFailure(ProviderException())).isTrue()
    }

    @Test
    fun `given a failure with record contents in its message, when reporting it, then only class names are sent`() {
        val failure = IllegalStateException("secret", IllegalArgumentException("{\"salt\":[1,2,3]}"))

        val reported = WooPosEncryptedPINStorage.sanitizedFailure("read", failure)

        assertThat(reported.message).isEqualTo(
            "Local PIN storage read failed: java.lang.IllegalStateException <- java.lang.IllegalArgumentException"
        )
        assertThat(reported.cause).isNull()
        assertThat(reported.stackTrace).isEqualTo(failure.stackTrace)
    }
}
