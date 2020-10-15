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
import com.pibity.erp.commons.utils.gson
import com.pibity.erp.entities.Key

fun serialize(key: Key): JsonObject {
  val json = JsonObject()
  json.addProperty(KeyConstants.DISPLAY_NAME, key.displayName)
  json.addProperty(KeyConstants.ORDER, key.keyOrder)
  when (key.type.id.name) {
    TypeConstants.TEXT -> {
      json.addProperty(KeyConstants.KEY_TYPE, key.type.id.name)
      json.addProperty(KeyConstants.DEFAULT, key.defaultStringValue)
    }
    TypeConstants.NUMBER -> {
      json.addProperty(KeyConstants.KEY_TYPE, key.type.id.name)
      json.addProperty(KeyConstants.DEFAULT, key.defaultLongValue)
    }
    TypeConstants.DECIMAL -> {
      json.addProperty(KeyConstants.KEY_TYPE, key.type.id.name)
      json.addProperty(KeyConstants.DEFAULT, key.defaultDoubleValue)
    }
    TypeConstants.BOOLEAN -> {
      json.addProperty(KeyConstants.KEY_TYPE, key.type.id.name)
      json.addProperty(KeyConstants.DEFAULT, key.defaultBooleanValue)
    }
    TypeConstants.FORMULA -> {
      json.addProperty(KeyConstants.KEY_TYPE, key.type.id.name)
      json.addProperty(KeyConstants.FORMULA_RETURN_TYPE, key.formula?.returnType?.id?.name)
      json.add(KeyConstants.FORMULA_EXPRESSION, gson.fromJson(key.formula?.expression, JsonObject::class.java))
      json.add(KeyConstants.FORMULA_SYMBOLS, gson.fromJson(key.formula?.symbolPaths, JsonArray::class.java))
    }
    TypeConstants.LIST -> {
      json.addProperty(KeyConstants.KEY_TYPE, key.type.id.name)
      json.add(KeyConstants.LIST_TYPE, serialize(key.list!!.type))
    }
    else -> {
      if (key.type.id.superTypeName == "Any") {
        json.addProperty(KeyConstants.KEY_TYPE, key.type.id.name)
        if (key.referencedVariable != null)
          json.add(KeyConstants.DEFAULT, serialize(key.referencedVariable!!))
      } else json.add(KeyConstants.KEY_TYPE, serialize(key.type))
    }
  }
  return json
}

fun serialize(entities: Set<Key>): JsonArray {
  val json = JsonArray()
  for (entity in entities)
    json.add(serialize(entity))
  return json
}
