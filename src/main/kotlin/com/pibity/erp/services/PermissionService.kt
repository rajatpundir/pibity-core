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
import com.pibity.erp.commons.constants.PermissionConstants
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
        ?: typeRepository.findType(organizationName = jsonParams.get("organization").asString, superTypeName = GLOBAL_TYPE, name = jsonParams.get("typeName").asString)
        ?: throw CustomJsonException("{typeName: 'Type could not be determined'}")
    val keyPermissions: JsonObject = validateKeyPermissions(jsonParams = jsonParams.get("permissions").asJsonObject, type = type)
    val typePermission = TypePermission(id = TypePermissionId(type = type, name = permissionName
        ?: jsonParams.get("permissionName").asString),
        creatable = if (jsonParams.has("creatable")) jsonParams.get("creatable").asBoolean else false,
        deletable = if (jsonParams.has("deletable")) jsonParams.get("deletable").asBoolean else false)
    for (key in type.keys) {
      when (key.type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> {
          typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = keyPermissions.get(key.id.name).asInt))
        }
        TypeConstants.FORMULA -> {
        }
        TypeConstants.LIST -> {
          if (key.list!!.type.id.superTypeName == GLOBAL_TYPE) {
            typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = keyPermissions.get(key.id.name).asInt))
          } else {
            if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.list!!.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.list!!.type.id.superTypeName)) {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), referencedTypePermission = createPermission(jsonParams = keyPermissions.get(key.id.name).asJsonObject, permissionOrganization = organization, permissionType = key.list!!.type, permissionName = typePermission.id.name)))
            } else {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = keyPermissions.get(key.id.name).asInt))
            }
          }
        }
        else -> {
          if (key.type.id.superTypeName == GLOBAL_TYPE) {
            typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = keyPermissions.get(key.id.name).asInt))
          } else {
            if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), referencedTypePermission = createPermission(jsonParams = keyPermissions.get(key.id.name).asJsonObject, permissionOrganization = organization, permissionType = key.type, permissionName = typePermission.id.name)))
            } else {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = keyPermissions.get(key.id.name).asInt))
            }
          }
        }
      }
    }
    if (typePermission.id.type.id.superTypeName == GLOBAL_TYPE && typePermission.keyPermissions.size == 0) {
      typePermission.minAccessLevel = PermissionConstants.READ_ACCESS
      typePermission.maxAccessLevel = PermissionConstants.READ_ACCESS
    } else {
      typePermission.maxAccessLevel = typePermission.keyPermissions.map {
        it.referencedTypePermission?.maxAccessLevel ?: it.accessLevel
      }.max() ?: PermissionConstants.NO_ACCESS
      typePermission.minAccessLevel = typePermission.keyPermissions.map {
        it.referencedTypePermission?.minAccessLevel ?: it.accessLevel
      }.min() ?: PermissionConstants.NO_ACCESS
    }
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
        superTypeName = GLOBAL_TYPE,
        typeName = jsonParams.get("typeName").asString,
        name = jsonParams.get("permissionName").asString
    ) ?: throw CustomJsonException("{permissionName: 'Permission could not be determined'}")
    typePermission.creatable = if (jsonParams.has("creatable")) jsonParams.get("creatable").asBoolean else false
    typePermission.deletable = if (jsonParams.has("deletable")) jsonParams.get("deletable").asBoolean else false
    val keyPermissions: JsonObject = validateKeyPermissions(jsonParams = jsonParams.get("permissions").asJsonObject, type = typePermission.id.type)
    val updatedPermissions = mutableSetOf<KeyPermission>()
    for (keyPermission in typePermission.keyPermissions) {
      when (keyPermission.id.key.type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> {
          updatedPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = keyPermission.id.key), accessLevel = keyPermissions.get(keyPermission.id.key.id.name).asInt))
        }
        TypeConstants.FORMULA -> {
        }
        TypeConstants.LIST -> {
          if (keyPermission.id.key.list!!.type.id.superTypeName == GLOBAL_TYPE) {
            updatedPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = keyPermission.id.key), accessLevel = keyPermissions.get(keyPermission.id.key.id.name).asInt))
          } else {
            if ((keyPermission.id.key.id.parentType.id.superTypeName == GLOBAL_TYPE && keyPermission.id.key.id.parentType.id.name == keyPermission.id.key.list!!.type.id.superTypeName)
                || (keyPermission.id.key.id.parentType.id.superTypeName != GLOBAL_TYPE && keyPermission.id.key.id.parentType.id.superTypeName == keyPermission.id.key.list!!.type.id.superTypeName)) {
              updatedPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = keyPermission.id.key), referencedTypePermission = updatePermission(jsonParams = keyPermissions.get(keyPermission.id.key.id.name).asJsonObject, keyTypePermission = keyPermission.referencedTypePermission!!)))
            } else {
              updatedPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = keyPermission.id.key), accessLevel = keyPermissions.get(keyPermission.id.key.id.name).asInt))
            }
          }
        }
        else -> {
          if (keyPermission.id.key.type.id.superTypeName == GLOBAL_TYPE) {
            updatedPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = keyPermission.id.key), accessLevel = keyPermissions.get(keyPermission.id.key.id.name).asInt))
          } else {
            if ((keyPermission.id.key.id.parentType.id.superTypeName == GLOBAL_TYPE && keyPermission.id.key.id.parentType.id.name == keyPermission.id.key.type.id.superTypeName)
                || (keyPermission.id.key.id.parentType.id.superTypeName != GLOBAL_TYPE && keyPermission.id.key.id.parentType.id.superTypeName == keyPermission.id.key.type.id.superTypeName)) {
              updatedPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = keyPermission.id.key), referencedTypePermission = updatePermission(jsonParams = keyPermissions.get(keyPermission.id.key.id.name).asJsonObject, keyTypePermission = keyPermission.referencedTypePermission!!)))
            } else {
              updatedPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = keyPermission.id.key), accessLevel = keyPermissions.get(keyPermission.id.key.id.name).asInt))
            }
          }
        }
      }
    }
    typePermission.keyPermissions = updatedPermissions
    if (typePermission.id.type.id.superTypeName == GLOBAL_TYPE && typePermission.keyPermissions.size == 0) {
      typePermission.minAccessLevel = PermissionConstants.READ_ACCESS
      typePermission.maxAccessLevel = PermissionConstants.READ_ACCESS
    } else {
      typePermission.maxAccessLevel = typePermission.keyPermissions.map {
        it.referencedTypePermission?.maxAccessLevel ?: it.accessLevel
      }.max() ?: PermissionConstants.NO_ACCESS
      typePermission.minAccessLevel = typePermission.keyPermissions.map {
        it.referencedTypePermission?.minAccessLevel ?: it.accessLevel
      }.min() ?: PermissionConstants.NO_ACCESS
    }
    return try {
      typePermissionRepository.save(typePermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{typeName: 'Permission could not be updated'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createDefaultPermission(type: Type, permissionName: String, accessLevel: Int): TypePermission {
    val typePermission = TypePermission(id = TypePermissionId(type = type, name = permissionName), creatable = type.id.superTypeName== GLOBAL_TYPE && accessLevel==PermissionConstants.WRITE_ACCESS, deletable = type.id.superTypeName== GLOBAL_TYPE && accessLevel==PermissionConstants.WRITE_ACCESS)
    for (key in type.keys) {
      when (key.type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = accessLevel))
        TypeConstants.FORMULA -> {
        }
        TypeConstants.LIST -> {
          if (key.list!!.type.id.superTypeName == GLOBAL_TYPE) {
            typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = accessLevel))
          } else {
            if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.list!!.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.list!!.type.id.superTypeName)) {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), referencedTypePermission = createDefaultPermission(key.list!!.type, permissionName, accessLevel)))
            } else {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = accessLevel))
            }
          }
        }
        else -> {
          if (key.type.id.superTypeName == GLOBAL_TYPE) {
            typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = accessLevel))
          } else {
            if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), referencedTypePermission = createDefaultPermission(key.type, permissionName, accessLevel)))
            } else {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = accessLevel))
            }
          }
        }
      }
    }
    if (typePermission.id.type.id.superTypeName == GLOBAL_TYPE && typePermission.keyPermissions.size == 0) {
      typePermission.minAccessLevel = PermissionConstants.READ_ACCESS
      typePermission.maxAccessLevel = PermissionConstants.READ_ACCESS
    } else {
      typePermission.maxAccessLevel = typePermission.keyPermissions.map {
        it.referencedTypePermission?.maxAccessLevel ?: it.accessLevel
      }.max() ?: PermissionConstants.NO_ACCESS
      typePermission.minAccessLevel = typePermission.keyPermissions.map {
        it.referencedTypePermission?.minAccessLevel ?: it.accessLevel
      }.min() ?: PermissionConstants.NO_ACCESS
    }
    return typePermission
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun getPermissionDetails(jsonParams: JsonObject): TypePermission {
    return (typePermissionRepository.findTypePermission(
        organizationName = jsonParams.get("organization").asString,
        superTypeName = GLOBAL_TYPE,
        typeName = jsonParams.get("typeName").asString,
        name = jsonParams.get("permissionName").asString
    ) ?: throw CustomJsonException("{permissionName: 'Permission could not be determined'}"))
  }

  fun superimposePermissions(typePermissions: Set<TypePermission>, type: Type): TypePermission {
    val typePermission = TypePermission(id = TypePermissionId(type = type, name = "SUPERIMPOSED_PERMISSION"),
        creatable = typePermissions.fold(false) { acc, it -> acc || it.creatable },
        deletable = typePermissions.fold(false) { acc, it -> acc || it.deletable })
    for (key in type.keys) {
      when (key.type.id.name) {
        TypeConstants.TEXT, TypeConstants.DECIMAL, TypeConstants.NUMBER, TypeConstants.BOOLEAN -> {
          typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = typePermissions.map { tp -> tp.keyPermissions.single { it.id.key == key }.accessLevel }.max()!!))
        }
        TypeConstants.FORMULA -> {
        }
        TypeConstants.LIST -> {
          if (key.list!!.type.id.superTypeName == GLOBAL_TYPE) {
            typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = typePermissions.map { tp -> tp.keyPermissions.single { it.id.key == key }.accessLevel }.max()!!))
          } else {
            if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.list!!.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.list!!.type.id.superTypeName)) {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), referencedTypePermission = superimposePermissions(typePermissions.map { tp -> tp.keyPermissions.single { it.id.key == key }.referencedTypePermission!! }.toSet(), key.type)))
            } else {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = typePermissions.map { tp -> tp.keyPermissions.single { it.id.key == key }.accessLevel }.max()!!))
            }
          }
        }
        else -> {
          if (key.type.id.superTypeName == GLOBAL_TYPE) {
            typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = typePermissions.map { tp -> tp.keyPermissions.single { it.id.key == key }.accessLevel }.max()!!))
          } else {
            if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), referencedTypePermission = superimposePermissions(typePermissions.map { tp -> tp.keyPermissions.single { it.id.key == key }.referencedTypePermission!! }.toSet(), key.type)))
            } else {
              typePermission.keyPermissions.add(KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key), accessLevel = typePermissions.map { tp -> tp.keyPermissions.single { it.id.key == key }.accessLevel }.max()!!))
            }
          }
        }
      }
    }
    if (typePermission.id.type.id.superTypeName == GLOBAL_TYPE && typePermission.keyPermissions.size == 0) {
      typePermission.minAccessLevel = PermissionConstants.READ_ACCESS
      typePermission.maxAccessLevel = PermissionConstants.READ_ACCESS
    } else {
      typePermission.maxAccessLevel = typePermission.keyPermissions.map {
        it.referencedTypePermission?.maxAccessLevel ?: it.accessLevel
      }.max() ?: PermissionConstants.NO_ACCESS
      typePermission.minAccessLevel = typePermission.keyPermissions.map {
        it.referencedTypePermission?.minAccessLevel ?: it.accessLevel
      }.min() ?: PermissionConstants.NO_ACCESS
    }
    return typePermission
  }
}
