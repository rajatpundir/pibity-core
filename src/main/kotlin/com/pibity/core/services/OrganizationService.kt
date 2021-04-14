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
import com.pibity.core.entities.Organization
import com.pibity.core.entities.Subspace
import com.pibity.core.entities.Type
import com.pibity.core.entities.User
import com.pibity.core.entities.mappings.UserSubspace
import com.pibity.core.entities.mappings.embeddables.UserSubspaceId
import com.pibity.core.repositories.jpa.*
import com.pibity.core.repositories.query.RoleRepository
import com.pibity.core.utils.*
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.FileReader
import java.sql.Timestamp

@Service
class OrganizationService(
  val organizationJpaRepository: OrganizationJpaRepository,
  val typeJpaRepository: TypeJpaRepository,
  val typeService: TypeService,
  val userJpaRepository: UserJpaRepository,
  val userService: UserService,
  val subspaceService: SubspaceService,
  val roleRepository: RoleRepository,
  val variableService: VariableService,
  val functionService: FunctionService
) {

  fun createOrganization(jsonParams: JsonObject, files: List<MultipartFile>, defaultTimestamp: Timestamp): Organization {
    val organizationName: String = jsonParams.get("organization").asString
    val organization: Organization = try {
      organizationJpaRepository.save(Organization(name = organizationName, created = defaultTimestamp))
    } catch (exception: Exception) {
      throw CustomJsonException("{'organization': 'Organization $organizationName is already present'}")
    }
    // Re-evaluate below when running this in production
    try {
      createKeycloakGroup(jsonParams = JsonObject().apply { addProperty(OrganizationConstants.ORGANIZATION_ID, organization.id) })
    } catch (exception: Exception) {
      println("Keycloak Group already exists")
    }
    createDefaultRoles(organization = organization, defaultTimestamp = defaultTimestamp)
    createPrimitiveTypes(organization = organization, defaultTimestamp = defaultTimestamp)
    val superuserId: String = try {
      getKeycloakId(KeycloakConstants.SUPERUSER_USERNAME)
    } catch (exception: Exception) {
      createKeycloakUser(jsonParams = JsonObject().apply {
        addProperty("firstName", "System")
        addProperty("lastName", "Administrator")
        addProperty("email", KeycloakConstants.SUPERUSER_USERNAME)
        addProperty("isEnabled", true)
      })
    }
    var superuser = User(organization = organization, username = superuserId,
      active = true,
      email = KeycloakConstants.SUPERUSER_USERNAME,
      firstName = "System",
      lastName = "Administrator",
      created = defaultTimestamp
    )
    superuser = userJpaRepository.save(superuser)
    val role: Subspace = roleRepository.findRole(orgId = organization.id, name = SpaceConstants.ADMIN)
        ?: throw CustomJsonException("{roleName: 'Role could not be determined'}")
    superuser.userSubspaces.add(UserSubspace(id = UserSubspaceId(user = superuser, role = role), created = defaultTimestamp))
    superuser = userJpaRepository.save(superuser)
    createCustomTypes(organization = organization, username = superuser.username, defaultTimestamp = defaultTimestamp, files = files)
    superuser.details = try {
      variableService.createVariable(jsonParams = JsonObject().apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, organization.id)
        addProperty(OrganizationConstants.USERNAME, superuserId)
        addProperty(OrganizationConstants.TYPE_NAME, "User")
        addProperty(VariableConstants.VARIABLE_NAME, superuserId)
        add(VariableConstants.VALUES, JsonObject())
      }, defaultTimestamp = defaultTimestamp, files = files).first
    } catch (exception: CustomJsonException) {
      throw CustomJsonException("{details: ${exception.message}}")
    }
    try {
      userJpaRepository.save(superuser)
    } catch (exception: Exception) {
      throw CustomJsonException("{${OrganizationConstants.USERNAME}: 'User could not be created'}")
    }
    createCustomFunctions(organization = organization, username = superuser.username, files = files, defaultTimestamp = defaultTimestamp)
    userService.createUser(JsonObject().apply {
      addProperty(OrganizationConstants.ORGANIZATION_ID, organization.id)
      addProperty(OrganizationConstants.USERNAME, superuserId)
      addProperty("email", jsonParams.get("admin").asString)
      addProperty("active", true)
      addProperty("firstName", jsonParams.get("firstName").asString)
      addProperty("lastName", jsonParams.get("lastName").asString)
      addProperty("password", jsonParams.get("password").asString)
      add("roles", JsonArray().apply { add(SpaceConstants.ADMIN) })
      add("details", jsonParams.get("details").asJsonObject)
    }, defaultTimestamp = defaultTimestamp, files = files)
    return organization
  }

  fun createDefaultRoles(organization: Organization, defaultTimestamp: Timestamp) {
    val jsonRoles: JsonArray = gson.fromJson(FileReader("src/main/resources/roles/index.json"), JsonArray::class.java)
    for (jsonRole in jsonRoles) {
      subspaceService.createRole(JsonObject().apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, organization.id)
        addProperty("roleName", jsonRole.asString)
      }, defaultTimestamp = defaultTimestamp)
    }
  }

  fun createPrimitiveTypes(organization: Organization, defaultTimestamp: Timestamp) {
    try {
      for (primitiveType in primitiveTypes)
        typeJpaRepository.save(Type(organization = organization, name = primitiveType, primitiveType = true, created = defaultTimestamp))
    } catch (exception: Exception) {
      throw CustomJsonException("{'organization': 'Organization ${organization.id} could not be created'}")
    }
   }

  fun createCustomTypes(organization: Organization, username: String, defaultTimestamp: Timestamp, files: List<MultipartFile>) {
    val customTypeFilenames: JsonArray = gson.fromJson(FileReader("src/main/resources/types/index.json"), JsonArray::class.java)
    for (filename in customTypeFilenames) {
      val types: JsonArray = gson.fromJson(FileReader("src/main/resources/types/${filename.asString}.json"), JsonArray::class.java)
      for (json in types) {
        val typeRequest = json.asJsonObject.apply {
          addProperty(OrganizationConstants.ORGANIZATION_ID, organization.id)
        }
        typeService.createType(jsonParams = getJsonParams(typeRequest.toString(), getExpectedParams("type", "createType")).apply {
          addProperty(OrganizationConstants.USERNAME, username)
        }, defaultTimestamp = defaultTimestamp, files = files)
      }
    }
  }

  fun createCustomFunctions(organization: Organization, username: String, files: List<MultipartFile>, defaultTimestamp: Timestamp) {
    val customTypeFilenames: JsonArray = gson.fromJson(FileReader("src/main/resources/functions/index.json"), JsonArray::class.java)
    for (filename in customTypeFilenames) {
      val functions: JsonArray = gson.fromJson(FileReader("src/main/resources/functions/${filename.asString}.json"), JsonArray::class.java)
      for (json in functions) {
        val functionRequest = json.asJsonObject.apply {
          addProperty(OrganizationConstants.ORGANIZATION_ID, organization.id)
        }
        functionService.createFunction(jsonParams = getJsonParams(functionRequest.toString(), getExpectedParams("function", "createFunction")).apply {
          addProperty(OrganizationConstants.USERNAME, username)
        }, files = files, defaultTimestamp = defaultTimestamp)
      }
    }
  }
}
