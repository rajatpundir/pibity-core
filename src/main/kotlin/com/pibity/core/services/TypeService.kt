/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.services

import com.google.gson.JsonObject
import com.pibity.core.commons.constants.*
import com.pibity.core.commons.CustomJsonException
import com.pibity.core.commons.lisp.validateSymbols
import com.pibity.core.entities.*
import com.pibity.core.entities.assertion.TypeAssertion
import com.pibity.core.entities.permission.TypePermission
import com.pibity.core.entities.uniqueness.TypeUniqueness
import com.pibity.core.repositories.jpa.FormulaJpaRepository
import com.pibity.core.repositories.jpa.KeyJpaRepository
import com.pibity.core.repositories.jpa.OrganizationJpaRepository
import com.pibity.core.repositories.jpa.TypeJpaRepository
import com.pibity.core.repositories.query.TypeRepository
import com.pibity.core.repositories.query.VariableRepository
import com.pibity.core.utils.*
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.sql.Timestamp

@Service
class TypeService(
  val organizationJpaRepository: OrganizationJpaRepository,
  val typeRepository: TypeRepository,
  val typeJpaRepository: TypeJpaRepository,
  val keyJpaRepository: KeyJpaRepository,
  val formulaJpaRepository: FormulaJpaRepository,
  val uniquenessService: UniquenessService,
  val assertionService: AssertionService,
  val variableRepository: VariableRepository,
  val variableService: VariableService,
  val typePermissionService: TypePermissionService,
  val subspaceService: SubspaceService
) {

  @Suppress("UNCHECKED_CAST")
  fun createType(jsonParams: JsonObject, files: List<MultipartFile>, defaultTimestamp: Timestamp): Type {
    val organization: Organization = organizationJpaRepository.getById(jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
      ?: throw CustomJsonException("{${OrganizationConstants.ORGANIZATION_ID}: 'Organization could not be found'}")
    val validTypes: MutableSet<Type> = typeRepository.findTypes(orgId = organization.id) as MutableSet<Type>
    val keys: JsonObject = validateTypeKeys(jsonParams.get("keys").asJsonObject, validTypes = validTypes, files = files)
    val type = typeJpaRepository.save(Type(organization = organization, name = validateTypeName(jsonParams.get(OrganizationConstants.TYPE_NAME).asString),
        autoId = if (jsonParams.has("autoId?")) jsonParams.get("autoId?").asBoolean else false, created = defaultTimestamp
    ))
    type.keys.addAll(
      keys.entrySet()
        .filter { (_, json) -> json.asJsonObject.get(KeyConstants.KEY_TYPE).asString != TypeConstants.FORMULA }
        .sortedBy { (_, json) -> json.asJsonObject.get(KeyConstants.ORDER).asLong }
        .map { (keyName, json) ->
          val keyJson: JsonObject = json.asJsonObject
          keyJpaRepository.save(
            Key(
              parentType = type,
              name = keyName,
              type = validTypes.single { it.name == keyJson.get(KeyConstants.KEY_TYPE).asString },
              keyOrder = keyJson.get(KeyConstants.ORDER).asInt,
              created = defaultTimestamp
            ).apply {
              when (this.type.name) {
                TypeConstants.TEXT -> defaultStringValue =
                  if (keyJson.has(KeyConstants.DEFAULT)) keyJson.get(KeyConstants.DEFAULT).asString else ""
                TypeConstants.NUMBER -> defaultLongValue =
                  if (keyJson.has(KeyConstants.DEFAULT)) keyJson.get(KeyConstants.DEFAULT).asLong else 0
                TypeConstants.DECIMAL -> defaultDecimalValue =
                  if (keyJson.has(KeyConstants.DEFAULT)) keyJson.get(KeyConstants.DEFAULT).asBigDecimal else (0.0).toBigDecimal()
                TypeConstants.BOOLEAN -> defaultBooleanValue =
                  if (keyJson.has(KeyConstants.DEFAULT)) keyJson.get(KeyConstants.DEFAULT).asBoolean else false
                TypeConstants.DATE -> defaultDateValue =
                  if (keyJson.has(KeyConstants.DEFAULT)) java.sql.Date(keyJson.get(KeyConstants.DEFAULT).asLong) else null
                TypeConstants.TIMESTAMP -> defaultTimestampValue =
                  if (keyJson.has(KeyConstants.DEFAULT)) Timestamp(keyJson.get(KeyConstants.DEFAULT).asLong) else null
                TypeConstants.TIME -> defaultTimeValue =
                  if (keyJson.has(KeyConstants.DEFAULT)) java.sql.Time(keyJson.get(KeyConstants.DEFAULT).asLong) else null
                TypeConstants.BLOB -> defaultBlobValue =
                  if (keyJson.has(KeyConstants.DEFAULT)) BlobProxy.generateProxy(files[keyJson.get(KeyConstants.DEFAULT).asInt].bytes) else BlobProxy.generateProxy("".toByteArray())
                else -> {
                  if (keyJson.has(KeyConstants.DEFAULT)) {
                    referencedVariable = variableRepository.findVariable(
                      type = this.type,
                      name = keyJson.get(KeyConstants.DEFAULT).asString
                    )
                      ?: throw CustomJsonException("{keys: {${this.name}: {default: 'Variable reference is not correct'}}}")
                  }
                }
              }
            })
        })
    type.keys.addAll(keys.entrySet()
      .filter { (_, json) -> json.asJsonObject.get(KeyConstants.KEY_TYPE).asString == TypeConstants.FORMULA }
      .sortedBy { (_, json) -> json.asJsonObject.get(KeyConstants.ORDER).asLong }
      .map { (keyName, json) ->
        val keyJson: JsonObject = json.asJsonObject
        keyJpaRepository.save(
          Key(
            parentType = type,
            name = keyName,
            type = validTypes.single { it.name == keyJson.get(KeyConstants.KEY_TYPE).asString },
            keyOrder = keyJson.get(KeyConstants.ORDER).asInt,
            created = defaultTimestamp
          )
        ).apply {
          val keyDependencies: MutableSet<Key> = mutableSetOf()
          val symbolPaths: Set<String> = validateOrEvaluateExpression(expression = keyJson.get(KeyConstants.FORMULA_EXPRESSION).asJsonObject,
            symbols = JsonObject(), mode = LispConstants.VALIDATE, expectedReturnType = keyJson.get(KeyConstants.FORMULA_RETURN_TYPE).asString) as Set<String>
          val symbols: JsonObject = validateSymbols(getSymbolsForFormula(type = type, symbolPaths = symbolPaths.toMutableSet(), keyDependencies = keyDependencies, excludeTopLevelFormulas = true))
          formula = formulaJpaRepository.save(
            Formula(
              key = this,
              returnType = validTypes.single { it.name == keyJson.get(KeyConstants.FORMULA_RETURN_TYPE).asString },
              expression = (validateOrEvaluateExpression(expression = keyJson.get(KeyConstants.FORMULA_EXPRESSION).asJsonObject, symbols = symbols,
                mode = LispConstants.REFLECT, expectedReturnType = keyJson.get(KeyConstants.FORMULA_RETURN_TYPE).asString) as JsonObject).toString(),
              symbolPaths = gson.toJson(symbolPaths),
              keyDependencies = keyDependencies,
              created = defaultTimestamp
            )
          )
        }
      })
    type.uniqueConstraints.addAll(createTypeUniquenessConstraints(jsonParams = jsonParams, files = files, defaultTimestamp = defaultTimestamp))
    type.typeAssertions.addAll(createTypeAssertions(jsonParams = jsonParams, defaultTimestamp = defaultTimestamp))
    type.permissions.addAll(typePermissionService.createDefaultTypePermission(type = type, defaultTimestamp = defaultTimestamp))
    type.permissions.addAll(createTypePermissions(jsonParams = jsonParams, defaultTimestamp = defaultTimestamp))
    assignTypePermissionsToRoles(jsonParams = jsonParams, defaultTimestamp = defaultTimestamp)
    createVariablesForType(jsonParams = jsonParams, defaultTimestamp = defaultTimestamp, files = files)
    return type
  }

  fun createTypeUniquenessConstraints(jsonParams: JsonObject, files: List<MultipartFile>, defaultTimestamp: Timestamp): Set<TypeUniqueness> {
    return jsonParams.get("uniqueConstraints").asJsonObject.entrySet().map { (uniquenessName, uniquenessJson) ->
      uniquenessService.createUniqueness(jsonParams = JsonObject().apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
        addProperty(OrganizationConstants.TYPE_NAME, jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
        addProperty("constraintName", uniquenessName)
        add("keys", uniquenessJson.asJsonObject.get("keys").asJsonArray)
        add("accumulators", jsonParams.get("accumulators").asJsonObject)
      }, files = files, defaultTimestamp = defaultTimestamp)
    }.toSet()
  }

  fun createTypeAssertions(jsonParams: JsonObject, defaultTimestamp: Timestamp): Set<TypeAssertion> {
    return jsonParams.get("assertions").asJsonObject.entrySet().map { (assertionName, assertionJson) ->
      assertionService.createAssertion(jsonParams = JsonObject().apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
        addProperty(OrganizationConstants.TYPE_NAME, jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
        addProperty("assertionName", assertionName)
        add("expression", assertionJson.asJsonObject)
      }, defaultTimestamp = defaultTimestamp)
    }.toSet()
  }

  fun createTypePermissions(jsonParams: JsonObject, defaultTimestamp: Timestamp): Set<TypePermission> {
    return jsonParams.get("permissions").asJsonObject.entrySet().map { (permissionName, permissionJson) ->
      typePermissionService.createTypePermission(jsonParams = permissionJson.asJsonObject.apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
        addProperty(OrganizationConstants.TYPE_NAME, jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
        addProperty("permissionName", permissionName)
      }, defaultTimestamp = defaultTimestamp)
    }.toSet()
  }

  fun assignTypePermissionsToRoles(jsonParams: JsonObject, defaultTimestamp: Timestamp) {
    jsonParams.get("roles").asJsonObject.entrySet().forEach { (roleName, permissionsJson) ->
      permissionsJson.asJsonArray.forEach {
        subspaceService.updateRoleTypePermissions(jsonParams = JsonObject().apply {
          addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
          addProperty(OrganizationConstants.TYPE_NAME, jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
          addProperty("roleName", roleName)
          addProperty("permissionName", it.asString)
        }, defaultTimestamp = defaultTimestamp)
      }
    }
  }

  fun createVariablesForType(jsonParams: JsonObject, files: List<MultipartFile>, defaultTimestamp: Timestamp) {
    variableService.executeQueue(jsonParams = JsonObject().apply {
      addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
      add(VariableConstants.QUEUE, jsonParams.get("variables").asJsonObject.entrySet().foldIndexed(JsonObject()) { index, acc, (variableName, valuesJson) ->
        acc.apply {
          add(index.toString(), JsonObject().apply {
            addProperty(VariableConstants.OPERATION, VariableConstants.CREATE)
            addProperty(OrganizationConstants.TYPE_NAME, jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
            addProperty(VariableConstants.VARIABLE_NAME, variableName)
            add(VariableConstants.VALUES, valuesJson.asJsonArray)
          })
        }
      })
    }, files = files, defaultTimestamp = defaultTimestamp)
  }

  fun getTypeDetails(jsonParams: JsonObject): Type {
    return typeRepository.findType(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
      ?: throw CustomJsonException("{${OrganizationConstants.TYPE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
  }
}
