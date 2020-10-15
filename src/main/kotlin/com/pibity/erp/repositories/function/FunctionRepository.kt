/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.function

import com.pibity.erp.entities.function.Function
import com.pibity.erp.entities.function.embeddables.FunctionId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional

interface FunctionRepository : CrudRepository<Function, FunctionId> {

  @Transactional(readOnly = true)
  @Query("SELECT f FROM Function f WHERE f.id.organization.id = :organizationName AND f.id.name = :name")
  fun findFunction(organizationName: String, name: String): Function?
}
