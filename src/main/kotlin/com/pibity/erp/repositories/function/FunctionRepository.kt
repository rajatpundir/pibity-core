/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.function

import com.pibity.erp.entities.function.Function
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.LockModeType
import javax.persistence.TypedQuery

@Repository
class FunctionRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findFunction(organizationId: Long, name: String): Function? {
    val hql = "SELECT f FROM Function f WHERE f.organization.id = :organizationId AND f.name = :name"
    val query: TypedQuery<Function> = entityManager.createQuery(hql, Function::class.java).apply {
      lockMode = LockModeType.OPTIMISTIC_FORCE_INCREMENT
      setParameter("organizationId", organizationId)
      setParameter("name", name)
    }
    return query.singleResult
  }
}
