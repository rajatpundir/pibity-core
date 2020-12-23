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
import com.pibity.erp.entities.Group
import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.Role
import com.pibity.erp.entities.mappings.GroupRole
import com.pibity.erp.entities.mappings.embeddables.GroupRoleId
import com.pibity.erp.repositories.jpa.GroupJpaRepository
import com.pibity.erp.repositories.query.GroupRepository
import com.pibity.erp.repositories.jpa.OrganizationJpaRepository
import com.pibity.erp.repositories.query.RoleRepository
import com.pibity.erp.repositories.mappings.GroupRoleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupService(
    val organizationJpaRepository: OrganizationJpaRepository,
    val groupRepository: GroupRepository,
    val roleRepository: RoleRepository,
    val groupRoleRepository: GroupRoleRepository,
    val groupJpaRepository: GroupJpaRepository
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createGroup(jsonParams: JsonObject): Group {
    val organization: Organization = organizationJpaRepository.getById(jsonParams.get("orgId").asLong)
        ?: throw CustomJsonException("{orgId: 'Organization could not be found'}")
    val group = Group(organization = organization, name = jsonParams.get("groupName").asString)
    return try {
      groupJpaRepository.save(group)
    } catch (exception: Exception) {
      throw CustomJsonException("{groupName: 'Group could not be created'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun updateGroup(jsonParams: JsonObject): Group {
    val group: Group = groupRepository.findGroup(organizationId = jsonParams.get("orgId").asLong, name = jsonParams.get("groupName").asString)
        ?: throw CustomJsonException("{groupName: 'Group could not be determined'}")
    val role: Role = roleRepository.findRole(organizationId = jsonParams.get("orgId").asLong, name = jsonParams.get("roleName").asString)
        ?: throw CustomJsonException("{roleName: 'Role could not be determined'}")
    when (jsonParams.get("operation").asString) {
      "add" -> group.groupRoles.add(GroupRole(id = GroupRoleId(group = group, role = role)))
      "remove" -> {
        groupRoleRepository.delete(GroupRole(id = GroupRoleId(group = group, role = role)))
        group.groupRoles.remove(GroupRole(id = GroupRoleId(group = group, role = role)))
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
    return (groupRepository.findGroup(organizationId = jsonParams.get("orgId").asLong, name = jsonParams.get("groupName").asString)
        ?: throw CustomJsonException("{groupName: 'Group could not be determined'}"))
  }
}
