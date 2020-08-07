package com.pibity.erp.repositories

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Repository
@Transactional
class ValueRepositoryImpl(val em: EntityManager) : ValueRepository {

  override fun getVariableNames(organizationName: String, variableSuperListId: Long, variableSuperTypeName: String, variableTypeName: String, keyName: String, stringValue: String?, longValue: Long?, doubleValue: Double?, booleanValue: Boolean?): List<String> {
    return listOf("SSS")
  }

}
