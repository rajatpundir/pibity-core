/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.*
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.lisp.validateSymbols
import com.pibity.erp.commons.utils.*
import com.pibity.erp.entities.*
import com.pibity.erp.entities.assertion.TypeAssertion
import com.pibity.erp.entities.permission.TypePermission
import com.pibity.erp.entities.uniqueness.TypeUniqueness
import com.pibity.erp.repositories.jpa.FormulaJpaRepository
import com.pibity.erp.repositories.jpa.KeyJpaRepository
import com.pibity.erp.repositories.jpa.OrganizationJpaRepository
import com.pibity.erp.repositories.jpa.TypeJpaRepository
import com.pibity.erp.repositories.query.TypeRepository
import com.pibity.erp.repositories.query.VariableRepository
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
  val roleService: RoleService
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
                    referencedVariable = variableRepository.findByTypeAndName(
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
          val symbols: JsonObject = validateSymbols(getSymbols(type = type, symbolPaths = symbolPaths.toMutableSet(), keyDependencies = keyDependencies, symbolsForFormula = true))
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
    type.uniqueConstraints.addAll(createTypeUniquenessConstraints(jsonParams = jsonParams, defaultTimestamp = defaultTimestamp))
    type.typeAssertions.addAll(createTypeAssertions(jsonParams = jsonParams, defaultTimestamp = defaultTimestamp))
    type.permissions.addAll(createDefaultPermissionsForType(type = type, defaultTimestamp = defaultTimestamp))
    type.permissions.addAll(createPermissionsForType(jsonParams = jsonParams, defaultTimestamp = defaultTimestamp))
    assignTypePermissionsToRoles(jsonParams = jsonParams, defaultTimestamp = defaultTimestamp)
    if (jsonParams.has("variables?"))
      createVariablesForType(jsonParams = jsonParams, defaultTimestamp = defaultTimestamp, files = files)
    return type
  }

  fun createTypeUniquenessConstraints(jsonParams: JsonObject, defaultTimestamp: Timestamp): Set<TypeUniqueness> {
    val uniqueConstraints: MutableSet<TypeUniqueness> = mutableSetOf()
    for ((constraintName, jsonKeys) in jsonParams.get("uniqueConstraints").asJsonObject.entrySet()) {
      uniqueConstraints.add(uniquenessService.createUniqueness(jsonParams = JsonObject().apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
        addProperty(OrganizationConstants.TYPE_NAME, jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
        addProperty("constraintName", constraintName)
        add("keys", jsonKeys.asJsonArray)
      }, defaultTimestamp = defaultTimestamp))
    }
    return uniqueConstraints
  }

  fun createTypeAssertions(jsonParams: JsonObject, defaultTimestamp: Timestamp): Set<TypeAssertion> {
    val assertions: MutableSet<TypeAssertion> = mutableSetOf()
    for ((assertionName, assertionExpression) in jsonParams.get("assertions").asJsonObject.entrySet()) {
      assertions.add(assertionService.createAssertion(jsonParams = JsonObject().apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
        addProperty(OrganizationConstants.TYPE_NAME, jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
        addProperty("assertionName", assertionName)
        add("expression", assertionExpression.asJsonObject)
      }, defaultTimestamp = defaultTimestamp))
    }
    return assertions
  }

  fun createPermissionsForType(jsonParams: JsonObject, defaultTimestamp: Timestamp): Set<TypePermission> {
    val typePermissions: MutableSet<TypePermission> = mutableSetOf()
    for (jsonPermission in jsonParams.get("permissions").asJsonArray) {
      if (jsonPermission.isJsonObject) {
        val (typePermission, _) = typePermissionService.createTypePermission(jsonParams = JsonObject().apply {
          addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
          addProperty(OrganizationConstants.TYPE_NAME, jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
          try {
            addProperty("permissionName", jsonParams.get("permissionName").asString)
          } catch (exception: Exception) {
            throw CustomJsonException("{permissions: {permissionName: ${MessageConstants.UNEXPECTED_VALUE}}}")
          }
          try {
            addProperty("creatable", jsonParams.get("creatable").asBoolean)
          } catch (exception: Exception) {
            throw CustomJsonException("{permissions: {creatable: ${MessageConstants.UNEXPECTED_VALUE}}}")
          }
          try {
            addProperty("deletable", jsonParams.get("deletable").asBoolean)
          } catch (exception: Exception) {
            throw CustomJsonException("{permissions: {deletable: ${MessageConstants.UNEXPECTED_VALUE}}}")
          }
          try {
            add("permissions", jsonParams.get("permissions").asJsonArray)
          } catch (exception: Exception) {
            throw CustomJsonException("{permissions: {permissions: ${MessageConstants.UNEXPECTED_VALUE}}}")
          }
        }, defaultTimestamp = defaultTimestamp)
        typePermissions.add(typePermission)
      } else throw CustomJsonException("{permissions: ${MessageConstants.UNEXPECTED_VALUE}}")
    }
    return typePermissions
  }

  fun assignTypePermissionsToRoles(jsonParams: JsonObject, defaultTimestamp: Timestamp) {
    for ((roleName, permissionNames) in jsonParams.get("roles").asJsonObject.entrySet()) {
      if (permissionNames.isJsonArray) {
        for (permissionName in permissionNames.asJsonArray) {
          roleService.updateRoleTypePermissions(jsonParams = JsonObject().apply {
            addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
            addProperty(OrganizationConstants.TYPE_NAME, jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
            addProperty("roleName", roleName)
            try {
              addProperty("permissionName", permissionName.asString)
            } catch (exception: Exception) {
              throw CustomJsonException("{roles: {${roleName}: ${MessageConstants.UNEXPECTED_VALUE}}")
            }
            addProperty("operation", "add")
          }, defaultTimestamp = defaultTimestamp)
        }
      } else throw CustomJsonException("{roles: {${roleName}: ${MessageConstants.UNEXPECTED_VALUE}}}")
    }
  }

  fun createVariablesForType(jsonParams: JsonObject, defaultTimestamp: Timestamp, files: List<MultipartFile>) {
    for ((variableName, values) in jsonParams.get("variables?").asJsonObject.entrySet()) {
      val jsonVariableParams = JsonObject()
      jsonVariableParams.apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
        addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
        addProperty(OrganizationConstants.TYPE_NAME, jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
        addProperty(VariableConstants.VARIABLE_NAME, variableName)
        try {
          add(VariableConstants.VALUES, values.asJsonObject)
        } catch (exception: Exception) {
          throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: {$variableName: {${VariableConstants.VALUES}: ${MessageConstants.UNEXPECTED_VALUE}}}}")
        }
      }
      try {
        variableService.createVariable(jsonParams = jsonVariableParams, defaultTimestamp = defaultTimestamp, files = files)
      } catch (exception: CustomJsonException) {
        throw CustomJsonException("{variables: {$variableName: ${exception.message}}}")
      }
    }
  }

  fun createDefaultPermissionsForType(type: Type, defaultTimestamp: Timestamp): Set<TypePermission> {
    val defaultPermissions = mutableSetOf<TypePermission>()
    defaultPermissions.add(
      typePermissionService.createDefaultTypePermission(
        type = type,
        permissionName = "READ_ALL",
        accessLevel = 1,
        defaultTimestamp = defaultTimestamp
      )
    )
    defaultPermissions.add(
      typePermissionService.createDefaultTypePermission(
        type = type,
        permissionName = "WRITE_ALL",
        accessLevel = 2,
        defaultTimestamp = defaultTimestamp
      )
    )
    return defaultPermissions
  }

  fun getTypeDetails(jsonParams: JsonObject): Type {
    return typeRepository.findType(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
      ?: throw CustomJsonException("{${OrganizationConstants.TYPE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
  }
}
