/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.repositories.query

import com.pibity.core.entities.Subspace
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Repository
class SubspaceRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findSubspace(orgId: Long, spaceName: String, name: String): Subspace? {
    val hql = "SELECT s FROM Subspace s WHERE s.space.organization.id = :orgId AND s.space.name = :spaceName AND s.name = :name"
    return try {
      entityManager.createQuery(hql, Subspace::class.java).apply {
        setParameter("orgId", orgId)
        setParameter("spaceName", spaceName)
        setParameter("name", name)
      }.singleResult
    } catch (exception: Exception) {
      null
    }
  }
}
