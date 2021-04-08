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
import com.pibity.core.entities.Group
import com.pibity.core.entities.Organization
import com.pibity.core.entities.Role
import com.pibity.core.entities.mappings.GroupRole
import com.pibity.core.entities.mappings.embeddables.GroupRoleId
import com.pibity.core.repositories.jpa.GroupJpaRepository
import com.pibity.core.repositories.query.GroupRepository
import com.pibity.core.repositories.jpa.OrganizationJpaRepository
import com.pibity.core.repositories.query.RoleRepository
import com.pibity.core.repositories.mappings.GroupRoleRepository
import org.springframework.stereotype.Service
import java.sql.Timestamp

@Service
class GroupService(
    val organizationJpaRepository: OrganizationJpaRepository,
    val groupRepository: GroupRepository,
    val roleRepository: RoleRepository,
    val groupRoleRepository: GroupRoleRepository,
    val groupJpaRepository: GroupJpaRepository
) {

  fun createGroup(jsonParams: JsonObject, defaultTimestamp: Timestamp): Group {
    val organization: Organization = organizationJpaRepository.getById(jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
      ?: throw CustomJsonException("{${OrganizationConstants.ORGANIZATION_ID}: ${MessageConstants.UNEXPECTED_VALUE}}")
    return try {
      groupJpaRepository.save(Group(organization = organization, name = jsonParams.get("groupName").asString, created = defaultTimestamp))
    } catch (exception: Exception) {
      throw CustomJsonException("{groupName: 'Group could not be created'}")
    }
  }

  fun updateGroup(jsonParams: JsonObject, defaultTimestamp: Timestamp): Group {
    val group: Group = groupRepository.findGroup(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get("groupName").asString)
      ?: throw CustomJsonException("{groupName: 'Group could not be determined'}")
    val role: Role = roleRepository.findRole(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get("roleName").asString)
      ?: throw CustomJsonException("{roleName: 'Role could not be determined'}")
    when (jsonParams.get("operation").asString) {
      "add" -> group.groupRoles.add(GroupRole(id = GroupRoleId(group = group, role = role), created = defaultTimestamp))
      "remove" -> {
        groupRoleRepository.delete(GroupRole(id = GroupRoleId(group = group, role = role), created = defaultTimestamp))
        group.groupRoles.remove(GroupRole(id = GroupRoleId(group = group, role = role), created = defaultTimestamp))
      }
      else -> throw CustomJsonException("{operation: 'Unexpected value for parameter'}")
    }
    return try {
      groupJpaRepository.save(group)
    } catch (exception: Exception) {
      throw CustomJsonException("{groupName: 'Unable to update role for group'}")
    }
  }

  fun getGroupDetails(jsonParams: JsonObject): Group {
    return (groupRepository.findGroup(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get("groupName").asString)
      ?: throw CustomJsonException("{groupName: 'Group could not be determined'}"))
  }
}
