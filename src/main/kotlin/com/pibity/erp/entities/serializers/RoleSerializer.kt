/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

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