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
    val typePermission = TypePermission(id = TypePermissionId(type = type, name = permissionName ?: jsonParams.get("permissionName").asString))
    for (key in type.keys) {
      when (key.type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> {
          val keyPermission = KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key))
          keyPermission.accessLevel = keyPermissions.get(key.id.name).asInt
          typePermission.keyPermissions.add(keyPermission)
        }
        TypeConstants.FORMULA -> {
        }
        TypeConstants.LIST -> {
          if (key.list!!.type.id.superTypeName == "Any") {
            val keyPermission = KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key))
            keyPermission.accessLevel = keyPermissions.get(key.id.name).asInt
            typePermission.keyPermissions.add(keyPermission)
          } else {
            if ((key.id.parentType.id.superTypeName == "Any" && key.id.parentType.id.name == key.list!!.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != "Any" && key.id.parentType.id.superTypeName == key.list!!.type.id.superTypeName)) {
              val keyPermission = KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key))
              keyPermission.referencedTypePermission = createPermission(jsonParams = keyPermissions.get(key.id.name).asJsonObject, permissionOrganization = organization, permissionType = key.list!!.type, permissionName = typePermission.id.name)
              typePermission.keyPermissions.add(keyPermission)
            } else {
              val keyPermission = KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key))
              keyPermission.accessLevel = keyPermissions.get(key.id.name).asInt
              typePermission.keyPermissions.add(keyPermission)
            }
          }
        }
        else -> {
          if (key.type.id.superTypeName == "Any") {
            val keyPermission = KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key))
            keyPermission.accessLevel = keyPermissions.get(key.id.name).asInt
            typePermission.keyPermissions.add(keyPermission)
          } else {
            if ((key.id.parentType.id.superTypeName == "Any" && key.id.parentType.id.name == key.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != "Any" && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
              val keyPermission = KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key))
              keyPermission.referencedTypePermission = createPermission(jsonParams = keyPermissions.get(key.id.name).asJsonObject, permissionOrganization = organization, permissionType = key.type, permissionName = typePermission.id.name)
              typePermission.keyPermissions.add(keyPermission)
            } else {
              val keyPermission = KeyPermission(id = KeyPermissionId(typePermission = typePermission, key = key))
              keyPermission.accessLevel = keyPermissions.get(key.id.name).asInt
              typePermission.keyPermissions.add(keyPermission)
            }
          }
        }
      }
    }
    return typePermission
  }

}
