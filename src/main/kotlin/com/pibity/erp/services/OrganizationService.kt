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
import com.pibity.erp.commons.constants.KeycloakConstants
import com.pibity.erp.commons.constants.RoleConstants
import com.pibity.erp.commons.constants.primitiveTypes
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.utils.*
import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.Role
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.User
import com.pibity.erp.entities.mappings.UserRole
import com.pibity.erp.entities.mappings.embeddables.UserRoleId
import com.pibity.erp.repositories.jpa.*
import com.pibity.erp.repositories.query.RoleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.FileReader

@Service
class OrganizationService(
    val organizationJpaRepository: OrganizationJpaRepository,
    val typeJpaRepository: TypeJpaRepository,
    val typeListJpaRepository: TypeListJpaRepository,
    val variableListJpaRepository: VariableListJpaRepository,
    val typeService: TypeService,
    val userJpaRepository: UserJpaRepository,
    val userService: UserService,
    val roleService: RoleService,
    val roleRepository: RoleRepository,
    val variableService: VariableService,
    val functionService: FunctionService
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createOrganization(jsonParams: JsonObject): Organization {
    val organizationName: String = jsonParams.get("organization").asString
    val organization: Organization = try {
      organizationJpaRepository.save(Organization(name = organizationName))
    } catch (exception: Exception) {
      throw CustomJsonException("{'organization': 'Organization $organizationName is already present'}")
    }
    // Re-evaluate below when running this in production
    try {
      createKeycloakGroup(jsonParams = JsonObject().apply { addProperty("orgId", organization.id) })
    } catch (exception: Exception) {
      println("Keycloak Group already exists")
    }
    createDefaultRoles(organization = organization)
    createPrimitiveTypes(organization = organization)
    val superuserId: String = try {
      getKeycloakId(KeycloakConstants.SUPERUSER_USERNAME)
    } catch (exception: Exception) {
      createKeycloakUser(jsonParams = jsonParams.apply { addProperty("email", KeycloakConstants.SUPERUSER_USERNAME) })
    }
    var superuser = User(organization = organization, username = superuserId,
        active = true,
        email = KeycloakConstants.SUPERUSER_USERNAME,
        firstName = "System",
        lastName = "Administrator"
    )
    superuser = userJpaRepository.save(superuser)
    val role: Role = roleRepository.findRole(organizationId = organization.id, name = RoleConstants.ADMIN)
        ?: throw CustomJsonException("{roleName: 'Role could not be determined'}")
    superuser.userRoles.add(UserRole(id = UserRoleId(user = superuser, role = role)))
    superuser = userJpaRepository.save(superuser)
    createCustomTypes(organization = organization, username = superuser.username)
    superuser.details = try {
      variableService.createVariable(jsonParams = JsonObject().apply {
        addProperty("orgId", organization.id)
        addProperty("username", superuserId)
        addProperty("typeName", "User")
        addProperty("variableName", superuserId)
        add("values", JsonObject())
      }).first
    } catch (exception: CustomJsonException) {
      throw CustomJsonException("{details: ${exception.message}}")
    }
    try {
      userJpaRepository.save(superuser)
    } catch (exception: Exception) {
      throw CustomJsonException("{username: 'User could not be created'}")
    }
    createCustomFunctions(organization = organization, username = superuser.username)
    userService.createUser(JsonObject().apply {
      addProperty("orgId", organization.id)
      addProperty("username", superuserId)
      addProperty("email", jsonParams.get("admin").asString)
      addProperty("active", true)
      addProperty("firstName", jsonParams.get("firstName").asString)
      addProperty("lastName", jsonParams.get("lastName").asString)
      addProperty("password", jsonParams.get("password").asString)
      add("roles", JsonArray().apply { add(RoleConstants.ADMIN) })
      add("details", jsonParams.get("details").asJsonObject)
    })
    return organization
  }

  fun createDefaultRoles(organization: Organization) {
    val jsonRoles: JsonArray = gson.fromJson(FileReader("src/main/resources/roles/index.json"), JsonArray::class.java)
    for (jsonRole in jsonRoles) {
      roleService.createRole(JsonObject().apply {
        addProperty("orgId", organization.id)
        addProperty("roleName", jsonRole.asString)
      })
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createPrimitiveTypes(organization: Organization): Type {
    val anyType: Type = typeJpaRepository.save(Type(organization = organization, superTypeName = GLOBAL_TYPE, name = GLOBAL_TYPE, primitiveType = true, multiplicity = 0))
    try {
      for (primitiveType in primitiveTypes)
        typeJpaRepository.save(Type(organization = organization, superTypeName = GLOBAL_TYPE, name = primitiveType, primitiveType = true, multiplicity = 0))
    } catch (exception: Exception) {
      throw CustomJsonException("{'organization': 'Organization ${organization.id} could not be created'}")
    }
    return anyType
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createCustomTypes(organization: Organization, username: String) {
    val customTypeFilenames: JsonArray = gson.fromJson(FileReader("src/main/resources/types/index.json"), JsonArray::class.java)
    for (filename in customTypeFilenames) {
      val types: JsonArray = gson.fromJson(FileReader("src/main/resources/types/${filename.asString}.json"), JsonArray::class.java)
      for (json in types) {
        val typeRequest = json.asJsonObject.apply {
          addProperty("orgId", organization.id)
        }
        typeService.createType(jsonParams = getJsonParams(typeRequest.toString(), getExpectedParams("type", "createType")).apply {
          addProperty("username", username)
        })
      }
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createCustomFunctions(organization: Organization, username: String) {
    val customTypeFilenames: JsonArray = gson.fromJson(FileReader("src/main/resources/functions/index.json"), JsonArray::class.java)
    for (filename in customTypeFilenames) {
      val functions: JsonArray = gson.fromJson(FileReader("src/main/resources/functions/${filename.asString}.json"), JsonArray::class.java)
      for (json in functions) {
        val functionRequest = json.asJsonObject.apply {
          addProperty("orgId", organization.id)
        }
        functionService.createFunction(jsonParams = getJsonParams(functionRequest.toString(), getExpectedParams("function", "createFunction")).apply {
          addProperty("username", username)
        })
      }
    }
  }
}
