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
import com.pibity.erp.commons.constants.FunctionConstants
import com.pibity.erp.commons.constants.MessageConstants
import com.pibity.erp.commons.constants.OrganizationConstants
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
import org.springframework.web.multipart.MultipartFile
import java.sql.Timestamp

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

  fun createUser(jsonParams: JsonObject, defaultTimestamp: Timestamp = Timestamp(System.currentTimeMillis()), files: List<MultipartFile>): User {
    val organization: Organization = organizationJpaRepository.getById(jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
      ?: throw CustomJsonException("{${OrganizationConstants.ORGANIZATION_ID}: ${MessageConstants.UNEXPECTED_VALUE}}")
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
        throw CustomJsonException("{roles: ${MessageConstants.UNEXPECTED_VALUE}}")
      }
      val role: Role = roleRepository.findRole(orgId = organization.id, name = roleName)
        ?: throw CustomJsonException("{roleName: 'Role could not be determined'}")
      user.userRoles.add(UserRole(id = UserRoleId(user = user, role = role)))
    }
    user = userJpaRepository.save(user)
    user.details = try {
      variableService.createVariable(jsonParams = JsonObject().apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
        addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
        addProperty(OrganizationConstants.TYPE_NAME, "User")
        addProperty("variableName", keycloakId)
        add("values", jsonParams.get("details").asJsonObject)
      }, defaultTimestamp = defaultTimestamp, files = files).first
    } catch (exception: CustomJsonException) {
      throw CustomJsonException("{details: ${exception.message}}")
    }
    return try {
      userJpaRepository.save(user)
    } catch (exception: Exception) {
      throw CustomJsonException("{${OrganizationConstants.USERNAME}: 'User could not be created'}")
    }
  }

  fun updateUserGroups(jsonParams: JsonObject): User {
    val user: User = userRepository.findUser(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, username = jsonParams.get(OrganizationConstants.USERNAME).asString)
      ?: throw CustomJsonException("{${OrganizationConstants.USERNAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    val group: Group = groupRepository.findGroup(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get("groupName").asString)
      ?: throw CustomJsonException("{groupName: ${MessageConstants.UNEXPECTED_VALUE}}")
    when (jsonParams.get("operation").asString) {
      "add" -> user.userGroups.add(UserGroup(id = UserGroupId(user = user, group = group)))
      "remove" -> {
        userGroupRepository.delete(UserGroup(id = UserGroupId(user = user, group = group)))
        user.userGroups.remove(UserGroup(id = UserGroupId(user = user, group = group)))
      }
      else -> throw CustomJsonException("{operation: ${MessageConstants.UNEXPECTED_VALUE}}")
    }
    return try {
      userJpaRepository.save(user)
    } catch (exception: Exception) {
      throw CustomJsonException("{${OrganizationConstants.USERNAME}: 'Unable to update group for user'}")
    }
  }

  fun updateUserRoles(jsonParams: JsonObject): User {
    val user: User = userRepository.findUser(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, username = jsonParams.get(OrganizationConstants.USERNAME).asString)
      ?: throw CustomJsonException("{${OrganizationConstants.USERNAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    val role: Role = roleRepository.findRole(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get("roleName").asString)
      ?: throw CustomJsonException("{roleName: ${MessageConstants.UNEXPECTED_VALUE}}")
    when (jsonParams.get("operation").asString) {
      "add" -> user.userRoles.add(UserRole(id = UserRoleId(user = user, role = role)))
      "remove" -> {
        userRoleRepository.delete(UserRole(id = UserRoleId(user = user, role = role)))
        user.userRoles.remove(UserRole(id = UserRoleId(user = user, role = role)))
      }
      else -> throw CustomJsonException("{operation: ${MessageConstants.UNEXPECTED_VALUE}}")
    }
    return try {
      userJpaRepository.save(user)
    } catch (exception: Exception) {
      throw CustomJsonException("{${OrganizationConstants.USERNAME}: 'Unable to update role for user'}")
    }
  }

  fun updateUserDetails(jsonParams: JsonObject): User {
    val user: User = userRepository.findUser(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, username = jsonParams.get(OrganizationConstants.USERNAME).asString)
      ?: throw CustomJsonException("{${OrganizationConstants.USERNAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
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
      throw CustomJsonException("{${OrganizationConstants.USERNAME}: 'Unable to update user details'}")
    }
  }

  fun getUserDetails(jsonParams: JsonObject): User {
    return (userRepository.findUser(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, username = jsonParams.get(OrganizationConstants.USERNAME).asString)
    ?: throw CustomJsonException("{${OrganizationConstants.USERNAME}: ${MessageConstants.UNEXPECTED_VALUE}}"))
  }

  fun getUserTypePermissions(jsonParams: JsonObject): Set<TypePermission> {
    return (userRepository.getUserTypePermissions(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, typeName = jsonParams.get(OrganizationConstants.TYPE_NAME).asString, username = jsonParams.get(OrganizationConstants.USERNAME).asString))
  }

  fun getUserFunctionPermissions(jsonParams: JsonObject): Set<FunctionPermission> {
    return (userRepository.getUserFunctionPermissions(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, functionName = jsonParams.get(FunctionConstants.FUNCTION_NAME).asString, username = jsonParams.get(OrganizationConstants.USERNAME).asString))
  }

  fun superimposeUserTypePermissions(jsonParams: JsonObject): TypePermission {
    val typePermissions: Set<TypePermission> = userRepository.getUserTypePermissions(orgId = jsonParams.get(
      OrganizationConstants.ORGANIZATION_ID).asLong, typeName = jsonParams.get(OrganizationConstants.TYPE_NAME).asString, username = jsonParams.get(OrganizationConstants.USERNAME).asString)
    if (typePermissions.isNotEmpty())
      return typePermissionService.superimposeTypePermissions(typePermissions = typePermissions, type = typePermissions.first().type)
    else
      throw CustomJsonException("{${OrganizationConstants.ERROR}: ${MessageConstants.UNAUTHORIZED_ACCESS}}")
  }

  fun superimposeUserFunctionPermissions(jsonParams: JsonObject): FunctionPermission {
    val functionPermissions: Set<FunctionPermission> = userRepository.getUserFunctionPermissions(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, functionName = jsonParams.get(FunctionConstants.FUNCTION_NAME).asString, username = jsonParams.get(OrganizationConstants.USERNAME).asString)
    if (functionPermissions.isNotEmpty())
      return functionPermissionService.superimposeFunctionPermissions(functionPermissions = functionPermissions, function = functionPermissions.first().function)
    else
      throw CustomJsonException("{${OrganizationConstants.ERROR}: ${MessageConstants.UNAUTHORIZED_ACCESS}}")
  }
}
