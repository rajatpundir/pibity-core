/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonObject
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.Category
import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.Type
import com.pibity.erp.repositories.CategoryRepository
import com.pibity.erp.repositories.OrganizationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    val organizationRepository: OrganizationRepository,
    val categoryRepository: CategoryRepository
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createCategory(jsonParams: JsonObject): Category {
    val organizationName: String = jsonParams.get("organization").asString
    val parentId: Long = jsonParams.get("parentCategoryId").asLong
    val name: String = jsonParams.get("name").asString
    val nextLabel: String = jsonParams.get("nextLabel").asString
    val code: String = jsonParams.get("code").asString
    val organization: Organization = organizationRepository.findByName(organizationName)
        ?: throw CustomJsonException("{'organization': 'Organization could not be found'}")
    val parent: Category = categoryRepository.findByOrganizationAndId(organization = organization, id = parentId)
        ?: try {
          categoryRepository.findByOrganizationAndParent(organization = organization).single()
        } catch (exception: Exception) {
          throw CustomJsonException("{'parentId': 'Parent category could not be determined'}")
        }
    // Create new category with given parent
    val category = Category(organization = parent.organization, name = name, nextLabel = nextLabel, code = code, parent = parent)
    category.ancestors.add(parent)
    category.ancestors.addAll(parent.ancestors)
    return try {
      categoryRepository.save(category)
    } catch (exception: Exception) {
      throw CustomJsonException("{'name': 'Category could not be created'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun updateCategory(jsonParams: JsonObject): Category {
    val organizationName: String = jsonParams.get("organization").asString
    val categoryId: Long = jsonParams.get("categoryId").asLong
    val organization: Organization = organizationRepository.findByName(organizationName)
        ?: throw CustomJsonException("{'organization': 'Organization could not be found'}")
    val category = categoryRepository.findByOrganizationAndId(organization = organization, id = categoryId)
        ?: throw CustomJsonException("{'name': 'Category could not be found'}")
    if (category.parent != null && jsonParams.has("parentCategoryId?")) {
      val parentId: Long = jsonParams.get("parentCategoryId?").asLong
      val parent: Category = categoryRepository.findByOrganizationAndId(organization = organization, id = parentId)
          ?: throw CustomJsonException("{'parentId': 'Parent category could not be found'}")
      if (category.children.contains(parent))
        throw CustomJsonException("{'parentId': 'Parent category is not valid'}")
      else
        category.parent = parent
      category.ancestors.add(parent)
      category.ancestors.addAll(parent.ancestors)
    }
    if (jsonParams.has("name?"))
      category.name = jsonParams.get("name?").asString
    if (jsonParams.has("nextLabel?"))
      category.nextLabel = jsonParams.get("nextLabel?").asString
    if (jsonParams.has("code?"))
      category.code = jsonParams.get("code?").asString
    return try {
      categoryRepository.save(category)
    } catch (exception: Exception) {
      throw CustomJsonException("{'name': 'Category could not be updated'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun removeCategory(jsonParams: JsonObject): Category {
    val organizationName: String = jsonParams.get("organization").asString
    val categoryId: Long = jsonParams.get("categoryId").asLong
    val organization: Organization = organizationRepository.findByName(organizationName)
        ?: throw CustomJsonException("{'organization': 'Organization could not be found'}")
    val category = categoryRepository.findByOrganizationAndId(organization = organization, id = categoryId)
        ?: throw CustomJsonException("{'name': 'Category could not be found'}")
    if (category.parent == null)
      throw CustomJsonException("{'categoryId': 'Default category cannot be removed'}")
    try {
      categoryRepository.delete(category)
    } catch (exception: Exception) {
      throw CustomJsonException("{'name': 'Category could not be removed'}")
    }
    return category
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun listTypes(jsonParams: JsonObject): Set<Type> {
    val organizationName: String = jsonParams.get("organization").asString
    val categoryId: Long = jsonParams.get("categoryId").asLong
    val organization: Organization = organizationRepository.findByName(organizationName)
        ?: throw CustomJsonException("{'organization': 'Organization could not be found'}")
    val category = categoryRepository.findByOrganizationAndId(organization = organization, id = categoryId)
        ?: throw CustomJsonException("{'name': 'Category could not be found'}")
    return category.types
  }
}
