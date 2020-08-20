/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.validateKeyPermissions
import com.pibity.erp.entities.KeyPermission
import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.TypePermission
import com.pibity.erp.entities.embeddables.KeyPermissionId
import com.pibity.erp.entities.embeddables.TypePermissionId
import com.pibity.erp.repositories.OrganizationRepository
import com.pibity.erp.repositories.TypePermissionRepository
import com.pibity.erp.repositories.TypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PermissionService(
    val organizationRepository: OrganizationRepository,
    val typeRepository: TypeRepository,
    val typePermissionRepository: TypePermissionRepository
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createPermission(jsonParams: JsonObject, permissionOrganization: Organization? = null, permissionType: Type? = null, permissionName: String? = null): TypePermission {
    val organization: Organization = permissionOrganization
        ?: organizationRepository.getById(jsonParams.get("organization").asString)
        ?: throw CustomJsonException("{organization: 'Organization could not be found'}")
    val type: Type = permissionType
        ?: typeRepository.findType(organization = organization, superTypeName = "Any", name = jsonParams.get("typeName").asString)
        ?: throw CustomJsonException("{typeName: 'Type could not be determined'}")
    val keyPermissions: JsonObject = validateKeyPermissions(jsonParams = jsonParams, type = type).get("permissions").asJsonObject
    val typePermission = TypePermission(id = TypePermissionId(type = type, name = permissionName
        ?: jsonParams.get("permissionName").asString))
    for (key in type.keys) {
      when (key.type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> {
          typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = keyPermissions.get(key.id.name).asInt))
        }
        TypeConstants.FORMULA -> {
        }
        TypeConstants.LIST -> {
          if (key.list!!.type.id.superTypeName == "Any") {
            typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = keyPermissions.get(key.id.name).asInt))
          } else {
            if ((key.id.parentType.id.superTypeName == "Any" && key.id.parentType.id.name == key.list!!.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != "Any" && key.id.parentType.id.superTypeName == key.list!!.type.id.superTypeName)) {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), referencedTypePermission = createPermission(jsonParams = keyPermissions.get(key.id.name).asJsonObject, permissionOrganization = organization, permissionType = key.list!!.type, permissionName = typePermission.id.name)))
            } else {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = keyPermissions.get(key.id.name).asInt))
            }
          }
        }
        else -> {
          if (key.type.id.superTypeName == "Any") {
            typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = keyPermissions.get(key.id.name).asInt))
          } else {
            if ((key.id.parentType.id.superTypeName == "Any" && key.id.parentType.id.name == key.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != "Any" && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), referencedTypePermission = createPermission(jsonParams = keyPermissions.get(key.id.name).asJsonObject, permissionOrganization = organization, permissionType = key.type, permissionName = typePermission.id.name)))
            } else {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = keyPermissions.get(key.id.name).asInt))
            }
          }
        }
      }
    }
    typePermission.highestAccessLevel = typePermission.keyPermissions.map { if (it.referencedTypePermission == null) it.accessLevel else it.referencedTypePermission.highestAccessLevel }.max()
        ?: 0
    typePermission.lowestAccessLevel = typePermission.keyPermissions.map { if (it.referencedTypePermission == null) it.accessLevel else it.referencedTypePermission.lowestAccessLevel }.min()
        ?: 0
    return try {
      typePermissionRepository.save(typePermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{permissionName: 'Permission could not be created'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun updatePermission(jsonParams: JsonObject, keyTypePermission: TypePermission? = null): TypePermission {
    val typePermission: TypePermission = keyTypePermission ?: typePermissionRepository.findTypePermission(
        organizationName = jsonParams.get("organization").asString,
        superTypeName = "Any",
        typeName = jsonParams.get("typeName").asString,
        name = jsonParams.get("permissionName").asString
    ) ?: throw CustomJsonException("{permissionName: 'Permission could not be determined'}")
    val keyPermissions: JsonObject = validateKeyPermissions(jsonParams = jsonParams, type = typePermission.id.type).get("permissions").asJsonObject
    val updatedPermissions = mutableSetOf<KeyPermission>()
    for (keyPermission in typePermission.keyPermissions) {
      when (keyPermission.id.key.type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> {
          updatedPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = keyPermission.id.key), accessLevel = keyPermissions.get(keyPermission.id.key.id.name).asInt))
        }
        TypeConstants.FORMULA -> {
        }
        TypeConstants.LIST -> {
          if (keyPermission.id.key.list!!.type.id.superTypeName == "Any") {
            updatedPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = keyPermission.id.key), accessLevel = keyPermissions.get(keyPermission.id.key.id.name).asInt))
          } else {
            if ((keyPermission.id.key.id.parentType.id.superTypeName == "Any" && keyPermission.id.key.id.parentType.id.name == keyPermission.id.key.list!!.type.id.superTypeName)
                || (keyPermission.id.key.id.parentType.id.superTypeName != "Any" && keyPermission.id.key.id.parentType.id.superTypeName == keyPermission.id.key.list!!.type.id.superTypeName)) {
              updatedPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = keyPermission.id.key), referencedTypePermission = updatePermission(jsonParams = keyPermissions.get(keyPermission.id.key.id.name).asJsonObject, keyTypePermission = keyPermission.referencedTypePermission!!)))
            } else {
              updatedPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = keyPermission.id.key), accessLevel = keyPermissions.get(keyPermission.id.key.id.name).asInt))
            }
          }
        }
        else -> {
          if (keyPermission.id.key.type.id.superTypeName == "Any") {
            updatedPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = keyPermission.id.key), accessLevel = keyPermissions.get(keyPermission.id.key.id.name).asInt))
          } else {
            if ((keyPermission.id.key.id.parentType.id.superTypeName == "Any" && keyPermission.id.key.id.parentType.id.name == keyPermission.id.key.type.id.superTypeName)
                || (keyPermission.id.key.id.parentType.id.superTypeName != "Any" && keyPermission.id.key.id.parentType.id.superTypeName == keyPermission.id.key.type.id.superTypeName)) {
              updatedPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = keyPermission.id.key), referencedTypePermission = updatePermission(jsonParams = keyPermissions.get(keyPermission.id.key.id.name).asJsonObject, keyTypePermission = keyPermission.referencedTypePermission!!)))
            } else {
              updatedPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = keyPermission.id.key), accessLevel = keyPermissions.get(keyPermission.id.key.id.name).asInt))
            }
          }
        }
      }
    }
    typePermission.keyPermissions = updatedPermissions
    typePermission.highestAccessLevel = typePermission.keyPermissions.map { if (it.referencedTypePermission == null) it.accessLevel else it.referencedTypePermission.highestAccessLevel }.max()
        ?: 0
    typePermission.lowestAccessLevel = typePermission.keyPermissions.map { if (it.referencedTypePermission == null) it.accessLevel else it.referencedTypePermission.lowestAccessLevel }.min()
        ?: 0
    return try {
      typePermissionRepository.save(typePermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{typeName: 'Permission could not be updated'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createDefaultPermission(type: Type, permissionName: String, accessLevel: Int): TypePermission {
    val typePermission = TypePermission(id = TypePermissionId(type = type, name = permissionName))
    for (key in type.keys) {
      when (key.type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = accessLevel))
        TypeConstants.FORMULA -> {
        }
        TypeConstants.LIST -> {
          if (key.list!!.type.id.superTypeName == "Any") {
            typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = accessLevel))
          } else {
            if ((key.id.parentType.id.superTypeName == "Any" && key.id.parentType.id.name == key.list!!.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != "Any" && key.id.parentType.id.superTypeName == key.list!!.type.id.superTypeName)) {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), referencedTypePermission = createDefaultPermission(key.list!!.type, permissionName, accessLevel)))
            } else {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = accessLevel))
            }
          }
        }
        else -> {
          if (key.type.id.superTypeName == "Any") {
            typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = accessLevel))
          } else {
            if ((key.id.parentType.id.superTypeName == "Any" && key.id.parentType.id.name == key.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != "Any" && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), referencedTypePermission = createDefaultPermission(key.type, permissionName, accessLevel)))
            } else {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = accessLevel))
            }
          }
        }
      }
    }
    return try {
      typePermissionRepository.save(typePermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{typeName: 'Permission $permissionName could not be created'}")
    }
  }

  fun getPermissionDetails(jsonParams: JsonObject): TypePermission {
    return (typePermissionRepository.findTypePermission(
        organizationName = jsonParams.get("organization").asString,
        superTypeName = "Any",
        typeName = jsonParams.get("typeName").asString,
        name = jsonParams.get("permissionName").asString
    ) ?: throw CustomJsonException("{permissionName: 'Permission could not be determined'}"))
  }
}
