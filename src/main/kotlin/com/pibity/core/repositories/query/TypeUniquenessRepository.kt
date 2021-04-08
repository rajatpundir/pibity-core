/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.repositories.query

import com.pibity.core.entities.uniqueness.TypeUniqueness
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Repository
class TypeUniquenessRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findAllTypeUniqueness(orgId: Long): Set<TypeUniqueness> {
    val hql = "SELECT tu FROM TypeUniqueness tu WHERE tu.type.organization.id = :orgId"
    return entityManager.createQuery(hql, TypeUniqueness::class.java).apply {
      setParameter("orgId", orgId)
    }.resultList.toSet()
  }

  @Transactional(readOnly = true)
  fun findTypeUniqueness(orgId: Long, typeName: String, name: String): TypeUniqueness? {
    val hql = "SELECT tu FROM TypeUniqueness tu WHERE tu.type.organization.id = :orgId AND tu.type.name = :typeName AND tu.name = :name"
    return try {
      entityManager.createQuery(hql, TypeUniqueness::class.java).apply {
        setParameter("orgId", orgId)
        setParameter("typeName", typeName)
        setParameter("name", name)
      }.singleResult
    } catch (exception: Exception) {
      null
    }
  }
}
