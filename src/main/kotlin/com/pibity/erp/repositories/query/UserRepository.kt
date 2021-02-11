/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.query

import com.pibity.erp.entities.User
import com.pibity.erp.entities.permission.FunctionPermission
import com.pibity.erp.entities.permission.TypePermission
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Repository
class UserRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findUser(organizationId: Long, username: String): User? {
    val hql = "SELECT u FROM User u WHERE u.organization.id = :organizationId AND u.username = :username"
    return try {
      entityManager.createQuery(hql, User::class.java).apply {
        setParameter("organizationId", organizationId)
        setParameter("username", username)
      }.singleResult
    } catch (exception: Exception) {
      null
    }
  }

  @Transactional(readOnly = true)
  fun getUserTypePermissions(organizationId: Long, typeName: String, username: String): Set<TypePermission> {
    val hql = "SELECT DISTINCT p0 FROM TypePermission p0 WHERE EXISTS (SELECT p FROM TypePermission p JOIN p.permissionRoles pr JOIN pr.id.role r JOIN r.roleUsers ru JOIN ru.id.user u WHERE p.type.organization.id = :organizationId AND p.type.name = :typeName AND u.organization.id = :organizationId AND u.username = :username AND p = p0) OR EXISTS (SELECT p FROM TypePermission p JOIN p.permissionRoles pr JOIN pr.id.role r JOIN r.roleGroups rg JOIN rg.id.group g JOIN g.groupUsers gu JOIN gu.id.user u WHERE p.type.organization.id = :organizationId AND p.type.name = :typeName AND u.organization.id = :organizationId AND u.username = :username AND p = p0)"
    return entityManager.createQuery(hql, TypePermission::class.java).apply {
      setParameter("organizationId", organizationId)
      setParameter("typeName", typeName)
      setParameter("username", username)
    }.resultList.toSet()
  }

  @Transactional(readOnly = true)
  fun getUserFunctionPermissions(organizationId: Long, functionName: String, username: String): Set<FunctionPermission> {
    val hql = "SELECT DISTINCT p0 FROM FunctionPermission p0 WHERE EXISTS (SELECT p FROM FunctionPermission p JOIN p.permissionRoles pr JOIN pr.id.role r JOIN r.roleUsers ru JOIN ru.id.user u WHERE p.function.organization.id = :organizationId AND p.function.name = :functionName AND u.organization.id = :organizationId AND u.username = :username AND p = p0) OR EXISTS (SELECT p FROM FunctionPermission p JOIN p.permissionRoles pr JOIN pr.id.role r JOIN r.roleGroups rg JOIN rg.id.group g JOIN g.groupUsers gu JOIN gu.id.user u WHERE p.function.organization.id = :organizationId AND p.function.name = :functionName AND u.organization.id = :organizationId AND u.username = :username AND p = p0)"
    return entityManager.createQuery(hql, FunctionPermission::class.java).apply {
      setParameter("organizationId", organizationId)
      setParameter("functionName", functionName)
      setParameter("username", username)
    }.resultList.toSet()
  }
}
