/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.Variable

val variableUpdateOperations = listOf("ref", "ref-drop", "create", "create-drop", "update")

fun validateVariableValues(values: JsonObject, type: Type): JsonObject {
  val expectedValues = JsonObject()
  for (key in type.keys) {
    if (!values.has(key.id.name)) {
      // If value is not provided, try to inject default value for they key
      when (key.type.id.name) {
        TypeConstants.TEXT -> expectedValues.addProperty(key.id.name, key.defaultStringValue
            ?: throw CustomJsonException("{${key.id.name}: 'Key value is not provided'}"))
        TypeConstants.NUMBER -> expectedValues.addProperty(key.id.name, key.defaultLongValue
            ?: throw CustomJsonException("{${key.id.name}: 'Key value is not provided'}"))
        TypeConstants.DECIMAL -> expectedValues.addProperty(key.id.name, key.defaultDoubleValue
            ?: throw CustomJsonException("{${key.id.name}: 'Key value is not provided'}"))
        TypeConstants.BOOLEAN -> expectedValues.addProperty(key.id.name, key.defaultBooleanValue
            ?: throw CustomJsonException("{${key.id.name}: 'Key value is not provided'}"))
        TypeConstants.LIST -> expectedValues.add(key.id.name, JsonArray())
        TypeConstants.FORMULA -> {
        }
        else -> expectedValues.addProperty(key.id.name, (key.referencedVariable
            ?: throw CustomJsonException("{${key.id.name}: 'Field is missing in request body'}")).id.superVariableName
        )
      }
    } else {
      if (values.get(key.id.name).isJsonObject) {
        when (key.type.id.name) {
          TypeConstants.TEXT,
          TypeConstants.NUMBER,
          TypeConstants.DECIMAL,
          TypeConstants.BOOLEAN,
          TypeConstants.LIST -> throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
          TypeConstants.FORMULA -> {
          }
          else -> {
            if (!values.get(key.id.name).asJsonObject.has("variableName"))
              throw CustomJsonException("{${key.id.name}: {variableName: 'Field is missing in request body'}}")
            if (!values.get(key.id.name).asJsonObject.has("values"))
              throw CustomJsonException("{${key.id.name}: {values: 'Field is missing in request body'}}")
            val valueJson = JsonObject()
            try {
              valueJson.addProperty("variableName", values.get(key.id.name).asJsonObject.get("variableName").asString)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.id.name}: {variableName: 'Unexpected value for parameter'}}")
            }
            try {
              valueJson.add("values", values.get(key.id.name).asJsonObject.get("values").asJsonObject)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.id.name}: {values: 'Unexpected value for parameter'}}")
            }
            // localVariableName is an optional field that can be used to reference variables inside local list variable fields of some global variable
            if (values.get(key.id.name).asJsonObject.has("localVariableName")) {
              try {
                valueJson.addProperty("localVariableName", values.get(key.id.name).asJsonObject.get("localVariableName").asString)
              } catch (exception: Exception) {
                throw CustomJsonException("{${key.id.name}: {values: 'Unexpected value for parameter'}}")
              }
            }
            expectedValues.add(key.id.name, valueJson)
          }
        }
      } else {
        when (key.type.id.name) {
          TypeConstants.TEXT -> try {
            expectedValues.addProperty(key.id.name, values.get(key.id.name).asString)
          } catch (exception: Exception) {
            throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
          }
          TypeConstants.NUMBER -> try {
            expectedValues.addProperty(key.id.name, values.get(key.id.name).asLong)
          } catch (exception: Exception) {
            throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
          }
          TypeConstants.DECIMAL -> try {
            expectedValues.addProperty(key.id.name, values.get(key.id.name).asDouble)
          } catch (exception: Exception) {
            throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
          }
          TypeConstants.BOOLEAN -> try {
            expectedValues.addProperty(key.id.name, values.get(key.id.name).asBoolean)
          } catch (exception: Exception) {
            throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
          }
          TypeConstants.LIST -> {
            val jsonArray: JsonArray = values.get(key.id.name).asJsonArray
            val expectedArray = JsonArray()
            for (ref in jsonArray) {
              if (ref.isJsonObject) {
                if (!ref.asJsonObject.has("variableName"))
                  throw CustomJsonException("{${key.id.name}: {variableName: 'Field is missing in request body of one of the variables'}}")
                if (!ref.asJsonObject.has("values"))
                  throw CustomJsonException("{${key.id.name}: {values: 'Field is missing in request body of one of the variables'}}")
                val valueJson = JsonObject()
                try {
                  valueJson.addProperty("variableName", ref.asJsonObject.get("variableName").asString)
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.id.name}: {variableName: 'Unexpected value for parameter'}}")
                }
                try {
                  valueJson.add("values", ref.asJsonObject.get("values").asJsonObject)
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.id.name}: {values: 'Unexpected value for parameter'}}")
                }
                expectedArray.add(valueJson)
              } else
                expectedArray.add(ref.asString)
            }
            expectedValues.add(key.id.name, expectedArray)
          }
          TypeConstants.FORMULA -> {
          }
          else -> expectedValues.addProperty(key.id.name, values.get(key.id.name).asString)
        }
      }
    }
  }
  return expectedValues
}

fun validateUpdatedVariableValues(values: JsonObject, type: Type): JsonObject {
  val expectedValues = JsonObject()
  for (key in type.keys) {
    if (values.has(key.id.name)) {
      when (key.type.id.name) {
        TypeConstants.TEXT -> {
          try {
            expectedValues.addProperty(key.id.name, values.get(key.id.name).asString)
          } catch (exception: Exception) {
            throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
          }
        }
        TypeConstants.NUMBER -> {
          try {
            expectedValues.addProperty(key.id.name, values.get(key.id.name).asLong)
          } catch (exception: Exception) {
            throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
          }
        }
        TypeConstants.DECIMAL -> {
          try {
            expectedValues.addProperty(key.id.name, values.get(key.id.name).asDouble)
          } catch (exception: Exception) {
            throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
          }
        }
        TypeConstants.BOOLEAN -> {
          try {
            expectedValues.addProperty(key.id.name, values.get(key.id.name).asBoolean)
          } catch (exception: Exception) {
            throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
          }
        }
        TypeConstants.LIST -> {
          val listParams = JsonObject()
          if (values.get(key.id.name).asJsonObject.has("add")) {
            if (!values.get(key.id.name).asJsonObject.get("add").isJsonArray)
              throw CustomJsonException("{${key.id.name}: {add: 'Unexpected value for parameter'}}")
            val listInsertionParams = JsonArray()
            values.get(key.id.name).asJsonObject.get("add").asJsonArray.toSet().forEach {
              try {
                if (it.isJsonObject) {
                  if (!it.asJsonObject.has("variableName"))
                    throw CustomJsonException("{${key.id.name}: {variableName: 'Field is missing in request body of one of the variables'}}")
                  else {
                    try {
                      it.asJsonObject.get("variableName").asString
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.id.name}: {variableName: 'Unexpected value for parameter'}}")
                    }
                  }
                  if (!it.asJsonObject.has("values"))
                    throw CustomJsonException("{${key.id.name}: {values: 'Field is missing in request body of one of the variables'}}")
                  else if (!it.asJsonObject.get("values").isJsonObject)
                    throw CustomJsonException("{${key.id.name}: {values: 'Unexpected value for parameter'}}")
                  listInsertionParams.add(it.asJsonObject)
                } else
                  listInsertionParams.add(it.asString)
              } catch (exception: Exception) {
                throw CustomJsonException("{${key.id.name}: {add: 'Unexpected value for parameter'}}")
              }
            }
            listParams.add("add", listInsertionParams)
          }
          if (values.get(key.id.name).asJsonObject.has("remove")) {
            if (!values.get(key.id.name).asJsonObject.get("remove").isJsonArray)
              throw CustomJsonException("{${key.id.name}: {remove: 'Unexpected value for parameter'}}")
            val listDeletionParams = JsonArray()
            values.get(key.id.name).asJsonObject.get("remove").asJsonArray.toSet().forEach {
              try {
                listDeletionParams.add(it.asString)
              } catch (exception: Exception) {
                throw CustomJsonException("{${key.id.name}: {remove: 'Unexpected value for parameter'}}")
              }
            }
            listParams.add("remove", listDeletionParams)
          }
          expectedValues.add(key.id.name, listParams)
        }
        TypeConstants.FORMULA -> {
        }
        else -> {
          if (key.type.id.superTypeName == "Any") {
            if (values.get(key.id.name).isJsonObject) {
              expectedValues.add(key.id.name, JsonObject().apply {
                if (!values.get(key.id.name).asJsonObject.has("variableName"))
                  throw CustomJsonException("{${key.id.name}: {variableName: 'Field is missing in request body'}}")
                else {
                  try {
                    addProperty("variableName", values.get(key.id.name).asJsonObject.get("variableName").asString)
                  } catch (exception: Exception) {
                    throw CustomJsonException("{${key.id.name}: {variableName: 'Unexpected value for parameter'}}")
                  }
                }
                if (!values.get(key.id.name).asJsonObject.has("values"))
                  throw CustomJsonException("{${key.id.name}: {values: 'Field is missing in request body'}}")
                else {
                  try {
                    add("values", values.get(key.id.name).asJsonObject.get("values").asJsonObject)
                  } catch (exception: Exception) {
                    throw CustomJsonException("{${key.id.name}: {values: 'Unexpected value for parameter'}}")
                  }
                }
              })
            } else {
              try {
                expectedValues.addProperty(key.id.name, values.get(key.id.name).asString)
              } catch (exception: Exception) {
                throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
              }
            }
          } else {
            if ((key.id.parentType.id.superTypeName == "Any" && key.id.parentType.id.name == key.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != "Any" && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
              if (values.get(key.id.name).isJsonObject) {
                expectedValues.add(key.id.name, JsonObject().apply {
                  if (!values.get(key.id.name).asJsonObject.has("values"))
                    throw CustomJsonException("{${key.id.name}: {values: 'Field is missing in request body'}}")
                  else {
                    try {
                      add(key.id.name, validateUpdatedVariableValues(values.get(key.id.name).asJsonObject, key.type))
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.id.name}: {values: 'Unexpected value for parameter'}}")
                    }
                  }
                })
              } else
                throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
            }
          }
        }
      }
    }
  }
  return expectedValues
}

fun getLeafNameTypeValues(prefix: String?, keys: MutableMap<String, Map<String, String>>, variable: Variable, depth: Int): Map<String, Map<String, String>> {
  for (value in variable.values) {
    val keyName: String = if (prefix != null) prefix + "_" + value.id.key.id.name else value.id.key.id.name
    when (value.id.key.type.id.name) {
      TypeConstants.TEXT -> keys[keyName] = mapOf("type" to value.id.key.type.id.name, "value" to value.stringValue.toString())
      TypeConstants.NUMBER -> keys[keyName] = mapOf("type" to value.id.key.type.id.name, "value" to value.longValue.toString())
      TypeConstants.DECIMAL -> keys[keyName] = mapOf("type" to value.id.key.type.id.name, "value" to value.doubleValue.toString())
      TypeConstants.BOOLEAN -> keys[keyName] = mapOf("type" to value.id.key.type.id.name, "value" to value.booleanValue.toString())
      TypeConstants.LIST -> {
      }
      TypeConstants.FORMULA -> {
        // Formulas at base level cannot be used inside other formulas at base level.
        if (depth != 0) {
          when (value.id.key.formula!!.returnType.id.name) {
            TypeConstants.TEXT -> keys[keyName] = mapOf("type" to value.id.key.type.id.name, "value" to value.stringValue.toString())
            TypeConstants.NUMBER -> keys[keyName] = mapOf("type" to value.id.key.type.id.name, "value" to value.longValue.toString())
            TypeConstants.DECIMAL -> keys[keyName] = mapOf("type" to value.id.key.type.id.name, "value" to value.doubleValue.toString())
            TypeConstants.BOOLEAN -> keys[keyName] = mapOf("type" to value.id.key.type.id.name, "value" to value.booleanValue.toString())
          }
        }
      }
      else -> if (value.referencedVariable != null) {
        getLeafNameTypeValues(prefix = keyName, keys = keys, variable = value.referencedVariable!!, depth = 1 + depth)
      }
    }
  }
  return keys
}
