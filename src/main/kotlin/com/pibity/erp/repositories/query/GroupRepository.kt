/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.query

import com.pibity.erp.entities.Group
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.LockModeType
import javax.persistence.TypedQuery

@Repository
class GroupRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findGroup(organizationId: Long, name: String): Group? {
    val hql = "SELECT g FROM Group g WHERE g.organization.id = :organizationId AND g.name = :name"
    return try {
      entityManager.createQuery(hql, Group::class.java).apply {
        setParameter("organizationId", organizationId)
        setParameter("name", name)
      }.singleResult
    } catch (exception: Exception) {
      null
    }
  }
}
