/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.*
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.*
import com.pibity.erp.entities.embeddables.TypeId
import com.pibity.erp.repositories.OrganizationRepository
import com.pibity.erp.repositories.TypeListRepository
import com.pibity.erp.repositories.TypeRepository
import com.pibity.erp.repositories.VariableListRepository
import com.pibity.erp.commons.utils.createKeycloakGroup
import com.pibity.erp.commons.utils.getExpectedParams
import com.pibity.erp.commons.utils.getJsonParams
import com.pibity.erp.commons.utils.gson

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.FileReader

@Service
class OrganizationService(
    val organizationRepository: OrganizationRepository,
    val typeRepository: TypeRepository,
    val typeListRepository: TypeListRepository,
    val variableListRepository: VariableListRepository,
    val typeService: TypeService,
    val userService: UserService,
    val roleService: RoleService
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createOrganization(jsonParams: JsonObject): Organization {
    val organizationName: String = jsonParams.get("organization").asString
    val organization: Organization = try {
      organizationRepository.save(Organization(id = organizationName))
    } catch (exception: Exception) {
      throw CustomJsonException("{'organization': 'Organization $organizationName is already present'}")
    }
    createKeycloakGroup(jsonParams = JsonObject().apply { addProperty("organization", organization.id) })
    createDefaultRoles(organization = organization)
    val superuser: User = userService.createUser(JsonObject().apply {
      addProperty("organization", organization.id)
      addProperty("email", KeycloakConstants.SUPERUSER_USERNAME)
      addProperty("active", true)
      addProperty("firstName", "System")
      addProperty("lastName", "Administrator")
      addProperty("password", KeycloakConstants.SUPERUSER_PASSWORD)
      addProperty("subGroupName", RoleConstants.ADMIN)
    })
    userService.updateUserRoles(JsonObject().apply {
      addProperty("organization", organization.id)
      addProperty("username", superuser.id.username)
      addProperty("roleName", RoleConstants.ADMIN)
      addProperty("operation", "add")
    })
    createPrimitiveTypes(organization = organization)
    try {
      val anyType = typeRepository.findType(organizationName = jsonParams.get("organization").asString, superTypeName = GLOBAL_TYPE, name = TypeConstants.TEXT)
          ?: throw CustomJsonException("{'organization': 'Organization ${organization.id} could not be created'}")
      val tl = typeListRepository.save(TypeList(type = anyType, min = 0, max = 0))
      val vl = variableListRepository.save(VariableList(listType = tl))
      organization.superList = vl
      organizationRepository.save(organization)
    } catch (exception: Exception) {
      throw CustomJsonException("{'organization': 'Organization $organizationName is already present'}")
    }
    createCustomTypes(organization = organization, username = superuser.id.username)
    val admin: User = userService.createUser(JsonObject().apply {
      addProperty("organization", organization.id)
      addProperty("email", jsonParams.get("admin").asString)
      addProperty("active", true)
      addProperty("firstName", jsonParams.get("firstName").asString)
      addProperty("lastName", jsonParams.get("lastName").asString)
      addProperty("password", jsonParams.get("password").asString)
      addProperty("subGroupName", RoleConstants.ADMIN)
    })
    userService.updateUserRoles(JsonObject().apply {
      addProperty("organization", organization.id)
      addProperty("username", admin.id.username)
      addProperty("roleName", RoleConstants.ADMIN)
      addProperty("operation", "add")
    })
    return organization
  }

  fun createDefaultRoles(organization: Organization) {
    val jsonRoles: JsonArray = gson.fromJson(FileReader("src/main/resources/roles/index.json"), JsonArray::class.java)
    for (jsonRole in jsonRoles) {
      roleService.createRole(JsonObject().apply {
        addProperty("organization", organization.id)
        addProperty("roleName", jsonRole.asString)
      })
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createPrimitiveTypes(organization: Organization) {
    try {
      for (primitiveType in primitiveTypes)
        typeRepository.save(Type(id = TypeId(organization = organization, superTypeName = GLOBAL_TYPE, name = primitiveType), displayName = primitiveType, primitiveType = true, multiplicity = 0))
    } catch (exception: Exception) {
      throw CustomJsonException("{'organization': 'Organization ${organization.id} could not be created'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createCustomTypes(organization: Organization, username: String) {
    val customTypeFilenames: JsonArray = gson.fromJson(FileReader("src/main/resources/types/index.json"), JsonArray::class.java)
    for (filename in customTypeFilenames) {
      val types: JsonArray = gson.fromJson(FileReader("src/main/resources/types/${filename.asString}.json"), JsonArray::class.java)
      for (json in types) {
        val typeRequest = json.asJsonObject.apply {
          addProperty("organization", organization.id)
        }
        typeService.createType(jsonParams = getJsonParams(typeRequest.toString(), getExpectedParams("type", "createType")).apply {
          addProperty("username", username)
        })
      }
    }
  }
}
