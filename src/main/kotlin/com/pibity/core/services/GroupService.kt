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
import com.pibity.core.commons.constants.GroupConstants
import com.pibity.core.commons.constants.SpaceConstants
import com.pibity.core.entities.Group
import com.pibity.core.entities.Organization
import com.pibity.core.entities.Subspace
import com.pibity.core.entities.mappings.GroupSubspace
import com.pibity.core.entities.mappings.embeddables.GroupSubspaceId
import com.pibity.core.repositories.jpa.GroupJpaRepository
import com.pibity.core.repositories.query.GroupRepository
import com.pibity.core.repositories.jpa.OrganizationJpaRepository
import com.pibity.core.repositories.query.SubspaceRepository
import com.pibity.core.repositories.mappings.GroupSubspaceRepository
import org.springframework.stereotype.Service
import java.sql.Timestamp

@Service
class GroupService(
  val organizationJpaRepository: OrganizationJpaRepository,
  val groupRepository: GroupRepository,
  val subspaceRepository: SubspaceRepository,
  val groupSubspaceRepository: GroupSubspaceRepository,
  val groupJpaRepository: GroupJpaRepository
) {

  fun createGroup(jsonParams: JsonObject, defaultTimestamp: Timestamp): Group {
    val organization: Organization = organizationJpaRepository.getById(jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
      ?: throw CustomJsonException("{${OrganizationConstants.ORGANIZATION_ID}: ${MessageConstants.UNEXPECTED_VALUE}}")
    return try {
      groupJpaRepository.save(Group(organization = organization, name = jsonParams.get(GroupConstants.GROUP_NAME).asString, created = defaultTimestamp))
    } catch (exception: Exception) {
      throw CustomJsonException("{${GroupConstants.GROUP_NAME}: 'Group could not be created'}")
    }
  }

  fun updateGroup(jsonParams: JsonObject, defaultTimestamp: Timestamp): Group {
    val group: Group = groupRepository.findGroup(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get(GroupConstants.GROUP_NAME).asString)
      ?: throw CustomJsonException("{${GroupConstants.GROUP_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    val subspace: Subspace = subspaceRepository.findSubspace(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong,
      spaceName = jsonParams.get(SpaceConstants.SPACE_NAME).asString, name = jsonParams.get(SpaceConstants.SUBSPACE_NAME).asString)
      ?: throw CustomJsonException("{${SpaceConstants.SUBSPACE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    when (jsonParams.get(GroupConstants.OPERATION).asString) {
      GroupConstants.ADD -> group.groupSubspaces.add(GroupSubspace(id = GroupSubspaceId(group = group, subspace = subspace), created = defaultTimestamp))
      GroupConstants.REMOVE -> {
        groupSubspaceRepository.delete(GroupSubspace(id = GroupSubspaceId(group = group, subspace = subspace), created = defaultTimestamp))
        group.groupSubspaces.remove(GroupSubspace(id = GroupSubspaceId(group = group, subspace = subspace), created = defaultTimestamp))
      }
      else -> throw CustomJsonException("{${GroupConstants.OPERATION}: '${MessageConstants.UNEXPECTED_VALUE}}")
    }
    return try {
      groupJpaRepository.save(group)
    } catch (exception: Exception) {
      throw CustomJsonException("{${GroupConstants.GROUP_NAME}: 'Unable to update subspace for group'}")
    }
  }

  fun getGroupDetails(jsonParams: JsonObject): Group {
    return (groupRepository.findGroup(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get(GroupConstants.GROUP_NAME).asString)
      ?: throw CustomJsonException("{${GroupConstants.GROUP_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}"))
  }
}
