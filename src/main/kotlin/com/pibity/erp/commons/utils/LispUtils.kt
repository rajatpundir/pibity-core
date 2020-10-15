/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons.utils

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.lisp.*

fun validateOrEvaluateExpression(jsonParams: JsonObject, mode: String, symbols: JsonObject): Any {
  val op: String = if (!jsonParams.has("op"))
    throw CustomJsonException("{op: 'Field is missing in request body'}")
  else try {
    jsonParams.get("op").asString
  } catch (exception: Exception) {
    throw CustomJsonException("{op: 'Unexpected value for parameter'}")
  }
  val args: List<JsonElement> = if (!jsonParams.has("args"))
    throw CustomJsonException("{args: 'Field is missing in request body'}")
  else try {
    jsonParams.get("args").asJsonArray.map { it }
  } catch (exception: Exception) {
    throw CustomJsonException("{args: 'Unexpected value for parameter'}")
  }
  val types: MutableList<String> = if (!jsonParams.has("types"))
    throw CustomJsonException("{types: 'Field is missing in request body'}")
  else try {
    jsonParams.get("types").asJsonArray.map { it.asString } as MutableList<String>
  } catch (exception: Exception) {
    throw CustomJsonException("{types: 'Unexpected value for parameter'}")
  }
  val expectedReturnType: String = if (!jsonParams.has("expectedReturnType"))
    throw CustomJsonException("{expectedReturnType: 'Field is missing in request body'}")
  else try {
    jsonParams.get("expectedReturnType").asString
  } catch (exception: Exception) {
    throw CustomJsonException("{expectedReturnType: 'Unexpected value for parameter'}")
  }
  return try {
    when (op) {
      "+" -> add(args = args, types = types, expectedReturnType = expectedReturnType, mode = mode, symbols = symbols)
      "*" -> multiply(args = args, types = types, expectedReturnType = expectedReturnType, mode = mode, symbols = symbols)
      "-" -> subtract(args = args, types = types, expectedReturnType = expectedReturnType, mode = mode, symbols = symbols)
      "/" -> divide(args = args, types = types, expectedReturnType = expectedReturnType, mode = mode, symbols = symbols)
      "^" -> power(args = args, types = types, expectedReturnType = expectedReturnType, mode = mode, symbols = symbols)
      "%" -> modulus(args = args, types = types, expectedReturnType = expectedReturnType, mode = mode, symbols = symbols)
      "==", ">", "<", ">=", "<=" -> compare(args = args, types = types, expectedReturnType = expectedReturnType, mode = mode, symbols = symbols, operator = op)
      "and" -> and(args = args, expectedReturnType = expectedReturnType, mode = mode, symbols = symbols)
      "or" -> or(args = args, expectedReturnType = expectedReturnType, mode = mode, symbols = symbols)
      "not" -> not(args = args, expectedReturnType = expectedReturnType, mode = mode, symbols = symbols)
      "if" -> ifThenElse(args = args, types = types, expectedReturnType = expectedReturnType, mode = mode, symbols = symbols)
      "let" -> let(args = args, expectedReturnType = expectedReturnType, mode = mode, symbols = symbols)
      "." -> dot(args = args, expectedReturnType = expectedReturnType, mode = mode, symbols = symbols)
      "id" -> identity(args = args, types = types, expectedReturnType = expectedReturnType, mode = mode)
      else -> throw CustomJsonException("Invalid operation")
    }
  } catch (exception: CustomJsonException) {
    throw CustomJsonException("{${op}: ${exception.message}}")
  }
}
