/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.serializers

import com.google.gson.*
import com.pibity.erp.commons.gson
import com.pibity.erp.entities.Type

class TypeSerializer : JsonSerializer<Type> {
  override fun serialize(src: Type?, typeOfSrc: java.lang.reflect.Type?, context: JsonSerializationContext?): JsonElement {
    val json = JsonObject()
    if (src != null) {
      if (src.id.superTypeName == "Any")
        json.addProperty("organizationId", src.id.organization.id)
      else
        json.addProperty("superTypeName", src.id.superTypeName)
      json.addProperty("typeName", src.id.name)
      json.addProperty("displayName", src.displayName)
      val jsonKeys = JsonObject()
      for (key in src.keys)
        jsonKeys.add(key.id.name, gson.fromJson(gson.toJson(key), JsonObject::class.java))
      json.add("keys", jsonKeys)
      if (src.id.superTypeName == "Any")
        json.add("permissions", gson.fromJson(gson.toJson(src.permissions), JsonArray::class.java))
    }
    return json
  }
}
