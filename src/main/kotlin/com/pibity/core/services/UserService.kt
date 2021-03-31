/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.services

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.core.commons.constants.*
import com.pibity.core.commons.exceptions.CustomJsonException
import com.pibity.core.utils.createKeycloakUser
import com.pibity.core.utils.getKeycloakId
import com.pibity.core.utils.gson
import com.pibity.core.utils.joinKeycloakGroups
import com.pibity.core.entities.*
import com.pibity.core.entities.mappings.UserGroup
import com.pibity.core.entities.mappings.UserRole
import com.pibity.core.entities.mappings.embeddables.UserGroupId
import com.pibity.core.entities.mappings.embeddables.UserRoleId
import com.pibity.core.entities.permission.FunctionPermission
import com.pibity.core.entities.permission.TypePermission
import com.pibity.core.repositories.jpa.OrganizationJpaRepository
import com.pibity.core.repositories.jpa.UserJpaRepository
import com.pibity.core.repositories.mappings.UserGroupRepository
import com.pibity.core.repositories.mappings.UserRoleRepository
import com.pibity.core.repositories.query.GroupRepository
import com.pibity.core.repositories.query.RoleRepository
import com.pibity.core.repositories.query.UserRepository
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

  fun createUser(jsonParams: JsonObject, files: List<MultipartFile>, defaultTimestamp: Timestamp): Pair<User, TypePermission> {
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
      lastName = jsonParams.get("lastName").asString,
      created = defaultTimestamp
    )
    user = userJpaRepository.save(user)
    jsonParams.get("roles").asJsonArray.forEach {
      val roleName: String = try {
        it.asString
      } catch (exception: Exception) {
        throw CustomJsonException("{roles: ${MessageConstants.UNEXPECTED_VALUE}}")
      }
      val role: Role = roleRepository.findRole(orgId = organization.id, name = roleName)
        ?: throw CustomJsonException("{roleName: 'Role could not be determined'}")
      user.userRoles.add(UserRole(id = UserRoleId(user = user, role = role), created = defaultTimestamp))
    }
    user = userJpaRepository.save(user)
    val (details: Variable, typePermission: TypePermission) = try {
      variableService.createVariable(jsonParams = JsonObject().apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
        addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
        addProperty(OrganizationConstants.TYPE_NAME, "User")
        addProperty(VariableConstants.VARIABLE_NAME, keycloakId)
        add(VariableConstants.VALUES, jsonParams.get("details").asJsonObject)
      }, defaultTimestamp = defaultTimestamp, files = files)
    } catch (exception: CustomJsonException) {
      throw CustomJsonException("{details: ${exception.message}}")
    }
    user.details = details
    return try {
      Pair(userJpaRepository.save(user), typePermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{${OrganizationConstants.USERNAME}: 'User could not be created'}")
    }
  }

  fun updateUserGroups(jsonParams: JsonObject, defaultTimestamp: Timestamp): Pair<User, TypePermission> {
    val user: User = userRepository.findUser(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, username = jsonParams.get(OrganizationConstants.USERNAME).asString)
      ?: throw CustomJsonException("{${OrganizationConstants.USERNAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    val group: Group = groupRepository.findGroup(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get("groupName").asString)
      ?: throw CustomJsonException("{groupName: ${MessageConstants.UNEXPECTED_VALUE}}")
    when (jsonParams.get("operation").asString) {
      "add" -> user.userGroups.add(UserGroup(id = UserGroupId(user = user, group = group), created = defaultTimestamp))
      "remove" -> {
        userGroupRepository.delete(UserGroup(id = UserGroupId(user = user, group = group), created = defaultTimestamp))
        user.userGroups.remove(UserGroup(id = UserGroupId(user = user, group = group), created = defaultTimestamp))
      }
      else -> throw CustomJsonException("{operation: ${MessageConstants.UNEXPECTED_VALUE}}")
    }
    val typePermission: TypePermission = superimposeUserTypePermissions(jsonParams = JsonObject().apply {
      addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
      addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
      addProperty(OrganizationConstants.TYPE_NAME, "User")
    }, defaultTimestamp = defaultTimestamp)
    return try {
      Pair(userJpaRepository.save(user), typePermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{${OrganizationConstants.USERNAME}: 'Unable to update group for user'}")
    }
  }

  fun updateUserRoles(jsonParams: JsonObject, defaultTimestamp: Timestamp): Pair<User, TypePermission> {
    val user: User = userRepository.findUser(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, username = jsonParams.get(OrganizationConstants.USERNAME).asString)
      ?: throw CustomJsonException("{${OrganizationConstants.USERNAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    val role: Role = roleRepository.findRole(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get("roleName").asString)
      ?: throw CustomJsonException("{roleName: ${MessageConstants.UNEXPECTED_VALUE}}")
    when (jsonParams.get("operation").asString) {
      "add" -> user.userRoles.add(UserRole(id = UserRoleId(user = user, role = role), created = defaultTimestamp))
      "remove" -> {
        userRoleRepository.delete(UserRole(id = UserRoleId(user = user, role = role), created = defaultTimestamp))
        user.userRoles.remove(UserRole(id = UserRoleId(user = user, role = role), created = defaultTimestamp))
      }
      else -> throw CustomJsonException("{operation: ${MessageConstants.UNEXPECTED_VALUE}}")
    }
    val typePermission: TypePermission = superimposeUserTypePermissions(jsonParams = JsonObject().apply {
      addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
      addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
      addProperty(OrganizationConstants.TYPE_NAME, "User")
    }, defaultTimestamp = defaultTimestamp)
    return try {
      Pair(userJpaRepository.save(user), typePermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{${OrganizationConstants.USERNAME}: 'Unable to update role for user'}")
    }
  }

  fun updateUserDetails(jsonParams: JsonObject, defaultTimestamp: Timestamp): Pair<User, TypePermission> {
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
    val typePermission: TypePermission = superimposeUserTypePermissions(jsonParams = JsonObject().apply {
      addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
      addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
      addProperty(OrganizationConstants.TYPE_NAME, "User")
    }, defaultTimestamp = defaultTimestamp)
    return try {
      Pair(userJpaRepository.save(user), typePermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{${OrganizationConstants.USERNAME}: 'Unable to update user details'}")
    }
  }

  fun getUserDetails(jsonParams: JsonObject, defaultTimestamp: Timestamp): Pair<User, TypePermission> {
    val user: User = userRepository.findUser(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, username = jsonParams.get(OrganizationConstants.USERNAME).asString)
      ?: throw CustomJsonException("{${OrganizationConstants.USERNAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    val typePermission: TypePermission = superimposeUserTypePermissions(jsonParams = JsonObject().apply {
      addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
      addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
      addProperty(OrganizationConstants.TYPE_NAME, "User")
    }, defaultTimestamp = defaultTimestamp)
    return(Pair(user, typePermission))
  }

  fun getUserTypePermissions(jsonParams: JsonObject): Set<TypePermission> {
    return (userRepository.getUserTypePermissions(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, typeName = jsonParams.get(OrganizationConstants.TYPE_NAME).asString, username = jsonParams.get(OrganizationConstants.USERNAME).asString))
  }

  fun getUserFunctionPermissions(jsonParams: JsonObject): Set<FunctionPermission> {
    return (userRepository.getUserFunctionPermissions(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, functionName = jsonParams.get(FunctionConstants.FUNCTION_NAME).asString, username = jsonParams.get(OrganizationConstants.USERNAME).asString))
  }

  fun superimposeUserTypePermissions(jsonParams: JsonObject, defaultTimestamp: Timestamp): TypePermission {
    val typePermissions: Set<TypePermission> = userRepository.getUserTypePermissions(orgId = jsonParams.get(
      OrganizationConstants.ORGANIZATION_ID).asLong, typeName = jsonParams.get(OrganizationConstants.TYPE_NAME).asString, username = jsonParams.get(OrganizationConstants.USERNAME).asString)
    if (typePermissions.isNotEmpty())
      return typePermissionService.superimposeTypePermissions(typePermissions = typePermissions, type = typePermissions.first().type, defaultTimestamp = defaultTimestamp)
    else
      throw CustomJsonException("{${OrganizationConstants.ERROR}: ${MessageConstants.UNAUTHORIZED_ACCESS}}")
  }

  fun superimposeUserFunctionPermissions(jsonParams: JsonObject, defaultTimestamp: Timestamp): FunctionPermission {
    val functionPermissions: Set<FunctionPermission> = userRepository.getUserFunctionPermissions(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, functionName = jsonParams.get(FunctionConstants.FUNCTION_NAME).asString, username = jsonParams.get(OrganizationConstants.USERNAME).asString)
    if (functionPermissions.isNotEmpty())
      return functionPermissionService.superimposeFunctionPermissions(functionPermissions = functionPermissions, function = functionPermissions.first().function, defaultTimestamp)
    else
      throw CustomJsonException("{${OrganizationConstants.ERROR}: ${MessageConstants.UNAUTHORIZED_ACCESS}}")
  }

  fun serialize(user: User, typePermission: TypePermission): JsonObject = JsonObject().apply {
    addProperty(OrganizationConstants.ORGANIZATION_ID, user.organization.id)
    addProperty(OrganizationConstants.USERNAME, user.username)
    addProperty("active", user.active)
    addProperty("email", user.email)
    addProperty("firstName", user.firstName)
    addProperty("lastName", user.lastName)
    add("details", variableService.serialize(user.details!!, typePermission))
    add("groups", com.pibity.core.serializers.mappings.serialize(user.userGroups))
    add("roles", com.pibity.core.serializers.mappings.serialize(user.userRoles))
  }

  fun serialize(entities: Set<User>, typePermission: TypePermission): JsonArray = entities.fold(JsonArray()) { acc, entity -> acc.apply { add(serialize(entity, typePermission)) } }
}
