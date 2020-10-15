/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.function

import com.pibity.erp.entities.function.FunctionOutput
import com.pibity.erp.entities.function.embeddables.FunctionOutputId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional

interface FunctionOutputRepository : CrudRepository<FunctionOutput, FunctionOutputId> {

  @Transactional(readOnly = true)
  @Query("SELECT fo FROM FunctionOutput fo WHERE fo.id.function.id.organization.id = :organizationName AND fo.id.function.id.name = :functionName")
  fun getFunctionOutputs(organizationName: String, functionName: String): Set<FunctionOutput>
}
