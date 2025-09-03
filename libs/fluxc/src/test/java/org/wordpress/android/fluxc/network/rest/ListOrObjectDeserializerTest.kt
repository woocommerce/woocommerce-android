package org.wordpress.android.fluxc.network.rest

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse

class ListOrObjectDeserializerTest {
    private fun gson() = GsonBuilder()
        .registerTypeHierarchyAdapter(List::class.java, ListOrObjectDeserializer())
        .create()

    data class Item(val a: Int, val b: String)

    @Test
    fun `when array of primitives is deserialized, then returns list of ints`() {
        val json = "[1, 2, 3]"
        val type = object : TypeToken<List<Int>>() {}.type
        val result: List<Int> = gson().fromJson(json, type)
        assertArrayEquals(arrayOf(1, 2, 3), result.toTypedArray())
    }

    @Test
    fun `when array of objects is deserialized, then returns list of data class`() {
        val json = """[{"a":1,"b":"x"},{"a":2,"b":"y"}]"""
        val type = object : TypeToken<List<Item>>() {}.type
        val result: List<Item> = gson().fromJson(json, type)
        assertEquals(listOf(Item(1, "x"), Item(2, "y")), result)
    }

    @Test
    fun `when object has numeric string keys, then converted to ordered list`() {
        val json = """{"2":"a","1":"b","10":"z"}"""
        val type = object : TypeToken<List<String>>() {}.type
        val result: List<String> = gson().fromJson(json, type)
        // Keys sorted numerically: 1,2,10 -> values a,b,z
        assertArrayEquals(arrayOf("b", "a", "z"), result.toTypedArray())
    }

    @Test(expected = JsonParseException::class)
    fun `when object has non numeric key, then throws json parse exception`() {
        val json = """{"a":1,"2":2}"""
        val type = object : TypeToken<List<Int>>() {}.type
        gson().fromJson<List<Int>>(json, type)
    }

    @Test
    fun `when json is null, then returns null list`() {
        val json = "null"
        val type = object : TypeToken<List<Int>?>() {}.type
        val result: List<Int>? = gson().fromJson(json, type)
        assertNull(result)
    }

    @Test(expected = JsonParseException::class)
    fun `when json type is string, then throws json parse exception`() {
        val json = "\"not an array or object\""
        val type = object : TypeToken<List<Int>>() {}.type
        gson().fromJson<List<Int>>(json, type)
    }

    @Test
    fun `when object has numeric keys and objects, then converted to ordered list of objects`() {
        val json = """{"2":{"a":2,"b":"y"},"1":{"a":1,"b":"x"}}"""
        val type = object : TypeToken<List<Item>>() {}.type
        val result: List<Item> = gson().fromJson(json, type)
        assertEquals(listOf(Item(1, "x"), Item(2, "y")), result)
    }

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

        val response = gson().fromJson(json, RootWPAPIRestResponse::class.java)

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

        val response = gson().fromJson(json, RootWPAPIRestResponse::class.java)

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

        val response = gson().fromJson(json, RootWPAPIRestResponse::class.java)

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

        val response = gson().fromJson(json, RootWPAPIRestResponse::class.java)

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

        val response = gson().fromJson(json, RootWPAPIRestResponse::class.java)

        assertThat(response.namespaces).isNotNull
        assertThat(response.namespaces).isEmpty()
    }
}
