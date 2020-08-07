/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.repositories

import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.Variable
import com.pibity.erp.entities.VariableList
import com.pibity.erp.entities.embeddables.VariableId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface VariableRepository : CrudRepository<Variable, VariableId> {

  @Transactional(readOnly = true)
  @Query("SELECT v from Variable v where v.id.superList = :superList and v.id.type = :type and v.id.name = :name")
  fun findByTypeAndName(superList: VariableList, type: Type, name: String = ""): Variable?

  @Transactional(readOnly = true)
  @Query("SELECT v from Variable v where v.id.type.id.organization = :organization and v.id.type.id.superTypeName = :superTypeName and v.id.type.id.name = :typeName and v.id.superList.id = :superList and v.id.name = :name")
  fun findVariable(organization: Organization, superTypeName: String, typeName: String, superList: Long, name: String = ""): Variable?

  @Transactional(readOnly = true)
  @Query("SELECT v from Variable v where v.id.superList = :superList and v.id.type = :type AND v.id.name LIKE %:name%")
  fun findBySimilarNames(
      @Param("superList") superList: VariableList,
      @Param("type") type: Type,
      @Param("name") name: String
  ): Set<Variable>
}
