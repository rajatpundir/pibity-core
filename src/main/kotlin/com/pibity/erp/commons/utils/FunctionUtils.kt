/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons.utils

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.KeyConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.constants.primitiveTypes
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.Key
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.Variable
import com.pibity.erp.entities.function.FunctionInput
import com.pibity.erp.entities.function.FunctionInputType
import com.pibity.erp.entities.function.FunctionOutputType
import java.math.BigDecimal
import java.sql.Timestamp

fun validateFunctionName(functionName: String): String {
  if (!keyIdentifierPattern.matcher(functionName).matches())
    throw CustomJsonException("{functionName: 'Function name $functionName is not a valid identifier'}")
  return functionName
}

fun getValueSymbolPaths(values: JsonObject, type: Type, paramsAreInputs: Boolean): Set<String> {
  val symbolPaths: MutableSet<String> = mutableSetOf()
  for (key in type.keys) {
    if (key.type.name != TypeConstants.FORMULA) {
      if (paramsAreInputs && !values.has(key.name))
        continue
      val keyExpression: JsonObject = if (!values.has(key.name))
        throw CustomJsonException("{${key.name}: 'Field is missing in request body'}")
      else {
        try {
          values.get(key.name).asJsonObject
        } catch (exception: Exception) {
          throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
        }
      }
      when (key.type.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.DATE, TypeConstants.TIMESTAMP, TypeConstants.TIME -> {
          try {
            symbolPaths.addAll(
              validateOrEvaluateExpression(
                jsonParams = keyExpression.deepCopy().apply { addProperty("expectedReturnType", key.type.name) },
                mode = "collect",
                symbols = JsonObject()
              ) as Set<String>
            )
          } catch (exception: CustomJsonException) {
            throw CustomJsonException("{${key.name}: ${exception.message}}")
          }
        }
        TypeConstants.FORMULA, TypeConstants.BLOB -> {
        }
        else -> {
          try {
            symbolPaths.addAll(
              validateOrEvaluateExpression(
                jsonParams = keyExpression.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) },
                mode = "collect",
                symbols = JsonObject()
              ) as Set<String>
            )
          } catch (exception: CustomJsonException) {
            throw CustomJsonException("{${key.name}: ${exception.message}}")
          }
        }
      }
    }
  }
  return symbolPaths
}

fun getSymbolPathsForInputsOrOutputs(
  jsonParams: JsonObject,
  globalTypes: Set<Type>,
  paramsAreInputs: Boolean
): Set<String> {
  val symbolPaths: MutableSet<String> = mutableSetOf()
  for ((keyName, json) in jsonParams.entrySet()) {
    if (!keyIdentifierPattern.matcher(keyName).matches())
      throw CustomJsonException("{${keyName}: 'Parameter name is not a valid identifier'}")
    if (paramsAreInputs && !json.isJsonObject) {
      val type: Type = try {
        globalTypes.single { it.name == json.asString }
      } catch (exception: Exception) {
        throw CustomJsonException("{${keyName}: 'Unexpected value for parameter'}")
      }
      if (!primitiveTypes.contains(type.name))
        throw CustomJsonException("{${keyName}: 'Unexpected value for parameter'}")
      if (type.name == TypeConstants.FORMULA)
        throw CustomJsonException("{${keyName}: 'Unexpected value for parameter'}")
    } else {
      val keyJson: JsonObject = try {
        json.asJsonObject
      } catch (exception: Exception) {
        throw CustomJsonException("{${keyName}: 'Unexpected value for parameter'}")
      }
      val type: Type = if (!keyJson.has("type"))
        throw CustomJsonException("{${keyName}: {type: 'Field is missing in request body'}}")
      else {
        try {
          globalTypes.single { it.name == keyJson.get("type").asString }
        } catch (exception: Exception) {
          throw CustomJsonException("{${keyName}: {type: 'Unexpected value for parameter'}}")
        }
      }
      if (type.name == TypeConstants.FORMULA)
        throw CustomJsonException("{${keyName}: {type: 'Unexpected value for parameter'}}")
      if (paramsAreInputs) {
        if (!primitiveTypes.contains(type.name)) {
          if (keyJson.has("variableName")) {
            val variableNameExpression: JsonObject = try {
              keyJson.get("variableName").asJsonObject
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {variableName: 'Unexpected value for parameter'}}")
            }
            try {
              symbolPaths.addAll(
                validateOrEvaluateExpression(
                  jsonParams = variableNameExpression.deepCopy()
                    .apply { addProperty("expectedReturnType", TypeConstants.TEXT) },
                  mode = "collect",
                  symbols = JsonObject()
                ) as Set<String>
              )
            } catch (exception: CustomJsonException) {
              throw CustomJsonException("{${keyName}: {variableName: ${exception.message}}}")
            }
          }
          if (keyJson.has("values")) {
            val values: JsonObject = try {
              keyJson.get("values").asJsonObject
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {values: 'Unexpected value for parameter'}}")
            }
            try {
              symbolPaths.addAll(getValueSymbolPaths(values = values, type = type, paramsAreInputs = paramsAreInputs))
            } catch (exception: CustomJsonException) {
              throw CustomJsonException("{${keyName}: {values: ${exception.message}}}")
            }
          }
        }
      } else {
        if (!primitiveTypes.contains(type.name)) {
          val variableNameExpression: JsonObject = if (!keyJson.has("variableName"))
            throw CustomJsonException("{${keyName}: {variableName: 'Field is missing in request body'}}")
          else {
            try {
              keyJson.get("variableName").asJsonObject
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {variableName: 'Unexpected value for parameter'}}")
            }
          }
          try {
            symbolPaths.addAll(
              validateOrEvaluateExpression(
                jsonParams = variableNameExpression.deepCopy()
                  .apply { addProperty("expectedReturnType", TypeConstants.TEXT) },
                mode = "collect",
                symbols = JsonObject()
              ) as Set<String>
            )
          } catch (exception: CustomJsonException) {
            throw CustomJsonException("{${keyName}: {variableName: ${exception.message}}}")
          }
          val values: JsonObject = if (!keyJson.has("values"))
            throw CustomJsonException("{${keyName}: {values: 'Field is missing in request body'}}")
          else {
            try {
              keyJson.get("values").asJsonObject
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {values: 'Unexpected value for parameter'}}")
            }
          }
          try {
            symbolPaths.addAll(getValueSymbolPaths(values = values, type = type, paramsAreInputs = paramsAreInputs))
          } catch (exception: CustomJsonException) {
            throw CustomJsonException("{${keyName}: {values: ${exception.message}}}")
          }
        } else {
          val values: JsonObject = if (!keyJson.has("values"))
            throw CustomJsonException("{${keyName}: {values: 'Field is missing in request body'}}")
          else {
            try {
              keyJson.get("values").asJsonObject
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {values: 'Unexpected value for parameter'}}")
            }
          }
          try {
            symbolPaths.addAll(
              validateOrEvaluateExpression(
                jsonParams = values.deepCopy().apply { addProperty("expectedReturnType", type.name) },
                mode = "collect",
                symbols = JsonObject()
              ) as Set<String>
            )
          } catch (exception: CustomJsonException) {
            throw CustomJsonException("{${keyName}: {values: ${exception.message}}}")
          }
        }
      }
    }
  }
  return symbolPaths
}

fun getInputSymbolPaths(jsonParams: JsonObject, globalTypes: Set<Type>): Set<String> {
  return try {
    getSymbolPathsForInputsOrOutputs(jsonParams = jsonParams, globalTypes = globalTypes, paramsAreInputs = true)
  } catch (exception: CustomJsonException) {
    throw CustomJsonException("{inputs: ${exception.message}}")
  }
}

fun getOutputSymbolPaths(jsonParams: JsonObject, globalTypes: Set<Type>): Set<String> {
  return try {
    getSymbolPathsForInputsOrOutputs(jsonParams = jsonParams, globalTypes = globalTypes, paramsAreInputs = false)
  } catch (exception: CustomJsonException) {
    throw CustomJsonException("{outputs: ${exception.message}}")
  }
}

fun validateValue(values: JsonObject, type: Type, symbols: JsonObject, paramsAreInputs: Boolean): JsonObject {
  val expectedValue = JsonObject()
  for (key in type.keys) {
    if (paramsAreInputs && !values.has(key.name))
      continue
    if (key.type.name != TypeConstants.FORMULA) {
      val keyExpression: JsonObject = if (!values.has(key.name))
        throw CustomJsonException("{${key.name}: 'Field is missing in request body'}")
      else {
        try {
          values.get(key.name).asJsonObject
        } catch (exception: Exception) {
          throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
        }
      }
      when (key.type.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> {
          try {
            validateOrEvaluateExpression(
              jsonParams = keyExpression.deepCopy().apply { addProperty("expectedReturnType", key.type.name) },
              mode = "validate",
              symbols = symbols
            )
          } catch (exception: CustomJsonException) {
            throw CustomJsonException("{${key.name}: ${exception.message}}")
          }
          expectedValue.add(key.name, keyExpression)
        }
        TypeConstants.FORMULA -> {
        }
        else -> {
          try {
            validateOrEvaluateExpression(
              jsonParams = keyExpression.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) },
              mode = "validate",
              symbols = symbols
            )
          } catch (exception: CustomJsonException) {
            throw CustomJsonException("{${key.name}: ${exception.message}}")
          }
          expectedValue.add(key.name, keyExpression)
        }
      }
    }
  }
  return expectedValue
}

fun validateInputsOrOutputs(
  jsonParams: JsonObject,
  globalTypes: Set<Type>,
  symbols: JsonObject,
  symbolPaths: Set<String>,
  paramsAreInputs: Boolean
): JsonObject {
  val expectedJson = JsonObject()
  for ((keyName, json) in jsonParams.entrySet()) {
    val expectedKeyJson = JsonObject()
    if (!keyIdentifierPattern.matcher(keyName).matches())
      throw CustomJsonException("{${keyName}: 'Parameter name is not a valid identifier'}")
    if (paramsAreInputs && symbolPaths.none { it == keyName || it.startsWith(prefix = "$keyName.") })
      continue
    if (paramsAreInputs && !json.isJsonObject) {
      val type: Type = try {
        globalTypes.single { it.name == json.asString }
      } catch (exception: Exception) {
        throw CustomJsonException("{${keyName}: 'Unexpected value for parameter'}")
      }
      if (type.name == TypeConstants.FORMULA)
        throw CustomJsonException("{${keyName}: 'Unexpected value for parameter'}")
      expectedJson.addProperty(keyName, type.name)
    } else {
      val keyJson: JsonObject = try {
        json.asJsonObject
      } catch (exception: Exception) {
        throw CustomJsonException("{${keyName}: 'Unexpected value for parameter'}")
      }
      val type: Type = if (!keyJson.has("type"))
        throw CustomJsonException("{${keyName}: {type: 'Field is missing in request body'}}")
      else {
        try {
          globalTypes.single { it.name == keyJson.get("type").asString }
        } catch (exception: Exception) {
          throw CustomJsonException("{${keyName}: {type: 'Unexpected value for parameter'}}")
        }
      }
      if (type.name == TypeConstants.FORMULA)
        throw CustomJsonException("{${keyName}: {type: 'Unexpected value for parameter'}}")
      expectedKeyJson.addProperty("type", type.name)
      if (paramsAreInputs) {
        if (keyJson.has(KeyConstants.DEFAULT)) {
          when (type.name) {
            TypeConstants.TEXT -> try {
              expectedKeyJson.addProperty(KeyConstants.DEFAULT, keyJson.get(KeyConstants.DEFAULT).asString)
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {${KeyConstants.DEFAULT}: 'Unexpected value for parameter'}}")
            }
            TypeConstants.NUMBER -> try {
              expectedKeyJson.addProperty(KeyConstants.DEFAULT, keyJson.get(KeyConstants.DEFAULT).asLong)
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {${KeyConstants.DEFAULT}: 'Unexpected value for parameter'}}")
            }
            TypeConstants.DECIMAL -> try {
              expectedKeyJson.addProperty(KeyConstants.DEFAULT, keyJson.get(KeyConstants.DEFAULT).asBigDecimal)
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {${KeyConstants.DEFAULT}: 'Unexpected value for parameter'}}")
            }
            TypeConstants.BOOLEAN -> try {
              expectedKeyJson.addProperty(KeyConstants.DEFAULT, keyJson.get(KeyConstants.DEFAULT).asBoolean)
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {${KeyConstants.DEFAULT}: 'Unexpected value for parameter'}}")
            }
            TypeConstants.DATE -> try {
              expectedKeyJson.addProperty(
                KeyConstants.DEFAULT,
                java.sql.Date(dateFormat.parse(keyJson.get(KeyConstants.DEFAULT).asString).time).toString()
              )
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {${KeyConstants.DEFAULT}: 'Unexpected value for parameter'}}")
            }
            TypeConstants.TIMESTAMP -> try {
              expectedKeyJson.addProperty(
                KeyConstants.DEFAULT,
                Timestamp(keyJson.get(KeyConstants.DEFAULT).asLong).time
              )
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {${KeyConstants.DEFAULT}: 'Unexpected value for parameter'}}")
            }
            TypeConstants.TIME -> try {
              expectedKeyJson.addProperty(
                KeyConstants.DEFAULT,
                java.sql.Time(keyJson.get(KeyConstants.DEFAULT).asLong).time
              )
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {${KeyConstants.DEFAULT}: 'Unexpected value for parameter'}}")
            }
            TypeConstants.FORMULA, TypeConstants.BLOB -> {
            }
            else -> try {
              expectedKeyJson.addProperty(KeyConstants.DEFAULT, keyJson.get(KeyConstants.DEFAULT).asString)
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {${KeyConstants.DEFAULT}: 'Unexpected value for parameter'}}")
            }
          }
        }
        if (!primitiveTypes.contains(type.name)) {
          if (keyJson.has("variableName")) {
            val variableNameExpression: JsonObject = try {
              keyJson.get("variableName").asJsonObject
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {variableName: 'Unexpected value for parameter'}}")
            }
            try {
              validateOrEvaluateExpression(
                jsonParams = variableNameExpression.deepCopy()
                  .apply { addProperty("expectedReturnType", TypeConstants.TEXT) }, mode = "validate", symbols = symbols
              )
            } catch (exception: CustomJsonException) {
              throw CustomJsonException("{${keyName}: {variableName: ${exception.message}}}")
            }
            expectedKeyJson.add("variableName", variableNameExpression)
          }
          if (keyJson.has("values")) {
            val values: JsonObject = try {
              keyJson.get("values").asJsonObject
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {values: 'Unexpected value for parameter'}}")
            }
            val validatedValues: JsonObject = try {
              validateValue(values = values, type = type, symbols = symbols, paramsAreInputs = paramsAreInputs)
            } catch (exception: CustomJsonException) {
              throw CustomJsonException("{${keyName}: {values: ${exception.message}}}")
            }
            if (validatedValues.size() != 0)
              expectedKeyJson.add("values", validatedValues)
          }
        }
      } else {
        if (!primitiveTypes.contains(type.name)) {
          val variableNameExpression: JsonObject = if (!keyJson.has("variableName"))
            throw CustomJsonException("{${keyName}: {variableName: 'Field is missing in request body'}}")
          else {
            try {
              keyJson.get("variableName").asJsonObject
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {variableName: 'Unexpected value for parameter'}}")
            }
          }
          try {
            validateOrEvaluateExpression(
              jsonParams = variableNameExpression.deepCopy()
                .apply { addProperty("expectedReturnType", TypeConstants.TEXT) }, mode = "validate", symbols = symbols
            )
          } catch (exception: CustomJsonException) {
            throw CustomJsonException("{${keyName}: {variableName: ${exception.message}}}")
          }
          expectedKeyJson.add("variableName", variableNameExpression)
          val values: JsonObject = if (!keyJson.has("values"))
            throw CustomJsonException("{${keyName}: {values: 'Field is missing in request body'}}")
          else {
            try {
              keyJson.get("values").asJsonObject
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {values: 'Unexpected value for parameter'}}")
            }
          }
          val validatedValues: JsonObject = try {
            validateValue(values = values, type = type, symbols = symbols, paramsAreInputs = paramsAreInputs)
          } catch (exception: CustomJsonException) {
            throw CustomJsonException("{${keyName}: {values: ${exception.message}}}")
          }
          if (validatedValues.size() != 0)
            expectedKeyJson.add("values", validatedValues)
        } else {
          val values: JsonObject = if (!keyJson.has("values"))
            throw CustomJsonException("{${keyName}: {values: 'Field is missing in request body'}}")
          else {
            try {
              keyJson.get("values").asJsonObject
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {values: 'Unexpected value for parameter'}}")
            }
          }
          try {
            validateOrEvaluateExpression(
              jsonParams = values.deepCopy().apply { addProperty("expectedReturnType", type.name) },
              mode = "validate",
              symbols = symbols
            )
          } catch (exception: CustomJsonException) {
            throw CustomJsonException("{${keyName}: {values: ${exception.message}}}")
          }
          expectedKeyJson.add("values", values)
        }
      }
      expectedJson.add(keyName, expectedKeyJson)
    }
  }
  return expectedJson
}

fun validateInputs(
  jsonParams: JsonObject,
  globalTypes: Set<Type>,
  symbols: JsonObject,
  symbolPaths: Set<String>
): JsonObject {
  return try {
    validateInputsOrOutputs(
      jsonParams = jsonParams,
      globalTypes = globalTypes,
      symbols = symbols,
      symbolPaths = symbolPaths,
      paramsAreInputs = true
    )
  } catch (exception: CustomJsonException) {
    throw CustomJsonException("{inputs: ${exception.message}}")
  }
}

fun validateOutputs(
  jsonParams: JsonObject,
  globalTypes: Set<Type>,
  symbols: JsonObject,
  symbolPaths: Set<String>
): JsonObject {
  return try {
    validateInputsOrOutputs(
      jsonParams = jsonParams,
      globalTypes = globalTypes,
      symbols = symbols,
      symbolPaths = symbolPaths,
      paramsAreInputs = false
    )
  } catch (exception: CustomJsonException) {
    throw CustomJsonException("{outputs: ${exception.message}}")
  }
}

fun getInputSymbols(inputs: JsonObject, globalTypes: Set<Type>, symbolPaths: Set<String>): JsonObject {
  val symbols = JsonObject()
  for ((inputName, inputType) in inputs.entrySet()) {
    if (symbolPaths.any { it.startsWith(prefix = inputName) }) {
      val typeName: String = if (inputType.isJsonObject)
        inputType.asJsonObject.get("type").asString
      else
        inputType.asString
      when (typeName) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.DATE, TypeConstants.TIMESTAMP, TypeConstants.TIME -> {
          symbols.add(inputName, JsonObject().apply { addProperty(KeyConstants.KEY_TYPE, typeName) })
        }
        TypeConstants.FORMULA, TypeConstants.BLOB -> {
        }
        else -> {
          val subSymbols: JsonObject = getInputTypeSymbols(
            type = globalTypes.single { it.name == typeName },
            symbolPaths = symbolPaths,
            prefix = "$inputName."
          )
          symbols.add(inputName, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
            if (subSymbols.size() != 0)
              add("values", subSymbols)
          })
        }
      }
    }
  }
  return symbols
}

fun getInputTypeSymbols(type: Type, symbolPaths: Set<String>, prefix: String): JsonObject {
  val symbols = JsonObject()
  for (key in type.keys) {
    if (symbolPaths.any { it.startsWith(prefix = prefix + key.name) }) {
      when (key.type.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.DATE, TypeConstants.TIMESTAMP, TypeConstants.TIME ->
          symbols.add(key.name, JsonObject().apply { addProperty(KeyConstants.KEY_TYPE, key.type.name) })
        TypeConstants.FORMULA ->
          symbols.add(
            key.name,
            JsonObject().apply { addProperty(KeyConstants.KEY_TYPE, key.formula!!.returnType.name) })
        TypeConstants.BLOB -> {
        }
        else -> {
          val subSymbols: JsonObject = if (symbolPaths.any { it.startsWith(prefix = prefix + key.name + ".") })
            getInputTypeSymbols(type = key.type, symbolPaths = symbolPaths, prefix = prefix + key.name + ".")
          else JsonObject()
          symbols.add(key.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
            if (subSymbols.size() != 0)
              add("values", subSymbols)
          })
        }
      }
    }
  }
  return symbols
}

fun getKeysForPaths(symbolPaths: MutableSet<String>, type: Type, prefix: String = ""): Set<Key> {
  val keyDependencies: MutableSet<Key> = mutableSetOf()
  for (key in type.keys) {
    if (symbolPaths.any { it.startsWith(prefix = prefix + key.name) }) {
      when (key.type.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.FORMULA -> {
          keyDependencies.add(key)
          symbolPaths.remove(prefix + key.name)
        }
        else -> {
          if (symbolPaths.any { it.startsWith(prefix = prefix + key.name + ".") })
            keyDependencies.addAll(
              getKeysForPaths(
                symbolPaths = symbolPaths,
                type = key.type,
                prefix = prefix + key.name + "."
              )
            )
          keyDependencies.add(key)
          symbolPaths.remove(prefix + key.name)
        }
      }
    }
  }
  return keyDependencies
}

fun getInputKeyDependencies(inputs: JsonObject, globalTypes: Set<Type>, symbolPaths: MutableSet<String>): Set<Key> {
  val keyDependencies: MutableSet<Key> = mutableSetOf()
  for ((inputName, inputType) in inputs.entrySet()) {
    if (inputType.isJsonObject && symbolPaths.any { it.startsWith(prefix = inputName) }) {
      val type: Type = globalTypes.single { it.name == inputType.asJsonObject.get("type").asString }
      when (type.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.FORMULA -> {
        }
        else -> keyDependencies.addAll(getKeysForPaths(symbolPaths = symbolPaths, type = type, prefix = "$inputName."))
      }
    }
  }
  return keyDependencies
}

fun validateFunctionArgs(args: JsonObject, inputs: Set<FunctionInput>, defaultTimestamp: Timestamp): JsonObject {
  val expectedJson = JsonObject()
  for (input in inputs) {
    when (input.type.name) {
      TypeConstants.TEXT -> try {
        expectedJson.addProperty(
          input.name,
          if (args.has(input.name)) args.get(input.name).asString else input.defaultStringValue!!
        )
      } catch (exception: Exception) {
        throw CustomJsonException("{args: {${input.name}: 'Unexpected value for parameter'}}")
      }
      TypeConstants.NUMBER -> try {
        expectedJson.addProperty(
          input.name,
          if (args.has(input.name)) args.get(input.name).asLong else input.defaultLongValue!!
        )
      } catch (exception: Exception) {
        throw CustomJsonException("{args: {${input.name}: 'Unexpected value for parameter'}}")
      }
      TypeConstants.DECIMAL -> try {
        expectedJson.addProperty(
          input.name,
          if (args.has(input.name)) args.get(input.name).asBigDecimal else input.defaultDecimalValue!!
        )
      } catch (exception: Exception) {
        throw CustomJsonException("{args: {${input.name}: 'Unexpected value for parameter'}}")
      }
      TypeConstants.BOOLEAN -> try {
        expectedJson.addProperty(
          input.name,
          if (args.has(input.name)) args.get(input.name).asBoolean else input.defaultBooleanValue!!
        )
      } catch (exception: Exception) {
        throw CustomJsonException("{args: {${input.name}: 'Unexpected value for parameter'}}")
      }
      TypeConstants.DATE -> try {
        expectedJson.addProperty(
          input.name,
          if (args.has(input.name)) java.sql.Date(dateFormat.parse(args.get(input.name).asString).time).toString()
          else if (input.defaultDateValue != null) input.defaultDateValue!!.toString()
          else java.sql.Date(defaultTimestamp.time).toString()
        )
      } catch (exception: Exception) {
        throw CustomJsonException("{args: {${input.name}: 'Unexpected value for parameter'}}")
      }
      TypeConstants.TIMESTAMP -> try {
        expectedJson.addProperty(
          input.name, if (args.has(input.name)) Timestamp(args.get(input.name).asLong).time
          else if (input.defaultTimestampValue != null) input.defaultTimestampValue!!.time
          else defaultTimestamp.time
        )
      } catch (exception: Exception) {
        throw CustomJsonException("{args: {${input.name}: 'Unexpected value for parameter'}}")
      }
      TypeConstants.TIME -> try {
        expectedJson.addProperty(
          input.name, if (args.has(input.name)) java.sql.Time(args.get(input.name).asLong).time
          else if (input.defaultTimeValue != null) input.defaultTimeValue!!.time
          else java.sql.Time(defaultTimestamp.time).time
        )
      } catch (exception: Exception) {
        throw CustomJsonException("{args: {${input.name}: 'Unexpected value for parameter'}}")
      }
      TypeConstants.FORMULA, TypeConstants.BLOB -> {
      }
      else -> try {
        expectedJson.addProperty(
          input.name,
          if (args.has(input.name)) args.get(input.name).asString else input.referencedVariable!!.name
        )
      } catch (exception: Exception) {
        throw CustomJsonException("{args: {${input.name}: 'Unexpected value for parameter'}}")
      }
    }
  }
  return expectedJson
}

fun getSymbolsForFunctionArgs(symbolPaths: Set<String>, variable: Variable, prefix: String): JsonObject {
  val expectedSymbols = JsonObject()
  for (value in variable.values) {
    if (symbolPaths.any { it.startsWith(prefix = prefix + value.key.name) }) {
      when (value.key.type.name) {
        TypeConstants.TEXT -> expectedSymbols.add(value.key.name, JsonObject().apply {
          addProperty(KeyConstants.KEY_TYPE, value.key.type.name)
          addProperty(KeyConstants.VALUE, value.stringValue!!)
        })
        TypeConstants.NUMBER -> expectedSymbols.add(value.key.name, JsonObject().apply {
          addProperty(KeyConstants.KEY_TYPE, value.key.type.name)
          addProperty(KeyConstants.VALUE, value.longValue!!)
        })
        TypeConstants.DECIMAL -> expectedSymbols.add(value.key.name, JsonObject().apply {
          addProperty(KeyConstants.KEY_TYPE, value.key.type.name)
          addProperty(KeyConstants.VALUE, value.decimalValue!!)
        })
        TypeConstants.BOOLEAN -> expectedSymbols.add(value.key.name, JsonObject().apply {
          addProperty(KeyConstants.KEY_TYPE, value.key.type.name)
          addProperty(KeyConstants.VALUE, value.booleanValue!!)
        })
        TypeConstants.FORMULA -> when (value.key.formula!!.returnType.name) {
          TypeConstants.TEXT -> expectedSymbols.add(value.key.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, value.key.formula!!.returnType.name)
            addProperty(KeyConstants.VALUE, value.stringValue!!)
          })
          TypeConstants.NUMBER -> expectedSymbols.add(value.key.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, value.key.formula!!.returnType.name)
            addProperty(KeyConstants.VALUE, value.longValue!!)
          })
          TypeConstants.DECIMAL -> expectedSymbols.add(value.key.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, value.key.formula!!.returnType.name)
            addProperty(KeyConstants.VALUE, value.decimalValue!!)
          })
          TypeConstants.BOOLEAN -> expectedSymbols.add(value.key.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, value.key.formula!!.returnType.name)
            addProperty(KeyConstants.VALUE, value.booleanValue!!)
          })
        }
        else -> {
          val subSymbols: JsonObject = if (symbolPaths.any { it.startsWith(prefix = prefix + value.key.name + ".") })
            getSymbolsForFunctionArgs(
              symbolPaths = symbolPaths,
              variable = value.referencedVariable!!,
              prefix = prefix + value.key.name + "."
            )
          else JsonObject()
          expectedSymbols.add(value.key.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
            addProperty(KeyConstants.VALUE, value.referencedVariable!!.name)
            if (subSymbols.size() != 0)
              add("values", subSymbols)
          })
        }
      }
    }
  }
  return expectedSymbols
}

fun getFunctionOutputTypeJson(functionOutputType: FunctionOutputType, symbols: JsonObject): JsonObject {
  val expectedJson = JsonObject()
  for (functionOutputKey in functionOutputType.functionOutputKeys) {
    val key: Key = functionOutputKey.key
    when (key.type.name) {
      TypeConstants.TEXT -> expectedJson.addProperty(
        key.name,
        validateOrEvaluateExpression(
          jsonParams = gson.fromJson(functionOutputKey.expression!!, JsonObject::class.java).apply {
            addProperty("expectedReturnType", key.type.name)
          }, mode = "evaluate", symbols = symbols
        ) as String
      )
      TypeConstants.NUMBER -> expectedJson.addProperty(
        key.name,
        validateOrEvaluateExpression(
          jsonParams = gson.fromJson(functionOutputKey.expression!!, JsonObject::class.java).apply {
            addProperty("expectedReturnType", key.type.name)
          }, mode = "evaluate", symbols = symbols
        ) as Long
      )
      TypeConstants.DECIMAL -> expectedJson.addProperty(
        key.name,
        validateOrEvaluateExpression(
          jsonParams = gson.fromJson(functionOutputKey.expression!!, JsonObject::class.java).apply {
            addProperty("expectedReturnType", key.type.name)
          }, mode = "evaluate", symbols = symbols
        ) as BigDecimal
      )
      TypeConstants.BOOLEAN -> expectedJson.addProperty(
        key.name,
        validateOrEvaluateExpression(
          jsonParams = gson.fromJson(functionOutputKey.expression!!, JsonObject::class.java).apply {
            addProperty("expectedReturnType", key.type.name)
          }, mode = "evaluate", symbols = symbols
        ) as Boolean
      )
      TypeConstants.DATE -> expectedJson.addProperty(
        key.name,
        (validateOrEvaluateExpression(
          jsonParams = gson.fromJson(functionOutputKey.expression!!, JsonObject::class.java).apply {
            addProperty("expectedReturnType", key.type.name)
          }, mode = "evaluate", symbols = symbols
        ) as java.sql.Date).toString()
      )
      TypeConstants.TIMESTAMP -> expectedJson.addProperty(
        key.name,
        (validateOrEvaluateExpression(
          jsonParams = gson.fromJson(functionOutputKey.expression!!, JsonObject::class.java).apply {
            addProperty("expectedReturnType", key.type.name)
          }, mode = "evaluate", symbols = symbols
        ) as Timestamp).time
      )
      TypeConstants.TIME -> expectedJson.addProperty(
        key.name,
        (validateOrEvaluateExpression(
          jsonParams = gson.fromJson(functionOutputKey.expression!!, JsonObject::class.java).apply {
            addProperty("expectedReturnType", key.type.name)
          }, mode = "evaluate", symbols = symbols
        ) as java.sql.Time).time
      )
      TypeConstants.FORMULA, TypeConstants.BLOB -> {
      }
      else -> {
        expectedJson.addProperty(
          key.name,
          validateOrEvaluateExpression(
            jsonParams = gson.fromJson(
              functionOutputKey.expression!!,
              JsonObject::class.java
            ).apply {
              addProperty("expectedReturnType", TypeConstants.TEXT)
            }, mode = "evaluate", symbols = symbols
          ) as String
        )
      }
    }
  }

  return expectedJson
}

fun getFunctionInputTypeJson(functionInputType: FunctionInputType, symbols: JsonObject): JsonObject {
  val expectedJson = JsonObject()
  for (functionInputKey in functionInputType.functionInputKeys) {
    val key: Key = functionInputKey.key
    when (key.type.name) {
      TypeConstants.TEXT -> expectedJson.addProperty(
        key.name,
        validateOrEvaluateExpression(
          jsonParams = gson.fromJson(functionInputKey.expression!!, JsonObject::class.java).apply {
            addProperty("expectedReturnType", key.type.name)
          }, mode = "evaluate", symbols = symbols
        ) as String
      )
      TypeConstants.NUMBER -> expectedJson.addProperty(
        key.name,
        validateOrEvaluateExpression(
          jsonParams = gson.fromJson(functionInputKey.expression!!, JsonObject::class.java).apply {
            addProperty("expectedReturnType", key.type.name)
          }, mode = "evaluate", symbols = symbols
        ) as Long
      )
      TypeConstants.DECIMAL -> expectedJson.addProperty(
        key.name,
        validateOrEvaluateExpression(
          jsonParams = gson.fromJson(functionInputKey.expression!!, JsonObject::class.java).apply {
            addProperty("expectedReturnType", key.type.name)
          }, mode = "evaluate", symbols = symbols
        ) as BigDecimal
      )
      TypeConstants.BOOLEAN -> expectedJson.addProperty(
        key.name,
        validateOrEvaluateExpression(
          jsonParams = gson.fromJson(functionInputKey.expression!!, JsonObject::class.java).apply {
            addProperty("expectedReturnType", key.type.name)
          }, mode = "evaluate", symbols = symbols
        ) as Boolean
      )
      TypeConstants.DATE -> expectedJson.addProperty(
        key.name,
        (validateOrEvaluateExpression(
          jsonParams = gson.fromJson(functionInputKey.expression!!, JsonObject::class.java).apply {
            addProperty("expectedReturnType", key.type.name)
          }, mode = "evaluate", symbols = symbols
        ) as java.sql.Date).toString()
      )
      TypeConstants.TIMESTAMP -> expectedJson.addProperty(
        key.name,
        (validateOrEvaluateExpression(
          jsonParams = gson.fromJson(functionInputKey.expression!!, JsonObject::class.java).apply {
            addProperty("expectedReturnType", key.type.name)
          }, mode = "evaluate", symbols = symbols
        ) as Timestamp).time
      )
      TypeConstants.TIME -> expectedJson.addProperty(
        key.name,
        (validateOrEvaluateExpression(
          jsonParams = gson.fromJson(functionInputKey.expression!!, JsonObject::class.java).apply {
            addProperty("expectedReturnType", key.type.name)
          }, mode = "evaluate", symbols = symbols
        ) as java.sql.Time).time
      )
      TypeConstants.FORMULA, TypeConstants.BLOB -> {
      }
      else -> {
        expectedJson.addProperty(
          key.name,
          validateOrEvaluateExpression(
            jsonParams = gson.fromJson(functionInputKey.expression!!, JsonObject::class.java).apply {
              addProperty("expectedReturnType", TypeConstants.TEXT)
            }, mode = "evaluate", symbols = symbols
          ) as String
        )
      }
    }
  }
  return expectedJson
}
