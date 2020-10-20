/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.GLOBAL_TYPE
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.utils.validateFunctionPermissions
import com.pibity.erp.entities.function.Function
import com.pibity.erp.entities.permission.FunctionInputPermission
import com.pibity.erp.entities.permission.FunctionOutputPermission
import com.pibity.erp.entities.permission.FunctionPermission
import com.pibity.erp.entities.permission.TypePermission
import com.pibity.erp.entities.permission.embeddables.FunctionInputPermissionId
import com.pibity.erp.entities.permission.embeddables.FunctionOutputPermissionId
import com.pibity.erp.entities.permission.embeddables.FunctionPermissionId
import com.pibity.erp.repositories.FunctionPermissionRepository
import com.pibity.erp.repositories.function.FunctionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FunctionPermissionService(
    val functionRepository: FunctionRepository,
    val functionPermissionRepository: FunctionPermissionRepository
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createFunctionPermission(jsonParams: JsonObject): FunctionPermission {
    val function: Function = functionRepository.findFunction(organizationName = jsonParams.get("organization").asString, name = jsonParams.get("functionName").asString)
        ?: throw CustomJsonException("{functionName: 'Function could not be determined'}")
    val jsonPermissions: JsonObject = validateFunctionPermissions(jsonParams = jsonParams.get("permissions").asJsonObject, function = function)
    val functionPermission = FunctionPermission(id = FunctionPermissionId(function = function, name = jsonParams.get("permissionName").asString))
    function.inputs.forEach {
      functionPermission.functionInputPermissions.add(FunctionInputPermission(id = FunctionInputPermissionId(functionPermission = functionPermission, functionInput = it), accessLevel = jsonPermissions.get("inputs").asJsonObject.get(it.id.name).asBoolean))
    }
    function.outputs.forEach {
      functionPermission.functionOutputPermissions.add(FunctionOutputPermission(id = FunctionOutputPermissionId(functionPermission = functionPermission, functionOutput = it), accessLevel = jsonPermissions.get("outputs").asJsonObject.get(it.id.name).asBoolean))
    }
    return try {
      functionPermissionRepository.save(functionPermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{permissionName: 'Permission could not be created'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun updateFunctionPermission(jsonParams: JsonObject): FunctionPermission {
    val functionPermission: FunctionPermission = functionPermissionRepository.findFunctionPermission(organizationName = jsonParams.get("organization").asString, functionName = jsonParams.get("functionName").asString, name = jsonParams.get("permissionName").asString)
        ?: throw CustomJsonException("{permissionName: 'Permission could not be determined'}")
    val jsonPermissions: JsonObject = validateFunctionPermissions(jsonParams = jsonParams.get("permissions").asJsonObject, function = functionPermission.id.function)
    functionPermission.functionInputPermissions.forEach { it.accessLevel = jsonPermissions.get("inputs").asJsonObject.get(it.id.functionInput.id.name).asBoolean }
    functionPermission.functionOutputPermissions.forEach { it.accessLevel = jsonPermissions.get("outputs").asJsonObject.get(it.id.functionOutput.id.name).asBoolean }
    return try {
      functionPermissionRepository.save(functionPermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{permissionName: 'Permission could not be created'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createDefaultFunctionPermission(function: Function): FunctionPermission {
    val functionPermission = FunctionPermission(id = FunctionPermissionId(function = function, name = "DEFAULT"))
    function.inputs.forEach {
      functionPermission.functionInputPermissions.add(FunctionInputPermission(id = FunctionInputPermissionId(functionPermission = functionPermission, functionInput = it), accessLevel = true))
    }
    function.outputs.forEach {
      functionPermission.functionOutputPermissions.add(FunctionOutputPermission(id = FunctionOutputPermissionId(functionPermission = functionPermission, functionOutput = it), accessLevel = true))
    }
    return try {
      functionPermissionRepository.save(functionPermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{permissionName: 'Permission could not be created'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun getFunctionPermissionDetails(jsonParams: JsonObject): FunctionPermission {
    return (functionPermissionRepository.findFunctionPermission(
        organizationName = jsonParams.get("organization").asString,
        functionName = jsonParams.get("functionName").asString,
        name = jsonParams.get("permissionName").asString
    ) ?: throw CustomJsonException("{permissionName: 'Permission could not be determined'}"))
  }

  fun superimposeFunctionPermissions(functionPermissions: Set<FunctionPermission>, function: Function): FunctionPermission {
    val functionPermission = FunctionPermission(id = FunctionPermissionId(function = function, name = "SUPERIMPOSED_PERMISSION"))
    for (input in function.inputs) {
      functionPermission.functionInputPermissions.add(FunctionInputPermission(id = FunctionInputPermissionId(functionPermission = functionPermission, functionInput = input),
          accessLevel = functionPermissions.map { fp -> fp.functionInputPermissions.single { it.id.functionInput.id.name == input.id.name }.accessLevel }.fold(initial = false) { acc, accessLevel -> acc || accessLevel }))
    }
    for (output in function.outputs) {
      functionPermission.functionOutputPermissions.add(FunctionOutputPermission(id = FunctionOutputPermissionId(functionPermission = functionPermission, functionOutput = output),
          accessLevel = functionPermissions.map { fp -> fp.functionOutputPermissions.single { it.id.functionOutput.id.name == output.id.name }.accessLevel }.fold(initial = false) { acc, accessLevel -> acc || accessLevel }))
    }
    return functionPermission
  }
}
