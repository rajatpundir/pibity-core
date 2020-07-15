/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories

import com.pibity.erp.entities.Category
import com.pibity.erp.entities.Organization
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional

interface CategoryRepository : CrudRepository<Category, Long> {

  @Transactional(readOnly = true)
  fun findByOrganizationAndId(organization: Organization, id: Long): Category?

  @Transactional(readOnly = true)
  fun findByOrganization(organization: Organization): Set<Category>

  @Transactional(readOnly = true)
  fun findByOrganizationAndParent(organization: Organization, parent: Category? = null): Set<Category>
}
