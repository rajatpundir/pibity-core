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
import com.pibity.erp.commons.constants.GLOBAL_TYPE
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.Variable

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
        else -> {
          if (key.referencedVariable == null)
            throw CustomJsonException("{${key.id.name}: 'Key value is not provided'}")
          else {
            if (key.type.id.superTypeName == GLOBAL_TYPE) {
              expectedValues.addProperty(key.id.name, key.referencedVariable!!.id.name)
            } else {
              if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.type.id.superTypeName)
                  || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
                throw CustomJsonException("{${key.id.name}: 'Internal local values cannot have a default'}")
              } else {
                expectedValues.add(key.id.name, JsonObject().apply {
                  addProperty("context", key.referencedVariable!!.id.superList.id)
                  addProperty("variableName", key.referencedVariable!!.id.name)
                })
              }
            }
          }
        }
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
            if (key.type.id.superTypeName == GLOBAL_TYPE)
              throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
            else {
              if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.type.id.superTypeName)
                  || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
                val valueJson = JsonObject()
                if (values.get(key.id.name).asJsonObject.has("values")) {
                  try {
                    valueJson.add("values", values.get(key.id.name).asJsonObject.get("values").asJsonObject)
                  } catch (exception: Exception) {
                    throw CustomJsonException("{${key.id.name}: {values: 'Unexpected value for parameter'}}")
                  }
                } else throw CustomJsonException("{${key.id.name}: {values: 'Field is missing in request body'}}")
                expectedValues.add(key.id.name, valueJson)
              } else {
                val valueJson = JsonObject()
                if (values.get(key.id.name).asJsonObject.has("context")) {
                  try {
                    valueJson.addProperty("context", values.get(key.id.name).asJsonObject.get("context").asLong)
                  } catch (exception: Exception) {
                    throw CustomJsonException("{${key.id.name}: {context: 'Unexpected value for parameter'}}")
                  }
                } else throw CustomJsonException("{${key.id.name}: {context: 'Field is missing in request body'}}")
                if (values.get(key.id.name).asJsonObject.has("variableName")) {
                  try {
                    valueJson.addProperty("variableName", values.get(key.id.name).asJsonObject.get("variableName").asString)
                  } catch (exception: Exception) {
                    throw CustomJsonException("{${key.id.name}: {variableName: 'Unexpected value for parameter'}}")
                  }
                } else throw CustomJsonException("{${key.id.name}: {variableName: 'Field is missing in request body'}}")
                expectedValues.add(key.id.name, valueJson)
              }
            }
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
            val jsonArray: JsonArray = try {
              values.get(key.id.name).asJsonArray
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
            }
            if (key.list!!.max != 0 && jsonArray.size() > key.list!!.max)
              throw CustomJsonException("{${key.id.name}: 'List cannot contain more than ${key.list!!.max} variables'}")
            if (jsonArray.size() < key.list!!.min)
              throw CustomJsonException("{${key.id.name}: 'List cannot contain less than ${key.list!!.min} variables'}")
            val expectedArray = JsonArray()
            for (ref in jsonArray) {
              if (key.list!!.type.id.superTypeName == GLOBAL_TYPE) {
                try {
                  expectedArray.add(ref.asString)
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
                }
              } else {
                if (ref.isJsonObject) {
                  if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.list!!.type.id.superTypeName)
                      || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.list!!.type.id.superTypeName)) {
                    val valueJson = JsonObject()
                    if (ref.asJsonObject.has("variableName")) {
                      try {
                        valueJson.addProperty("variableName", ref.asJsonObject.get("variableName").asString)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.id.name}: {variableName: 'Unexpected value for parameter'}}")
                      }
                    } else throw CustomJsonException("{${key.id.name}: {variableName: 'Field is missing in request body of one of the variables'}}")
                    if (ref.asJsonObject.has("values")) {
                      try {
                        valueJson.add("values", ref.asJsonObject.get("values").asJsonObject)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.id.name}: {values: 'Unexpected value for parameter'}}")
                      }
                    } else throw CustomJsonException("{${key.id.name}: {values: 'Field is missing in request body of one of the variables'}}")
                    expectedArray.add(valueJson)
                  } else {
                    val valueJson = JsonObject()
                    if (ref.asJsonObject.has("context")) {
                      try {
                        valueJson.addProperty("context", ref.asJsonObject.get("context").asLong)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.id.name}: {context: 'Unexpected value for parameter'}}")
                      }
                    } else throw CustomJsonException("{${key.id.name}: {context: 'Field is missing in request body'}}")
                    if (ref.asJsonObject.has("variableName")) {
                      try {
                        valueJson.addProperty("variableName", ref.asJsonObject.get("variableName").asString)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.id.name}: {variableName: 'Unexpected value for parameter'}}")
                      }
                    } else throw CustomJsonException("{${key.id.name}: {variableName: 'Field is missing in request body of one of the variables'}}")
                    expectedArray.add(valueJson)
                  }
                } else throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
              }
            }
            expectedValues.add(key.id.name, expectedArray)
          }
          TypeConstants.FORMULA -> {
          }
          else -> {
            if (key.type.id.superTypeName == GLOBAL_TYPE) {
              try {
                expectedValues.addProperty(key.id.name, values.get(key.id.name).asString)
              } catch (exception: Exception) {
                throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
              }
            } else throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
          }
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
          if (!values.get(key.id.name).isJsonObject)
            throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
          else {
            val listParams = JsonObject()
            if (key.list!!.type.id.superTypeName == GLOBAL_TYPE) {
              if (values.get(key.id.name).asJsonObject.has("add")) {
                if (!values.get(key.id.name).asJsonObject.get("add").isJsonArray)
                  throw CustomJsonException("{${key.id.name}: {add: 'Unexpected value for parameter'}}")
                else {
                  val params = JsonArray()
                  values.get(key.id.name).asJsonObject.get("add").asJsonArray.toSet().forEach {
                    try {
                      params.add(it.asString)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.id.name}: {add: 'Unexpected value for parameter'}}")
                    }
                  }
                  listParams.add("add", params)
                }
              }
              if (values.get(key.id.name).asJsonObject.has("remove")) {
                if (!values.get(key.id.name).asJsonObject.get("remove").isJsonArray)
                  throw CustomJsonException("{${key.id.name}: {remove: 'Unexpected value for parameter'}}")
                else {
                  val params = JsonArray()
                  values.get(key.id.name).asJsonObject.get("remove").asJsonArray.toSet().forEach {
                    try {
                      params.add(it.asString)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.id.name}: {remove: 'Unexpected value for parameter'}}")
                    }
                  }
                  listParams.add("remove", params)
                }
              }
            } else {
              if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.list!!.type.id.superTypeName)
                  || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.list!!.type.id.superTypeName)) {
                if (values.get(key.id.name).asJsonObject.has("add")) {
                  if (!values.get(key.id.name).asJsonObject.get("add").isJsonArray)
                    throw CustomJsonException("{${key.id.name}: {add: 'Unexpected value for parameter'}}")
                  else {
                    val params = JsonArray()
                    values.get(key.id.name).asJsonObject.get("add").asJsonArray.toSet().forEach {
                      try {
                        val jsonObject = JsonObject()
                        if (!it.asJsonObject.has("variableName"))
                          throw CustomJsonException("{${key.id.name}: {add: {variableName: 'Field is missing in request body'}}}")
                        else {
                          try {
                            jsonObject.addProperty("variableName", it.asJsonObject.get("variableName").asString)
                          } catch (exception: Exception) {
                            throw CustomJsonException("{${key.id.name}: {add: {variableName: 'Unexpected value for parameter'}}}")
                          }
                        }
                        if (!it.asJsonObject.has("values"))
                          throw CustomJsonException("{${key.id.name}: {add: {values: 'Field is missing in request body'}}}")
                        else {
                          try {
                            jsonObject.add("values", validateVariableValues(it.asJsonObject.get("values").asJsonObject, key.list!!.type))
                          } catch (exception: CustomJsonException) {
                            throw CustomJsonException("{${key.id.name}: {add: {values: ${exception.message}}}}")
                          }
                        }
                        params.add(jsonObject)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.id.name}: {add: {values: {'Unexpected value for parameter'}}}")
                      }
                    }
                    listParams.add("add", params)
                  }
                }
                if (values.get(key.id.name).asJsonObject.has("remove")) {
                  if (!values.get(key.id.name).asJsonObject.get("remove").isJsonArray)
                    throw CustomJsonException("{${key.id.name}: {remove: 'Unexpected value for parameter'}}")
                  else {
                    val params = JsonArray()
                    values.get(key.id.name).asJsonObject.get("remove").asJsonArray.toSet().forEach {
                      try {
                        params.add(it.asString)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.id.name}: {remove: 'Unexpected value for parameter'}}")
                      }
                    }
                    listParams.add("remove", params)
                  }
                }
                if (values.get(key.id.name).asJsonObject.has("update")) {
                  if (!values.get(key.id.name).asJsonObject.get("update").isJsonArray)
                    throw CustomJsonException("{${key.id.name}: {update: 'Unexpected value for parameter'}}")
                  else {
                    val params = JsonArray()
                    values.get(key.id.name).asJsonObject.get("update").asJsonArray.toSet().forEach {
                      try {
                        val jsonObject = JsonObject()
                        if (!it.asJsonObject.has("variableName"))
                          throw CustomJsonException("{${key.id.name}: {update: {variableName: 'Field is missing in request body'}}}")
                        else {
                          try {
                            jsonObject.addProperty("variableName", it.asJsonObject.get("variableName").asString)
                          } catch (exception: Exception) {
                            throw CustomJsonException("{${key.id.name}: {update: {variableName: 'Unexpected value for parameter'}}}")
                          }
                        }
                        if (!it.asJsonObject.has("values"))
                          throw CustomJsonException("{${key.id.name}: {update: {values: 'Field is missing in request body'}}}")
                        else {
                          try {
                            jsonObject.add("values", validateUpdatedVariableValues(it.asJsonObject.get("values").asJsonObject, key.list!!.type))
                          } catch (exception: CustomJsonException) {
                            throw CustomJsonException("{${key.id.name}: {update: {values: ${exception.message}}}")
                          }
                        }
                        params.add(jsonObject)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.id.name}: {update: {values: {'Unexpected value for parameter'}}}")
                      }
                    }
                    listParams.add("update", params)
                  }
                }
              } else {
                if (values.get(key.id.name).asJsonObject.has("add")) {
                  if (!values.get(key.id.name).asJsonObject.get("add").isJsonArray)
                    throw CustomJsonException("{${key.id.name}: {add: 'Unexpected value for parameter'}}")
                  else {
                    val params = JsonArray()
                    values.get(key.id.name).asJsonObject.get("add").asJsonArray.toSet().forEach {
                      val jsonObject = JsonObject()
                      if (!it.asJsonObject.has("variableName"))
                        throw CustomJsonException("{${key.id.name}: {add: {variableName: 'Field is missing in request body'}}}")
                      else {
                        try {
                          jsonObject.addProperty("variableName", it.asJsonObject.get("variableName").asString)
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.id.name}: {add: {variableName: 'Unexpected value for parameter'}}}")
                        }
                      }
                      if (!it.asJsonObject.has("context"))
                        throw CustomJsonException("{${key.id.name}: {add: {context: 'Field is missing in request body'}}}")
                      else {
                        try {
                          jsonObject.addProperty("context", it.asJsonObject.get("context").asLong)
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.id.name}: {add: {context: 'Unexpected value for parameter'}}}")
                        }
                      }
                      params.add(jsonObject)
                    }
                    listParams.add("add", params)
                  }
                }
                if (values.get(key.id.name).asJsonObject.has("remove")) {
                  if (!values.get(key.id.name).asJsonObject.get("remove").isJsonArray)
                    throw CustomJsonException("{${key.id.name}: {remove: 'Unexpected value for parameter'}}")
                  else {
                    val params = JsonArray()
                    values.get(key.id.name).asJsonObject.get("remove").asJsonArray.toSet().forEach {
                      val jsonObject = JsonObject()
                      if (!it.asJsonObject.has("variableName"))
                        throw CustomJsonException("{${key.id.name}: {remove: {variableName: 'Field is missing in request body'}}}")
                      else {
                        try {
                          jsonObject.addProperty("variableName", it.asJsonObject.get("variableName").asString)
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.id.name}: {remove: {variableName: 'Unexpected value for parameter'}}}")
                        }
                      }
                      if (!it.asJsonObject.has("context"))
                        throw CustomJsonException("{${key.id.name}: {remove: {context: 'Field is missing in request body'}}}")
                      else {
                        try {
                          jsonObject.addProperty("context", it.asJsonObject.get("context").asLong)
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.id.name}: {remove: {context: 'Unexpected value for parameter'}}}")
                        }
                      }
                      params.add(jsonObject)
                    }
                    listParams.add("remove", params)
                  }
                }
              }
            }
            expectedValues.add(key.id.name, listParams)
          }
        }
        TypeConstants.FORMULA -> {
        }
        else -> {
          if (key.type.id.superTypeName == GLOBAL_TYPE) {
            try {
              expectedValues.addProperty(key.id.name, values.get(key.id.name).asString)
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
            }
          } else {
            if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
              if (!values.get(key.id.name).isJsonObject)
                throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
              else expectedValues.add(key.id.name, JsonObject().apply {
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
                    add("values", validateUpdatedVariableValues(values.get(key.id.name).asJsonObject.get("values").asJsonObject, key.type))
                  } catch (exception: Exception) {
                    throw CustomJsonException("{${key.id.name}: {values: ${exception.message}}}")
                  }
                }
              })
            } else {
              if (!values.get(key.id.name).isJsonObject)
                throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
              else expectedValues.add(key.id.name, JsonObject().apply {
                if (!values.get(key.id.name).asJsonObject.has("variableName"))
                  throw CustomJsonException("{${key.id.name}: {variableName: 'Field is missing in request body'}}")
                else {
                  try {
                    addProperty("variableName", values.get(key.id.name).asJsonObject.get("variableName").asString)
                  } catch (exception: Exception) {
                    throw CustomJsonException("{${key.id.name}: {variableName: 'Unexpected value for parameter'}}")
                  }
                }
                if (!values.get(key.id.name).asJsonObject.has("context"))
                  throw CustomJsonException("{${key.id.name}: {context: 'Field is missing in request body'}}")
                else {
                  try {
                    addProperty("context", values.get(key.id.name).asJsonObject.get("context").asLong)
                  } catch (exception: Exception) {
                    throw CustomJsonException("{${key.id.name}: {context: ${exception.message}}}")
                  }
                }
              })
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
