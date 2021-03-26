/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.function

import com.pibity.erp.entities.function.Function
import com.pibity.erp.entities.function.FunctionInput
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.LockModeType
import javax.persistence.TypedQuery

@Repository
class FunctionRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findFunction(orgId: Long, name: String): Function? {
    val hql = "SELECT f FROM Function f WHERE f.organization.id = :orgId AND f.name = :name"
    val query: TypedQuery<Function> = entityManager.createQuery(hql, Function::class.java).apply {
      setParameter("orgId", orgId)
      setParameter("name", name)
    }
    return query.singleResult
  }

  @Transactional(readOnly = true)
  fun findFunctions(orgId: Long): Set<Function> {
    val hql = "SELECT DISTINCT f FROM Function f WHERE f.organization.id = :orgId"
    val query: TypedQuery<Function> = entityManager.createQuery(hql, Function::class.java).apply {
      setParameter("orgId", orgId)
    }
    return query.resultList.toSet()
  }

  @Transactional(readOnly = true)
  fun findFunctionInput(orgId: Long, functionName: String, name: String): FunctionInput? {
    val hql = "SELECT DISTINCT fi FROM FunctionInput fi WHERE fi.function.organization.id = :orgId AND fi.function.name = :functionName AND fi.name = :name"
    val query: TypedQuery<FunctionInput> = entityManager.createQuery(hql, FunctionInput::class.java).apply {
      setParameter("orgId", orgId)
      setParameter("functionName", functionName)
      setParameter("name", name)
    }
    return query.singleResult
  }
}
