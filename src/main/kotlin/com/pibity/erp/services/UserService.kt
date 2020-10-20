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
import com.pibity.erp.commons.utils.createKeycloakUser
import com.pibity.erp.commons.utils.getKeycloakId
import com.pibity.erp.entities.Group
import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.Role
import com.pibity.erp.entities.User
import com.pibity.erp.entities.embeddables.UserId
import com.pibity.erp.entities.mappings.UserGroup
import com.pibity.erp.entities.mappings.UserRole
import com.pibity.erp.entities.mappings.embeddables.UserGroupId
import com.pibity.erp.entities.mappings.embeddables.UserRoleId
import com.pibity.erp.entities.permission.FunctionPermission
import com.pibity.erp.entities.permission.TypePermission
import com.pibity.erp.repositories.GroupRepository
import com.pibity.erp.repositories.OrganizationRepository
import com.pibity.erp.repositories.RoleRepository
import com.pibity.erp.repositories.UserRepository
import com.pibity.erp.repositories.mappings.UserGroupRepository
import com.pibity.erp.repositories.mappings.UserRoleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    val organizationRepository: OrganizationRepository,
    val roleRepository: RoleRepository,
    val groupRepository: GroupRepository,
    val userRepository: UserRepository,
    val typePermissionService: TypePermissionService,
    val functionPermissionService: FunctionPermissionService,
    val userRoleRepository: UserRoleRepository,
    val userGroupRepository: UserGroupRepository
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createUser(jsonParams: JsonObject): User {
    val organization: Organization = organizationRepository.getById(jsonParams.get("organization").asString)
        ?: throw CustomJsonException("{organization: 'Organization could not be found'}")
    val keycloakId: String = try {
      getKeycloakId(jsonParams.get("email").asString)
    } catch (exception: Exception) {
      createKeycloakUser(jsonParams = jsonParams)
    }
    val user = User(id = UserId(organization = organization, username = keycloakId),
        active = jsonParams.get("active").asBoolean,
        email = jsonParams.get("email").asString,
        firstName = jsonParams.get("firstName").asString,
        lastName = jsonParams.get("lastName").asString)
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
      "remove" -> {
        userGroupRepository.delete(UserGroup(id = UserGroupId(user = user, group = group)))
        user.userGroups.remove(UserGroup(id = UserGroupId(user = user, group = group)))
      }
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
        ?: throw CustomJsonException("{username: 'User could not be determined'}")
    val role: Role = roleRepository.findRole(organizationName = jsonParams.get("organization").asString, name = jsonParams.get("roleName").asString)
        ?: throw CustomJsonException("{roleName: 'Role could not be determined'}")
    when (jsonParams.get("operation").asString) {
      "add" -> user.userRoles.add(UserRole(id = UserRoleId(user = user, role = role)))
      "remove" -> {
        userRoleRepository.delete(UserRole(id = UserRoleId(user = user, role = role)))
        user.userRoles.remove(UserRole(id = UserRoleId(user = user, role = role)))
      }
      else -> throw CustomJsonException("{operation: 'Unexpected value for parameter'}")
    }
    return try {
      userRepository.save(user)
    } catch (exception: Exception) {
      throw CustomJsonException("{username: 'Unable to update role for user'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun updateUserDetails(jsonParams: JsonObject): User {
    val user: User = userRepository.findUser(organizationName = jsonParams.get("organization").asString, username = jsonParams.get("username").asString)
        ?: throw CustomJsonException("{username: 'User could not be determined'}")
    if (jsonParams.has("active?"))
      user.active = jsonParams.get("active?").asBoolean
    if (jsonParams.has("email?"))
      user.email = jsonParams.get("email?").asString
    if (jsonParams.has("fistName?"))
      user.firstName = jsonParams.get("fistName?").asString
    if (jsonParams.has("lastName?"))
      user.lastName = jsonParams.get("fistName?").asString
    return try {
      userRepository.save(user)
    } catch (exception: Exception) {
      throw CustomJsonException("{username: 'Unable to update user details'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun getUserDetails(jsonParams: JsonObject): User {
    return (userRepository.findUser(organizationName = jsonParams.get("organization").asString, username = jsonParams.get("username").asString)
        ?: throw CustomJsonException("{username: 'User could not be determined'}"))
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun getUserTypePermissions(jsonParams: JsonObject): Set<TypePermission> {
    println("------------------------")
    println(jsonParams)
    return (userRepository.getUserTypePermissions(organizationName = jsonParams.get("organization").asString, superTypeName = GLOBAL_TYPE, typeName = jsonParams.get("typeName").asString, username = jsonParams.get("username").asString))
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun getUserFunctionPermissions(jsonParams: JsonObject): Set<FunctionPermission> {
    println("------------------------")
    println(jsonParams)
    return (userRepository.getUserFunctionPermissions(organizationName = jsonParams.get("organization").asString, functionName = jsonParams.get("functionName").asString, username = jsonParams.get("username").asString))
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun superimposeUserTypePermissions(jsonParams: JsonObject): TypePermission {
    println("------------------------")
    println(jsonParams)
    val typePermissions: Set<TypePermission> = userRepository.getUserTypePermissions(organizationName = jsonParams.get("organization").asString, superTypeName = if (jsonParams.has("superTypeName")) jsonParams.get("superTypeName").asString else GLOBAL_TYPE, typeName = jsonParams.get("typeName").asString, username = jsonParams.get("username").asString)
    if (typePermissions.isNotEmpty())
      return typePermissionService.superimposeTypePermissions(typePermissions = typePermissions, type = typePermissions.first().id.type)
    else
      throw CustomJsonException("{error: 'Unauthorized Access'}")
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun superimposeUserFunctionPermissions(jsonParams: JsonObject): FunctionPermission {
    println("------------------------")
    println(jsonParams)
    val functionPermissions: Set<FunctionPermission> = userRepository.getUserFunctionPermissions(organizationName = jsonParams.get("organization").asString, functionName = jsonParams.get("functionName").asString, username = jsonParams.get("username").asString)
    if (functionPermissions.isNotEmpty())
      return functionPermissionService.superimposeFunctionPermissions(functionPermissions = functionPermissions, function = functionPermissions.first().id.function)
    else
      throw CustomJsonException("{error: 'Unauthorized Access'}")
  }
}
