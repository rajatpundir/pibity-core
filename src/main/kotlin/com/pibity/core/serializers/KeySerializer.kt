/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.serializers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.core.commons.constants.KeyConstants
import com.pibity.core.commons.constants.TypeConstants
import com.pibity.core.commons.utils.gson
import com.pibity.core.entities.Key

fun serialize(key: Key): JsonObject = JsonObject().apply {
  addProperty(KeyConstants.ORDER, key.keyOrder)
  addProperty(KeyConstants.KEY_TYPE, key.type.name)
  when (key.type.name) {
    TypeConstants.TEXT -> addProperty(KeyConstants.DEFAULT, key.defaultStringValue!!)
    TypeConstants.NUMBER -> addProperty(KeyConstants.DEFAULT, key.defaultLongValue!!)
    TypeConstants.DECIMAL -> addProperty(KeyConstants.DEFAULT, key.defaultDecimalValue!!)
    TypeConstants.BOOLEAN -> addProperty(KeyConstants.DEFAULT, key.defaultBooleanValue!!)
    TypeConstants.DATE -> addProperty(KeyConstants.DEFAULT, key.defaultDateValue!!.time)
    TypeConstants.TIMESTAMP -> addProperty(KeyConstants.DEFAULT, key.defaultTimestampValue!!.time)
    TypeConstants.TIME -> addProperty(KeyConstants.DEFAULT, key.defaultTimeValue!!.time)
    TypeConstants.BLOB -> addProperty(KeyConstants.DEFAULT, key.defaultBlobValue.toString())
    TypeConstants.FORMULA -> {
      addProperty(KeyConstants.FORMULA_RETURN_TYPE, key.formula!!.returnType.name)
      add(KeyConstants.FORMULA_EXPRESSION, gson.fromJson(key.formula!!.expression, JsonObject::class.java))
    }
    else -> if (key.referencedVariable != null) addProperty(KeyConstants.DEFAULT, key.referencedVariable!!.name)
  }
}

fun serialize(entities: Set<Key>): JsonArray = entities.fold(JsonArray()) { acc, entity -> acc.apply { add(serialize(entity)) } }
