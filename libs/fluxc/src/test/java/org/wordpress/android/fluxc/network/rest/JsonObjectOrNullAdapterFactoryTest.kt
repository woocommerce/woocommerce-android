package org.wordpress.android.fluxc.network.rest

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonObjectOrNullAdapterFactoryTest {
    data class SampleModel(val id: Int, val name: String?) : JsonObjectOrNull()

    private fun gson(): Gson = GsonBuilder()
        .registerTypeAdapterFactory(JsonObjectOrNullAdapterFactory())
        .create()

    @Test
    fun `when json is a valid object, then it is deserialized`() {
        val json = """{"id": 7, "name": "woo"}"""
        val result: SampleModel = gson().fromJson(json, SampleModel::class.java)
        assertEquals(SampleModel(7, "woo"), result)
    }

    @Test
    fun `when json is an empty array, then returns null`() {
        val json = "[]"
        val result: SampleModel? = gson().fromJson(json, SampleModel::class.java)
        assertNull(result)
    }

    @Test
    fun `when json is null, then returns null`() {
        val json = "null"
        val result: SampleModel? = gson().fromJson(json, SampleModel::class.java)
        assertNull(result)
    }

    @Test
    fun `when json is a primitive string, then returns null`() {
        val json = "\"primitive\""
        val result: SampleModel? = gson().fromJson(json, SampleModel::class.java)
        assertNull(result)
    }

    @Test
    fun `when json is a primitive number, then returns null`() {
        val json = "123"
        val result: SampleModel? = gson().fromJson(json, SampleModel::class.java)
        assertNull(result)
    }

    @Test
    fun `when json is a primitive boolean, then returns null`() {
        val json = "true"
        val result: SampleModel? = gson().fromJson(json, SampleModel::class.java)
        assertNull(result)
    }
}
