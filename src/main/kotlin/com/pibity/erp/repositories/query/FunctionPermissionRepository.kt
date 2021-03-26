/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.query

import com.pibity.erp.entities.permission.FunctionPermission
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.LockModeType
import javax.persistence.TypedQuery

@Repository
class FunctionPermissionRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findFunctionPermission(orgId: Long, functionName: String, name: String): FunctionPermission? {
    val hql = "SELECT p FROM FunctionPermission p WHERE p.function.organization.id = :orgId AND p.function.name = :functionName AND p.name = :name"
    return try {
      entityManager.createQuery(hql, FunctionPermission::class.java).apply {
        setParameter("orgId", orgId)
        setParameter("functionName", functionName)
        setParameter("name", name)
      }.singleResult
    } catch (exception: Exception) {
      null
    }
  }
}
