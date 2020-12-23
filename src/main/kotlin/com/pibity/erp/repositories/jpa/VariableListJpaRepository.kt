/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories.jpa

import com.pibity.erp.entities.VariableList
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional

interface VariableListJpaRepository : CrudRepository<VariableList, Long> {

  @Transactional(readOnly = true)
  fun getById(id: Long): VariableList?
}
