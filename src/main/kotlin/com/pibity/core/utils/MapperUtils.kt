/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.core.commons.constants.MapperConstants
import com.pibity.core.commons.constants.MessageConstants
import com.pibity.core.commons.constants.TypeConstants
import com.pibity.core.commons.exceptions.CustomJsonException
import com.pibity.core.entities.function.FunctionInput
import com.pibity.core.entities.function.Mapper
import org.springframework.web.multipart.MultipartFile
import java.sql.Timestamp

fun validateMapperName(mapperName: String): String {
  return if (!keyIdentifierPattern.matcher(mapperName).matches())
    throw CustomJsonException("{${MapperConstants.MAPPER_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
  else mapperName
}

fun validateQueryParamsForExecution(jsonParams: JsonObject, queryParams: MutableSet<FunctionInput>, files: List<MultipartFile>): JsonObject = queryParams.fold(JsonObject()) { acc, functionInput ->
  acc.apply {
    try {
      when(functionInput.type.name) {
        TypeConstants.TEXT -> addProperty(functionInput.name, jsonParams.get(functionInput.name).asString)
        TypeConstants.NUMBER -> addProperty(functionInput.name, jsonParams.get(functionInput.name).asLong)
        TypeConstants.DECIMAL -> addProperty(functionInput.name, jsonParams.get(functionInput.name).asBigDecimal)
        TypeConstants.BOOLEAN -> addProperty(functionInput.name, jsonParams.get(functionInput.name).asBoolean)
        TypeConstants.DATE -> addProperty(functionInput.name, jsonParams.get(functionInput.name).asString)
        TypeConstants.TIMESTAMP -> addProperty(functionInput.name, jsonParams.get(functionInput.name).asString)
        TypeConstants.TIME -> addProperty(functionInput.name, jsonParams.get(functionInput.name).asString)
        TypeConstants.BLOB -> {
          val fileIndex: Int = jsonParams.get(functionInput.name).asInt
          if (fileIndex < 0 && fileIndex > (files.size - 1))
            throw CustomJsonException("{}")
          else
            addProperty(functionInput.name, fileIndex)
        }
        TypeConstants.FORMULA -> throw CustomJsonException("{}")
        else -> addProperty(functionInput.name, jsonParams.get(functionInput.name).asString)
      }
    } catch (exception: Exception) {
      throw CustomJsonException("{${MapperConstants.QUERY_PARAMS}: {${functionInput.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
    }
  }
}

fun validateMapperArg(arg: JsonObject, mapper: Mapper, defaultTimestamp: Timestamp, files: List<MultipartFile>): JsonObject = mapper.functionInput.function.inputs.fold(JsonObject()) { acc, input ->
  acc.apply {
    if (!(mapper.query && input == mapper.functionInput)) {
      when (input.type.name) {
        TypeConstants.TEXT -> addProperty(input.name, if (arg.has(input.name)) arg.get(input.name).asString else input.defaultStringValue!!)
        TypeConstants.NUMBER -> addProperty(input.name, if (arg.has(input.name)) arg.get(input.name).asLong else input.defaultLongValue!!)
        TypeConstants.DECIMAL -> addProperty(input.name, if (arg.has(input.name)) arg.get(input.name).asBigDecimal else input.defaultDecimalValue!!)
        TypeConstants.BOOLEAN -> addProperty(input.name, if (arg.has(input.name)) arg.get(input.name).asBoolean else input.defaultBooleanValue!!)
        TypeConstants.DATE -> addProperty(input.name, if (arg.has(input.name)) java.sql.Date(arg.get(input.name).asLong).time
        else if (input.defaultDateValue != null) input.defaultDateValue!!.time else java.sql.Date(defaultTimestamp.time).time)
        TypeConstants.TIMESTAMP -> addProperty(input.name, if (arg.has(input.name)) Timestamp(arg.get(input.name).asLong).time
        else if (input.defaultTimestampValue != null) input.defaultTimestampValue!!.time else defaultTimestamp.time)
        TypeConstants.TIME -> addProperty(input.name, if (arg.has(input.name)) java.sql.Time(arg.get(input.name).asLong).time
        else if (input.defaultTimeValue != null) input.defaultTimeValue!!.time else java.sql.Time(defaultTimestamp.time).time)
        TypeConstants.BLOB -> if (arg.has(input.name)) {
          val fileIndex: Int = arg.get(input.name).asInt
          if (fileIndex < 0 && fileIndex > (files.size - 1))
            throw CustomJsonException("{}")
          else
            addProperty(input.name, fileIndex)
        }
        TypeConstants.FORMULA -> throw CustomJsonException("{}")
        else -> addProperty(input.name, if (arg.has(input.name)) arg.get(input.name).asString else input.referencedVariable!!.name)
      }
    }
  }
}

fun validateMapperArgs(args: JsonArray, mapper: Mapper, defaultTimestamp: Timestamp, files: List<MultipartFile>): JsonArray = args.fold(JsonArray()) { acc, json ->
  acc.apply {
    try {
      add(validateMapperArg(arg = json.asJsonObject, mapper = mapper, defaultTimestamp = defaultTimestamp, files = files))
    } catch (exception: Exception) {
      throw CustomJsonException("{${MapperConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
    }
  }
}
