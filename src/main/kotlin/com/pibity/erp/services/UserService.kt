/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.GLOBAL_TYPE
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.*
import com.pibity.erp.entities.embeddables.UserId
import com.pibity.erp.entities.mappings.UserGroup
import com.pibity.erp.entities.mappings.UserRole
import com.pibity.erp.entities.mappings.embeddables.UserGroupId
import com.pibity.erp.entities.mappings.embeddables.UserRoleId
import com.pibity.erp.repositories.GroupRepository
import com.pibity.erp.repositories.OrganizationRepository
import com.pibity.erp.repositories.RoleRepository
import com.pibity.erp.repositories.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    val organizationRepository: OrganizationRepository,
    val roleRepository: RoleRepository,
    val groupRepository: GroupRepository,
    val userRepository: UserRepository,
    val permissionService: PermissionService
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
      "add" -> user.userGroups.add(UserGroup(id = UserGroupId(user = user, group = group)))
      "remove" -> user.userGroups.remove(UserGroup(id = UserGroupId(user = user, group = group)))
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
        ?: throw CustomJsonException("{username: 'user could not be determined'}")
    val role: Role = roleRepository.findRole(organizationName = jsonParams.get("organization").asString, name = jsonParams.get("roleName").asString)
        ?: throw CustomJsonException("{roleName: 'Role could not be determined'}")
    when (jsonParams.get("operation").asString) {
      "add" -> user.userRoles.add(UserRole(id = UserRoleId(user = user, role = role)))
      "remove" -> user.userRoles.remove(UserRole(id = UserRoleId(user = user, role = role)))
      else -> throw CustomJsonException("{operation: 'Unexpected value for parameter'}")
    }
    return try {
      userRepository.save(user)
    } catch (exception: Exception) {
      throw CustomJsonException("{username: 'Unable to update role for user'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun getUserDetails(jsonParams: JsonObject): User {
    return (userRepository.findUser(organizationName = jsonParams.get("organization").asString, username = jsonParams.get("username").asString)
        ?: throw CustomJsonException("{username: 'User could not be determined'}"))
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun getUserPermissions(jsonParams: JsonObject): Set<TypePermission> {
    return (userRepository.getUserPermissions(organizationName = jsonParams.get("organization").asString, superTypeName = GLOBAL_TYPE, typeName = jsonParams.get("typeName").asString, username = jsonParams.get("username").asString))
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun superimposeUserPermissions(jsonParams: JsonObject): TypePermission {
    val typePermissions = userRepository.getUserPermissions(organizationName = jsonParams.get("organization").asString, superTypeName = if(jsonParams.has("superTypeName") )jsonParams.get("superTypeName").asString else GLOBAL_TYPE, typeName = jsonParams.get("typeName").asString, username = jsonParams.get("username").asString)
    if (typePermissions.isNotEmpty())
      return permissionService.superimposePermissions(typePermissions = typePermissions, type = typePermissions.first().id.type)
    else
      throw CustomJsonException("[]")
  }
}
