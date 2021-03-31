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
import com.pibity.core.utils.validateOrEvaluateExpression
import java.math.BigDecimal
import java.util.*

@Suppress("UNCHECKED_CAST")
fun ifThenElse(args: List<JsonElement>, types: MutableList<String>, symbols: JsonObject, mode: String, expectedReturnType: String): Any {
  return when (mode) {
    LispConstants.VALIDATE -> {
      if (args.size != 3)
        throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
      if (types.isEmpty())
        throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
      val expressionTypes: List<String> = listOf(types.first(), types.first())
      val expectedReturnTypes: List<String> = when (types.first()) {
        TypeConstants.TEXT -> listOf(TypeConstants.TEXT, TypeConstants.BLOB)
        TypeConstants.BLOB -> listOf(TypeConstants.BLOB)
        TypeConstants.NUMBER, TypeConstants.DECIMAL -> listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT, TypeConstants.BLOB)
        TypeConstants.BOOLEAN -> listOf(TypeConstants.BOOLEAN, TypeConstants.TEXT, TypeConstants.BLOB)
        else -> throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
      }
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{${LispConstants.EXPECTED_RETURN_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}")
      val collectedSymbols = mutableSetOf<String>()
      if (args.first().isJsonObject) {
        collectedSymbols.addAll(validateOrEvaluateExpression(expression = args.first().asJsonObject, symbols = symbols, mode = mode, expectedReturnType = TypeConstants.BOOLEAN) as Set<String>)
      } else {
        try {
          args.first().asBoolean
        } catch (exception: Exception) {
          throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
        }
      }
      args.drop(1).zip(expressionTypes).forEach { (arg, type) ->
        if (arg.isJsonObject) {
          collectedSymbols.addAll(validateOrEvaluateExpression(expression = arg.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = type) as Set<String>)
        } else {
          try {
            when (type) {
              TypeConstants.TEXT -> arg.asString
              TypeConstants.NUMBER -> arg.asLong
              TypeConstants.DECIMAL -> arg.asBigDecimal
              else -> arg.asBoolean
            }
          } catch (exception: Exception) {
            throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
          }
        }
      }
      collectedSymbols
    }
    LispConstants.REFLECT -> JsonObject().apply {
      addProperty(LispConstants.OPERATION, OperatorConstants.IF_THEN_ELSE)
      add(LispConstants.TYPES, JsonArray().apply {
        if (types.isEmpty())
          throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
        add(types.first())
      })
      add(LispConstants.ARGS, JsonArray().apply {
        if (args.size != 3)
          throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
        val expressionTypes: List<String> = listOf(types.first(), types.first())
        val expectedReturnTypes: List<String> = when (types.first()) {
          TypeConstants.TEXT -> listOf(TypeConstants.TEXT, TypeConstants.BLOB)
          TypeConstants.BLOB -> listOf(TypeConstants.BLOB)
          TypeConstants.NUMBER, TypeConstants.DECIMAL -> listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT, TypeConstants.BLOB)
          TypeConstants.BOOLEAN -> listOf(TypeConstants.BOOLEAN, TypeConstants.TEXT, TypeConstants.BLOB)
          else -> throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
        }
        if (!expectedReturnTypes.contains(expectedReturnType))
          throw CustomJsonException("{${LispConstants.EXPECTED_RETURN_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}")
        if (args.first().isJsonObject)
          add(validateOrEvaluateExpression(expression = args.first().asJsonObject, symbols = symbols, mode = mode, expectedReturnType = TypeConstants.BOOLEAN) as JsonObject)
        else
          add(args.first())
        args.drop(1).zip(expressionTypes).forEach { (arg, type) ->
          if (arg.isJsonObject)
            add(validateOrEvaluateExpression(expression = arg.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = type) as JsonObject)
          else
            add(arg)
        }
      })
    }
    else -> {
      val condition: Boolean = if (args.first().isJsonObject) {
        validateOrEvaluateExpression(expression = args.first().asJsonObject, symbols = symbols, mode = mode, expectedReturnType = TypeConstants.BOOLEAN) as Boolean
      } else args.first().asBoolean
      val expression = if (condition) args[1] else args[2]
      if (expression.isJsonObject) {
        val evaluatedArg = validateOrEvaluateExpression(expression = expression.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = types.first())
        when (types.first()) {
          TypeConstants.TEXT -> {
            when(expectedReturnType) {
              TypeConstants.TEXT -> evaluatedArg as String
              else -> Base64.getDecoder().decode(evaluatedArg as String)
            }
          }
          TypeConstants.NUMBER -> {
            when (expectedReturnType) {
              TypeConstants.NUMBER -> evaluatedArg as Long
              TypeConstants.DECIMAL -> (evaluatedArg as Long).toBigDecimal()
              TypeConstants.TEXT -> (evaluatedArg as Long).toString()
              else -> Base64.getDecoder().decode((evaluatedArg as Long).toString())
            }
          }
          TypeConstants.DECIMAL -> {
            when (expectedReturnType) {
              TypeConstants.DECIMAL -> evaluatedArg as BigDecimal
              TypeConstants.NUMBER -> (evaluatedArg as BigDecimal).toLong()
              TypeConstants.TEXT -> (evaluatedArg as BigDecimal).toString()
              else -> Base64.getDecoder().decode((evaluatedArg as BigDecimal).toString())
            }
          }
          TypeConstants.BOOLEAN -> {
            when(expectedReturnType) {
              TypeConstants.BOOLEAN -> evaluatedArg as Boolean
              TypeConstants.TEXT -> (evaluatedArg as Boolean).toString()
              else -> Base64.getDecoder().decode((evaluatedArg as Boolean).toString())
            }
          }
          else -> evaluatedArg as ByteArray
        }
      } else {
        when (types.first()) {
          TypeConstants.TEXT -> {
            when(expectedReturnType) {
              TypeConstants.TEXT -> expression.asString
              else -> Base64.getDecoder().decode(expression.asString)
            }
          }
          TypeConstants.NUMBER, TypeConstants.DECIMAL -> {
            when (expectedReturnType) {
              TypeConstants.DECIMAL -> expression.asBigDecimal
              TypeConstants.NUMBER -> expression.asLong
              TypeConstants.TEXT -> expression.asString
              else -> Base64.getDecoder().decode(expression.asString)
            }
          }
          TypeConstants.BOOLEAN -> {
            when(expectedReturnType) {
              TypeConstants.BOOLEAN -> expression.asBoolean
              TypeConstants.TEXT -> expression.asString
              else -> Base64.getDecoder().decode(expression.asString)
            }
          }
          else -> Base64.getDecoder().decode(expression.asString)
        }
      }
    }
  }
}
