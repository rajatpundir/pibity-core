/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories

import com.pibity.erp.entities.Type
import com.pibity.erp.entities.Variable
import com.pibity.erp.entities.VariableList
import com.pibity.erp.entities.embeddables.VariableId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional

interface VariableRepository : CrudRepository<Variable, VariableId> {

  @Transactional(readOnly = true)
  @Query("SELECT v FROM Variable v WHERE v.id.superList = :superList AND v.id.type = :type AND v.id.name = :name")
  fun findByTypeAndName(superList: VariableList, type: Type, name: String = ""): Variable?

  @Transactional(readOnly = true)
  @Query("SELECT v FROM Variable v WHERE v.id.type.id.organization.id = :organizationName AND v.id.type.id.superTypeName = :superTypeName AND v.id.type.id.name = :typeName AND v.id.superList.id = :superList AND v.id.name = :name")
  fun findVariable(organizationName: String, superTypeName: String, typeName: String, superList: Long, name: String = ""): Variable?
}
