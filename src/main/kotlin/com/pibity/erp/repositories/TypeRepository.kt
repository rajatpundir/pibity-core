/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories

import com.pibity.erp.entities.Type
import com.pibity.erp.entities.embeddables.TypeId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional

interface TypeRepository : CrudRepository<Type, TypeId> {

  @Transactional(readOnly = true)
  @Query("SELECT t FROM Type t WHERE t.id.organization.id = :organizationName")
  fun findByOrganization(organizationName: String): Set<Type>

  @Transactional(readOnly = true)
  @Query("SELECT t FROM Type t WHERE t.id.organization.id = :organizationName AND t.id.superTypeName = 'Any'")
  fun findGlobalTypes(organizationName: String): Set<Type>

  @Transactional(readOnly = true)
  @Query("SELECT t FROM Type t WHERE t.id.organization.id = :organizationName AND t.id.superTypeName = :superTypeName AND t.id.name = :name")
  fun findType(organizationName: String, superTypeName: String, name: String): Type?
}
