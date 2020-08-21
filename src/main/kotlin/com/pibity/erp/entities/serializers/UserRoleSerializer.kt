package com.pibity.erp.entities.serializers

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.pibity.erp.commons.gson
import com.pibity.erp.entities.UserRole

class UserRoleSerializer : JsonSerializer<UserRole> {
  override fun serialize(src: UserRole?, typeOfSrc: java.lang.reflect.Type?, context: JsonSerializationContext?): JsonElement {
    val json = JsonObject()
    if (src != null) {
      return (gson.fromJson(gson.toJson(src.id.role), JsonObject::class.java))
    }
    return json
  }
}
