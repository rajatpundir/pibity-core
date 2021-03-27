/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.commons.lisp

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pibity.core.commons.constants.*
import com.pibity.core.commons.exceptions.CustomJsonException
import com.pibity.core.commons.utils.validateOrEvaluateExpression
import java.sql.Timestamp
import java.util.*
import java.util.regex.Pattern

val symbolIdentifierPattern: Pattern = Pattern.compile("^([a-z][a-zA-Z0-9]*)+(::([a-z][a-zA-Z0-9]*)+)?$")

fun validateSymbols(symbols: JsonObject): JsonObject {
  val expectedSymbols = JsonObject()
  for ((symbolName, symbolObject) in symbols.entrySet()) {
    val expectedSymbol = JsonObject()
    val symbol: JsonObject = try {
      symbolObject.asJsonObject
    } catch (exception: Exception) {
      throw CustomJsonException("{$symbolName: ${MessageConstants.UNEXPECTED_VALUE}}")
    }
    if (!symbolIdentifierPattern.matcher(symbolName).matches())
      throw CustomJsonException("{$symbolName: ${MessageConstants.UNEXPECTED_VALUE}}")
    if (symbol.has(SymbolConstants.SYMBOL_TYPE)) {
      try {
        expectedSymbol.addProperty(SymbolConstants.SYMBOL_TYPE, symbol.get(SymbolConstants.SYMBOL_TYPE).asString)
      } catch (exception: Exception) {
        throw CustomJsonException("{$symbolName: {${SymbolConstants.SYMBOL_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}}")
      }
      when (symbol.get(SymbolConstants.SYMBOL_TYPE).asString) {
        TypeConstants.TEXT -> expectedSymbol.addProperty(SymbolConstants.SYMBOL_VALUE, if (symbol.has(SymbolConstants.SYMBOL_VALUE)) try {
          symbol.get(SymbolConstants.SYMBOL_VALUE).asString
        } catch (exception: Exception) {
          throw CustomJsonException("{$symbolName: {${SymbolConstants.SYMBOL_VALUE}: ${MessageConstants.UNEXPECTED_VALUE}}}")
        } else "")
        TypeConstants.NUMBER -> expectedSymbol.addProperty(SymbolConstants.SYMBOL_VALUE, if (symbol.has(SymbolConstants.SYMBOL_VALUE)) try {
          symbol.get(SymbolConstants.SYMBOL_VALUE).asLong
        } catch (exception: Exception) {
          throw CustomJsonException("{$symbolName: {${SymbolConstants.SYMBOL_VALUE}: ${MessageConstants.UNEXPECTED_VALUE}}}")
        } else 0L)
        TypeConstants.DECIMAL -> expectedSymbol.addProperty(SymbolConstants.SYMBOL_VALUE, if (symbol.has(SymbolConstants.SYMBOL_VALUE)) try {
          symbol.get(SymbolConstants.SYMBOL_VALUE).asBigDecimal
        } catch (exception: Exception) {
          throw CustomJsonException("{$symbolName: {${SymbolConstants.SYMBOL_VALUE}: ${MessageConstants.UNEXPECTED_VALUE}}}")
        } else null)
        TypeConstants.BOOLEAN -> expectedSymbol.addProperty(SymbolConstants.SYMBOL_VALUE, if (symbol.has(SymbolConstants.SYMBOL_VALUE)) try {
          symbol.get(SymbolConstants.SYMBOL_VALUE).asBoolean
        } catch (exception: Exception) {
          throw CustomJsonException("{$symbolName: {${SymbolConstants.SYMBOL_VALUE}: ${MessageConstants.UNEXPECTED_VALUE}}}")
        } else false)
        TypeConstants.DATE -> expectedSymbol.addProperty(SymbolConstants.SYMBOL_VALUE, if (symbol.has(SymbolConstants.SYMBOL_VALUE)) try {
          java.sql.Date(symbol.get(SymbolConstants.SYMBOL_VALUE).asLong).toString()
        } catch (exception: Exception) {
          throw CustomJsonException("{$symbolName: {${SymbolConstants.SYMBOL_VALUE}: ${MessageConstants.UNEXPECTED_VALUE}}}")
        } else java.sql.Date(System.currentTimeMillis()).toString())
        TypeConstants.TIMESTAMP -> expectedSymbol.addProperty(SymbolConstants.SYMBOL_VALUE, if (symbol.has(SymbolConstants.SYMBOL_VALUE)) try {
          Timestamp(symbol.get(SymbolConstants.SYMBOL_VALUE).asLong).time
        } catch (exception: Exception) {
          throw CustomJsonException("{$symbolName: {${SymbolConstants.SYMBOL_VALUE}: ${MessageConstants.UNEXPECTED_VALUE}}}")
        } else System.currentTimeMillis())
        TypeConstants.TIME -> expectedSymbol.addProperty(SymbolConstants.SYMBOL_VALUE, if (symbol.has(SymbolConstants.SYMBOL_VALUE)) try {
          java.sql.Time(symbol.get(SymbolConstants.SYMBOL_VALUE).asLong).time
        } catch (exception: Exception) {
          throw CustomJsonException("{$symbolName: {${SymbolConstants.SYMBOL_VALUE}: ${MessageConstants.UNEXPECTED_VALUE}}}")
        } else java.sql.Time(System.currentTimeMillis()).time)
        TypeConstants.BLOB -> expectedSymbol.addProperty(SymbolConstants.SYMBOL_VALUE, if (symbol.has(SymbolConstants.SYMBOL_VALUE)) try {
          symbol.get(SymbolConstants.SYMBOL_VALUE).asString
        } catch (exception: Exception) {
          throw CustomJsonException("{$symbolName: {${SymbolConstants.SYMBOL_VALUE}: ${MessageConstants.UNEXPECTED_VALUE}}}")
        } else "")
        else -> throw CustomJsonException("{$symbolName: {${SymbolConstants.SYMBOL_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}}")
      }
    }
    if (symbol.has(SymbolConstants.SYMBOL_VALUES)) {
      try {
        expectedSymbol.add(SymbolConstants.SYMBOL_VALUES, validateSymbols(symbols = symbol.get(SymbolConstants.SYMBOL_VALUES).asJsonObject))
      } catch (exception: CustomJsonException) {
        throw CustomJsonException("{$symbolName: {${SymbolConstants.SYMBOL_VALUES}: ${exception.message}}}")
      }
    }
    if (expectedSymbol.size() == 0)
      throw CustomJsonException("{$symbolName: ${MessageConstants.UNEXPECTED_VALUE}}")
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

fun getSymbolPaths(symbols: JsonObject, prefix: String = ""): Set<String> {
  val symbolPaths = mutableSetOf<String>()
  for ((symbolName, symbolObject) in symbols.entrySet()) {
    val symbol = symbolObject.asJsonObject
    if (symbol.has(SymbolConstants.SYMBOL_TYPE))
      symbolPaths.add(prefix + symbolName)
    if (symbol.has(SymbolConstants.SYMBOL_VALUES))
      symbolPaths.addAll(getSymbolPaths(symbols = symbol.get(SymbolConstants.SYMBOL_VALUES).asJsonObject, prefix = "$prefix$symbolName."))
  }
  return symbolPaths
}

@Suppress("UNCHECKED_CAST")
fun let(args: List<JsonElement>, symbols: JsonObject, mode: String, expectedReturnType: String): Any {
  return when (mode) {
    LispConstants.VALIDATE -> {
      if (args.size != 2 || !args.first().isJsonObject || !args[1].isJsonObject)
        throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
      val updatedSymbols: JsonObject = updateSymbols(prevSymbols = symbols, nextSymbols = validateSymbols(symbols = args.first().asJsonObject))
      val collectedSymbols = mutableSetOf<String>()
      collectedSymbols.addAll(validateOrEvaluateExpression(expression = args[1].asJsonObject, symbols = updatedSymbols, mode = mode, expectedReturnType = expectedReturnType) as Set<String>)
      collectedSymbols.removeAll(getSymbolPaths(symbols = updatedSymbols))
      collectedSymbols
    }
    LispConstants.REFLECT -> JsonObject().apply {
      addProperty(LispConstants.OPERATION, OperatorConstants.LET)
      add(LispConstants.TYPES, JsonArray())
      add(LispConstants.ARGS, JsonArray().apply {
        if (args.size != 2 || !args.first().isJsonObject || !args[1].isJsonObject)
          throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
        val updatedSymbols: JsonObject = updateSymbols(prevSymbols = symbols, nextSymbols = validateSymbols(symbols = args.first().asJsonObject))
        add(validateSymbols(symbols = args.first().asJsonObject))
        add(validateOrEvaluateExpression(expression = args[1].asJsonObject, symbols = updatedSymbols, mode = mode, expectedReturnType = expectedReturnType) as JsonObject)
      })
    }
    else -> {
      val updatedSymbols: JsonObject = updateSymbols(prevSymbols = symbols, nextSymbols = validateSymbols(symbols = args.first().asJsonObject))
      validateOrEvaluateExpression(expression = args[1].asJsonObject, symbols = updatedSymbols, mode = mode, expectedReturnType = expectedReturnType)
    }
  }
}

fun dot(args: List<JsonElement>, symbols: JsonObject, mode: String, expectedReturnType: String): Any {
  when (mode) {
    LispConstants.VALIDATE -> {
      if (args.isEmpty())
        throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
      var symbolsJson: JsonObject = symbols
      val symbolPaths: List<String> = args.map {
        try {
          it.asString
        } catch (exception: Exception) {
          throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
        }
      }
      for ((index, symbolName) in symbolPaths.withIndex()) {
        if (symbolsJson.has(symbolName)) {
          if (index == (symbolPaths.size - 1)) {
            if (symbolsJson.get(symbolName).asJsonObject.has(SymbolConstants.SYMBOL_TYPE)) {
              return when (symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_TYPE).asString) {
                TypeConstants.TEXT -> when (expectedReturnType) {
                  TypeConstants.TEXT, TypeConstants.BLOB -> mutableSetOf(symbolPaths.joinToString(separator = "."))
                  else -> throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
                TypeConstants.NUMBER -> when (expectedReturnType) {
                  TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TIMESTAMP, TypeConstants.TIME, TypeConstants.DATE, TypeConstants.BLOB -> mutableSetOf(symbolPaths.joinToString(separator = "."))
                  else -> throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
                TypeConstants.DECIMAL -> when (expectedReturnType) {
                  TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BLOB -> mutableSetOf(symbolPaths.joinToString(separator = "."))
                  else -> throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
                TypeConstants.BOOLEAN -> when (expectedReturnType) {
                  TypeConstants.TEXT, TypeConstants.BOOLEAN, TypeConstants.BLOB -> mutableSetOf(symbolPaths.joinToString(separator = "."))
                  else -> throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
                TypeConstants.DATE -> when (expectedReturnType) {
                  TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TIMESTAMP, TypeConstants.TIME, TypeConstants.DATE, TypeConstants.BLOB -> mutableSetOf(symbolPaths.joinToString(separator = "."))
                  else -> throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
                TypeConstants.TIMESTAMP -> when (expectedReturnType) {
                  TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TIMESTAMP, TypeConstants.TIME, TypeConstants.DATE, TypeConstants.BLOB -> mutableSetOf(symbolPaths.joinToString(separator = "."))
                  else -> throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
                TypeConstants.TIME -> when (expectedReturnType) {
                  TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TIMESTAMP, TypeConstants.TIME, TypeConstants.BLOB -> mutableSetOf(symbolPaths.joinToString(separator = "."))
                  else -> throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
                TypeConstants.BLOB -> when (expectedReturnType) {
                  TypeConstants.BLOB -> mutableSetOf(symbolPaths.joinToString(separator = "."))
                  else -> throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
                else -> throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
              }
            } else throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
          } else {
            try {
              symbolsJson = symbolsJson.get(symbolName).asJsonObject.get("values").asJsonObject
            } catch (exception: Exception) {
              throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
            }
          }
        } else throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
        if (index == (symbolPaths.size - 1))
          throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
      }
      return mutableSetOf(symbolPaths.joinToString(separator = "."))
    }
    LispConstants.REFLECT -> JsonObject().apply {
      dot(args = args, symbols = symbols, mode = LispConstants.EVALUATE, expectedReturnType = expectedReturnType)
      addProperty(LispConstants.OPERATION, OperatorConstants.ADD)
      add(LispConstants.TYPES, JsonArray())
      add(LispConstants.ARGS, JsonArray().apply { args.forEach { add(it.asString) } })
    }
    else -> {
      var symbolsJson: JsonObject = symbols
      val symbolPaths: List<String> = args.map {
        try {
          it.asString
        } catch (exception: Exception) {
          throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
        }
      }
      for ((index, symbolName) in symbolPaths.withIndex()) {
        if (symbolsJson.has(symbolName)) {
          if (index == (symbolPaths.size - 1)) {
            if (symbolsJson.get(symbolName).asJsonObject.has(SymbolConstants.SYMBOL_TYPE)) {
              return when (symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_TYPE).asString) {
                TypeConstants.TEXT -> when (expectedReturnType) {
                  TypeConstants.TEXT -> symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asString
                  TypeConstants.BLOB -> Base64.getDecoder().decode(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asString)
                  else -> throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
                TypeConstants.NUMBER -> when (expectedReturnType) {
                  TypeConstants.TEXT -> symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asString
                  TypeConstants.NUMBER -> symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong
                  TypeConstants.DECIMAL -> symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asBigDecimal
                  TypeConstants.BLOB -> Base64.getDecoder().decode(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asString)
                  else -> throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
                TypeConstants.DECIMAL -> when (expectedReturnType) {
                  TypeConstants.TEXT -> symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asString
                  TypeConstants.NUMBER -> symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong
                  TypeConstants.DECIMAL -> symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asBigDecimal
                  TypeConstants.BLOB -> Base64.getDecoder().decode(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asString)
                  else -> throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
                TypeConstants.BOOLEAN -> when (expectedReturnType) {
                  TypeConstants.TEXT -> symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asString
                  TypeConstants.BOOLEAN -> symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asBoolean
                  TypeConstants.BLOB -> Base64.getDecoder().decode(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asString)
                  else -> throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
                TypeConstants.DATE -> when (expectedReturnType) {
                  TypeConstants.TEXT -> java.sql.Date(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong).time.toString()
                  TypeConstants.NUMBER -> symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong
                  TypeConstants.DECIMAL -> symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong.toBigDecimal()
                  TypeConstants.DATE -> java.sql.Date(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong)
                  TypeConstants.TIMESTAMP -> Timestamp(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong)
                  TypeConstants.TIME -> java.sql.Time(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong)
                  TypeConstants.BLOB -> Base64.getDecoder().decode(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong.toString())
                  else -> throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
                TypeConstants.TIMESTAMP -> when (expectedReturnType) {
                  TypeConstants.TEXT -> Timestamp(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong).toString()
                  TypeConstants.NUMBER -> symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong
                  TypeConstants.DECIMAL -> symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asBigDecimal
                  TypeConstants.DATE -> java.sql.Date(Timestamp(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong).time)
                  TypeConstants.TIMESTAMP -> Timestamp(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong)
                  TypeConstants.TIME -> java.sql.Time(Timestamp(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong).time)
                  TypeConstants.BLOB -> Base64.getDecoder().decode(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong.toString())
                  else -> throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
                TypeConstants.TIME -> when (expectedReturnType) {
                  TypeConstants.TEXT -> java.sql.Time(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong).toString()
                  TypeConstants.NUMBER -> symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong
                  TypeConstants.DECIMAL -> symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asBigDecimal
                  TypeConstants.DATE -> java.sql.Date(java.sql.Time(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong).time)
                  TypeConstants.TIMESTAMP -> Timestamp(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong)
                  TypeConstants.TIME -> java.sql.Time(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong)
                  TypeConstants.BLOB -> Base64.getDecoder().decode(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asLong.toString())
                  else -> throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
                TypeConstants.BLOB -> when (expectedReturnType) {
                  TypeConstants.BLOB -> Base64.getDecoder().decode(symbolsJson.get(symbolName).asJsonObject.get(SymbolConstants.SYMBOL_VALUE).asString)
                  else -> throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
                else -> throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
              }
            } else throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
          } else symbolsJson = symbolsJson.get(symbolName).asJsonObject.get("values").asJsonObject
        } else throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
        if (index == (symbolPaths.size - 1))
          throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
      }
    }
  }
  throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
}

fun identity(types: MutableList<String>, args: List<JsonElement>, mode: String, expectedReturnType: String): Any {
  return when (mode) {
    LispConstants.VALIDATE -> {
      if (types.isEmpty())
        throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
      if (args.isEmpty())
        throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
      val argType: String = try {
        types.first()
      } catch (exception: Exception) {
        throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
      }
      val expectedReturnTypes: List<String> = when (argType) {
        TypeConstants.TEXT -> listOf(TypeConstants.TEXT, TypeConstants.BLOB)
        TypeConstants.NUMBER, TypeConstants.DECIMAL -> listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT, TypeConstants.BLOB)
        TypeConstants.BOOLEAN -> listOf(TypeConstants.BOOLEAN, TypeConstants.TEXT, TypeConstants.BLOB)
        else -> throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
      }
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
      when (argType) {
        TypeConstants.TEXT -> try {
          args.first().asString
        } catch (exception: Exception) {
          throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
        }
        TypeConstants.NUMBER -> try {
          args.first().asLong
        } catch (exception: Exception) {
          throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
        }
        TypeConstants.DECIMAL -> try {
          args.first().asBigDecimal
        } catch (exception: Exception) {
          throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
        }
        else -> try {
          args.first().asBoolean
        } catch (exception: Exception) {
          throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
        }
      }
      mutableSetOf<String>()
    }
    LispConstants.REFLECT -> JsonObject().apply {
      addProperty(LispConstants.OPERATION, OperatorConstants.IDENTITY)
      add(LispConstants.TYPES, JsonArray().apply {
        if (types.isEmpty())
          throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
        add(types.first())
      })
      add(LispConstants.ARGS, JsonArray().apply {
        if (args.isEmpty())
          throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
        val argType: String = try {
          types.first()
        } catch (exception: Exception) {
          throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
        }
        val expectedReturnTypes: List<String> = when (argType) {
          TypeConstants.TEXT -> listOf(TypeConstants.TEXT, TypeConstants.BLOB)
          TypeConstants.NUMBER, TypeConstants.DECIMAL -> listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT, TypeConstants.BLOB)
          TypeConstants.BOOLEAN -> listOf(TypeConstants.BOOLEAN, TypeConstants.TEXT, TypeConstants.BLOB)
          else -> throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
        }
        if (!expectedReturnTypes.contains(expectedReturnType))
          throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
        when (argType) {
          TypeConstants.TEXT -> try {
            add(args.first().asString)
          } catch (exception: Exception) {
            throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
          }
          TypeConstants.NUMBER -> try {
            add(args.first().asLong)
          } catch (exception: Exception) {
            throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
          }
          TypeConstants.DECIMAL -> try {
            add(args.first().asBigDecimal)
          } catch (exception: Exception) {
            throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
          }
          else -> try {
            add(args.first().asBoolean)
          } catch (exception: Exception) {
            throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
          }
        }
      })
    }
    else -> when (types.first()) {
      TypeConstants.TEXT -> when (expectedReturnType) {
        TypeConstants.TEXT -> args.first().asString
        else -> Base64.getDecoder().decode(args.first().asString)
      }
      TypeConstants.NUMBER -> when (expectedReturnType) {
        TypeConstants.NUMBER -> args.first().asLong
        TypeConstants.DECIMAL -> args.first().asLong.toBigDecimal()
        TypeConstants.TEXT -> args.first().asLong.toString()
        else -> Base64.getDecoder().decode(args.first().asLong.toString())
      }
      TypeConstants.DECIMAL -> when (expectedReturnType) {
        TypeConstants.NUMBER -> args.first().asLong
        TypeConstants.DECIMAL -> args.first().asBigDecimal
        TypeConstants.TEXT -> args.first().asBigDecimal.toString()
        else -> Base64.getDecoder().decode(args.first().asBigDecimal.toString())
      }
      else -> when (expectedReturnType) {
        TypeConstants.BOOLEAN -> args.first().asBoolean
        TypeConstants.TEXT -> args.first().asBoolean.toString()
        else -> Base64.getDecoder().decode(args.first().asBoolean.toString())
      }
    }
  }
}
