/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
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
  json.addProperty(KeyConstants.ORDER, key.keyOrder)
  when (key.type.name) {
    TypeConstants.TEXT -> {
      json.addProperty(KeyConstants.KEY_TYPE, key.type.name)
      json.addProperty(KeyConstants.DEFAULT, key.defaultStringValue!!)
    }
    TypeConstants.NUMBER -> {
      json.addProperty(KeyConstants.KEY_TYPE, key.type.name)
      json.addProperty(KeyConstants.DEFAULT, key.defaultLongValue!!)
    }
    TypeConstants.DECIMAL -> {
      json.addProperty(KeyConstants.KEY_TYPE, key.type.name)
      json.addProperty(KeyConstants.DEFAULT, key.defaultDecimalValue!!)
    }
    TypeConstants.BOOLEAN -> {
      json.addProperty(KeyConstants.KEY_TYPE, key.type.name)
      json.addProperty(KeyConstants.DEFAULT, key.defaultBooleanValue!!)
    }
    TypeConstants.DATE -> {
      json.addProperty(KeyConstants.KEY_TYPE, key.type.name)
      json.addProperty(KeyConstants.DEFAULT, key.defaultTimeValue.toString())
    }
    TypeConstants.TIME -> {
      json.addProperty(KeyConstants.KEY_TYPE, key.type.name)
      json.addProperty(KeyConstants.DEFAULT, key.defaultTimestampValue.toString())
    }
    TypeConstants.TIMESTAMP -> {
      json.addProperty(KeyConstants.KEY_TYPE, key.type.name)
      json.addProperty(KeyConstants.DEFAULT, key.defaultDateValue.toString())
    }
    TypeConstants.BLOB -> {
      json.addProperty(KeyConstants.KEY_TYPE, key.type.name)
      json.addProperty(KeyConstants.DEFAULT, key.defaultBlobValue.toString())
    }
    TypeConstants.FORMULA -> {
      json.addProperty(KeyConstants.KEY_TYPE, key.type.name)
      json.addProperty(KeyConstants.FORMULA_RETURN_TYPE, key.formula!!.returnType.name)
      json.add(KeyConstants.FORMULA_EXPRESSION, gson.fromJson(key.formula!!.expression, JsonObject::class.java))
    }
    else -> {
      json.addProperty(KeyConstants.KEY_TYPE, key.type.name)
      if (key.referencedVariable != null)
        json.add(KeyConstants.DEFAULT, serialize(key.referencedVariable!!))
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
