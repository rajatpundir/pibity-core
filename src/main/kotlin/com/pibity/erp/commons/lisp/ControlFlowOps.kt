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

fun ifThenElse(args: List<JsonElement>, types: MutableList<String>, expectedReturnType: String, mode: String, symbols: JsonObject): Any {
  return when (mode) {
    "validate" -> {
      if (args.size != 3)
        throw CustomJsonException("{args: 'Unexpected value for parameter'}")
      if (types.size < 1)
        throw CustomJsonException("{types: 'Unexpected value for parameter'}")
      val expressionTypes: List<String> = listOf(types.first(), types.first())
      val expectedReturnTypes: List<String> = when (types.first()) {
        TypeConstants.TEXT -> listOf(TypeConstants.TEXT)
        TypeConstants.NUMBER, TypeConstants.DECIMAL -> listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT)
        TypeConstants.BOOLEAN -> listOf(TypeConstants.BOOLEAN, TypeConstants.TEXT)
        else -> throw CustomJsonException("{types: 'Unexpected value for parameter'}")
      }
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{expectedReturnType: 'Unexpected value for parameter'}")
      if (args.first().isJsonObject) {
        validateOrEvaluateExpression(jsonParams = args.first().asJsonObject.apply {
          addProperty("expectedReturnType", TypeConstants.BOOLEAN)
        }, mode = mode, symbols = symbols) as String
      } else {
        try {
          args.first().asBoolean
        } catch (exception: Exception) {
          throw CustomJsonException("{args: 'Unexpected value for parameter'}")
        }
      }
      args.drop(1).zip(expressionTypes).forEach { (arg, type) ->
        if (arg.isJsonObject) {
          validateOrEvaluateExpression(jsonParams = arg.asJsonObject.apply {
            addProperty("expectedReturnType", type)
          }, mode = mode, symbols = symbols) as String
        } else {
          try {
            when (type) {
              TypeConstants.TEXT -> arg.asString
              TypeConstants.NUMBER -> arg.asLong
              TypeConstants.DECIMAL -> arg.asBigDecimal
              else -> arg.asBoolean
            }
          } catch (exception: Exception) {
            throw CustomJsonException("{args: 'Unexpected value for parameter'}")
          }
        }
      }
      expectedReturnType
    }
    "collect" -> {
      if (args.size != 3)
        throw CustomJsonException("{args: 'Unexpected value for parameter'}")
      if (types.size < 1)
        throw CustomJsonException("{types: 'Unexpected value for parameter'}")
      val expressionTypes: List<String> = listOf(types.first(), types.first())
      val expectedReturnTypes: List<String> = when (types.first()) {
        TypeConstants.TEXT -> listOf(TypeConstants.TEXT)
        TypeConstants.NUMBER, TypeConstants.DECIMAL -> listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT)
        TypeConstants.BOOLEAN -> listOf(TypeConstants.BOOLEAN, TypeConstants.TEXT)
        else -> throw CustomJsonException("{types: 'Unexpected value for parameter'}")
      }
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{expectedReturnType: 'Unexpected value for parameter'}")
      val collectedSymbols = mutableSetOf<String>()
      if (args.first().isJsonObject) {
        collectedSymbols.addAll(validateOrEvaluateExpression(jsonParams = args.first().asJsonObject.apply {
          addProperty("expectedReturnType", TypeConstants.BOOLEAN)
        }, mode = mode, symbols = symbols) as Set<String>)
      }
      args.drop(1).zip(expressionTypes).forEach { (arg, type) ->
        if (arg.isJsonObject) {
          collectedSymbols.addAll(validateOrEvaluateExpression(jsonParams = arg.asJsonObject.apply {
            addProperty("expectedReturnType", type)
          }, mode = mode, symbols = symbols) as Set<String>)
        }
      }
      collectedSymbols
    }
    else -> {
      val condition: Boolean = if (args.first().isJsonObject) {
        validateOrEvaluateExpression(jsonParams = args.first().asJsonObject.apply {
          addProperty("expectedReturnType", TypeConstants.BOOLEAN)
        }, mode = mode, symbols = symbols) as Boolean
      } else args.first().asBoolean
      val expression = if (condition) args[1] else args[2]
      if (expression.isJsonObject) {
        val evaluatedArg = validateOrEvaluateExpression(jsonParams = expression.asJsonObject.apply {
          addProperty("expectedReturnType", types.first())
        }, mode = mode, symbols = symbols)
        when (types.first()) {
          TypeConstants.TEXT -> evaluatedArg as String
          TypeConstants.NUMBER -> {
            when (expectedReturnType) {
              TypeConstants.NUMBER -> evaluatedArg as Long
              TypeConstants.DECIMAL -> (evaluatedArg as Long).toBigDecimal()
              else -> (evaluatedArg as Long).toString()
            }
          }
          TypeConstants.DECIMAL -> {
            when (expectedReturnType) {
              TypeConstants.DECIMAL -> evaluatedArg as BigDecimal
              TypeConstants.NUMBER -> (evaluatedArg as BigDecimal).toLong()
              else -> (evaluatedArg as BigDecimal).toString()
            }
          }
          else -> {
            if (expectedReturnType == TypeConstants.BOOLEAN)
              evaluatedArg as Boolean
            else
              (evaluatedArg as Boolean).toString()
          }
        }
      } else {
        when (types.first()) {
          TypeConstants.TEXT -> expression.asString
          TypeConstants.NUMBER, TypeConstants.DECIMAL -> {
            when (expectedReturnType) {
              TypeConstants.DECIMAL -> expression.asBigDecimal
              TypeConstants.NUMBER -> expression.asLong
              else -> expression.asString
            }
          }
          else -> {
            if (expectedReturnType == TypeConstants.BOOLEAN)
              expression.asBoolean
            else
              expression.asString
          }
        }
      }
    }
  }
}
