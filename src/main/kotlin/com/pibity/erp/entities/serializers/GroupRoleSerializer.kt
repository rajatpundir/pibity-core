package com.pibity.erp.entities.serializers

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.pibity.erp.commons.gson
import com.pibity.erp.entities.GroupRole

class GroupRoleSerializer : JsonSerializer<GroupRole> {
  override fun serialize(src: GroupRole?, typeOfSrc: java.lang.reflect.Type?, context: JsonSerializationContext?): JsonElement {
    val json = JsonObject()
    if (src != null) {
      return (gson.fromJson(gson.toJson(src.id.role), JsonObject::class.java))
    }
    return json
  }
}
