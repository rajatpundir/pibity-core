/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.LispConstants
import com.pibity.erp.commons.constants.MessageConstants
import com.pibity.erp.commons.constants.OrganizationConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.lisp.validateSymbols
import com.pibity.erp.commons.utils.*
import com.pibity.erp.entities.assertion.TypeAssertion
import com.pibity.erp.entities.Key
import com.pibity.erp.entities.Type
import com.pibity.erp.repositories.jpa.AssertionJpaRepository
import com.pibity.erp.repositories.query.TypeRepository
import org.springframework.stereotype.Service

@Service
class AssertionService(
    val typeRepository: TypeRepository,
    val assertionJpaRepository: AssertionJpaRepository
) {

  @Suppress("UNCHECKED_CAST")
  fun createAssertion(jsonParams: JsonObject): TypeAssertion {
    val type: Type = typeRepository.findType(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
        ?: throw CustomJsonException("{${OrganizationConstants.TYPE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    val assertionName: String = if (typeIdentifierPattern.matcher(jsonParams.get("assertionName").asString).matches())
      jsonParams.get("assertionName").asString
    else
      throw CustomJsonException("{assertionName: 'Assertion name ${jsonParams.get("assertionName").asString} is not a valid identifier'}")
    val keyDependencies: MutableSet<Key> = mutableSetOf()
    val symbolPaths: Set<String> = validateOrEvaluateExpression(expression = jsonParams.get("expression").asJsonObject,
      symbols = JsonObject(), mode = LispConstants.VALIDATE, expectedReturnType = TypeConstants.BOOLEAN) as Set<String>
    val symbols: JsonObject = validateSymbols(getSymbols(type = type, symbolPaths = symbolPaths.toMutableSet(), keyDependencies = keyDependencies, symbolsForFormula = false))
    return try {
      assertionJpaRepository.save(
        TypeAssertion(type = type, name = assertionName,
        symbolPaths = gson.toJson(symbolPaths),
        expression = (validateOrEvaluateExpression(expression = jsonParams.get("expression").asJsonObject, symbols = symbols,
          mode = LispConstants.REFLECT, expectedReturnType = TypeConstants.BOOLEAN) as JsonObject).toString(),
        keyDependencies = keyDependencies)
      )
    } catch (exception: Exception) {
      throw CustomJsonException("{assertionName: 'Unable to create Type Assertion'}")
    }
  }
}
