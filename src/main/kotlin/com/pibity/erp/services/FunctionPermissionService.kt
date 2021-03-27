/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.FunctionConstants
import com.pibity.erp.commons.constants.MessageConstants
import com.pibity.erp.commons.constants.OrganizationConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.function.Function
import com.pibity.erp.entities.permission.FunctionInputPermission
import com.pibity.erp.entities.permission.FunctionOutputPermission
import com.pibity.erp.entities.permission.FunctionPermission
import com.pibity.erp.repositories.query.FunctionPermissionRepository
import com.pibity.erp.repositories.function.FunctionRepository
import com.pibity.erp.repositories.jpa.FunctionPermissionJpaRepository
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
    val functionPermissionsJson: JsonObject = validateFunctionPermissions(jsonParams = jsonParams.get("permissions").asJsonObject, function = function)
    return try {
      functionPermissionJpaRepository.save(FunctionPermission(function = function, name = jsonParams.get("permissionName").asString, created = defaultTimestamp).apply {
        functionInputPermissions = function.inputs.map { FunctionInputPermission(functionPermission = this, functionInput = it, accessLevel = functionPermissionsJson.get(FunctionConstants.INPUTS).asJsonObject.get(it.name).asBoolean, created = defaultTimestamp) }.toMutableSet()
        functionOutputPermissions = function.outputs.map { FunctionOutputPermission(functionPermission = this, functionOutput = it, accessLevel = functionPermissionsJson.get(FunctionConstants.OUTPUTS).asJsonObject.get(it.name).asBoolean, created = defaultTimestamp) }.toMutableSet()

      })
    } catch (exception: Exception) {
      throw CustomJsonException("{permissionName: 'Permission could not be created'}")
    }
  }

  fun updateFunctionPermission(jsonParams: JsonObject): FunctionPermission {
    val functionPermission: FunctionPermission = (functionPermissionRepository.findFunctionPermission(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, functionName = jsonParams.get("functionName").asString, name = jsonParams.get("permissionName").asString)
        ?: throw CustomJsonException("{permissionName: ${MessageConstants.UNEXPECTED_VALUE}}")).apply {
      val functionPermissionsJson: JsonObject = validateFunctionPermissions(jsonParams = jsonParams.get("permissions").asJsonObject, function = this.function)
      functionInputPermissions.forEach { it.accessLevel = functionPermissionsJson.get(FunctionConstants.INPUTS).asJsonObject.get(it.functionInput.name).asBoolean }
      functionOutputPermissions.forEach { it.accessLevel = functionPermissionsJson.get(FunctionConstants.OUTPUTS).asJsonObject.get(it.functionOutput.name).asBoolean }
    }
    return try {
      functionPermissionJpaRepository.save(functionPermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{permissionName: 'Permission could not be created'}")
    }
  }

  fun createDefaultFunctionPermission(function: Function, defaultTimestamp: Timestamp): FunctionPermission {
    return try {
      functionPermissionJpaRepository.save(FunctionPermission(function = function, name = "DEFAULT", created = defaultTimestamp).apply {
        functionInputPermissions = function.inputs.map { FunctionInputPermission(functionPermission = this, functionInput = it, accessLevel = true, created = defaultTimestamp) }.toMutableSet()
        functionOutputPermissions = function.outputs.map { FunctionOutputPermission(functionPermission = this, functionOutput = it, accessLevel = true, created = defaultTimestamp) }.toMutableSet()
      })
    } catch (exception: Exception) {
      throw CustomJsonException("{permissionName: 'Permission could not be created'}")
    }
  }

  fun validateFunctionPermissions(jsonParams: JsonObject, function: Function): JsonObject = JsonObject().apply {
    val inputsJson = try {
      jsonParams.get(FunctionConstants.INPUTS).asJsonObject
    } catch (exception: Exception) {
      throw CustomJsonException("{permissions: {${FunctionConstants.INPUTS}: ${MessageConstants.UNEXPECTED_VALUE}}}")
    }
    add(FunctionConstants.INPUTS, function.inputs.fold(JsonObject()) { acc, input ->
      acc.apply {
        if (!inputsJson.has(input.name))
          throw CustomJsonException("{permissions: {${FunctionConstants.INPUTS}: {${input.name}: ${MessageConstants.MISSING_FIELD}}}}")
        else try {
          addProperty(input.name, inputsJson.get(input.name).asBoolean)
        } catch (exception: Exception) {
          throw CustomJsonException("{permissions: {${FunctionConstants.INPUTS}: {${input.name}: ${MessageConstants.UNEXPECTED_VALUE}}}}")
        }
      }
    })
    val outputsJson = try {
      jsonParams.get(FunctionConstants.OUTPUTS).asJsonObject
    } catch (exception: Exception) {
      throw CustomJsonException("{permissions: {${FunctionConstants.OUTPUTS}: ${MessageConstants.UNEXPECTED_VALUE}}}")
    }
    add(FunctionConstants.OUTPUTS, function.outputs.fold(JsonObject()) { acc, output ->
      acc.apply {
        if (!outputsJson.has(output.name))
          throw CustomJsonException("{permissions: {${FunctionConstants.OUTPUTS}: {${output.name}: ${MessageConstants.MISSING_FIELD}}}}")
        else try {
          addProperty(output.name, outputsJson.get(output.name).asBoolean)
        } catch (exception: Exception) {
          throw CustomJsonException("{permissions: {${FunctionConstants.OUTPUTS}: {${output.name}: ${MessageConstants.UNEXPECTED_VALUE}}}}")
        }
      }
    })
  }

  fun getFunctionPermissionDetails(jsonParams: JsonObject): FunctionPermission {
    return (functionPermissionRepository.findFunctionPermission(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, functionName = jsonParams.get(FunctionConstants.FUNCTION_NAME).asString, name = jsonParams.get("permissionName").asString)
      ?: throw CustomJsonException("{permissionName: 'Permission could not be determined'}"))
  }

  fun superimposeFunctionPermissions(functionPermissions: Set<FunctionPermission>, function: Function, defaultTimestamp: Timestamp): FunctionPermission {
    return FunctionPermission(function = function, name = "SUPERIMPOSED_PERMISSION", created = defaultTimestamp).apply {
      functionInputPermissions = function.inputs.map { input ->
        FunctionInputPermission(functionPermission = this, functionInput = input, accessLevel = functionPermissions.map { fp -> fp.functionInputPermissions.single { it.functionInput.name == input.name }.accessLevel }.fold(initial = false) { acc, accessLevel -> acc || accessLevel }, created = defaultTimestamp)
      }.toMutableSet()
      functionOutputPermissions = function.outputs.map { output ->
        FunctionOutputPermission(functionPermission = this, functionOutput = output,
          accessLevel = functionPermissions.map { fp -> fp.functionOutputPermissions.single { it.functionOutput.name == output.name }.accessLevel }.fold(initial = false) { acc, accessLevel -> acc || accessLevel }, created = defaultTimestamp)
      }.toMutableSet()
    }
  }
}
