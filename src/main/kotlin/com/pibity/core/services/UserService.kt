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
import com.pibity.core.commons.CustomJsonException
import com.pibity.core.utils.createKeycloakUser
import com.pibity.core.utils.getKeycloakId
import com.pibity.core.utils.gson
import com.pibity.core.utils.joinKeycloakGroups
import com.pibity.core.entities.*
import com.pibity.core.entities.mappings.UserGroup
import com.pibity.core.entities.mappings.UserSubspace
import com.pibity.core.entities.mappings.embeddables.UserGroupId
import com.pibity.core.entities.mappings.embeddables.UserSubspaceId
import com.pibity.core.entities.permission.FunctionPermission
import com.pibity.core.entities.permission.TypePermission
import com.pibity.core.repositories.jpa.OrganizationJpaRepository
import com.pibity.core.repositories.jpa.SubspaceJpaRepository
import com.pibity.core.repositories.jpa.UserJpaRepository
import com.pibity.core.repositories.mappings.UserGroupRepository
import com.pibity.core.repositories.mappings.UserSubspaceRepository
import com.pibity.core.repositories.query.GroupRepository
import com.pibity.core.repositories.query.SpaceRepository
import com.pibity.core.repositories.query.SubspaceRepository
import com.pibity.core.repositories.query.UserRepository
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.sql.Timestamp

@Service
class UserService(
  val organizationJpaRepository: OrganizationJpaRepository,
  val groupRepository: GroupRepository,
  val spaceRepository: SpaceRepository,
  val subspaceRepository: SubspaceRepository,
  val subspaceJpaRepository: SubspaceJpaRepository,
  val userRepository: UserRepository,
  val userJpaRepository: UserJpaRepository,
  val userSubspaceRepository: UserSubspaceRepository,
  val userGroupRepository: UserGroupRepository,
  @Lazy val variableService: VariableService
) {

  fun createUser(jsonParams: JsonObject, files: List<MultipartFile>, defaultTimestamp: Timestamp): Pair<User, TypePermission> {
    val organization: Organization = organizationJpaRepository.getById(jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
      ?: throw CustomJsonException("{${OrganizationConstants.ORGANIZATION_ID}: ${MessageConstants.UNEXPECTED_VALUE}}")
    val keycloakId: String = try {
      getKeycloakId(jsonParams.get(UserConstants.EMAIL).asString)
    } catch (exception: Exception) {
      createKeycloakUser(jsonParams = jsonParams)
    }
    joinKeycloakGroups(jsonParams = jsonParams.apply {
      addProperty(KeycloakConstants.KEYCLOAK_USERNAME, keycloakId)
      if (!jsonParams.has(KeycloakConstants.SUBGROUPS))
        jsonParams.add(KeycloakConstants.SUBGROUPS, gson.fromJson(gson.toJson(listOf(KeycloakConstants.SUBGROUP_USER)), JsonArray::class.java))
    })
    val user = userJpaRepository.save(User(organization = organization, username = keycloakId,
      active = jsonParams.get(UserConstants.ACTIVE).asBoolean,
      email = jsonParams.get(UserConstants.EMAIL).asString,
      firstName = jsonParams.get(UserConstants.FIRST_NAME).asString,
      lastName = jsonParams.get(UserConstants.LAST_NAME).asString,
      created = defaultTimestamp
    )).apply {
      this.userSubspaces.addAll(jsonParams.get("subspaces").asJsonArray.map {
        userSubspaceRepository.save(UserSubspace(id = UserSubspaceId(user = this,
          subspace = subspaceRepository.findSubspace(orgId = this.organization.id, spaceName = it.asJsonObject.get(SpaceConstants.SPACE_NAME).asString, name = it.asJsonObject.get(SpaceConstants.SUBSPACE_NAME).asString)
            ?: throw CustomJsonException("{${OrganizationConstants.ORGANIZATION_ID}: ${MessageConstants.UNEXPECTED_VALUE}}")),
          created = defaultTimestamp))
      })
      this.userSubspaces.addAll(spaceRepository.findDefaultSpaces(orgId = this.organization.id).map {
        userSubspaceRepository.save(UserSubspace(id = UserSubspaceId(user = this,
          subspace = subspaceJpaRepository.save(Subspace(space = it, name = it.name + "_" + this.username, created = defaultTimestamp))),
          created = defaultTimestamp))
      })
    }
    val (details: Variable, typePermission: TypePermission) = try {
      variableService.createVariable(jsonParams = JsonObject().apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
        addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
        addProperty(OrganizationConstants.TYPE_NAME, "User")
        addProperty(VariableConstants.VARIABLE_NAME, keycloakId)
        add(VariableConstants.VALUES, jsonParams.get(UserConstants.DETAILS).asJsonObject)
      }, defaultTimestamp = defaultTimestamp, files = files)
    } catch (exception: CustomJsonException) {
      throw CustomJsonException("{${UserConstants.DETAILS}: ${exception.message}}")
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
    val group: Group = groupRepository.findGroup(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get(GroupConstants.GROUP_NAME).asString)
      ?: throw CustomJsonException("{${GroupConstants.GROUP_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    when (jsonParams.get(GroupConstants.OPERATION).asString) {
      GroupConstants.ADD -> user.userGroups.add(UserGroup(id = UserGroupId(user = user, group = group), created = defaultTimestamp))
      GroupConstants.REMOVE -> {
        userGroupRepository.delete(UserGroup(id = UserGroupId(user = user, group = group), created = defaultTimestamp))
        user.userGroups.remove(UserGroup(id = UserGroupId(user = user, group = group), created = defaultTimestamp))
      }
      else -> throw CustomJsonException("{${GroupConstants.OPERATION}: ${MessageConstants.UNEXPECTED_VALUE}}")
    }
    val typePermission: TypePermission = getUserTypePermission(
      orgId = user.organization.id,
      username = user.username,
      subspaceName = "${SpaceConstants.SPACE_USER}_${user.username}",
      spaceName = SpaceConstants.SPACE_USER,
      typeName = "User",
      permissionType = PermissionConstants.READ,
      defaultTimestamp = defaultTimestamp)
    return try {
      Pair(userJpaRepository.save(user), typePermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{${OrganizationConstants.USERNAME}: 'Unable to update group for user'}")
    }
  }

  fun updateUserSubspaces(jsonParams: JsonObject, defaultTimestamp: Timestamp): Pair<User, TypePermission> {
    val user: User = userRepository.findUser(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, username = jsonParams.get(OrganizationConstants.USERNAME).asString)
      ?: throw CustomJsonException("{${OrganizationConstants.USERNAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    val subspace: Subspace = subspaceRepository.findSubspace(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, spaceName = jsonParams.get(SpaceConstants.SPACE_NAME).asString, name = jsonParams.get(SpaceConstants.SUBSPACE_NAME).asString)
      ?: throw CustomJsonException("{${SpaceConstants.SUBSPACE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    when (jsonParams.get(GroupConstants.OPERATION).asString) {
      GroupConstants.ADD -> user.userSubspaces.add(UserSubspace(id = UserSubspaceId(user = user, subspace = subspace), created = defaultTimestamp))
      GroupConstants.REMOVE -> {
        userSubspaceRepository.delete(UserSubspace(id = UserSubspaceId(user = user, subspace = subspace), created = defaultTimestamp))
        user.userSubspaces.remove(UserSubspace(id = UserSubspaceId(user = user, subspace = subspace), created = defaultTimestamp))
      }
      else -> throw CustomJsonException("{${GroupConstants.OPERATION}: ${MessageConstants.UNEXPECTED_VALUE}}")
    }
    val typePermission: TypePermission = getUserTypePermission(
      orgId = user.organization.id,
      username = user.username,
      subspaceName = "${SpaceConstants.SPACE_USER}_${user.username}",
      spaceName = SpaceConstants.SPACE_USER,
      typeName = "User",
      permissionType = PermissionConstants.READ,
      defaultTimestamp = defaultTimestamp)
    return try {
      Pair(userJpaRepository.save(user), typePermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{${OrganizationConstants.USERNAME}: 'Unable to update role for user'}")
    }
  }

  fun updateUserDetails(jsonParams: JsonObject, defaultTimestamp: Timestamp): Pair<User, TypePermission> {
    val user: User = userRepository.findUser(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, username = jsonParams.get(OrganizationConstants.USERNAME).asString)
      ?: throw CustomJsonException("{${OrganizationConstants.USERNAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    if (jsonParams.has("${UserConstants.ACTIVE}?"))
      user.active = jsonParams.get("${UserConstants.ACTIVE}?").asBoolean
    if (jsonParams.has("${UserConstants.EMAIL}?"))
      user.email = jsonParams.get("${UserConstants.EMAIL}?").asString
    if (jsonParams.has("${UserConstants.FIRST_NAME}?"))
      user.firstName = jsonParams.get("${UserConstants.FIRST_NAME}?").asString
    if (jsonParams.has("${UserConstants.LAST_NAME}?"))
      user.lastName = jsonParams.get("${UserConstants.LAST_NAME}?").asString
    val typePermission: TypePermission = getUserTypePermission(
      orgId = user.organization.id,
      username = user.username,
      subspaceName = "${user.username}_${user.username}",
      spaceName = user.username,
      typeName = "User",
      permissionType = PermissionConstants.READ,
      defaultTimestamp = defaultTimestamp)
    return try {
      Pair(userJpaRepository.save(user), typePermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{${OrganizationConstants.USERNAME}: 'Unable to update user details'}")
    }
  }

  fun getUserDetails(jsonParams: JsonObject, defaultTimestamp: Timestamp): Pair<User, TypePermission> {
    val user: User = userRepository.findUser(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, username = jsonParams.get(OrganizationConstants.USERNAME).asString)
      ?: throw CustomJsonException("{${OrganizationConstants.USERNAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    val typePermission: TypePermission = getUserTypePermission(
      orgId = user.organization.id,
      username = user.username,
      subspaceName = "${user.username}_${user.username}",
      spaceName = user.username,
      typeName = "User",
      permissionType = PermissionConstants.READ,
      defaultTimestamp = defaultTimestamp)
    return(Pair(user, typePermission))
  }

  fun getUserTypePermissions(jsonParams: JsonObject): Set<TypePermission> {
    return (userRepository.getUserTypePermissions(
      orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong,
      username = jsonParams.get(OrganizationConstants.USERNAME).asString,
      subspaceName = jsonParams.get(SpaceConstants.SUBSPACE_NAME).asString,
      spaceName = jsonParams.get(SpaceConstants.SPACE_NAME).asString,
      typeName = jsonParams.get(OrganizationConstants.TYPE_NAME).asString,
      permissionType = jsonParams.get(PermissionConstants.PERMISSION_TYPE).asString))
  }

  fun getUserFunctionPermissions(jsonParams: JsonObject): Set<FunctionPermission> {
    return (userRepository.getUserFunctionPermissions(
      orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong,
      username = jsonParams.get(OrganizationConstants.USERNAME).asString,
      subspaceName = jsonParams.get(SpaceConstants.SUBSPACE_NAME).asString,
      spaceName = jsonParams.get(SpaceConstants.SPACE_NAME).asString,
      functionName = jsonParams.get(FunctionConstants.FUNCTION_NAME).asString))
  }

  fun getUserTypePermission(orgId: Long, username: String, subspaceName: String, spaceName: String, permissionType: String, typeName: String, defaultTimestamp: Timestamp): TypePermission {
    val typePermissions: Set<TypePermission> = userRepository.getUserTypePermissions(orgId = orgId, username = username, subspaceName = subspaceName, spaceName = spaceName,
      permissionType = permissionTypes.single { it == permissionType }, typeName = typeName)
    return if (typePermissions.isEmpty())
      throw CustomJsonException("{${OrganizationConstants.ERROR}: ${MessageConstants.UNAUTHORIZED_ACCESS}}")
    else {
      val typePermission: TypePermission = typePermissions.first()
      TypePermission(type = typePermission.type, name = "*", permissionType = typePermission.permissionType,
        keys = typePermissions.fold(mutableSetOf()) { acc, tp -> acc.apply { addAll(tp.keys) } },
        created = defaultTimestamp)
    }
  }

  fun getUserFunctionPermission(orgId: Long, username: String, subspaceName: String, spaceName: String, permissionType: String, functionName: String, defaultTimestamp: Timestamp): FunctionPermission {
    val functionPermissions: Set<FunctionPermission> = userRepository.getUserFunctionPermissions(orgId = orgId, username = username, subspaceName = subspaceName, spaceName = spaceName, functionName = functionName)
    return if (functionPermissions.isEmpty())
      throw CustomJsonException("{${OrganizationConstants.ERROR}: ${MessageConstants.UNAUTHORIZED_ACCESS}}")
    else {
      val functionPermission: FunctionPermission = functionPermissions.first()
      FunctionPermission(function = functionPermission.function, name = "*",
        functionInputs = functionPermissions.fold(mutableSetOf()) { acc, tp -> acc.apply { addAll(tp.functionInputs) } },
        functionOutputs = functionPermissions.fold(mutableSetOf()) { acc, tp -> acc.apply { addAll(tp.functionOutputs) } },
        created = defaultTimestamp)
    }
  }

  fun serialize(user: User, typePermission: TypePermission): JsonObject = JsonObject().apply {
    addProperty(OrganizationConstants.ORGANIZATION_ID, user.organization.id)
    addProperty(OrganizationConstants.USERNAME, user.username)
    addProperty(UserConstants.ACTIVE, user.active)
    addProperty(UserConstants.EMAIL, user.email)
    addProperty(UserConstants.FIRST_NAME, user.firstName)
    addProperty(UserConstants.LAST_NAME, user.lastName)
    add(UserConstants.DETAILS, variableService.serialize(user.details!!, typePermission))
    add("groups", com.pibity.core.serializers.mappings.serialize(user.userGroups))
    add("subspaces", com.pibity.core.serializers.mappings.serialize(user.userSubspaces))
  }

  fun serialize(entities: Set<User>, typePermission: TypePermission): JsonArray = entities.fold(JsonArray()) { acc, entity -> acc.apply { add(serialize(entity, typePermission)) } }
}
