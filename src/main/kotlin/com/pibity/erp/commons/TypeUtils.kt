/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.KeyConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.constants.formulaReturnTypes
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.Type
import org.codehaus.janino.ExpressionEvaluator
import java.util.regex.Pattern

val typeIdentifierPattern: Pattern = Pattern.compile("^[A-Z][a-zA-Z0-9]*$")

val keyIdentifierPattern: Pattern = Pattern.compile("^[a-z][a-zA-Z0-9]*$")

val keyTypeIdentifierPattern: Pattern = Pattern.compile("^([A-Z][a-zA-Z0-9]*)?(::[A-Z][a-zA-Z0-9]*)?\$")

fun validateTypeName(typeName: String): String {
  if (!typeIdentifierPattern.matcher(typeName).matches())
    throw CustomJsonException("{typeName: 'Type name $typeName is not a valid identifier'}")
  return typeName
}

fun validateSuperTypeName(typeName: String): String {
  if (!typeIdentifierPattern.matcher(typeName).matches())
    throw CustomJsonException("{superTypeName: 'Type name $typeName is not a valid identifier'}")
  return typeName
}

fun validateTypeKeys(keys: JsonObject): JsonObject {
  val expectedKeys = JsonObject()
  for ((keyName, keyObject) in keys.entrySet()) {
    val expectedKey = JsonObject()
    val key = keyObject.asJsonObject
    if (!keyIdentifierPattern.matcher(keyName).matches())
      throw CustomJsonException("{keys: {$keyName: 'Key name is not a valid identifier'}}")
    // validate type for key
    if (!key.has(KeyConstants.KEY_TYPE))
      throw CustomJsonException("{keys: {$keyName: {${KeyConstants.KEY_TYPE}: 'Type is not provided'}}}")
    else {
      if (key.get(KeyConstants.KEY_TYPE).isJsonObject) {
        val nestedType: JsonObject = key.get(KeyConstants.KEY_TYPE).asJsonObject
        if (!nestedType.has("typeName"))
          throw CustomJsonException("{keys: {$keyName: {${KeyConstants.KEY_TYPE}: {typeName: 'Field is missing in request body'}}}}")
        val nestedTypeName: String = try {
          nestedType.get("typeName").asString
        } catch (exception: Exception) {
          throw CustomJsonException("{keys: {$keyName: {${KeyConstants.KEY_TYPE}: {typeName: 'Unexpected value for parameter'}}}}")
        }
        if (!nestedType.has("displayName"))
          throw CustomJsonException("{keys: {$keyName: {${KeyConstants.KEY_TYPE}: {displayName: 'Field is missing in request body'}}}}")
        try {
          nestedType.get("displayName").asString
        } catch (exception: Exception) {
          throw CustomJsonException("{keys: {$keyName: {${KeyConstants.KEY_TYPE}: {displayName: 'Unexpected value for parameter'}}}}")
        }
        if (!nestedType.has("keys"))
          throw CustomJsonException("{keys: {$keyName: {${KeyConstants.KEY_TYPE}: {keys: 'Field is missing in request body'}}}}")
        val nestedTypeKeys: JsonObject = try {
          nestedType.get("keys").asJsonObject
        } catch (exception: Exception) {
          throw CustomJsonException("{keys: {$keyName: {${KeyConstants.KEY_TYPE}: {keys: 'Unexpected value for parameter'}}}}")
        }
        try {
          validateTypeName(nestedTypeName)
          validateTypeKeys(nestedTypeKeys)
          expectedKey.add(KeyConstants.KEY_TYPE, JsonObject().apply {
            addProperty("typeName", nestedTypeName)
            addProperty("displayName", nestedType.get("displayName").asString)
            add("keys", nestedTypeKeys)
          })
        } catch (exception: CustomJsonException) {
          throw CustomJsonException("{keys: {$keyName: {${KeyConstants.KEY_TYPE}: ${exception.message}}}}")
        }
        // validate default value for key
        if (key.has(KeyConstants.DEFAULT)) {
          try {
            when (key.get(KeyConstants.KEY_TYPE).asJsonObject.get("typeName").asString) {
              TypeConstants.TEXT -> expectedKey.addProperty(KeyConstants.DEFAULT, key.get(KeyConstants.DEFAULT).asString)
              TypeConstants.NUMBER -> expectedKey.addProperty(KeyConstants.DEFAULT, key.get(KeyConstants.DEFAULT).asLong)
              TypeConstants.DECIMAL -> expectedKey.addProperty(KeyConstants.DEFAULT, key.get(KeyConstants.DEFAULT).asDouble)
              TypeConstants.BOOLEAN -> expectedKey.addProperty(KeyConstants.DEFAULT, key.get(KeyConstants.DEFAULT).asBoolean)
              TypeConstants.LIST, TypeConstants.FORMULA -> {
              }
              else -> expectedKey.addProperty(KeyConstants.DEFAULT, key.get(KeyConstants.DEFAULT).asString)
            }
          } catch (exception: Exception) {
            throw CustomJsonException("{keys: {$keyName: {${KeyConstants.DEFAULT}: 'Default value for key is not valid'}}}")
          }
        }
      } else {
        try {
          expectedKey.addProperty(KeyConstants.KEY_TYPE, key.get(KeyConstants.KEY_TYPE).asString)
        } catch (exception: Exception) {
          throw CustomJsonException("{keys: {$keyName: {${KeyConstants.KEY_TYPE}: 'Unexpected value for parameter'}}}")
        }
        // Validate type name for key
        if (!keyTypeIdentifierPattern.matcher(key.get(KeyConstants.KEY_TYPE).asString).matches())
          throw CustomJsonException("{keys: {$keyName: {${KeyConstants.KEY_TYPE}: 'Type name for key is not valid'}}}")
        // validate default value for key
        if (key.has(KeyConstants.DEFAULT)) {
          try {
            when (key.get(KeyConstants.KEY_TYPE).asString) {
              TypeConstants.TEXT -> expectedKey.addProperty(KeyConstants.DEFAULT, key.get(KeyConstants.DEFAULT).asString)
              TypeConstants.NUMBER -> expectedKey.addProperty(KeyConstants.DEFAULT, key.get(KeyConstants.DEFAULT).asLong)
              TypeConstants.DECIMAL -> expectedKey.addProperty(KeyConstants.DEFAULT, key.get(KeyConstants.DEFAULT).asDouble)
              TypeConstants.BOOLEAN -> expectedKey.addProperty(KeyConstants.DEFAULT, key.get(KeyConstants.DEFAULT).asBoolean)
              TypeConstants.LIST, TypeConstants.FORMULA -> {
              }
              else -> expectedKey.addProperty(KeyConstants.DEFAULT, key.get(KeyConstants.DEFAULT).asString)
            }
          } catch (exception: Exception) {
            throw CustomJsonException("{keys: {$keyName: {${KeyConstants.DEFAULT}: 'Default value for key is not valid'}}}")
          }
        }
        when (key.get(KeyConstants.KEY_TYPE).asString) {
          TypeConstants.LIST -> {
            if (!key.has(KeyConstants.LIST_TYPE))
              throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_TYPE}: 'List Type is not provided'}}}")
            else {
              if (key.get(KeyConstants.LIST_TYPE).isJsonObject) {
                val nestedType: JsonObject = key.get(KeyConstants.LIST_TYPE).asJsonObject
                if (!nestedType.has("typeName"))
                  throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_TYPE}: {typeName: 'Field is missing in request body'}}}}")
                val nestedTypeName: String = try {
                  nestedType.get("typeName").asString
                } catch (exception: Exception) {
                  throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_TYPE}: {typeName: 'Unexpected value for parameter'}}}}")
                }
                if (!nestedType.has("displayName"))
                  throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_TYPE}: {displayName: 'Field is missing in request body'}}}}")
                try {
                  nestedType.get("displayName").asString
                } catch (exception: Exception) {
                  throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_TYPE}: {displayName: 'Unexpected value for parameter'}}}}")
                }
                if (!nestedType.has("keys"))
                  throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_TYPE}: {keys: 'Field is missing in request body'}}}}")
                val nestedTypeKeys: JsonObject = try {
                  nestedType.get("keys").asJsonObject
                } catch (exception: Exception) {
                  throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_TYPE}: {keys: 'Unexpected value for parameter'}}}}")
                }
                try {
                  validateTypeName(nestedTypeName)
                  validateTypeKeys(nestedTypeKeys)
                  expectedKey.add(KeyConstants.LIST_TYPE, JsonObject().apply {
                    addProperty("typeName", nestedTypeName)
                    addProperty("displayName", nestedType.get("displayName").asString)
                    add("keys", nestedTypeKeys)
                  })
                } catch (exception: CustomJsonException) {
                  throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_TYPE}: ${exception.message}}}}")
                }
              } else {
                try {
                  expectedKey.addProperty(KeyConstants.LIST_TYPE, key.get(KeyConstants.LIST_TYPE).asString)
                } catch (exception: Exception) {
                  throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_TYPE}: 'List Type is not valid'}}}")
                }
                // Validate list type name for key
                if (!keyTypeIdentifierPattern.matcher(key.get(KeyConstants.LIST_TYPE).asString).matches())
                  throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_TYPE}: 'Type name for key is not valid'}}}")
              }
            }
            if (!key.has(KeyConstants.LIST_MAX_SIZE))
              throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_MAX_SIZE}: 'Max size for List is not provided'}}}")
            else {
              val listMaxSize: Int = try {
                key.get(KeyConstants.LIST_MAX_SIZE).asInt
              } catch (exception: CustomJsonException) {
                throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_MAX_SIZE}: 'Max size for List is not valid'}}}")
              }
              expectedKey.addProperty(KeyConstants.LIST_MAX_SIZE, listMaxSize)
              if (listMaxSize != -1 && listMaxSize < 1)
                throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_MAX_SIZE}: 'Max size for List is not valid'}}}")
            }
            if (!key.has(KeyConstants.LIST_MIN_SIZE))
              throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_MIN_SIZE}: 'Min size for List is not provided'}}}")
            else {
              val listMinSize: Int = try {
                key.get(KeyConstants.LIST_MIN_SIZE).asInt
              } catch (exception: CustomJsonException) {
                throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_MIN_SIZE}: 'Min size for List is not valid'}}}")
              }
              expectedKey.addProperty(KeyConstants.LIST_MIN_SIZE, listMinSize)
              if (listMinSize < 0)
                throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_MIN_SIZE}: 'Min size for List is not valid'}}}")
            }
          }
          TypeConstants.FORMULA -> {
            if (!key.has(KeyConstants.FORMULA_EXPRESSION))
              throw CustomJsonException("{keys: {$keyName: {${KeyConstants.FORMULA_EXPRESSION}: 'Formula expression is not provided'}}}")
            else {
              try {
                expectedKey.addProperty(KeyConstants.FORMULA_EXPRESSION, key.get(KeyConstants.FORMULA_EXPRESSION).asString)
              } catch (exception: Exception) {
                throw CustomJsonException("{keys: {$keyName: {${KeyConstants.FORMULA_EXPRESSION}: 'Formula expression is not valid'}}}")
              }
            }
            if (!key.has(KeyConstants.FORMULA_RETURN_TYPE))
              throw CustomJsonException("{keys: {$keyName: {${KeyConstants.FORMULA_RETURN_TYPE}: 'Return Type is not provided'}}}")
            else {
              try {
                expectedKey.addProperty(KeyConstants.FORMULA_RETURN_TYPE, key.get(KeyConstants.FORMULA_RETURN_TYPE).asString)
              } catch (exception: Exception) {
                throw CustomJsonException("{keys: {$keyName: {${KeyConstants.FORMULA_RETURN_TYPE}: 'Return Type is not valid'}}}")
              }
            }
          }
        }
      }
    }
    // validate order for key
    if (!key.has(KeyConstants.ORDER))
      throw CustomJsonException("{keys: {$keyName: {${KeyConstants.ORDER}: 'Key order is not provided'}}}")
    else {
      try {
        expectedKey.addProperty(KeyConstants.ORDER, key.get(KeyConstants.ORDER).asInt)
      } catch (exception: Exception) {
        throw CustomJsonException("{keys: {$keyName: {${KeyConstants.ORDER}: 'Order for key is not valid'}}}")
      }
    }
    // validate displayName for key
    if (key.has(KeyConstants.DISPLAY_NAME)) {
      try {
        expectedKey.addProperty(KeyConstants.DISPLAY_NAME, key.get(KeyConstants.DISPLAY_NAME).asString)
      } catch (exception: Exception) {
        throw CustomJsonException("{keys: {$keyName: {${KeyConstants.DISPLAY_NAME}: 'Display name for key is not valid'}}}")
      }
    }
    expectedKeys.add(keyName, expectedKey)
  }
  return expectedKeys
}

fun validateFormulaTypeKeys(type: Type) {
  // Construct injectable keys with appropriate classes
  val leafKeyTypeAndValues: Map<String, Map<String, String>> = getLeafNameTypeValues(prefix = null, keys = mutableMapOf(), type = type, depth = 0)
  val injectedKeys: Array<String?> = arrayOfNulls(leafKeyTypeAndValues.size)
  val injectedClasses: Array<Class<*>?> = arrayOfNulls(leafKeyTypeAndValues.size)
  var index = 0
  for ((key, value) in leafKeyTypeAndValues) {
    injectedKeys[index] = key
    when (value[KeyConstants.KEY_TYPE]) {
      TypeConstants.TEXT -> injectedClasses[index] = String::class.javaObjectType
      TypeConstants.NUMBER -> injectedClasses[index] = Long::class.javaObjectType
      TypeConstants.DECIMAL -> injectedClasses[index] = Double::class.javaObjectType
      TypeConstants.BOOLEAN -> injectedClasses[index] = Boolean::class.javaObjectType
    }
    index += 1
  }
  // Validate all formula type keys
  type.keys.filter { it.type.id.name == TypeConstants.FORMULA }.forEach {
    try {
      val evaluator = ExpressionEvaluator()
      evaluator.setParameters(injectedKeys, injectedClasses)
      when (it.formula!!.returnType.id.name) {
        TypeConstants.TEXT -> evaluator.setExpressionType(String::class.javaObjectType)
        TypeConstants.NUMBER -> evaluator.setExpressionType(Long::class.javaObjectType)
        TypeConstants.DECIMAL -> evaluator.setExpressionType(Double::class.javaObjectType)
        TypeConstants.BOOLEAN -> evaluator.setExpressionType(Boolean::class.javaObjectType)
      }
      evaluator.cook(it.formula!!.expression)
    } catch (exception: Exception) {
      throw CustomJsonException("{keys:{${it.id.name}: {${KeyConstants.FORMULA_EXPRESSION}: 'Expression for formula is not valid'}}}")
    }
  }
}

fun getLeafNameTypeValues(prefix: String?, keys: MutableMap<String, Map<String, String>>, type: Type, depth: Int): Map<String, Map<String, String>> {
  for (key in type.keys) {
    val keyName: String = if (prefix != null) prefix + "_" + key.id.name else key.id.name
    val keyTypeAndValue: MutableMap<String, String> = HashMap()
    when (key.type.id.name) {
      in formulaReturnTypes -> {
        keyTypeAndValue[KeyConstants.KEY_TYPE] = key.type.id.name
        keys[keyName] = keyTypeAndValue
      }
      TypeConstants.LIST -> {
      }
      TypeConstants.FORMULA -> {
        // Formulas at base level cannot be used inside other formulas at base level.
        if (depth != 0) {
          when (key.formula!!.returnType.id.name) {
            in formulaReturnTypes -> {
              keyTypeAndValue[KeyConstants.KEY_TYPE] = key.formula!!.returnType.id.name
              keys[keyName] = keyTypeAndValue
            }
          }
        }
      }
      else -> getLeafNameTypeValues(prefix = keyName, keys = keys, type = key.type, depth = 1 + depth)
    }
  }
  return keys
}
