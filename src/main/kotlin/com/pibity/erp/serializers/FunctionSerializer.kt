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
import com.pibity.erp.commons.constants.*
import com.pibity.erp.commons.utils.gson
import com.pibity.erp.entities.function.Function
import com.pibity.erp.entities.function.FunctionInputKey
import com.pibity.erp.entities.function.FunctionOutputKey

fun serialize(function: Function): JsonObject = JsonObject().apply {
  addProperty(OrganizationConstants.ORGANIZATION_ID, function.organization.id)
  addProperty(FunctionConstants.FUNCTION_NAME, function.name)
  add(FunctionConstants.INPUTS, function.inputs.fold(JsonObject()) { acc, input ->
    acc.apply {
      when (input.type.name) {
        in primitiveTypes ->
          addProperty(input.name, input.type.name)
        TypeConstants.FORMULA -> {
        }
        else -> add(input.name, JsonObject().apply {
          addProperty(KeyConstants.KEY_TYPE, input.type.name)
          if (input.variableName != null)
            add(VariableConstants.VARIABLE_NAME, gson.fromJson(input.variableName, JsonObject::class.java))
          if (input.values.isNotEmpty())
            add(VariableConstants.VALUES, serialize(input.values))
        })
      }
    }
  })
  add(FunctionConstants.OUTPUTS, function.outputs.fold(JsonObject()) { acc, output ->
    acc.apply {
      when (output.type.name) {
        in primitiveTypes -> {
          add(output.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, output.type.name)
            add(VariableConstants.VALUES, gson.fromJson(output.variableName, JsonObject::class.java))
          })
        }
        TypeConstants.FORMULA -> {
        }
        else ->
          add(output.name, JsonObject().apply {
            when (output.operation) {
              FunctionConstants.CREATE -> {
                addProperty(VariableConstants.OPERATION, VariableConstants.CREATE)
                addProperty(KeyConstants.KEY_TYPE, output.type.name)
                add(VariableConstants.VARIABLE_NAME, gson.fromJson(output.variableName, JsonObject::class.java))
                add(VariableConstants.VALUES, serialize(output.values.toList()))
              }
              FunctionConstants.UPDATE -> {
                addProperty(VariableConstants.OPERATION, VariableConstants.UPDATE)
                addProperty(KeyConstants.KEY_TYPE, output.type.name)
                add(VariableConstants.VARIABLE_NAME, gson.fromJson(output.variableName, JsonObject::class.java))
                if (output.values.isNotEmpty())
                  add(VariableConstants.VALUES, serialize(output.values.toList()))
              }
              FunctionConstants.DELETE -> {
                addProperty(VariableConstants.OPERATION, VariableConstants.DELETE)
                addProperty(KeyConstants.KEY_TYPE, output.type.name)
                add(VariableConstants.VARIABLE_NAME, gson.fromJson(output.variableName, JsonObject::class.java))
              }
            }
          })
      }
    }
  })
  add("permissions", serialize(function.permissions))
}

fun serialize(entities: Set<Function>): JsonArray = entities.fold(JsonArray()) { acc, entity -> acc.apply { add(serialize(entity)) } }

fun serialize(functionInputKeys: Set<FunctionInputKey>): JsonObject = functionInputKeys.fold(JsonObject()) { acc, functionInputKey ->
  acc.apply {
    when (functionInputKey.key.type.name) {
      in primitiveTypes ->
        add(functionInputKey.key.name, gson.fromJson(functionInputKey.expression, JsonObject::class.java))
      TypeConstants.FORMULA, TypeConstants.BLOB -> {
      }
      else -> add(functionInputKey.key.name, gson.fromJson(functionInputKey.expression, JsonObject::class.java))
    }
  }
}

fun serialize(functionOutputKeys: List<FunctionOutputKey>): JsonObject = functionOutputKeys.fold(JsonObject()) { acc, functionOutputKey ->
  acc.apply {
    when (functionOutputKey.key.type.name) {
      in primitiveTypes ->
        add(functionOutputKey.key.name, gson.fromJson(functionOutputKey.expression, JsonObject::class.java))
      TypeConstants.FORMULA -> {
      }
      else -> add(functionOutputKey.key.name, gson.fromJson(functionOutputKey.expression, JsonObject::class.java))
    }
  }
}
