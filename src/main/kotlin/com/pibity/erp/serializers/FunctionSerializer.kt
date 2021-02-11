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
import com.pibity.erp.entities.function.Function
import com.pibity.erp.entities.function.FunctionInputType
import com.pibity.erp.entities.function.FunctionOutputType

fun serialize(function: Function): JsonObject {
  val json = JsonObject()
  json.addProperty("orgId", function.organization.id)
  json.addProperty("functionName", function.name)
  json.add("inputs", JsonObject().apply {
    for (input in function.inputs) {
      when (input.type.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN ->
          addProperty(input.name, input.type.name)
        TypeConstants.FORMULA -> {
        }
        else ->
          add(input.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, input.type.name)
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
      when (output.type.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.DATE, TypeConstants.TIMESTAMP, TypeConstants.TIME -> {
          add(output.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, output.type.name)
            add("values", gson.fromJson(output.variableName, JsonObject::class.java))
          })
        }
        TypeConstants.FORMULA -> {
        }
        else ->
          add(output.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, output.type.name)
            add("variableName", gson.fromJson(output.variableName, JsonObject::class.java))
            add("values", serialize(output.values!!))
          })
      }
    }
  })
  json.add("permissions", serialize(function.permissions))
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
    when (functionInputKey.key.type.name) {
      TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN ->
        json.add(functionInputKey.key.name, gson.fromJson(functionInputKey.expression, JsonObject::class.java))
      TypeConstants.FORMULA, TypeConstants.BLOB -> {
      }
      else -> json.add(functionInputKey.key.name, gson.fromJson(functionInputKey.expression, JsonObject::class.java))
    }
  }
  return json
}

fun serialize(functionOutputType: FunctionOutputType): JsonObject {
  val json = JsonObject()
  for (functionOutputKey in functionOutputType.functionOutputKeys) {
    when (functionOutputKey.key.type.name) {
      TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN ->
        json.add(functionOutputKey.key.name, gson.fromJson(functionOutputKey.expression, JsonObject::class.java))
      TypeConstants.FORMULA -> {
      }
      else -> json.add(functionOutputKey.key.name, gson.fromJson(functionOutputKey.expression, JsonObject::class.java))
    }
  }
  return json
}
