/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories

import com.pibity.erp.entities.permission.FunctionPermission
import com.pibity.erp.entities.permission.TypePermission
import com.pibity.erp.entities.permission.embeddables.FunctionPermissionId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional

interface FunctionPermissionRepository : CrudRepository<FunctionPermission, FunctionPermissionId> {

  @Transactional(readOnly = true)
  @Query("SELECT p FROM FunctionPermission p WHERE p.id.function.id.organization.id = :organizationName AND p.id.function.id.name = :functionName AND p.id.name = :name")
  fun findFunctionPermission(organizationName: String, functionName: String, name: String): FunctionPermission?
}
