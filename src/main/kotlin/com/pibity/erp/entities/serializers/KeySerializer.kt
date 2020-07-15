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
import com.pibity.erp.commons.constants.KeyConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.gson
import com.pibity.erp.entities.Key
import java.lang.reflect.Type

class KeySerializer : JsonSerializer<Key> {
  override fun serialize(src: Key?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
    val json = JsonObject()
    if (src != null) {
      json.addProperty(KeyConstants.DISPLAY_NAME, src.displayName)
      json.addProperty(KeyConstants.ORDER, src.keyOrder)
      when (src.type.id.name) {
        TypeConstants.TEXT -> {
          json.addProperty(KeyConstants.KEY_TYPE, src.type.id.name)
          json.addProperty(KeyConstants.DEFAULT, src.defaultStringValue)
        }
        TypeConstants.NUMBER -> {
          json.addProperty(KeyConstants.KEY_TYPE, src.type.id.name)
          json.addProperty(KeyConstants.DEFAULT, src.defaultLongValue)
        }
        TypeConstants.DECIMAL -> {
          json.addProperty(KeyConstants.KEY_TYPE, src.type.id.name)
          json.addProperty(KeyConstants.DEFAULT, src.defaultDoubleValue)
        }
        TypeConstants.BOOLEAN -> {
          json.addProperty(KeyConstants.KEY_TYPE, src.type.id.name)
          json.addProperty(KeyConstants.DEFAULT, src.defaultBooleanValue)
        }
        TypeConstants.FORMULA -> {
          json.addProperty(KeyConstants.KEY_TYPE, src.type.id.name)
          json.addProperty(KeyConstants.FORMULA_EXPRESSION, src.formula?.expression)
          json.addProperty(KeyConstants.FORMULA_RETURN_TYPE, src.formula?.returnType?.id?.name)
        }
        TypeConstants.LIST -> {
          json.addProperty(KeyConstants.KEY_TYPE, src.type.id.name)
          json.add(KeyConstants.LIST_TYPE, gson.fromJson(gson.toJson(src.list?.type), JsonObject::class.java))
        }
        else -> {
          json.add(KeyConstants.KEY_TYPE, gson.fromJson(gson.toJson(src.type), JsonObject::class.java))
          if (src.referencedVariable != null)
            json.add(KeyConstants.DEFAULT, gson.fromJson(gson.toJson(src.referencedVariable), JsonObject::class.java))
        }
      }
    }
    return json
  }
}
