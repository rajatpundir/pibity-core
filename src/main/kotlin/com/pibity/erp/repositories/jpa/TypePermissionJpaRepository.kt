/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.jpa

import com.pibity.erp.entities.permission.TypePermission
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional

interface TypePermissionJpaRepository : CrudRepository<TypePermission, Long>
//{
//  @Transactional(readOnly = true)
//  @Query("SELECT p FROM TypePermission p WHERE p.type.organization.id = :organizationId AND p.type.superTypeName = :superTypeName AND p.type.name = :typeName AND p.name = :name")
//  fun findTypePermission(organizationId: Long, superTypeName: String, typeName: String, name: String): TypePermission?
//}