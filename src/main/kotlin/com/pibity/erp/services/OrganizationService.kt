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
import com.pibity.erp.commons.constants.GLOBAL_TYPE
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.constants.primitiveTypes
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.getExpectedParams
import com.pibity.erp.commons.getJsonParams
import com.pibity.erp.commons.gson
import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.TypeList
import com.pibity.erp.entities.VariableList
import com.pibity.erp.entities.embeddables.TypeId
import com.pibity.erp.repositories.OrganizationRepository
import com.pibity.erp.repositories.TypeListRepository
import com.pibity.erp.repositories.TypeRepository
import com.pibity.erp.repositories.VariableListRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.FileReader

@Service
class OrganizationService(
    val organizationRepository: OrganizationRepository,
    val typeRepository: TypeRepository,
    val typeListRepository: TypeListRepository,
    val variableListRepository: VariableListRepository,
    val typeService: TypeService
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createOrganization(jsonParams: JsonObject): Organization {
    val organizationName: String = jsonParams.get("organization").asString
    val organization: Organization = try {
      organizationRepository.save(Organization(id = organizationName))
    } catch (exception: Exception) {
      throw CustomJsonException("{'organization': 'Organization $organizationName is already present'}")
    }
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
    createCustomTypes(organization = organization)
    return organization
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
  fun createCustomTypes(organization: Organization) {
    val customTypeFilenames: JsonArray = gson.fromJson(FileReader("src/main/resources/types/index.json"), JsonArray::class.java)
    for (filename in customTypeFilenames) {
      val types: JsonArray = gson.fromJson(FileReader("src/main/resources/types/${filename.asString}.json"), JsonArray::class.java)
      for (json in types) {
        val typeRequest = json.asJsonObject.apply { addProperty("organization", organization.id) }
        typeService.createType(jsonParams = getJsonParams(typeRequest.toString(), getExpectedParams("type", "createType")))
      }
    }
  }
}
