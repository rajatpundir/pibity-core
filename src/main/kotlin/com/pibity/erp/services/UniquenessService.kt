/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonObject
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.utils.typeIdentifierPattern
import com.pibity.erp.entities.Key
import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.uniqueness.KeyUniqueness
import com.pibity.erp.entities.uniqueness.TypeUniqueness
import com.pibity.erp.repositories.jpa.KeyUniquenessJpaRepository
import com.pibity.erp.repositories.jpa.OrganizationJpaRepository
import com.pibity.erp.repositories.jpa.TypeUniquenessJpaRepository
import com.pibity.erp.repositories.query.TypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UniquenessService(
    val organizationJpaRepository: OrganizationJpaRepository,
    val typeRepository: TypeRepository,
    val typeUniquenessJpaRepository: TypeUniquenessJpaRepository,
    val keyUniquenessJpaRepository: KeyUniquenessJpaRepository
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createUniqueness(jsonParams: JsonObject): TypeUniqueness {
    val organization: Organization = organizationJpaRepository.getById(jsonParams.get("orgId").asLong)
        ?: throw CustomJsonException("{orgId : 'Organization can not be found'}")
    val type: Type = typeRepository.findType(organizationId = organization.id, name = jsonParams.get("typeName").asString)
        ?: throw CustomJsonException("{typeName : 'Type can not be determined'}")
    val constraintName: String = jsonParams.get("constraintName").asString
    if (!typeIdentifierPattern.matcher(constraintName).matches())
      throw CustomJsonException("{constraintName: 'Constraint name $constraintName is not a valid identifier'}")
    val typeUniqueness: TypeUniqueness = try {
      typeUniquenessJpaRepository.save(TypeUniqueness(type = type, name = constraintName))
    } catch (exception: Exception) {
      throw CustomJsonException("{constraintName: 'Unable to create Type Uniqueness constraint'}")
    }
    for (json in jsonParams.get("keys").asJsonArray) {
      val keyName: String = try {
        json.asString
      } catch (exception: Exception) {
        throw CustomJsonException("{keys: 'Unexpected value for parameter'}")
      }
      val key: Key = try {
        type.keys.single { it.name == keyName }
      } catch (exception: Exception) {
        throw CustomJsonException("{keys: {${keyName}: 'Unexpected value for parameter'}}")
      }
      val keyUniqueness: KeyUniqueness = try {
        keyUniquenessJpaRepository.save(KeyUniqueness(typeUniqueness = typeUniqueness, key = key))
      } catch (exception: Exception) {
        throw CustomJsonException("{constraintName: 'Unable to create Type Uniqueness constraint'}")
      }
      typeUniqueness.keyUniquenessConstraints.add(keyUniqueness)
    }
    return typeUniqueness
  }
}
