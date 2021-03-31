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
import com.pibity.core.entities.Value
import com.pibity.core.entities.Variable
import com.pibity.core.entities.permission.TypePermission
import org.springframework.web.multipart.MultipartFile
import java.sql.Timestamp
import java.util.*

fun validateVariableValues(values: JsonObject, typePermission: TypePermission, defaultTimestamp: Timestamp, files: List<MultipartFile>): JsonObject {
  return typePermission.keyPermissions.fold(JsonObject()) { acc, keyPermission ->
    acc.apply {
      val key: Key = keyPermission.key
      if (keyPermission.accessLevel < PermissionConstants.WRITE_ACCESS) {
        when (key.type.name) {
          TypeConstants.TEXT -> addProperty(key.name, key.defaultStringValue ?: "")
          TypeConstants.NUMBER -> addProperty(key.name, key.defaultLongValue ?: 0)
          TypeConstants.DECIMAL -> addProperty(key.name, key.defaultDecimalValue ?: (0.0).toBigDecimal())
          TypeConstants.BOOLEAN -> addProperty(key.name, key.defaultBooleanValue ?: false)
          TypeConstants.DATE -> addProperty(key.name, key.defaultDateValue?.time ?: java.sql.Date(defaultTimestamp.time).time)
          TypeConstants.TIMESTAMP -> addProperty(key.name, key.defaultTimestampValue?.time ?: defaultTimestamp.time)
          TypeConstants.TIME -> addProperty(key.name, key.defaultTimeValue?.time ?: java.sql.Time(defaultTimestamp.time).time)
          TypeConstants.BLOB, TypeConstants.FORMULA -> {
          }
          else -> {
            if (key.referencedVariable == null)
              throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
            else
              addProperty(key.name, key.referencedVariable!!.name)
          }
        }
      } else {
        if (!values.has(key.name)) {
          when (key.type.name) {
            TypeConstants.TEXT -> addProperty(key.name, key.defaultStringValue ?: "")
            TypeConstants.NUMBER -> addProperty(key.name, key.defaultLongValue ?: 0)
            TypeConstants.DECIMAL -> addProperty(key.name, key.defaultDecimalValue ?: (0.0).toBigDecimal())
            TypeConstants.BOOLEAN -> addProperty(key.name, key.defaultBooleanValue ?: false)
            TypeConstants.DATE -> addProperty(key.name, key.defaultDateValue?.time ?: java.sql.Date(defaultTimestamp.time).time)
            TypeConstants.TIMESTAMP -> addProperty(key.name, key.defaultTimestampValue?.time ?: defaultTimestamp.time)
            TypeConstants.TIME -> addProperty(key.name, key.defaultTimeValue?.time ?: java.sql.Time(defaultTimestamp.time).time)
            TypeConstants.BLOB, TypeConstants.FORMULA -> {
            }
            else -> {
              if (key.referencedVariable != null)
                addProperty(key.name, key.referencedVariable!!.name)
              else
                throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
            }
          }
        } else {
          try {
            when (key.type.name) {
              TypeConstants.TEXT -> addProperty(key.name, values.get(key.name).asString)
              TypeConstants.NUMBER -> addProperty(key.name, values.get(key.name).asLong)
              TypeConstants.DECIMAL -> addProperty(key.name, values.get(key.name).asBigDecimal)
              TypeConstants.BOOLEAN -> addProperty(key.name, values.get(key.name).asBoolean)
              TypeConstants.DATE -> addProperty(key.name, java.sql.Date(values.get(key.name).asLong).time)
              TypeConstants.TIMESTAMP -> addProperty(key.name, Timestamp(values.get(key.name).asLong).time)
              TypeConstants.TIME -> addProperty(key.name, java.sql.Time(values.get(key.name).asLong).time)
              TypeConstants.BLOB -> {
                val fileIndex: Int = values.get(key.name).asInt
                if (fileIndex < 0 && fileIndex > (files.size - 1))
                  throw CustomJsonException("{}")
                else
                  addProperty(key.name, fileIndex)
              }
              TypeConstants.FORMULA -> {
              }
              else -> addProperty(key.name, values.get(key.name).asString)
            }
          } catch (exception: Exception) {
            throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
          }
        }
      }
    }
  }
}

fun validateUpdatedVariableValues(values: JsonObject, typePermission: TypePermission, defaultTimestamp: Timestamp, files: List<MultipartFile>): JsonObject {
  return typePermission.keyPermissions.fold(JsonObject()) { acc, keyPermission ->
    acc.apply {
      val key: Key = keyPermission.key
      if (keyPermission.accessLevel == PermissionConstants.WRITE_ACCESS) {
        if (!values.has(key.name)) {
          when (key.type.name) {
            TypeConstants.TEXT -> addProperty(key.name, key.defaultStringValue ?: "")
            TypeConstants.NUMBER -> addProperty(key.name, key.defaultLongValue ?: 0)
            TypeConstants.DECIMAL -> addProperty(key.name, key.defaultDecimalValue ?: (0.0).toBigDecimal())
            TypeConstants.BOOLEAN -> addProperty(key.name, key.defaultBooleanValue ?: false)
            TypeConstants.DATE -> addProperty(key.name, key.defaultDateValue?.time ?: java.sql.Date(defaultTimestamp.time).time)
            TypeConstants.TIMESTAMP -> addProperty(key.name, key.defaultTimestampValue?.time ?: defaultTimestamp.time)
            TypeConstants.TIME -> addProperty(key.name, key.defaultTimeValue?.time ?: java.sql.Time(defaultTimestamp.time).time)
            TypeConstants.BLOB, TypeConstants.FORMULA -> {
            }
            else -> {
              if (key.referencedVariable != null)
                addProperty(key.name, key.referencedVariable!!.name)
              else
                throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
            }
          }
        } else {
          try {
            when (key.type.name) {
              TypeConstants.TEXT -> addProperty(key.name, values.get(key.name).asString)
              TypeConstants.NUMBER -> addProperty(key.name, values.get(key.name).asLong)
              TypeConstants.DECIMAL -> addProperty(key.name, values.get(key.name).asBigDecimal)
              TypeConstants.BOOLEAN -> addProperty(key.name, values.get(key.name).asBoolean)
              TypeConstants.DATE -> addProperty(key.name, java.sql.Date(values.get(key.name).asLong).time)
              TypeConstants.TIMESTAMP -> addProperty(key.name, Timestamp(values.get(key.name).asLong).time)
              TypeConstants.TIME -> addProperty(key.name, java.sql.Time(values.get(key.name).asLong).time)
              TypeConstants.BLOB -> {
                val fileIndex: Int = values.get(key.name).asInt
                if (fileIndex < 0 && fileIndex > (files.size - 1))
                  throw CustomJsonException("{}")
                else
                  addProperty(key.name, fileIndex)
              }
              TypeConstants.FORMULA -> {
              }
              else -> addProperty(key.name, values.get(key.name).asString)
            }
          } catch (exception: Exception) {
            throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
          }
        }
      }
    }
  }
}

fun getSymbolValues(variable: Variable, symbolPaths: MutableSet<String>, valueDependencies: MutableSet<Value> = mutableSetOf(), prefix: String = "", level: Int = 0, symbolsForFormula: Boolean): JsonObject {
  return variable.values.fold(JsonObject()) { acc, value ->
    acc.apply {
      if (symbolPaths.any { it.startsWith(prefix = prefix + value.key.name) }) {
        when (value.key.type.name) {
          TypeConstants.TEXT -> add(value.key.name, JsonObject().apply {
            addProperty(SymbolConstants.SYMBOL_TYPE, value.key.type.name)
            addProperty(SymbolConstants.SYMBOL_VALUE, value.stringValue!!)
          })
          TypeConstants.NUMBER -> add(value.key.name, JsonObject().apply {
            addProperty(SymbolConstants.SYMBOL_TYPE, value.key.type.name)
            addProperty(SymbolConstants.SYMBOL_VALUE, value.longValue!!)
          })
          TypeConstants.DECIMAL -> add(value.key.name, JsonObject().apply {
            addProperty(SymbolConstants.SYMBOL_TYPE, value.key.type.name)
            addProperty(SymbolConstants.SYMBOL_VALUE, value.decimalValue!!)
          })
          TypeConstants.BOOLEAN -> add(value.key.name, JsonObject().apply {
            addProperty(SymbolConstants.SYMBOL_TYPE, value.key.type.name)
            addProperty(SymbolConstants.SYMBOL_VALUE, value.booleanValue!!)
          })
          TypeConstants.DATE -> add(value.key.name, JsonObject().apply {
            addProperty(SymbolConstants.SYMBOL_TYPE, value.key.type.name)
            addProperty(SymbolConstants.SYMBOL_VALUE, value.dateValue!!.time)
          })
          TypeConstants.TIMESTAMP -> add(value.key.name, JsonObject().apply {
            addProperty(SymbolConstants.SYMBOL_TYPE, value.key.type.name)
            addProperty(SymbolConstants.SYMBOL_VALUE, value.timestampValue!!.time)
          })
          TypeConstants.TIME -> add(value.key.name, JsonObject().apply {
            addProperty(SymbolConstants.SYMBOL_TYPE, value.key.type.name)
            addProperty(SymbolConstants.SYMBOL_VALUE, value.timeValue!!.time)
          })
          TypeConstants.BLOB -> add(value.key.name, JsonObject().apply {
            addProperty(SymbolConstants.SYMBOL_TYPE, value.key.type.name)
            addProperty(SymbolConstants.SYMBOL_VALUE, Base64.getEncoder().encodeToString(value.blobValue!!.getBytes(0, value.blobValue!!.length().toInt())))
          })
          TypeConstants.FORMULA -> if (!symbolsForFormula || level != 0) {
            when (value.key.formula!!.returnType.name) {
              TypeConstants.TEXT -> add(value.key.name, JsonObject().apply {
                addProperty(SymbolConstants.SYMBOL_TYPE, value.key.formula!!.returnType.name)
                addProperty(SymbolConstants.SYMBOL_VALUE, value.stringValue!!)
              })
              TypeConstants.NUMBER -> add(value.key.name, JsonObject().apply {
                addProperty(SymbolConstants.SYMBOL_TYPE, value.key.formula!!.returnType.name)
                addProperty(SymbolConstants.SYMBOL_VALUE, value.longValue!!)
              })
              TypeConstants.DECIMAL -> add(value.key.name, JsonObject().apply {
                addProperty(SymbolConstants.SYMBOL_TYPE, value.key.formula!!.returnType.name)
                addProperty(SymbolConstants.SYMBOL_VALUE, value.decimalValue!!)
              })
              TypeConstants.BOOLEAN -> add(value.key.name, JsonObject().apply {
                addProperty(SymbolConstants.SYMBOL_TYPE, value.key.formula!!.returnType.name)
                addProperty(SymbolConstants.SYMBOL_VALUE, value.booleanValue!!)
              })
              TypeConstants.DATE -> add(value.key.name, JsonObject().apply {
                addProperty(SymbolConstants.SYMBOL_TYPE, value.key.formula!!.returnType.name)
                addProperty(SymbolConstants.SYMBOL_VALUE, value.dateValue!!.time)
              })
              TypeConstants.TIMESTAMP -> add(value.key.name, JsonObject().apply {
                addProperty(SymbolConstants.SYMBOL_TYPE, value.key.formula!!.returnType.name)
                addProperty(SymbolConstants.SYMBOL_VALUE, value.timestampValue!!.time)
              })
              TypeConstants.TIME -> add(value.key.name, JsonObject().apply {
                addProperty(SymbolConstants.SYMBOL_TYPE, value.key.formula!!.returnType.name)
                addProperty(SymbolConstants.SYMBOL_VALUE, value.timeValue!!.time)
              })
              TypeConstants.BLOB -> add(value.key.name, JsonObject().apply {
                addProperty(SymbolConstants.SYMBOL_TYPE, value.key.formula!!.returnType.name)
                addProperty(SymbolConstants.SYMBOL_VALUE, Base64.getEncoder().encodeToString(value.blobValue!!.getBytes(0, value.blobValue!!.length().toInt())))
              })
              else -> throw CustomJsonException("{}")
            }
          }
          else -> add(value.key.name, JsonObject().apply {
            addProperty(SymbolConstants.SYMBOL_TYPE, TypeConstants.TEXT)
            addProperty(KeyConstants.VALUE, value.referencedVariable!!.name)
            if (symbolPaths.any { it.startsWith(prefix = prefix + value.key.name + ".") })
              add(SymbolConstants.SYMBOL_VALUES,  getSymbolValues(prefix = prefix + value.key.name + ".", level = level + 1,variable = value.referencedVariable!!,
                symbolPaths = symbolPaths, valueDependencies = valueDependencies, symbolsForFormula = symbolsForFormula)
              )
          })
        }
        valueDependencies.add(value)
        symbolPaths.remove(prefix + value.key.name)
      }
    }
  }
}
