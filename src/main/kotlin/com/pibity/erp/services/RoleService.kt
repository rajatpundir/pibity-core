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
import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.Role
import com.pibity.erp.entities.TypePermission
import com.pibity.erp.entities.embeddables.RoleId
import com.pibity.erp.repositories.OrganizationRepository
import com.pibity.erp.repositories.RoleRepository
import com.pibity.erp.repositories.TypePermissionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RoleService(
    val organizationRepository: OrganizationRepository,
    val typePermissionRepository: TypePermissionRepository,
    val roleRepository: RoleRepository
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createRole(jsonParams: JsonObject): Role {
    val organization: Organization = organizationRepository.getById(jsonParams.get("organization").asString)
        ?: throw CustomJsonException("{organization: 'Organization could not be found'}")
    val role = Role(id = RoleId(organization = organization, name = jsonParams.get("roleName").asString))
    return try {
      roleRepository.save(role)
    } catch (exception: Exception) {
      throw CustomJsonException("{roleName: 'Role could not be created'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun updateRole(jsonParams: JsonObject): Role {
    val role: Role = roleRepository.findRole(organizationName = jsonParams.get("organization").asString, name = jsonParams.get("roleName").asString)
        ?: throw CustomJsonException("{roleName: 'Role could not be determined'}")
    val typePermission: TypePermission = typePermissionRepository.findTypePermission(
        organizationName = jsonParams.get("organization").asString,
        superTypeName = "Any",
        typeName = jsonParams.get("typeName").asString,
        name = jsonParams.get("permissionName").asString
    ) ?: throw CustomJsonException("{permissionName: 'Permission could not be determined'}")
    when (jsonParams.get("operation").asString) {
      "add" -> role.permissions.add(typePermission)
      "remove" -> role.permissions.remove(typePermission)
    }
    return try {
      roleRepository.save(role)
    } catch (exception: Exception) {
      throw CustomJsonException("{roleName: 'Unable to assign permission to role'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun getSuperimposedPermission(jsonParams: JsonObject) {

  }

}
