/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.services

import com.google.gson.JsonObject
import com.pibity.core.commons.constants.MessageConstants
import com.pibity.core.commons.constants.OrganizationConstants
import com.pibity.core.commons.CustomJsonException
import com.pibity.core.entities.Space
import com.pibity.core.entities.Subspace
import com.pibity.core.repositories.jpa.SubspaceJpaRepository
import com.pibity.core.repositories.query.SpaceRepository
import com.pibity.core.repositories.query.SubspaceRepository
import org.springframework.stereotype.Service
import java.sql.Timestamp

@Service
class SubspaceService(
  val spaceRepository: SpaceRepository,
  val subspaceJpaRepository: SubspaceJpaRepository,
  val subspaceRepository: SubspaceRepository
) {

  fun createSubspace(jsonParams: JsonObject, defaultTimestamp: Timestamp): Subspace {
    val space: Space = spaceRepository.findSpace(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get("spaceName").asString)
      ?: throw CustomJsonException("{${OrganizationConstants.ORGANIZATION_ID}: ${MessageConstants.UNEXPECTED_VALUE}}")
    return try {
      subspaceJpaRepository.save(Subspace(space = space, name = jsonParams.get("subspaceName").asString, created = defaultTimestamp))
    } catch (exception: Exception) {
      throw CustomJsonException("{subspaceName: 'Subspace could not be created'}")
    }
  }

  fun getSubspaceDetails(jsonParams: JsonObject): Subspace {
    return (subspaceRepository.findSubspace(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, spaceName = jsonParams.get("spaceName").asString, name = jsonParams.get("subspaceName").asString)
        ?: throw CustomJsonException("{subspaceName: ${MessageConstants.UNEXPECTED_VALUE}}"))
  }
}
