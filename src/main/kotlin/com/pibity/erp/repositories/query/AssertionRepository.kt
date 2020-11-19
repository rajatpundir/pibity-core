/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.query

import com.pibity.erp.commons.constants.GLOBAL_TYPE
import com.pibity.erp.entities.TypeAssertion
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Repository
class AssertionRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findAssertion(organizationId: Long, superTypeName: String = GLOBAL_TYPE, typeName: String, name: String): TypeAssertion? {
    val hql = "SELECT a from Assertion a WHERE a.type.organization.id = :organizationId AND a.type.superTypeName = :superTypeName AND a.type.name = :typeName AND a.name = :name"
    return try {
      entityManager.createQuery(hql, TypeAssertion::class.java).apply {
        setParameter("organizationId", organizationId)
        setParameter("superTypeName", superTypeName)
        setParameter("typeName", typeName)
        setParameter("name", name)
      }.singleResult
    } catch (exception: Exception) {
      null
    }
  }
}
