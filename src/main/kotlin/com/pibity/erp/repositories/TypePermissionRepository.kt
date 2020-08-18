/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories

import com.pibity.erp.entities.TypePermission
import com.pibity.erp.entities.embeddables.TypePermissionId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional

interface TypePermissionRepository : CrudRepository<TypePermission, TypePermissionId> {

  @Transactional(readOnly = true)
  @Query("SELECT p FROM TypePermission p WHERE p.id.type.id.organization.id = :organizationName AND p.id.type.id.superTypeName = :superTypeName AND p.id.type.id.name = :typeName AND p.id.name = :name")
  fun findTypePermission(organizationName: String, superTypeName: String, typeName: String, name: String): TypePermission?
}
