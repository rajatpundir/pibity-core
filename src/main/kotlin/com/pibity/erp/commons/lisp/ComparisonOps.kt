/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons.lisp

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.LispConstants
import com.pibity.erp.commons.constants.MessageConstants
import com.pibity.erp.commons.constants.OperatorConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.utils.validateOrEvaluateExpression
import java.math.BigDecimal
import java.util.*

@Suppress("UNCHECKED_CAST")
fun compare(operator: String, types: MutableList<String>, args: List<JsonElement>, symbols: JsonObject, mode: String, expectedReturnType: String): Any {
  val acceptableTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT)
  val expectedReturnTypes = listOf(TypeConstants.BOOLEAN, TypeConstants.TEXT, TypeConstants.BLOB)
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
                TypeConstants.DECIMAL -> arg.asBigDecimal
                else -> arg.asString
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
      addProperty(LispConstants.OPERATION, operator)
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
      val argType: String = if (types.contains(TypeConstants.TEXT)) TypeConstants.TEXT
      else if (types.contains(TypeConstants.DECIMAL)) TypeConstants.DECIMAL
      else TypeConstants.NUMBER
      if (args.size < 2) {
        when (expectedReturnType) {
          TypeConstants.BOOLEAN -> args.isNotEmpty()
          TypeConstants.TEXT -> args.isNotEmpty().toString()
          TypeConstants.BLOB -> Base64.getDecoder().decode(args.isNotEmpty().toString())
          else -> throw CustomJsonException("")
        }
      } else {
        if (args.first().isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(expression = args.first().asJsonObject, symbols = symbols, mode = mode, expectedReturnType = types.first())
          when (types.first()) {
            TypeConstants.DECIMAL -> {
              when (argType) {
                TypeConstants.DECIMAL -> evaluatedArg as BigDecimal
                TypeConstants.NUMBER -> (evaluatedArg as BigDecimal).toLong()
                else -> evaluatedArg.toString()
              }
            }
            TypeConstants.NUMBER -> {
              when (argType) {
                TypeConstants.DECIMAL -> (evaluatedArg as Long).toBigDecimal()
                TypeConstants.NUMBER -> evaluatedArg as Long
                else -> evaluatedArg.toString()
              }
            }
            else -> evaluatedArg.toString()
          }
        } else {
          when (types.first()) {
            TypeConstants.DECIMAL -> {
              when (argType) {
                TypeConstants.DECIMAL -> args.first().asBigDecimal
                TypeConstants.NUMBER -> (args.first().asBigDecimal).toLong()
                else -> args.first().toString()
              }
            }
            TypeConstants.NUMBER -> {
              when (argType) {
                TypeConstants.DECIMAL -> (args.first().asLong).toBigDecimal()
                TypeConstants.NUMBER -> args.first().asLong
                else -> args.first().toString()
              }
            }
            else -> args.first().toString()
          }
        }
      }
      val result: Boolean = args.zip(types).map { (value, type) ->
        if (value.isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(expression = value.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = type)
          when (argType) {
            TypeConstants.DECIMAL -> {
              when (type) {
                TypeConstants.DECIMAL -> evaluatedArg as BigDecimal
                else -> (evaluatedArg as Long).toBigDecimal()
              }
            }
            TypeConstants.NUMBER -> {
              when (type) {
                TypeConstants.DECIMAL -> (evaluatedArg as BigDecimal).toLong()
                else -> evaluatedArg as Long
              }
            }
            else -> evaluatedArg.toString()
          }
        } else {
          when (argType) {
            TypeConstants.DECIMAL -> {
              when (type) {
                TypeConstants.DECIMAL -> value.asBigDecimal
                else -> value.asLong.toBigDecimal()
              }
            }
            TypeConstants.NUMBER -> value.asLong
            else -> value.asString
          }
        }
      }.zipWithNext().fold(true) { acc, (a, b) ->
        when (operator) {
          OperatorConstants.EQUALS -> {
            when (argType) {
              TypeConstants.DECIMAL -> acc && (a as BigDecimal == b as BigDecimal)
              TypeConstants.NUMBER -> acc && (a as Long == b as Long)
              else -> acc && (a as String == b as String)
            }
          }
          OperatorConstants.GREATER_OR_EQUALS -> {
            when (argType) {
              TypeConstants.DECIMAL -> acc && ((a as BigDecimal) >= b as BigDecimal)
              TypeConstants.NUMBER -> acc && ((a as Long) >= b as Long)
              else -> acc && ((a as String) >= b as String)
            }
          }
          OperatorConstants.LESS_OR_EQUALS -> {
            when (argType) {
              TypeConstants.DECIMAL -> acc && ((a as BigDecimal) <= b as BigDecimal)
              TypeConstants.NUMBER -> acc && ((a as Long) <= b as Long)
              else -> acc && ((a as String) <= b as String)
            }
          }
          OperatorConstants.GREATER_THAN -> {
            when (argType) {
              TypeConstants.DECIMAL -> acc && ((a as BigDecimal) > b as BigDecimal)
              TypeConstants.NUMBER -> acc && ((a as Long) > b as Long)
              else -> acc && ((a as String) > b as String)
            }
          }
          OperatorConstants.LESS_THAN -> {
            when (argType) {
              TypeConstants.DECIMAL -> acc && ((a as BigDecimal) < b as BigDecimal)
              TypeConstants.NUMBER -> acc && ((a as Long) < b as Long)
              else -> acc && ((a as String) < b as String)
            }
          }
          else -> throw CustomJsonException("{}")
        }
      }
      when(expectedReturnType) {
        TypeConstants.BOOLEAN -> result
        TypeConstants.TEXT -> result.toString()
        TypeConstants.BLOB -> Base64.getDecoder().decode(result.toString())
        else -> throw CustomJsonException("{}")
      }
    }
  }
}
