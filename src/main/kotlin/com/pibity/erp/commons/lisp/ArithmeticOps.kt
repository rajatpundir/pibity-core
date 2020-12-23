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
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.utils.validateOrEvaluateExpression
import java.math.BigDecimal
import kotlin.math.pow

fun add(args: List<JsonElement>, types: MutableList<String>, expectedReturnType: String, mode: String, symbols: JsonObject): Any {
  val acceptableTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL)
  val expectedReturnTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT)
  return when (mode) {
    "validate" -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{expectedReturnType: 'Unexpected value for parameter'}")
      args.zip(types) { arg, type ->
        if (!acceptableTypes.contains(type))
          throw CustomJsonException("{types: 'Unexpected value for parameter'}")
        else {
          if (arg.isJsonObject)
            validateOrEvaluateExpression(arg.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = mode, symbols = symbols) as String
          else {
            try {
              when (type) {
                TypeConstants.NUMBER -> arg.asLong
                else -> arg.asBigDecimal
              }
              type
            } catch (exception: Exception) {
              throw CustomJsonException("{args: 'Unexpected value for parameter'}")
            }
          }
        }
      }
      expectedReturnType
    }
    "collect" -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{expectedReturnType: 'Unexpected value for parameter'}")
      val collectedSymbols = mutableSetOf<String>()
      args.zip(types).forEach { (arg, type) ->
        if (!acceptableTypes.contains(type))
          throw CustomJsonException("{types: 'Unexpected value for parameter'}")
        else {
          if (arg.isJsonObject)
            collectedSymbols.addAll(validateOrEvaluateExpression(arg.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = mode, symbols = symbols) as Set<String>)
        }
      }
      collectedSymbols
    }
    else -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      val returnTypeIsDouble: Boolean = types.contains(TypeConstants.DECIMAL)
      val evaluatedExpression = args.zip(types).fold(if (returnTypeIsDouble) (0.0).toBigDecimal() else 0L) { acc, (arg, type) ->
        if (returnTypeIsDouble) {
          if (arg.isJsonObject) {
            val evaluatedArg = validateOrEvaluateExpression(arg.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = "evaluate", symbols = symbols)
            acc as BigDecimal + (if (type == TypeConstants.DECIMAL) evaluatedArg as BigDecimal else (evaluatedArg as Long).toBigDecimal())
          } else
            acc as BigDecimal + (if (type == TypeConstants.DECIMAL) arg.asBigDecimal else arg.asLong.toBigDecimal())
        } else {
          if (arg.isJsonObject) {
            val evaluatedArg = validateOrEvaluateExpression(arg.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = "evaluate", symbols = symbols)
            acc as Long + (if (type == TypeConstants.DECIMAL) (evaluatedArg as BigDecimal).toLong() else evaluatedArg as Long)
          } else
            acc as Long + (if (type == TypeConstants.DECIMAL) arg.asBigDecimal.toLong() else arg.asLong)
        }
      }
      when (expectedReturnType) {
        TypeConstants.DECIMAL -> if (returnTypeIsDouble) evaluatedExpression else (evaluatedExpression as Long).toBigDecimal()
        TypeConstants.NUMBER -> if (returnTypeIsDouble) (evaluatedExpression as BigDecimal).toLong() else evaluatedExpression
        TypeConstants.TEXT -> evaluatedExpression.toString()
        else -> throw CustomJsonException("{}")
      }
    }
  }
}

fun multiply(args: List<JsonElement>, types: MutableList<String>, expectedReturnType: String, mode: String, symbols: JsonObject): Any {
  val acceptableTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL)
  val expectedReturnTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT)
  return when (mode) {
    "validate" -> {
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{expectedReturnType: 'Unexpected value for parameter'}")
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      args.zip(types) { arg, type ->
        if (!acceptableTypes.contains(type))
          throw CustomJsonException("{types: 'Unexpected value for parameter'}")
        else {
          if (arg.isJsonObject)
            validateOrEvaluateExpression(arg.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = mode, symbols = symbols) as String
          else {
            try {
              when (type) {
                TypeConstants.NUMBER -> arg.asLong
                else -> arg.asBigDecimal
              }
              type
            } catch (exception: Exception) {
              throw CustomJsonException("{args: 'Unexpected value for parameter'}")
            }
          }
        }
      }
      expectedReturnType
    }
    "collect" -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{expectedReturnType: 'Unexpected value for parameter'}")
      val collectedSymbols = mutableSetOf<String>()
      args.zip(types).forEach { (arg, type) ->
        if (!acceptableTypes.contains(type))
          throw CustomJsonException("{types: 'Unexpected value for parameter'}")
        else {
          if (arg.isJsonObject)
            collectedSymbols.addAll(validateOrEvaluateExpression(arg.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = mode, symbols = symbols) as Set<String>)
        }
      }
      collectedSymbols
    }
    else -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      val returnTypeIsDouble: Boolean = types.contains(TypeConstants.DECIMAL)
      val evaluatedExpression = args.zip(types).fold(if (returnTypeIsDouble) (1.0).toBigDecimal() else 1L) { acc, (arg, type) ->
        if (returnTypeIsDouble) {
          if (arg.isJsonObject) {
            val evaluatedArg = validateOrEvaluateExpression(arg.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = "evaluate", symbols = symbols)
            acc as BigDecimal * (if (type == TypeConstants.DECIMAL) evaluatedArg as BigDecimal else (evaluatedArg as Long).toBigDecimal())
          } else
            acc as BigDecimal * (if (type == TypeConstants.DECIMAL) arg.asBigDecimal else arg.asLong.toBigDecimal())
        } else {
          if (arg.isJsonObject) {
            val evaluatedArg = validateOrEvaluateExpression(arg.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = "evaluate", symbols = symbols)
            acc as Long * (if (type == TypeConstants.DECIMAL) (evaluatedArg as BigDecimal).toLong() else evaluatedArg as Long)
          } else
            acc as Long * (if (type == TypeConstants.DECIMAL) arg.asBigDecimal.toLong() else arg.asLong)
        }
      }
      when (expectedReturnType) {
        TypeConstants.DECIMAL -> if (returnTypeIsDouble) evaluatedExpression else (evaluatedExpression as Long).toBigDecimal()
        TypeConstants.NUMBER -> if (returnTypeIsDouble) (evaluatedExpression as BigDecimal).toLong() else evaluatedExpression
        TypeConstants.TEXT -> evaluatedExpression.toString()
        else -> throw CustomJsonException("{}")
      }
    }
  }
}

fun subtract(args: List<JsonElement>, types: MutableList<String>, expectedReturnType: String, mode: String, symbols: JsonObject): Any {
  val acceptableTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL)
  val expectedReturnTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT)
  return when (mode) {
    "validate" -> {
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{expectedReturnType: 'Unexpected value for parameter'}")
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      args.zip(types) { arg, type ->
        if (!acceptableTypes.contains(type))
          throw CustomJsonException("{types: 'Unexpected value for parameter'}")
        else {
          if (arg.isJsonObject)
            validateOrEvaluateExpression(arg.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = mode, symbols = symbols) as String
          else {
            try {
              when (type) {
                TypeConstants.NUMBER -> arg.asLong
                else -> arg.asBigDecimal
              }
              type
            } catch (exception: Exception) {
              throw CustomJsonException("{args: 'Unexpected value for parameter'}")
            }
          }
        }
      }
      expectedReturnType
    }
    "collect" -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{expectedReturnType: 'Unexpected value for parameter'}")
      val collectedSymbols = mutableSetOf<String>()
      args.zip(types).forEach { (arg, type) ->
        if (!acceptableTypes.contains(type))
          throw CustomJsonException("{types: 'Unexpected value for parameter'}")
        else {
          if (arg.isJsonObject)
            collectedSymbols.addAll(validateOrEvaluateExpression(arg.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = mode, symbols = symbols) as Set<String>)
        }
      }
      collectedSymbols
    }
    else -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      val returnTypeIsDouble: Boolean = types.contains(TypeConstants.DECIMAL)
      val first: Any = if (args.isEmpty()) {
        if (returnTypeIsDouble) (0.0).toBigDecimal() else 0L
      } else {
        if (args.first().isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(args.first().asJsonObject.apply { addProperty("expectedReturnType", types.first()) }, mode = "evaluate", symbols = symbols)
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
            val evaluatedArg = validateOrEvaluateExpression(value.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = "evaluate", symbols = symbols)
            acc as BigDecimal - (if (type == TypeConstants.DECIMAL) evaluatedArg as BigDecimal else (evaluatedArg as Long).toBigDecimal())
          } else
            acc as BigDecimal - (if (type == TypeConstants.DECIMAL) value.asBigDecimal else value.asLong.toBigDecimal())
        } else {
          if (value.isJsonObject) {
            val evaluatedArg = validateOrEvaluateExpression(value.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = "evaluate", symbols = symbols)
            acc as Long - (if (type == TypeConstants.DECIMAL) (evaluatedArg as BigDecimal).toLong() else evaluatedArg as Long)
          } else
            acc as Long - (if (type == TypeConstants.DECIMAL) value.asBigDecimal.toLong() else value.asLong)
        }
      }
      when (expectedReturnType) {
        TypeConstants.DECIMAL -> if (returnTypeIsDouble) evaluatedExpression else (evaluatedExpression as Long).toBigDecimal()
        TypeConstants.NUMBER -> if (returnTypeIsDouble) (evaluatedExpression as BigDecimal).toLong() else evaluatedExpression
        TypeConstants.TEXT -> evaluatedExpression.toString()
        else -> throw CustomJsonException("{}")
      }
    }
  }
}

fun divide(args: List<JsonElement>, types: MutableList<String>, expectedReturnType: String, mode: String, symbols: JsonObject): Any {
  val acceptableTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL)
  val expectedReturnTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT)
  return when (mode) {
    "validate" -> {
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{expectedReturnType: 'Unexpected value for parameter'}")
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      args.zip(types) { arg, type ->
        if (!acceptableTypes.contains(type))
          throw CustomJsonException("{types: 'Unexpected value for parameter'}")
        else {
          if (arg.isJsonObject)
            validateOrEvaluateExpression(arg.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = mode, symbols = symbols) as String
          else {
            try {
              when (type) {
                TypeConstants.NUMBER -> arg.asLong
                else -> arg.asBigDecimal
              }
              type
            } catch (exception: Exception) {
              throw CustomJsonException("{args: 'Unexpected value for parameter'}")
            }
          }
        }
      }
      expectedReturnType
    }
    "collect" -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{expectedReturnType: 'Unexpected value for parameter'}")
      val collectedSymbols = mutableSetOf<String>()
      args.zip(types).forEach { (arg, type) ->
        if (!acceptableTypes.contains(type))
          throw CustomJsonException("{types: 'Unexpected value for parameter'}")
        else {
          if (arg.isJsonObject)
            collectedSymbols.addAll(validateOrEvaluateExpression(arg.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = mode, symbols = symbols) as Set<String>)
        }
      }
      collectedSymbols
    }
    else -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      val first: BigDecimal = if (args.isEmpty())
        (1.0).toBigDecimal()
      else {
        if (args.first().isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(args.first().asJsonObject.apply { addProperty("expectedReturnType", types.first()) }, mode = "evaluate", symbols = symbols)
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
          val evaluatedArg = validateOrEvaluateExpression(value.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = "evaluate", symbols = symbols)
          acc / (if (type == TypeConstants.DECIMAL) evaluatedArg as BigDecimal else (evaluatedArg as Long).toBigDecimal())
        } else
          acc / (if (type == TypeConstants.DECIMAL) value.asBigDecimal else value.asLong.toBigDecimal())
      }
      when (expectedReturnType) {
        TypeConstants.DECIMAL -> evaluatedExpression
        TypeConstants.NUMBER -> evaluatedExpression.toLong()
        TypeConstants.TEXT -> evaluatedExpression.toString()
        else -> throw CustomJsonException("{}")
      }
    }
  }
}

fun power(args: List<JsonElement>, types: MutableList<String>, expectedReturnType: String, mode: String, symbols: JsonObject): Any {
  val acceptableTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL)
  val expectedReturnTypes = listOf(TypeConstants.DECIMAL, TypeConstants.TEXT)
  return when (mode) {
    "validate" -> {
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{expectedReturnType: 'Unexpected value for parameter'}")
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      args.zip(types) { arg, type ->
        if (!acceptableTypes.contains(type))
          throw CustomJsonException("{types: 'Unexpected value for parameter'}")
        else {
          if (arg.isJsonObject)
            validateOrEvaluateExpression(arg.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = mode, symbols = symbols) as String
          else {
            try {
              when (type) {
                TypeConstants.NUMBER -> arg.asLong
                else -> arg.asBigDecimal
              }
              type
            } catch (exception: Exception) {
              throw CustomJsonException("{args: 'Unexpected value for parameter'}")
            }
          }
        }
      }
      expectedReturnType
    }
    "collect" -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{expectedReturnType: 'Unexpected value for parameter'}")
      val collectedSymbols = mutableSetOf<String>()
      args.zip(types).forEach { (arg, type) ->
        if (!acceptableTypes.contains(type))
          throw CustomJsonException("{types: 'Unexpected value for parameter'}")
        else {
          if (arg.isJsonObject)
            collectedSymbols.addAll(validateOrEvaluateExpression(arg.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = mode, symbols = symbols) as Set<String>)
        }
      }
      collectedSymbols
    }
    else -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      val first: BigDecimal = if (args.size < 2) (1.0).toBigDecimal() else {
        val v1: BigDecimal = if (args.first().isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(args.first().asJsonObject.apply { addProperty("expectedReturnType", types.first()) }, mode = "evaluate", symbols = symbols)
          if (types.first() == TypeConstants.DECIMAL)
            evaluatedArg as BigDecimal
          else
            (evaluatedArg as Long).toBigDecimal()
        } else args.first().asBigDecimal
        val v2: BigDecimal = if (args[1].isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(args[1].asJsonObject.apply { addProperty("expectedReturnType", types[1]) }, mode = "evaluate", symbols = symbols)
          if (types[1] == TypeConstants.DECIMAL)
            evaluatedArg as BigDecimal
          else
            (evaluatedArg as Long).toBigDecimal()
        } else args[1].asBigDecimal
        v1.toDouble().pow(v2.toDouble()).toBigDecimal()
      }
      val evaluatedExpression = args.drop(2).zip(types.drop(2)).fold(first) { acc, (value, type) ->
        if (value.isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(value.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = "evaluate", symbols = symbols)
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
        else -> throw CustomJsonException("{}")
      }
    }
  }
}

fun modulus(args: List<JsonElement>, types: MutableList<String>, expectedReturnType: String, mode: String, symbols: JsonObject): Any {
  val acceptableTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL)
  val expectedReturnTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT)
  return when (mode) {
    "validate" -> {
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{expectedReturnType: 'Unexpected value for parameter'}")
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      args.zip(types) { arg, type ->
        if (!acceptableTypes.contains(type))
          throw CustomJsonException("{types: 'Unexpected value for parameter'}")
        else {
          if (arg.isJsonObject)
            validateOrEvaluateExpression(arg.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = mode, symbols = symbols) as String
          else {
            try {
              when (type) {
                TypeConstants.NUMBER -> arg.asLong
                else -> arg.asBigDecimal
              }
              type
            } catch (exception: Exception) {
              throw CustomJsonException("{args: 'Unexpected value for parameter'}")
            }
          }
        }
      }
      expectedReturnType
    }
    "collect" -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{expectedReturnType: 'Unexpected value for parameter'}")
      val collectedSymbols = mutableSetOf<String>()
      args.zip(types).forEach { (arg, type) ->
        if (!acceptableTypes.contains(type))
          throw CustomJsonException("{types: 'Unexpected value for parameter'}")
        else {
          if (arg.isJsonObject)
            collectedSymbols.addAll(validateOrEvaluateExpression(arg.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = mode, symbols = symbols) as Set<String>)
        }
      }
      collectedSymbols
    }
    else -> {
      if (args.size > types.size)
        repeat(args.size - types.size) { types.add(types.last()) }
      val first = if (args.isEmpty())
        throw CustomJsonException("{}")
      else {
        if (args.first().isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(args.first().asJsonObject.apply { addProperty("expectedReturnType", types.first()) }, mode = "evaluate", symbols = symbols)
          if (types.first() == TypeConstants.DECIMAL)
            (evaluatedArg as BigDecimal).toLong()
          else
            evaluatedArg as Long
        } else
          args.first().asLong
      }
      val evaluatedExpression: Long = args.drop(1).zip(types.drop(1)).fold(first) { acc, (value, type) ->
        if (value.isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(value.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = "evaluate", symbols = symbols)
          acc % (if (type == TypeConstants.DECIMAL) (evaluatedArg as BigDecimal).toLong() else (evaluatedArg as Long))
        } else
          acc % value.asLong
      }
      when (expectedReturnType) {
        TypeConstants.NUMBER -> evaluatedExpression
        TypeConstants.DECIMAL -> evaluatedExpression.toBigDecimal()
        TypeConstants.TEXT -> evaluatedExpression.toString()
        else -> throw CustomJsonException("{}")
      }
    }
  }
}
