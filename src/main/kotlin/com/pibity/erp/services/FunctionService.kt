/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.GLOBAL_TYPE
import com.pibity.erp.commons.constants.KeyConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.lisp.getSymbolPaths
import com.pibity.erp.commons.utils.*
import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.Variable
import com.pibity.erp.entities.function.*
import com.pibity.erp.entities.function.Function
import com.pibity.erp.entities.permission.FunctionPermission
import com.pibity.erp.repositories.function.FunctionInputRepository
import com.pibity.erp.repositories.function.FunctionOutputRepository
import com.pibity.erp.repositories.function.FunctionRepository
import com.pibity.erp.repositories.function.jpa.*
import com.pibity.erp.repositories.jpa.OrganizationJpaRepository
import com.pibity.erp.repositories.query.TypeRepository
import com.pibity.erp.repositories.query.VariableRepository
import com.pibity.erp.serializers.serialize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FunctionService(
    val organizationJpaRepository: OrganizationJpaRepository,
    val typeRepository: TypeRepository,
    val functionRepository: FunctionRepository,
    val functionJpaRepository: FunctionJpaRepository,
    val functionInputRepository: FunctionInputRepository,
    val functionInputJpaRepository: FunctionInputJpaRepository,
    val functionInputTypeJpaRepository: FunctionInputTypeJpaRepository,
    val functionOutputRepository: FunctionOutputRepository,
    val functionOutputJpaRepository: FunctionOutputJpaRepository,
    val functionOutputTypeJpaRepository: FunctionOutputTypeJpaRepository,
    val functionPermissionService: FunctionPermissionService,
    val variableRepository: VariableRepository,
    val variableService: VariableService,
    val roleService: RoleService,
    val userService: UserService

) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createFunction(jsonParams: JsonObject): Function {
    val organization: Organization = organizationJpaRepository.getById(jsonParams.get("orgId").asLong)
        ?: throw CustomJsonException("{orgId: 'Organization could not be found'}")
    val globalTypes: MutableSet<Type> = typeRepository.findGlobalTypes(organizationId = organization.id) as MutableSet<Type>
    val symbolPaths: MutableSet<String> = mutableSetOf()
    symbolPaths.apply {
      addAll(getInputSymbolPaths(jsonParams = jsonParams.get("inputs").asJsonObject, globalTypes = globalTypes))
      addAll(getOutputSymbolPaths(jsonParams = jsonParams.get("outputs").asJsonObject, globalTypes = globalTypes))
    }
    val symbols: JsonObject = getInputSymbols(inputs = jsonParams.get("inputs").asJsonObject, globalTypes = globalTypes, symbolPaths = symbolPaths)
    val inputs: JsonObject = validateInputs(jsonParams = jsonParams.get("inputs").asJsonObject, globalTypes = globalTypes, symbolPaths = symbolPaths, symbols = symbols)
    val outputs: JsonObject = validateOutputs(jsonParams = jsonParams.get("outputs").asJsonObject, globalTypes = globalTypes, symbolPaths = symbolPaths, symbols = symbols)
    var function = Function(organization = organization, name = validateFunctionName(jsonParams.get("functionName").asString), symbolPaths = gson.toJson(getSymbolPaths(jsonParams = symbols)))
    function = functionJpaRepository.save(function)
    inputs.entrySet().forEach { (inputName, input) ->
      val type: Type = if (input.isJsonObject)
        globalTypes.single { it.name == input.asJsonObject.get(KeyConstants.KEY_TYPE).asString }
      else
        globalTypes.single { it.name == input.asString }
      var functionInput = FunctionInput(function = function, name = inputName, type = type,
          variableName = when (type.name) {
            TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.FORMULA, TypeConstants.LIST -> null
            else -> if (input.asJsonObject.has("variableName"))
              input.asJsonObject.get("variableName").asJsonObject.toString()
            else null
          },
          variableNameKeyDependencies = when (type.name) {
            TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.FORMULA, TypeConstants.LIST -> mutableSetOf()
            else -> if (input.asJsonObject.has("variableName"))
              getInputKeyDependencies(inputs = inputs, globalTypes = globalTypes, symbolPaths = validateOrEvaluateExpression(jsonParams = input.asJsonObject.get("variableName").asJsonObject.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) }, mode = "collect", symbols = JsonObject()) as MutableSet<String>).toMutableSet()
            else mutableSetOf()
          }
      )
      if (input.isJsonObject) {
        when (type.name) {
          TypeConstants.TEXT -> functionInput.defaultStringValue = if (input.asJsonObject.has(KeyConstants.DEFAULT)) input.asJsonObject.get(KeyConstants.DEFAULT).asString else ""
          TypeConstants.NUMBER -> functionInput.defaultLongValue = if (input.asJsonObject.has(KeyConstants.DEFAULT)) input.asJsonObject.get(KeyConstants.DEFAULT).asLong else 0
          TypeConstants.DECIMAL -> functionInput.defaultDoubleValue = if (input.asJsonObject.has(KeyConstants.DEFAULT)) input.asJsonObject.get(KeyConstants.DEFAULT).asDouble else 0.0
          TypeConstants.BOOLEAN -> functionInput.defaultBooleanValue = if (input.asJsonObject.has(KeyConstants.DEFAULT)) input.asJsonObject.get(KeyConstants.DEFAULT).asBoolean else false
          TypeConstants.FORMULA, TypeConstants.LIST -> {
          }
          else -> if (input.asJsonObject.has(KeyConstants.DEFAULT)) {
            functionInput.referencedVariable = variableRepository.findVariable(organizationId = organization.id, superList = organization.superList!!.id, superTypeName = GLOBAL_TYPE, typeName = type.name, name = input.asJsonObject.get(KeyConstants.DEFAULT).asString)
                ?: throw CustomJsonException("{inputs: {${inputName}: {${KeyConstants.DEFAULT}: 'Unexpected value for parameter'}}}")
          }
        }
      }
      functionInput = functionInputJpaRepository.save(functionInput)
      if (input.isJsonObject && input.asJsonObject.has("values")) {
        functionInput.values = saveFunctionInputType(inputs = inputs, globalTypes = globalTypes, functionInput = FunctionInput(function = function, name = inputName, type = type), type = type, values = input.asJsonObject.get("values").asJsonObject)
        functionInputJpaRepository.save(functionInput)
      }
      function.inputs.add(functionInput)
    }
    outputs.entrySet().forEach { (outputName, output) ->
      val type: Type = globalTypes.single { it.name == output.asJsonObject.get(KeyConstants.KEY_TYPE).asString }
      var functionOutput = FunctionOutput(function = function, name = outputName, type = type,
          variableName = when (type.name) {
            TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN ->
              output.asJsonObject.get("values").asJsonObject.toString()
            TypeConstants.FORMULA, TypeConstants.LIST -> ""
            else -> output.asJsonObject.get("variableName").asJsonObject.toString()
          },
          variableNameKeyDependencies = when (type.name) {
            TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN ->
              getInputKeyDependencies(inputs = inputs, globalTypes = globalTypes, symbolPaths = validateOrEvaluateExpression(jsonParams = output.asJsonObject.get("values").asJsonObject.deepCopy().apply { addProperty("expectedReturnType", type.name) }, mode = "collect", symbols = JsonObject()) as MutableSet<String>).toMutableSet()
            TypeConstants.FORMULA, TypeConstants.LIST -> mutableSetOf()
            else -> getInputKeyDependencies(inputs = inputs, globalTypes = globalTypes, symbolPaths = validateOrEvaluateExpression(jsonParams = output.asJsonObject.get("variableName").asJsonObject.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) }, mode = "collect", symbols = JsonObject()) as MutableSet<String>).toMutableSet()
          })
      functionOutput = functionOutputJpaRepository.save(functionOutput)
      when (type.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.FORMULA, TypeConstants.LIST -> {
        }
        else -> {
          functionOutput.values = saveFunctionOutputType(inputs = inputs, globalTypes = globalTypes, functionOutput = functionOutput, type = type, values = output.asJsonObject.get("values").asJsonObject)
          functionOutputJpaRepository.save(functionOutput)
        }
      }
      function.outputs.add(functionOutput)
    }
    function.permissions.add(functionPermissionService.createDefaultFunctionPermission(function = function))
    createPermissionsForFunction(jsonParams = jsonParams)
    assignFunctionPermissionsToRoles(jsonParams = jsonParams)
    return function
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createPermissionsForFunction(jsonParams: JsonObject) {
    for (jsonPermission in jsonParams.get("permissions").asJsonArray) {
      if (jsonPermission.isJsonObject) {
        functionPermissionService.createFunctionPermission(jsonParams = JsonObject().apply {
          addProperty("orgId", jsonParams.get("orgId").asString)
          addProperty("functionName", jsonParams.get("functionName").asString)
          try {
            addProperty("permissionName", jsonParams.get("permissionName").asString)
          } catch (exception: Exception) {
            throw CustomJsonException("{permissions: {permissionName: 'Unexpected value for parameter'}}")
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
  fun assignFunctionPermissionsToRoles(jsonParams: JsonObject) {
    for ((roleName, permissionNames) in jsonParams.get("roles").asJsonObject.entrySet()) {
      if (permissionNames.isJsonArray) {
        for (permissionName in permissionNames.asJsonArray) {
          roleService.updateRoleFunctionPermissions(jsonParams = JsonObject().apply {
            addProperty("orgId", jsonParams.get("orgId").asString)
            addProperty("functionName", jsonParams.get("functionName").asString)
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
  fun executeFunction(jsonParams: JsonObject): JsonObject {
    val functionPermission: FunctionPermission =
        userService.superimposeUserFunctionPermissions(jsonParams = JsonObject().apply {
          addProperty("orgId", jsonParams.get("orgId").asString)
          addProperty("username", jsonParams.get("username").asString)
          addProperty("functionName", jsonParams.get("functionName").asString)
        })
    val function: Function = functionPermission.function
    val inputs: Set<FunctionInput> = functionInputRepository.getFunctionInputs(organizationId = functionPermission.function.organization.id, functionName = jsonParams.get("functionName").asString)
    val args: JsonObject = validateFunctionArgs(args = jsonParams.get("args").asJsonObject, inputs = inputs)
    val symbolPaths: Set<String> = gson.fromJson(function.symbolPaths, JsonArray::class.java).map { it.asString }.toSet()
    val symbols = JsonObject()
    for (inputPermission in functionPermission.functionInputPermissions) {
      val input: FunctionInput = inputPermission.functionInput
      when (input.type.name) {
        TypeConstants.TEXT -> symbols.add(input.name, JsonObject().apply {
          addProperty(KeyConstants.KEY_TYPE, input.type.name)
          addProperty(KeyConstants.VALUE, if (inputPermission.accessLevel) args.get(input.name).asString else try {
            input.defaultStringValue!!
          } catch (exception: Exception) {
            throw CustomJsonException("{error: 'Unauthorized Access'}")})
        })
        TypeConstants.NUMBER -> symbols.add(input.name, JsonObject().apply {
          addProperty(KeyConstants.KEY_TYPE, input.type.name)
          addProperty(KeyConstants.VALUE, if (inputPermission.accessLevel) args.get(input.name).asLong else try {
            input.defaultLongValue!!
          } catch (exception: Exception) {
            throw CustomJsonException("{error: 'Unauthorized Access'}")})
        })
        TypeConstants.DECIMAL -> symbols.add(input.name, JsonObject().apply {
          addProperty(KeyConstants.KEY_TYPE, input.type.name)
          addProperty(KeyConstants.VALUE, if (inputPermission.accessLevel) args.get(input.name).asDouble else try {
            input.defaultDoubleValue!!
          } catch (exception: Exception) {
            throw CustomJsonException("{error: 'Unauthorized Access'}")})
        })
        TypeConstants.BOOLEAN -> symbols.add(input.name, JsonObject().apply {
          addProperty(KeyConstants.KEY_TYPE, input.type.name)
          addProperty(KeyConstants.VALUE, if (inputPermission.accessLevel) args.get(input.name).asBoolean else try {
            input.defaultBooleanValue!!
          } catch (exception: Exception) {
            throw CustomJsonException("{error: 'Unauthorized Access'}")})
        })
        TypeConstants.FORMULA, TypeConstants.LIST -> {
        }
        else -> {
          val variable: Variable = if (inputPermission.accessLevel) variableRepository.findVariable(organizationId = functionPermission.function.organization.id,
              superTypeName = GLOBAL_TYPE, typeName = input.type.name,
              superList = functionPermission.function.organization.superList!!.id,
              name = args.get(input.name).asString)
              ?: throw CustomJsonException("{args: {${input.name}: 'Unexpected value for parameter'}}")
          else try {
            input.referencedVariable!!
          } catch (exception: Exception) {
            throw CustomJsonException("{error: 'Unauthorized Access'}")
          }
          symbols.add(input.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
            addProperty(KeyConstants.VALUE, variable.name)
            add("values", getSymbolsForFunctionArgs(symbolPaths = symbolPaths, variable = variable, prefix = input.name + "."))
          })
        }
      }
    }
    for (input in inputs) {
      when (input.type.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.LIST, TypeConstants.FORMULA -> {
        }
        else -> {
          if (input.variableName != null || input.values != null) {
            try {
              variableService.updateVariable(jsonParams = JsonObject().apply {
                addProperty("orgId", jsonParams.get("orgId").asString)
                addProperty("username", jsonParams.get("username").asString)
                addProperty("typeName", input.type.name)
                addProperty("variableName", args.get(input.name).asString)
                if (input.variableName != null) {
                  addProperty("updatedVariableName", validateOrEvaluateExpression(jsonParams = gson.fromJson(input.variableName, JsonObject::class.java).apply {
                    addProperty("expectedReturnType", TypeConstants.TEXT)
                  }, mode = "evaluate", symbols = symbols) as String)
                }
                if (input.values != null)
                  add("values", getFunctionInputTypeJson(functionInputType = input.values!!, symbols = symbols))
              })
            } catch (exception: CustomJsonException) {
              throw CustomJsonException("{inputs: {${input.name}: ${exception.message}}}")
            }
          }
        }
      }
    }
    val results = JsonObject()
    for (outputPermission in functionPermission.functionOutputPermissions) {
      val output = outputPermission.functionOutput
      when (output.type.name) {
        TypeConstants.TEXT -> if (outputPermission.accessLevel) results.addProperty(output.name, validateOrEvaluateExpression(jsonParams = gson.fromJson(output.variableName, JsonObject::class.java).apply {
          addProperty("expectedReturnType", output.type.name)
        }, mode = "evaluate", symbols = symbols) as String)
        TypeConstants.NUMBER -> if (outputPermission.accessLevel) results.addProperty(output.name, validateOrEvaluateExpression(jsonParams = gson.fromJson(output.variableName, JsonObject::class.java).apply {
          addProperty("expectedReturnType", output.type.name)
        }, mode = "evaluate", symbols = symbols) as Long)
        TypeConstants.DECIMAL -> if (outputPermission.accessLevel) results.addProperty(output.name, validateOrEvaluateExpression(jsonParams = gson.fromJson(output.variableName, JsonObject::class.java).apply {
          addProperty("expectedReturnType", output.type.name)
        }, mode = "evaluate", symbols = symbols) as Double)
        TypeConstants.BOOLEAN -> if (outputPermission.accessLevel) results.addProperty(output.name, validateOrEvaluateExpression(jsonParams = gson.fromJson(output.variableName, JsonObject::class.java).apply {
          addProperty("expectedReturnType", output.type.name)
        }, mode = "evaluate", symbols = symbols) as Boolean)
        TypeConstants.FORMULA, TypeConstants.LIST -> {
        }
        else -> {
          val (variable: Variable, _) = variableService.createVariable(jsonParams = JsonObject().apply {
            addProperty("orgId", jsonParams.get("orgId").asString)
            addProperty("username", jsonParams.get("username").asString)
            addProperty("typeName", output.type.name)
            addProperty("variableName", validateOrEvaluateExpression(jsonParams = gson.fromJson(output.variableName, JsonObject::class.java).apply {
              addProperty("expectedReturnType", TypeConstants.TEXT)
            }, mode = "evaluate", symbols = symbols) as String)
            add("values", getFunctionOutputTypeJson(functionOutputType = output.values!!, symbols = symbols))
          })
          if (outputPermission.accessLevel) results.add(output.name, serialize(try {
            variable
          } catch (exception: CustomJsonException) {
            throw CustomJsonException("{outputs: {${output.name}: ${exception.message}}}")
          }))
        }
      }
    }
    return results
  }

  fun saveFunctionInputType(inputs: JsonObject, globalTypes: Set<Type>, functionInput: FunctionInput, type: Type, values: JsonObject): FunctionInputType {
    val functionInputType = FunctionInputType(functionInput = functionInput, type = type)
    for (key in type.keys) {
      if (values.has(key.name)) {
        when (key.type.name) {
          TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN ->
            functionInputType.functionInputKeys.add(FunctionInputKey(functionInputType = functionInputType, key = key,
                expression = values.get(key.name).asJsonObject.toString(),
                keyDependencies = getInputKeyDependencies(inputs = inputs, globalTypes = globalTypes, symbolPaths = validateOrEvaluateExpression(jsonParams = values.get(key.name).asJsonObject.deepCopy().apply { addProperty("expectedReturnType", key.type.name) }, mode = "collect", symbols = JsonObject()) as MutableSet<String>).toMutableSet()))
          TypeConstants.FORMULA, TypeConstants.LIST -> {
          }
          else -> {
            if (key.type.superTypeName == GLOBAL_TYPE) {
              functionInputType.functionInputKeys.add(FunctionInputKey(functionInputType = functionInputType, key = key,
                  expression = values.get(key.name).asJsonObject.toString(),
                  keyDependencies = getInputKeyDependencies(inputs = inputs, globalTypes = globalTypes, symbolPaths = validateOrEvaluateExpression(jsonParams = values.get(key.name).asJsonObject.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) }, mode = "collect", symbols = JsonObject()) as MutableSet<String>).toMutableSet()))
            } else {
              if ((key.parentType.superTypeName == GLOBAL_TYPE && key.parentType.name == key.type.superTypeName)
                  || (key.parentType.superTypeName != GLOBAL_TYPE && key.parentType.superTypeName == key.type.superTypeName)) {
                functionInputType.functionInputKeys.add(FunctionInputKey(functionInputType = functionInputType, key = key,
                    referencedFunctionInputType = saveFunctionInputType(inputs = inputs, globalTypes = globalTypes, functionInput = functionInput, type = key.type, values = values.get(key.name).asJsonObject)))
              } else {
                val symbolPaths: MutableSet<String> = mutableSetOf()
                symbolPaths.apply {
                  addAll(validateOrEvaluateExpression(jsonParams = values.get(key.name).asJsonObject.get("context").asJsonObject.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.NUMBER) },
                      mode = "collect", symbols = JsonObject()) as MutableSet<String>)
                  addAll(validateOrEvaluateExpression(jsonParams = values.get(key.name).asJsonObject.get("variableName").asJsonObject.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) },
                      mode = "collect", symbols = JsonObject()) as MutableSet<String>)
                }
                functionInputType.functionInputKeys.add(FunctionInputKey(functionInputType = functionInputType, key = key,
                    expression = values.get(key.name).asJsonObject.toString(),
                    keyDependencies = getInputKeyDependencies(inputs = inputs, globalTypes = globalTypes, symbolPaths = symbolPaths).toMutableSet()))
              }
            }
          }
        }
      }
    }
    return functionInputTypeJpaRepository.save(functionInputType)
  }

  fun saveFunctionOutputType(inputs: JsonObject, globalTypes: Set<Type>, functionOutput: FunctionOutput, type: Type, values: JsonObject): FunctionOutputType {
    val functionOutputType = FunctionOutputType(functionOutput = functionOutput, type = type)
    for (key in type.keys) {
      when (key.type.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN ->
          functionOutputType.functionOutputKeys.add(FunctionOutputKey(functionOutputType = functionOutputType, key = key,
              expression = values.get(key.name).asJsonObject.toString(),
              keyDependencies = getInputKeyDependencies(inputs = inputs, globalTypes = globalTypes, symbolPaths = validateOrEvaluateExpression(jsonParams = values.get(key.name).asJsonObject.deepCopy().apply { addProperty("expectedReturnType", key.type.name) }, mode = "collect", symbols = JsonObject()) as MutableSet<String>).toMutableSet()))
        TypeConstants.FORMULA, TypeConstants.LIST -> {
        }
        else -> {
          if (key.type.superTypeName == GLOBAL_TYPE) {
            functionOutputType.functionOutputKeys.add(FunctionOutputKey(functionOutputType = functionOutputType, key = key,
                expression = values.get(key.name).asJsonObject.toString(),
                keyDependencies = getInputKeyDependencies(inputs = inputs, globalTypes = globalTypes, symbolPaths = validateOrEvaluateExpression(jsonParams = values.get(key.name).asJsonObject.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) }, mode = "collect", symbols = JsonObject()) as MutableSet<String>).toMutableSet()))
          } else {
            if ((key.parentType.superTypeName == GLOBAL_TYPE && key.parentType.name == key.type.superTypeName)
                || (key.parentType.superTypeName != GLOBAL_TYPE && key.parentType.superTypeName == key.type.superTypeName)) {
              functionOutputType.functionOutputKeys.add(FunctionOutputKey(functionOutputType = functionOutputType, key = key,
                  referencedFunctionOutputType = saveFunctionOutputType(inputs = inputs, globalTypes = globalTypes, functionOutput = functionOutput, type = key.type, values = values.get(key.name).asJsonObject)))
            } else {
              val symbolPaths: MutableSet<String> = mutableSetOf()
              symbolPaths.apply {
                addAll(validateOrEvaluateExpression(jsonParams = values.get(key.name).asJsonObject.get("context").asJsonObject.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.NUMBER) },
                    mode = "collect", symbols = JsonObject()) as MutableSet<String>)
                addAll(validateOrEvaluateExpression(jsonParams = values.get(key.name).asJsonObject.get("variableName").asJsonObject.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) },
                    mode = "collect", symbols = JsonObject()) as MutableSet<String>)
              }
              functionOutputType.functionOutputKeys.add(FunctionOutputKey(functionOutputType = functionOutputType, key = key,
                  expression = values.get(key.name).asJsonObject.toString(),
                  keyDependencies = getInputKeyDependencies(inputs = inputs, globalTypes = globalTypes, symbolPaths = symbolPaths).toMutableSet()))
            }
          }
        }
      }
    }
    return functionOutputTypeJpaRepository.save(functionOutputType)
  }
}
