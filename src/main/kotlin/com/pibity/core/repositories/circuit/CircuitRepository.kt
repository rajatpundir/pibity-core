/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.repositories.circuit

import com.pibity.core.entities.circuit.Circuit
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.TypedQuery

@Repository
class CircuitRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findCircuit(orgId: Long, name: String): Circuit? {
    val hql = "SELECT c FROM Circuit c WHERE c.organization.id = :orgId AND c.name = :name"
    val query: TypedQuery<Circuit> = entityManager.createQuery(hql, Circuit::class.java).apply {
      setParameter("orgId", orgId)
      setParameter("name", name)
    }
    return query.singleResult
  }

  @Transactional(readOnly = true)
  fun findCircuits(orgId: Long): Set<Circuit> {
    val hql = "SELECT DISTINCT c FROM Circuit c WHERE c.organization.id = :orgId"
    val query: TypedQuery<Circuit> = entityManager.createQuery(hql, Circuit::class.java).apply {
      setParameter("orgId", orgId)
    }
    return query.resultList.toSet()
  }
}
