/*
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.repositories.query

import com.pibity.core.entities.User
import com.pibity.core.entities.permission.FunctionPermission
import com.pibity.core.entities.permission.TypePermission
import javax.persistence.EntityManager
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class UserRepository(val entityManager: EntityManager) {

  @Transactional(readOnly = true)
  fun findUser(orgId: Long, username: String): User? {
    val hql = "SELECT u FROM User u WHERE u.organization.id = :orgId AND u.username = :username"
    return try {
      entityManager
          .createQuery(hql, User::class.java)
          .apply {
            setParameter("orgId", orgId)
            setParameter("username", username)
          }
          .singleResult
    } catch (exception: Exception) {
      null
    }
  }

  @Transactional(readOnly = true)
  fun getUserTypePermissions(
      orgId: Long,
      username: String,
      subspaceName: String,
      spaceName: String,
      permissionType: String,
      typeName: String
  ): Set<TypePermission> {
    val hql = "SELECT DISTINCT p0 FROM TypePermission p0 \n" +
        "WHERE EXISTS (SELECT p FROM TypePermission p \n" +
        "JOIN p.permissionSpaces ps \n" +
        "JOIN ps.id.space s \n" +
        "JOIN s.spaceSubspaces ssu \n" +
        "JOIN ssu.id.subspace su \n" +
        "JOIN su.subspaceUsers suu \n" +
        "JOIN suu.id.user u \n" +
        "WHERE u.organization.id = :orgId\n" +
        "AND u.username = :username\n" +
        "AND u.active = true\n" +
        "AND su.name = :subspaceName\n" +
        "AND su.space.organization.id = :orgId\n" +
        "AND su.space.name = :spaceName\n" +
        "AND su.space.active = true\n" +
        "AND su.name = :subspaceName\n" +
        "AND p.type.organization.id = :orgId\n" +
        "AND p.type.name = :typeName\n" +
        "AND p.permissionType = :permissionType \n" +
        "AND p = p0)\n" +
        "OR EXISTS (SELECT p FROM TypePermission p \n" +
        "JOIN p.permissionSpaces ps \n" +
        "JOIN ps.id.space s \n" +
        "JOIN s.spaceSubspaces ssu \n" +
        "JOIN ssu.id.subspace su \n" +
        "JOIN su.subspaceGroups sug \n" +
        "JOIN sug.id.group g \n" +
        "JOIN g.groupUsers gu \n" +
        "JOIN gu.id.user u \n" +
        "WHERE u.organization.id = :orgId\n" +
        "AND u.username = :username\n" +
        "AND u.active = true\n" +
        "AND su.name = :subspaceName\n" +
        "AND su.space.organization.id = :orgId\n" +
        "AND su.space.name = :spaceName\n" +
        "AND su.space.active = true\n" +
        "AND su.name = :subspaceName\n" +
        "AND p.type.organization.id = :orgId\n" +
        "AND p.type.name = :typeName\n" +
        "AND p.permissionType = :permissionType \n" +
        "AND p = p0)"
    return entityManager
        .createQuery(hql, TypePermission::class.java)
        .apply {
          setParameter("orgId", orgId)
          setParameter("username", username)
          setParameter("subspaceName", subspaceName)
          setParameter("spaceName", spaceName)
          setParameter("permissionType", permissionType)
          setParameter("typeName", typeName)
        }
        .resultList
        .toSet()
  }

  @Transactional(readOnly = true)
  fun getUserFunctionPermissions(
    orgId: Long,
    username: String,
    subspaceName: String,
    spaceName: String,
    functionName: String
  ): Set<FunctionPermission> {
    val hql = "SELECT DISTINCT p0 FROM FunctionPermission p0 \n" +
        "WHERE EXISTS (SELECT p FROM FunctionPermission p \n" +
        "JOIN p.permissionSpaces ps \n" +
        "JOIN ps.id.space s \n" +
        "JOIN s.spaceSubspaces ssu \n" +
        "JOIN ssu.id.subspace su \n" +
        "JOIN su.subspaceUsers suu \n" +
        "JOIN suu.id.user u \n" +
        "WHERE u.organization.id = :orgId\n" +
        "AND u.username = :username\n" +
        "AND u.active = true\n" +
        "AND su.name = :subspaceName\n" +
        "AND su.space.organization.id = :orgId\n" +
        "AND su.space.name = :spaceName\n" +
        "AND su.space.active = true\n" +
        "AND su.name = :subspaceName\n" +
        "AND p.function.organization.id = :orgId\n" +
        "AND p.function.name = :functionName\n" +
        "AND p = p0)\n" +
        "OR EXISTS (SELECT p FROM FunctionPermission p \n" +
        "JOIN p.permissionSpaces ps \n" +
        "JOIN ps.id.space s \n" +
        "JOIN s.spaceSubspaces ssu \n" +
        "JOIN ssu.id.subspace su \n" +
        "JOIN su.subspaceGroups sug \n" +
        "JOIN sug.id.group g \n" +
        "JOIN g.groupUsers gu \n" +
        "JOIN gu.id.user u \n" +
        "WHERE u.organization.id = :orgId\n" +
        "AND u.username = :username\n" +
        "AND u.active = true\n" +
        "AND su.name = :subspaceName\n" +
        "AND su.space.organization.id = :orgId\n" +
        "AND su.space.name = :spaceName\n" +
        "AND su.space.active = true\n" +
        "AND su.name = :subspaceName\n" +
        "AND p.function.organization.id = :orgId\n" +
        "AND p.function.name = :functionName\n" +
        "AND p = p0)"
    return entityManager
      .createQuery(hql, FunctionPermission::class.java)
      .apply {
        setParameter("orgId", orgId)
        setParameter("username", username)
        setParameter("subspaceName", subspaceName)
        setParameter("spaceName", spaceName)
        setParameter("functionName", functionName)
      }
      .resultList
      .toSet()
  }
}
