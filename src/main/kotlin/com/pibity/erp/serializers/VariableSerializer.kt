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
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.entities.Variable

fun serialize(src: Variable): JsonObject {
  val json = JsonObject()
  if (src.id.type.id.superTypeName == "Any") {
    json.addProperty("organization", src.id.type.id.organization.id)
    json.addProperty("active", src.active)
  } else
    json.addProperty("context", src.id.superList.id)
  json.addProperty("typeName", src.id.type.id.name)
  json.addProperty("variableName", src.id.name)
  val jsonValues = JsonObject()
  for (value in src.values) {
    when (value.id.key.type.id.name) {
      TypeConstants.TEXT -> jsonValues.addProperty(value.id.key.id.name, value.stringValue!!)
      TypeConstants.NUMBER -> jsonValues.addProperty(value.id.key.id.name, value.longValue!!)
      TypeConstants.DECIMAL -> jsonValues.addProperty(value.id.key.id.name, value.doubleValue!!)
      TypeConstants.BOOLEAN -> jsonValues.addProperty(value.id.key.id.name, value.booleanValue!!)
      TypeConstants.FORMULA -> {
        when (value.id.key.formula!!.returnType.id.name) {
          TypeConstants.TEXT -> jsonValues.addProperty(value.id.key.id.name, value.stringValue!!)
          TypeConstants.NUMBER -> jsonValues.addProperty(value.id.key.id.name, value.longValue!!)
          TypeConstants.DECIMAL -> jsonValues.addProperty(value.id.key.id.name, value.doubleValue!!)
          TypeConstants.BOOLEAN -> jsonValues.addProperty(value.id.key.id.name, value.booleanValue!!)
        }
      }
      TypeConstants.LIST -> jsonValues.add(value.id.key.id.name, serialize(value.list!!.variables))
      else -> {
        if (value.referencedVariable!!.id.type.id.superTypeName == "Any")
          jsonValues.addProperty(value.id.key.id.name, value.referencedVariable!!.id.name)
        else
          jsonValues.add(value.id.key.id.name, serialize(value.referencedVariable!!))
      }
    }
  }
  json.add("values", jsonValues)
  return json
}

fun serialize(src: Set<Variable>): JsonArray {
  val json = JsonArray()
  for (entity in src)
    json.add(serialize(entity))
  return json
}

fun serialize(src: List<Variable>): JsonArray {
  val json = JsonArray()
  for (entity in src)
    json.add(serialize(entity))
  return json
}
