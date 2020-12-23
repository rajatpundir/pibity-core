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
import com.pibity.erp.commons.constants.GLOBAL_TYPE
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
      if (variableJson.asJsonObject.has("context")) {
        try {
          expectedVariable.addProperty("context?", variableJson.asJsonObject.get("context").asLong)
        } catch (exception: Exception) {
          throw CustomJsonException("{context: 'Unexpected value for parameter'}")
        }
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
        TypeConstants.LIST -> expectedValues.add(key.name, JsonArray())
        TypeConstants.FORMULA -> {
        }
        else -> {
          if (key.referencedVariable == null)
            throw CustomJsonException("{${key.name}: 'Key value is not provided'}")
          else {
            if (key.type.superTypeName == GLOBAL_TYPE) {
              expectedValues.addProperty(key.name, key.referencedVariable!!.name)
            } else {
              if ((key.parentType.superTypeName == GLOBAL_TYPE && key.parentType.name == key.type.superTypeName)
                  || (key.parentType.superTypeName != GLOBAL_TYPE && key.parentType.superTypeName == key.type.superTypeName)) {
                throw CustomJsonException("{${key.name}: 'Internal local values cannot have a default'}")
              } else {
                expectedValues.add(key.name, JsonObject().apply {
                  addProperty("context", key.referencedVariable!!.superList.id)
                  addProperty("variableName", key.referencedVariable!!.name)
                })
              }
            }
          }
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
            TypeConstants.LIST -> throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            TypeConstants.FORMULA -> {
            }
            else -> {
              if (key.type.superTypeName == GLOBAL_TYPE)
                throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
              else {
                if ((key.parentType.superTypeName == GLOBAL_TYPE && key.parentType.name == key.type.superTypeName)
                    || (key.parentType.superTypeName != GLOBAL_TYPE && key.parentType.superTypeName == key.type.superTypeName)) {
                  val valueJson = JsonObject()
                  if (values.get(key.name).asJsonObject.has("values")) {
                    try {
                      valueJson.add("values", values.get(key.name).asJsonObject.get("values").asJsonObject)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {values: 'Unexpected value for parameter'}}")
                    }
                  } else throw CustomJsonException("{${key.name}: {values: 'Field is missing in request body'}}")
                  expectedValues.add(key.name, valueJson)
                } else {
                  val valueJson = JsonObject()
                  if (values.get(key.name).asJsonObject.has("context")) {
                    try {
                      valueJson.addProperty("context", values.get(key.name).asJsonObject.get("context").asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {context: 'Unexpected value for parameter'}}")
                    }
                  } else throw CustomJsonException("{${key.name}: {context: 'Field is missing in request body'}}")
                  if (values.get(key.name).asJsonObject.has("variableName")) {
                    try {
                      valueJson.addProperty("variableName", values.get(key.name).asJsonObject.get("variableName").asString)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {variableName: 'Unexpected value for parameter'}}")
                    }
                  } else throw CustomJsonException("{${key.name}: {variableName: 'Field is missing in request body'}}")
                  expectedValues.add(key.name, valueJson)
                }
              }
            }
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
            TypeConstants.LIST -> {
              val jsonArray: JsonArray = try {
                values.get(key.name).asJsonArray
              } catch (exception: Exception) {
                throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
              }
              if (key.list!!.max != 0 && jsonArray.size() > key.list!!.max)
                throw CustomJsonException("{${key.name}: 'List cannot contain more than ${key.list!!.max} variables'}")
              if (jsonArray.size() < key.list!!.min)
                throw CustomJsonException("{${key.name}: 'List cannot contain less than ${key.list!!.min} variables'}")
              val expectedArray = JsonArray()
              for (ref in jsonArray) {
                if (key.list!!.type.superTypeName == GLOBAL_TYPE) {
                  try {
                    expectedArray.add(ref.asString)
                  } catch (exception: Exception) {
                    throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
                  }
                } else {
                  if (ref.isJsonObject) {
                    if ((key.parentType.superTypeName == GLOBAL_TYPE && key.parentType.name == key.list!!.type.superTypeName)
                        || (key.parentType.superTypeName != GLOBAL_TYPE && key.parentType.superTypeName == key.list!!.type.superTypeName)) {
                      val valueJson = JsonObject()
                      if (ref.asJsonObject.has("variableName")) {
                        try {
                          valueJson.addProperty("variableName", ref.asJsonObject.get("variableName").asString)
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.name}: {variableName: 'Unexpected value for parameter'}}")
                        }
                      } else throw CustomJsonException("{${key.name}: {variableName: 'Field is missing in request body of one of the variables'}}")
                      if (ref.asJsonObject.has("values")) {
                        try {
                          valueJson.add("values", ref.asJsonObject.get("values").asJsonObject)
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.name}: {values: 'Unexpected value for parameter'}}")
                        }
                      } else throw CustomJsonException("{${key.name}: {values: 'Field is missing in request body of one of the variables'}}")
                      expectedArray.add(valueJson)
                    } else {
                      val valueJson = JsonObject()
                      if (ref.asJsonObject.has("context")) {
                        try {
                          valueJson.addProperty("context", ref.asJsonObject.get("context").asLong)
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.name}: {context: 'Unexpected value for parameter'}}")
                        }
                      } else throw CustomJsonException("{${key.name}: {context: 'Field is missing in request body'}}")
                      if (ref.asJsonObject.has("variableName")) {
                        try {
                          valueJson.addProperty("variableName", ref.asJsonObject.get("variableName").asString)
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.name}: {variableName: 'Unexpected value for parameter'}}")
                        }
                      } else throw CustomJsonException("{${key.name}: {variableName: 'Field is missing in request body of one of the variables'}}")
                      expectedArray.add(valueJson)
                    }
                  } else throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
                }
              }
              expectedValues.add(key.name, expectedArray)
            }
            TypeConstants.FORMULA -> {
            }
            else -> {
              if (key.type.superTypeName == GLOBAL_TYPE) {
                try {
                  expectedValues.addProperty(key.name, values.get(key.name).asString)
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
                }
              } else throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
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
        TypeConstants.LIST -> {
          if (!values.get(key.name).isJsonObject)
            throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
          else {
            val listParams = JsonObject()
            if (key.list!!.type.superTypeName == GLOBAL_TYPE) {
              if (keyPermission.accessLevel == PermissionConstants.WRITE_ACCESS) {
                if (values.get(key.name).asJsonObject.has("add")) {
                  if (!values.get(key.name).asJsonObject.get("add").isJsonArray)
                    throw CustomJsonException("{${key.name}: {add: 'Unexpected value for parameter'}}")
                  else {
                    val params = JsonArray()
                    values.get(key.name).asJsonObject.get("add").asJsonArray.toSet().forEach {
                      try {
                        params.add(it.asString)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {add: 'Unexpected value for parameter'}}")
                      }
                    }
                    listParams.add("add", params)
                  }
                }
                if (values.get(key.name).asJsonObject.has("remove")) {
                  if (!values.get(key.name).asJsonObject.get("remove").isJsonArray)
                    throw CustomJsonException("{${key.name}: {remove: 'Unexpected value for parameter'}}")
                  else {
                    val params = JsonArray()
                    values.get(key.name).asJsonObject.get("remove").asJsonArray.toSet().forEach {
                      try {
                        params.add(it.asString)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {remove: 'Unexpected value for parameter'}}")
                      }
                    }
                    listParams.add("remove", params)
                  }
                }
                expectedValues.add(key.name, listParams)
              }
            } else {
              if ((key.parentType.superTypeName == GLOBAL_TYPE && key.parentType.name == key.list!!.type.superTypeName)
                  || (key.parentType.superTypeName != GLOBAL_TYPE && key.parentType.superTypeName == key.list!!.type.superTypeName)) {
                if (values.get(key.name).asJsonObject.has("add")) {
                  if (keyPermission.referencedTypePermission!!.creatable) {
                    if (!values.get(key.name).asJsonObject.get("add").isJsonArray)
                      throw CustomJsonException("{${key.name}: {add: 'Unexpected value for parameter'}}")
                    else {
                      val params = JsonArray()
                      values.get(key.name).asJsonObject.get("add").asJsonArray.toSet().forEach {
                        val jsonObject = JsonObject()
                        if (!it.asJsonObject.has("variableName"))
                          throw CustomJsonException("{${key.name}: {add: {variableName: 'Field is missing in request body'}}}")
                        else {
                          try {
                            jsonObject.addProperty("variableName", it.asJsonObject.get("variableName").asString)
                          } catch (exception: Exception) {
                            throw CustomJsonException("{${key.name}: {add: {variableName: 'Unexpected value for parameter'}}}")
                          }
                        }
                        if (!it.asJsonObject.has("values"))
                          throw CustomJsonException("{${key.name}: {add: {values: 'Field is missing in request body'}}}")
                        else {
                          try {
                            jsonObject.add("values", validateVariableValues(it.asJsonObject.get("values").asJsonObject, keyPermission.referencedTypePermission))
                          } catch (exception: CustomJsonException) {
                            throw CustomJsonException("{${key.name}: {add: {values: ${exception.message}}}}")
                          }
                        }
                        params.add(jsonObject)
                      }
                      listParams.add("add", params)
                    }
                  }
                }
                if (values.get(key.name).asJsonObject.has("remove")) {
                  if (keyPermission.referencedTypePermission!!.deletable) {
                    if (!values.get(key.name).asJsonObject.get("remove").isJsonArray)
                      throw CustomJsonException("{${key.name}: {remove: 'Unexpected value for parameter'}}")
                    else {
                      val params = JsonArray()
                      values.get(key.name).asJsonObject.get("remove").asJsonArray.toSet().forEach {
                        try {
                          params.add(it.asString)
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.name}: {remove: 'Unexpected value for parameter'}}")
                        }
                      }
                      listParams.add("remove", params)
                    }
                  }
                }
                if (values.get(key.name).asJsonObject.has("update")) {
                  if (!values.get(key.name).asJsonObject.get("update").isJsonArray)
                    throw CustomJsonException("{${key.name}: {update: 'Unexpected value for parameter'}}")
                  else {
                    val params = JsonArray()
                    values.get(key.name).asJsonObject.get("update").asJsonArray.toSet().forEach {
                      val jsonObject = JsonObject()
                      if (!it.asJsonObject.has("variableName"))
                        throw CustomJsonException("{${key.name}: {update: {variableName: 'Field is missing in request body'}}}")
                      else {
                        try {
                          jsonObject.addProperty("variableName", it.asJsonObject.get("variableName").asString)
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.name}: {update: {variableName: 'Unexpected value for parameter'}}}")
                        }
                      }
                      if (!it.asJsonObject.has("values"))
                        throw CustomJsonException("{${key.name}: {update: {values: 'Field is missing in request body'}}}")
                      else {
                        try {
                          jsonObject.add("values", validateUpdatedVariableValues(it.asJsonObject.get("values").asJsonObject, keyPermission.referencedTypePermission!!))
                        } catch (exception: CustomJsonException) {
                          throw CustomJsonException("{${key.name}: {update: {values: ${exception.message}}}")
                        }
                      }
                      params.add(jsonObject)
                    }
                    listParams.add("update", params)
                  }
                }
                expectedValues.add(key.name, listParams)
              } else {
                if (keyPermission.accessLevel == PermissionConstants.WRITE_ACCESS) {
                  if (values.get(key.name).asJsonObject.has("add")) {
                    if (!values.get(key.name).asJsonObject.get("add").isJsonArray)
                      throw CustomJsonException("{${key.name}: {add: 'Unexpected value for parameter'}}")
                    else {
                      val params = JsonArray()
                      values.get(key.name).asJsonObject.get("add").asJsonArray.toSet().forEach {
                        val jsonObject = JsonObject()
                        if (!it.asJsonObject.has("context"))
                          throw CustomJsonException("{${key.name}: {add: {context: 'Field is missing in request body'}}}")
                        else {
                          try {
                            jsonObject.addProperty("context", it.asJsonObject.get("context").asLong)
                          } catch (exception: Exception) {
                            throw CustomJsonException("{${key.name}: {add: {context: 'Unexpected value for parameter'}}}")
                          }
                        }
                        if (!it.asJsonObject.has("variableName"))
                          throw CustomJsonException("{${key.name}: {add: {variableName: 'Field is missing in request body'}}}")
                        else {
                          try {
                            jsonObject.addProperty("variableName", it.asJsonObject.get("variableName").asString)
                          } catch (exception: Exception) {
                            throw CustomJsonException("{${key.name}: {add: {variableName: 'Unexpected value for parameter'}}}")
                          }
                        }
                        params.add(jsonObject)
                      }
                      listParams.add("add", params)
                    }
                  }
                  if (values.get(key.name).asJsonObject.has("remove")) {
                    if (!values.get(key.name).asJsonObject.get("remove").isJsonArray)
                      throw CustomJsonException("{${key.name}: {remove: 'Unexpected value for parameter'}}")
                    else {
                      val params = JsonArray()
                      values.get(key.name).asJsonObject.get("remove").asJsonArray.toSet().forEach {
                        val jsonObject = JsonObject()
                        if (!it.asJsonObject.has("context"))
                          throw CustomJsonException("{${key.name}: {remove: {context: 'Field is missing in request body'}}}")
                        else {
                          try {
                            jsonObject.addProperty("context", it.asJsonObject.get("context").asLong)
                          } catch (exception: Exception) {
                            throw CustomJsonException("{${key.name}: {remove: {context: 'Unexpected value for parameter'}}}")
                          }
                        }
                        if (!it.asJsonObject.has("variableName"))
                          throw CustomJsonException("{${key.name}: {remove: {variableName: 'Field is missing in request body'}}}")
                        else {
                          try {
                            jsonObject.addProperty("variableName", it.asJsonObject.get("variableName").asString)
                          } catch (exception: Exception) {
                            throw CustomJsonException("{${key.name}: {remove: {variableName: 'Unexpected value for parameter'}}}")
                          }
                        }
                        params.add(jsonObject)
                      }
                      listParams.add("remove", params)
                    }
                  }
                  expectedValues.add(key.name, listParams)
                }
              }
            }
          }
        }
        else -> {
          if (key.type.superTypeName == GLOBAL_TYPE) {
            if (keyPermission.accessLevel == PermissionConstants.WRITE_ACCESS) {
              try {
                expectedValues.addProperty(key.name, values.get(key.name).asString)
              } catch (exception: Exception) {
                throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
              }
            }
          } else {
            if ((key.parentType.superTypeName == GLOBAL_TYPE && key.parentType.name == key.type.superTypeName)
                || (key.parentType.superTypeName != GLOBAL_TYPE && key.parentType.superTypeName == key.type.superTypeName)) {
              if (!values.get(key.name).isJsonObject)
                throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
              else expectedValues.add(key.name, JsonObject().apply {
                if (!values.get(key.name).asJsonObject.has("values"))
                  throw CustomJsonException("{${key.name}: {values: 'Field is missing in request body'}}")
                else {
                  try {
                    add("values", validateUpdatedVariableValues(
                        values = values.get(key.name).asJsonObject.get("values").asJsonObject,
                        typePermission = keyPermission.referencedTypePermission!!))
                  } catch (exception: Exception) {
                    throw CustomJsonException("{${key.name}: {values: ${exception.message}}}")
                  }
                }
              })
            } else {
              if (keyPermission.accessLevel == PermissionConstants.WRITE_ACCESS) {
                if (!values.get(key.name).isJsonObject)
                  throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
                else expectedValues.add(key.name, JsonObject().apply {
                  if (!values.get(key.name).asJsonObject.has("context"))
                    throw CustomJsonException("{${key.name}: {context: 'Field is missing in request body'}}")
                  else {
                    try {
                      addProperty("context", values.get(key.name).asJsonObject.get("context").asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {context: ${exception.message}}}")
                    }
                  }
                  if (!values.get(key.name).asJsonObject.has("variableName"))
                    throw CustomJsonException("{${key.name}: {variableName: 'Field is missing in request body'}}")
                  else {
                    try {
                      addProperty("variableName", values.get(key.name).asJsonObject.get("variableName").asString)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {variableName: 'Unexpected value for parameter'}}")
                    }
                  }
                })
              }
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
      TypeConstants.LIST, TypeConstants.BLOB -> {
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
        if (value.key.type.superTypeName == GLOBAL_TYPE) {
          val subSymbols: JsonObject = if (symbolPaths.any { it.startsWith(prefix = prefix + value.key.name + ".") })
            getSymbolValues(prefix = prefix + value.key.name + ".", variable = value.referencedVariable!!, symbolPaths = symbolPaths, level = level + 1, symbolsForFormula = symbolsForFormula)
          else JsonObject()
          symbols.add(value.key.name, JsonObject().apply {
            addProperty("type", TypeConstants.TEXT)
            addProperty("value", value.referencedVariable!!.name)
            if (subSymbols.size() != 0)
              add("values", subSymbols)
          })
        } else {
          if ((value.key.parentType.superTypeName == GLOBAL_TYPE && value.key.parentType.name == value.key.type.superTypeName)
              || (value.key.parentType.superTypeName != GLOBAL_TYPE && value.key.parentType.superTypeName == value.key.type.superTypeName)) {
            val subSymbols: JsonObject = getSymbolValues(prefix = prefix + value.key.name + ".", variable = value.referencedVariable!!, symbolPaths = symbolPaths, level = level + 1, symbolsForFormula = symbolsForFormula)
            if (subSymbols.size() != 0)
              symbols.add(value.key.name, JsonObject().apply { add("values", subSymbols) })
          } else {
            val subSymbols: JsonObject = if (symbolPaths.any { it.startsWith(prefix = prefix + value.key.name + ".") })
              getSymbolValues(prefix = prefix + value.key.name + ".", variable = value.referencedVariable!!, symbolPaths = symbolPaths, level = level + 1, symbolsForFormula = symbolsForFormula)
            else JsonObject()
            symbols.add(value.key.name + "::context", JsonObject().apply {
              addProperty(KeyConstants.KEY_TYPE, TypeConstants.NUMBER)
              addProperty(KeyConstants.VALUE, value.referencedVariable!!.superList.id)
            })
            symbols.add(value.key.name, JsonObject().apply {
              addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
              addProperty(KeyConstants.VALUE, value.referencedVariable!!.name)
              if (subSymbols.size() != 0)
                add("values", subSymbols)
            })
          }
        }
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
        TypeConstants.LIST, TypeConstants.BLOB -> {
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
          if (value.key.type.superTypeName == GLOBAL_TYPE) {
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
          } else {
            if ((value.key.parentType.superTypeName == GLOBAL_TYPE && value.key.parentType.name == value.key.type.superTypeName)
                || (value.key.parentType.superTypeName != GLOBAL_TYPE && value.key.parentType.superTypeName == value.key.type.superTypeName)) {
              val subSymbols: JsonObject = getSymbolValuesAndUpdateDependencies(prefix = prefix + value.key.name + ".", variable = value.referencedVariable!!, symbolPaths = symbolPaths, level = level + 1, valueDependencies = valueDependencies, variableDependencies = variableDependencies, symbolsForFormula = symbolsForFormula)
              if (subSymbols.size() != 0)
                symbols.add(value.key.name, JsonObject().apply { add("values", subSymbols) })
            } else {
              if (symbolPaths.any { it.startsWith(prefix = prefix + value.key.name + ".") }) {
                val subSymbols: JsonObject = getSymbolValuesAndUpdateDependencies(prefix = prefix + value.key.name + ".", variable = value.referencedVariable!!, symbolPaths = symbolPaths, level = level + 1, valueDependencies = valueDependencies, variableDependencies = variableDependencies, symbolsForFormula = symbolsForFormula)
                val keySymbols = JsonObject().apply {
                  if (subSymbols.size() != 0)
                    add("values", subSymbols)
                }
                if (symbolPaths.contains(prefix + value.key.name)) {
                  keySymbols.addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
                  keySymbols.addProperty(KeyConstants.VALUE, value.referencedVariable!!.name)
                  variableDependencies.add(value.referencedVariable!!)
                }
                if (keySymbols.size() != 0) {
                  symbols.add(value.key.name, keySymbols)
                  valueDependencies.add(value)
                }
                if (symbolPaths.contains(prefix + value.key.name + "::context")) {
                  symbols.add(value.key.name + "::context", JsonObject().apply {
                    addProperty(KeyConstants.KEY_TYPE, TypeConstants.NUMBER)
                    addProperty(KeyConstants.VALUE, value.referencedVariable!!.superList.id)
                  })
                  variableDependencies.add(value.referencedVariable!!)
                  valueDependencies.add(value)
                }
              } else {
                if (symbolPaths.contains(prefix + value.key.name)) {
                  symbols.add(value.key.name, JsonObject().apply {
                    addProperty(KeyConstants.KEY_TYPE, TypeConstants.TEXT)
                    addProperty(KeyConstants.VALUE, value.referencedVariable!!.name)
                  })
                  variableDependencies.add(value.referencedVariable!!)
                  valueDependencies.add(value)
                }
                if (symbolPaths.contains(prefix + value.key.name + "::context")) {
                  symbols.add(value.key.name + "::context", JsonObject().apply {
                    addProperty(KeyConstants.KEY_TYPE, TypeConstants.NUMBER)
                    addProperty(KeyConstants.VALUE, value.referencedVariable!!.superList.id)
                  })
                  variableDependencies.add(value.referencedVariable!!)
                  valueDependencies.add(value)
                }
              }
            }
          }
        }
      }
    }
  }
  return symbols
}
