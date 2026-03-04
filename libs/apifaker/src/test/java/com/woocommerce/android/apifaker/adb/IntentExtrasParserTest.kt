package com.woocommerce.android.apifaker.adb

import android.content.Intent
import com.woocommerce.android.apifaker.models.ApiType
import com.woocommerce.android.apifaker.models.HttpMethod
import com.woocommerce.android.apifaker.models.QueryParameter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class IntentExtrasParserTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val intent: Intent = mock()

    @Test
    fun `when parseApiType with wp-api, then returns WPApi`() {
        whenever(intent.getStringExtra(Extras.API_TYPE)).thenReturn("wp-api")

        val result = IntentExtrasParser.parseApiType(intent)

        assertEquals(ApiType.WPApi, result)
    }

    @Test
    fun `when parseApiType with wp-com, then returns WPCom`() {
        whenever(intent.getStringExtra(Extras.API_TYPE)).thenReturn("wp-com")

        val result = IntentExtrasParser.parseApiType(intent)

        assertEquals(ApiType.WPCom, result)
    }

    @Test
    fun `when parseApiType with custom and host provided, then returns Custom with host`() {
        whenever(intent.getStringExtra(Extras.API_TYPE)).thenReturn("custom")
        whenever(intent.getStringExtra(Extras.CUSTOM_HOST)).thenReturn("my-api.example.com")

        val result = IntentExtrasParser.parseApiType(intent)

        assertEquals(ApiType.Custom("my-api.example.com"), result)
    }

    @Test
    fun `when parseApiType with custom and no host, then throws error`() {
        whenever(intent.getStringExtra(Extras.API_TYPE)).thenReturn("custom")
        whenever(intent.getStringExtra(Extras.CUSTOM_HOST)).thenReturn(null)

        val exception = assertThrows(IllegalStateException::class.java) {
            IntentExtrasParser.parseApiType(intent)
        }

        assertTrue(exception.message!!.contains(Extras.CUSTOM_HOST))
    }

    @Test
    fun `when parseApiType with missing extra, then throws error`() {
        whenever(intent.getStringExtra(Extras.API_TYPE)).thenReturn(null)

        val exception = assertThrows(IllegalStateException::class.java) {
            IntentExtrasParser.parseApiType(intent)
        }

        assertTrue(exception.message!!.contains(Extras.API_TYPE))
    }

    @Test
    fun `when parseApiType with unknown type, then throws error`() {
        whenever(intent.getStringExtra(Extras.API_TYPE)).thenReturn("unknown-type")

        val exception = assertThrows(IllegalStateException::class.java) {
            IntentExtrasParser.parseApiType(intent)
        }

        assertTrue(exception.message!!.contains("unknown-type"))
    }

    @Test
    fun `when parseHttpMethod with GET, then returns GET`() {
        whenever(intent.getStringExtra(Extras.HTTP_METHOD)).thenReturn("GET")

        val result = IntentExtrasParser.parseHttpMethod(intent)

        assertEquals(HttpMethod.GET, result)
    }

    @Test
    fun `when parseHttpMethod with POST, then returns POST`() {
        whenever(intent.getStringExtra(Extras.HTTP_METHOD)).thenReturn("POST")

        val result = IntentExtrasParser.parseHttpMethod(intent)

        assertEquals(HttpMethod.POST, result)
    }

    @Test
    fun `when parseHttpMethod with lowercase input, then returns correct method`() {
        whenever(intent.getStringExtra(Extras.HTTP_METHOD)).thenReturn("delete")

        val result = IntentExtrasParser.parseHttpMethod(intent)

        assertEquals(HttpMethod.DELETE, result)
    }

    @Test
    fun `when parseHttpMethod with mixed case input, then returns correct method`() {
        whenever(intent.getStringExtra(Extras.HTTP_METHOD)).thenReturn("pAtCh")

        val result = IntentExtrasParser.parseHttpMethod(intent)

        assertEquals(HttpMethod.PATCH, result)
    }

    @Test
    fun `when parseHttpMethod with null, then returns null`() {
        whenever(intent.getStringExtra(Extras.HTTP_METHOD)).thenReturn(null)

        val result = IntentExtrasParser.parseHttpMethod(intent)

        assertNull(result)
    }

    @Test
    fun `when parseHttpMethod with invalid value, then throws exception`() {
        whenever(intent.getStringExtra(Extras.HTTP_METHOD)).thenReturn("INVALID")

        assertThrows(IllegalArgumentException::class.java) {
            IntentExtrasParser.parseHttpMethod(intent)
        }
    }

    @Test
    fun `when parseQueryParameters with null, then returns empty list`() {
        whenever(intent.getStringExtra(Extras.QUERY_PARAMS)).thenReturn(null)

        val result = IntentExtrasParser.parseQueryParameters(intent)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `when parseQueryParameters with empty String, then returns empty list`() {
        whenever(intent.getStringExtra(Extras.QUERY_PARAMS)).thenReturn("")

        val result = IntentExtrasParser.parseQueryParameters(intent)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `when parseQueryParameters with single param, then returns single parameter`() {
        whenever(intent.getStringExtra(Extras.QUERY_PARAMS)).thenReturn("key=value")

        val result = IntentExtrasParser.parseQueryParameters(intent)

        assertEquals(listOf(QueryParameter("key", "value")), result)
    }

    @Test
    fun `when parseQueryParameters with multiple params, then returns all parameters`() {
        whenever(intent.getStringExtra(Extras.QUERY_PARAMS)).thenReturn("key1=value1,key2=value2,key3=value3")

        val result = IntentExtrasParser.parseQueryParameters(intent)

        assertEquals(
            listOf(
                QueryParameter("key1", "value1"),
                QueryParameter("key2", "value2"),
                QueryParameter("key3", "value3")
            ),
            result
        )
    }

    @Test
    fun `when parseQueryParameters with spaces around values, then trims whitespace`() {
        whenever(intent.getStringExtra(Extras.QUERY_PARAMS)).thenReturn(" key = value ")

        val result = IntentExtrasParser.parseQueryParameters(intent)

        assertEquals(listOf(QueryParameter("key", "value")), result)
    }

    @Test
    fun `when parseQueryParameters with value containing equals sign, then splits only on first equals`() {
        whenever(intent.getStringExtra(Extras.QUERY_PARAMS)).thenReturn("key=val=ue")

        val result = IntentExtrasParser.parseQueryParameters(intent)

        assertEquals(listOf(QueryParameter("key", "val=ue")), result)
    }

    @Test
    fun `when parseQueryParameters with invalid format, then throws exception`() {
        whenever(intent.getStringExtra(Extras.QUERY_PARAMS)).thenReturn("invalid-no-equals")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            IntentExtrasParser.parseQueryParameters(intent)
        }

        assertTrue(exception.message!!.contains("invalid-no-equals"))
    }

    @Test
    fun `when parseResponseBody with inline body, then returns body string`() {
        whenever(intent.getStringExtra(Extras.RESPONSE_BODY_FILE)).thenReturn(null)
        whenever(intent.getStringExtra(Extras.RESPONSE_BODY)).thenReturn("{\"status\":\"ok\"}")

        val result = IntentExtrasParser.parseResponseBody(intent)

        assertEquals("{\"status\":\"ok\"}", result)
    }

    @Test
    fun `when parseResponseBody with file path, then returns file contents`() {
        val file = temporaryFolder.newFile("response.json")
        file.writeText("{\"from\":\"file\"}")
        whenever(intent.getStringExtra(Extras.RESPONSE_BODY_FILE)).thenReturn(file.absolutePath)

        val result = IntentExtrasParser.parseResponseBody(intent)

        assertEquals("{\"from\":\"file\"}", result)
    }

    @Test
    fun `when parseResponseBody with both file and inline, then file takes priority`() {
        val file = temporaryFolder.newFile("response.json")
        file.writeText("{\"from\":\"file\"}")
        whenever(intent.getStringExtra(Extras.RESPONSE_BODY_FILE)).thenReturn(file.absolutePath)
        whenever(intent.getStringExtra(Extras.RESPONSE_BODY)).thenReturn("{\"from\":\"inline\"}")

        val result = IntentExtrasParser.parseResponseBody(intent)

        assertEquals("{\"from\":\"file\"}", result)
    }

    @Test
    fun `when parseResponseBody with neither file nor inline, then returns null`() {
        whenever(intent.getStringExtra(Extras.RESPONSE_BODY_FILE)).thenReturn(null)
        whenever(intent.getStringExtra(Extras.RESPONSE_BODY)).thenReturn(null)

        val result = IntentExtrasParser.parseResponseBody(intent)

        assertNull(result)
    }

    @Test
    fun `when parseRequestBody with inline body, then returns body string`() {
        whenever(intent.getStringExtra(Extras.REQUEST_BODY_FILE)).thenReturn(null)
        whenever(intent.getStringExtra(Extras.REQUEST_BODY)).thenReturn("{\"name\":\"test\"}")

        val result = IntentExtrasParser.parseRequestBody(intent)

        assertEquals("{\"name\":\"test\"}", result)
    }

    @Test
    fun `when parseRequestBody with file path, then returns file contents`() {
        val file = temporaryFolder.newFile("request.json")
        file.writeText("{\"from\":\"request-file\"}")
        whenever(intent.getStringExtra(Extras.REQUEST_BODY_FILE)).thenReturn(file.absolutePath)

        val result = IntentExtrasParser.parseRequestBody(intent)

        assertEquals("{\"from\":\"request-file\"}", result)
    }

    @Test
    fun `when parseRequestBody with both file and inline, then file takes priority`() {
        val file = temporaryFolder.newFile("request.json")
        file.writeText("{\"from\":\"file\"}")
        whenever(intent.getStringExtra(Extras.REQUEST_BODY_FILE)).thenReturn(file.absolutePath)
        whenever(intent.getStringExtra(Extras.REQUEST_BODY)).thenReturn("{\"from\":\"inline\"}")

        val result = IntentExtrasParser.parseRequestBody(intent)

        assertEquals("{\"from\":\"file\"}", result)
    }

    @Test
    fun `when parseRequestBody with neither file nor inline, then returns null`() {
        whenever(intent.getStringExtra(Extras.REQUEST_BODY_FILE)).thenReturn(null)
        whenever(intent.getStringExtra(Extras.REQUEST_BODY)).thenReturn(null)

        val result = IntentExtrasParser.parseRequestBody(intent)

        assertNull(result)
    }
}
