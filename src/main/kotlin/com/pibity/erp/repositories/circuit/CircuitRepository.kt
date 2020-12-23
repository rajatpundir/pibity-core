/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.circuit

import com.pibity.erp.entities.circuit.Circuit
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.LockModeType
import javax.persistence.TypedQuery

@Repository
class CircuitRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findCircuit(organizationId: Long, name: String): Circuit? {
    val hql = "SELECT c FROM Circuit c WHERE c.organization.id = :organizationId AND c.name = :name"
    val query: TypedQuery<Circuit> = entityManager.createQuery(hql, Circuit::class.java).apply {
      lockMode = LockModeType.OPTIMISTIC_FORCE_INCREMENT
      setParameter("organizationId", organizationId)
      setParameter("name", name)
    }
    return query.singleResult
  }

  @Transactional(readOnly = true)
  fun findCircuits(organizationId: Long): Set<Circuit> {
    val hql = "SELECT DISTINCT c FROM Circuit c WHERE c.organization.id = :organizationId"
    val query: TypedQuery<Circuit> = entityManager.createQuery(hql, Circuit::class.java).apply {
      lockMode = LockModeType.OPTIMISTIC_FORCE_INCREMENT
      setParameter("organizationId", organizationId)
    }
    return query.resultList.toSet()
  }
}
