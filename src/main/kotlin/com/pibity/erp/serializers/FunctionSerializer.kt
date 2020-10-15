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
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.utils.gson
import com.pibity.erp.entities.function.Function
import com.pibity.erp.entities.function.FunctionInputType
import com.pibity.erp.entities.function.FunctionOutputType

fun serialize(function: Function): JsonObject {
  val json = JsonObject()
  json.addProperty("organizationId", function.id.organization.id)
  json.addProperty("functionName", function.id.name)
  json.add("inputs", JsonObject().apply {
    for (input in function.inputs) {
      when (input.type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN ->
          addProperty(input.id.name, input.type.id.name)
        TypeConstants.FORMULA, TypeConstants.LIST -> {
        }
        else ->
          add(input.id.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, input.type.id.name)
            if (input.variableName != null)
              add("variableName", gson.fromJson(input.variableName, JsonObject::class.java))
            if (input.values != null)
              add("values", serialize(input.values!!))
          })
      }
    }
  })
  json.add("outputs", JsonObject().apply {
    for (output in function.outputs) {
      when (output.type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> {
          add(output.id.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, output.type.id.name)
            if (output.variableName != null)
              add("values", gson.fromJson(output.variableName, JsonObject::class.java))
          })
        }
        TypeConstants.FORMULA, TypeConstants.LIST -> {
        }
        else ->
          add(output.id.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, output.type.id.name)
            add("variableName", gson.fromJson(output.variableName, JsonObject::class.java))
            add("values", serialize(output.values!!))
          })
      }
    }
  })
  return json
}

fun serialize(entities: Set<Function>): JsonArray {
  val json = JsonArray()
  for (entity in entities)
    json.add(serialize(entity))
  return json
}

fun serialize(functionInputType: FunctionInputType): JsonObject {
  val json = JsonObject()
  for (functionInputKey in functionInputType.functionInputKeys) {
    when (functionInputKey.id.key.type.id.name) {
      TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN ->
        json.add(functionInputKey.id.key.id.name, gson.fromJson(functionInputKey.expression, JsonObject::class.java))
      TypeConstants.FORMULA, TypeConstants.LIST -> {
      }
      else -> if (functionInputKey.id.key.type.id.superTypeName == GLOBAL_TYPE) {
        json.add(functionInputKey.id.key.id.name, gson.fromJson(functionInputKey.expression, JsonObject::class.java))
      } else {
        if ((functionInputKey.id.key.id.parentType.id.superTypeName == GLOBAL_TYPE && functionInputKey.id.key.id.parentType.id.name == functionInputKey.id.key.type.id.superTypeName)
            || (functionInputKey.id.key.id.parentType.id.superTypeName != GLOBAL_TYPE && functionInputKey.id.key.id.parentType.id.superTypeName == functionInputKey.id.key.type.id.superTypeName)) {
          json.add(functionInputKey.id.key.id.name, serialize(functionInputKey.referencedFunctionInputType!!))
        } else {
          json.add(functionInputKey.id.key.id.name, gson.fromJson(functionInputKey.expression, JsonObject::class.java))
        }
      }
    }
  }
  return json
}

fun serialize(functionOutputType: FunctionOutputType): JsonObject {
  val json = JsonObject()
  for (functionOutputKey in functionOutputType.functionOutputKeys) {
    when (functionOutputKey.id.key.type.id.name) {
      TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN ->
        json.add(functionOutputKey.id.key.id.name, gson.fromJson(functionOutputKey.expression, JsonObject::class.java))
      TypeConstants.FORMULA, TypeConstants.LIST -> {
      }
      else -> if (functionOutputKey.id.key.type.id.superTypeName == GLOBAL_TYPE) {
        json.add(functionOutputKey.id.key.id.name, gson.fromJson(functionOutputKey.expression, JsonObject::class.java))
      } else {
        if ((functionOutputKey.id.key.id.parentType.id.superTypeName == GLOBAL_TYPE && functionOutputKey.id.key.id.parentType.id.name == functionOutputKey.id.key.type.id.superTypeName)
            || (functionOutputKey.id.key.id.parentType.id.superTypeName != GLOBAL_TYPE && functionOutputKey.id.key.id.parentType.id.superTypeName == functionOutputKey.id.key.type.id.superTypeName)) {
          json.add(functionOutputKey.id.key.id.name, serialize(functionOutputKey.referencedFunctionOutputType!!))
        } else {
          json.add(functionOutputKey.id.key.id.name, gson.fromJson(functionOutputKey.expression, JsonObject::class.java))
        }
      }
    }
  }
  return json
}
