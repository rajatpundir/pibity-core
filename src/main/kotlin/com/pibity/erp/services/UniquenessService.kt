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
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.utils.typeIdentifierPattern
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.uniqueness.TypeUniqueness
import com.pibity.erp.repositories.jpa.OrganizationJpaRepository
import com.pibity.erp.repositories.jpa.TypeUniquenessJpaRepository
import com.pibity.erp.repositories.query.TypeRepository
import org.springframework.stereotype.Service
import java.sql.Timestamp

@Service
class UniquenessService(
    val organizationJpaRepository: OrganizationJpaRepository,
    val typeRepository: TypeRepository,
    val typeUniquenessJpaRepository: TypeUniquenessJpaRepository
) {

  fun createUniqueness(jsonParams: JsonObject, defaultTimestamp: Timestamp): TypeUniqueness {
    val type: Type = typeRepository.findType(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
        ?: throw CustomJsonException("{${OrganizationConstants.TYPE_NAME} : ${MessageConstants.UNEXPECTED_VALUE}}")
    val constraintName: String = jsonParams.get("constraintName").asString
    if (!typeIdentifierPattern.matcher(constraintName).matches() || jsonParams.get("keys").asJsonArray.size() == 0)
      throw CustomJsonException("{constraintName: 'Constraint name $constraintName is not a valid identifier'}")
    return try {
      typeUniquenessJpaRepository.save(TypeUniqueness(type = type, name = constraintName, created = defaultTimestamp).apply {
        keys.addAll(jsonParams.get("keys").asJsonArray.map { json ->
          type.keys.single { it.name == json.asString }.apply { isUniquenessDependency = true }
        })
      })
    } catch (exception: Exception) {
      throw CustomJsonException("{constraintName: ''Unable to create Type Uniqueness constraint}")
    }
  }
}
