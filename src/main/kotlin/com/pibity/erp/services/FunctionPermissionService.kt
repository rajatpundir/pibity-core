/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonObject
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.utils.validateFunctionPermissions
import com.pibity.erp.entities.function.Function
import com.pibity.erp.entities.permission.FunctionInputPermission
import com.pibity.erp.entities.permission.FunctionOutputPermission
import com.pibity.erp.entities.permission.FunctionPermission
import com.pibity.erp.repositories.query.FunctionPermissionRepository
import com.pibity.erp.repositories.function.FunctionRepository
import com.pibity.erp.repositories.jpa.FunctionPermissionJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FunctionPermissionService(
    val functionRepository: FunctionRepository,
    val functionPermissionRepository: FunctionPermissionRepository,
    val functionPermissionJpaRepository: FunctionPermissionJpaRepository
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createFunctionPermission(jsonParams: JsonObject): FunctionPermission {
    val function: Function = functionRepository.findFunction(organizationId = jsonParams.get("orgId").asLong, name = jsonParams.get("functionName").asString)
        ?: throw CustomJsonException("{functionName: 'Function could not be determined'}")
    val jsonPermissions: JsonObject = validateFunctionPermissions(jsonParams = jsonParams.get("permissions").asJsonObject, function = function)
    val functionPermission = FunctionPermission(function = function, name = jsonParams.get("permissionName").asString)
    function.inputs.forEach {
      functionPermission.functionInputPermissions.add(FunctionInputPermission(functionPermission = functionPermission, functionInput = it, accessLevel = jsonPermissions.get("inputs").asJsonObject.get(it.name).asBoolean))
    }
    function.outputs.forEach {
      functionPermission.functionOutputPermissions.add(FunctionOutputPermission(functionPermission = functionPermission, functionOutput = it, accessLevel = jsonPermissions.get("outputs").asJsonObject.get(it.name).asBoolean))
    }
    return try {
      functionPermissionJpaRepository.save(functionPermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{permissionName: 'Permission could not be created'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun updateFunctionPermission(jsonParams: JsonObject): FunctionPermission {
    val functionPermission: FunctionPermission = functionPermissionRepository.findFunctionPermission(organizationId = jsonParams.get("orgId").asLong, functionName = jsonParams.get("functionName").asString, name = jsonParams.get("permissionName").asString)
        ?: throw CustomJsonException("{permissionName: 'Permission could not be determined'}")
    val jsonPermissions: JsonObject = validateFunctionPermissions(jsonParams = jsonParams.get("permissions").asJsonObject, function = functionPermission.function)
    functionPermission.functionInputPermissions.forEach { it.accessLevel = jsonPermissions.get("inputs").asJsonObject.get(it.functionInput.name).asBoolean }
    functionPermission.functionOutputPermissions.forEach { it.accessLevel = jsonPermissions.get("outputs").asJsonObject.get(it.functionOutput.name).asBoolean }
    return try {
      functionPermissionJpaRepository.save(functionPermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{permissionName: 'Permission could not be created'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createDefaultFunctionPermission(function: Function): FunctionPermission {
    val functionPermission = FunctionPermission(function = function, name = "DEFAULT")
    function.inputs.forEach {
      functionPermission.functionInputPermissions.add(FunctionInputPermission(functionPermission = functionPermission, functionInput = it, accessLevel = true))
    }
    function.outputs.forEach {
      functionPermission.functionOutputPermissions.add(FunctionOutputPermission(functionPermission = functionPermission, functionOutput = it, accessLevel = true))
    }
    return try {
      functionPermissionJpaRepository.save(functionPermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{permissionName: 'Permission could not be created'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun getFunctionPermissionDetails(jsonParams: JsonObject): FunctionPermission {
    return (functionPermissionRepository.findFunctionPermission(
        organizationId = jsonParams.get("orgId").asLong,
        functionName = jsonParams.get("functionName").asString,
        name = jsonParams.get("permissionName").asString
    ) ?: throw CustomJsonException("{permissionName: 'Permission could not be determined'}"))
  }

  fun superimposeFunctionPermissions(functionPermissions: Set<FunctionPermission>, function: Function): FunctionPermission {
    val functionPermission = FunctionPermission(function = function, name = "SUPERIMPOSED_PERMISSION")
    for (input in function.inputs) {
      functionPermission.functionInputPermissions.add(FunctionInputPermission(functionPermission = functionPermission, functionInput = input,
          accessLevel = functionPermissions.map { fp -> fp.functionInputPermissions.single { it.functionInput.name == input.name }.accessLevel }.fold(initial = false) { acc, accessLevel -> acc || accessLevel }))
    }
    for (output in function.outputs) {
      functionPermission.functionOutputPermissions.add(FunctionOutputPermission(functionPermission = functionPermission, functionOutput = output,
          accessLevel = functionPermissions.map { fp -> fp.functionOutputPermissions.single { it.functionOutput.name == output.name }.accessLevel }.fold(initial = false) { acc, accessLevel -> acc || accessLevel }))
    }
    return functionPermission
  }
}
