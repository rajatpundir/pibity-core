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
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.Key
import com.pibity.erp.entities.Type
import java.sql.Timestamp
import java.util.regex.Pattern

val typeIdentifierPattern: Pattern = Pattern.compile("^[A-Z][a-zA-Z0-9]*$")

val keyIdentifierPattern: Pattern = Pattern.compile("[a-z][a-zA-Z0-9]*(_[a-z][a-zA-Z0-9]*)*$")

fun validateTypeName(typeName: String): String {
  if (!typeIdentifierPattern.matcher(typeName).matches())
    throw CustomJsonException("{typeName: 'Type name $typeName is not a valid identifier'}")
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
      try {
        expectedKey.addProperty(KeyConstants.KEY_TYPE, key.get(KeyConstants.KEY_TYPE).asString)
      } catch (exception: Exception) {
        throw CustomJsonException("{keys: {$keyName: {${KeyConstants.KEY_TYPE}: 'Unexpected value for parameter'}}}")
      }
      // Validate type name for key
      if (!typeIdentifierPattern.matcher(key.get(KeyConstants.KEY_TYPE).asString).matches())
        throw CustomJsonException("{keys: {$keyName: {${KeyConstants.KEY_TYPE}: 'Type name for key is not valid'}}}")
      // validate default value for key
      if (key.has(KeyConstants.DEFAULT)) {
        try {
          when (key.get(KeyConstants.KEY_TYPE).asString) {
            TypeConstants.TEXT -> expectedKey.addProperty(
                KeyConstants.DEFAULT,
                key.get(KeyConstants.DEFAULT).asString
            )
            TypeConstants.NUMBER -> expectedKey.addProperty(
                KeyConstants.DEFAULT,
                key.get(KeyConstants.DEFAULT).asLong
            )
            TypeConstants.DECIMAL -> expectedKey.addProperty(
                KeyConstants.DEFAULT,
                key.get(KeyConstants.DEFAULT).asBigDecimal
            )
            TypeConstants.BOOLEAN -> expectedKey.addProperty(
                KeyConstants.DEFAULT,
                key.get(KeyConstants.DEFAULT).asBoolean
            )
            TypeConstants.DATE -> expectedKey.addProperty(
                KeyConstants.DEFAULT,
                java.sql.Date(dateFormat.parse(key.get(KeyConstants.DEFAULT).asString).time).toString()
            )
            TypeConstants.TIMESTAMP -> expectedKey.addProperty(
                KeyConstants.DEFAULT,
                Timestamp(key.get(KeyConstants.DEFAULT).asLong).time
            )
            TypeConstants.TIME -> expectedKey.addProperty(
                KeyConstants.DEFAULT,
                java.sql.Time(key.get(KeyConstants.DEFAULT).asLong).time
            )
            TypeConstants.FORMULA, TypeConstants.BLOB -> {
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
      if (key.get(KeyConstants.KEY_TYPE).asString == TypeConstants.FORMULA) {
        if (!key.has(KeyConstants.FORMULA_RETURN_TYPE))
          throw CustomJsonException("{keys: {$keyName: {${KeyConstants.FORMULA_RETURN_TYPE}: 'Return Type is not provided'}}}")
        else {
          try {
            expectedKey.addProperty(
                KeyConstants.FORMULA_RETURN_TYPE,
                key.get(KeyConstants.FORMULA_RETURN_TYPE).asString
            )
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
    expectedKeys.add(keyName, expectedKey)
  }
  return expectedKeys
}

fun getSymbols(
    type: Type,
    symbolPaths: MutableSet<String>,
    prefix: String = "",
    level: Int,
    keyDependencies: MutableSet<Key>,
    typeDependencies: MutableSet<Type>,
    symbolsForFormula: Boolean = true
): JsonObject {
  val symbols = JsonObject()
  for (key in type.keys) {
    if (symbolPaths.any { it.startsWith(prefix = prefix + key.name) }) {
      when (key.type.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> {
          symbols.add(key.name, JsonObject().apply { addProperty(KeyConstants.KEY_TYPE, key.type.name) })
          symbolPaths.remove(prefix + key.name)
          keyDependencies.add(key.apply {
            if (symbolsForFormula) isFormulaDependency = true else isAssertionDependency = true
          })
        }
        TypeConstants.FORMULA -> if (level != 0) {
          symbols.add(
              key.name,
              JsonObject().apply { addProperty(KeyConstants.KEY_TYPE, key.formula!!.returnType.name) })
          symbolPaths.remove(prefix + key.name)
          keyDependencies.add(key.apply {
            if (symbolsForFormula) isFormulaDependency = true else isAssertionDependency = true
          })
        }
        else -> {
          if (symbolPaths.any { it.startsWith(prefix = prefix + key.name + ".") }) {
            val subSymbols: JsonObject = getSymbols(
                prefix = prefix + key.name + ".",
                type = key.type,
                symbolPaths = symbolPaths,
                level = level + 1,
                keyDependencies = keyDependencies,
                typeDependencies = typeDependencies,
                symbolsForFormula = symbolsForFormula
            )
            val keySymbols = JsonObject().apply {
              if (symbolPaths.contains(prefix + key.name)) {
                addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
                typeDependencies.add(key.type.apply {
                  if (symbolsForFormula) isFormulaDependency = true else isAssertionDependency = true
                })
              }
              if (subSymbols.size() != 0)
                add("values", subSymbols)
            }
            if (keySymbols.size() != 0) {
              symbols.add(key.name, keySymbols)
              keyDependencies.add(key.apply { isVariableDependency = true })
            }
          } else {
            symbols.add(key.name, JsonObject().apply {
              addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
            })
            typeDependencies.add(key.type.apply {
              if (symbolsForFormula) isFormulaDependency = true else isAssertionDependency = true
            })
            keyDependencies.add(key.apply { isVariableDependency = true })
          }
        }
      }
    }
  }
  return symbols
}
