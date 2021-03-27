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
import com.pibity.core.commons.exceptions.CustomJsonException
import com.pibity.core.entities.Organization
import com.pibity.core.entities.Role
import com.pibity.core.entities.mappings.RoleFunctionPermission
import com.pibity.core.entities.mappings.RoleTypePermission
import com.pibity.core.entities.mappings.embeddables.RoleFunctionPermissionId
import com.pibity.core.entities.mappings.embeddables.RoleTypePermissionId
import com.pibity.core.entities.permission.FunctionPermission
import com.pibity.core.entities.permission.TypePermission
import com.pibity.core.repositories.jpa.OrganizationJpaRepository
import com.pibity.core.repositories.jpa.RoleJpaRepository
import com.pibity.core.repositories.mappings.RoleFunctionPermissionRepository
import com.pibity.core.repositories.mappings.RoleTypePermissionRepository
import com.pibity.core.repositories.query.FunctionPermissionRepository
import com.pibity.core.repositories.query.RoleRepository
import com.pibity.core.repositories.query.TypePermissionRepository
import org.springframework.stereotype.Service
import java.sql.Timestamp

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

  fun createRole(jsonParams: JsonObject, defaultTimestamp: Timestamp): Role {
    val organization: Organization = organizationJpaRepository.getById(jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
      ?: throw CustomJsonException("{${OrganizationConstants.ORGANIZATION_ID}: ${MessageConstants.UNEXPECTED_VALUE}}")
    return try {
      roleJpaRepository.save(Role(organization = organization, name = jsonParams.get("roleName").asString, created = defaultTimestamp))
    } catch (exception: Exception) {
      throw CustomJsonException("{roleName: 'Role could not be created'}")
    }
  }

  fun updateRoleTypePermissions(jsonParams: JsonObject, defaultTimestamp: Timestamp): Role {
    val role: Role = roleRepository.findRole(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get("roleName").asString)
      ?: throw CustomJsonException("{roleName: ${MessageConstants.UNEXPECTED_VALUE}}")
    val typePermission: TypePermission = typePermissionRepository.findTypePermission(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, typeName = jsonParams.get(OrganizationConstants.TYPE_NAME).asString, name = jsonParams.get("permissionName").asString)
      ?: throw CustomJsonException("{permissionName: ${MessageConstants.UNEXPECTED_VALUE}}")
    when (jsonParams.get("operation").asString) {
      "add" -> role.roleTypePermissions.add(RoleTypePermission(id = RoleTypePermissionId(role = role, permission = typePermission), created = defaultTimestamp))
      "remove" -> {
        roleTypePermissionRepository.delete(RoleTypePermission(id = RoleTypePermissionId(role = role, permission = typePermission), created = defaultTimestamp))
        role.roleTypePermissions.remove(RoleTypePermission(id = RoleTypePermissionId(role = role, permission = typePermission), created = defaultTimestamp))
      }
      else -> throw CustomJsonException("{operation: ${MessageConstants.UNEXPECTED_VALUE}}")
    }
    return try {
      roleJpaRepository.save(role)
    } catch (exception: Exception) {
      throw CustomJsonException("{roleName: 'Unable to update permission for role'}")
    }
  }

  fun updateRoleFunctionPermissions(jsonParams: JsonObject, defaultTimestamp: Timestamp): Role {
    val role: Role = roleRepository.findRole(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get("roleName").asString)
        ?: throw CustomJsonException("{roleName: ${MessageConstants.UNEXPECTED_VALUE}}")
    val functionPermission: FunctionPermission = functionPermissionRepository.findFunctionPermission(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, functionName = jsonParams.get(FunctionConstants.FUNCTION_NAME).asString, name = jsonParams.get("permissionName").asString)
      ?: throw CustomJsonException("{permissionName: ${MessageConstants.UNEXPECTED_VALUE}}")
    when (jsonParams.get("operation").asString) {
      "add" -> role.roleFunctionPermissions.add(RoleFunctionPermission(id = RoleFunctionPermissionId(role = role, permission = functionPermission), created = defaultTimestamp))
      "remove" -> {
        roleFunctionPermissionRepository.delete(RoleFunctionPermission(id = RoleFunctionPermissionId(role = role, permission = functionPermission), created = defaultTimestamp))
        role.roleFunctionPermissions.remove(RoleFunctionPermission(id = RoleFunctionPermissionId(role = role, permission = functionPermission), created = defaultTimestamp))
      }
      else -> throw CustomJsonException("{operation: ${MessageConstants.UNEXPECTED_VALUE}}")
    }
    return try {
      roleJpaRepository.save(role)
    } catch (exception: Exception) {
      throw CustomJsonException("{roleName: 'Unable to update permission for role'}")
    }
  }

  fun getRoleDetails(jsonParams: JsonObject): Role {
    return (roleRepository.findRole(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get("roleName").asString)
        ?: throw CustomJsonException("{roleName: ${MessageConstants.UNEXPECTED_VALUE}}"))
  }
}
