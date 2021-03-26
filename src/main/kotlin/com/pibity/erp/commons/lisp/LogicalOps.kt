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
import com.pibity.erp.commons.constants.LispConstants
import com.pibity.erp.commons.constants.MessageConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.utils.validateOrEvaluateExpression
import java.util.*

@Suppress("UNCHECKED_CAST")
fun and(args: List<JsonElement>, symbols: JsonObject, mode: String, expectedReturnType: String): Any {
  val expectedReturnTypes = listOf(TypeConstants.BOOLEAN, TypeConstants.TEXT, TypeConstants.BLOB)
  return when (mode) {
    LispConstants.VALIDATE -> {
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{${LispConstants.EXPECTED_RETURN_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}")
      val collectedSymbols = mutableSetOf<String>()
      args.forEach {
        if (it.isJsonObject)
          collectedSymbols.addAll(validateOrEvaluateExpression(expression = it.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = TypeConstants.BOOLEAN) as Set<String>)
        else {
          try {
            it.asBoolean
          } catch (exception: Exception) {
            throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
          }
        }
      }
      collectedSymbols
    }
    else -> {
      val result: Boolean = args.fold(true) { acc, arg ->
        acc && if (arg.isJsonObject)
          validateOrEvaluateExpression(expression = arg.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = TypeConstants.BOOLEAN) as Boolean
        else
          arg.asBoolean
      }
      when (expectedReturnType) {
        TypeConstants.BOOLEAN -> result
        TypeConstants.TEXT -> result.toString()
        else -> Base64.getDecoder().decode(result.toString())
      }
    }
  }
}

@Suppress("UNCHECKED_CAST")
fun or(args: List<JsonElement>, symbols: JsonObject, mode: String, expectedReturnType: String): Any {
  val expectedReturnTypes = listOf(TypeConstants.BOOLEAN, TypeConstants.TEXT, TypeConstants.BLOB)
  return when (mode) {
    LispConstants.VALIDATE -> {
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{${LispConstants.EXPECTED_RETURN_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}")
      val collectedSymbols = mutableSetOf<String>()
      args.forEach {
        if (it.isJsonObject)
          collectedSymbols.addAll(validateOrEvaluateExpression(expression = it.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = TypeConstants.BOOLEAN) as Set<String>)
        else {
          try {
            it.asBoolean
          } catch (exception: Exception) {
            throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
          }
        }
      }
      collectedSymbols
    }
    else -> {
      val result: Boolean = args.fold(false) { acc, arg ->
        acc || if (arg.isJsonObject)
          validateOrEvaluateExpression(expression = arg.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = TypeConstants.BOOLEAN) as Boolean
        else
          arg.asBoolean
      }
      when (expectedReturnType) {
        TypeConstants.BOOLEAN -> result
        TypeConstants.TEXT -> result.toString()
        else -> Base64.getDecoder().decode(result.toString())
      }
    }
  }
}

@Suppress("UNCHECKED_CAST")
fun not(args: List<JsonElement>, symbols: JsonObject, mode: String, expectedReturnType: String): Any {
  val expectedReturnTypes = listOf(TypeConstants.BOOLEAN, TypeConstants.TEXT, TypeConstants.BLOB)
  return when (mode) {
    LispConstants.VALIDATE -> {
      if (!expectedReturnTypes.contains(expectedReturnType))
        throw CustomJsonException("{${LispConstants.EXPECTED_RETURN_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}")
      val collectedSymbols = mutableSetOf<String>()
      if (args.isNotEmpty()) {
        if (args.first().isJsonObject)
          collectedSymbols.addAll(validateOrEvaluateExpression(expression = args.first().asJsonObject, symbols = symbols, mode = mode, expectedReturnType = TypeConstants.BOOLEAN) as Set<String>)
        else {
          try {
            args.first().asBoolean
          } catch (exception: Exception) {
            throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
          }
        }
      }
      collectedSymbols
    }
    else -> {
      val result: Boolean = if (args.isEmpty()) false
      else {
        if (args.first().isJsonObject)
          !(validateOrEvaluateExpression(expression = args.first().asJsonObject, symbols = symbols, mode = mode, expectedReturnType = TypeConstants.BOOLEAN) as Boolean)
        else
          !args.first().asBoolean
      }
      when (expectedReturnType) {
        TypeConstants.BOOLEAN -> result
        TypeConstants.TEXT -> result.toString()
        else -> result.toString().toByteArray()
      }
    }
  }
}
