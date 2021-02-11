/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.PermissionConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.utils.validateKeyPermissions
import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.permission.KeyPermission
import com.pibity.erp.entities.permission.TypePermission
import com.pibity.erp.repositories.jpa.OrganizationJpaRepository
import com.pibity.erp.repositories.jpa.TypePermissionJpaRepository
import com.pibity.erp.repositories.query.TypePermissionRepository
import com.pibity.erp.repositories.query.TypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.min

@Service
class TypePermissionService(
    val organizationJpaRepository: OrganizationJpaRepository,
    val typeRepository: TypeRepository,
    val typePermissionRepository: TypePermissionRepository,
    val typePermissionJpaRepository: TypePermissionJpaRepository
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createTypePermission(jsonParams: JsonObject, permissionOrganization: Organization? = null, permissionType: Type? = null, permissionName: String? = null): Pair<TypePermission, Int> {
    val organization: Organization = permissionOrganization
        ?: organizationJpaRepository.getById(jsonParams.get("orgId").asLong)
        ?: throw CustomJsonException("{orgId: 'Organization could not be found'}")
    val type: Type = permissionType
        ?: typeRepository.findType(organizationId = jsonParams.get("orgId").asLong, name = jsonParams.get("typeName").asString)
        ?: throw CustomJsonException("{typeName: 'Type could not be determined'}")
    val keyPermissions: JsonObject = validateKeyPermissions(jsonParams = jsonParams.get("permissions").asJsonObject, type = type)
    val typePermission = TypePermission(type = type, name = permissionName ?: jsonParams.get("permissionName").asString,
        creatable = if (jsonParams.has("creatable")) jsonParams.get("creatable").asBoolean else false,
        deletable = if (jsonParams.has("deletable")) jsonParams.get("deletable").asBoolean else false)
    for (key in type.keys) {
      when (key.type.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN ->
          typePermission.keyPermissions.add(KeyPermission(typePermission = typePermission, key = key, accessLevel = keyPermissions.get(key.name).asInt))
        TypeConstants.FORMULA ->
          typePermission.keyPermissions.add(KeyPermission(typePermission = typePermission, key = key, accessLevel = min(keyPermissions.get(key.name).asInt, PermissionConstants.READ_ACCESS)))
             else -> {
               typePermission.keyPermissions.add(KeyPermission(typePermission = typePermission, key = key, accessLevel = keyPermissions.get(key.name).asInt))
               }
      }
    }
    return try {
      Pair(typePermissionJpaRepository.save(typePermission), typePermission.keyPermissions.map { it.accessLevel }.maxOrNull()
          ?: 0)
    } catch (exception: Exception) {
      throw CustomJsonException("{permissionName: 'Permission could not be created'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun updateTypePermission(jsonParams: JsonObject, keyTypePermission: TypePermission? = null): Pair<TypePermission, Int> {
    val typePermission: TypePermission = keyTypePermission ?: typePermissionRepository.findTypePermission(
        organizationId = jsonParams.get("orgId").asLong,
        typeName = jsonParams.get("typeName").asString,
        name = jsonParams.get("permissionName").asString
    ) ?: throw CustomJsonException("{permissionName: 'Permission could not be determined'}")
    typePermission.creatable = if (jsonParams.has("creatable")) jsonParams.get("creatable").asBoolean else false
    typePermission.deletable = if (jsonParams.has("deletable")) jsonParams.get("deletable").asBoolean else false
    val keyPermissions: JsonObject = validateKeyPermissions(jsonParams = jsonParams.get("permissions").asJsonObject, type = typePermission.type)
    for (keyPermission in typePermission.keyPermissions) {
      when (keyPermission.key.type.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN ->
          keyPermission.accessLevel = keyPermissions.get(keyPermission.key.name).asInt
        TypeConstants.FORMULA ->
          keyPermission.accessLevel = min(keyPermissions.get(keyPermission.key.name).asInt, PermissionConstants.READ_ACCESS)
             else -> {
               keyPermission.accessLevel = keyPermissions.get(keyPermission.key.name).asInt
        }
      }
    }
    return try {
      Pair(typePermissionJpaRepository.save(typePermission), typePermission.keyPermissions.map { it.accessLevel }.maxOrNull()
          ?: 0)
    } catch (exception: Exception) {
      throw CustomJsonException("{typeName: 'Permission could not be updated'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createDefaultTypePermission(type: Type, permissionName: String, accessLevel: Int): TypePermission {
    val typePermission = TypePermission(type = type, name = permissionName, creatable = accessLevel == PermissionConstants.WRITE_ACCESS, deletable = accessLevel == PermissionConstants.WRITE_ACCESS)
    for (key in type.keys) {
      when (key.type.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN ->
          typePermission.keyPermissions.add(KeyPermission(typePermission = typePermission, key = key, accessLevel = accessLevel))
        TypeConstants.FORMULA ->
          typePermission.keyPermissions.add(KeyPermission(typePermission = typePermission, key = key, accessLevel = min(accessLevel, PermissionConstants.READ_ACCESS)))
            else -> {
              typePermission.keyPermissions.add(KeyPermission(typePermission = typePermission, key = key, accessLevel = accessLevel))
        }
      }
    }
    return typePermissionJpaRepository.save(typePermission)
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun getTypePermissionDetails(jsonParams: JsonObject): TypePermission {
    return (typePermissionRepository.findTypePermission(
        organizationId = jsonParams.get("orgId").asLong,
        typeName = jsonParams.get("typeName").asString,
        name = jsonParams.get("permissionName").asString
    ) ?: throw CustomJsonException("{permissionName: 'Permission could not be determined'}"))
  }

  fun superimposeTypePermissions(typePermissions: Set<TypePermission>, type: Type): TypePermission {
    val typePermission = TypePermission(type = type, name = "SUPERIMPOSED_PERMISSION",
        creatable = typePermissions.fold(false) { acc, it -> acc || it.creatable },
        deletable = typePermissions.fold(false) { acc, it -> acc || it.deletable })
    for (key in type.keys) {
      when (key.type.name) {
        TypeConstants.TEXT, TypeConstants.DECIMAL, TypeConstants.NUMBER, TypeConstants.BOOLEAN, TypeConstants.FORMULA ->
          typePermission.keyPermissions.add(KeyPermission(typePermission = typePermission, key = key, accessLevel = typePermissions.map { tp -> tp.keyPermissions.single { it.key == key }.accessLevel }.maxOrNull()!!))
              else -> {
                typePermission.keyPermissions.add(KeyPermission(typePermission = typePermission, key = key, accessLevel = typePermissions.map { tp -> tp.keyPermissions.single { it.key == key }.accessLevel }.maxOrNull()!!))
              }
      }
    }
    return typePermission
  }
}
