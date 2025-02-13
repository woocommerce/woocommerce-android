package org.wordpress.android.fluxc.utils

import com.google.gson.Gson
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.JsonAdapter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JsonElementToLongSerializerDeserializerTest {
    private val sut = JsonElementToLongSerializerDeserializer()

    @Test
    fun `when value is true then null value is returned`() {
        val jsonElement = JsonPrimitive(true)
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isNull()
    }

    @Test
    fun `when value is a valid string number then the value is returned`() {
        val value = 12345L
        val valueString = value.toString()
        val jsonElement = JsonPrimitive(valueString)
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isEqualTo(value)
    }

    @Test
    fun `when value is an invalid string number then the value is returned`() {
        val jsonElement = JsonPrimitive("12345_test")
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isNull()
    }

    @Test
    fun `when value is different number format then the value is parsed to long and returned`() {
        val value = 12345L
        val valueInt = value.toDouble()
        val jsonElement = JsonPrimitive(valueInt)
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isEqualTo(value)
    }

    @Test
    fun `test serialization deserialization`() {
        val gson = Gson()
        val value = 12345L
        val json = gson.toJson(TestData(value))
        val deserialized = gson.fromJson(json, TestData::class.java)
        assertThat(deserialized.value).isEqualTo(value)
    }

    data class TestData(
        @JsonAdapter(JsonElementToLongSerializerDeserializer::class)
        val value: Long
    )
}
