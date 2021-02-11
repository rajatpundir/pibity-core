/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonObject
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.Role
import com.pibity.erp.entities.mappings.RoleFunctionPermission
import com.pibity.erp.entities.mappings.RoleTypePermission
import com.pibity.erp.entities.mappings.embeddables.RoleFunctionPermissionId
import com.pibity.erp.entities.mappings.embeddables.RoleTypePermissionId
import com.pibity.erp.entities.permission.FunctionPermission
import com.pibity.erp.entities.permission.TypePermission
import com.pibity.erp.repositories.jpa.OrganizationJpaRepository
import com.pibity.erp.repositories.jpa.RoleJpaRepository
import com.pibity.erp.repositories.mappings.RoleFunctionPermissionRepository
import com.pibity.erp.repositories.mappings.RoleTypePermissionRepository
import com.pibity.erp.repositories.query.FunctionPermissionRepository
import com.pibity.erp.repositories.query.RoleRepository
import com.pibity.erp.repositories.query.TypePermissionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RoleService(
    val organizationJpaRepository: OrganizationJpaRepository,
    val typePermissionRepository: TypePermissionRepository,
    val roleRepository: RoleRepository,
    val roleJpaRepository: RoleJpaRepository,
    val roleTypePermissionRepository: RoleTypePermissionRepository,
    val functionPermissionRepository: FunctionPermissionRepository,
    val roleFunctionPermissionRepository: RoleFunctionPermissionRepository
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createRole(jsonParams: JsonObject): Role {
    val organization: Organization = organizationJpaRepository.getById(jsonParams.get("orgId").asLong)
        ?: throw CustomJsonException("{orgId: 'Organization could not be found'}")
    val role = Role(organization = organization, name = jsonParams.get("roleName").asString)
    return try {
      roleJpaRepository.save(role)
    } catch (exception: Exception) {
      throw CustomJsonException("{roleName: 'Role could not be created'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun updateRoleTypePermissions(jsonParams: JsonObject): Role {
    val role: Role = roleRepository.findRole(organizationId = jsonParams.get("orgId").asLong, name = jsonParams.get("roleName").asString)
        ?: throw CustomJsonException("{roleName: 'Role could not be determined'}")
    val typePermission: TypePermission = typePermissionRepository.findTypePermission(
        organizationId = jsonParams.get("orgId").asLong,
        typeName = jsonParams.get("typeName").asString,
        name = jsonParams.get("permissionName").asString
    ) ?: throw CustomJsonException("{permissionName: 'Permission could not be determined'}")
    when (jsonParams.get("operation").asString) {
      "add" -> role.roleTypePermissions.add(RoleTypePermission(id = RoleTypePermissionId(role = role, permission = typePermission)))
      "remove" -> {
        roleTypePermissionRepository.delete(RoleTypePermission(id = RoleTypePermissionId(role = role, permission = typePermission)))
        role.roleTypePermissions.remove(RoleTypePermission(id = RoleTypePermissionId(role = role, permission = typePermission)))
      }
      else -> throw CustomJsonException("{operation: 'Unexpected value for parameter'}")
    }
    return try {
      roleJpaRepository.save(role)
    } catch (exception: Exception) {
      throw CustomJsonException("{roleName: 'Unable to update permission for role'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun updateRoleFunctionPermissions(jsonParams: JsonObject): Role {
    val role: Role = roleRepository.findRole(organizationId = jsonParams.get("orgId").asLong, name = jsonParams.get("roleName").asString)
        ?: throw CustomJsonException("{roleName: 'Role could not be determined'}")
    val functionPermission: FunctionPermission = functionPermissionRepository.findFunctionPermission(organizationId = jsonParams.get("orgId").asLong,
        functionName = jsonParams.get("functionName").asString,
        name = jsonParams.get("permissionName").asString)
        ?: throw CustomJsonException("{permissionName: 'Permission could not be determined'}")
    when (jsonParams.get("operation").asString) {
      "add" -> role.roleFunctionPermissions.add(RoleFunctionPermission(id = RoleFunctionPermissionId(role = role, permission = functionPermission)))
      "remove" -> {
        roleFunctionPermissionRepository.delete(RoleFunctionPermission(id = RoleFunctionPermissionId(role = role, permission = functionPermission)))
        role.roleFunctionPermissions.remove(RoleFunctionPermission(id = RoleFunctionPermissionId(role = role, permission = functionPermission)))
      }
      else -> throw CustomJsonException("{operation: 'Unexpected value for parameter'}")
    }
    return try {
      roleJpaRepository.save(role)
    } catch (exception: Exception) {
      throw CustomJsonException("{roleName: 'Unable to update permission for role'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun getRoleDetails(jsonParams: JsonObject): Role {
    return (roleRepository.findRole(organizationId = jsonParams.get("orgId").asLong, name = jsonParams.get("roleName").asString)
        ?: throw CustomJsonException("{roleName: 'Role could not be determined'}"))
  }
}
