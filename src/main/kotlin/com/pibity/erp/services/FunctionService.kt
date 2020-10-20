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
import com.pibity.erp.entities.function.embeddables.*
import com.pibity.erp.repositories.OrganizationRepository
import com.pibity.erp.repositories.TypeRepository
import com.pibity.erp.repositories.VariableRepository
import com.pibity.erp.repositories.function.*
import com.pibity.erp.serializers.serialize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FunctionService(
    val organizationRepository: OrganizationRepository,
    val typeRepository: TypeRepository,
    val functionRepository: FunctionRepository,
    val functionInputRepository: FunctionInputRepository,
    val functionInputTypeRepository: FunctionInputTypeRepository,
    val functionOutputRepository: FunctionOutputRepository,
    val functionOutputTypeRepository: FunctionOutputTypeRepository,
    val functionPermissionService: FunctionPermissionService,
    val variableRepository: VariableRepository,
    val variableService: VariableService,
    val roleService: RoleService
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createFunction(jsonParams: JsonObject): Function {
    println(jsonParams)
    val organization: Organization = organizationRepository.getById(jsonParams.get("organization").asString)
        ?: throw CustomJsonException("{organization: 'Organization could not be found'}")
    val globalTypes: MutableSet<Type> = typeRepository.findGlobalTypes(organizationName = organization.id) as MutableSet<Type>
    val symbolPaths: MutableSet<String> = mutableSetOf()
    symbolPaths.apply {
      addAll(getInputSymbolPaths(jsonParams = jsonParams.get("inputs").asJsonObject, globalTypes = globalTypes))
      addAll(getOutputSymbolPaths(jsonParams = jsonParams.get("outputs").asJsonObject, globalTypes = globalTypes))
    }
    val symbols: JsonObject = getInputSymbols(inputs = jsonParams.get("inputs").asJsonObject, globalTypes = globalTypes, symbolPaths = symbolPaths)
    val inputs: JsonObject = validateInputs(jsonParams = jsonParams.get("inputs").asJsonObject, globalTypes = globalTypes, symbolPaths = symbolPaths, symbols = symbols)
    val outputs: JsonObject = validateOutputs(jsonParams = jsonParams.get("outputs").asJsonObject, globalTypes = globalTypes, symbolPaths = symbolPaths, symbols = symbols)
    val function = Function(id = FunctionId(organization = organization, name = validateFunctionName(jsonParams.get("functionName").asString)), symbolPaths = gson.toJson(getSymbolPaths(jsonParams = symbols)))
    functionRepository.save(function)
    inputs.entrySet().forEach { (inputName, input) ->
      val type: Type = if (input.isJsonObject)
        globalTypes.single { it.id.name == input.asJsonObject.get(KeyConstants.KEY_TYPE).asString }
      else
        globalTypes.single { it.id.name == input.asString }
      val functionInput = FunctionInput(id = FunctionInputId(function = function, name = inputName), type = type,
          variableName = when (type.id.name) {
            TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.FORMULA, TypeConstants.LIST -> null
            else -> if (input.asJsonObject.has("variableName"))
              input.asJsonObject.get("variableName").asJsonObject.toString()
            else null
          },
          variableNameKeyDependencies = when (type.id.name) {
            TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.FORMULA, TypeConstants.LIST -> mutableSetOf()
            else -> if (input.asJsonObject.has("variableName"))
              getInputKeyDependencies(inputs = inputs, globalTypes = globalTypes, symbolPaths = validateOrEvaluateExpression(jsonParams = input.asJsonObject.get("variableName").asJsonObject.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) }, mode = "collect", symbols = JsonObject()) as MutableSet<String>).toMutableSet()
            else mutableSetOf()
          }
      )
      if (input.isJsonObject) {
        when (type.id.name) {
          TypeConstants.TEXT -> functionInput.defaultStringValue = if (input.asJsonObject.has(KeyConstants.DEFAULT)) input.asJsonObject.get(KeyConstants.DEFAULT).asString else ""
          TypeConstants.NUMBER -> functionInput.defaultLongValue = if (input.asJsonObject.has(KeyConstants.DEFAULT)) input.asJsonObject.get(KeyConstants.DEFAULT).asLong else 0
          TypeConstants.DECIMAL -> functionInput.defaultDoubleValue = if (input.asJsonObject.has(KeyConstants.DEFAULT)) input.asJsonObject.get(KeyConstants.DEFAULT).asDouble else 0.0
          TypeConstants.BOOLEAN -> functionInput.defaultBooleanValue = if (input.asJsonObject.has(KeyConstants.DEFAULT)) input.asJsonObject.get(KeyConstants.DEFAULT).asBoolean else false
          TypeConstants.FORMULA, TypeConstants.LIST -> {
          }
          else -> if (input.asJsonObject.has(KeyConstants.DEFAULT)) {
            functionInput.referencedVariable = variableRepository.findVariable(organizationName = organization.id, superList = organization.superList!!.id, superTypeName = GLOBAL_TYPE, typeName = type.id.name, name = input.asJsonObject.get(KeyConstants.DEFAULT).asString)
                ?: throw CustomJsonException("{inputs: {${inputName}: {${KeyConstants.DEFAULT}: 'Unexpected value for parameter'}}}")
          }
        }
      }
      functionInputRepository.save(functionInput)
      if (input.isJsonObject && input.asJsonObject.has("values")) {
        functionInput.values = saveFunctionInputType(inputs = inputs, globalTypes = globalTypes, functionInput = FunctionInput(id = FunctionInputId(function = function, name = inputName), type = type), type = type, values = input.asJsonObject.get("values").asJsonObject)
        functionInputRepository.save(functionInput)
      }
      function.inputs.add(functionInput)
    }
    outputs.entrySet().forEach { (outputName, output) ->
      val type: Type = globalTypes.single { it.id.name == output.asJsonObject.get(KeyConstants.KEY_TYPE).asString }
      val functionOutput = FunctionOutput(id = FunctionOutputId(function = function, name = outputName), type = type,
          variableName = when (type.id.name) {
            TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN ->
              output.asJsonObject.get("values").asJsonObject.toString()
            TypeConstants.FORMULA, TypeConstants.LIST -> ""
            else -> output.asJsonObject.get("variableName").asJsonObject.toString()
          },
          variableNameKeyDependencies = when (type.id.name) {
            TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN ->
              getInputKeyDependencies(inputs = inputs, globalTypes = globalTypes, symbolPaths = validateOrEvaluateExpression(jsonParams = output.asJsonObject.get("values").asJsonObject.deepCopy().apply { addProperty("expectedReturnType", type.id.name) }, mode = "collect", symbols = JsonObject()) as MutableSet<String>).toMutableSet()
            TypeConstants.FORMULA, TypeConstants.LIST -> mutableSetOf()
            else -> getInputKeyDependencies(inputs = inputs, globalTypes = globalTypes, symbolPaths = validateOrEvaluateExpression(jsonParams = output.asJsonObject.get("variableName").asJsonObject.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) }, mode = "collect", symbols = JsonObject()) as MutableSet<String>).toMutableSet()
          })
      functionOutputRepository.save(functionOutput)
      when (type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.FORMULA, TypeConstants.LIST -> {
        }
        else -> {
          functionOutput.values = saveFunctionOutputType(inputs = inputs, globalTypes = globalTypes, functionOutput = functionOutput, type = type, values = output.asJsonObject.get("values").asJsonObject)
          functionOutputRepository.save(functionOutput)
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
          addProperty("organization", jsonParams.get("organization").asString)
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
            addProperty("organization", jsonParams.get("organization").asString)
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
    println(jsonParams)
    val organization: Organization = organizationRepository.getById(jsonParams.get("organization").asString)
        ?: throw CustomJsonException("{organization: 'Organization could not be found'}")
    val function: Function = functionRepository.findFunction(organizationName = organization.id, name = jsonParams.get("functionName").asString)
        ?: throw CustomJsonException("{functionName: 'Function could not be determined'}")
    val inputs: Set<FunctionInput> = functionInputRepository.getFunctionInputs(organizationName = organization.id, functionName = jsonParams.get("functionName").asString)
    val args: JsonObject = validateFunctionArgs(args = jsonParams.get("args").asJsonObject, inputs = inputs)
    val symbolPaths: Set<String> = gson.fromJson(function.symbolPaths, JsonArray::class.java).map { it.asString }.toSet()
    val symbols = JsonObject()
    for (input in inputs) {
      println("--------INPUT::---${input.id.name}--------")
      when (input.type.id.name) {
        TypeConstants.TEXT -> symbols.add(input.id.name, JsonObject().apply {
          addProperty(KeyConstants.KEY_TYPE, input.type.id.name)
          addProperty(KeyConstants.VALUE, args.get(input.id.name).asString)
        })
        TypeConstants.NUMBER -> symbols.add(input.id.name, JsonObject().apply {
          addProperty(KeyConstants.KEY_TYPE, input.type.id.name)
          addProperty(KeyConstants.VALUE, args.get(input.id.name).asLong)
        })
        TypeConstants.DECIMAL -> symbols.add(input.id.name, JsonObject().apply {
          addProperty(KeyConstants.KEY_TYPE, input.type.id.name)
          addProperty(KeyConstants.VALUE, args.get(input.id.name).asDouble)
        })
        TypeConstants.BOOLEAN -> symbols.add(input.id.name, JsonObject().apply {
          addProperty(KeyConstants.KEY_TYPE, input.type.id.name)
          addProperty(KeyConstants.VALUE, args.get(input.id.name).asBoolean)
        })
        TypeConstants.FORMULA, TypeConstants.LIST -> {
        }
        else -> {
          val variable: Variable = variableRepository.findVariable(organizationName = organization.id, superTypeName = GLOBAL_TYPE, typeName = input.type.id.name, superList = organization.superList!!.id, name = args.get(input.id.name).asString)
              ?: throw CustomJsonException("{args: {${input.id.name}: 'Unexpected value for parameter'}}")
          symbols.add(input.id.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
            addProperty(KeyConstants.VALUE, variable.id.name)
            add("values", getSymbolsForFunctionArgs(symbolPaths = symbolPaths, variable = variable, prefix = input.id.name + "."))
          })
        }
      }
    }
    println("-------SYMBOL---PATHS----------")
    println(symbolPaths)
    println("-------------SYMBOLS--------------")
    println(symbols)
    for (input in inputs) {
      when (input.type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.LIST, TypeConstants.FORMULA -> {
        }
        else -> {
          if (input.variableName != null || input.values != null) {
            try {
              variableService.updateVariable(jsonParams = JsonObject().apply {
                addProperty("organization", jsonParams.get("organization").asString)
                addProperty("username", jsonParams.get("username").asString)
                addProperty("typeName", input.type.id.name)
                addProperty("variableName", args.get(input.id.name).asString)
                if (input.variableName != null) {
                  addProperty("updatedVariableName", validateOrEvaluateExpression(jsonParams = gson.fromJson(input.variableName, JsonObject::class.java).apply {
                    addProperty("expectedReturnType", TypeConstants.TEXT)
                  }, mode = "evaluate", symbols = symbols) as String)
                }
                if (input.values != null)
                  add("values", getFunctionInputTypeJson(functionInputType = input.values!!, symbols = symbols))
              })
            } catch (exception: CustomJsonException) {
              throw CustomJsonException("{inputs: {${input.id.name}: ${exception.message}}}")
            }
          }
        }
      }
    }
    val results = JsonObject()
    val outputs: Set<FunctionOutput> = functionOutputRepository.getFunctionOutputs(organizationName = organization.id, functionName = jsonParams.get("functionName").asString)
    for (output in outputs) {
      println("--${output.id.name}--")
      when (output.type.id.name) {
        TypeConstants.TEXT -> results.addProperty(output.id.name, validateOrEvaluateExpression(jsonParams = gson.fromJson(output.variableName, JsonObject::class.java).apply {
          addProperty("expectedReturnType", output.type.id.name)
        }, mode = "evaluate", symbols = symbols) as String)
        TypeConstants.NUMBER -> results.addProperty(output.id.name, validateOrEvaluateExpression(jsonParams = gson.fromJson(output.variableName, JsonObject::class.java).apply {
          addProperty("expectedReturnType", output.type.id.name)
        }, mode = "evaluate", symbols = symbols) as Long)
        TypeConstants.DECIMAL -> results.addProperty(output.id.name, validateOrEvaluateExpression(jsonParams = gson.fromJson(output.variableName, JsonObject::class.java).apply {
          addProperty("expectedReturnType", output.type.id.name)
        }, mode = "evaluate", symbols = symbols) as Double)
        TypeConstants.BOOLEAN -> results.addProperty(output.id.name, validateOrEvaluateExpression(jsonParams = gson.fromJson(output.variableName, JsonObject::class.java).apply {
          addProperty("expectedReturnType", output.type.id.name)
        }, mode = "evaluate", symbols = symbols) as Boolean)
        TypeConstants.FORMULA, TypeConstants.LIST -> {
        }
        else -> results.add(output.id.name, serialize(try {
          variableService.createVariable(jsonParams = JsonObject().apply {
            addProperty("organization", jsonParams.get("organization").asString)
            addProperty("username", jsonParams.get("username").asString)
            addProperty("typeName", output.type.id.name)
            addProperty("variableName", validateOrEvaluateExpression(jsonParams = gson.fromJson(output.variableName, JsonObject::class.java).apply {
              addProperty("expectedReturnType", TypeConstants.TEXT)
            }, mode = "evaluate", symbols = symbols) as String)
            add("values", getFunctionOutputTypeJson(functionOutputType = output.values!!, symbols = symbols))
          })
        } catch (exception: CustomJsonException) {
          throw CustomJsonException("{outputs: {${output.id.name}: ${exception.message}}}")
        }))
      }
    }
    return results
  }

  fun saveFunctionInputType(inputs: JsonObject, globalTypes: Set<Type>, functionInput: FunctionInput, type: Type, values: JsonObject): FunctionInputType {
    println("--------saveFunctionInputType----------")
    println(values)
    val functionInputType = FunctionInputType(id = FunctionInputTypeId(functionInput = functionInput, type = type))
    for (key in type.keys) {
      if (values.has(key.id.name)) {
        when (key.type.id.name) {
          TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN ->
            functionInputType.functionInputKeys.add(FunctionInputKey(id = FunctionInputKeyId(functionInputType = functionInputType, key = key),
                expression = values.get(key.id.name).asJsonObject.toString(),
                keyDependencies = getInputKeyDependencies(inputs = inputs, globalTypes = globalTypes, symbolPaths = validateOrEvaluateExpression(jsonParams = values.get(key.id.name).asJsonObject.deepCopy().apply { addProperty("expectedReturnType", key.type.id.name) }, mode = "collect", symbols = JsonObject()) as MutableSet<String>).toMutableSet()))
          TypeConstants.FORMULA, TypeConstants.LIST -> {
          }
          else -> {
            if (key.type.id.superTypeName == GLOBAL_TYPE) {
              functionInputType.functionInputKeys.add(FunctionInputKey(id = FunctionInputKeyId(functionInputType = functionInputType, key = key),
                  expression = values.get(key.id.name).asJsonObject.toString(),
                  keyDependencies = getInputKeyDependencies(inputs = inputs, globalTypes = globalTypes, symbolPaths = validateOrEvaluateExpression(jsonParams = values.get(key.id.name).asJsonObject.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) }, mode = "collect", symbols = JsonObject()) as MutableSet<String>).toMutableSet()))
            } else {
              if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.type.id.superTypeName)
                  || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
                functionInputType.functionInputKeys.add(FunctionInputKey(id = FunctionInputKeyId(functionInputType = functionInputType, key = key),
                    referencedFunctionInputType = saveFunctionInputType(inputs = inputs, globalTypes = globalTypes, functionInput = functionInput, type = key.type, values = values.get(key.id.name).asJsonObject)))
              } else {
                val symbolPaths: MutableSet<String> = mutableSetOf()
                symbolPaths.apply {
                  addAll(validateOrEvaluateExpression(jsonParams = values.get(key.id.name).asJsonObject.get("context").asJsonObject.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.NUMBER) },
                      mode = "collect", symbols = JsonObject()) as MutableSet<String>)
                  addAll(validateOrEvaluateExpression(jsonParams = values.get(key.id.name).asJsonObject.get("variableName").asJsonObject.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) },
                      mode = "collect", symbols = JsonObject()) as MutableSet<String>)
                }
                functionInputType.functionInputKeys.add(FunctionInputKey(id = FunctionInputKeyId(functionInputType = functionInputType, key = key),
                    expression = values.get(key.id.name).asJsonObject.toString(),
                    keyDependencies = getInputKeyDependencies(inputs = inputs, globalTypes = globalTypes, symbolPaths = symbolPaths).toMutableSet()))
              }
            }
          }
        }
      }
    }
    return functionInputTypeRepository.save(functionInputType)
  }

  fun saveFunctionOutputType(inputs: JsonObject, globalTypes: Set<Type>, functionOutput: FunctionOutput, type: Type, values: JsonObject): FunctionOutputType {
    println("--------saveFunctionOutputType----------")
    println(values)
    val functionOutputType = FunctionOutputType(id = FunctionOutputTypeId(functionOutput = functionOutput, type = type))
    for (key in type.keys) {
      when (key.type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN ->
          functionOutputType.functionOutputKeys.add(FunctionOutputKey(id = FunctionOutputKeyId(functionOutputType = functionOutputType, key = key),
              expression = values.get(key.id.name).asJsonObject.toString(),
              keyDependencies = getInputKeyDependencies(inputs = inputs, globalTypes = globalTypes, symbolPaths = validateOrEvaluateExpression(jsonParams = values.get(key.id.name).asJsonObject.deepCopy().apply { addProperty("expectedReturnType", key.type.id.name) }, mode = "collect", symbols = JsonObject()) as MutableSet<String>).toMutableSet()))
        TypeConstants.FORMULA, TypeConstants.LIST -> {
        }
        else -> {
          if (key.type.id.superTypeName == GLOBAL_TYPE) {
            functionOutputType.functionOutputKeys.add(FunctionOutputKey(id = FunctionOutputKeyId(functionOutputType = functionOutputType, key = key),
                expression = values.get(key.id.name).asJsonObject.toString(),
                keyDependencies = getInputKeyDependencies(inputs = inputs, globalTypes = globalTypes, symbolPaths = validateOrEvaluateExpression(jsonParams = values.get(key.id.name).asJsonObject.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) }, mode = "collect", symbols = JsonObject()) as MutableSet<String>).toMutableSet()))
          } else {
            if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
              functionOutputType.functionOutputKeys.add(FunctionOutputKey(id = FunctionOutputKeyId(functionOutputType = functionOutputType, key = key),
                  referencedFunctionOutputType = saveFunctionOutputType(inputs = inputs, globalTypes = globalTypes, functionOutput = functionOutput, type = key.type, values = values.get(key.id.name).asJsonObject)))
            } else {
              val symbolPaths: MutableSet<String> = mutableSetOf()
              symbolPaths.apply {
                addAll(validateOrEvaluateExpression(jsonParams = values.get(key.id.name).asJsonObject.get("context").asJsonObject.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.NUMBER) },
                    mode = "collect", symbols = JsonObject()) as MutableSet<String>)
                addAll(validateOrEvaluateExpression(jsonParams = values.get(key.id.name).asJsonObject.get("variableName").asJsonObject.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) },
                    mode = "collect", symbols = JsonObject()) as MutableSet<String>)
              }
              functionOutputType.functionOutputKeys.add(FunctionOutputKey(id = FunctionOutputKeyId(functionOutputType = functionOutputType, key = key),
                  expression = values.get(key.id.name).asJsonObject.toString(),
                  keyDependencies = getInputKeyDependencies(inputs = inputs, globalTypes = globalTypes, symbolPaths = symbolPaths).toMutableSet()))
            }
          }
        }
      }
    }
    return functionOutputTypeRepository.save(functionOutputType)
  }
}
