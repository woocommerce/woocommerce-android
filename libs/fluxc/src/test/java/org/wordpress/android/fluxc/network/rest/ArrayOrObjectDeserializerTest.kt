package org.wordpress.android.fluxc.network.rest

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArrayOrObjectDeserializerTest {
    private fun gson() = GsonBuilder()
        .registerTypeHierarchyAdapter(Array<Any>::class.java, ArrayOrObjectDeserializer())
        .create()

    data class Item(val a: Int, val b: String)

    @Test
    fun `when array of primitives is deserialized, then returns array of ints`() {
        val json = "[1, 2, 3]"
        val type = object : TypeToken<Array<Int>>() {}.type
        val result: Array<Int> = gson().fromJson(json, type)
        assertArrayEquals(arrayOf(1, 2, 3), result)
    }

    @Test
    fun `when array of objects is deserialized, then returns array of data class`() {
        val json = """[{"a":1,"b":"x"},{"a":2,"b":"y"}]"""
        val type = object : TypeToken<Array<Item>>() {}.type
        val result: Array<Item> = gson().fromJson(json, type)
        assertArrayEquals(arrayOf(Item(1, "x"), Item(2, "y")), result)
    }

    @Test
    fun `when object has numeric string keys, then converted to ordered array`() {
        val json = """{"2":"a","1":"b","10":"z"}"""
        val type = object : TypeToken<Array<String>>() {}.type
        val result: Array<String> = gson().fromJson(json, type)
        // Keys sorted numerically: 1,2,10 -> values b,a,z
        assertArrayEquals(arrayOf("b", "a", "z"), result)
    }

    @Test(expected = JsonParseException::class)
    fun `when object has non numeric key, then throws json parse exception`() {
        val json = """{"a":1,"2":2}"""
        val type = object : TypeToken<Array<Int>>() {}.type
        gson().fromJson<Array<Int>>(json, type)
    }

    @Test
    fun `when json is null, then returns null array`() {
        val json = "null"
        val type = object : TypeToken<Array<Int>?>() {}.type
        val result: Array<Int>? = gson().fromJson(json, type)
        assertNull(result)
    }

    @Test(expected = JsonParseException::class)
    fun `when json type is string, then throws json parse exception`() {
        val json = "\"not an array or object\""
        val type = object : TypeToken<Array<Int>>() {}.type
        gson().fromJson<Array<Int>>(json, type)
    }

    @Test
    fun `when object has numeric keys and objects, then converted to ordered array of objects`() {
        val json = """{"2":{"a":2,"b":"y"},"1":{"a":1,"b":"x"}}"""
        val type = object : TypeToken<Array<Item>>() {}.type
        val result: Array<Item> = gson().fromJson(json, type)
        assertEquals(listOf(Item(1, "x"), Item(2, "y")), result.toList())
    }
}
