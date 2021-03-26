/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.query

import com.pibity.erp.entities.permission.TypePermission
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.LockModeType
import javax.persistence.TypedQuery

@Repository
class TypePermissionRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findTypePermission(orgId: Long, typeName: String, name: String): TypePermission? {
    val hql = "SELECT p FROM TypePermission p WHERE p.type.organization.id = :orgId AND p.type.name = :typeName AND p.name = :name"
    return try {
      entityManager.createQuery(hql, TypePermission::class.java).apply {
        setParameter("orgId", orgId)
        setParameter("typeName", typeName)
        setParameter("name", name)
      }.singleResult
    } catch (exception: Exception) {
      null
    }
  }
}
