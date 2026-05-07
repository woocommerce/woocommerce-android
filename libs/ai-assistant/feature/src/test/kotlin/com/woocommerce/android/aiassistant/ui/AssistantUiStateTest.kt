package com.woocommerce.android.aiassistant.ui

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantUiStateTest {
    @Test
    fun `when status is streaming, then turn is active`() {
        val state = AssistantUiState(status = AssistantUiStatus.STREAMING)

        assertThat(state.isTurnActive).isTrue()
    }

    @Test
    fun `when status is awaiting confirmation, then turn is active`() {
        val state = AssistantUiState(status = AssistantUiStatus.AWAITING_CONFIRMATION)

        assertThat(state.isTurnActive).isTrue()
    }

    @Test
    fun `when status is idle, then turn is not active`() {
        val state = AssistantUiState(status = AssistantUiStatus.IDLE)

        assertThat(state.isTurnActive).isFalse()
    }

    @Test
    fun `when status is error, then turn is not active`() {
        val state = AssistantUiState(status = AssistantUiStatus.ERROR)

        assertThat(state.isTurnActive).isFalse()
    }
}
