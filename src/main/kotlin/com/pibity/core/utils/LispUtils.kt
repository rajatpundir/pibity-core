/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.utils

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pibity.core.commons.constants.LispConstants
import com.pibity.core.commons.constants.MessageConstants
import com.pibity.core.commons.constants.OperatorConstants
import com.pibity.core.commons.CustomJsonException
import com.pibity.core.commons.lisp.*

fun validateOrEvaluateExpression(expression: JsonObject, symbols: JsonObject, mode: String, expectedReturnType: String): Any {
  val op: String = try {
    expression.get(LispConstants.OPERATION).asString
  } catch (exception: Exception) {
    throw CustomJsonException("{${LispConstants.OPERATION}: ${MessageConstants.UNEXPECTED_VALUE}}")
  }
  val args: List<JsonElement> = try {
    expression.get(LispConstants.ARGS).asJsonArray.map { it }
  } catch (exception: Exception) {
    throw CustomJsonException("{${LispConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
  }
  val types: MutableList<String> = try {
    expression.get(LispConstants.TYPES).asJsonArray.map { it.asString } as MutableList<String>
  } catch (exception: Exception) {
    throw CustomJsonException("{${LispConstants.TYPES}: ${MessageConstants.UNEXPECTED_VALUE}}")
  }
  return try {
    when (op) {
      OperatorConstants.ADD -> add(types = types, args = args, symbols = symbols, mode = mode, expectedReturnType = expectedReturnType)
      OperatorConstants.MULTIPLY -> multiply(types = types, args = args, symbols = symbols, mode = mode, expectedReturnType = expectedReturnType)
      OperatorConstants.SUBTRACT -> subtract(types = types, args = args, symbols = symbols, mode = mode, expectedReturnType = expectedReturnType)
      OperatorConstants.DIVIDE -> divide(types = types, args = args, symbols = symbols, mode = mode, expectedReturnType = expectedReturnType)
      OperatorConstants.POWER -> power(types = types, args = args, symbols = symbols, mode = mode, expectedReturnType = expectedReturnType)
      OperatorConstants.MODULUS -> modulus(types = types, args = args, symbols = symbols, mode = mode, expectedReturnType = expectedReturnType)
      OperatorConstants.EQUALS, OperatorConstants.GREATER_THAN, OperatorConstants.LESS_THAN, OperatorConstants.GREATER_OR_EQUALS, OperatorConstants.LESS_OR_EQUALS ->
        compare(operator = op, types = types, args = args, symbols = symbols, mode = mode, expectedReturnType = expectedReturnType)
      OperatorConstants.AND -> and(args = args, symbols = symbols, mode = mode, expectedReturnType = expectedReturnType)
      OperatorConstants.OR -> or(args = args, symbols = symbols, mode = mode, expectedReturnType = expectedReturnType)
      OperatorConstants.NOT -> not(args = args, symbols = symbols, mode = mode, expectedReturnType = expectedReturnType)
      OperatorConstants.IF_THEN_ELSE -> ifThenElse(types = types, args = args, symbols = symbols, mode = mode, expectedReturnType = expectedReturnType)
      OperatorConstants.LET -> let(args = args, symbols = symbols, mode = mode, expectedReturnType = expectedReturnType)
      OperatorConstants.DOT -> dot(args = args, symbols = symbols, mode = mode, expectedReturnType = expectedReturnType)
      OperatorConstants.IDENTITY -> identity( types = types,args = args, mode = mode, expectedReturnType = expectedReturnType)
      OperatorConstants.CONCAT -> concat(types = types, args = args, symbols = symbols, mode = mode, expectedReturnType = expectedReturnType)
      else -> throw CustomJsonException(MessageConstants.UNEXPECTED_VALUE)
    }
  } catch (exception: CustomJsonException) {
    throw CustomJsonException("{${op}: ${exception.message}}")
  }
}
