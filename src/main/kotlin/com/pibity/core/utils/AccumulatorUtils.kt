/*
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.core.commons.constants.*
import com.pibity.core.commons.CustomJsonException
import com.pibity.core.commons.lisp.getSymbolPaths
import com.pibity.core.entities.Key
import com.pibity.core.entities.Type
import com.pibity.core.entities.uniqueness.TypeUniqueness
import org.springframework.web.multipart.MultipartFile
import java.sql.Time
import java.sql.Timestamp
import java.util.*

fun validateAccumulatorName(accumulatorName: String): String {
  return if (!keyIdentifierPattern.matcher(accumulatorName).matches())
    throw CustomJsonException("{${OrganizationConstants.TYPE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
  else accumulatorName
}

@Suppress("UNCHECKED_CAST")
fun validateAccumulator(jsonParams: JsonObject, types: Set<Type>, typeUniqueness: TypeUniqueness, symbolPaths: MutableSet<String>, keyDependencies: MutableSet<Key>, files: List<MultipartFile>): JsonObject {
  return JsonObject().apply {
    addProperty(AccumulatorConstants.ACCUMULATOR_NAME, validateAccumulatorName(accumulatorName = jsonParams.get(AccumulatorConstants.ACCUMULATOR_NAME).asString))
    add("keys", gson.fromJson(jsonParams.get("keys").asJsonArray.map { keyName -> typeUniqueness.keys.single { it.name == keyName.asString } }.apply {
      if (this.isEmpty() || this.size == typeUniqueness.keys.size)
        throw CustomJsonException("{keys: ${MessageConstants.UNEXPECTED_VALUE}}")
    }.toString(), JsonArray::class.java))
    val accumulatorType: Type = types.single { it.name == jsonParams.get(OrganizationConstants.TYPE_NAME).asString && it.name != TypeConstants.FORMULA }
    addProperty(AccumulatorConstants.ACCUMULATOR_TYPE, accumulatorType.name)
    when (accumulatorType.name) {
      TypeConstants.TEXT -> addProperty(AccumulatorConstants.INITIAL_VALUE, jsonParams.get(AccumulatorConstants.INITIAL_VALUE).asString)
      TypeConstants.NUMBER -> addProperty(AccumulatorConstants.INITIAL_VALUE, jsonParams.get(AccumulatorConstants.INITIAL_VALUE).asLong)
      TypeConstants.DECIMAL -> addProperty(AccumulatorConstants.INITIAL_VALUE, jsonParams.get(AccumulatorConstants.INITIAL_VALUE).asBigDecimal)
      TypeConstants.BOOLEAN -> addProperty(AccumulatorConstants.INITIAL_VALUE, jsonParams.get(AccumulatorConstants.INITIAL_VALUE).asBoolean)
      TypeConstants.DATE -> if(jsonParams.has(AccumulatorConstants.INITIAL_VALUE)) addProperty(AccumulatorConstants.INITIAL_VALUE, Date(jsonParams.get(AccumulatorConstants.INITIAL_VALUE).asLong).time)
      TypeConstants.TIMESTAMP -> if(jsonParams.has(AccumulatorConstants.INITIAL_VALUE)) addProperty(AccumulatorConstants.INITIAL_VALUE, Timestamp(jsonParams.get(AccumulatorConstants.INITIAL_VALUE).asLong).time)
      TypeConstants.TIME -> if(jsonParams.has(AccumulatorConstants.INITIAL_VALUE)) addProperty(AccumulatorConstants.INITIAL_VALUE, Time(jsonParams.get(AccumulatorConstants.INITIAL_VALUE).asLong).time)
      TypeConstants.BLOB -> {
        val fileIndex: Int = jsonParams.get(AccumulatorConstants.INITIAL_VALUE).asInt
        if (fileIndex < 0 && fileIndex > (files.size - 1))
          throw CustomJsonException("{}")
        else
          addProperty(AccumulatorConstants.INITIAL_VALUE, fileIndex)
      }
      TypeConstants.FORMULA -> throw CustomJsonException("{}")
      else -> addProperty(AccumulatorConstants.INITIAL_VALUE, jsonParams.get(AccumulatorConstants.INITIAL_VALUE).asString)
    }
    symbolPaths.addAll(validateOrEvaluateExpression(expression = jsonParams.get(AccumulatorConstants.FORWARD_EXPRESSION).asJsonObject,
      symbols = JsonObject(), mode = LispConstants.VALIDATE, expectedReturnType = if (accumulatorType.name in primitiveTypes) accumulatorType.name else TypeConstants.TEXT) as Set<String>)
    symbolPaths.addAll(validateOrEvaluateExpression(expression = jsonParams.get(AccumulatorConstants.BACKWARD_EXPRESSION).asJsonObject,
      symbols = JsonObject(), mode = LispConstants.VALIDATE, expectedReturnType = if (accumulatorType.name in primitiveTypes) accumulatorType.name else TypeConstants.TEXT) as Set<String>)
    val symbols: JsonObject = JsonObject().apply {
      add("acc", JsonObject().apply {
        addProperty(SymbolConstants.SYMBOL_TYPE, if (accumulatorType.name in primitiveTypes) accumulatorType.name else TypeConstants.TEXT)
        if (accumulatorType.name !in primitiveTypes)
          add(SymbolConstants.SYMBOL_VALUES, getSymbolsForAccumulator(type = accumulatorType, symbolPaths = symbolPaths, keyDependencies = keyDependencies, prefix = "acc.", symbolsForFormula = false))
      })
      add("it", JsonObject().apply {
        addProperty(SymbolConstants.SYMBOL_TYPE, TypeConstants.TEXT)
        add(SymbolConstants.SYMBOL_VALUES, getSymbolsForAccumulator(type = typeUniqueness.type, symbolPaths = symbolPaths, keyDependencies = keyDependencies, prefix = "it.", symbolsForFormula = false))
      })
    }
    symbolPaths.apply {
      clear()
      addAll(getSymbolPaths(symbols = symbols))
    }
    add(AccumulatorConstants.FORWARD_EXPRESSION, validateOrEvaluateExpression(expression = jsonParams.get(AccumulatorConstants.FORWARD_EXPRESSION).asJsonObject, symbols = symbols,
      mode = LispConstants.REFLECT, expectedReturnType = if (accumulatorType.name in primitiveTypes) accumulatorType.name else TypeConstants.TEXT) as JsonObject)
    add(AccumulatorConstants.BACKWARD_EXPRESSION, validateOrEvaluateExpression(expression = jsonParams.get(AccumulatorConstants.BACKWARD_EXPRESSION).asJsonObject, symbols = symbols,
      mode = LispConstants.REFLECT, expectedReturnType = if (accumulatorType.name in primitiveTypes) accumulatorType.name else TypeConstants.TEXT) as JsonObject)
  }
}

fun getSymbolsForAccumulator(type: Type, symbolPaths: MutableSet<String>, keyDependencies: MutableSet<Key> = mutableSetOf(), symbolsForFormula: Boolean, prefix: String = "", level: Int = 0): JsonObject {
  return type.keys.fold(JsonObject()) { acc, key ->
    acc.apply {
      if (symbolPaths.any { it.startsWith(prefix = prefix + key.name) }) {
        when (key.type.name) {
          in primitiveTypes -> add(key.name, JsonObject().apply { addProperty(SymbolConstants.SYMBOL_TYPE, key.type.name) })
          TypeConstants.FORMULA -> if (!symbolsForFormula || level != 0)
            add(key.name, JsonObject().apply { addProperty(SymbolConstants.SYMBOL_TYPE, key.formula!!.returnType.name) })
          else -> add(key.name, JsonObject().apply {
            addProperty(SymbolConstants.SYMBOL_TYPE, TypeConstants.TEXT)
            if (key.referencedVariable != null && symbolPaths.any { it.startsWith(prefix = prefix + key.name + ".") })
              add(SymbolConstants.SYMBOL_VALUES,  getSymbolsForAccumulator(prefix = prefix + key.name + ".", level = level + 1, symbolPaths = symbolPaths,
                type = key.referencedVariable!!.type, keyDependencies = keyDependencies, symbolsForFormula = symbolsForFormula)
              )
          })
        }
        keyDependencies.add(key.apply { isAccumulatorDependency = true })
        symbolPaths.remove(prefix + key.name)
      }
    }
  }
}
