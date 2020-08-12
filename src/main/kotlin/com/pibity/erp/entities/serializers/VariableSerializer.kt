/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.serializers

import com.google.gson.*
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.gson
import com.pibity.erp.entities.Variable
import java.lang.reflect.Type

class VariableSerializer : JsonSerializer<Variable> {
  override fun serialize(src: Variable?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
    val json = JsonObject()
    if (src != null) {
      if (src.id.type.id.superTypeName == "Any")
        json.addProperty("organization", src.id.type.id.organization.id)
      else
        json.addProperty("context", src.id.superList.id)
      json.addProperty("variableName", src.id.name)
      json.addProperty("typeName", src.id.type.id.name)
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
          TypeConstants.LIST -> jsonValues.add(value.id.key.id.name, gson.fromJson(gson.toJson(value.list!!.variables), JsonArray::class.java))
          else -> {
            if (value.referencedVariable!!.id.type.id.superTypeName == "Any")
              jsonValues.addProperty(value.id.key.id.name, value.referencedVariable!!.id.name)
            else
              jsonValues.add(value.id.key.id.name, gson.fromJson(gson.toJson(value.referencedVariable!!), JsonObject::class.java))
          }
        }
      }
      json.add("values", jsonValues)
    }
    return json
  }
}
