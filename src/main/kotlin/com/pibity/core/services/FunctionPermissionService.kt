/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.services

import com.google.gson.JsonObject
import com.pibity.core.commons.constants.FunctionConstants
import com.pibity.core.commons.constants.MessageConstants
import com.pibity.core.commons.constants.OrganizationConstants
import com.pibity.core.commons.CustomJsonException
import com.pibity.core.commons.constants.PermissionConstants
import com.pibity.core.entities.function.Function
import com.pibity.core.entities.permission.FunctionPermission
import com.pibity.core.repositories.query.FunctionPermissionRepository
import com.pibity.core.repositories.function.FunctionRepository
import com.pibity.core.repositories.jpa.FunctionPermissionJpaRepository
import org.springframework.stereotype.Service
import java.sql.Timestamp

@Service
class FunctionPermissionService(
    val functionRepository: FunctionRepository,
    val functionPermissionRepository: FunctionPermissionRepository,
    val functionPermissionJpaRepository: FunctionPermissionJpaRepository
) {

  fun createFunctionPermission(jsonParams: JsonObject, defaultTimestamp: Timestamp): FunctionPermission {
    val function: Function = functionRepository.findFunction(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get(FunctionConstants.FUNCTION_NAME).asString)
        ?: throw CustomJsonException("{${FunctionConstants.FUNCTION_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    return try {
      functionPermissionJpaRepository.save(FunctionPermission(function = function, name = jsonParams.get(PermissionConstants.PERMISSION_NAME).asString,
        inputs = jsonParams.get(FunctionConstants.INPUTS).asJsonArray.map { function.inputs.single { key -> key.name == it.asString } }.toMutableSet(),
        outputs = jsonParams.get(FunctionConstants.OUTPUTS).asJsonArray.map { function.outputs.single { key -> key.name == it.asString } }.toMutableSet(),
        created = defaultTimestamp))
    } catch (exception: Exception) {
      throw CustomJsonException("{${PermissionConstants.PERMISSION_NAME}: 'Permission could not be saved'}")
    }
  }

  fun updateFunctionPermission(jsonParams: JsonObject): FunctionPermission {
    val functionPermission: FunctionPermission = (functionPermissionRepository.findFunctionPermission(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, functionName = jsonParams.get(FunctionConstants.FUNCTION_NAME).asString, name = jsonParams.get(PermissionConstants.PERMISSION_NAME).asString)
        ?: throw CustomJsonException("{${PermissionConstants.PERMISSION_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}"))
    return try {
      functionPermissionJpaRepository.save(functionPermission.apply {
        inputs = jsonParams.get(FunctionConstants.INPUTS).asJsonArray.map { function.inputs.single { key -> key.name == it.asString } }.toMutableSet()
        outputs = jsonParams.get(FunctionConstants.OUTPUTS).asJsonArray.map { function.outputs.single { key -> key.name == it.asString } }.toMutableSet()
      })
    } catch (exception: Exception) {
      throw CustomJsonException("{${PermissionConstants.PERMISSION_NAME}: 'Permission could not be saved'}")
    }
  }

  fun createDefaultFunctionPermission(function: Function, defaultTimestamp: Timestamp): FunctionPermission {
    return try {
      functionPermissionJpaRepository.save(FunctionPermission(function = function, name = "DEFAULT", inputs = function.inputs, outputs = function.outputs, created = defaultTimestamp))
    } catch (exception: Exception) {
      throw CustomJsonException("{${PermissionConstants.PERMISSION_NAME}: 'Permission could not be saved'}")
    }
  }

  fun getFunctionPermissionDetails(jsonParams: JsonObject): FunctionPermission {
    return (functionPermissionRepository.findFunctionPermission(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, functionName = jsonParams.get(FunctionConstants.FUNCTION_NAME).asString, name = jsonParams.get(PermissionConstants.PERMISSION_NAME).asString)
      ?: throw CustomJsonException("{${PermissionConstants.PERMISSION_NAME}: 'Permission could not be determined'}"))
  }
}
