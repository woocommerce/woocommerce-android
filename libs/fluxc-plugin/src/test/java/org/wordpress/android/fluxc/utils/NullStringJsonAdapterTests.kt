package org.wordpress.android.fluxc.utils

import com.google.gson.Gson
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NullStringJsonAdapterTests {
    data class Example(
        @JsonAdapter(NullStringJsonAdapter::class, nullSafe = false)
        @SerializedName("an_id")
        val id: String?
    )

    private val gson = Gson()

    @Test
    fun `when passing null in json, then it should be deserialized to null value`() {
        val json = """{
            "an_id": null
            }"""

        val example = gson.fromJson(json, Example::class.java)

        assertThat(example.id).isNull()
    }

    @Test
    fun `when serializing a null value, then it should be exposed to the json`() {
        val example = Example(null)

        val json = gson.toJson(example)

        assertThat(json).contains(""""an_id":null""")
    }

    @Test
    fun `when passing non-null value in json, then it should be deserialized to the correct value`() {
        val json = """{
            "an_id": "some_id"
            }"""

        val example = gson.fromJson(json, Example::class.java)

        assertThat(example.id).isEqualTo("some_id")
    }

    @Test
    fun `when serializing a non-null value, then it should be correctly serialized`() {
        val example = Example("some_id")

        val json = gson.toJson(example)

        assertThat(json).contains(""""an_id":"some_id"""")
    }
}
