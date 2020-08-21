/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories

import com.pibity.erp.entities.Group
import com.pibity.erp.entities.embeddables.GroupId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional

interface GroupRepository : CrudRepository<Group, GroupId> {

  @Transactional(readOnly = true)
  @Query("SELECT g FROM Group g WHERE g.id.organization.id = :organizationName AND g.id.name = :name")
  fun findGroup(organizationName: String, name: String): Group?
}
