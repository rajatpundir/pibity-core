/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.GLOBAL_TYPE
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.lisp.getSymbolPaths
import com.pibity.erp.commons.lisp.validateSymbols
import com.pibity.erp.commons.utils.*
import com.pibity.erp.entities.TypeAssertion
import com.pibity.erp.entities.Key
import com.pibity.erp.entities.Type
import com.pibity.erp.repositories.jpa.AssertionJpaRepository
import com.pibity.erp.repositories.query.AssertionRepository
import com.pibity.erp.repositories.query.TypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.regex.Pattern

@Service
class AssertionService(
    val typeRepository: TypeRepository,
    val assertionRepository: AssertionRepository,
    val assertionJpaRepository: AssertionJpaRepository
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createAssertion(jsonParams: JsonObject): TypeAssertion {
    val type: Type = typeRepository.findType(organizationId = jsonParams.get("orgId").asLong, superTypeName = GLOBAL_TYPE, name = jsonParams.get("typeName").asString)
        ?: throw CustomJsonException("{typeName: 'Type could not be determined'}")
    val keyDependencies: MutableSet<Key> = mutableSetOf()
    val typeDependencies: MutableSet<Type> = mutableSetOf()
    val assertionIdentifierPattern: Pattern = Pattern.compile("^[a-z][a-zA-Z0-9]*$")
    val assertionName: String = if (assertionIdentifierPattern.matcher(jsonParams.get("assertionName").asString).matches())
      jsonParams.get("assertionName").asString
    else
      throw CustomJsonException("{assertionName: 'Assertion name ${jsonParams.get("assertionName").asString} is not a valid identifier'}")
    val symbolPaths: Set<String> = validateOrEvaluateExpression(jsonParams = jsonParams.get("assertion").asJsonObject.deepCopy().apply {
      addProperty("expectedReturnType", TypeConstants.BOOLEAN)
    }, mode = "collect", symbols = JsonObject()) as Set<String>
    val symbols: JsonObject = validateSymbols(jsonParams = getSymbols(type = type, prefix = "", symbolPaths = symbolPaths.toMutableSet(), level = 1, keyDependencies = keyDependencies, typeDependencies = typeDependencies, symbolsForFormula = false))
    try {
      validateOrEvaluateExpression(jsonParams = jsonParams.get("assertion").asJsonObject.deepCopy().apply {
        addProperty("expectedReturnType", TypeConstants.BOOLEAN)
      }, mode = "validate", symbols = symbols) as String
    } catch (exception: CustomJsonException) {
      throw CustomJsonException("{assertion: ${exception.message}}")
    }
    val assertion = TypeAssertion(type = type.apply { hasAssertions = true },
        name = assertionName,
        symbolPaths = gson.toJson(getSymbolPaths(jsonParams = symbols)),
        expression = jsonParams.get("assertion").asJsonObject.toString(),
        keyDependencies = keyDependencies,
        typeDependencies = typeDependencies)
    return try {
      assertionJpaRepository.save(assertion)
    } catch (exception: Exception) {
      throw CustomJsonException("{assertionName: 'Assertion could not be saved'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun getAssertionDetails(jsonParams: JsonObject): TypeAssertion {
    return assertionRepository.findAssertion(organizationId = jsonParams.get("orgId").asLong, typeName = jsonParams.get("typeName").asString, name = jsonParams.get("typeName").asString)
        ?: throw CustomJsonException("{assertionName: 'Assertion could not be saved'}")
  }
}
