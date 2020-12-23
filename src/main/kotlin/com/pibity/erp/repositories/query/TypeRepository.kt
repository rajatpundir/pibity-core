/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.query

import com.pibity.erp.commons.constants.GLOBAL_TYPE
import com.pibity.erp.entities.Type
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Repository
class TypeRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findByOrganization(organizationId: Long): Set<Type> {
    val hql = "SELECT t FROM Type t WHERE t.organization.id = :organizationId"
    return entityManager.createQuery(hql, Type::class.java).apply {
      setParameter("organizationId", organizationId)
    }.resultList.toSet()
  }

  @Transactional(readOnly = true)
  fun findGlobalTypes(organizationId: Long, superTypeName: String = GLOBAL_TYPE): Set<Type> {
    val hql = "SELECT t FROM Type t WHERE t.organization.id = :organizationId AND t.superTypeName = :superTypeName"
    return entityManager.createQuery(hql, Type::class.java).apply {
      setParameter("organizationId", organizationId)
      setParameter("superTypeName", superTypeName)
    }.resultList.toSet()
  }

  @Transactional(readOnly = true)
  fun findType(organizationId: Long, superTypeName: String, name: String): Type? {
    val hql = "SELECT t FROM Type t WHERE t.organization.id = :organizationId AND t.superTypeName = :superTypeName AND t.name = :name"
    return try {
      entityManager.createQuery(hql, Type::class.java).apply {
        setParameter("organizationId", organizationId)
        setParameter("superTypeName", superTypeName)
        setParameter("name", name)
      }.singleResult
    } catch (exception: Exception) {
      null
    }
  }
}
