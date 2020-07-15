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
import com.pibity.erp.entities.Category
import java.lang.reflect.Type

class CategorySerializer : JsonSerializer<Category> {
  override fun serialize(src: Category?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
    val json = JsonObject()
    if (src != null) {
      json.addProperty("id", src.id)
      json.addProperty("parent", src.parent?.id)
      json.addProperty("name", src.name)
      json.addProperty("nextLabel", src.nextLabel)
      json.addProperty("code", src.code)
    }
    return json
  }
}
