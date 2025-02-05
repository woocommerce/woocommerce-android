package org.wordpress.android.fluxc.utils

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.util.ArrayDeque
import java.util.Deque

internal fun json(build: JsonObjectBuilder.() -> Unit): JsonObject {
    return JsonObjectBuilder().json(build)
}

internal class JsonObjectBuilder {
    private val deque: Deque<JsonObject> = ArrayDeque()

    fun json(build: JsonObjectBuilder.() -> Unit): JsonObject {
        deque.push(JsonObject())
        this.build()
        return deque.pop()
    }

    infix fun String.To(value: Int) {
        deque.peek().addProperty(this, value)
    }

    infix fun String.To(value: String) {
        deque.peek().addProperty(this, value)
    }

    infix fun String.To(value: JsonElement) {
        deque.peek().add(this, value)
    }

    infix fun String.To(value: Collection<JsonElement>) {
        deque.peek().add(
                this,
                JsonArray().apply {
                    value.forEach { item -> add(item) }
                }
        )
    }

    infix fun String.To(value: IntArray) {
        deque.peek().add(this,
                JsonArray().apply {
                    value.forEach { item ->
                        add(item)
                    }
                }
        )
    }
}
