/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.services

import com.google.gson.JsonObject
import com.pibity.core.commons.CustomJsonException
import com.pibity.core.commons.constants.*
import com.pibity.core.entities.Organization
import com.pibity.core.entities.Space
import com.pibity.core.entities.Subspace
import com.pibity.core.entities.mappings.SpaceFunctionPermission
import com.pibity.core.entities.mappings.SpaceTypePermission
import com.pibity.core.entities.mappings.embeddables.SpaceFunctionPermissionId
import com.pibity.core.entities.mappings.embeddables.SpaceTypePermissionId
import com.pibity.core.entities.permission.FunctionPermission
import com.pibity.core.entities.permission.TypePermission
import com.pibity.core.repositories.jpa.OrganizationJpaRepository
import com.pibity.core.repositories.jpa.SpaceJpaRepository
import com.pibity.core.repositories.jpa.SubspaceJpaRepository
import com.pibity.core.repositories.mappings.SpaceFunctionPermissionRepository
import com.pibity.core.repositories.mappings.SpaceTypePermissionRepository
import com.pibity.core.repositories.query.FunctionPermissionRepository
import com.pibity.core.repositories.query.SpaceRepository
import com.pibity.core.repositories.query.TypePermissionRepository
import org.springframework.stereotype.Service
import java.sql.Timestamp

@Service
class SpaceService(
  val organizationJpaRepository: OrganizationJpaRepository,
  val typePermissionRepository: TypePermissionRepository,
  val spaceRepository: SpaceRepository,
  val spaceJpaRepository: SpaceJpaRepository,
  val subspaceJpaRepository: SubspaceJpaRepository,
  val spaceTypePermissionRepository: SpaceTypePermissionRepository,
  val functionPermissionRepository: FunctionPermissionRepository,
  val spaceFunctionPermissionRepository: SpaceFunctionPermissionRepository
) {

  fun createSpace(jsonParams: JsonObject, defaultTimestamp: Timestamp): Space {
    val organization: Organization = organizationJpaRepository.getById(jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
      ?: throw CustomJsonException("{${OrganizationConstants.ORGANIZATION_ID}: ${MessageConstants.UNEXPECTED_VALUE}}")
    return try {
      subspaceJpaRepository.save(Subspace(
        space = spaceJpaRepository.save(Space(organization = organization, name = jsonParams.get(SpaceConstants.SPACE_NAME).asString,
          active = jsonParams.get(UserConstants.ACTIVE).asBoolean, default = jsonParams.get(KeyConstants.DEFAULT).asBoolean, created = defaultTimestamp)),
        name = SpaceConstants.SUBSPACE_MASTER, created = defaultTimestamp)).space
    } catch (exception: Exception) {
      throw CustomJsonException("{${SpaceConstants.SPACE_NAME}: 'Space could not be saved'}")
    }
  }

  fun updateSpaceTypePermissions(jsonParams: JsonObject, defaultTimestamp: Timestamp): Space {
    val space: Space = spaceRepository.findSpace(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get(SpaceConstants.SPACE_NAME).asString)
      ?: throw CustomJsonException("{${SpaceConstants.SPACE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    val typePermission: TypePermission = typePermissionRepository.findTypePermission(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, typeName = jsonParams.get(OrganizationConstants.TYPE_NAME).asString, name = jsonParams.get(PermissionConstants.PERMISSION_NAME).asString)
      ?: throw CustomJsonException("{${PermissionConstants.PERMISSION_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    when (jsonParams.get(GroupConstants.OPERATION).asString) {
      GroupConstants.ADD -> space.spaceTypePermissions.add(SpaceTypePermission(id = SpaceTypePermissionId(space = space, permission = typePermission), created = defaultTimestamp))
      GroupConstants.REMOVE -> {
        spaceTypePermissionRepository.delete(SpaceTypePermission(id = SpaceTypePermissionId(space = space, permission = typePermission), created = defaultTimestamp))
        space.spaceTypePermissions.remove(SpaceTypePermission(id = SpaceTypePermissionId(space = space, permission = typePermission), created = defaultTimestamp))
      }
      else -> throw CustomJsonException("{${GroupConstants.OPERATION}: ${MessageConstants.UNEXPECTED_VALUE}}")
    }
    return try {
      spaceJpaRepository.save(space)
    } catch (exception: Exception) {
      throw CustomJsonException("{${SpaceConstants.SPACE_NAME}: 'Space could not be saved'}")
    }
  }

  fun updateSpaceFunctionPermissions(jsonParams: JsonObject, defaultTimestamp: Timestamp): Space {
    val space: Space = spaceRepository.findSpace(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get(SpaceConstants.SPACE_NAME).asString)
        ?: throw CustomJsonException("{${SpaceConstants.SPACE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    val functionPermission: FunctionPermission = functionPermissionRepository.findFunctionPermission(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, functionName = jsonParams.get(FunctionConstants.FUNCTION_NAME).asString, name = jsonParams.get(PermissionConstants.PERMISSION_NAME).asString)
      ?: throw CustomJsonException("{${PermissionConstants.PERMISSION_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    when (jsonParams.get(GroupConstants.OPERATION).asString) {
      GroupConstants.ADD -> space.spaceFunctionPermissions.add(SpaceFunctionPermission(id = SpaceFunctionPermissionId(space = space, permission = functionPermission), created = defaultTimestamp))
      GroupConstants.REMOVE -> {
        spaceFunctionPermissionRepository.delete(SpaceFunctionPermission(id = SpaceFunctionPermissionId(space = space, permission = functionPermission), created = defaultTimestamp))
        space.spaceFunctionPermissions.remove(SpaceFunctionPermission(id = SpaceFunctionPermissionId(space = space, permission = functionPermission), created = defaultTimestamp))
      }
      else -> throw CustomJsonException("{${GroupConstants.OPERATION}: ${MessageConstants.UNEXPECTED_VALUE}}")
    }
    return try {
      spaceJpaRepository.save(space)
    } catch (exception: Exception) {
      throw CustomJsonException("{${SpaceConstants.SPACE_NAME}: 'Space could not be saved'}")
    }
  }

  fun getSpaceDetails(jsonParams: JsonObject): Space {
    return (spaceRepository.findSpace(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get(SpaceConstants.SPACE_NAME).asString)
        ?: throw CustomJsonException("{${SpaceConstants.SPACE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}"))
  }
}
