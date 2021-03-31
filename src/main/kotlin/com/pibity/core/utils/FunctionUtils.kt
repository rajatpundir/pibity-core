/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.utils

import com.google.gson.JsonObject
import com.pibity.core.commons.constants.*
import com.pibity.core.commons.exceptions.CustomJsonException
import com.pibity.core.entities.Key
import com.pibity.core.entities.Type
import com.pibity.core.entities.function.FunctionInput
import com.pibity.core.entities.permission.FunctionInputPermission
import org.springframework.web.multipart.MultipartFile
import java.sql.Timestamp

fun validateFunctionName(functionName: String): String {
  return if (!keyIdentifierPattern.matcher(functionName).matches())
    throw CustomJsonException("{${FunctionConstants.FUNCTION_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
  else functionName
}

fun validateFunctionInputs(inputs: JsonObject, validTypes: Set<Type>): JsonObject = inputs.entrySet().fold(JsonObject()) { acc, (inputName, input) ->
  acc.apply {
    if (!keyIdentifierPattern.matcher(inputName).matches())
      throw CustomJsonException("{${FunctionConstants.INPUTS}: {$inputName: ${MessageConstants.UNEXPECTED_VALUE}}}")
    else {
      try {
        if (!input.isJsonObject)
          add(inputName, JsonObject().apply { addProperty(KeyConstants.KEY_TYPE, validTypes.single { it.name == input.asString }.name) })
        else {
          add(inputName, JsonObject().apply {
            val inputJson: JsonObject = input.asJsonObject
            val inputType: Type = validTypes.single { it.name == inputJson.get(KeyConstants.KEY_TYPE).asString && it.name != TypeConstants.FORMULA }
            addProperty(KeyConstants.KEY_TYPE, inputType.name)
            if (inputJson.has(KeyConstants.DEFAULT)) {
              when(inputType.name) {
                TypeConstants.TEXT -> addProperty(KeyConstants.DEFAULT, inputJson.get(KeyConstants.DEFAULT).asString)
                TypeConstants.NUMBER -> addProperty(KeyConstants.DEFAULT, inputJson.get(KeyConstants.DEFAULT).asLong)
                TypeConstants.DECIMAL -> addProperty(KeyConstants.DEFAULT, inputJson.get(KeyConstants.DEFAULT).asDouble)
                TypeConstants.BOOLEAN -> addProperty(KeyConstants.DEFAULT, inputJson.get(KeyConstants.DEFAULT).asBoolean)
                TypeConstants.DATE -> addProperty(KeyConstants.DEFAULT, java.sql.Date(inputJson.get(KeyConstants.DEFAULT).asLong).time)
                TypeConstants.TIMESTAMP -> addProperty(KeyConstants.DEFAULT, Timestamp(inputJson.get(KeyConstants.DEFAULT).asLong).time)
                TypeConstants.TIME -> addProperty(KeyConstants.DEFAULT, java.sql.Time(inputJson.get(KeyConstants.DEFAULT).asLong).time)
                TypeConstants.BLOB -> addProperty(KeyConstants.DEFAULT, inputJson.get(KeyConstants.DEFAULT).asInt)
                TypeConstants.FORMULA -> throw CustomJsonException("{}")
                else -> addProperty(KeyConstants.DEFAULT, inputJson.get(KeyConstants.DEFAULT).asString)
              }
            }
            if (inputType.name !in primitiveTypes) {
              if(inputJson.has(VariableConstants.VARIABLE_NAME))
                add(VariableConstants.VARIABLE_NAME, inputJson.get(VariableConstants.VARIABLE_NAME).asJsonObject)
              if(inputJson.has(VariableConstants.VALUES)) {
                val valuesJson: JsonObject = inputJson.get(VariableConstants.VALUES).asJsonObject
                add(VariableConstants.VALUES, inputType.keys
                  .filter { key -> valuesJson.has(key.name) && key.type.name != TypeConstants.FORMULA }
                  .fold(JsonObject()) { values, key ->
                    values.apply { add(key.name, valuesJson.get(key.name).asJsonObject) }
                })
              }
            }
          })
        }
      } catch (exception: Exception) {
        throw CustomJsonException("{${FunctionConstants.INPUTS}: {$inputName: ${MessageConstants.UNEXPECTED_VALUE}}}")
      }
    }
  }
}

fun validateFunctionOutputs(outputs: JsonObject, validTypes: Set<Type>): JsonObject = outputs.entrySet().fold(JsonObject()) { acc, (outputName, output) ->
  acc.apply {
    if (!keyIdentifierPattern.matcher(outputName).matches())
      throw CustomJsonException("{${FunctionConstants.OUTPUTS}: {$outputName: ${MessageConstants.UNEXPECTED_VALUE}}}")
    else {
      try {
        add(outputName, JsonObject().apply {
          val outputJson: JsonObject = output.asJsonObject
          val outputType: Type = validTypes.single { it.name == outputJson.get(KeyConstants.KEY_TYPE).asString && it.name != TypeConstants.FORMULA }
          when(outputType.name) {
            in primitiveTypes -> add(FunctionConstants.VALUE, outputJson.get(FunctionConstants.VALUE).asJsonObject)
            TypeConstants.FORMULA -> throw CustomJsonException("{}")
            else -> {
              val operation: String = outputJson.get(VariableConstants.OPERATION).asString
              addProperty(VariableConstants.OPERATION, outputJson.get(VariableConstants.OPERATION).asString)
              add(VariableConstants.VARIABLE_NAME, outputJson.get(VariableConstants.VARIABLE_NAME).asJsonObject)
              when(operation) {
                VariableConstants.CREATE -> {
                  val valuesJson: JsonObject = outputJson.get(VariableConstants.VALUES).asJsonObject
                  add(VariableConstants.VALUES, outputType.keys
                    .filter { key -> key.type.name != TypeConstants.FORMULA }
                    .fold(JsonObject()) { values, key ->
                      values.apply { add(key.name, valuesJson.get(key.name).asJsonObject) }
                    })
                }
                VariableConstants.UPDATE -> {
                  val valuesJson: JsonObject = outputJson.get(VariableConstants.VALUES).asJsonObject
                  add(VariableConstants.VALUES, outputType.keys
                    .filter { key -> valuesJson.has(key.name) && key.type.name != TypeConstants.FORMULA }
                    .fold(JsonObject()) { values, key ->
                      values.apply { add(key.name, valuesJson.get(key.name).asJsonObject) }
                    })
                }
                VariableConstants.DELETE -> {}
                else -> throw CustomJsonException("{}")
              }
            }
          }
        })
      } catch (exception: Exception) {
        throw CustomJsonException("{${FunctionConstants.OUTPUTS}: {$outputName: ${MessageConstants.UNEXPECTED_VALUE}}}")
      }
    }
  }
}

@Suppress("UNCHECKED_CAST")
fun getSymbolPathsForFunctionInputs(inputs: JsonObject, validTypes: Set<Type>): Set<String> = inputs.entrySet().fold(mutableSetOf()) { acc, (inputName, input) ->
  acc.apply {
    try {
      val inputJson: JsonObject = input.asJsonObject
      val inputType: Type = validTypes.single { it.name == inputJson.get(KeyConstants.KEY_TYPE).asString && it.name != TypeConstants.FORMULA }
      if (inputType.name !in primitiveTypes) {
        if(inputJson.has(VariableConstants.VARIABLE_NAME))
          addAll(
            validateOrEvaluateExpression(expression = inputJson.get(VariableConstants.VARIABLE_NAME).asJsonObject,
            symbols = JsonObject(), mode = LispConstants.VALIDATE, expectedReturnType = TypeConstants.TEXT) as Set<String>)
        if(inputJson.has(VariableConstants.VALUES)) {
          val valuesJson: JsonObject = inputJson.get(VariableConstants.VALUES).asJsonObject
          inputType.keys.filter { key -> valuesJson.has(key.name) && key.type.name != TypeConstants.FORMULA }
            .forEach { key ->
              addAll(
                validateOrEvaluateExpression(expression = valuesJson.get(key.name).asJsonObject,
                symbols = JsonObject(), mode = LispConstants.VALIDATE, expectedReturnType = if (key.type.name in primitiveTypes) key.type.name else TypeConstants.TEXT) as Set<String>)
            }
        }
      }
    } catch (exception: CustomJsonException) {
      throw CustomJsonException("{${FunctionConstants.INPUTS}: {$inputName: ${exception.message}}}")
    }
  }
}

@Suppress("UNCHECKED_CAST")
fun getSymbolPathsForFunctionOutputs(outputs: JsonObject, validTypes: Set<Type>): Set<String> = outputs.entrySet().fold(mutableSetOf()) { acc, (outputName, output) ->
  acc.apply {
    try {
      val outputJson: JsonObject = output.asJsonObject
      val outputType: Type = validTypes.single { it.name == outputJson.get(KeyConstants.KEY_TYPE).asString && it.name != TypeConstants.FORMULA }
      when(outputType.name) {
        in primitiveTypes -> addAll(
          validateOrEvaluateExpression(expression = outputJson.get(FunctionConstants.VALUE).asJsonObject,
          symbols = JsonObject(), mode = LispConstants.VALIDATE, expectedReturnType = outputType.name) as Set<String>)
        TypeConstants.FORMULA -> throw CustomJsonException("{}")
        else -> {
          val operation: String = outputJson.get(VariableConstants.OPERATION).asString
          addAll(
            validateOrEvaluateExpression(expression = outputJson.get(VariableConstants.VARIABLE_NAME).asJsonObject,
            symbols = JsonObject(), mode = LispConstants.VALIDATE, expectedReturnType = TypeConstants.TEXT) as Set<String>)
          when(operation) {
            VariableConstants.CREATE -> {
              val valuesJson: JsonObject = outputJson.get(VariableConstants.VALUES).asJsonObject
              outputType.keys.filter { key -> key.name != TypeConstants.FORMULA }.forEach { key ->
                addAll(
                  validateOrEvaluateExpression(expression = valuesJson.get(key.name).asJsonObject, symbols = JsonObject(), mode = LispConstants.VALIDATE,
                  expectedReturnType = if (key.type.name in primitiveTypes) key.type.name else TypeConstants.TEXT) as Set<String>)
              }
            }
            VariableConstants.UPDATE -> {
              val valuesJson: JsonObject = outputJson.get(VariableConstants.VALUES).asJsonObject
              outputType.keys.filter { key -> valuesJson.has(key.name) && key.type.name != TypeConstants.FORMULA }
                .forEach { key ->
                  addAll(
                    validateOrEvaluateExpression(expression = valuesJson.get(key.name).asJsonObject, symbols = JsonObject(), mode = LispConstants.VALIDATE,
                    expectedReturnType = if (key.type.name in primitiveTypes) key.type.name else TypeConstants.TEXT) as Set<String>)
                }
            }
            VariableConstants.DELETE -> {}
            else -> throw CustomJsonException("{}")
          }
        }
      }
    } catch (exception: CustomJsonException) {
      throw CustomJsonException("{${FunctionConstants.OUTPUTS}: {$outputName: ${exception.message}}}")
    }
  }
}

fun getSymbolPathsForFunction(inputs: JsonObject, outputs: JsonObject, validTypes: Set<Type>): MutableSet<String> = mutableSetOf<String>().apply {
  addAll(getSymbolPathsForFunctionInputs(inputs = inputs, validTypes = validTypes))
  addAll(getSymbolPathsForFunctionOutputs(outputs = outputs, validTypes = validTypes))
}

fun getSymbolsForFunction(inputs: Map<String, Type>, symbolPaths: MutableSet<String>): JsonObject = inputs.entries.fold(JsonObject()) { acc, (inputName, inputType) ->
  acc.apply {
    if (symbolPaths.contains(inputName))
      add(inputName, JsonObject().apply {
        addProperty(SymbolConstants.SYMBOL_TYPE, if (inputType.name in primitiveTypes) inputType.name else TypeConstants.TEXT)
        if (symbolPaths.any { it.startsWith("$inputName.")})
          add(SymbolConstants.SYMBOL_VALUES, getSymbols(type = inputType, symbolPaths = symbolPaths, symbolsForFormula = false, prefix = "$inputName."))
      })
    else if (symbolPaths.any { it.startsWith("$inputName.")})
      add(inputName, JsonObject().apply {
        add(SymbolConstants.SYMBOL_VALUES, getSymbols(type = inputType, symbolPaths = symbolPaths, symbolsForFormula = false, prefix = "$inputName."))
      })
  }
}

fun validateFunctionInputs(inputs: JsonObject, validTypes: Set<Type>, symbols: JsonObject) = inputs.entrySet().fold(JsonObject()) { acc, (inputName, input) ->
  acc.apply {
    if (!keyIdentifierPattern.matcher(inputName).matches())
      throw CustomJsonException("{${FunctionConstants.INPUTS}: {$inputName: ${MessageConstants.UNEXPECTED_VALUE}}}")
    else {
      try {
        if (!input.isJsonObject)
          add(inputName, JsonObject().apply { addProperty(KeyConstants.KEY_TYPE, input.asString) })
        else {
          add(inputName, JsonObject().apply {
            val inputJson: JsonObject = input.asJsonObject
            val inputType: Type = validTypes.single { it.name == inputJson.get(KeyConstants.KEY_TYPE).asString && it.name != TypeConstants.FORMULA }
            addProperty(KeyConstants.KEY_TYPE, inputType.name)
            if (inputJson.has(KeyConstants.DEFAULT)) {
              when(inputType.name) {
                TypeConstants.TEXT -> addProperty(KeyConstants.DEFAULT, inputJson.get(KeyConstants.DEFAULT).asString)
                TypeConstants.NUMBER -> addProperty(KeyConstants.DEFAULT, inputJson.get(KeyConstants.DEFAULT).asLong)
                TypeConstants.DECIMAL -> addProperty(KeyConstants.DEFAULT, inputJson.get(KeyConstants.DEFAULT).asDouble)
                TypeConstants.BOOLEAN -> addProperty(KeyConstants.DEFAULT, inputJson.get(KeyConstants.DEFAULT).asBoolean)
                TypeConstants.DATE -> addProperty(KeyConstants.DEFAULT, java.sql.Date(inputJson.get(KeyConstants.DEFAULT).asLong).time)
                TypeConstants.TIMESTAMP -> addProperty(KeyConstants.DEFAULT, Timestamp(inputJson.get(KeyConstants.DEFAULT).asLong).time)
                TypeConstants.TIME -> addProperty(KeyConstants.DEFAULT, java.sql.Time(inputJson.get(KeyConstants.DEFAULT).asLong).time)
                TypeConstants.BLOB -> addProperty(KeyConstants.DEFAULT, inputJson.get(KeyConstants.DEFAULT).asInt)
                TypeConstants.FORMULA -> throw CustomJsonException("{}")
                else -> addProperty(KeyConstants.DEFAULT, inputJson.get(KeyConstants.DEFAULT).asString)
              }
            }
            if (inputType.name !in primitiveTypes) {
              if(inputJson.has(VariableConstants.VARIABLE_NAME))
                add(VariableConstants.VARIABLE_NAME, validateOrEvaluateExpression(expression = inputJson.get(VariableConstants.VARIABLE_NAME).asJsonObject,
                  symbols = symbols, mode = LispConstants.REFLECT, expectedReturnType = TypeConstants.TEXT) as JsonObject)
              if(inputJson.has(VariableConstants.VALUES)) {
                val valuesJson: JsonObject = inputJson.get(VariableConstants.VALUES).asJsonObject
                add(VariableConstants.VALUES, inputType.keys
                  .filter { key -> valuesJson.has(key.name) && key.type.name != TypeConstants.FORMULA }
                  .fold(JsonObject()) { values, key ->
                    values.apply { add(key.name, validateOrEvaluateExpression(expression = valuesJson.get(key.name).asJsonObject,
                      symbols = symbols, mode = LispConstants.REFLECT, expectedReturnType = if (key.type.name in primitiveTypes) key.type.name else TypeConstants.TEXT) as JsonObject) }
                  })
              }
            }
          })
        }
      } catch (exception: Exception) {
        throw CustomJsonException("{${FunctionConstants.INPUTS}: {$inputName: ${MessageConstants.UNEXPECTED_VALUE}}}")
      }
    }
  }
}

fun validateFunctionOutputs(outputs: JsonObject, validTypes: Set<Type>, symbols: JsonObject): JsonObject = outputs.entrySet().fold(JsonObject()) { acc, (outputName, output) ->
  acc.apply {
    if (!keyIdentifierPattern.matcher(outputName).matches())
      throw CustomJsonException("{${FunctionConstants.OUTPUTS}: {$outputName: ${MessageConstants.UNEXPECTED_VALUE}}}")
    else {
      try {
        add(outputName, JsonObject().apply {
          val outputJson: JsonObject = output.asJsonObject
          val outputType: Type = validTypes.single { it.name == outputJson.get(KeyConstants.KEY_TYPE).asString && it.name != TypeConstants.FORMULA }
          when(outputType.name) {
            in primitiveTypes -> add(FunctionConstants.VALUE, validateOrEvaluateExpression(expression = outputJson.get(FunctionConstants.VALUE).asJsonObject,
              symbols = symbols, mode = LispConstants.REFLECT, expectedReturnType = if (outputType.name in primitiveTypes) outputType.name else TypeConstants.TEXT) as JsonObject)
            TypeConstants.FORMULA -> throw CustomJsonException("{}")
            else -> {
              val operation: String = outputJson.get(VariableConstants.OPERATION).asString
              addProperty(VariableConstants.OPERATION, outputJson.get(VariableConstants.OPERATION).asString)
              add(VariableConstants.VARIABLE_NAME, validateOrEvaluateExpression(expression = outputJson.get(VariableConstants.VARIABLE_NAME).asJsonObject,
                symbols = symbols, mode = LispConstants.REFLECT, expectedReturnType = TypeConstants.TEXT) as JsonObject)
              when(operation) {
                VariableConstants.CREATE -> {
                  val valuesJson: JsonObject = outputJson.get(VariableConstants.VALUES).asJsonObject
                  add(VariableConstants.VALUES, outputType.keys
                    .filter { key -> key.type.name != TypeConstants.FORMULA }
                    .fold(JsonObject()) { values, key ->
                      values.apply { add(key.name, validateOrEvaluateExpression(expression = valuesJson.get(key.name).asJsonObject,
                        symbols = symbols, mode = LispConstants.REFLECT, expectedReturnType = if (key.type.name in primitiveTypes) key.type.name else TypeConstants.TEXT) as JsonObject) }
                    })
                }
                VariableConstants.UPDATE -> {
                  val valuesJson: JsonObject = outputJson.get(VariableConstants.VALUES).asJsonObject
                  add(VariableConstants.VALUES, outputType.keys
                    .filter { key -> valuesJson.has(key.name) && key.type.name != TypeConstants.FORMULA }
                    .fold(JsonObject()) { values, key ->
                      values.apply { add(key.name, validateOrEvaluateExpression(expression = valuesJson.get(key.name).asJsonObject,
                        symbols = symbols, mode = LispConstants.REFLECT, expectedReturnType = if (key.type.name in primitiveTypes) key.type.name else TypeConstants.TEXT) as JsonObject) }
                    })
                }
                VariableConstants.DELETE -> {}
                else -> throw CustomJsonException("{}")
              }
            }
          }
        })
      } catch (exception: Exception) {
        throw CustomJsonException("{${FunctionConstants.OUTPUTS}: {$outputName: ${MessageConstants.UNEXPECTED_VALUE}}}")
      }
    }
  }
}

fun validateFunction(jsonParams: JsonObject, validTypes: Set<Type>): Quadruple<String, JsonObject, JsonObject, JsonObject> {
  val inputs: JsonObject = validateFunctionInputs(inputs = jsonParams.get(FunctionConstants.INPUTS).asJsonObject, validTypes = validTypes)
  val outputs: JsonObject = validateFunctionOutputs(outputs = jsonParams.get(FunctionConstants.INPUTS).asJsonObject, validTypes = validTypes)
  val symbolPaths: MutableSet<String> = getSymbolPathsForFunction(inputs = inputs, outputs = outputs, validTypes = validTypes)
  val symbols = getSymbolsForFunction(symbolPaths = symbolPaths,
    inputs = inputs.entrySet().fold(mutableMapOf()) { acc, (inputName, input) ->
      acc.apply { set(inputName, validTypes.single { it.name == input.asJsonObject.get(KeyConstants.KEY_TYPE).asString}) }
    })
  return Quadruple(
    validateFunctionName(functionName = jsonParams.get(FunctionConstants.FUNCTION_NAME).asString),
    validateFunctionInputs(inputs = inputs, validTypes = validTypes, symbols = symbols),
    validateFunctionOutputs(outputs = outputs, validTypes = validTypes, symbols = symbols),
    symbols)
}

fun getKeyDependencies(inputs: Map<String, Type>, symbolPaths: MutableSet<String>): MutableSet<Key> = inputs.entries.fold(mutableSetOf()) { acc, (inputName, inputType) ->
  acc.apply {
    val keyDependencies: MutableSet<Key> = mutableSetOf()
    getSymbols(type = inputType, symbolPaths = symbolPaths, keyDependencies = keyDependencies, symbolsForFormula = false, prefix = "$inputName.")
    addAll(keyDependencies)
  }
}

fun validateFunctionArgs(args: JsonObject, inputs: Set<FunctionInputPermission>, defaultTimestamp: Timestamp, files: List<MultipartFile>): JsonObject = inputs.fold(JsonObject()) { acc, inputPermission ->
  acc.apply {
    val input: FunctionInput = inputPermission.functionInput
    try {
      when (input.type.name) {
        TypeConstants.TEXT -> addProperty(input.name, if (inputPermission.accessLevel && args.has(input.name)) args.get(input.name).asString else input.defaultStringValue!!)
        TypeConstants.NUMBER -> addProperty(input.name, if (inputPermission.accessLevel && args.has(input.name)) args.get(input.name).asLong else input.defaultLongValue!!)
        TypeConstants.DECIMAL -> addProperty(input.name, if (inputPermission.accessLevel && args.has(input.name)) args.get(input.name).asBigDecimal else input.defaultDecimalValue!!)
        TypeConstants.BOOLEAN -> addProperty(input.name, if (inputPermission.accessLevel && args.has(input.name)) args.get(input.name).asBoolean else input.defaultBooleanValue!!)
        TypeConstants.DATE -> addProperty(input.name, if (inputPermission.accessLevel && args.has(input.name)) java.sql.Date(args.get(input.name).asLong).time
        else if (input.defaultDateValue != null) input.defaultDateValue!!.time else java.sql.Date(defaultTimestamp.time).time)
        TypeConstants.TIMESTAMP -> addProperty(input.name, if (inputPermission.accessLevel && args.has(input.name)) Timestamp(args.get(input.name).asLong).time
        else if (input.defaultTimestampValue != null) input.defaultTimestampValue!!.time else defaultTimestamp.time)
        TypeConstants.TIME -> addProperty(input.name, if (inputPermission.accessLevel && args.has(input.name)) java.sql.Time(args.get(input.name).asLong).time
        else if (input.defaultTimeValue != null) input.defaultTimeValue!!.time else java.sql.Time(defaultTimestamp.time).time)
        TypeConstants.BLOB -> if (inputPermission.accessLevel) {
          val fileIndex: Int = args.get(input.name).asInt
          if (fileIndex < 0 && fileIndex > (files.size - 1))
            throw CustomJsonException("{}")
          else
            addProperty(input.name, fileIndex)
        }
        TypeConstants.FORMULA -> throw CustomJsonException("{}")
        else -> addProperty(input.name, if (inputPermission.accessLevel && args.has(input.name)) args.get(input.name).asString else input.referencedVariable!!.name)
      }
    } catch (exception: Exception) {
      throw CustomJsonException("{${FunctionConstants.ARGS}: {${input.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
    }
  }
}
