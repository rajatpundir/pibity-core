/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.repositories.query

import com.pibity.core.entities.accumulator.TypeAccumulator
import com.pibity.core.entities.accumulator.VariableAccumulator
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Repository
class VariableAccumulatorRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findVariableAccumulator(typeAccumulator: TypeAccumulator, level: Int, hash: String): VariableAccumulator? {
    val hql = "SELECT v FROM VariableAccumulator v WHERE v.typeAccumulator = :typeAccumulator AND v.level = :level AND v.hash = :hash"
    return try {
      entityManager.createQuery(hql, VariableAccumulator::class.java).apply {
        setParameter("typeAccumulator", typeAccumulator)
        setParameter("level", level)
        setParameter("hash", hash)
      }.singleResult
    } catch (exception: Exception) {
      null
    }
  }
}
