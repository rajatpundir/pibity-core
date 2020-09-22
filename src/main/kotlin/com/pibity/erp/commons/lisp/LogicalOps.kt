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
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.utils.validateOrEvaluateExpression

fun and(args: List<JsonElement>, expectedReturnType: String, mode: String, symbols: JsonObject): Any {
  val expectedReturnTypes = listOf(TypeConstants.BOOLEAN, TypeConstants.TEXT)
  return when (mode) {
    "validate" -> {
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{expectedReturnType: 'Unexpected value for parameter'}")
      args.forEach {
        if (it.isJsonObject)
          validateOrEvaluateExpression(it.asJsonObject.apply { addProperty("expectedReturnType", TypeConstants.BOOLEAN) }, mode = mode, symbols = symbols) as String
        else {
          try {
            it.asBoolean
          } catch (exception: Exception) {
            throw CustomJsonException("{args: 'Unexpected value for parameter'}")
          }
        }
      }
      expectedReturnType
    }
    else -> {
      val result: Boolean = args.fold(true) { acc, arg ->
        acc && if (arg.isJsonObject)
          validateOrEvaluateExpression(arg.asJsonObject.apply { addProperty("expectedReturnType", TypeConstants.BOOLEAN) }, mode = "evaluate", symbols = symbols) as Boolean
        else
          arg.asBoolean
      }
      when (expectedReturnType) {
        TypeConstants.BOOLEAN -> result
        else -> result.toString()
      }
    }
  }
}

fun or(args: List<JsonElement>, expectedReturnType: String, mode: String, symbols: JsonObject): Any {
  val expectedReturnTypes = listOf(TypeConstants.BOOLEAN, TypeConstants.TEXT)
  return when (mode) {
    "validate" -> {
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{expectedReturnType: 'Unexpected value for parameter'}")
      args.forEach {
        if (it.isJsonObject)
          validateOrEvaluateExpression(it.asJsonObject.apply { addProperty("expectedReturnType", TypeConstants.BOOLEAN) }, mode = mode, symbols = symbols) as String
        else {
          try {
            it.asBoolean
          } catch (exception: Exception) {
            throw CustomJsonException("{args: 'Unexpected value for parameter'}")
          }
        }
      }
      expectedReturnType
    }
    else -> {
      val result: Boolean = args.fold(false) { acc, arg ->
        acc || if (arg.isJsonObject)
          validateOrEvaluateExpression(arg.asJsonObject.apply { addProperty("expectedReturnType", TypeConstants.BOOLEAN) }, mode = "evaluate", symbols = symbols) as Boolean
        else
          arg.asBoolean
      }
      when (expectedReturnType) {
        TypeConstants.BOOLEAN -> result
        else -> result.toString()
      }
    }
  }
}

fun not(args: List<JsonElement>, expectedReturnType: String, mode: String, symbols: JsonObject): Any {
  val expectedReturnTypes = listOf(TypeConstants.BOOLEAN, TypeConstants.TEXT)
  return when (mode) {
    "validate" -> {
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{expectedReturnType: 'Unexpected value for parameter'}")
      if (args.isNotEmpty()) {
        if (args.first().isJsonObject)
          validateOrEvaluateExpression(args.first().asJsonObject.apply { addProperty("expectedReturnType", TypeConstants.BOOLEAN) }, mode = mode, symbols = symbols) as String
        else {
          try {
            args.first().asBoolean
          } catch (exception: Exception) {
            throw CustomJsonException("{args: 'Unexpected value for parameter'}")
          }
        }
      }
      expectedReturnType
    }
    else -> {
      val result: Boolean = if (args.isEmpty()) false
      else {
        if (args.first().isJsonObject)
          !(validateOrEvaluateExpression(args.first().asJsonObject.apply { addProperty("expectedReturnType", TypeConstants.BOOLEAN) }, mode = "evaluate", symbols = symbols) as Boolean)
        else
          !args.first().asBoolean
      }
      when (expectedReturnType) {
        TypeConstants.BOOLEAN -> result
        else -> result.toString()
      }
    }
  }
}
