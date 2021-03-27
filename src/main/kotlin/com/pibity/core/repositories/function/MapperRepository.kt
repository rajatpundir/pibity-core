/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.repositories.function

import com.pibity.core.entities.function.Mapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.TypedQuery

@Repository
class MapperRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findMapper(orgId: Long, name: String): Mapper? {
    val hql = "SELECT m FROM Mapper m WHERE m.organization.id = :orgId AND m.name = :name"
    val query: TypedQuery<Mapper> = entityManager.createQuery(hql, Mapper::class.java).apply {
//      lockMode = LockModeType.OPTIMISTIC_FORCE_INCREMENT
      setParameter("orgId", orgId)
      setParameter("name", name)
    }
    return try {
      query.singleResult
    } catch (exception: Exception) {
      null
    }
  }

  @Transactional(readOnly = true)
  fun findMappers(orgId: Long): Set<Mapper> {
    val hql = "SELECT DISTINCT m FROM Mapper m WHERE m.organization.id = :orgId"
    val query: TypedQuery<Mapper> = entityManager.createQuery(hql, Mapper::class.java).apply {
//      lockMode = LockModeType.OPTIMISTIC_FORCE_INCREMENT
      setParameter("orgId", orgId)
    }
    return query.resultList.toSet()
  }
}
