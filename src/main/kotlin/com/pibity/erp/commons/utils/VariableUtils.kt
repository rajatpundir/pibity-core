/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.KeyConstants
import com.pibity.erp.commons.constants.PermissionConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.Value
import com.pibity.erp.entities.Variable
import com.pibity.erp.entities.permission.TypePermission
import java.sql.Timestamp
import java.text.SimpleDateFormat

val dateFormat = SimpleDateFormat("yyyy-MM-dd")

data class Quadruple<T1, T2, T3, T4>(val t1: T1, val t2: T2, val t3: T3, val t4: T4)

fun validateMutatedVariables(jsonParams: JsonArray): JsonArray {
  val expectedParams = JsonArray()
  for (variableJson in jsonParams) {
    val expectedVariable = JsonObject()
    if (!variableJson.isJsonObject)
      throw CustomJsonException("{error: 'Unexpected value for parameter'}")
    else {
      if (!variableJson.asJsonObject.has("op"))
        throw CustomJsonException("{op: 'Field is missing in request body'}")
      else {
        try {
          expectedVariable.addProperty("op", variableJson.asJsonObject.get("op").asString)
        } catch (exception: Exception) {
          throw CustomJsonException("{op: 'Unexpected value for parameter'}")
        }
        if (!listOf("create", "update", "delete").contains(variableJson.asJsonObject.get("op").asString))
          throw CustomJsonException("{op: 'Unexpected value for parameter'}")
      }
      if (!variableJson.asJsonObject.has("typeName"))
        throw CustomJsonException("{typeName: 'Field is missing in request body'}")
      else try {
        expectedVariable.addProperty("typeName", variableJson.asJsonObject.get("typeName").asString)
      } catch (exception: Exception) {
        throw CustomJsonException("{typeName: 'Unexpected value for parameter'}")
      }
      if (!variableJson.asJsonObject.has("variableName"))
        throw CustomJsonException("{variableName: 'Field is missing in request body'}")
      else try {
        expectedVariable.addProperty("variableName", variableJson.asJsonObject.get("variableName").asString)
      } catch (exception: Exception) {
        throw CustomJsonException("{variableName: 'Unexpected value for parameter'}")
      }
      if (variableJson.asJsonObject.get("op").asString == "update") {
        if (variableJson.asJsonObject.has("updatedVariableName")) {
          try {
            expectedVariable.addProperty("updatedVariableName?", variableJson.asJsonObject.get("updatedVariableName").asString)
          } catch (exception: Exception) {
            throw CustomJsonException("{updatedVariableName: 'Unexpected value for parameter'}")
          }
        }
      }
      if (variableJson.asJsonObject.get("op").asString != "delete") {
        if (!variableJson.asJsonObject.has("values"))
          throw CustomJsonException("{values, values: 'Field is missing in request body'}")
        else try {
          expectedVariable.add("values", variableJson.asJsonObject.get("values").asJsonObject)
        } catch (exception: Exception) {
          throw CustomJsonException("{values: 'Unexpected value for parameter'}")
        }
      }
    }
    expectedParams.add(expectedVariable)
  }
  return expectedParams
}

fun validateVariableValues(values: JsonObject, typePermission: TypePermission): JsonObject {
  val expectedValues = JsonObject()
  for (keyPermission in typePermission.keyPermissions) {
    val key = keyPermission.key
    if (!values.has(key.name)) {
      // If value is not provided, try to inject default value for they key
      when (key.type.name) {
        TypeConstants.TEXT -> expectedValues.addProperty(key.name, key.defaultStringValue
            ?: throw CustomJsonException("{${key.name}: 'Key value is not provided'}"))
        TypeConstants.NUMBER -> expectedValues.addProperty(key.name, key.defaultLongValue
            ?: throw CustomJsonException("{${key.name}: 'Key value is not provided'}"))
        TypeConstants.DECIMAL -> expectedValues.addProperty(key.name, key.defaultDecimalValue
            ?: throw CustomJsonException("{${key.name}: 'Key value is not provided'}"))
        TypeConstants.BOOLEAN -> expectedValues.addProperty(key.name, key.defaultBooleanValue
            ?: throw CustomJsonException("{${key.name}: 'Key value is not provided'}"))
        TypeConstants.DATE -> expectedValues.addProperty(key.name, key.defaultDateValue?.toString()
            ?: throw CustomJsonException("{${key.name}: 'Key value is not provided'}"))
        TypeConstants.TIMESTAMP -> expectedValues.addProperty(key.name, key.defaultTimestampValue?.time
            ?: throw CustomJsonException("{${key.name}: 'Key value is not provided'}"))
        TypeConstants.TIME -> expectedValues.addProperty(key.name, key.defaultTimeValue?.time
            ?: throw CustomJsonException("{${key.name}: 'Key value is not provided'}"))
        TypeConstants.BLOB -> throw CustomJsonException("{${key.name}: 'Key value is not provided'}")
              TypeConstants.FORMULA -> {
        }
        else -> {
          if (key.referencedVariable == null)
            throw CustomJsonException("{${key.name}: 'Key value is not provided'}")
          else
            expectedValues.addProperty(key.name, key.referencedVariable!!.name)
        }
      }
    } else {
      if (key.type.name in listOf(TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.DATE, TypeConstants.TIMESTAMP, TypeConstants.TIME) && keyPermission.accessLevel != PermissionConstants.WRITE_ACCESS) {
        // Default value is used as Write permission is not present
        when (key.type.name) {
          TypeConstants.TEXT -> expectedValues.addProperty(key.name, key.defaultStringValue
              ?: throw CustomJsonException("{${key.name}: 'Key default value is not defined'}"))
          TypeConstants.NUMBER -> expectedValues.addProperty(key.name, key.defaultLongValue
              ?: throw CustomJsonException("{${key.name}: 'Key default value is not defined'}"))
          TypeConstants.DECIMAL -> expectedValues.addProperty(key.name, key.defaultDecimalValue
              ?: throw CustomJsonException("{${key.name}: 'Key default value is not defined'}"))
          TypeConstants.BOOLEAN -> expectedValues.addProperty(key.name, key.defaultBooleanValue
              ?: throw CustomJsonException("{${key.name}: 'Key default value is not defined'}"))
          TypeConstants.DATE -> expectedValues.addProperty(key.name, key.defaultDateValue?.toString()
              ?: throw CustomJsonException("{${key.name}: 'Key value is not provided'}"))
          TypeConstants.TIMESTAMP -> expectedValues.addProperty(key.name, key.defaultTimestampValue?.time
              ?: throw CustomJsonException("{${key.name}: 'Key value is not provided'}"))
          TypeConstants.TIME -> expectedValues.addProperty(key.name, key.defaultTimeValue?.time
              ?: throw CustomJsonException("{${key.name}: 'Key value is not provided'}"))
          TypeConstants.BLOB -> throw CustomJsonException("{${key.name}: 'Key value is not provided'}")
        }
      } else {
        if (values.get(key.name).isJsonObject) {
          when (key.type.name) {
            TypeConstants.TEXT,
            TypeConstants.NUMBER,
            TypeConstants.DECIMAL,
            TypeConstants.BOOLEAN,
            TypeConstants.DATE,
            TypeConstants.TIMESTAMP,
            TypeConstants.TIME,
            TypeConstants.BLOB,
            TypeConstants.FORMULA -> {
            }
            else ->
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
        } else {
          when (key.type.name) {
            TypeConstants.TEXT -> try {
              expectedValues.addProperty(key.name, values.get(key.name).asString)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
            TypeConstants.NUMBER -> try {
              expectedValues.addProperty(key.name, values.get(key.name).asLong)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
            TypeConstants.DECIMAL -> try {
              expectedValues.addProperty(key.name, values.get(key.name).asBigDecimal)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
            TypeConstants.BOOLEAN -> try {
              expectedValues.addProperty(key.name, values.get(key.name).asBoolean)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
            TypeConstants.DATE -> try {
              expectedValues.addProperty(key.name, java.sql.Date(dateFormat.parse(values.get(key.name).asString).time).toString())
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
            TypeConstants.TIMESTAMP -> try {
              expectedValues.addProperty(key.name, Timestamp(values.get(key.name).asLong).time)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
            TypeConstants.TIME -> try {
              expectedValues.addProperty(key.name, java.sql.Time(values.get(key.name).asLong).time)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
            TypeConstants.BLOB -> try {
              expectedValues.addProperty(key.name, values.get(key.name).asByte)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
            TypeConstants.FORMULA -> {
            }
            else -> {
              try {
                expectedValues.addProperty(key.name, values.get(key.name).asString)
              } catch (exception: Exception) {
                throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
              }
            }
          }
        }
      }
    }
  }
  return expectedValues
}

fun validateUpdatedVariableValues(values: JsonObject, typePermission: TypePermission): JsonObject {
  val expectedValues = JsonObject()
  for (keyPermission in typePermission.keyPermissions) {
    val key = keyPermission.key
    if (values.has(key.name)) {
      when (key.type.name) {
        TypeConstants.TEXT -> {
          if (keyPermission.accessLevel == PermissionConstants.WRITE_ACCESS) {
            try {
              expectedValues.addProperty(key.name, values.get(key.name).asString)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
          }
        }
        TypeConstants.NUMBER -> {
          if (keyPermission.accessLevel == PermissionConstants.WRITE_ACCESS) {
            try {
              expectedValues.addProperty(key.name, values.get(key.name).asLong)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
          }
        }
        TypeConstants.DECIMAL -> {
          if (keyPermission.accessLevel == PermissionConstants.WRITE_ACCESS) {
            try {
              expectedValues.addProperty(key.name, values.get(key.name).asBigDecimal)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
          }
        }
        TypeConstants.BOOLEAN -> {
          if (keyPermission.accessLevel == PermissionConstants.WRITE_ACCESS) {
            try {
              expectedValues.addProperty(key.name, values.get(key.name).asBoolean)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
          }
        }
        TypeConstants.DATE -> {
          if (keyPermission.accessLevel == PermissionConstants.WRITE_ACCESS) {
            try {
              expectedValues.addProperty(key.name, values.get(key.name).asString)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
          }
        }
        TypeConstants.TIME -> {
          if (keyPermission.accessLevel == PermissionConstants.WRITE_ACCESS) {
            try {
              expectedValues.addProperty(key.name, values.get(key.name).asLong)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
          }
        }
        TypeConstants.TIMESTAMP -> {
          if (keyPermission.accessLevel == PermissionConstants.WRITE_ACCESS) {
            try {
              expectedValues.addProperty(key.name, values.get(key.name).asLong)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
          }
        }
        TypeConstants.BLOB -> {
          if (keyPermission.accessLevel == PermissionConstants.WRITE_ACCESS) {
            try {
              expectedValues.addProperty(key.name, values.get(key.name).asByte)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
          }
        }
        TypeConstants.FORMULA -> {
        }
        else -> {
          if (keyPermission.accessLevel == PermissionConstants.WRITE_ACCESS) {
            try {
              expectedValues.addProperty(key.name, values.get(key.name).asString)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
          }
        }
      }
    }
  }
  return expectedValues
}

fun getSymbolValues(variable: Variable, symbolPaths: MutableSet<String>, prefix: String = "", level: Int = 0, symbolsForFormula: Boolean = true): JsonObject {
  val symbols = JsonObject()
  for (value in variable.values) {
    when (value.key.type.name) {
      TypeConstants.TEXT -> {
        if (symbolPaths.contains(prefix + value.key.name)) {
          symbols.add(value.key.name, JsonObject().apply {
            addProperty("type", value.key.type.name)
            addProperty("value", value.stringValue!!)
          })
          symbolPaths.remove(prefix + value.key.name)
        }
      }
      TypeConstants.NUMBER -> {
        if (symbolPaths.contains(prefix + value.key.name)) {
          symbols.add(value.key.name, JsonObject().apply {
            addProperty("type", value.key.type.name)
            addProperty("value", value.longValue!!)
          })
          symbolPaths.remove(prefix + value.key.name)
        }
      }
      TypeConstants.DECIMAL -> {
        if (symbolPaths.contains(prefix + value.key.name)) {
          symbols.add(value.key.name, JsonObject().apply {
            addProperty("type", value.key.type.name)
            addProperty("value", value.decimalValue!!)
          })
          symbolPaths.remove(prefix + value.key.name)
        }
      }
      TypeConstants.BOOLEAN -> {
        if (symbolPaths.contains(prefix + value.key.name)) {
          symbols.add(value.key.name, JsonObject().apply {
            addProperty("type", value.key.type.name)
            addProperty("value", value.booleanValue!!)
          })
          symbolPaths.remove(prefix + value.key.name)
        }
      }
      TypeConstants.DATE -> {
        if (symbolPaths.contains(prefix + value.key.name)) {
          symbols.add(value.key.name, JsonObject().apply {
            addProperty("type", value.key.type.name)
            addProperty("value", value.dateValue.toString())
          })
          symbolPaths.remove(prefix + value.key.name)
        }
      }
      TypeConstants.TIME -> {
        if (symbolPaths.contains(prefix + value.key.name)) {
          symbols.add(value.key.name, JsonObject().apply {
            addProperty("type", value.key.type.name)
            addProperty("value", value.timeValue.toString().toLong())
          })
          symbolPaths.remove(prefix + value.key.name)
        }
      }
      TypeConstants.TIMESTAMP -> {
        if (symbolPaths.contains(prefix + value.key.name)) {
          symbols.add(value.key.name, JsonObject().apply {
            addProperty("type", value.key.type.name)
            addProperty("value", value.timestampValue.toString().toLong())
          })
          symbolPaths.remove(prefix + value.key.name)
        }
      }
     TypeConstants.BLOB -> {
      }
      TypeConstants.FORMULA -> {
        if ((!symbolsForFormula || level != 0) && symbolPaths.contains(prefix + value.key.name)) {
          symbols.add(value.key.name, JsonObject().apply { addProperty("type", value.key.formula!!.returnType.name) })
          when (value.key.formula!!.returnType.name) {
            TypeConstants.TEXT -> symbols.add(value.key.name, JsonObject().apply {
              addProperty("type", value.key.formula!!.returnType.name)
              addProperty("value", value.stringValue!!)
            })
            TypeConstants.NUMBER -> symbols.add(value.key.name, JsonObject().apply {
              addProperty("type", value.key.formula!!.returnType.name)
              addProperty("value", value.longValue!!)
            })
            TypeConstants.DECIMAL -> symbols.add(value.key.name, JsonObject().apply {
              addProperty("type", value.key.formula!!.returnType.name)
              addProperty("value", value.decimalValue!!)
            })
            TypeConstants.BOOLEAN -> symbols.add(value.key.name, JsonObject().apply {
              addProperty("type", value.key.formula!!.returnType.name)
              addProperty("value", value.booleanValue!!)
            })
            else -> throw CustomJsonException("{${value.key.name}: 'Unable to compute formula value'}")
          }
          symbolPaths.remove(prefix + value.key.name)
        }
      }
      else -> if (symbolPaths.any { it.startsWith(prefix = prefix + value.key.name) }) {
        val subSymbols: JsonObject = if (symbolPaths.any { it.startsWith(prefix = prefix + value.key.name + ".") })
          getSymbolValues(prefix = prefix + value.key.name + ".", variable = value.referencedVariable!!, symbolPaths = symbolPaths, level = level + 1, symbolsForFormula = symbolsForFormula)
        else JsonObject()
        symbols.add(value.key.name, JsonObject().apply {
          addProperty("type", TypeConstants.TEXT)
          addProperty("value", value.referencedVariable!!.name)
          if (subSymbols.size() != 0)
            add("values", subSymbols)
        })
      }
    }
  }
  return symbols
}

fun getSymbolValuesAndUpdateDependencies(variable: Variable, symbolPaths: MutableSet<String>, prefix: String = "", level: Int = 0, valueDependencies: MutableSet<Value>, variableDependencies: MutableSet<Variable>, symbolsForFormula: Boolean = true): JsonObject {
  val symbols = JsonObject()
  for (value in variable.values) {
    if (symbolPaths.any { it.startsWith(prefix = prefix + value.key.name) }) {
      when (value.key.type.name) {
        TypeConstants.TEXT -> {
          symbols.add(value.key.name, JsonObject().apply {
            addProperty("type", value.key.type.name)
            addProperty("value", value.stringValue!!)
          })
          symbolPaths.remove(prefix + value.key.name)
          valueDependencies.add(value)
        }
        TypeConstants.NUMBER -> {
          symbols.add(value.key.name, JsonObject().apply {
            addProperty("type", value.key.type.name)
            addProperty("value", value.longValue!!)
          })
          symbolPaths.remove(prefix + value.key.name)
          valueDependencies.add(value)
        }
        TypeConstants.DECIMAL -> {
          symbols.add(value.key.name, JsonObject().apply {
            addProperty("type", value.key.type.name)
            addProperty("value", value.decimalValue!!)
          })
          symbolPaths.remove(prefix + value.key.name)
          valueDependencies.add(value)
        }
        TypeConstants.BOOLEAN -> {
          symbols.add(value.key.name, JsonObject().apply {
            addProperty("type", value.key.type.name)
            addProperty("value", value.booleanValue!!)
          })
          symbolPaths.remove(prefix + value.key.name)
          valueDependencies.add(value)
        }
        TypeConstants.DATE -> {
          symbols.add(value.key.name, JsonObject().apply {
            addProperty("type", value.key.type.name)
            addProperty("value", value.dateValue.toString())
          })
          symbolPaths.remove(prefix + value.key.name)
          valueDependencies.add(value)
        }
        TypeConstants.TIME -> {
          symbols.add(value.key.name, JsonObject().apply {
            addProperty("type", value.key.type.name)
            addProperty("value", value.timeValue.toString().toLong())
          })
          symbolPaths.remove(prefix + value.key.name)
          valueDependencies.add(value)
        }
        TypeConstants.TIMESTAMP -> {
          symbols.add(value.key.name, JsonObject().apply {
            addProperty("type", value.key.type.name)
            addProperty("value", value.timestampValue.toString().toLong())
          })
          symbolPaths.remove(prefix + value.key.name)
          valueDependencies.add(value)
        }
        TypeConstants.BLOB -> {
        }
        TypeConstants.FORMULA -> if (!symbolsForFormula || level != 0) {
          symbols.add(value.key.name, JsonObject().apply { addProperty("type", value.key.formula!!.returnType.name) })
          when (value.key.formula!!.returnType.name) {
            TypeConstants.TEXT -> symbols.add(value.key.name, JsonObject().apply {
              addProperty("type", value.key.formula!!.returnType.name)
              addProperty("value", value.stringValue!!)
            })
            TypeConstants.NUMBER -> symbols.add(value.key.name, JsonObject().apply {
              addProperty("type", value.key.formula!!.returnType.name)
              addProperty("value", value.longValue!!)
            })
            TypeConstants.DECIMAL -> symbols.add(value.key.name, JsonObject().apply {
              addProperty("type", value.key.formula!!.returnType.name)
              addProperty("value", value.decimalValue!!)
            })
            TypeConstants.BOOLEAN -> symbols.add(value.key.name, JsonObject().apply {
              addProperty("type", value.key.formula!!.returnType.name)
              addProperty("value", value.booleanValue!!)
            })
            else -> throw CustomJsonException("{${value.key.name}: 'Unable to compute formula value'}")
          }
          symbolPaths.remove(prefix + value.key.name)
          valueDependencies.add(value)
        }
        else -> {
          if (symbolPaths.any { it.startsWith(prefix = prefix + value.key.name + ".") }) {
            val subSymbols: JsonObject = getSymbolValuesAndUpdateDependencies(prefix = prefix + value.key.name + ".", variable = value.referencedVariable!!, symbolPaths = symbolPaths, level = level + 1, valueDependencies = valueDependencies, variableDependencies = variableDependencies, symbolsForFormula = symbolsForFormula)
            val valueSymbols = JsonObject().apply {
              if (symbolPaths.contains(prefix + value.key.name)) {
                addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
                addProperty(KeyConstants.VALUE, value.referencedVariable!!.name)
                variableDependencies.add(value.referencedVariable!!)
              }
              if (subSymbols.size() != 0)
                add("values", subSymbols)
            }
            if (valueSymbols.size() != 0) {
              symbols.add(value.key.name, valueSymbols)
              valueDependencies.add(value)
            }
          } else {
            symbols.add(value.key.name, JsonObject().apply {
              addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
              addProperty(KeyConstants.VALUE, value.referencedVariable!!.name)
              variableDependencies.add(value.referencedVariable!!)
              valueDependencies.add(value)
            })
          }
        }
      }
    }
  }
  return symbols
}
