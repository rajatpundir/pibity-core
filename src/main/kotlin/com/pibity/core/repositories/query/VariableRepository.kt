/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.repositories.query

import com.pibity.core.entities.Type
import com.pibity.core.entities.Variable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Repository
class VariableRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findByTypeAndName(type: Type, name: String): Variable? {
    val hql = "SELECT v FROM Variable v WHERE v.type = :type AND v.name = :name"
    return try {
      entityManager.createQuery(hql, Variable::class.java).apply {
        setParameter("type", type)
        setParameter("name", name)
      }.singleResult
    } catch (exception: Exception) {
      null
    }
  }

  @Transactional(readOnly = true)
  fun findVariable(orgId: Long, typeName: String, name: String = ""): Variable? {
    val hql =
      "SELECT v FROM Variable v WHERE v.type.organization.id = :orgId AND v.type.name = :typeName AND v.name = :name"
    return try {
      entityManager.createQuery(hql, Variable::class.java).apply {
        setParameter("orgId", orgId)
        setParameter("typeName", typeName)
        setParameter("name", name)
      }.singleResult
    } catch (exception: Exception) {
      null
    }
  }
}
