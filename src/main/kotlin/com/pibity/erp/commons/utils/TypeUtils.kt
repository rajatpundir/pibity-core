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
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.Key
import com.pibity.erp.entities.Type
import java.util.regex.Pattern

val typeIdentifierPattern: Pattern = Pattern.compile("^[A-Z][a-zA-Z0-9]*$")

val keyIdentifierPattern: Pattern = Pattern.compile("^[a-z][a-zA-Z0-9]*$")

val keyTypeIdentifierPattern: Pattern = Pattern.compile("^([A-Z][a-zA-Z0-9]*)(::[A-Z][a-zA-Z0-9]*)?\$")

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
    val key: JsonObject = try {
      keyObject.asJsonObject
    } catch (exception: Exception) {
      throw CustomJsonException("{keys: {$keyName: 'Unexpected value for parameter'}}")
    }
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
        if (nestedType.has("multiplicity")) {
          try {
            nestedType.get("multiplicity").asLong
          } catch (exception: Exception) {
            throw CustomJsonException("{keys: {$keyName: {${KeyConstants.KEY_TYPE}: {multiplicity: 'Unexpected value for parameter'}}}}")
          }
        }
        try {
          validateTypeName(nestedTypeName)
          validateTypeKeys(nestedTypeKeys)
          expectedKey.add(KeyConstants.KEY_TYPE, JsonObject().apply {
            addProperty("typeName", nestedTypeName)
            addProperty("displayName", nestedType.get("displayName").asString)
            add("keys", nestedTypeKeys)
            if (nestedType.has("multiplicity") && nestedType.get("multiplicity").asLong >= 0)
              addProperty("multiplicity?", nestedType.get("multiplicity").asLong)
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
              else -> {
                /* Referential keys with local types does not make sense to have defaults */
              }
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
              else -> {
                // Keys with reference to local types of some global variable does not make sense to have default value, at the time of writing this line.
                if (!key.get(KeyConstants.KEY_TYPE).asString.contains("::"))
                  expectedKey.addProperty(KeyConstants.DEFAULT, key.get(KeyConstants.DEFAULT).asString)
              }
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
              if (listMaxSize < 0)
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
            if (!key.has(KeyConstants.FORMULA_RETURN_TYPE))
              throw CustomJsonException("{keys: {$keyName: {${KeyConstants.FORMULA_RETURN_TYPE}: 'Return Type is not provided'}}}")
            else {
              try {
                expectedKey.addProperty(KeyConstants.FORMULA_RETURN_TYPE, key.get(KeyConstants.FORMULA_RETURN_TYPE).asString)
              } catch (exception: Exception) {
                throw CustomJsonException("{keys: {$keyName: {${KeyConstants.FORMULA_RETURN_TYPE}: 'Return Type is not valid'}}}")
              }
            }
            if (!key.has(KeyConstants.FORMULA_EXPRESSION))
              throw CustomJsonException("{keys: {$keyName: {${KeyConstants.FORMULA_EXPRESSION}: 'Formula expression is not provided'}}}")
            else {
              try {
                expectedKey.add(KeyConstants.FORMULA_EXPRESSION, key.get(KeyConstants.FORMULA_EXPRESSION).asJsonObject)
              } catch (exception: Exception) {
                throw CustomJsonException("{keys: {$keyName: {${KeyConstants.FORMULA_EXPRESSION}: 'Formula expression is not valid'}}}")
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

fun getSymbols(type: Type, symbolPaths: MutableSet<String>, prefix: String = "", level: Int, keyDependencies: MutableSet<Key>): JsonObject {
  val symbols = JsonObject()
  for (key in type.keys) {
    when (key.type.id.name) {
      TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> if (symbolPaths.contains(prefix + key.id.name)) {
        symbols.add(key.id.name, JsonObject().apply { addProperty(KeyConstants.KEY_TYPE, key.type.id.name) })
        symbolPaths.remove(prefix + key.id.name)
        keyDependencies.add(key.apply { isDependency = true })
      }
      TypeConstants.LIST -> {
      }
      TypeConstants.FORMULA -> {
        if (level != 0 && symbolPaths.contains(prefix + key.id.name)) {
          symbols.add(key.id.name, JsonObject().apply { addProperty(KeyConstants.KEY_TYPE, key.formula!!.returnType.id.name) })
          symbolPaths.remove(prefix + key.id.name)
          keyDependencies.add(key.apply { isDependency = true })
        }
      }
      else -> if (symbolPaths.any { it.startsWith(prefix = prefix + key.id.name) }) {
        if (key.type.id.superTypeName == GLOBAL_TYPE) {
          val subSymbols: JsonObject = if (symbolPaths.any { it.startsWith(prefix = prefix + key.id.name + ".") })
            getSymbols(prefix = prefix + key.id.name + ".", type = key.type, symbolPaths = symbolPaths, level = level + 1, keyDependencies = keyDependencies)
          else JsonObject()
          symbols.add(key.id.name, JsonObject().apply {
            addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
            if (subSymbols.size() != 0)
              add("values", subSymbols)
          })
          keyDependencies.add(key.apply { isVariableDependency = true })
        } else {
          if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.type.id.superTypeName)
              || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
            val subSymbols: JsonObject = getSymbols(prefix = prefix + key.id.name + ".", type = key.type, symbolPaths = symbolPaths, level = level + 1, keyDependencies = keyDependencies)
            if (subSymbols.size() != 0) {
              symbols.add(key.id.name, JsonObject().apply {
                add("values", subSymbols)
              })
            }
          } else {
            val subSymbols: JsonObject = if (symbolPaths.any { it.startsWith(prefix = prefix + key.id.name + ".") })
              getSymbols(prefix = prefix + key.id.name + ".", type = key.type, symbolPaths = symbolPaths, level = level + 1, keyDependencies = keyDependencies)
            else JsonObject()
            symbols.add(key.id.name + "::context", JsonObject().apply {
              addProperty(KeyConstants.KEY_TYPE, TypeConstants.NUMBER)
            })
            symbols.add(key.id.name, JsonObject().apply {
              addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
              if (subSymbols.size() != 0)
                add("values", subSymbols)
            })
            keyDependencies.add(key.apply { isVariableDependency = true })
          }
        }
      }
    }
  }
  return symbols
}
