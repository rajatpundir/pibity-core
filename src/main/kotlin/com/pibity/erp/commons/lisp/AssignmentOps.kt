/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons.lisp

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.KeyConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.utils.validateOrEvaluateExpression
import java.util.regex.Pattern

val symbolIdentifierPattern: Pattern = Pattern.compile("^[a-z][a-zA-Z0-9]*$")

fun validateSymbols(jsonParams: JsonObject): JsonObject {
  val expectedSymbols = JsonObject()
  for ((symbolName, symbolObject) in jsonParams.entrySet()) {
    val expectedSymbol = JsonObject()
    val symbol = try {
      symbolObject.asJsonObject
    } catch (exception: Exception) {
      throw CustomJsonException("{$symbolName: 'Unexpected value for parameter'}")
    }
    if (!symbolIdentifierPattern.matcher(symbolName).matches())
      throw CustomJsonException("{$symbolName: 'Symbol name is not a valid identifier'}")
    if (symbol.has(KeyConstants.KEY_TYPE)) {
      try {
        expectedSymbol.addProperty(KeyConstants.KEY_TYPE, symbol.get(KeyConstants.KEY_TYPE).asString)
      } catch (exception: Exception) {
        throw CustomJsonException("{$symbolName: {${KeyConstants.KEY_TYPE}: 'Unexpected value for parameter'}}")
      }
      when (symbol.get(KeyConstants.KEY_TYPE).asString) {
        TypeConstants.TEXT -> expectedSymbol.addProperty(KeyConstants.VALUE, if (symbol.has(KeyConstants.VALUE)) try {
          symbol.get(KeyConstants.VALUE).asString
        } catch (exception: Exception) {
          throw CustomJsonException("{$symbolName: {${KeyConstants.VALUE}: 'Unexpected value for parameter'}}")
        } else "")
        TypeConstants.NUMBER -> expectedSymbol.addProperty(KeyConstants.VALUE, if (symbol.has(KeyConstants.VALUE)) try {
          symbol.get(KeyConstants.VALUE).asLong
        } catch (exception: Exception) {
          throw CustomJsonException("{$symbolName: {${KeyConstants.VALUE}: 'Unexpected value for parameter'}}")
        } else 0L)
        TypeConstants.DECIMAL -> expectedSymbol.addProperty(KeyConstants.VALUE, if (symbol.has(KeyConstants.VALUE)) try {
          symbol.get(KeyConstants.VALUE).asDouble
        } catch (exception: Exception) {
          throw CustomJsonException("{$symbolName: {${KeyConstants.VALUE}: 'Unexpected value for parameter'}}")
        } else 0.0)
        TypeConstants.BOOLEAN -> expectedSymbol.addProperty(KeyConstants.VALUE, if (symbol.has(KeyConstants.VALUE)) try {
          symbol.get(KeyConstants.VALUE).asBoolean
        } catch (exception: Exception) {
          throw CustomJsonException("{$symbolName: {${KeyConstants.VALUE}: 'Unexpected value for parameter'}}")
        } else false)
        else -> throw CustomJsonException("{$symbolName: {${KeyConstants.KEY_TYPE}: 'Unexpected value for parameter'}}")
      }
      expectedSymbols.add(symbolName, expectedSymbol)
    } else {
      try {
        expectedSymbols.add(symbolName, validateSymbols(jsonParams = symbol))
      } catch (exception: CustomJsonException) {
        throw CustomJsonException("{$symbolName: ${exception.message}}")
      }
    }
  }
  return expectedSymbols
}

fun updateSymbols(prevSymbols: JsonObject, nextSymbols: JsonObject): JsonObject {
  val updatedSymbols: JsonObject = prevSymbols.deepCopy()
  for ((symbolName, symbolObject) in nextSymbols.entrySet())
    updatedSymbols.add(symbolName, symbolObject.asJsonObject)
  return updatedSymbols
}

fun let(args: List<JsonElement>, expectedReturnType: String, mode: String, symbols: JsonObject): Any {
  return when (mode) {
    "validate" -> {
      if (args.size < 2 || !args.first().isJsonObject)
        throw CustomJsonException("{args: 'Unexpected value for parameter'}")
      val updatedSymbols: JsonObject = updateSymbols(prevSymbols = symbols, nextSymbols = validateSymbols(jsonParams = args.first().asJsonObject))
      validateOrEvaluateExpression(jsonParams = args[1].asJsonObject.apply {
        addProperty("expectedReturnType", expectedReturnType)
      }, mode = mode, symbols = updatedSymbols) as String
    }
    else -> {
      val updatedSymbols: JsonObject = updateSymbols(prevSymbols = symbols, nextSymbols = validateSymbols(jsonParams = args.first().asJsonObject))
      validateOrEvaluateExpression(jsonParams = args[1].asJsonObject.apply {
        addProperty("expectedReturnType", expectedReturnType)
      }, mode = mode, symbols = updatedSymbols)
    }
  }
}

// TODO. This function needs a rewrite for improved elegance.
fun dot(args: List<JsonElement>, expectedReturnType: String, mode: String, symbols: JsonObject): Any {
  when (mode) {
    "validate" -> {
      if (args.isEmpty())
        throw CustomJsonException("{args: 'Unexpected value for parameter'}")
      var symbolsJson: JsonObject = symbols
      val symbolPath: List<String> = args.map {
        try {
          if (it.isJsonObject) validateOrEvaluateExpression(jsonParams = it.asJsonObject.apply {
            addProperty("expectedReturnType", TypeConstants.TEXT)
          }, mode = "evaluate", symbols = symbols) as String else it.asString
        } catch (exception: Exception) {
          throw CustomJsonException("{args: 'Unexpected value for parameter'}")
        }
      }
      for ((index, symbolName) in symbolPath.withIndex()) {
        if (symbolsJson.has(symbolName)) {
          if (symbolsJson.get(symbolName).asJsonObject.has(KeyConstants.KEY_TYPE)) {
            return when (symbolsJson.get(symbolName).asJsonObject.get(KeyConstants.KEY_TYPE).asString) {
              TypeConstants.TEXT -> when (expectedReturnType) {
                TypeConstants.TEXT -> TypeConstants.TEXT
                else -> throw CustomJsonException("{args: 'Unexpected value for parameter'}")
              }
              TypeConstants.NUMBER -> when (expectedReturnType) {
                TypeConstants.TEXT -> TypeConstants.TEXT
                TypeConstants.NUMBER -> TypeConstants.NUMBER
                TypeConstants.DECIMAL -> TypeConstants.DECIMAL
                else -> throw CustomJsonException("{args: 'Unexpected value for parameter'}")
              }
              TypeConstants.DECIMAL -> when (expectedReturnType) {
                TypeConstants.TEXT -> TypeConstants.TEXT
                TypeConstants.NUMBER -> TypeConstants.NUMBER
                TypeConstants.DECIMAL -> TypeConstants.DECIMAL
                else -> throw CustomJsonException("{args: 'Unexpected value for parameter'}")
              }
              TypeConstants.BOOLEAN -> when (expectedReturnType) {
                TypeConstants.TEXT -> TypeConstants.TEXT
                TypeConstants.BOOLEAN -> TypeConstants.BOOLEAN
                else -> throw CustomJsonException("{args: 'Unexpected value for parameter'}")
              }
              else -> throw CustomJsonException("{args: 'Unexpected value for parameter'}")
            }
          } else symbolsJson = symbolsJson.get(symbolName).asJsonObject
        } else throw CustomJsonException("{args: 'Unexpected value for parameter'}")
        if (index == (args.size - 1))
          throw CustomJsonException("{args: 'Unexpected value for parameter'}")
      }
    }
    else -> {
      var symbolsJson: JsonObject = symbols
      val symbolPath: List<String> = args.map {
        try {
          if (it.isJsonObject)
            validateOrEvaluateExpression(jsonParams = it.asJsonObject.apply {
              addProperty("expectedReturnType", TypeConstants.TEXT)
            }, mode = "evaluate", symbols = symbols) as String
          else it.asString
        } catch (exception: Exception) {
          throw CustomJsonException("{args: 'Unexpected value for parameter'}")
        }
      }
      for ((index, symbolName) in symbolPath.withIndex()) {
        if (symbolsJson.has(symbolName)) {
          if (symbolsJson.get(symbolName).asJsonObject.has(KeyConstants.KEY_TYPE)) {
            return when (symbolsJson.get(symbolName).asJsonObject.get(KeyConstants.KEY_TYPE).asString) {
              TypeConstants.TEXT -> when (expectedReturnType) {
                TypeConstants.TEXT -> symbolsJson.get(symbolName).asJsonObject.get(KeyConstants.VALUE).asString
                else -> throw CustomJsonException("{args: 'Unexpected value for parameter'}")
              }
              TypeConstants.NUMBER -> when (expectedReturnType) {
                TypeConstants.TEXT -> symbolsJson.get(symbolName).asJsonObject.get(KeyConstants.VALUE).asString
                TypeConstants.NUMBER -> symbolsJson.get(symbolName).asJsonObject.get(KeyConstants.VALUE).asLong
                TypeConstants.DECIMAL -> symbolsJson.get(symbolName).asJsonObject.get(KeyConstants.VALUE).asDouble
                else -> throw CustomJsonException("{args: 'Unexpected value for parameter'}")
              }
              TypeConstants.DECIMAL -> when (expectedReturnType) {
                TypeConstants.TEXT -> symbolsJson.get(symbolName).asJsonObject.get(KeyConstants.VALUE).asString
                TypeConstants.NUMBER -> symbolsJson.get(symbolName).asJsonObject.get(KeyConstants.VALUE).asLong
                TypeConstants.DECIMAL -> symbolsJson.get(symbolName).asJsonObject.get(KeyConstants.VALUE).asDouble
                else -> throw CustomJsonException("{args: 'Unexpected value for parameter'}")
              }
              TypeConstants.BOOLEAN -> when (expectedReturnType) {
                TypeConstants.TEXT -> symbolsJson.get(symbolName).asJsonObject.get(KeyConstants.VALUE).asString
                TypeConstants.BOOLEAN -> symbolsJson.get(symbolName).asJsonObject.get(KeyConstants.VALUE).asBoolean
                else -> throw CustomJsonException("{args: 'Unexpected value for parameter'}")
              }
              else -> throw CustomJsonException("{args: 'Unexpected value for parameter'}")
            }
          } else symbolsJson = symbolsJson.get(symbolName).asJsonObject
        } else throw CustomJsonException("{args: 'Unexpected value for parameter'}")
        if (index == (args.size - 1))
          throw CustomJsonException("{args: 'Unexpected value for parameter'}")
      }
    }
  }
  throw CustomJsonException("{args: 'Unexpected value for parameter'}")
}
