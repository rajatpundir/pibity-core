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
import com.pibity.erp.entities.embeddables.RoleId
import com.pibity.erp.repositories.OrganizationRepository
import com.pibity.erp.repositories.RoleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RoleService(
    val organizationRepository: OrganizationRepository,
    val roleRepository: RoleRepository
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createRole(jsonParams: JsonObject): Role {
    val organization: Organization = organizationRepository.getById(jsonParams.get("organization").asString)
        ?: throw CustomJsonException("{organization: 'Organization could not be found'}")
    val role = Role(id = RoleId(organization = organization, name = jsonParams.get("role").asString))
    return try {
      roleRepository.save(role)
    } catch (exception: Exception) {
      throw CustomJsonException("{role: 'Role could not be created'}")
    }
  }
}
