/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.query

import com.pibity.erp.entities.Variable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Repository
class ValueRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun queryVariables(hql: String, injectedValues: MutableMap<String, Any>, limit: Int, offset: Int): List<Variable> {
    return entityManager.createQuery(hql, Variable::class.java).apply {
      for ((k, v) in injectedValues)
        setParameter(k, v)
      firstResult = offset
      maxResults = limit
    }.resultList
  }
}
