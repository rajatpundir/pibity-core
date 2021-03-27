/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.repositories.query

import com.pibity.core.entities.Type
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Repository
class TypeRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findTypes(orgId: Long): Set<Type> {
    val hql = "SELECT t FROM Type t WHERE t.organization.id = :orgId"
    return entityManager.createQuery(hql, Type::class.java).apply {
      setParameter("orgId", orgId)
    }.resultList.toSet()
  }

  @Transactional(readOnly = true)
  fun findType(orgId: Long, name: String): Type? {
    val hql = "SELECT t FROM Type t WHERE t.organization.id = :orgId AND t.name = :name"
    return try {
      entityManager.createQuery(hql, Type::class.java).apply {
        setParameter("orgId", orgId)
        setParameter("name", name)
      }.singleResult
    } catch (exception: Exception) {
      null
    }
  }
}
