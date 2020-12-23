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

fun compare(args: List<JsonElement>, types: MutableList<String>, expectedReturnType: String, mode: String, symbols: JsonObject, operator: String): Any {
  val acceptableTypes = listOf(TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.TEXT)
  val expectedReturnTypes = listOf(TypeConstants.BOOLEAN, TypeConstants.TEXT)
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
                TypeConstants.DECIMAL -> arg.asBigDecimal
                else -> arg.asString
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
      val argType: String = if (types.contains(TypeConstants.TEXT)) TypeConstants.TEXT
      else if (types.contains(TypeConstants.DECIMAL)) TypeConstants.DECIMAL
      else TypeConstants.NUMBER
      val result: Boolean = args.isNotEmpty()
      if (args.size < 2) {
        when (expectedReturnType) {
          TypeConstants.BOOLEAN -> result
          TypeConstants.TEXT -> result.toString()
          else -> throw CustomJsonException("")
        }
      } else {
        if (args.first().isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(args.first().asJsonObject.apply { addProperty("expectedReturnType", types.first()) }, mode = "evaluate", symbols = symbols)
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
      args.zip(types).map { (value, type) ->
        if (value.isJsonObject) {
          val evaluatedArg = validateOrEvaluateExpression(value.asJsonObject.apply { addProperty("expectedReturnType", type) }, mode = "evaluate", symbols = symbols)
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
          ">" -> {
            when (argType) {
              TypeConstants.DECIMAL -> acc && ((a as BigDecimal) > b as BigDecimal)
              TypeConstants.NUMBER -> acc && ((a as Long) > b as Long)
              else -> acc && ((a as String) > b as String)
            }
          }
          ">=" -> {
            when (argType) {
              TypeConstants.DECIMAL -> acc && ((a as BigDecimal) >= b as BigDecimal)
              TypeConstants.NUMBER -> acc && ((a as Long) >= b as Long)
              else -> acc && ((a as String) >= b as String)
            }
          }
          "<" -> {
            when (argType) {
              TypeConstants.DECIMAL -> acc && ((a as BigDecimal) < b as BigDecimal)
              TypeConstants.NUMBER -> acc && ((a as Long) < b as Long)
              else -> acc && ((a as String) < b as String)
            }
          }
          "<=" -> {
            when (argType) {
              TypeConstants.DECIMAL -> acc && ((a as BigDecimal) <= b as BigDecimal)
              TypeConstants.NUMBER -> acc && ((a as Long) <= b as Long)
              else -> acc && ((a as String) <= b as String)
            }
          }
          "==" -> {
            when (argType) {
              TypeConstants.DECIMAL -> acc && (a as BigDecimal == b as BigDecimal)
              TypeConstants.NUMBER -> acc && (a as Long == b as Long)
              else -> acc && (a as String == b as String)
            }
          }
          else -> throw CustomJsonException("{}")
        }
      }
    }
  }
}
