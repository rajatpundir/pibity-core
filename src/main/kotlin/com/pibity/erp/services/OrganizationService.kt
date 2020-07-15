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
import com.pibity.erp.commons.constants.primitiveTypes
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.getExpectedParams
import com.pibity.erp.commons.getJsonParams
import com.pibity.erp.commons.gson
import com.pibity.erp.entities.Category
import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.embeddables.TypeId
import com.pibity.erp.repositories.CategoryRepository
import com.pibity.erp.repositories.OrganizationRepository
import com.pibity.erp.repositories.TypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.FileReader

@Service
class OrganizationService(
    val organizationRepository: OrganizationRepository,
    val categoryRepository: CategoryRepository,
    val typeRepository: TypeRepository,
    val typeService: TypeService
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createOrganization(jsonParams: JsonObject): Organization {
    val organizationName: String = jsonParams.get("organization").asString
    val organization: Organization = try {
      organizationRepository.save(Organization(name = organizationName))
    } catch (exception: Exception) {
      throw CustomJsonException("{'organization': 'Organization $organizationName is already present'}")
    }
    try {
      categoryRepository.save(Category(organization = organization, name = "default"))
    } catch (exception: Exception) {
      throw CustomJsonException("{'organization': 'Default category for organization $organizationName could not be created'}")
    }
    createPrimitiveTypes(organization = organization)
    createCustomTypes(organization = organization)
    return organization
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createPrimitiveTypes(organization: Organization) {
    try {
      for (primitiveType in primitiveTypes)
        typeRepository.save(Type(id = TypeId(organization = organization, superTypeName = "Any", name = primitiveType), displayName = primitiveType, primitiveType = true))
    } catch (exception: Exception) {
      throw CustomJsonException("{'organization': 'Organization ${organization.name} could not be created'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createCustomTypes(organization: Organization) {
    val customTypesList = listOf("Country", "Currency", "Customer", "Supplier", "Product", "PurchaseOrder", "SaleCreditNote", "PurchaseCreditNote")
    for (filename in customTypesList) {
      val types: JsonArray = gson.fromJson(FileReader("src/main/resources/types/$filename.json"), JsonArray::class.java)
      for (json in types) {
        val typeRequest = json.asJsonObject.apply { addProperty("organization", organization.name) }
        typeService.createType(jsonParams = getJsonParams(typeRequest.toString(), getExpectedParams("type", "createType")))
      }
    }
  }

  fun listAllCategories(jsonParams: JsonObject): Set<Category> {
    val organizationName: String = jsonParams.get("organization").asString
    val organization: Organization = organizationRepository.findByName(organizationName)
        ?: throw CustomJsonException("{'organization': 'Organization could not be found'}")
    return categoryRepository.findByOrganization(organization)
  }
}
