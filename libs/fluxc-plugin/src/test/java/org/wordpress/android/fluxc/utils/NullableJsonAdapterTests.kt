package org.wordpress.android.fluxc.utils

import com.google.gson.Gson
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.MalformedJsonException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import java.math.BigDecimal

class NullableJsonAdapterTests {
    data class StringExample(
        @JsonAdapter(NullStringJsonAdapter::class, nullSafe = false)
        @SerializedName("an_id")
        val id: String?
    )

    data class NumberExample(
        @JsonAdapter(NullBigDecimalJsonAdapter::class, nullSafe = false)
        @SerializedName("amount")
        val amount: BigDecimal?
    )

    private val gson = Gson()

    @Test
    fun `when passing null string in json, then it should be deserialized to null value`() {
        val json = """{
            "an_id": null
            }"""

        val example = gson.fromJson(json, StringExample::class.java)

        assertThat(example.id).isNull()
    }

    @Test
    fun `when serializing a null string value, then it should be exposed to the json`() {
        val example = StringExample(null)

        val json = gson.toJson(example)

        assertThat(json).contains(""""an_id":null""")
    }

    @Test
    fun `when passing non-null string value in json, then it should be deserialized to the correct value`() {
        val json = """{
            "an_id": "some_id"
            }"""

        val example = gson.fromJson(json, StringExample::class.java)

        assertThat(example.id).isEqualTo("some_id")
    }

    @Test
    fun `when serializing a non-null string value, then it should be correctly serialized`() {
        val example = StringExample("some_id")

        val json = gson.toJson(example)

        assertThat(json).contains(""""an_id":"some_id"""")
    }

    @Test
    fun `when passing null number in json, then it should be deserialized to null value`() {
        val json = """{
            "amount": null
            }"""

        val example = gson.fromJson(json, NumberExample::class.java)

        assertThat(example.amount).isNull()
    }

    @Test
    fun `when passing non-null number value in json, then it should be deserialized to the correct value`() {
        val json = """{
            "amount": 100.50
            }"""

        val example = gson.fromJson(json, NumberExample::class.java)

        assertThat(example.amount).isEqualByComparingTo(BigDecimal("100.50"))
    }

    @Test
    fun `when passing non-null number string value in json, then it should be deserialized to the correct value`() {
        val json = """{
            "amount": "100.50"
            }"""

        val example = gson.fromJson(json, NumberExample::class.java)

        assertThat(example.amount).isEqualByComparingTo(BigDecimal("100.50"))
    }

    @Test
    fun `when passing invalid number string value in json, then it should throw`() {
        val json = """{
            "amount": "invalid"
            }"""

        assertThatThrownBy { gson.fromJson(json, NumberExample::class.java).amount }
            .hasCauseInstanceOf(MalformedJsonException::class.java)
    }

    @Test
    fun `when serializing a null number value, then it should be exposed to the json`() {
        val example = NumberExample(null)

        val json = gson.toJson(example)

        assertThat(json).contains(""""amount":null""")
    }

    @Test
    fun `when serializing a non-null number value, then it should be correctly serialized`() {
        val example = NumberExample(BigDecimal("100.50"))

        val json = gson.toJson(example)

        assertThat(json).contains(""""amount":100.50""")
    }
}
