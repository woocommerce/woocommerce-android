package org.wordpress.android.fluxc.utils

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PrimitiveBooleanJsonDeserializerTest {
    private val sut = PrimitiveBooleanJsonDeserializer()

    @Test
    fun `when value is true then true value is returned`() {
        val jsonElement = JsonPrimitive(true)
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isTrue
    }

    @Test
    fun `when value is false then false value is returned`() {
        val jsonElement = JsonPrimitive(false)
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isFalse
    }

    @Test
    fun `when value is 0 then false value is returned`() {
        val jsonElement = JsonPrimitive(0)
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isFalse
    }

    @Test
    fun `when value is 1 then true value is returned`() {
        val jsonElement = JsonPrimitive(1)
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isTrue
    }

    @Test
    fun `when value is a number and is not 1 then null value is returned`() {
        val jsonElement = JsonPrimitive(189)
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isNull()
    }

    @Test
    fun `when value is a string number and is 1 then true value is returned`() {
        val jsonElement = JsonPrimitive("1")
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isTrue
    }

    @Test
    fun `when value is a string number and is 0 then false value is returned`() {
        val jsonElement = JsonPrimitive("0")
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isFalse
    }

    @Test
    fun `when value is a string number and is 1 with spaces then true value is returned`() {
        val jsonElement = JsonPrimitive("  1 ")
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isTrue
    }

    @Test
    fun `when value is a string number and is 0 with spaces then false value is returned`() {
        val jsonElement = JsonPrimitive(" 0  ")
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isFalse
    }

    @Test
    fun `when value is a string boolean and is true then true value is returned`() {
        val jsonElement = JsonPrimitive("true")
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isTrue
    }

    @Test
    fun `when value is a string boolean and is false then false value is returned`() {
        val jsonElement = JsonPrimitive("false")
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isFalse
    }

    @Test
    fun `when value is a string boolean and is true with spaces then true value is returned`() {
        val jsonElement = JsonPrimitive(" true  ")
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isTrue
    }

    @Test
    fun `when value is a string boolean and is false with spaces then false value is returned`() {
        val jsonElement = JsonPrimitive("  false ")
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isFalse
    }

    @Test
    fun `when value is not valid then null is returned`() {
        val jsonElement = JsonObject()
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isNull()
    }

    @Test
    fun `when value is null then null is returned`() {
        val jsonElement = JsonNull.INSTANCE
        val result = sut.deserialize(
            json = jsonElement,
            typeOfT = null,
            context = null
        )
        assertThat(result).isNull()
    }
}
