/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories

import com.pibity.erp.entities.permission.TypePermission
import com.pibity.erp.entities.User
import com.pibity.erp.entities.embeddables.UserId
import com.pibity.erp.entities.permission.FunctionPermission
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional

interface UserRepository : CrudRepository<User, UserId> {

  @Transactional(readOnly = true)
  @Query("SELECT u FROM User u WHERE u.id.organization.id = :organizationName AND u.id.username = :username")
  fun findUser(organizationName: String, username: String): User?

  @Transactional(readOnly = true)
  @Query("SELECT DISTINCT p0 FROM TypePermission p0 WHERE EXISTS (SELECT p FROM TypePermission p JOIN p.permissionRoles pr JOIN pr.id.role r JOIN r.roleUsers ru JOIN ru.id.user u WHERE p.id.type.id.organization.id = :organizationName AND p.id.type.id.superTypeName = :superTypeName AND p.id.type.id.name = :typeName AND u.id.organization.id = :organizationName AND u.id.username = :username AND p = p0) OR EXISTS (SELECT p FROM TypePermission p JOIN p.permissionRoles pr JOIN pr.id.role r JOIN r.roleGroups rg JOIN rg.id.group g JOIN g.groupUsers gu JOIN gu.id.user u WHERE p.id.type.id.organization.id = :organizationName AND p.id.type.id.superTypeName = :superTypeName AND p.id.type.id.name = :typeName AND u.id.organization.id = :organizationName AND u.id.username = :username AND p = p0)")
  fun getUserTypePermissions(organizationName: String, superTypeName: String, typeName: String, username: String): Set<TypePermission>

  // TODO. This is not yet working. However, it should work same as above query.
  @Transactional(readOnly = true)
  @Query("SELECT DISTINCT p0 FROM FunctionPermission p0 WHERE EXISTS (SELECT p FROM FunctionPermission p JOIN p.permissionRoles pr JOIN pr.id.role r JOIN r.roleUsers ru JOIN ru.id.user u WHERE p.id.function.id.organization.id = :organizationName AND p.id.function.id.name = :functionName AND u.id.organization.id = :organizationName AND u.id.username = :username AND p = p0) OR EXISTS (SELECT p FROM FunctionPermission p JOIN p.permissionRoles pr JOIN pr.id.role r JOIN r.roleGroups rg JOIN rg.id.group g JOIN g.groupUsers gu JOIN gu.id.user u WHERE p.id.function.id.organization.id = :organizationName AND p.id.function.id.name = :functionName AND u.id.organization.id = :organizationName AND u.id.username = :username AND p = p0)")
  fun getUserFunctionPermissions(organizationName: String, functionName: String, username: String): Set<FunctionPermission>
}
