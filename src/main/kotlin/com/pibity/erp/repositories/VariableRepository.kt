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
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface VariableRepository : CrudRepository<Variable, Long> {

  @Transactional(readOnly = true)
  @Query("SELECT v from Variable v where v.id.type = :type and v.id.superVariableName = :superVariableName and v.id.name = :name")
  fun findByTypeAndName(type: Type, superVariableName: String, name: String = ""): Variable?

  @Transactional(readOnly = true)
  @Query("SELECT v from Variable v where v.id.type.id.organization.name = :organizationName and v.id.type.id.superTypeName = :superTypeName and v.id.type.id.name = :typeName and v.id.superVariableName = :superVariableName and v.id.name = :name")
  fun findVariable(organizationName: String, superTypeName: String, typeName: String, superVariableName: String, name: String = ""): Variable?

  @Query("SELECT v from Variable v where v.id.type = :type AND v.id.superVariableName LIKE %:superVariableName%")
  fun findBySimilarNames(
      @Param("type") type: Type,
      @Param("superVariableName") superVariableName: String
  ): Set<Variable>
}
