/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.function

import com.pibity.erp.entities.function.FunctionInput
import com.pibity.erp.entities.function.embeddables.FunctionInputId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional

interface FunctionInputRepository : CrudRepository<FunctionInput, FunctionInputId> {

  @Transactional(readOnly = true)
  @Query("SELECT fi FROM FunctionInput fi WHERE fi.id.function.id.organization.id = :organizationName AND fi.id.function.id.name = :functionName")
  fun getFunctionInputs(organizationName: String, functionName: String): Set<FunctionInput>
}
