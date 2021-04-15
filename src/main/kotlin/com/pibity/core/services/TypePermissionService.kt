/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.services

import com.google.gson.JsonObject
import com.pibity.core.commons.CustomJsonException
import com.pibity.core.commons.constants.*
import com.pibity.core.entities.Type
import com.pibity.core.entities.permission.TypePermission
import com.pibity.core.repositories.jpa.OrganizationJpaRepository
import com.pibity.core.repositories.jpa.TypePermissionJpaRepository
import com.pibity.core.repositories.query.TypePermissionRepository
import com.pibity.core.repositories.query.TypeRepository
import org.springframework.stereotype.Service
import java.sql.Timestamp

@Service
class TypePermissionService(
  val organizationJpaRepository: OrganizationJpaRepository,
  val typeRepository: TypeRepository,
  val typePermissionRepository: TypePermissionRepository,
  val typePermissionJpaRepository: TypePermissionJpaRepository
  ) {

  fun createTypePermission(jsonParams: JsonObject, defaultTimestamp: Timestamp): TypePermission {
    val type: Type = typeRepository.findType(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
      ?: throw CustomJsonException("{${OrganizationConstants.TYPE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    return try {
      typePermissionJpaRepository.save(TypePermission(type = type, name = jsonParams.get("permissionName").asString,
        permissionType = permissionTypes.single { it == jsonParams.get("permissionType").asString }, created = defaultTimestamp).apply {
          if (permissionType != VariableConstants.DELETE)
            keys = jsonParams.get("keys").asJsonArray.map { type.keys.single { key -> key.name == it.asString } }.toMutableSet()
      })
    } catch (exception: Exception) {
      throw CustomJsonException("{permissionName: 'Permission could not be saved'}")
    }
  }

  fun updateTypePermission(jsonParams: JsonObject): TypePermission {
    val typePermission: TypePermission = typePermissionRepository.findTypePermission(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong,
      typeName = jsonParams.get(OrganizationConstants.TYPE_NAME).asString, name = jsonParams.get("permissionName").asString)
      ?: throw CustomJsonException("{permissionName: ${MessageConstants.UNEXPECTED_VALUE}}")
    return try {
      typePermissionJpaRepository.save(typePermission.apply {
        if (permissionType != VariableConstants.DELETE)
          keys = jsonParams.get("keys").asJsonArray.map { type.keys.single { key -> key.name == it.asString } }.toMutableSet()
      })
    } catch (exception: Exception) {
      throw CustomJsonException("{permissionName: 'Permission could not be saved'}")
    }
  }

  fun createDefaultTypePermission(type: Type, defaultTimestamp: Timestamp): Set<TypePermission> {
    return typePermissionJpaRepository.saveAll(setOf(
      TypePermission(type = type, name = PermissionConstants.CREATE, permissionType = PermissionConstants.CREATE, keys = type.keys, created = defaultTimestamp),
      TypePermission(type = type, name = PermissionConstants.READ, permissionType = PermissionConstants.READ, keys = type.keys, created = defaultTimestamp),
      TypePermission(type = type, name = PermissionConstants.UPDATE, permissionType = PermissionConstants.UPDATE, keys = type.keys, created = defaultTimestamp),
      TypePermission(type = type, name = PermissionConstants.DELETE, permissionType = PermissionConstants.DELETE, keys = mutableSetOf(), created = defaultTimestamp))).toSet()
  }

  fun getTypePermissionDetails(jsonParams: JsonObject): TypePermission {
    return (typePermissionRepository.findTypePermission(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong,
      typeName = jsonParams.get(OrganizationConstants.TYPE_NAME).asString, name = jsonParams.get("permissionName").asString)
      ?: throw CustomJsonException("{permissionName: ${MessageConstants.UNEXPECTED_VALUE}}"))
  }
}
