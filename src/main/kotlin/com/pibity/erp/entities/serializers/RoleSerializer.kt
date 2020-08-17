package com.pibity.erp.entities.serializers

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.pibity.erp.entities.Role

class RoleSerializer : JsonSerializer<Role> {
  override fun serialize(src: Role?, typeOfSrc: java.lang.reflect.Type?, context: JsonSerializationContext?): JsonElement {
    val json = JsonObject()
    if (src != null) {
      json.addProperty("role", src.id.name)
    }
    return json
  }
}