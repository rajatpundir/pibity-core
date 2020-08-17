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
import com.pibity.erp.commons.validateKeyPermissions
import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.Type
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
  fun createPermission(jsonParams: JsonObject) {
    println(jsonParams)
    val organization: Organization = organizationRepository.getById(jsonParams.get("organization").asString)
        ?: throw CustomJsonException("{organization: 'Organization could not be found'}")
    val type: Type = typeRepository.findType(organization = organization, superTypeName = "Any", name = jsonParams.get("typeName").asString)
        ?: throw CustomJsonException("{typeName: 'Type could not be determined'}")
    val keyPermissions: JsonObject = validateKeyPermissions(keyPermissions = jsonParams.get("keyPermissions").asJsonObject, type = type)
    println(keyPermissions)
  }

}