/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.query

import com.pibity.erp.entities.uniqueness.TypeUniqueness
import com.pibity.erp.entities.uniqueness.VariableUniqueness
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Repository
class VariableUniquenessRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findVariableUniqueness(typeUniqueness: TypeUniqueness, level: Int, hash: String): VariableUniqueness? {
    val hql = "SELECT v FROM VariableUniqueness v WHERE v.typeUniqueness = :typeUniqueness AND v.level = :level AND v.hash = :hash"
    return try {
      entityManager.createQuery(hql, VariableUniqueness::class.java).apply {
        setParameter("typeUniqueness", typeUniqueness)
        setParameter("level", level)
        setParameter("hash", hash)
      }.singleResult
    } catch (exception: Exception) {
      null
    }
  }
}
