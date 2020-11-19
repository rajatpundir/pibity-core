/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.function

import com.pibity.erp.entities.function.FunctionInput
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.LockModeType
import javax.persistence.TypedQuery

@Repository
class FunctionInputRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun getFunctionInputs(organizationId: Long, functionName: String): Set<FunctionInput> {
    val hql = "SELECT fi FROM FunctionInput fi WHERE fi.function.organization.id = :organizationId AND fi.function.name = :functionName"
    val query: TypedQuery<FunctionInput> = entityManager.createQuery(hql, FunctionInput::class.java).apply {
      lockMode = LockModeType.OPTIMISTIC_FORCE_INCREMENT
      setParameter("organizationId", organizationId)
      setParameter("functionName", functionName)
    }
    return query.resultList.toSet()
  }
}
