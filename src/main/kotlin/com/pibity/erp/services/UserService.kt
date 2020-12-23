/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.GLOBAL_TYPE
import com.pibity.erp.commons.constants.RoleConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.utils.createKeycloakUser
import com.pibity.erp.commons.utils.getKeycloakId
import com.pibity.erp.commons.utils.gson
import com.pibity.erp.commons.utils.joinKeycloakGroups
import com.pibity.erp.entities.Group
import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.Role
import com.pibity.erp.entities.User
import com.pibity.erp.entities.mappings.UserGroup
import com.pibity.erp.entities.mappings.UserRole
import com.pibity.erp.entities.mappings.embeddables.UserGroupId
import com.pibity.erp.entities.mappings.embeddables.UserRoleId
import com.pibity.erp.entities.permission.FunctionPermission
import com.pibity.erp.entities.permission.TypePermission
import com.pibity.erp.repositories.jpa.OrganizationJpaRepository
import com.pibity.erp.repositories.jpa.UserJpaRepository
import com.pibity.erp.repositories.mappings.UserGroupRepository
import com.pibity.erp.repositories.mappings.UserRoleRepository
import com.pibity.erp.repositories.query.GroupRepository
import com.pibity.erp.repositories.query.RoleRepository
import com.pibity.erp.repositories.query.UserRepository
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    val organizationJpaRepository: OrganizationJpaRepository,
    val roleRepository: RoleRepository,
    val groupRepository: GroupRepository,
    val userRepository: UserRepository,
    val userJpaRepository: UserJpaRepository,
    val typePermissionService: TypePermissionService,
    val functionPermissionService: FunctionPermissionService,
    val userRoleRepository: UserRoleRepository,
    val userGroupRepository: UserGroupRepository,
    @Lazy val variableService: VariableService
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createUser(jsonParams: JsonObject): User {
    val organization: Organization = organizationJpaRepository.getById(jsonParams.get("orgId").asLong)
        ?: throw CustomJsonException("{orgId: 'Organization could not be found'}")
    val keycloakId: String = try {
      getKeycloakId(jsonParams.get("email").asString)
    } catch (exception: Exception) {
      createKeycloakUser(jsonParams = jsonParams)
    }
    joinKeycloakGroups(jsonParams = jsonParams.apply {
      addProperty("keycloakUserId", keycloakId)
      if (!jsonParams.has("subGroups"))
        jsonParams.add("subGroups", gson.fromJson(gson.toJson(listOf(RoleConstants.USER)), JsonArray::class.java))
    })
    var user = User(organization = organization, username = keycloakId,
        active = jsonParams.get("active").asBoolean,
        email = jsonParams.get("email").asString,
        firstName = jsonParams.get("firstName").asString,
        lastName = jsonParams.get("lastName").asString)
    user = userJpaRepository.save(user)
    jsonParams.get("roles").asJsonArray.forEach {
      val roleName: String = try {
        it.asString
      } catch (exception: Exception) {
        throw CustomJsonException("{roles: 'Unexpected value for parameter'}")
      }
      val role: Role = roleRepository.findRole(organizationId = organization.id, name = roleName)
          ?: throw CustomJsonException("{roleName: 'Role could not be determined'}")
      user.userRoles.add(UserRole(id = UserRoleId(user = user, role = role)))
    }
    user = userJpaRepository.save(user)
    user.details = try {
      variableService.createVariable(jsonParams = JsonObject().apply {
        addProperty("orgId", jsonParams.get("orgId").asString)
        addProperty("username", jsonParams.get("username").asString)
        addProperty("typeName", "User")
        addProperty("variableName", keycloakId)
        add("values", jsonParams.get("details").asJsonObject)
      }).first
    } catch (exception: CustomJsonException) {
      throw CustomJsonException("{details: ${exception.message}}")
    }
    return try {
      userJpaRepository.save(user)
    } catch (exception: Exception) {
      throw CustomJsonException("{username: 'User could not be created'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun updateUserGroups(jsonParams: JsonObject): User {
    val user: User = userRepository.findUser(organizationId = jsonParams.get("orgId").asLong, username = jsonParams.get("username").asString)
        ?: throw CustomJsonException("{username: 'User could not be determined'}")
    val group: Group = groupRepository.findGroup(organizationId = jsonParams.get("orgId").asLong, name = jsonParams.get("groupName").asString)
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
      userJpaRepository.save(user)
    } catch (exception: Exception) {
      throw CustomJsonException("{username: 'Unable to update group for user'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun updateUserRoles(jsonParams: JsonObject): User {
    val user: User = userRepository.findUser(organizationId = jsonParams.get("orgId").asLong, username = jsonParams.get("username").asString)
        ?: throw CustomJsonException("{username: 'User could not be determined'}")
    val role: Role = roleRepository.findRole(organizationId = jsonParams.get("orgId").asLong, name = jsonParams.get("roleName").asString)
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
      userJpaRepository.save(user)
    } catch (exception: Exception) {
      throw CustomJsonException("{username: 'Unable to update role for user'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun updateUserDetails(jsonParams: JsonObject): User {
    val user: User = userRepository.findUser(organizationId = jsonParams.get("orgId").asLong, username = jsonParams.get("username").asString)
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
      userJpaRepository.save(user)
    } catch (exception: Exception) {
      throw CustomJsonException("{username: 'Unable to update user details'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun getUserDetails(jsonParams: JsonObject): User {
    return (userRepository.findUser(organizationId = jsonParams.get("orgId").asLong, username = jsonParams.get("username").asString)
        ?: throw CustomJsonException("{username: 'User could not be determined'}"))
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun getUserTypePermissions(jsonParams: JsonObject): Set<TypePermission> {
    return (userRepository.getUserTypePermissions(organizationId = jsonParams.get("orgId").asLong, superTypeName = GLOBAL_TYPE, typeName = jsonParams.get("typeName").asString, username = jsonParams.get("username").asString))
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun getUserFunctionPermissions(jsonParams: JsonObject): Set<FunctionPermission> {
    return (userRepository.getUserFunctionPermissions(organizationId = jsonParams.get("orgId").asLong, functionName = jsonParams.get("functionName").asString, username = jsonParams.get("username").asString))
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun superimposeUserTypePermissions(jsonParams: JsonObject): TypePermission {
    val typePermissions: Set<TypePermission> = userRepository.getUserTypePermissions(organizationId = jsonParams.get("orgId").asLong, superTypeName = if (jsonParams.has("superTypeName")) jsonParams.get("superTypeName").asString else GLOBAL_TYPE, typeName = jsonParams.get("typeName").asString, username = jsonParams.get("username").asString)
    if (typePermissions.isNotEmpty())
      return typePermissionService.superimposeTypePermissions(typePermissions = typePermissions, type = typePermissions.first().type)
    else
      throw CustomJsonException("{error: 'Unauthorized Access'}")
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun superimposeUserFunctionPermissions(jsonParams: JsonObject): FunctionPermission {
    val functionPermissions: Set<FunctionPermission> = userRepository.getUserFunctionPermissions(organizationId = jsonParams.get("orgId").asLong, functionName = jsonParams.get("functionName").asString, username = jsonParams.get("username").asString)
    if (functionPermissions.isNotEmpty())
      return functionPermissionService.superimposeFunctionPermissions(functionPermissions = functionPermissions, function = functionPermissions.first().function)
    else
      throw CustomJsonException("{error: 'Unauthorized Access'}")
  }
}
