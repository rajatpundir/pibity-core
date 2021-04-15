/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.repositories.query

import com.pibity.core.entities.Space
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Repository
class SpaceRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findDefaultSpaces(orgId: Long): Set<Space> {
    val hql = "SELECT s FROM Space s WHERE s.organization.id = :orgId AND s.default = true"
    return entityManager.createQuery(hql, Space::class.java).apply {
      setParameter("orgId", orgId)
    }.resultList.toSet()
  }

  @Transactional(readOnly = true)
  fun findSpace(orgId: Long, name: String): Space? {
    val hql = "SELECT s FROM Space s WHERE s.organization.id = :orgId AND s.name = :name"
    return try {
      entityManager.createQuery(hql, Space::class.java).apply {
        setParameter("orgId", orgId)
        setParameter("name", name)
      }.singleResult
    } catch (exception: Exception) {
      null
    }
  }
}
