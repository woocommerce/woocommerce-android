package com.woocommerce.android.aiassistant.core.chat

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DiagnosticsTest {
    @Test
    fun `given default diagnostics, when constructed, then transport and tool diagnostics are absent`() {
        val diagnostics = Diagnostics()

        assertThat(diagnostics.transport).isNull()
        assertThat(diagnostics.tool).isNull()
    }

    @Test
    fun `given equivalent transport diagnostics, when compared, then they are equal`() {
        val first = Diagnostics(
            transport = TransportDiagnostics(httpStatus = 400),
        )
        val second = Diagnostics(
            transport = TransportDiagnostics(httpStatus = 400),
        )

        assertThat(first).isEqualTo(second)
    }
}
