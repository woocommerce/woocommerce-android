package org.wordpress.android.fluxc.model

import com.google.gson.JsonObject

class WCProductImageModel(val id: Long) {
    var dateCreated: String = ""
    var src: String = ""
    var alt: String = ""
    var name: String = ""

    fun toJson(): JsonObject {
        return JsonObject().also { json ->
            json.addProperty("id", id)
            // If id == 0 then the variation image has been deleted. Don't include the other
            // fields or this delete request will fail.
            if (id > 0) {
                json.addProperty("date_created", dateCreated)
                json.addProperty("src", src)
                json.addProperty("alt", alt)
                json.addProperty("name", name)
            }
        }
    }
}
