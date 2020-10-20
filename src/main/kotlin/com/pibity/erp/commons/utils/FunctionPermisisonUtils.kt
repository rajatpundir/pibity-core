/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons.utils

import com.google.gson.JsonObject
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.function.Function

fun validateFunctionPermissions(jsonParams: JsonObject, function: Function): JsonObject {
  val expectedFunctionPermissions = JsonObject()
  val inputsJson = try {
    jsonParams.get("inputs").asJsonObject
  } catch (exception: Exception) {
    throw CustomJsonException("{permissions: {inputs: 'Unexpected value for parameter'}}")
  }
  val expectedInputPermissions = JsonObject()
  for (input in function.inputs) {
    if (!inputsJson.has(input.id.name))
      throw CustomJsonException("{permissions: {inputs: {${input.id.name}: 'Field is missing in request body'}}}")
    else try {
      expectedInputPermissions.addProperty(input.id.name, inputsJson.get(input.id.name).asBoolean)
    } catch (exception: Exception) {
      throw CustomJsonException("{permissions: {inputs: {${input.id.name}: 'Unexpected value for parameter'}}}")
    }
  }
  expectedFunctionPermissions.add("inputs", expectedInputPermissions)
  val outputsJson = try {
    jsonParams.get("outputs").asJsonObject
  } catch (exception: Exception) {
    throw CustomJsonException("{permissions: {outputs: 'Unexpected value for parameter'}}")
  }
  val expectedOutputPermissions = JsonObject()
  for (output in function.outputs) {
    if (!outputsJson.has(output.id.name))
      throw CustomJsonException("{permissions: {outputs: {${output.id.name}: 'Field is missing in request body'}}}")
    else try {
      expectedOutputPermissions.addProperty(output.id.name, outputsJson.get(output.id.name).asBoolean)
    } catch (exception: Exception) {
      throw CustomJsonException("{permissions: {outputs: {${output.id.name}: 'Unexpected value for parameter'}}}")
    }
  }
  expectedFunctionPermissions.add("outputs", expectedOutputPermissions)
  return expectedFunctionPermissions
}
