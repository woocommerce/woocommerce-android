package org.wordpress.android.fluxc.utils

import com.google.gson.JsonPrimitive
import org.assertj.core.api.Assertions
import org.junit.Test

class NonNegativeDoubleJsonDeserializerTest {
    private val sut = NonNegativeDoubleJsonDeserializer()

    @Test
    fun `when value is true then null value is returned`() {
        val jsonElement = JsonPrimitive(true)
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        Assertions.assertThat(result).isNull()
    }

    @Test
    fun `when value is a valid string number then the value is returned`() {
        val value = 12345.0
        val valueString = value.toString()
        val jsonElement = JsonPrimitive(valueString)
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        Assertions.assertThat(result).isEqualTo(value)
    }

    @Test
    fun `when value is an invalid string number then the value is returned`() {
        val jsonElement = JsonPrimitive("12345_test")
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        Assertions.assertThat(result).isNull()
    }

    @Test
    fun `when value is less than 0 then null value is returned`() {
        val value = -5.0
        val jsonElement = JsonPrimitive(value)
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        Assertions.assertThat(result).isNull()
    }

    @Test
    fun `when value is greater than 0 then the value is returned`() {
        val value = 12345.0
        val jsonElement = JsonPrimitive(value)
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        Assertions.assertThat(result).isEqualTo(value)
    }

    @Test
    fun `when value is different number format then the value is parsed to double and returned`() {
        val value = 12345.0
        val valueInt = value.toInt()
        val jsonElement = JsonPrimitive(valueInt)
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        Assertions.assertThat(result).isEqualTo(value)
    }
}
