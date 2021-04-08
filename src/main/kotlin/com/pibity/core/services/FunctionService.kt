/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.services

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.core.commons.Base64DecodedMultipartFile
import com.pibity.core.commons.constants.*
import com.pibity.core.commons.CustomJsonException
import com.pibity.core.commons.lisp.getSymbolPaths
import com.pibity.core.entities.Key
import com.pibity.core.entities.Organization
import com.pibity.core.entities.Type
import com.pibity.core.entities.Variable
import com.pibity.core.entities.function.*
import com.pibity.core.entities.function.Function
import com.pibity.core.entities.permission.FunctionPermission
import com.pibity.core.entities.permission.TypePermission
import com.pibity.core.repositories.function.jpa.*
import com.pibity.core.repositories.jpa.OrganizationJpaRepository
import com.pibity.core.repositories.query.TypeRepository
import com.pibity.core.repositories.query.VariableRepository
import com.pibity.core.utils.*
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.util.*

@Service
class FunctionService(
  val organizationJpaRepository: OrganizationJpaRepository,
  val typeRepository: TypeRepository,
  val functionJpaRepository: FunctionJpaRepository,
  val functionInputJpaRepository: FunctionInputJpaRepository,
  val functionInputKeyJpaRepository: FunctionInputKeyJpaRepository,
  val functionOutputJpaRepository: FunctionOutputJpaRepository,
  val functionOutputKeyJpaRepository: FunctionOutputKeyJpaRepository,
  val functionPermissionService: FunctionPermissionService,
  val variableRepository: VariableRepository,
  val variableService: VariableService,
  val roleService: RoleService,
  val userService: UserService
) {

  @Suppress("UNCHECKED_CAST")
  fun createFunction(jsonParams: JsonObject, files: List<MultipartFile>, defaultTimestamp: Timestamp): Function {
    val organization: Organization = organizationJpaRepository.getById(jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong)
      ?: throw CustomJsonException("{${OrganizationConstants.ORGANIZATION_ID}: 'Organization could not be found'}")
    val validTypes: MutableSet<Type> = typeRepository.findTypes(orgId = organization.id) as MutableSet<Type>
    val (functionName: String, inputs: JsonObject, outputs: JsonObject, symbols: JsonObject) = validateFunction(jsonParams = jsonParams, validTypes = validTypes)
    val function: Function = functionJpaRepository.save(Function(organization = organization, name = functionName, symbolPaths = gson.toJson(getSymbolPaths(symbols = symbols)), created = defaultTimestamp))
    function.inputs.addAll(inputs.entrySet().map { (inputName, input) ->
        val inputJson: JsonObject = input.asJsonObject
        val type: Type = validTypes.single { it.name == inputJson.get(KeyConstants.KEY_TYPE).asString }
        functionInputJpaRepository.save(FunctionInput(function = function, name = inputName, type = type).apply {
          when (type.name) {
            in primitiveTypes -> {
              when(type.name) {
                TypeConstants.TEXT -> defaultStringValue = if (inputJson.has(KeyConstants.DEFAULT)) inputJson.get(KeyConstants.DEFAULT).asString else ""
                TypeConstants.NUMBER -> defaultLongValue = if (inputJson.has(KeyConstants.DEFAULT)) inputJson.get(KeyConstants.DEFAULT).asLong else 0
                TypeConstants.DECIMAL -> defaultDecimalValue = if (inputJson.has(KeyConstants.DEFAULT)) inputJson.get(KeyConstants.DEFAULT).asBigDecimal else (0.0).toBigDecimal()
                TypeConstants.BOOLEAN -> defaultBooleanValue = if (inputJson.has(KeyConstants.DEFAULT)) inputJson.get(KeyConstants.DEFAULT).asBoolean else false
                TypeConstants.DATE -> defaultDateValue = if (inputJson.has(KeyConstants.DEFAULT)) Date(inputJson.get(KeyConstants.DEFAULT).asLong) else null
                TypeConstants.TIMESTAMP -> defaultTimestampValue = if (inputJson.has(KeyConstants.DEFAULT)) Timestamp(inputJson.get(KeyConstants.DEFAULT).asLong) else null
                TypeConstants.TIME -> defaultTimeValue = if (inputJson.has(KeyConstants.DEFAULT)) Time(inputJson.get(KeyConstants.DEFAULT).asLong) else null
                TypeConstants.BLOB -> defaultBlobValue = if (inputJson.has(KeyConstants.DEFAULT)) BlobProxy.generateProxy(files[inputJson.get(KeyConstants.DEFAULT).asInt].bytes) else BlobProxy.generateProxy(Base64.getEncoder().encode("".toByteArray()))
              }
            }
            TypeConstants.FORMULA -> {}
            else -> {
              if (inputJson.has(KeyConstants.DEFAULT)) {
                referencedVariable = variableRepository.findVariable(type = type, name = inputJson.get(KeyConstants.DEFAULT).asString)
                  ?: throw CustomJsonException("{${FunctionConstants.INPUTS}: {$inputName: ${MessageConstants.UNEXPECTED_VALUE}}}")
              }
              variableName = if (inputJson.has(VariableConstants.VARIABLE_NAME)) inputJson.get(VariableConstants.VARIABLE_NAME).asJsonObject.toString() else null
              variableNameKeyDependencies = if (input.asJsonObject.has(VariableConstants.VARIABLE_NAME)) {
                getKeyDependencies(symbolPaths = (validateOrEvaluateExpression(expression = input.asJsonObject.get(VariableConstants.VARIABLE_NAME).asJsonObject,
                  symbols = symbols, mode = LispConstants.VALIDATE, expectedReturnType = TypeConstants.TEXT) as Set<String>).toMutableSet(),
                inputs = inputs.entrySet().fold(mutableMapOf()) { acc, (inputName, input) ->
                  acc.apply { set(inputName, validTypes.single { it.name == input.asJsonObject.get(KeyConstants.KEY_TYPE).asString}) }
                })
              } else mutableSetOf()
            }
          }
        }).apply {
          if (type.name !in primitiveTypes) {
            if (inputJson.has(VariableConstants.VALUES)) {
              val valuesJson: JsonObject = inputJson.get(VariableConstants.VALUES).asJsonObject
              this.values.addAll(
                valuesJson.entrySet().map { (keyName, keyExpression) ->
                  val key: Key = type.keys.single { it.name == keyName }
                  functionInputKeyJpaRepository.save(
                    FunctionInputKey(functionInput = this, key = key, expression = keyExpression.asJsonObject.toString(),
                      keyDependencies = getKeyDependencies(symbolPaths = (validateOrEvaluateExpression(expression = keyExpression.asJsonObject,
                        symbols = symbols, mode = LispConstants.VALIDATE, expectedReturnType = if (key.type.name in primitiveTypes) key.type.name else TypeConstants.TEXT ) as Set<String>).toMutableSet(),
                        inputs = inputs.entrySet().fold(mutableMapOf()) { acc, (inputName, input) ->
                          acc.apply { set(inputName, validTypes.single { it.name == input.asJsonObject.get(KeyConstants.KEY_TYPE).asString}) }
                        }), created = defaultTimestamp
                    )
                  )
                }
              )
            }
          }
        }
      }
    )
    function.outputs.addAll(outputs.entrySet().map { (outputName, output) ->
      val outputJson: JsonObject = output.asJsonObject
      val type: Type = validTypes.single { it.name == outputJson.get(KeyConstants.KEY_TYPE).asString }
      when (type.name) {
        in primitiveTypes -> {
          val valueJson: JsonObject = outputJson.get(FunctionConstants.VALUE).asJsonObject
          FunctionOutput(function = function, name = outputName, type = type, variableName = valueJson.asJsonObject.toString(),
            variableNameKeyDependencies = getKeyDependencies(symbolPaths = (validateOrEvaluateExpression(expression = valueJson.asJsonObject,
              symbols = symbols, mode = LispConstants.VALIDATE, expectedReturnType = type.name) as Set<String>).toMutableSet(),
              inputs = inputs.entrySet().fold(mutableMapOf()) { acc, (inputName, input) ->
                acc.apply { set(inputName, validTypes.single { it.name == input.asJsonObject.get(KeyConstants.KEY_TYPE).asString}) }
              }), created = defaultTimestamp)
        }
        TypeConstants.FORMULA -> throw CustomJsonException("{}")
        else -> {
          val operation: String = outputJson.get(VariableConstants.OPERATION).asString
          functionOutputJpaRepository.save(FunctionOutput(function = function, name = outputName, type = type,
            operation = when(operation) {
              VariableConstants.CREATE -> FunctionConstants.CREATE
              VariableConstants.UPDATE -> FunctionConstants.UPDATE
              VariableConstants.DELETE -> FunctionConstants.DELETE
              else -> throw CustomJsonException("{}")
            },
            variableName = outputJson.get(VariableConstants.VARIABLE_NAME).asJsonObject.toString(),
            variableNameKeyDependencies = getKeyDependencies(symbolPaths = (validateOrEvaluateExpression(expression = outputJson.get(VariableConstants.VARIABLE_NAME).asJsonObject,
              symbols = symbols, mode = LispConstants.VALIDATE, expectedReturnType = TypeConstants.TEXT) as Set<String>).toMutableSet(),
              inputs = inputs.entrySet().fold(mutableMapOf()) { acc, (inputName, input) ->
                acc.apply { set(inputName, validTypes.single { it.name == input.asJsonObject.get(KeyConstants.KEY_TYPE).asString}) }
              }), created = defaultTimestamp)).apply {
            when (operation) {
              VariableConstants.CREATE -> {
                val valuesJson: JsonObject = outputJson.get(VariableConstants.VALUES).asJsonObject
                this.values.addAll(type.keys.filter { it.name != TypeConstants.FORMULA }.map { key ->
                  functionOutputKeyJpaRepository.save(
                    FunctionOutputKey(functionOutput = this, key = key, expression = valuesJson.get(key.name).asJsonObject.toString(),
                      keyDependencies = getKeyDependencies(symbolPaths = (validateOrEvaluateExpression(expression = valuesJson.get(key.name).asJsonObject,
                        symbols = symbols, mode = LispConstants.VALIDATE, expectedReturnType = if (key.type.name in primitiveTypes) key.type.name else TypeConstants.TEXT) as Set<String>).toMutableSet(),
                        inputs = inputs.entrySet().fold(mutableMapOf()) { acc, (inputName, input) ->
                          acc.apply { set(inputName, validTypes.single { it.name == input.asJsonObject.get(KeyConstants.KEY_TYPE).asString}) }
                        }), created = defaultTimestamp))
                })
              }
              VariableConstants.UPDATE -> {
                val valuesJson: JsonObject = outputJson.get(VariableConstants.VALUES).asJsonObject
                this.values.addAll(valuesJson.entrySet().filter { (keyName, _) -> type.keys.any { it.name == keyName && it.type.name != TypeConstants.FORMULA} }
                  .map { (keyName, _) ->
                    val key: Key = type.keys.single { it.name == keyName }
                    functionOutputKeyJpaRepository.save(
                      FunctionOutputKey(functionOutput = this, key = key, expression = valuesJson.get(key.name).asJsonObject.toString(),
                        keyDependencies = getKeyDependencies(symbolPaths = (validateOrEvaluateExpression(expression = valuesJson.get(key.name).asJsonObject,
                          symbols = symbols, mode = LispConstants.VALIDATE, expectedReturnType = if (key.type.name in primitiveTypes) key.type.name else TypeConstants.TEXT) as Set<String>).toMutableSet(),
                          inputs = inputs.entrySet().fold(mutableMapOf()) { acc, (inputName, input) ->
                            acc.apply { set(inputName, validTypes.single { it.name == input.asJsonObject.get(KeyConstants.KEY_TYPE).asString}) }
                          }), created = defaultTimestamp))
                  })
              }
              VariableConstants.DELETE -> {}
              else -> throw CustomJsonException("{}")
            }
          }
        }
      }
    })
    function.permissions.add(functionPermissionService.createDefaultFunctionPermission(function = function, defaultTimestamp = defaultTimestamp))
    function.permissions.addAll(createPermissionsForFunction(jsonParams = jsonParams, defaultTimestamp = defaultTimestamp))
    assignFunctionPermissionsToRoles(jsonParams = jsonParams, defaultTimestamp = defaultTimestamp)
    return function
  }

  fun createPermissionsForFunction(jsonParams: JsonObject, defaultTimestamp: Timestamp): Set<FunctionPermission> = jsonParams.get("permissions").asJsonArray.fold(mutableSetOf()) { acc, jsonPermission ->
    acc.apply {
      try {
        add(functionPermissionService.createFunctionPermission(jsonParams = JsonObject().apply {
          addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
          addProperty(FunctionConstants.FUNCTION_NAME, jsonParams.get(FunctionConstants.FUNCTION_NAME).asString)
          addProperty("permissionName", jsonPermission.asJsonObject.get("permissionName").asString)
          add("permissions", jsonPermission.asJsonObject.get("permissions").asJsonArray)
        }, defaultTimestamp = defaultTimestamp))
      } catch (exception: Exception) {
        throw CustomJsonException("{permissions: {permissionName: 'Unexpected value for parameter'}}")
      }
    }
  }

  fun assignFunctionPermissionsToRoles(jsonParams: JsonObject, defaultTimestamp: Timestamp) {
    for ((roleName, permissionNames) in jsonParams.get("roles").asJsonObject.entrySet()) {
      if (permissionNames.isJsonArray) {
        for (permissionName in permissionNames.asJsonArray) {
          roleService.updateRoleFunctionPermissions(jsonParams = JsonObject().apply {
            addProperty("orgId", jsonParams.get("orgId").asString)
            addProperty(FunctionConstants.FUNCTION_NAME, jsonParams.get(FunctionConstants.FUNCTION_NAME).asString)
            addProperty("roleName", roleName)
            try {
              addProperty("permissionName", permissionName.asString)
            } catch (exception: Exception) {
              throw CustomJsonException("{roles: {${roleName}: 'Unexpected value for parameter'}")
            }
            addProperty("operation", "add")
          }, defaultTimestamp = defaultTimestamp)
        }
      } else throw CustomJsonException("{roles: {${roleName}: 'Unexpected value for parameter'}}")
    }
  }

  @Suppress("UNCHECKED_CAST")
  fun executeFunction(jsonParams: JsonObject, files: MutableList<MultipartFile>, defaultTimestamp: Timestamp): JsonObject {
    val functionPermission: FunctionPermission = userService.superimposeUserFunctionPermissions(jsonParams = JsonObject().apply {
      addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
      addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
      addProperty(FunctionConstants.FUNCTION_NAME, jsonParams.get(FunctionConstants.FUNCTION_NAME).asString)
    }, defaultTimestamp = defaultTimestamp)
    val inputs: Set<FunctionInput> = functionPermission.function.inputs
    val args: JsonObject = validateFunctionArgs(args = jsonParams.get(FunctionConstants.ARGS).asJsonObject, inputs = functionPermission.functionInputPermissions, defaultTimestamp = defaultTimestamp, files = files)
    val symbolPaths: Set<String> = gson.fromJson(functionPermission.function.symbolPaths, JsonArray::class.java).map { it.asString }.toSet()
    val symbols: JsonObject = functionPermission.functionInputPermissions.fold(JsonObject()) { acc, inputPermission ->
      val input: FunctionInput = inputPermission.functionInput
      try {
        acc.apply {
          when (input.type.name) {
            TypeConstants.TEXT -> add(input.name, JsonObject().apply {
              addProperty(SymbolConstants.SYMBOL_TYPE, input.type.name)
              addProperty(SymbolConstants.SYMBOL_VALUE, args.get(input.name).asString)
            })
            TypeConstants.NUMBER -> add(input.name, JsonObject().apply {
              addProperty(SymbolConstants.SYMBOL_TYPE, input.type.name)
              addProperty(SymbolConstants.SYMBOL_VALUE, args.get(input.name).asLong)
            })
            TypeConstants.DECIMAL -> add(input.name, JsonObject().apply {
              addProperty(SymbolConstants.SYMBOL_TYPE, input.type.name)
              addProperty(SymbolConstants.SYMBOL_VALUE, args.get(input.name).asBigDecimal)
            })
            TypeConstants.BOOLEAN -> add(input.name, JsonObject().apply {
              addProperty(SymbolConstants.SYMBOL_TYPE, input.type.name)
              addProperty(SymbolConstants.SYMBOL_VALUE, args.get(input.name).asBoolean)
            })
            TypeConstants.DATE -> add(input.name, JsonObject().apply {
              addProperty(SymbolConstants.SYMBOL_TYPE, input.type.name)
              addProperty(SymbolConstants.SYMBOL_VALUE, args.get(input.name).asString)
            })
            TypeConstants.TIMESTAMP -> add(input.name, JsonObject().apply {
              addProperty(SymbolConstants.SYMBOL_TYPE, input.type.name)
              addProperty(SymbolConstants.SYMBOL_VALUE, args.get(input.name).asLong)
            })
            TypeConstants.TIME -> add(input.name, JsonObject().apply {
              addProperty(SymbolConstants.SYMBOL_TYPE, input.type.name)
              addProperty(SymbolConstants.SYMBOL_VALUE, args.get(input.name).asLong)
            })
            TypeConstants.BLOB -> add(input.name, JsonObject().apply {
              addProperty(SymbolConstants.SYMBOL_TYPE, input.type.name)
              addProperty(SymbolConstants.SYMBOL_VALUE, Base64.getEncoder().encodeToString(files[args.get(input.name).asInt].bytes))
            })
            TypeConstants.FORMULA -> throw CustomJsonException("{}")
            else -> {
              val variable: Variable = variableRepository.findVariable(type = input.type, name = args.get(input.name).asString)!!
              add(input.name, JsonObject().apply {
                addProperty(SymbolConstants.SYMBOL_TYPE, TypeConstants.TEXT)
                addProperty(SymbolConstants.SYMBOL_VALUE, variable.name)
                add(VariableConstants.VALUES, getSymbolValues(symbolPaths = symbolPaths.toMutableSet(), variable = variable, prefix = input.name + ".", excludeTopLevelFormulas = false))
              })
            }
          }
        }
      } catch (exception: Exception) {
        throw CustomJsonException("{${FunctionConstants.ARGS}: {${input.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
      }
    }
    inputs.filter { it.type.name !in primitiveTypes && it.type.name != TypeConstants.FORMULA }.forEach { input ->
      try {
        variableService.updateVariable(jsonParams = JsonObject().apply {
          addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
          addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
          addProperty(OrganizationConstants.TYPE_NAME, input.type.name)
          addProperty(VariableConstants.VARIABLE_NAME, args.get(input.name).asString)
          if (input.variableName != null)
            addProperty("${VariableConstants.UPDATED_VARIABLE_NAME}?",
              validateOrEvaluateExpression(expression = gson.fromJson(input.variableName, JsonObject::class.java), symbols = symbols, mode = LispConstants.EVALUATE, expectedReturnType = TypeConstants.TEXT) as String)
          add(VariableConstants.VALUES, input.values.fold(JsonObject()) { acc, value ->
            acc.apply {
              val evaluatedValue = validateOrEvaluateExpression(expression = gson.fromJson(value.expression, JsonObject::class.java), symbols = symbols,
                mode = LispConstants.EVALUATE, expectedReturnType = if (value.key.type.name in primitiveTypes) value.key.type.name else TypeConstants.TEXT)
              when(value.key.type.name) {
                TypeConstants.TEXT -> addProperty(value.key.name, evaluatedValue as String)
                TypeConstants.NUMBER -> addProperty(value.key.name, evaluatedValue as Long)
                TypeConstants.DECIMAL -> addProperty(value.key.name, evaluatedValue as BigDecimal)
                TypeConstants.BOOLEAN -> addProperty(value.key.name, evaluatedValue as Boolean)
                TypeConstants.DATE -> addProperty(value.key.name, (evaluatedValue as Date).toString())
                TypeConstants.TIMESTAMP -> addProperty(value.key.name, (evaluatedValue as Timestamp).time)
                TypeConstants.TIME -> addProperty(value.key.name, (evaluatedValue as Time).time)
                TypeConstants.BLOB -> addProperty(value.key.name, files.apply { add(Base64DecodedMultipartFile(evaluatedValue as ByteArray)) }.size - 1)
                TypeConstants.FORMULA -> throw CustomJsonException("{}")
                else -> addProperty(value.key.name, evaluatedValue as String)
              }
            }
          })
        }, defaultTimestamp = defaultTimestamp, files = files)
      } catch (exception: CustomJsonException) {
        throw CustomJsonException("{${FunctionConstants.INPUTS}: {${input.name}: ${exception.message}}}")
      }
    }
    return functionPermission.functionOutputPermissions.fold(JsonObject()) { acc, outputPermission ->
      acc.apply {
        val output: FunctionOutput = outputPermission.functionOutput
        try {
          val evaluatedValue = validateOrEvaluateExpression(expression = gson.fromJson(output.variableName, JsonObject::class.java), symbols = symbols,
            mode = LispConstants.EVALUATE, expectedReturnType = if (output.type.name in primitiveTypes) output.type.name else TypeConstants.TEXT)
          when (output.type.name) {
            TypeConstants.TEXT -> if (outputPermission.accessLevel) addProperty(output.name, evaluatedValue as String)
            TypeConstants.NUMBER -> if (outputPermission.accessLevel) addProperty(output.name, evaluatedValue as Long)
            TypeConstants.DECIMAL -> if (outputPermission.accessLevel) addProperty(output.name, evaluatedValue as BigDecimal)
            TypeConstants.BOOLEAN -> if (outputPermission.accessLevel) addProperty(output.name, evaluatedValue as Boolean)
            TypeConstants.DATE -> if (outputPermission.accessLevel) addProperty(output.name, (evaluatedValue as Date).toString())
            TypeConstants.TIMESTAMP -> if (outputPermission.accessLevel) addProperty(output.name, (evaluatedValue as Timestamp).time)
            TypeConstants.TIME -> if (outputPermission.accessLevel) addProperty(output.name, (evaluatedValue as Time).time)
            TypeConstants.BLOB -> if (outputPermission.accessLevel) addProperty(output.name, Base64.getEncoder().encodeToString(evaluatedValue as ByteArray))
            TypeConstants.FORMULA -> throw CustomJsonException("{}")
            else -> {
              val variableParams: JsonObject = JsonObject().apply {
                addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
                addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
                addProperty(OrganizationConstants.TYPE_NAME, output.type.name)
                addProperty(VariableConstants.VARIABLE_NAME, evaluatedValue as String)
                if (output.operation != FunctionConstants.DELETE)
                  add(VariableConstants.VALUES, output.values.filter { value -> value.key.type.name != TypeConstants.FORMULA }.fold(JsonObject()) { acc, value ->
                    acc.apply {
                      val keyValue = validateOrEvaluateExpression(expression = gson.fromJson(value.expression, JsonObject::class.java), symbols = symbols,
                        mode = LispConstants.EVALUATE, expectedReturnType = if (value.key.type.name in primitiveTypes) value.key.type.name else TypeConstants.TEXT)
                      when(value.key.type.name) {
                        TypeConstants.TEXT -> addProperty(value.key.name, keyValue as String)
                        TypeConstants.NUMBER -> addProperty(value.key.name, keyValue as Long)
                        TypeConstants.DECIMAL -> addProperty(value.key.name, keyValue as BigDecimal)
                        TypeConstants.BOOLEAN -> addProperty(value.key.name, keyValue as Boolean)
                        TypeConstants.DATE -> addProperty(value.key.name, (keyValue as Date).toString())
                        TypeConstants.TIMESTAMP -> addProperty(value.key.name, (keyValue as Timestamp).time)
                        TypeConstants.TIME -> addProperty(value.key.name, (keyValue as Time).time)
                        TypeConstants.BLOB -> addProperty(value.key.name, files.apply { add(Base64DecodedMultipartFile(keyValue as ByteArray)) }.size - 1)
                        TypeConstants.FORMULA -> throw CustomJsonException("{}")
                        else -> addProperty(value.key.name, keyValue as String)
                      }
                    }
                  })
              }
              val (variable: Variable, typePermission: TypePermission) = when(output.operation) {
                FunctionConstants.CREATE -> variableService.createVariable(jsonParams = variableParams, defaultTimestamp = defaultTimestamp, files = files)
                FunctionConstants.UPDATE -> variableService.updateVariable(jsonParams = variableParams, defaultTimestamp = defaultTimestamp, files = files)
                FunctionConstants.DELETE -> variableService.deleteVariable(jsonParams = variableParams, defaultTimestamp = defaultTimestamp)
                else -> throw CustomJsonException("{}")
              }
              if (outputPermission.accessLevel)
                add(output.type.name, variableService.serialize(variable = variable, typePermission = typePermission))
            }
          }
        } catch (exception: CustomJsonException) {
          throw CustomJsonException("{${FunctionConstants.OUTPUTS}: {${output.name}: ${exception.message}}}")
        }
      }
    }
  }
}
