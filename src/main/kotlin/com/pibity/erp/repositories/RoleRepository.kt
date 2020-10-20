/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories

import com.pibity.erp.entities.Role
import com.pibity.erp.entities.embeddables.RoleId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional

interface RoleRepository : CrudRepository<Role, RoleId> {

  @Transactional(readOnly = true)
  @Query("SELECT r FROM Role r WHERE r.id.organization.id = :organizationName AND r.id.name = :name")
  fun findRole(organizationName: String, name: String): Role?
}
