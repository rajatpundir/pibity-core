/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.serializers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.KeyConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.entities.Key

fun serialize(src: Key): JsonObject {
  val json = JsonObject()
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
      json.add(KeyConstants.LIST_TYPE, serialize(src.list!!.type))
    }
    else -> {
      if (src.type.id.superTypeName == "Any") {
        json.addProperty(KeyConstants.KEY_TYPE, src.type.id.name)
        if (src.referencedVariable != null)
          json.add(KeyConstants.DEFAULT, serialize(src.referencedVariable!!))
      } else json.add(KeyConstants.KEY_TYPE, serialize(src.type))
    }
  }
  return json
}

fun serialize(src: Set<Key>): JsonArray {
  val json = JsonArray()
  for (entity in src)
    json.add(serialize(entity))
  return json
}
