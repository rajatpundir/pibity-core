/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.repositories.query

import com.pibity.core.entities.accumulator.VariableAccumulator
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Repository
class ValueAccumulatorRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun queryAccumulators(hql: String, injectedValues: MutableMap<String, Any>, limit: Int, offset: Int): List<VariableAccumulator> {
    return entityManager.createQuery(hql, VariableAccumulator::class.java).apply {
      for ((k, v) in injectedValues)
        setParameter(k, v)
      firstResult = offset
      maxResults = limit
    }.resultList
  }
}
