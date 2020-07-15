/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories

import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.Type
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional

interface TypeRepository : CrudRepository<Type, Long> {

  @Transactional(readOnly = true)
  @Query("SELECT t from Type t where t.id.organization = :organization")
  fun findByOrganization(organization: Organization): Set<Type>

  @Transactional(readOnly = true)
  @Query("SELECT t from Type t where t.id.organization = :organization and t.id.superTypeName = 'Any'")
  fun findGlobalTypes(organization: Organization): Set<Type>

  @Transactional(readOnly = true)
  @Query("SELECT t from Type t where t.id.organization = :organization and t.id.superTypeName = :superTypeName and t.id.name = :name")
  fun findType(organization: Organization, superTypeName: String, name: String): Type?
}
