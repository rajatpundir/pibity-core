/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.KeyConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.constants.formulaReturnTypes
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.lisp.getSymbolPaths
import com.pibity.erp.commons.lisp.validateSymbols
import com.pibity.erp.commons.utils.*
import com.pibity.erp.entities.Formula
import com.pibity.erp.entities.Key
import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.permission.TypePermission
import com.pibity.erp.repositories.jpa.KeyJpaRepository
import com.pibity.erp.repositories.jpa.OrganizationJpaRepository
import com.pibity.erp.repositories.jpa.TypeJpaRepository
import com.pibity.erp.repositories.query.TypeRepository
import com.pibity.erp.repositories.query.VariableRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp

@Service
class TypeService(
    val organizationJpaRepository: OrganizationJpaRepository,
    val typeRepository: TypeRepository,
    val typeJpaRepository: TypeJpaRepository,
    val keyJpaRepository: KeyJpaRepository,
    val uniquenessService: UniquenessService,
    val variableRepository: VariableRepository,
    val variableService: VariableService,
    val typePermissionService: TypePermissionService,
    val roleService: RoleService) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createType(jsonParams: JsonObject, typeOrganization: Organization? = null, globalTypes: MutableSet<Type>? = null, localTypes: MutableSet<Type>? = null): Type {
     val typeName: String = validateTypeName(jsonParams.get("typeName").asString)
    val autoId: Boolean = if (jsonParams.has("autoId?")) jsonParams.get("autoId?").asBoolean else false
    val keys: JsonObject = validateTypeKeys(jsonParams.get("keys").asJsonObject)
    val organization: Organization = typeOrganization
        ?: organizationJpaRepository.getById(jsonParams.get("orgId").asLong)
        ?: throw CustomJsonException("{orgId: 'Organization could not be found'}")
    val validTypes: MutableSet<Type> = globalTypes
        ?: typeRepository.findGlobalTypes(organizationId = organization.id) as MutableSet<Type>
    var type = Type(organization = organization, name = typeName, autoId = autoId)
    type = try {
      typeJpaRepository.save(type)
    } catch (exception: Exception) {
      throw CustomJsonException("{typeName: 'Unable to create Type'}")
    }
    for ((keyName, json) in keys.entrySet()) {
      val keyJson = json.asJsonObject
      val keyType: Type = try {
        validTypes.first { it.name == keyJson.get(KeyConstants.KEY_TYPE).asString }
      } catch (exception: Exception) {
        throw CustomJsonException("{keys: {$keyName: {${KeyConstants.KEY_TYPE}: 'Key type is not valid'}}}")
      }
      if (keyType.name != TypeConstants.FORMULA) {
        val keyOrder: Int = keyJson.get(KeyConstants.ORDER).asInt
        val key = Key(parentType = type, name = keyName, keyOrder = keyOrder, type = keyType)
        when (keyType.name) {
          else -> setValueForKey(key = key, keyJson = keyJson)
        }
        type.keys.add(keyJpaRepository.save(key))
      }
    }
    type = try {
      typeJpaRepository.save(type)
    } catch (exception: Exception) {
      throw CustomJsonException("{typeName: 'Unable to create Type'}")
    }
    for ((keyName, json) in keys.entrySet()) {
      val keyJson = json.asJsonObject
      if (!keyJson.get(KeyConstants.KEY_TYPE).isJsonObject && keyJson.get(KeyConstants.KEY_TYPE).asString == TypeConstants.FORMULA) {
        val key = Key(parentType = type, name = keyName, keyOrder = keyJson.get(KeyConstants.ORDER).asInt, type = validTypes.first { it.name == TypeConstants.FORMULA })
        when (keyJson.get(KeyConstants.FORMULA_RETURN_TYPE).asString) {
          in formulaReturnTypes -> {
            val returnType: Type = try {
              validTypes.first { it.name == keyJson.get(KeyConstants.FORMULA_RETURN_TYPE).asString }
            } catch (exception: Exception) {
              throw CustomJsonException("{keys: {$keyName: {${KeyConstants.FORMULA_RETURN_TYPE}: 'Return type is not valid'}}}")
            }
            val keyDependencies: MutableSet<Key> = mutableSetOf()
            val typeDependencies: MutableSet<Type> = mutableSetOf()
            val symbolPaths = validateOrEvaluateExpression(jsonParams = keyJson.get(KeyConstants.FORMULA_EXPRESSION).asJsonObject.deepCopy().apply {
              addProperty("expectedReturnType", keyJson.get(KeyConstants.FORMULA_RETURN_TYPE).asString)
            }, mode = "collect", symbols = JsonObject()) as Set<String>
            val symbols: JsonObject = validateSymbols(jsonParams = getSymbols(type = type, prefix = "", symbolPaths = symbolPaths.toMutableSet(), level = 0, keyDependencies = keyDependencies, typeDependencies = typeDependencies, symbolsForFormula = true))
            try {
              validateOrEvaluateExpression(jsonParams = keyJson.get(KeyConstants.FORMULA_EXPRESSION).asJsonObject.deepCopy().apply {
                addProperty("expectedReturnType", keyJson.get(KeyConstants.FORMULA_RETURN_TYPE).asString)
              }, mode = "validate", symbols = symbols) as String
            } catch (exception: CustomJsonException) {
              throw CustomJsonException("{keys: {$keyName: {expression: ${exception.message}}}}")
            }
            val formula = Formula(returnType = returnType,
                expression = keyJson.get(KeyConstants.FORMULA_EXPRESSION).asJsonObject.toString(),
                symbolPaths = gson.toJson(getSymbolPaths(jsonParams = symbols)),
                keyDependencies = keyDependencies,
                typeDependencies = typeDependencies)
            key.formula = formula
          }
          else -> throw CustomJsonException("{keys: {$keyName: {${KeyConstants.FORMULA_RETURN_TYPE}: 'Return type is not valid'}}}")
        }
        type.keys.add(keyJpaRepository.save(key))
      }
    }
    type = try {
      typeJpaRepository.save(type)
    } catch (exception: Exception) {
      throw CustomJsonException("{typeName: 'Unable to create Type'}")
    }
    type.permissions.addAll(createDefaultPermissionsForType(type))
    createPermissionsForType(jsonParams = jsonParams)
    assignTypePermissionsToRoles(jsonParams = jsonParams)
    createTypeUniquenessConstraints(jsonParams = jsonParams)
    if (jsonParams.has("variables?"))
      createVariablesForType(jsonParams = jsonParams)
    return type
  }

  private fun setValueForKey(key: Key, keyJson: JsonObject) {
    val defaultValue: JsonElement? = if (keyJson.has("default")) keyJson.get("default") else null
    when (key.type.name) {
      TypeConstants.TEXT -> key.defaultStringValue = defaultValue?.asString ?: ""
      TypeConstants.NUMBER -> key.defaultLongValue = defaultValue?.asLong ?: 0
      TypeConstants.DECIMAL -> key.defaultDecimalValue = defaultValue?.asBigDecimal ?: (0).toBigDecimal()
      TypeConstants.BOOLEAN -> key.defaultBooleanValue = defaultValue?.asBoolean ?: false
      TypeConstants.DATE -> key.defaultDateValue = if (defaultValue != null) java.sql.Date(dateFormat.parse(defaultValue.asString).time) else null
      TypeConstants.TIMESTAMP -> key.defaultTimestampValue = if (defaultValue != null) Timestamp(defaultValue.asLong) else null
      TypeConstants.TIME -> key.defaultTimeValue = if (defaultValue != null) java.sql.Time(defaultValue.asLong) else null
      TypeConstants.FORMULA, TypeConstants.BLOB -> {
      }
      else -> if (defaultValue != null) {
        key.referencedVariable = variableRepository.findByTypeAndName(type = key.type, name = defaultValue.asString)
            ?: throw CustomJsonException("{keys: {${key.name}: {default: 'Variable reference is not correct'}}}")
      }
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createTypeUniquenessConstraints(jsonParams: JsonObject) {
    for ((constraintName, jsonKeys) in jsonParams.get("uniqueConstraints").asJsonObject.entrySet()) {
      uniquenessService.createUniqueness(jsonParams = JsonObject().apply {
        addProperty("orgId", jsonParams.get("orgId").asString)
        addProperty("typeName", jsonParams.get("typeName").asString)
        addProperty("constraintName", constraintName)
        add("keys", jsonKeys.asJsonArray)
      })
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createPermissionsForType(jsonParams: JsonObject) {
    for (jsonPermission in jsonParams.get("permissions").asJsonArray) {
      if (jsonPermission.isJsonObject) {
        typePermissionService.createTypePermission(jsonParams = JsonObject().apply {
          addProperty("orgId", jsonParams.get("orgId").asString)
          addProperty("typeName", jsonParams.get("typeName").asString)
          try {
            addProperty("permissionName", jsonParams.get("permissionName").asString)
          } catch (exception: Exception) {
            throw CustomJsonException("{permissions: {permissionName: 'Unexpected value for parameter'}}")
          }
          try {
            addProperty("creatable", jsonParams.get("creatable").asBoolean)
          } catch (exception: Exception) {
            throw CustomJsonException("{permissions: {creatable: 'Unexpected value for parameter'}}")
          }
          try {
            addProperty("deletable", jsonParams.get("deletable").asBoolean)
          } catch (exception: Exception) {
            throw CustomJsonException("{permissions: {deletable: 'Unexpected value for parameter'}}")
          }
          try {
            add("permissions", jsonParams.get("permissions").asJsonArray)
          } catch (exception: Exception) {
            throw CustomJsonException("{permissions: {permissions: 'Unexpected value for parameter'}}")
          }
        })
      } else throw CustomJsonException("{permissions: 'Unexpected value for parameter'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun assignTypePermissionsToRoles(jsonParams: JsonObject) {
    for ((roleName, permissionNames) in jsonParams.get("roles").asJsonObject.entrySet()) {
      if (permissionNames.isJsonArray) {
        for (permissionName in permissionNames.asJsonArray) {
          roleService.updateRoleTypePermissions(jsonParams = JsonObject().apply {
            addProperty("orgId", jsonParams.get("orgId").asString)
            addProperty("typeName", jsonParams.get("typeName").asString)
            addProperty("roleName", roleName)
            try {
              addProperty("permissionName", permissionName.asString)
            } catch (exception: Exception) {
              throw CustomJsonException("{roles: {${roleName}: 'Unexpected value for parameter'}")
            }
            addProperty("operation", "add")
          })
        }
      } else throw CustomJsonException("{roles: {${roleName}: 'Unexpected value for parameter'}}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createVariablesForType(jsonParams: JsonObject) {
    for ((variableName, values) in jsonParams.get("variables?").asJsonObject.entrySet()) {
      val jsonVariableParams = JsonObject()
      jsonVariableParams.apply {
        addProperty("orgId", jsonParams.get("orgId").asString)
        addProperty("username", jsonParams.get("username").asString)
        addProperty("typeName", jsonParams.get("typeName").asString)
        addProperty("variableName", variableName)
        try {
          add("values", values.asJsonObject)
        } catch (exception: Exception) {
          throw CustomJsonException("{variables: {$variableName: {values: 'Unexpected value for parameter'}}}")
        }
      }
      try {
        variableService.createVariable(jsonParams = jsonVariableParams)
      } catch (exception: CustomJsonException) {
        throw CustomJsonException("{variables: {$variableName: ${exception.message}}}")
      }
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createDefaultPermissionsForType(type: Type): Set<TypePermission> {
    val defaultPermissions = mutableSetOf<TypePermission>()
    defaultPermissions.add(typePermissionService.createDefaultTypePermission(type = type, permissionName = "READ_ALL", accessLevel = 1))
    defaultPermissions.add(typePermissionService.createDefaultTypePermission(type = type, permissionName = "WRITE_ALL", accessLevel = 2))
    return defaultPermissions
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun getTypeDetails(jsonParams: JsonObject): Type {
    return typeRepository.findType(organizationId = jsonParams.get("orgId").asLong, name = jsonParams.get("typeName").asString)
        ?: throw CustomJsonException("{typeName: 'Type could not be determined'}")
  }
}
