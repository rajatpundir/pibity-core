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
import com.pibity.core.commons.constants.LispConstants
import com.pibity.core.commons.constants.MessageConstants
import com.pibity.core.commons.constants.OperatorConstants
import com.pibity.core.commons.constants.TypeConstants
import com.pibity.core.commons.exceptions.CustomJsonException
import com.pibity.core.commons.utils.validateOrEvaluateExpression
import java.math.BigDecimal
import java.util.*
import kotlin.math.pow

@Suppress("UNCHECKED_CAST")
fun add(types: MutableList<String>, args: List<JsonElement>, symbols: JsonObject, mode: String, expectedReturnType: String): Any {
  val acceptableTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL)
  val expectedReturnTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT, TypeConstants.BLOB)
  return when (mode) {
    LispConstants.VALIDATE -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{${LispConstants.EXPECTED_RETURN_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}")
      val collectedSymbols = mutableSetOf<String>()
      args.zip(types) { arg, type ->
        if (!acceptableTypes.contains(type))
          throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
        else {
          if (arg.isJsonObject)
            collectedSymbols.addAll(validateOrEvaluateExpression(expression = arg.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = type) as Set<String>)
          else {
            try {
              when (type) {
                TypeConstants.NUMBER -> arg.asLong
                else -> arg.asBigDecimal
              }
              type
            } catch (exception: Exception) {
              throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
            }
          }
        }
      }
      collectedSymbols
    }
    LispConstants.REFLECT -> JsonObject().apply {
      addProperty(LispConstants.OPERATION, OperatorConstants.ADD)
      add(LispConstants.TYPES, JsonArray().apply { types.dropLast(args.size - types.size).forEach { add(it) } })
      add(LispConstants.ARGS, JsonArray().apply {
        if (args.size > types.size)
          repeat(args.size - types.size) { types.add(types.last()) }
        if (!expectedReturnTypes.contains(expectedReturnType))
          throw CustomJsonException("{${LispConstants.EXPECTED_RETURN_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}")
        args.zip(types) { arg, type ->
          if (!acceptableTypes.contains(type))
            throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
          else {
            if (arg.isJsonObject)
              add(validateOrEvaluateExpression(expression = arg.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = type) as JsonObject)
            else
              add(arg)
          }
        }
      })
    }
    else -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      val returnTypeIsDouble: Boolean = types.contains(TypeConstants.DECIMAL)
      val evaluatedExpression = args.zip(types).fold(if (returnTypeIsDouble) (0.0).toBigDecimal() else 0L) { acc, (arg, type) ->
        if (returnTypeIsDouble) {
          if (arg.isJsonObject) {
            val evaluatedArg = validateOrEvaluateExpression(expression = arg.asJsonObject,symbols = symbols,  mode = mode, expectedReturnType = type)
            acc as BigDecimal + (if (type == TypeConstants.DECIMAL) evaluatedArg as BigDecimal else (evaluatedArg as Long).toBigDecimal())
          } else
            acc as BigDecimal + (if (type == TypeConstants.DECIMAL) arg.asBigDecimal else arg.asLong.toBigDecimal())
        } else {
          if (arg.isJsonObject) {
            val evaluatedArg = validateOrEvaluateExpression(expression = arg.asJsonObject,symbols = symbols,  mode = mode, expectedReturnType = type)
            acc as Long + (if (type == TypeConstants.DECIMAL) (evaluatedArg as BigDecimal).toLong() else evaluatedArg as Long)
          } else
            acc as Long + (if (type == TypeConstants.DECIMAL) arg.asBigDecimal.toLong() else arg.asLong)
        }
      }
      when (expectedReturnType) {
        TypeConstants.DECIMAL -> if (returnTypeIsDouble) evaluatedExpression else (evaluatedExpression as Long).toBigDecimal()
        TypeConstants.NUMBER -> if (returnTypeIsDouble) (evaluatedExpression as BigDecimal).toLong() else evaluatedExpression
        TypeConstants.TEXT -> evaluatedExpression.toString()
        TypeConstants.BLOB -> Base64.getDecoder().decode(evaluatedExpression.toString())
        else -> throw CustomJsonException("{}")
      }
    }
  }
}

@Suppress("UNCHECKED_CAST")
fun multiply(types: MutableList<String>, args: List<JsonElement>, symbols: JsonObject, mode: String, expectedReturnType: String): Any {
  val acceptableTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL)
  val expectedReturnTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT, TypeConstants.BLOB)
  return when (mode) {
    LispConstants.VALIDATE -> {
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{${LispConstants.EXPECTED_RETURN_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}")
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      val collectedSymbols = mutableSetOf<String>()
      args.zip(types) { arg, type ->
        if (!acceptableTypes.contains(type))
          throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
        else {
          if (arg.isJsonObject)
            collectedSymbols.addAll(validateOrEvaluateExpression(expression = arg.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = type) as Set<String>)
          else {
            try {
              when (type) {
                TypeConstants.NUMBER -> arg.asLong
                else -> arg.asBigDecimal
              }
              type
            } catch (exception: Exception) {
              throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
            }
          }
        }
      }
      collectedSymbols
    }
    LispConstants.REFLECT -> JsonObject().apply {
      addProperty(LispConstants.OPERATION, OperatorConstants.MULTIPLY)
      add(LispConstants.TYPES, JsonArray().apply { types.dropLast(args.size - types.size).forEach { add(it) } })
      add(LispConstants.ARGS, JsonArray().apply {
        if (args.size > types.size)
          repeat(args.size - types.size) { types.add(types.last()) }
        if (!expectedReturnTypes.contains(expectedReturnType))
          throw CustomJsonException("{${LispConstants.EXPECTED_RETURN_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}")
        args.zip(types) { arg, type ->
          if (!acceptableTypes.contains(type))
            throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
          else {
            if (arg.isJsonObject)
              add(validateOrEvaluateExpression(expression = arg.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = type) as JsonObject)
            else
              add(arg)
          }
        }
      })
    }
    else -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      val returnTypeIsDouble: Boolean = types.contains(TypeConstants.DECIMAL)
      val evaluatedExpression = args.zip(types).fold(if (returnTypeIsDouble) (1.0).toBigDecimal() else 1L) { acc, (arg, type) ->
        if (returnTypeIsDouble) {
          if (arg.isJsonObject) {
            val evaluatedArg = validateOrEvaluateExpression(expression = arg.asJsonObject,symbols = symbols,  mode = mode, expectedReturnType = type)
            acc as BigDecimal * (if (type == TypeConstants.DECIMAL) evaluatedArg as BigDecimal else (evaluatedArg as Long).toBigDecimal())
          } else
            acc as BigDecimal * (if (type == TypeConstants.DECIMAL) arg.asBigDecimal else arg.asLong.toBigDecimal())
        } else {
          if (arg.isJsonObject) {
            val evaluatedArg = validateOrEvaluateExpression(expression = arg.asJsonObject,symbols = symbols,  mode = mode, expectedReturnType = type)
            acc as Long * (if (type == TypeConstants.DECIMAL) (evaluatedArg as BigDecimal).toLong() else evaluatedArg as Long)
          } else
            acc as Long * (if (type == TypeConstants.DECIMAL) arg.asBigDecimal.toLong() else arg.asLong)
        }
      }
      when (expectedReturnType) {
        TypeConstants.DECIMAL -> if (returnTypeIsDouble) evaluatedExpression else (evaluatedExpression as Long).toBigDecimal()
        TypeConstants.NUMBER -> if (returnTypeIsDouble) (evaluatedExpression as BigDecimal).toLong() else evaluatedExpression
        TypeConstants.TEXT -> evaluatedExpression.toString()
        TypeConstants.BLOB -> Base64.getDecoder().decode(evaluatedExpression.toString())
        else -> throw CustomJsonException("{}")
      }
    }
  }
}

@Suppress("UNCHECKED_CAST")
fun subtract(types: MutableList<String>, args: List<JsonElement>, symbols: JsonObject, mode: String, expectedReturnType: String): Any {
  val acceptableTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL)
  val expectedReturnTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT, TypeConstants.BLOB)
  return when (mode) {
    LispConstants.VALIDATE -> {
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{${LispConstants.EXPECTED_RETURN_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}")
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      val collectedSymbols = mutableSetOf<String>()
      args.zip(types) { arg, type ->
        if (!acceptableTypes.contains(type))
          throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
        else {
          if (arg.isJsonObject)
            collectedSymbols.addAll(validateOrEvaluateExpression(expression = arg.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = type) as Set<String>)
          else {
            try {
              when (type) {
                TypeConstants.NUMBER -> arg.asLong
                else -> arg.asBigDecimal
              }
              type
            } catch (exception: Exception) {
              throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
            }
          }
        }
      }
      collectedSymbols
    }
    LispConstants.REFLECT -> JsonObject().apply {
      addProperty(LispConstants.OPERATION, OperatorConstants.SUBTRACT)
      add(LispConstants.TYPES, JsonArray().apply { types.dropLast(args.size - types.size).forEach { add(it) } })
      add(LispConstants.ARGS, JsonArray().apply {
        if (args.size > types.size)
          repeat(args.size - types.size) { types.add(types.last()) }
        if (!expectedReturnTypes.contains(expectedReturnType))
          throw CustomJsonException("{${LispConstants.EXPECTED_RETURN_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}")
        args.zip(types) { arg, type ->
          if (!acceptableTypes.contains(type))
            throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
          else {
            if (arg.isJsonObject)
              add(validateOrEvaluateExpression(expression = arg.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = type) as JsonObject)
            else
              add(arg)
          }
        }
      })
    }
    else -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      val returnTypeIsDouble: Boolean = types.contains(TypeConstants.DECIMAL)
      val first: Any = if (args.isEmpty()) {
        if (returnTypeIsDouble) (0.0).toBigDecimal() else 0L
      } else {
        if (args.first().isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(expression = args.first().asJsonObject, symbols = symbols, mode = mode, expectedReturnType = types.first())
          if (types.first() == TypeConstants.DECIMAL) {
            if (returnTypeIsDouble)
              evaluatedArg as BigDecimal
            else
              (evaluatedArg as BigDecimal).toLong()
          } else {
            if (returnTypeIsDouble)
              (evaluatedArg as Long).toBigDecimal()
            else
              evaluatedArg as Long
          }
        } else {
          if (types.first() == TypeConstants.DECIMAL) {
            if (returnTypeIsDouble)
              args.first().asBigDecimal
            else
              (args.first().asBigDecimal).toLong()
          } else {
            if (returnTypeIsDouble)
              (args.first().asLong).toBigDecimal()
            else
              args.first().asLong
          }
        }
      }
      val evaluatedExpression = args.drop(1).zip(types.drop(1)).fold(first) { acc, (value, type) ->
        if (returnTypeIsDouble) {
          if (value.isJsonObject) {
            val evaluatedArg = validateOrEvaluateExpression(expression = value.asJsonObject,symbols = symbols,  mode = mode, expectedReturnType = type)
            acc as BigDecimal - (if (type == TypeConstants.DECIMAL) evaluatedArg as BigDecimal else (evaluatedArg as Long).toBigDecimal())
          } else
            acc as BigDecimal - (if (type == TypeConstants.DECIMAL) value.asBigDecimal else value.asLong.toBigDecimal())
        } else {
          if (value.isJsonObject) {
            val evaluatedArg = validateOrEvaluateExpression(expression = value.asJsonObject,symbols = symbols,  mode = mode, expectedReturnType =  type)
            acc as Long - (if (type == TypeConstants.DECIMAL) (evaluatedArg as BigDecimal).toLong() else evaluatedArg as Long)
          } else
            acc as Long - (if (type == TypeConstants.DECIMAL) value.asBigDecimal.toLong() else value.asLong)
        }
      }
      when (expectedReturnType) {
        TypeConstants.DECIMAL -> if (returnTypeIsDouble) evaluatedExpression else (evaluatedExpression as Long).toBigDecimal()
        TypeConstants.NUMBER -> if (returnTypeIsDouble) (evaluatedExpression as BigDecimal).toLong() else evaluatedExpression
        TypeConstants.TEXT -> evaluatedExpression.toString()
        TypeConstants.BLOB -> Base64.getDecoder().decode(evaluatedExpression.toString())
        else -> throw CustomJsonException("{}")
      }
    }
  }
}

@Suppress("UNCHECKED_CAST")
fun divide(types: MutableList<String>, args: List<JsonElement>, symbols: JsonObject, mode: String, expectedReturnType: String): Any {
  val acceptableTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL)
  val expectedReturnTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT, TypeConstants.BLOB)
  return when (mode) {
    LispConstants.VALIDATE -> {
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{${LispConstants.EXPECTED_RETURN_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}")
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      val collectedSymbols = mutableSetOf<String>()
      args.zip(types) { arg, type ->
        if (!acceptableTypes.contains(type))
          throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
        else {
          if (arg.isJsonObject)
            collectedSymbols.addAll(validateOrEvaluateExpression(expression = arg.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = type) as Set<String>)
          else {
            try {
              when (type) {
                TypeConstants.NUMBER -> arg.asLong
                else -> arg.asBigDecimal
              }
              type
            } catch (exception: Exception) {
              throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
            }
          }
        }
      }
      collectedSymbols
    }
    LispConstants.REFLECT -> JsonObject().apply {
      addProperty(LispConstants.OPERATION, OperatorConstants.DIVIDE)
      add(LispConstants.TYPES, JsonArray().apply { types.dropLast(args.size - types.size).forEach { add(it) } })
      add(LispConstants.ARGS, JsonArray().apply {
        if (args.size > types.size)
          repeat(args.size - types.size) { types.add(types.last()) }
        if (!expectedReturnTypes.contains(expectedReturnType))
          throw CustomJsonException("{${LispConstants.EXPECTED_RETURN_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}")
        args.zip(types) { arg, type ->
          if (!acceptableTypes.contains(type))
            throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
          else {
            if (arg.isJsonObject)
              add(validateOrEvaluateExpression(expression = arg.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = type) as JsonObject)
            else
              add(arg)
          }
        }
      })
    }
    else -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      val first: BigDecimal = if (args.isEmpty())
        (1.0).toBigDecimal()
      else {
        if (args.first().isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(expression = args.first().asJsonObject, symbols = symbols, mode = mode, expectedReturnType = types.first())
          if (types.first() == TypeConstants.DECIMAL)
            evaluatedArg as BigDecimal
          else
            (evaluatedArg as Long).toBigDecimal()
        } else {
          if (types.first() == TypeConstants.DECIMAL)
            args.first().asBigDecimal
          else
            (args.first().asLong).toBigDecimal()
        }
      }
      val evaluatedExpression: BigDecimal = args.drop(1).zip(types.drop(1)).fold(first) { acc, (value, type) ->
        if (value.isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(expression = value.asJsonObject,symbols = symbols,  mode = mode, expectedReturnType =  type)
          acc / (if (type == TypeConstants.DECIMAL) evaluatedArg as BigDecimal else (evaluatedArg as Long).toBigDecimal())
        } else
          acc / (if (type == TypeConstants.DECIMAL) value.asBigDecimal else value.asLong.toBigDecimal())
      }
      when (expectedReturnType) {
        TypeConstants.DECIMAL -> evaluatedExpression
        TypeConstants.NUMBER -> evaluatedExpression.toLong()
        TypeConstants.TEXT -> evaluatedExpression.toString()
        TypeConstants.BLOB -> Base64.getDecoder().decode(evaluatedExpression.toString())
        else -> throw CustomJsonException("{}")
      }
    }
  }
}

@Suppress("UNCHECKED_CAST")
fun power(types: MutableList<String>, args: List<JsonElement>, symbols: JsonObject, mode: String, expectedReturnType: String): Any {
  val acceptableTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL)
  val expectedReturnTypes = listOf(TypeConstants.DECIMAL, TypeConstants.TEXT, TypeConstants.BLOB)
  return when (mode) {
    LispConstants.VALIDATE -> {
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{${LispConstants.EXPECTED_RETURN_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}")
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      val collectedSymbols = mutableSetOf<String>()
      args.zip(types) { arg, type ->
        if (!acceptableTypes.contains(type))
          throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
        else {
          if (arg.isJsonObject)
            collectedSymbols.addAll(validateOrEvaluateExpression(expression = arg.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = type) as Set<String>)
          else {
            try {
              when (type) {
                TypeConstants.NUMBER -> arg.asLong
                else -> arg.asBigDecimal
              }
              type
            } catch (exception: Exception) {
              throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
            }
          }
        }
      }
      collectedSymbols
    }
    LispConstants.REFLECT -> JsonObject().apply {
      addProperty(LispConstants.OPERATION, OperatorConstants.POWER)
      add(LispConstants.TYPES, JsonArray().apply { types.dropLast(args.size - types.size).forEach { add(it) } })
      add(LispConstants.ARGS, JsonArray().apply {
        if (args.size > types.size)
          repeat(args.size - types.size) { types.add(types.last()) }
        if (!expectedReturnTypes.contains(expectedReturnType))
          throw CustomJsonException("{${LispConstants.EXPECTED_RETURN_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}")
        args.zip(types) { arg, type ->
          if (!acceptableTypes.contains(type))
            throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
          else {
            if (arg.isJsonObject)
              add(validateOrEvaluateExpression(expression = arg.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = type) as JsonObject)
            else
              add(arg)
          }
        }
      })
    }
    else -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      val first: BigDecimal = if (args.size < 2) (1.0).toBigDecimal() else {
        val v1: BigDecimal = if (args.first().isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(expression = args.first().asJsonObject, symbols = symbols, mode = mode, expectedReturnType = types.first())
          if (types.first() == TypeConstants.DECIMAL)
            evaluatedArg as BigDecimal
          else
            (evaluatedArg as Long).toBigDecimal()
        } else args.first().asBigDecimal
        val v2: BigDecimal = if (args[1].isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(expression = args[1].asJsonObject,symbols = symbols,  mode = mode, expectedReturnType = types[1])
          if (types[1] == TypeConstants.DECIMAL)
            evaluatedArg as BigDecimal
          else
            (evaluatedArg as Long).toBigDecimal()
        } else args[1].asBigDecimal
        v1.toDouble().pow(v2.toDouble()).toBigDecimal()
      }
      val evaluatedExpression = args.drop(2).zip(types.drop(2)).fold(first) { acc, (value, type) ->
        if (value.isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(expression = value.asJsonObject,symbols = symbols,  mode = mode, expectedReturnType = type)
          if (type == TypeConstants.DECIMAL)
            acc.toDouble().pow(evaluatedArg as Double).toBigDecimal()
          else
            acc.toDouble().pow((evaluatedArg as Long).toDouble()).toBigDecimal()
        } else
          acc.toDouble().pow(value.asDouble).toBigDecimal()
      }
      when (expectedReturnType) {
        TypeConstants.DECIMAL -> evaluatedExpression
        TypeConstants.TEXT -> evaluatedExpression.toString()
        TypeConstants.BLOB -> Base64.getDecoder().decode(evaluatedExpression.toString())
        else -> throw CustomJsonException("{}")
      }
    }
  }
}

@Suppress("UNCHECKED_CAST")
fun modulus(types: MutableList<String>, args: List<JsonElement>, symbols: JsonObject, mode: String, expectedReturnType: String): Any {
  val acceptableTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL)
  val expectedReturnTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT, TypeConstants.BLOB)
  return when (mode) {
    LispConstants.VALIDATE -> {
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{${LispConstants.EXPECTED_RETURN_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}")
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      val collectedSymbols = mutableSetOf<String>()
      args.zip(types) { arg, type ->
        if (!acceptableTypes.contains(type))
          throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
        else {
          if (arg.isJsonObject)
            collectedSymbols.addAll(validateOrEvaluateExpression(expression = arg.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = type) as Set<String>)
          else {
            try {
              when (type) {
                TypeConstants.NUMBER -> arg.asLong
                else -> arg.asBigDecimal
              }
              type
            } catch (exception: Exception) {
              throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
            }
          }
        }
      }
      collectedSymbols
    }
    LispConstants.REFLECT -> JsonObject().apply {
      addProperty(LispConstants.OPERATION, OperatorConstants.MODULUS)
      add(LispConstants.TYPES, JsonArray().apply { types.dropLast(args.size - types.size).forEach { add(it) } })
      add(LispConstants.ARGS, JsonArray().apply {
        if (args.size > types.size)
          repeat(args.size - types.size) { types.add(types.last()) }
        if (!expectedReturnTypes.contains(expectedReturnType))
          throw CustomJsonException("{${LispConstants.EXPECTED_RETURN_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}")
        args.zip(types) { arg, type ->
          if (!acceptableTypes.contains(type))
            throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
          else {
            if (arg.isJsonObject)
              add(validateOrEvaluateExpression(expression = arg.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = type) as JsonObject)
            else
              add(arg)
          }
        }
      })
    }
    else -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      val first = if (args.isEmpty())
        throw CustomJsonException("{}")
      else {
        if (args.first().isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(expression = args.first().asJsonObject, symbols = symbols, mode = mode, expectedReturnType = types.first())
          if (types.first() == TypeConstants.DECIMAL)
            (evaluatedArg as BigDecimal).toLong()
          else
            evaluatedArg as Long
        } else
          args.first().asLong
      }
      val evaluatedExpression: Long = args.drop(1).zip(types.drop(1)).fold(first) { acc, (value, type) ->
        if (value.isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(expression = value.asJsonObject,symbols = symbols,  mode = mode, expectedReturnType =  type)
          acc % (if (type == TypeConstants.DECIMAL) (evaluatedArg as BigDecimal).toLong() else (evaluatedArg as Long))
        } else
          acc % value.asLong
      }
      when (expectedReturnType) {
        TypeConstants.NUMBER -> evaluatedExpression
        TypeConstants.DECIMAL -> evaluatedExpression.toBigDecimal()
        TypeConstants.TEXT -> evaluatedExpression.toString()
        TypeConstants.BLOB -> Base64.getDecoder().decode(evaluatedExpression.toString())
        else -> throw CustomJsonException("{}")
      }
    }
  }
}
