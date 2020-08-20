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
import com.pibity.erp.entities.*
import com.pibity.erp.entities.embeddables.UserId
import com.pibity.erp.repositories.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    val organizationRepository: OrganizationRepository,
    val roleRepository: RoleRepository,
    val groupRepository: GroupRepository,
    val userRepository: UserRepository
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createUser(jsonParams: JsonObject): User {
    val organization: Organization = organizationRepository.getById(jsonParams.get("organization").asString)
        ?: throw CustomJsonException("{organization: 'Organization could not be found'}")
    val user = User(id = UserId(organization = organization, username = jsonParams.get("username").asString))
    return try {
      userRepository.save(user)
    } catch (exception: Exception) {
      throw CustomJsonException("{username: 'User could not be created'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun updateUserGroups(jsonParams: JsonObject): User {
    val user: User = userRepository.findUser(organizationName = jsonParams.get("organization").asString, username = jsonParams.get("username").asString)
        ?: throw CustomJsonException("{username: 'User could not be determined'}")
    val group: Group = groupRepository.findGroup(organizationName = jsonParams.get("organization").asString, name = jsonParams.get("groupName").asString)
        ?: throw CustomJsonException("{groupName: 'Group could not be determined'}")
    when (jsonParams.get("operation").asString) {
      "add" -> user.groups.add(group)
      "remove" -> user.groups.remove(group)
      else -> throw CustomJsonException("{operation: 'Unexpected value for parameter'}")
    }
    return try {
      userRepository.save(user)
    } catch (exception: Exception) {
      throw CustomJsonException("{username: 'Unable to update group for user'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun updateUserRoles(jsonParams: JsonObject): User {
    val user: User = userRepository.findUser(organizationName = jsonParams.get("organization").asString, username = jsonParams.get("username").asString)
        ?: throw CustomJsonException("{userName: 'user could not be determined'}")
    val role: Role = roleRepository.findRole(organizationName = jsonParams.get("organization").asString, name = jsonParams.get("roleName").asString)
        ?: throw CustomJsonException("{roleName: 'Role could not be determined'}")
    when (jsonParams.get("operation").asString) {
      "add" -> user.roles.add(role)
      "remove" -> user.roles.remove(role)
      else -> throw CustomJsonException("{operation: 'Unexpected value for parameter'}")
    }
    return try {
      userRepository.save(user)
    } catch (exception: Exception) {
      throw CustomJsonException("{username: 'Unable to update role for user'}")
    }
  }
}
