/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.query

import com.pibity.erp.entities.Type
import com.pibity.erp.entities.Variable
import com.pibity.erp.entities.VariableList
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Repository
class VariableRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findByTypeAndName(superList: VariableList, type: Type, name: String): Variable? {
    val hql = "SELECT v FROM Variable v WHERE v.superList = :superList AND v.type = :type AND v.name = :name"
    return try {
      entityManager.createQuery(hql, Variable::class.java).apply {
        setParameter("superList", superList)
        setParameter("type", type)
        setParameter("name", name)
      }.singleResult
    } catch (exception: Exception) {
      null
    }
  }

  @Transactional(readOnly = true)
  fun findVariable(organizationId: Long, superTypeName: String, typeName: String, superList: Long, name: String = ""): Variable? {
    val hql = "SELECT v FROM Variable v WHERE v.type.organization.id = :organizationId AND v.type.superTypeName = :superTypeName AND v.type.name = :typeName AND v.superList.id = :superList AND v.name = :name"
    return try {
      entityManager.createQuery(hql, Variable::class.java).apply {
        setParameter("organizationId", organizationId)
        setParameter("superTypeName", superTypeName)
        setParameter("typeName", typeName)
        setParameter("superList", superList)
        setParameter("name", name)
      }.singleResult
    } catch (exception: Exception) {
      null
    }
  }
}
