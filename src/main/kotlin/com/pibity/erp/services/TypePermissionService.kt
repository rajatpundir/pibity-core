/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.MessageConstants
import com.pibity.erp.commons.constants.OrganizationConstants
import com.pibity.erp.commons.constants.PermissionConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.permission.KeyPermission
import com.pibity.erp.entities.permission.TypePermission
import com.pibity.erp.repositories.jpa.OrganizationJpaRepository
import com.pibity.erp.repositories.jpa.TypePermissionJpaRepository
import com.pibity.erp.repositories.query.TypePermissionRepository
import com.pibity.erp.repositories.query.TypeRepository
import org.springframework.stereotype.Service

@Service
class TypePermissionService(
  val organizationJpaRepository: OrganizationJpaRepository,
  val typeRepository: TypeRepository,
  val typePermissionRepository: TypePermissionRepository,
  val typePermissionJpaRepository: TypePermissionJpaRepository
  ) {

  fun createTypePermission(jsonParams: JsonObject): Pair<TypePermission, Int> {
    val type: Type = typeRepository.findType(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
      ?: throw CustomJsonException("{${OrganizationConstants.TYPE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    val typePermission = typePermissionJpaRepository.save(TypePermission(type = type, name = jsonParams.get("permissionName").asString).apply {
      if (jsonParams.has("creatable"))
        creatable = jsonParams.get("creatable").asBoolean
      if (jsonParams.has("deletable"))
        deletable = jsonParams.get("deletable").asBoolean
      val keyPermissionsJson: JsonObject = validateKeyPermissions(jsonParams = jsonParams.get("permissions").asJsonObject, type = type)
      keyPermissions.addAll(type.keys.map { KeyPermission(typePermission = this, key = it, accessLevel = keyPermissionsJson.get(it.name).asInt) })
    })
    return try {
      typePermissionJpaRepository.save(typePermission)
      Pair(typePermission, typePermission.keyPermissions.map { it.accessLevel }.maxOrNull() ?: 0)
    } catch (exception: Exception) {
      throw CustomJsonException("{permissionName: 'Permission could not be saved'}")
    }
  }

  fun updateTypePermission(jsonParams: JsonObject): Pair<TypePermission, Int> {
    val typePermission: TypePermission = (typePermissionRepository.findTypePermission(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, typeName = jsonParams.get(OrganizationConstants.TYPE_NAME).asString, name = jsonParams.get("permissionName").asString)
      ?: throw CustomJsonException("{permissionName: ${MessageConstants.UNEXPECTED_VALUE}}")).apply {
      if (jsonParams.has("creatable"))
        creatable = jsonParams.get("creatable").asBoolean
      if (jsonParams.has("deletable"))
        deletable = jsonParams.get("deletable").asBoolean
      val keyPermissionsJson: JsonObject = validateKeyPermissions(jsonParams = jsonParams.get("permissions").asJsonObject, type = this.type)
      keyPermissions.forEach { keyPermission ->
        if (keyPermissionsJson.has(keyPermission.key.name))
          keyPermission.accessLevel = keyPermissionsJson.get(keyPermission.key.name).asInt
      }
    }
    return try {
      typePermissionJpaRepository.save(typePermission)
      Pair(typePermission, typePermission.keyPermissions.map { it.accessLevel }.maxOrNull() ?: 0)
    } catch (exception: Exception) {
      throw CustomJsonException("{${OrganizationConstants.TYPE_NAME}: 'Permission could not be updated'}")
    }
  }

  fun createDefaultTypePermission(type: Type, permissionName: String, accessLevel: Int): TypePermission {
    return typePermissionJpaRepository.save(TypePermission(type = type, name = permissionName,
      creatable = accessLevel == PermissionConstants.WRITE_ACCESS,
      deletable = accessLevel == PermissionConstants.WRITE_ACCESS).apply {
      keyPermissions.addAll(type.keys.map { key -> KeyPermission(typePermission = this, key = key, accessLevel = accessLevel) })
    })
  }

  fun validateKeyPermissions(jsonParams: JsonObject, type: Type): JsonObject {
    return type.keys.fold(JsonObject()) { acc, key ->
      acc.apply {
        if (jsonParams.has(key.name)) {
          val accessLevel: Int = try {
            jsonParams.get(key.name).asInt
          } catch (exception: Exception) {
            throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
          }
          when (key.type.name) {
            TypeConstants.FORMULA -> if (accessLevel < PermissionConstants.NO_ACCESS || accessLevel > PermissionConstants.READ_ACCESS)
              throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
            else -> if (accessLevel < PermissionConstants.NO_ACCESS || accessLevel > PermissionConstants.WRITE_ACCESS)
              throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
          }
          addProperty(key.name, accessLevel)
        } else throw CustomJsonException("{${key.name}: ${MessageConstants.MISSING_FIELD}}")
      }
    }
  }

  fun getTypePermissionDetails(jsonParams: JsonObject): TypePermission {
    return (typePermissionRepository.findTypePermission(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, typeName = jsonParams.get(OrganizationConstants.TYPE_NAME).asString, name = jsonParams.get("permissionName").asString)
      ?: throw CustomJsonException("{permissionName: ${MessageConstants.UNEXPECTED_VALUE}}"))
  }

  fun superimposeTypePermissions(typePermissions: Set<TypePermission>, type: Type): TypePermission {
    return TypePermission(type = type, name = "SUPERIMPOSED_PERMISSION",
      creatable = typePermissions.fold(false) { acc, it -> acc || it.creatable },
      deletable = typePermissions.fold(false) { acc, it -> acc || it.deletable }).apply {
        keyPermissions.addAll(type.keys.map { key ->
          KeyPermission(typePermission = this, key = key, accessLevel = typePermissions.map { tp -> tp.keyPermissions.single { it.key == key }.accessLevel }.maxOrNull()!!)
        })
    }
  }
}
