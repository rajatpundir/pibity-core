/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
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

val symbolIdentifierPattern: Pattern = Pattern.compile("^([a-z][a-zA-Z0-9]*)+(::([a-z][a-zA-Z0-9]*)+)?$")

fun validateSymbols(jsonParams: JsonObject): JsonObject {
  val expectedSymbols = JsonObject()
  for ((symbolName, symbolObject) in jsonParams.entrySet()) {
    val expectedSymbol = JsonObject()
    val symbol: JsonObject = try {
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
    }
    if (symbol.has("values")) {
      try {
        expectedSymbol.add("values", validateSymbols(jsonParams = symbol.get("values").asJsonObject))
      } catch (exception: CustomJsonException) {
        throw CustomJsonException("{$symbolName: {values: ${exception.message}}}")
      }
    }
    if (expectedSymbol.size() == 0)
      throw CustomJsonException("{$symbolName: 'Unexpected value for parameter'}")
    expectedSymbols.add(symbolName, expectedSymbol)
  }
  return expectedSymbols
}

fun updateSymbols(prevSymbols: JsonObject, nextSymbols: JsonObject): JsonObject {
  val updatedSymbols: JsonObject = prevSymbols.deepCopy()
  for ((symbolName, symbolObject) in nextSymbols.entrySet())
    updatedSymbols.add(symbolName, symbolObject.asJsonObject)
  return updatedSymbols
}

fun getSymbolPaths(jsonParams: JsonObject, prefix: String = ""): Set<String> {
  val symbolPaths = mutableSetOf<String>()
  for ((symbolName, symbolObject) in jsonParams.entrySet()) {
    val symbol = symbolObject.asJsonObject
    if (symbol.has(KeyConstants.KEY_TYPE))
      symbolPaths.add(prefix + symbolName)
    if (symbol.has("values"))
      symbolPaths.addAll(getSymbolPaths(jsonParams = symbol.get("values").asJsonObject, prefix = "$prefix$symbolName."))
  }
  return symbolPaths
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
    "collect" -> {
      if (args.size < 2 || !args.first().isJsonObject)
        throw CustomJsonException("{args: 'Unexpected value for parameter'}")
      val updatedSymbols: JsonObject = updateSymbols(prevSymbols = symbols, nextSymbols = validateSymbols(jsonParams = args.first().asJsonObject))
      val collectedSymbols = mutableSetOf<String>()
      collectedSymbols.addAll(validateOrEvaluateExpression(jsonParams = args[1].asJsonObject.apply {
        addProperty("expectedReturnType", expectedReturnType)
      }, mode = mode, symbols = updatedSymbols) as Set<String>)
      collectedSymbols.removeAll(getSymbolPaths(jsonParams = updatedSymbols))
      collectedSymbols
    }
    else -> {
      val updatedSymbols: JsonObject = updateSymbols(prevSymbols = symbols, nextSymbols = validateSymbols(jsonParams = args.first().asJsonObject))
      validateOrEvaluateExpression(jsonParams = args[1].asJsonObject.apply {
        addProperty("expectedReturnType", expectedReturnType)
      }, mode = mode, symbols = updatedSymbols)
    }
  }
}

fun dot(args: List<JsonElement>, expectedReturnType: String, mode: String, symbols: JsonObject): Any {
  when (mode) {
    "validate" -> {
      if (args.isEmpty())
        throw CustomJsonException("{args: 'Unexpected value for parameter'}")
      var symbolsJson: JsonObject = symbols
      val symbolPaths: List<String> = args.map {
        try {
          it.asString
        } catch (exception: Exception) {
          throw CustomJsonException("{args: 'Unexpected value for parameter'}")
        }
      }
      for ((index, symbolName) in symbolPaths.withIndex()) {
        if (symbolsJson.has(symbolName)) {
          if (index == (symbolPaths.size - 1)) {
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
            } else throw CustomJsonException("{args: 'Unexpected value for parameter'}")
          } else {
            try {
              symbolsJson = symbolsJson.get(symbolName).asJsonObject.get("values").asJsonObject
            } catch (exception: Exception) {
              throw CustomJsonException("{args: 'Unexpected value for parameter'}")
            }
          }
        } else throw CustomJsonException("{args: 'Unexpected value for parameter'}")
        if (index == (symbolPaths.size - 1))
          throw CustomJsonException("{args: 'Unexpected value for parameter'}")
      }
    }
    "collect" -> {
      if (args.isEmpty())
        throw CustomJsonException("{args: 'Unexpected value for parameter'}")
      val symbolPaths: List<String> = args.map {
        try {
          it.asString
        } catch (exception: Exception) {
          throw CustomJsonException("{args: 'Unexpected value for parameter'}")
        }
      }
      return mutableSetOf(symbolPaths.joinToString(separator = "."))
    }
    else -> {
      var symbolsJson: JsonObject = symbols
      val symbolPaths: List<String> = args.map {
        try {
          it.asString
        } catch (exception: Exception) {
          throw CustomJsonException("{args: 'Unexpected value for parameter'}")
        }
      }
      for ((index, symbolName) in symbolPaths.withIndex()) {
        if (symbolsJson.has(symbolName)) {
          if (index == (symbolPaths.size - 1)) {
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
            } else throw CustomJsonException("{args: 'Unexpected value for parameter'}")
          } else symbolsJson = symbolsJson.get(symbolName).asJsonObject.get("values").asJsonObject
        } else throw CustomJsonException("{args: 'Unexpected value for parameter'}")
        if (index == (symbolPaths.size - 1))
          throw CustomJsonException("{args: 'Unexpected value for parameter'}")
      }
    }
  }
  throw CustomJsonException("{args: 'Unexpected value for parameter'}")
}

fun identity(args: List<JsonElement>, types: MutableList<String>, expectedReturnType: String, mode: String): Any {
  return when (mode) {
    "validate" -> {
      if (types.isEmpty())
        throw CustomJsonException("{types: 'Unexpected value for parameter'}")
      if (args.isEmpty())
        throw CustomJsonException("{args: 'Unexpected value for parameter'}")
      val argType: String = try {
        types.first()
      } catch (exception: Exception) {
        throw CustomJsonException("{types: 'Unexpected value for parameter'}")
      }
      val expectedReturnTypes: List<String> = when (argType) {
        TypeConstants.TEXT -> listOf(TypeConstants.TEXT)
        TypeConstants.NUMBER, TypeConstants.DECIMAL -> listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT)
        TypeConstants.BOOLEAN -> listOf(TypeConstants.BOOLEAN, TypeConstants.TEXT)
        else -> throw CustomJsonException("{types: 'Unexpected value for parameter'}")
      }
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{types: 'Unexpected value for parameter'}")
      when (argType) {
        TypeConstants.TEXT -> try {
          args.first().asString
        } catch (exception: Exception) {
          throw CustomJsonException("{args: 'Unexpected value for parameter'}")
        }
        TypeConstants.NUMBER -> try {
          args.first().asLong
        } catch (exception: Exception) {
          throw CustomJsonException("{args: 'Unexpected value for parameter'}")
        }
        TypeConstants.DECIMAL -> try {
          args.first().asDouble
        } catch (exception: Exception) {
          throw CustomJsonException("{args: 'Unexpected value for parameter'}")
        }
        else -> try {
          args.first().asBoolean
        } catch (exception: Exception) {
          throw CustomJsonException("{args: 'Unexpected value for parameter'}")
        }
      }
      expectedReturnType
    }
    "collect" -> mutableSetOf<String>()
    else -> when (types.first()) {
      TypeConstants.TEXT -> args.first().asString
      TypeConstants.NUMBER -> when (expectedReturnType) {
        TypeConstants.NUMBER -> args.first().asLong
        TypeConstants.DECIMAL -> args.first().asLong.toDouble()
        else -> args.first().asLong.toString()
      }
      TypeConstants.DECIMAL -> when (expectedReturnType) {
        TypeConstants.NUMBER -> args.first().asLong
        TypeConstants.DECIMAL -> args.first().asDouble
        else -> args.first().asDouble.toString()
      }
      else -> when (expectedReturnType) {
        TypeConstants.BOOLEAN -> args.first().asBoolean
        else -> args.first().asBoolean.toString()
      }
    }
  }
}
