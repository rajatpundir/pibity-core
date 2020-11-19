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
import com.pibity.erp.commons.constants.GLOBAL_TYPE
import com.pibity.erp.commons.constants.KeyConstants
import com.pibity.erp.commons.constants.PermissionConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.entities.permission.TypePermission
import com.pibity.erp.entities.Variable

fun serialize(variable: Variable): JsonObject {
  val json = JsonObject()
  if (variable.type.superTypeName == "Any") {
    json.addProperty("orgId", variable.type.organization.id)
    json.addProperty("typeName", variable.type.name)
    json.addProperty("active", variable.active)
  } else
    json.addProperty("context", variable.superList.id)
  json.addProperty("variableName", variable.name)
//  json.addProperty("version", variable.version.time)
  val jsonValues = JsonObject()
  for (value in variable.values) {
    when (value.key.type.name) {
      TypeConstants.TEXT -> jsonValues.addProperty(value.key.name, value.stringValue!!)
      TypeConstants.NUMBER -> jsonValues.addProperty(value.key.name, value.longValue!!)
      TypeConstants.DECIMAL -> jsonValues.addProperty(value.key.name, value.doubleValue!!)
      TypeConstants.BOOLEAN -> jsonValues.addProperty(value.key.name, value.booleanValue!!)
      TypeConstants.FORMULA -> {
        when (value.key.formula!!.returnType.name) {
          TypeConstants.TEXT -> jsonValues.addProperty(value.key.name, value.stringValue!!)
          TypeConstants.NUMBER -> jsonValues.addProperty(value.key.name, value.longValue!!)
          TypeConstants.DECIMAL -> jsonValues.addProperty(value.key.name, value.doubleValue!!)
          TypeConstants.BOOLEAN -> jsonValues.addProperty(value.key.name, value.booleanValue!!)
        }
      }
      TypeConstants.LIST -> jsonValues.add(value.key.name, serialize(value.list!!.variables))
      else -> {
        if (value.referencedVariable!!.type.superTypeName == "Any")
          jsonValues.addProperty(value.key.name, value.referencedVariable!!.name)
        else
          jsonValues.add(value.key.name, serialize(value.referencedVariable!!))
      }
    }
  }
  json.add("values", jsonValues)
  return json
}

fun serialize(entities: Set<Variable>): JsonArray {
  val json = JsonArray()
  for (entity in entities)
    json.add(serialize(entity))
  return json
}

fun serialize(entities: List<Variable>): JsonArray {
  val json = JsonArray()
  for (entity in entities)
    json.add(serialize(entity))
  return json
}

fun serialize(variable: Variable, typePermission: TypePermission): JsonObject {
  val json = JsonObject()
  if (variable.type.superTypeName == "Any") {
    json.addProperty("organization", variable.type.organization.id)
    json.addProperty("typeName", variable.type.name)
    json.addProperty("active", variable.active)
  } else
    json.addProperty("context", variable.superList.id)
  json.addProperty("variableName", variable.name)
  val jsonValues = JsonObject()
  for (value in variable.values) {
    when (value.key.type.name) {
      TypeConstants.TEXT -> {
        if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
          jsonValues.addProperty(value.key.name, value.stringValue!!)
      }
      TypeConstants.NUMBER -> {
        if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
          jsonValues.addProperty(value.key.name, value.longValue!!)
      }
      TypeConstants.DECIMAL -> {
        if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
          jsonValues.addProperty(value.key.name, value.doubleValue!!)
      }
      TypeConstants.BOOLEAN -> {
        if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
          jsonValues.addProperty(value.key.name, value.booleanValue!!)
      }
      TypeConstants.FORMULA -> {
        if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS) {
          when (value.key.formula!!.returnType.name) {
            TypeConstants.TEXT -> jsonValues.addProperty(value.key.name, value.stringValue!!)
            TypeConstants.NUMBER -> jsonValues.addProperty(value.key.name, value.longValue!!)
            TypeConstants.DECIMAL -> jsonValues.addProperty(value.key.name, value.doubleValue!!)
            TypeConstants.BOOLEAN -> jsonValues.addProperty(value.key.name, value.booleanValue!!)
          }
        }
      }
      TypeConstants.LIST -> {
        if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
          jsonValues.add(value.key.name, serialize(value.list!!.variables, typePermission.keyPermissions.single { it.key == value.key }.referencedTypePermission!!))
      }
      else -> {
        if (value.referencedVariable!!.type.superTypeName == "Any") {
          if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
            jsonValues.addProperty(value.key.name, value.referencedVariable!!.name)
        }
        else {
          if ((value.key.parentType.superTypeName == GLOBAL_TYPE && value.key.parentType.name == value.key.type.superTypeName)
                  || (value.key.parentType.superTypeName != GLOBAL_TYPE && value.key.parentType.superTypeName == value.key.type.superTypeName)) {
            val variableJson: JsonObject = serialize(value.referencedVariable!!, typePermission.keyPermissions.single { it.key == value.key }.referencedTypePermission!!)
            if (variableJson.get("values").asJsonObject.size() != 0)
              jsonValues.add(value.key.name, variableJson)
          } else {
            if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
              jsonValues.add(value.key.name, serialize(value.referencedVariable!!))
          }
        }

      }
    }
  }
  json.add("values", jsonValues)
  return json
}

fun serialize(variables: Set<Variable>, typePermission: TypePermission): JsonArray {
  val json = JsonArray()
  for (variable in variables)
    json.add(serialize(variable = variable, typePermission = typePermission))
  return json
}
