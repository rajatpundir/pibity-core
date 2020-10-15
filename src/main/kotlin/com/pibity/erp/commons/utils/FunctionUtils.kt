/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons.utils

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.GLOBAL_TYPE
import com.pibity.erp.commons.constants.KeyConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.constants.primitiveTypes
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.Key
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.Variable
import com.pibity.erp.entities.function.FunctionInput
import com.pibity.erp.entities.function.FunctionOutput
import com.pibity.erp.entities.function.FunctionOutputKey
import com.pibity.erp.entities.function.FunctionOutputType

fun validateFunctionName(functionName: String): String {
  if (!keyIdentifierPattern.matcher(functionName).matches())
    throw CustomJsonException("{functionName: 'Function name $functionName is not a valid identifier'}")
  return functionName
}

fun getValueSymbolPaths(values: JsonObject, type: Type, paramsAreInputs: Boolean): Set<String> {
  val symbolPaths: MutableSet<String> = mutableSetOf()
  for (key in type.keys) {
    if (key.type.id.name != TypeConstants.FORMULA) {
      if (paramsAreInputs && !values.has(key.id.name))
        continue
      val keyExpression: JsonObject = if (!values.has(key.id.name))
        throw CustomJsonException("{${key.id.name}: 'Field is missing in request body'}")
      else {
        try {
          values.get(key.id.name).asJsonObject
        } catch (exception: Exception) {
          throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
        }
      }
      when (key.type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> {
          try {
            symbolPaths.addAll(validateOrEvaluateExpression(jsonParams = keyExpression.deepCopy().apply { addProperty("expectedReturnType", key.type.id.name) }, mode = "collect", symbols = JsonObject()) as Set<String>)
          } catch (exception: CustomJsonException) {
            throw CustomJsonException("{${key.id.name}: ${exception.message}}")
          }
        }
        TypeConstants.FORMULA, TypeConstants.LIST -> {
        }
        else -> {
          if (key.type.id.superTypeName == GLOBAL_TYPE) {
            try {
              symbolPaths.addAll(validateOrEvaluateExpression(jsonParams = keyExpression.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) }, mode = "collect", symbols = JsonObject()) as Set<String>)
            } catch (exception: CustomJsonException) {
              throw CustomJsonException("{${key.id.name}: ${exception.message}}")
            }
          } else {
            if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
              try {
                symbolPaths.addAll(getValueSymbolPaths(values = keyExpression, type = key.type, paramsAreInputs = paramsAreInputs))
              } catch (exception: CustomJsonException) {
                throw CustomJsonException("{${key.id.name}: ${exception.message}}")
              }
            } else {
              val contextExpression: JsonObject = if (!keyExpression.has("context"))
                throw CustomJsonException("{${key.id.name}: {context: 'Field is missing in request body'}}")
              else {
                try {
                  keyExpression.get("context").asJsonObject
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.id.name}: {context: 'Unexpected value for parameter'}}")
                }
              }
              val variableNameExpression: JsonObject = if (!keyExpression.has("variableName"))
                throw CustomJsonException("{${key.id.name}: {variableName: 'Field is missing in request body'}}")
              else {
                try {
                  keyExpression.get("variableName").asJsonObject
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.id.name}: {variableName: 'Unexpected value for parameter'}}")
                }
              }
              try {
                symbolPaths.addAll(validateOrEvaluateExpression(jsonParams = contextExpression.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.NUMBER) }, mode = "collect", symbols = JsonObject()) as Set<String>)
              } catch (exception: CustomJsonException) {
                throw CustomJsonException("{${key.id.name}: {context: ${exception.message}}}")
              }
              try {
                symbolPaths.addAll(validateOrEvaluateExpression(jsonParams = variableNameExpression.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) }, mode = "collect", symbols = JsonObject()) as Set<String>)
              } catch (exception: CustomJsonException) {
                throw CustomJsonException("{${key.id.name}: {variableName: ${exception.message}}}")
              }
            }
          }
        }
      }
    }
  }
  return symbolPaths
}

fun getSymbolPathsForInputsOrOutputs(jsonParams: JsonObject, globalTypes: Set<Type>, paramsAreInputs: Boolean): Set<String> {
  val symbolPaths: MutableSet<String> = mutableSetOf()
  for ((keyName, json) in jsonParams.entrySet()) {
    if (!keyIdentifierPattern.matcher(keyName).matches())
      throw CustomJsonException("{${keyName}: 'Parameter name is not a valid identifier'}")
    if (paramsAreInputs && !json.isJsonObject) {
      val type: Type = try {
        globalTypes.single { it.id.name == json.asString }
      } catch (exception: Exception) {
        throw CustomJsonException("{${keyName}: 'Unexpected value for parameter'}")
      }
      if (!primitiveTypes.contains(type.id.name))
        throw CustomJsonException("{${keyName}: 'Unexpected value for parameter'}")
      if (listOf(TypeConstants.FORMULA, TypeConstants.LIST).contains(type.id.name))
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
          globalTypes.single { it.id.name == keyJson.get("type").asString }
        } catch (exception: Exception) {
          throw CustomJsonException("{${keyName}: {type: 'Unexpected value for parameter'}}")
        }
      }
      if (listOf(TypeConstants.FORMULA, TypeConstants.LIST).contains(type.id.name))
        throw CustomJsonException("{${keyName}: {type: 'Unexpected value for parameter'}}")
      if (paramsAreInputs) {
        if (!primitiveTypes.contains(type.id.name)) {
          if (keyJson.has("variableName")) {
            val variableNameExpression: JsonObject = try {
              keyJson.get("variableName").asJsonObject
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {variableName: 'Unexpected value for parameter'}}")
            }
            try {
              symbolPaths.addAll(validateOrEvaluateExpression(jsonParams = variableNameExpression.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) }, mode = "collect", symbols = JsonObject()) as Set<String>)
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
        if (!primitiveTypes.contains(type.id.name)) {
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
            symbolPaths.addAll(validateOrEvaluateExpression(jsonParams = variableNameExpression.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) }, mode = "collect", symbols = JsonObject()) as Set<String>)
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
            symbolPaths.addAll(validateOrEvaluateExpression(jsonParams = values.deepCopy().apply { addProperty("expectedReturnType", type.id.name) }, mode = "collect", symbols = JsonObject()) as Set<String>)
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
    if (paramsAreInputs && !values.has(key.id.name))
      continue
    if (key.type.id.name != TypeConstants.FORMULA) {
      val keyExpression: JsonObject = if (!values.has(key.id.name))
        throw CustomJsonException("{${key.id.name}: 'Field is missing in request body'}")
      else {
        try {
          values.get(key.id.name).asJsonObject
        } catch (exception: Exception) {
          throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
        }
      }
      when (key.type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> {
          try {
            validateOrEvaluateExpression(jsonParams = keyExpression.deepCopy().apply { addProperty("expectedReturnType", key.type.id.name) }, mode = "validate", symbols = symbols)
          } catch (exception: CustomJsonException) {
            throw CustomJsonException("{${key.id.name}: ${exception.message}}")
          }
          expectedValue.add(key.id.name, keyExpression)
        }
        TypeConstants.FORMULA, TypeConstants.LIST -> {
        }
        else -> {
          if (key.type.id.superTypeName == GLOBAL_TYPE) {
            try {
              validateOrEvaluateExpression(jsonParams = keyExpression.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) }, mode = "validate", symbols = symbols)
            } catch (exception: CustomJsonException) {
              throw CustomJsonException("{${key.id.name}: ${exception.message}}")
            }
            expectedValue.add(key.id.name, keyExpression)
          } else {
            if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
              try {
                expectedValue.add(key.id.name, validateValue(values = keyExpression, type = key.type, symbols = symbols, paramsAreInputs = paramsAreInputs))
              } catch (exception: CustomJsonException) {
                throw CustomJsonException("{${key.id.name}: ${exception.message}}")
              }
            } else {
              val contextExpression: JsonObject = if (!keyExpression.has("context"))
                throw CustomJsonException("{${key.id.name}: {context: 'Field is missing in request body'}}")
              else {
                try {
                  keyExpression.get("context").asJsonObject
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.id.name}: {context: 'Unexpected value for parameter'}}")
                }
              }
              val variableNameExpression: JsonObject = if (!keyExpression.has("variableName"))
                throw CustomJsonException("{${key.id.name}: {variableName: 'Field is missing in request body'}}")
              else {
                try {
                  keyExpression.get("variableName").asJsonObject
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.id.name}: {variableName: 'Unexpected value for parameter'}}")
                }
              }
              try {
                validateOrEvaluateExpression(jsonParams = contextExpression.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.NUMBER) }, mode = "validate", symbols = symbols)
              } catch (exception: CustomJsonException) {
                throw CustomJsonException("{${key.id.name}: {context: ${exception.message}}}")
              }
              try {
                validateOrEvaluateExpression(jsonParams = variableNameExpression.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) }, mode = "validate", symbols = symbols)
              } catch (exception: CustomJsonException) {
                throw CustomJsonException("{${key.id.name}: {variableName: ${exception.message}}}")
              }
              expectedValue.add(key.id.name, JsonObject().apply {
                add("context", contextExpression)
                add("variableName", variableNameExpression)
              })
            }
          }
        }
      }
    }
  }
  return expectedValue
}

fun validateInputsOrOutputs(jsonParams: JsonObject, globalTypes: Set<Type>, symbols: JsonObject, symbolPaths: Set<String>, paramsAreInputs: Boolean): JsonObject {
  val expectedJson = JsonObject()
  for ((keyName, json) in jsonParams.entrySet()) {
    val expectedKeyJson = JsonObject()
    if (!keyIdentifierPattern.matcher(keyName).matches())
      throw CustomJsonException("{${keyName}: 'Parameter name is not a valid identifier'}")
    if (paramsAreInputs && symbolPaths.none { it == keyName || it.startsWith(prefix = "$keyName.") })
      continue
    if (paramsAreInputs && !json.isJsonObject) {
      val type: Type = try {
        globalTypes.single { it.id.name == json.asString }
      } catch (exception: Exception) {
        throw CustomJsonException("{${keyName}: 'Unexpected value for parameter'}")
      }
      if (!primitiveTypes.contains(type.id.name))
        throw CustomJsonException("{${keyName}: 'Unexpected value for parameter'}")
      if (listOf(TypeConstants.FORMULA, TypeConstants.LIST).contains(type.id.name))
        throw CustomJsonException("{${keyName}: 'Unexpected value for parameter'}")
      expectedJson.addProperty(keyName, type.id.name)
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
          globalTypes.single { it.id.name == keyJson.get("type").asString }
        } catch (exception: Exception) {
          throw CustomJsonException("{${keyName}: {type: 'Unexpected value for parameter'}}")
        }
      }
      if (listOf(TypeConstants.FORMULA, TypeConstants.LIST).contains(type.id.name))
        throw CustomJsonException("{${keyName}: {type: 'Unexpected value for parameter'}}")
      expectedKeyJson.addProperty("type", type.id.name)
      if (paramsAreInputs) {
        if (!primitiveTypes.contains(type.id.name)) {
          if (keyJson.has("variableName")) {
            val variableNameExpression: JsonObject = try {
              keyJson.get("variableName").asJsonObject
            } catch (exception: Exception) {
              throw CustomJsonException("{${keyName}: {variableName: 'Unexpected value for parameter'}}")
            }
            try {
              validateOrEvaluateExpression(jsonParams = variableNameExpression.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) }, mode = "validate", symbols = symbols)
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
            expectedKeyJson.add("values", try {
              validateValue(values = values, type = type, symbols = symbols, paramsAreInputs = paramsAreInputs)
            } catch (exception: CustomJsonException) {
              throw CustomJsonException("{${keyName}: {values: ${exception.message}}}")
            })
          }
        }
      } else {
        if (!primitiveTypes.contains(type.id.name)) {
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
            validateOrEvaluateExpression(jsonParams = variableNameExpression.deepCopy().apply { addProperty("expectedReturnType", TypeConstants.TEXT) }, mode = "validate", symbols = symbols)
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
          expectedKeyJson.add("values", try {
            validateValue(values = values, type = type, symbols = symbols, paramsAreInputs = paramsAreInputs)
          } catch (exception: CustomJsonException) {
            throw CustomJsonException("{${keyName}: {values: ${exception.message}}}")
          })
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
            validateOrEvaluateExpression(jsonParams = values.deepCopy().apply { addProperty("expectedReturnType", type.id.name) }, mode = "validate", symbols = symbols)
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

fun validateInputs(jsonParams: JsonObject, globalTypes: Set<Type>, symbols: JsonObject, symbolPaths: Set<String>): JsonObject {
  return try {
    validateInputsOrOutputs(jsonParams = jsonParams, globalTypes = globalTypes, symbols = symbols, symbolPaths = symbolPaths, paramsAreInputs = true)
  } catch (exception: CustomJsonException) {
    throw CustomJsonException("{inputs: ${exception.message}}")
  }
}

fun validateOutputs(jsonParams: JsonObject, globalTypes: Set<Type>, symbols: JsonObject, symbolPaths: Set<String>): JsonObject {
  return try {
    validateInputsOrOutputs(jsonParams = jsonParams, globalTypes = globalTypes, symbols = symbols, symbolPaths = symbolPaths, paramsAreInputs = false)
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
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> {
          symbols.add(inputName, JsonObject().apply { addProperty(KeyConstants.KEY_TYPE, typeName) })
        }
        TypeConstants.FORMULA, TypeConstants.LIST -> {
        }
        else -> {
          val subSymbols: JsonObject = getInputTypeSymbols(type = globalTypes.single { it.id.name == typeName }, symbolPaths = symbolPaths, prefix = "$inputName.")
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
    if (symbolPaths.any { it.startsWith(prefix = prefix + key.id.name) }) {
      when (key.type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN ->
          symbols.add(key.id.name, JsonObject().apply { addProperty(KeyConstants.KEY_TYPE, key.type.id.name) })
        TypeConstants.FORMULA ->
          symbols.add(key.id.name, JsonObject().apply { addProperty(KeyConstants.KEY_TYPE, key.formula!!.returnType.id.name) })
        TypeConstants.LIST -> {
        }
        else -> {
          if (key.type.id.superTypeName == GLOBAL_TYPE) {
            val subSymbols: JsonObject = if (symbolPaths.any { it.startsWith(prefix = prefix + key.id.name + ".") })
              getInputTypeSymbols(type = key.type, symbolPaths = symbolPaths, prefix = prefix + key.id.name + ".")
            else JsonObject()
            symbols.add(key.id.name, JsonObject().apply {
              addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
              if (subSymbols.size() != 0)
                add("values", subSymbols)
            })
          } else {
            if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
              val subSymbols: JsonObject = getInputTypeSymbols(type = key.type, symbolPaths = symbolPaths, prefix = prefix + key.id.name + ".")
              symbols.add(key.id.name, JsonObject().apply {
                addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
                if (subSymbols.size() != 0)
                  add("values", subSymbols)
              })
            } else {
              val subSymbols: JsonObject = if (symbolPaths.any { it.startsWith(prefix = prefix + key.id.name + ".") })
                getInputTypeSymbols(type = key.type, symbolPaths = symbolPaths, prefix = prefix + key.id.name + ".")
              else JsonObject()
              symbols.add(key.id.name + "::context", JsonObject().apply {
                addProperty(KeyConstants.KEY_TYPE, TypeConstants.NUMBER)
              })
              symbols.add(key.id.name, JsonObject().apply {
                addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
                if (subSymbols.size() != 0)
                  add("values", subSymbols)
              })
            }
          }
        }
      }
    }
  }
  return symbols
}

fun getKeysForPaths(symbolPaths: MutableSet<String>, type: Type, prefix: String = ""): Set<Key> {
  val keyDependencies: MutableSet<Key> = mutableSetOf()
  for (key in type.keys) {
    if (symbolPaths.any { it.startsWith(prefix = prefix + key.id.name) }) {
      when (key.type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.FORMULA -> {
          keyDependencies.add(key)
          symbolPaths.remove(prefix + key.id.name)
        }
        TypeConstants.LIST -> {
        }
        else -> {
          if (key.type.id.superTypeName == GLOBAL_TYPE) {
            if (symbolPaths.any { it.startsWith(prefix = prefix + key.id.name + ".") })
              keyDependencies.addAll(getKeysForPaths(symbolPaths = symbolPaths, type = key.type, prefix = prefix + key.id.name + "."))
            keyDependencies.add(key)
            symbolPaths.remove(prefix + key.id.name)
          } else {
            if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.type.id.superTypeName))
              keyDependencies.addAll(getKeysForPaths(symbolPaths = symbolPaths, type = key.type, prefix = prefix + key.id.name + "."))
            else {
              if (symbolPaths.any { it.startsWith(prefix = prefix + key.id.name + ".") })
                keyDependencies.addAll(getKeysForPaths(symbolPaths = symbolPaths, type = key.type, prefix = prefix + key.id.name + "."))
              keyDependencies.add(key)
              symbolPaths.remove(prefix + key.id.name)
            }
          }
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
      val type: Type = globalTypes.single { it.id.name == inputType.asJsonObject.get("type").asString }
      when (type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.FORMULA, TypeConstants.LIST -> {
        }
        else -> keyDependencies.addAll(getKeysForPaths(symbolPaths = symbolPaths, type = type, prefix = "$inputName."))
      }
    }
  }
  return keyDependencies
}

fun validateFunctionArgs(args: JsonObject, inputs: Set<FunctionInput>): JsonObject {
  val expectedJson = JsonObject()
  for (input in inputs) {
    if (args.has(input.id.name)) {
      when (input.type.id.name) {
        TypeConstants.TEXT -> expectedJson.addProperty(input.id.name, try {
          args.get(input.id.name).asString
        } catch (exception: Exception) {
          throw CustomJsonException("{args: {${input.id.name}: 'Unexpected value for parameter'}}")
        })
        TypeConstants.NUMBER -> expectedJson.addProperty(input.id.name, try {
          args.get(input.id.name).asLong
        } catch (exception: Exception) {
          throw CustomJsonException("{args: {${input.id.name}: 'Unexpected value for parameter'}}")
        })
        TypeConstants.DECIMAL -> expectedJson.addProperty(input.id.name, try {
          args.get(input.id.name).asDouble
        } catch (exception: Exception) {
          throw CustomJsonException("{args: {${input.id.name}: 'Unexpected value for parameter'}}")
        })
        TypeConstants.BOOLEAN -> expectedJson.addProperty(input.id.name, try {
          args.get(input.id.name).asBoolean
        } catch (exception: Exception) {
          throw CustomJsonException("{args: {${input.id.name}: 'Unexpected value for parameter'}}")
        })
        TypeConstants.LIST, TypeConstants.FORMULA -> {
        }
        else -> expectedJson.addProperty(input.id.name, try {
          args.get(input.id.name).asString
        } catch (exception: Exception) {
          throw CustomJsonException("{args: {${input.id.name}: 'Unexpected value for parameter'}}")
        })
      }
    } else throw CustomJsonException("{args: {${input.id.name}: 'Field is missing in request body'}}")
  }
  return expectedJson
}

fun getSymbolsForFunctionArgs(symbolPaths: Set<String>, variable: Variable, prefix: String): JsonObject {
  val expectedSymbols = JsonObject()
  for (value in variable.values) {
    if (symbolPaths.any { it.startsWith(prefix = prefix + value.id.key.id.name) }) {
      when (value.id.key.type.id.name) {
        TypeConstants.TEXT -> expectedSymbols.add(value.id.key.id.name, JsonObject().apply {
          addProperty(KeyConstants.KEY_TYPE, value.id.key.type.id.name)
          addProperty(KeyConstants.VALUE, value.stringValue!!)
        })
        TypeConstants.NUMBER -> expectedSymbols.add(value.id.key.id.name, JsonObject().apply {
          addProperty(KeyConstants.KEY_TYPE, value.id.key.type.id.name)
          addProperty(KeyConstants.VALUE, value.longValue!!)
        })
        TypeConstants.DECIMAL -> expectedSymbols.add(value.id.key.id.name, JsonObject().apply {
          addProperty(KeyConstants.KEY_TYPE, value.id.key.type.id.name)
          addProperty(KeyConstants.VALUE, value.doubleValue!!)
        })
        TypeConstants.BOOLEAN -> expectedSymbols.add(value.id.key.id.name, JsonObject().apply {
          addProperty(KeyConstants.KEY_TYPE, value.id.key.type.id.name)
          addProperty(KeyConstants.VALUE, value.booleanValue!!)
        })
        TypeConstants.FORMULA -> when (value.id.key.formula!!.returnType.id.name) {
          TypeConstants.TEXT -> expectedSymbols.add(value.id.key.id.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, value.id.key.formula!!.returnType.id.name)
            addProperty(KeyConstants.VALUE, value.stringValue!!)
          })
          TypeConstants.NUMBER -> expectedSymbols.add(value.id.key.id.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, value.id.key.formula!!.returnType.id.name)
            addProperty(KeyConstants.VALUE, value.longValue!!)
          })
          TypeConstants.DECIMAL -> expectedSymbols.add(value.id.key.id.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, value.id.key.formula!!.returnType.id.name)
            addProperty(KeyConstants.VALUE, value.doubleValue!!)
          })
          TypeConstants.BOOLEAN -> expectedSymbols.add(value.id.key.id.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, value.id.key.formula!!.returnType.id.name)
            addProperty(KeyConstants.VALUE, value.booleanValue!!)
          })
        }
        TypeConstants.LIST -> {
        }
        else -> {
          if (value.id.key.type.id.superTypeName == GLOBAL_TYPE) {
            val subSymbols: JsonObject = if (symbolPaths.any { it.startsWith(prefix = prefix + value.id.key.id.name + ".") })
              getSymbolsForFunctionArgs(symbolPaths = symbolPaths, variable = value.referencedVariable!!, prefix = prefix + value.id.key.id.name + ".")
            else JsonObject()
            expectedSymbols.add(value.id.key.id.name, JsonObject().apply {
              addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
              addProperty(KeyConstants.VALUE, value.referencedVariable!!.id.name)
              if (subSymbols.size() != 0)
                add("values", subSymbols)
            })
          } else {
            if ((value.id.key.id.parentType.id.superTypeName == GLOBAL_TYPE && value.id.key.id.parentType.id.name == value.id.key.type.id.superTypeName)
                || (value.id.key.id.parentType.id.superTypeName != GLOBAL_TYPE && value.id.key.id.parentType.id.superTypeName == value.id.key.type.id.superTypeName)) {
              expectedSymbols.add(value.id.key.id.name, JsonObject().apply {
                add("values", getSymbolsForFunctionArgs(symbolPaths = symbolPaths, variable = value.referencedVariable!!, prefix = prefix + value.id.key.id.name + "."))
              })
            } else {
              val subSymbols: JsonObject = if (symbolPaths.any { it.startsWith(prefix = prefix + value.id.key.id.name + ".") })
                getSymbolsForFunctionArgs(symbolPaths = symbolPaths, variable = value.referencedVariable!!, prefix = prefix + value.id.key.id.name + ".")
              else JsonObject()
              expectedSymbols.add(value.id.key.id.name + "::context", JsonObject().apply {
                addProperty(KeyConstants.KEY_TYPE, TypeConstants.NUMBER)
                addProperty(KeyConstants.VALUE, value.referencedVariable!!.id.superList.id)
              })
              expectedSymbols.add(value.id.key.id.name, JsonObject().apply {
                addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
                addProperty(KeyConstants.VALUE, value.referencedVariable!!.id.name)
                if (subSymbols.size() != 0)
                  add("values", subSymbols)
              })
            }
          }
        }
      }
    }
  }
  return expectedSymbols
}

fun getFunctionOutputTypeJson(functionOutputType: FunctionOutputType, symbols: JsonObject) :JsonObject {
  val expectedJson = JsonObject()
  for (functionOutputKey in functionOutputType.functionOutputKeys) {
    val key: Key = functionOutputKey.id.key
    when(key.type.id.name) {
      TypeConstants.TEXT -> expectedJson.addProperty(key.id.name, validateOrEvaluateExpression(jsonParams = gson.fromJson(functionOutputKey.expression!!, JsonObject::class.java).apply {
        addProperty("expectedReturnType", key.type.id.name)
      }, mode = "evaluate", symbols = symbols) as String)
      TypeConstants.NUMBER -> expectedJson.addProperty(key.id.name, validateOrEvaluateExpression(jsonParams = gson.fromJson(functionOutputKey.expression!!, JsonObject::class.java).apply {
        addProperty("expectedReturnType", key.type.id.name)
      }, mode = "evaluate", symbols = symbols) as Long)
      TypeConstants.DECIMAL -> expectedJson.addProperty(key.id.name, validateOrEvaluateExpression(jsonParams = gson.fromJson(functionOutputKey.expression!!, JsonObject::class.java).apply {
        addProperty("expectedReturnType", key.type.id.name)
      }, mode = "evaluate", symbols = symbols) as Double)
      TypeConstants.BOOLEAN -> expectedJson.addProperty(key.id.name, validateOrEvaluateExpression(jsonParams = gson.fromJson(functionOutputKey.expression!!, JsonObject::class.java).apply {
        addProperty("expectedReturnType", key.type.id.name)
      }, mode = "evaluate", symbols = symbols) as Boolean)
      TypeConstants.FORMULA, TypeConstants.LIST -> {}
      else -> {
        if (key.type.id.superTypeName == GLOBAL_TYPE) {
          expectedJson.addProperty(key.id.name, validateOrEvaluateExpression(jsonParams = gson.fromJson(functionOutputKey.expression!!, JsonObject::class.java).apply {
            addProperty("expectedReturnType", TypeConstants.TEXT)
          }, mode = "evaluate", symbols = symbols) as String)
        } else {
          if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.type.id.superTypeName)
              || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
            expectedJson.add(key.id.name, JsonObject().apply {
              add("values", getFunctionOutputTypeJson(functionOutputType = functionOutputKey.referencedFunctionOutputType!!, symbols = symbols))
            })
          } else {
            expectedJson.add(key.id.name, JsonObject().apply {
              addProperty("context", validateOrEvaluateExpression(jsonParams = gson.fromJson(functionOutputKey.expression!!, JsonObject::class.java).get("context").asJsonObject.apply {
                addProperty("expectedReturnType", TypeConstants.NUMBER)
              }, mode = "evaluate", symbols = symbols) as Long)
              addProperty("variableName", validateOrEvaluateExpression(jsonParams = gson.fromJson(functionOutputKey.expression!!, JsonObject::class.java).get("variableName").asJsonObject.apply {
                addProperty("expectedReturnType", TypeConstants.TEXT)
              }, mode = "evaluate", symbols = symbols) as String)
            })
          }
        }
      }
    }
  }
  println("-------getFunctionOutputTypeJson--------")
  println(expectedJson)
  return expectedJson
}
