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
import java.util.*

@Suppress("UNCHECKED_CAST")
fun concat(types: MutableList<String>, args: List<JsonElement>, symbols: JsonObject, mode: String, expectedReturnType: String): Any {
  val acceptableTypes = listOf(TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.DATE, TypeConstants.TIMESTAMP, TypeConstants.TIME)
  val expectedReturnTypes = listOf(TypeConstants.TEXT, TypeConstants.BLOB)
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
            collectedSymbols.addAll(validateOrEvaluateExpression(expression = arg.asJsonObject, mode = mode, symbols = symbols, expectedReturnType = type) as Set<String>)
          else {
            try {
              arg.asString
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
      addProperty(LispConstants.OPERATION, OperatorConstants.CONCAT)
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
      val evaluatedExpression: String = args.zip(types).fold("") { acc, (arg, type) ->
        if(arg.isJsonObject)
          acc + validateOrEvaluateExpression(expression = arg.asJsonObject, symbols = symbols, mode = mode, expectedReturnType = TypeConstants.TEXT) as String
        else
          acc + arg.asString
      }
      when(expectedReturnType) {
        TypeConstants.TEXT -> evaluatedExpression
        else -> Base64.getDecoder().decode(evaluatedExpression)
      }
    }
  }
}
