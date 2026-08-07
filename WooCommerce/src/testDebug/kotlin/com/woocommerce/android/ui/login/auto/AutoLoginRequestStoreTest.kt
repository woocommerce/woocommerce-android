package com.woocommerce.android.ui.login.auto

import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@Suppress("FunctionNaming")
class AutoLoginRequestStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `given a valid payload, when consumed twice, then it is returned and deleted only once`() {
        val store = createStore()
        stage(validPayload())

        val first = store.consume()
        val second = store.consume()

        assertThat(first).isInstanceOf(AutoLoginRequestParseResult.Success::class.java)
        assertThat(second).isEqualTo(AutoLoginRequestParseResult.Invalid)
        assertThat(requestFile()).doesNotExist()
    }

    @Test
    fun `given an oversized payload, when consumed, then it is rejected and deleted`() {
        val store = createStore()
        stage("x".repeat(AutoLoginRequestStore.MAX_PAYLOAD_BYTES + 1))

        assertThat(store.consume()).isEqualTo(AutoLoginRequestParseResult.Invalid)
        assertThat(requestFile()).doesNotExist()
    }

    @Test
    fun `given a terminal status, when published, then output contains only its fixed name`() {
        val store = createStore()

        assertThat(store.publish(AutoLoginStatus.AUTH_FAILED)).isTrue()

        assertThat(File(rootDirectory(), "status.ready").readText())
            .isEqualTo("AUTH_FAILED\n")
            .doesNotContain("user@example.test", "application-password", "store.example")
        assertThat(File(rootDirectory(), "status.tmp")).doesNotExist()
    }

    private fun createStore() = AutoLoginRequestStore(
        rootDirectory = rootDirectory(),
        parser = AutoLoginRequestParser()
    )

    private fun stage(payload: String) {
        rootDirectory().mkdirs()
        requestFile().writeText(payload)
    }

    private fun rootDirectory() = File(temporaryFolder.root, "auto-login")

    private fun requestFile() = File(rootDirectory(), "request.ready")

    private fun validPayload() = """
        {
          "connection":"WP_API",
          "site_url":"https://store.example",
          "username":"user@example.test",
          "password":"application-password"
        }
    """.trimIndent()
}
