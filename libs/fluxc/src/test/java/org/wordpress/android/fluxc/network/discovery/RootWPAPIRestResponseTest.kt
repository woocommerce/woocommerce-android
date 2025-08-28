package org.wordpress.android.fluxc.network.discovery

import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RootWPAPIRestResponseTest {
    @Test
    fun `given namespaces as array, when deserializing, then should parse correctly`() {
        val json = """
            {
              "name": "Test Site",
              "description": "Test Description",
              "url": "https://example.com",
              "gmt_offset": "0",
              "namespaces": ["oembed/1.0", "wc/v3", "wp/v2"]
            }
        """.trimIndent()

        val gson = Gson()
        val response = gson.fromJson(json, RootWPAPIRestResponse::class.java)

        assertThat(response.namespaces).hasSize(3)
        val expectedNamespaces = listOf("oembed/1.0", "wc/v3", "wp/v2")
        assertThat(response.namespaces).isEqualTo(expectedNamespaces)
    }

    @Test
    fun `given namespaces as object, when deserializing, then should parse correctly`() {
        val json = """
            {
              "name": "Test Site",
              "description": "Test Description",
              "url": "https://example.com",
              "gmt_offset": "0",
              "namespaces": {
                "0": "oembed/1.0",
                "1": "akismet/v1",
                "4": "jetpack/v4",
                "8": "wc/v3",
                "29": "wp/v2"
              }
            }
        """.trimIndent()

        val gson = Gson()
        val response = gson.fromJson(json, RootWPAPIRestResponse::class.java)

        assertThat(response.namespaces).hasSize(5)
        val expectedNamespaces = listOf("oembed/1.0", "akismet/v1", "jetpack/v4", "wc/v3", "wp/v2")
        assertThat(response.namespaces).hasSize(expectedNamespaces.size)
        expectedNamespaces.forEach { namespace ->
            assertThat(response.namespaces).contains(namespace)
        }
    }

    @Test
    fun `given namespaces missing, when deserializing, then should be null`() {
        val json = """
            {
              "name": "Test Site",
              "description": "Test Description",
              "url": "https://example.com",
              "gmt_offset": "0"
            }
        """.trimIndent()

        val gson = Gson()
        val response = gson.fromJson(json, RootWPAPIRestResponse::class.java)

        assertThat(response).isNotNull
        assertThat(response.namespaces).isNull()
    }

    @Test
    fun `given namespaces as empty array, when deserializing, then should return empty list`() {
        val json = """
            {
              "name": "Test Site",
              "description": "Test Description",
              "url": "https://example.com",
              "gmt_offset": "0",
              "namespaces": []
            }
        """.trimIndent()

        val gson = Gson()
        val response = gson.fromJson(json, RootWPAPIRestResponse::class.java)

        assertThat(response.namespaces).isNotNull
        assertThat(response.namespaces).isEmpty()
    }

    @Test
    fun `given namespaces as empty object, when deserializing, then should return empty list`() {
        val json = """
            {
              "name": "Test Site",
              "description": "Test Description",
              "url": "https://example.com",
              "gmt_offset": "0",
              "namespaces": {}
            }
        """.trimIndent()

        val gson = Gson()
        val response = gson.fromJson(json, RootWPAPIRestResponse::class.java)

        assertThat(response.namespaces).isNotNull
        assertThat(response.namespaces).isEmpty()
    }
}
