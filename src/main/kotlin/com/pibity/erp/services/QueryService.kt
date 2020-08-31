/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.GLOBAL_TYPE
import com.pibity.erp.commons.constants.PermissionConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.TypePermission
import com.pibity.erp.entities.Variable
import com.pibity.erp.repositories.ValueRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QueryService(
    val valueRepository: ValueRepository,
    val userService: UserService
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun queryVariables(jsonParams: JsonObject): List<Variable> {
    val typePermission = userService.superimposeUserPermissions(jsonParams =
    JsonObject().apply {
      addProperty("organization", jsonParams.get("organization").asString)
      addProperty("username", jsonParams.get("username").asString)
      addProperty("typeName", jsonParams.get("typeName").asString)
    })
    val (generatedQuery, _, injectedValues) = try {
      generateQuery(jsonParams.get("query").asJsonObject, username = jsonParams.get("username").asString, typePermission = typePermission)
    } catch (exception: CustomJsonException) {
      throw CustomJsonException("{query : ${exception.message}}")
    }
    return valueRepository.queryVariables(generatedQuery, injectedValues)
  }

  fun generateQuery(queryJson: JsonObject, username: String, typePermission: TypePermission, injectedVariableCount: Int = 0, injectedValues: MutableMap<String, Any> = mutableMapOf(), parentValueAlias: String? = null): Triple<String, Int, MutableMap<String, Any>> {
    val valuesJson: JsonObject = if (!queryJson.has("values"))
      throw CustomJsonException("{values: 'Field is missing in request body'}")
    else {
      try {
        queryJson.get("values").asJsonObject
      } catch (exception: Exception) {
        throw CustomJsonException("{values: 'Unexpected value for parameter'}")
      }
    }
    var variableCount: Int = injectedVariableCount
    val keyQueries = mutableListOf<String>()
    val variableAlias = "v${variableCount++}"
    for (keyPermission in typePermission.keyPermissions) {
      if (keyPermission.accessLevel > PermissionConstants.NO_ACCESS || keyPermission.referencedTypePermission != null) {
        val key = keyPermission.id.key
        if (valuesJson.has(key.id.name) && key.type.id.name != TypeConstants.FORMULA) {
          val valueAlias = "v${variableCount++}"
          var keyQuery = "SELECT $valueAlias FROM Value $valueAlias WHERE ${valueAlias}.id.variable = $variableAlias AND ${valueAlias}.id.key = :v${variableCount}"
          injectedValues["v${variableCount++}"] = key
          when (key.type.id.name) {
            TypeConstants.TEXT -> {
              if (!valuesJson.get(key.id.name).isJsonObject) {
                keyQuery += " AND ${valueAlias}.stringValue = :v${variableCount}"
                injectedValues["v${variableCount++}"] = try {
                  valuesJson.get(key.id.name).asString
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
                }
              } else {
                val keyQueryJson: JsonObject = valuesJson.get(key.id.name).asJsonObject
                when {
                  keyQueryJson.has("equals") -> {
                    keyQuery += " AND ${valueAlias}.stringValue = :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("equals").asString
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.id.name}: {equals: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("like") -> {
                    keyQuery += " AND ${valueAlias}.stringValue LIKE :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("like").asString
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.id.name}: {like: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("between") -> {
                    keyQuery += " AND ${valueAlias}.stringValue BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get("between").isJsonArray)
                      throw CustomJsonException("{${key.id.name}: {between: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("between").asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.id.name}: {between: 'Unexpected value for parameter'}}")
                    keyQueryJson.get("between").asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        it.asString
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.id.name}: {between: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                  keyQueryJson.has("notBetween") -> {
                    keyQuery += " AND ${valueAlias}.stringValue NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get("notBetween").isJsonArray)
                      throw CustomJsonException("{${key.id.name}: {notBetween: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("notBetween").asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.id.name}: {notBetween: 'Unexpected value for parameter'}}")
                    keyQueryJson.get("notBetween").asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        it.asString
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.id.name}: {notBetween: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                  keyQueryJson.has("in") -> {
                    keyQuery += " AND ${valueAlias}.stringValue IN :v${variableCount}"
                    if (!keyQueryJson.get("in").isJsonArray)
                      throw CustomJsonException("{${key.id.name}: {in: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("in").asJsonArray.size() == 0)
                      throw CustomJsonException("{${key.id.name}: {in: 'Unexpected value for parameter'}}")
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("in").asJsonArray.map {
                        it.asString
                      }
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.id.name}: {in: 'Unexpected value for parameter'}}")
                    }
                  }
                }
              }
            }
            TypeConstants.NUMBER -> {
              if (!valuesJson.get(key.id.name).isJsonObject) {
                keyQuery += " AND ${valueAlias}.longValue = :v${variableCount}"
                injectedValues["v${variableCount++}"] = try {
                  valuesJson.get(key.id.name).asLong
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
                }
              } else {
                val keyQueryJson: JsonObject = valuesJson.get(key.id.name).asJsonObject
                when {
                  keyQueryJson.has("equals") -> {
                    keyQuery += " AND ${valueAlias}.longValue = :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("equals").asLong
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("between") -> {
                    keyQuery += " AND ${valueAlias}.longValue BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get("between").isJsonArray)
                      throw CustomJsonException("{${key.id.name}: {between: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("between").asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.id.name}: {between: 'Unexpected value for parameter'}}")
                    keyQueryJson.get("between").asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        it.asLong
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.id.name}: {between: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                  keyQueryJson.has("notBetween") -> {
                    keyQuery += " AND ${valueAlias}.longValue NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get("notBetween").isJsonArray)
                      throw CustomJsonException("{${key.id.name}: {notBetween: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("notBetween").asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.id.name}: {notBetween: 'Unexpected value for parameter'}}")
                    keyQueryJson.get("notBetween").asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        it.asLong
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.id.name}: {notBetween: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                  keyQueryJson.has("in") -> {
                    keyQuery += " AND ${valueAlias}.longValue IN :v${variableCount}"
                    if (!keyQueryJson.get("in").isJsonArray)
                      throw CustomJsonException("{${key.id.name}: {in: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("in").asJsonArray.size() == 0)
                      throw CustomJsonException("{${key.id.name}: {in: 'Unexpected value for parameter'}}")
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("in").asJsonArray.map {
                        it.asLong
                      }
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.id.name}: {in: 'Unexpected value for parameter'}}")
                    }
                  }
                }
              }
            }
            TypeConstants.DECIMAL -> {
              if (!valuesJson.get(key.id.name).isJsonObject) {
                keyQuery += " AND ${valueAlias}.doubleValue = :v${variableCount}"
                injectedValues["v${variableCount++}"] = try {
                  valuesJson.get(key.id.name).asDouble
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
                }
              } else {
                val keyQueryJson: JsonObject = valuesJson.get(key.id.name).asJsonObject
                when {
                  keyQueryJson.has("equals") -> {
                    keyQuery += " AND ${valueAlias}.doubleValue = :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("equals").asDouble
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.id.name}: {equals:'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("between") -> {
                    keyQuery += " AND ${valueAlias}.doubleValue BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get("between").isJsonArray)
                      throw CustomJsonException("{${key.id.name}: {between: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("between").asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.id.name}: {between: 'Unexpected value for parameter'}}")
                    keyQueryJson.get("between").asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        it.asDouble
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.id.name}: {between: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                  keyQueryJson.has("notBetween") -> {
                    keyQuery += " AND ${valueAlias}.doubleValue NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get("notBetween").isJsonArray)
                      throw CustomJsonException("{${key.id.name}: {notBetween: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("notBetween").asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.id.name}: {notBetween: 'Unexpected value for parameter'}}")
                    keyQueryJson.get("notBetween").asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        it.asDouble
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.id.name}: {notBetween: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                  keyQueryJson.has("in") -> {
                    keyQuery += " AND ${valueAlias}.doubleValue IN :v${variableCount}"
                    if (!keyQueryJson.get("in").isJsonArray)
                      throw CustomJsonException("{${key.id.name}: {in: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("in").asJsonArray.size() == 0)
                      throw CustomJsonException("{${key.id.name}: {in: 'Unexpected value for parameter'}}")
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("in").asJsonArray.map {
                        it.asDouble
                      }
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.id.name}: {in: 'Unexpected value for parameter'}}")
                    }
                  }
                }
              }
            }
            TypeConstants.BOOLEAN -> {
              if (!valuesJson.get(key.id.name).isJsonObject) {
                keyQuery += " AND ${valueAlias}.booleanValue = :v${variableCount}"
                injectedValues["v${variableCount++}"] = try {
                  valuesJson.get(key.id.name).asBoolean
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
                }
              } else {
                val keyQueryJson: JsonObject = valuesJson.get(key.id.name).asJsonObject
                if (keyQueryJson.has("equals")) {
                  keyQuery += " AND ${valueAlias}.booleanValue = :v${variableCount}"
                  injectedValues["v${variableCount++}"] = try {
                    keyQueryJson.get("equals").asBoolean
                  } catch (exception: Exception) {
                    throw CustomJsonException("{${key.id.name}: {equals: 'Unexpected value for parameter'}}")
                  }
                }
              }
            }
            TypeConstants.LIST, TypeConstants.FORMULA -> {
            }
            else -> {
              if (key.type.id.superTypeName == GLOBAL_TYPE) {
                if (!valuesJson.get(key.id.name).isJsonObject) {
                  keyQuery += " AND ${valueAlias}.referencedVariable.id.superList = :v${variableCount} AND ${valueAlias}.referencedVariable.id.type = :v${variableCount + 1} AND ${valueAlias}.referencedVariable.id.name = :v${variableCount + 2}"
                  injectedValues["v${variableCount++}"] = typePermission.id.type.id.organization.superList!!
                  injectedValues["v${variableCount++}"] = key.type
                  injectedValues["v${variableCount++}"] = try {
                    valuesJson.get(key.id.name).asString
                  } catch (exception: Exception) {
                    throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
                  }
                } else {
                  val keyQueryJson: JsonObject = valuesJson.get(key.id.name).asJsonObject
                  when {
                    keyQueryJson.has("equals") -> {
                      if (keyQueryJson.get("equals").isJsonObject) {
                        val (generatedQuery, count, _) = try {
                          generateQuery(queryJson = keyQueryJson.get("equals").asJsonObject, injectedVariableCount = variableCount, injectedValues = injectedValues, parentValueAlias = valueAlias, username = username, typePermission = userService.superimposeUserPermissions(jsonParams = JsonObject().apply {
                            addProperty("organization", key.id.parentType.id.organization.id)
                            addProperty("username", username)
                            addProperty("typeName", key.type.id.name)
                          }))
                        } catch (exception: CustomJsonException) {
                          throw CustomJsonException("{${key.id.name}: {equals: ${exception.message}}}")
                        }
                        variableCount = count
                        keyQuery += " AND EXISTS (${generatedQuery})"
                      } else {
                        keyQuery += " AND ${valueAlias}.referencedVariable.id.superList = :v${variableCount} AND ${valueAlias}.referencedVariable.id.type = :v${variableCount + 1} AND ${valueAlias}.referencedVariable.id.name = :v${variableCount + 2}"
                        injectedValues["v${variableCount++}"] = typePermission.id.type.id.organization.superList!!
                        injectedValues["v${variableCount++}"] = key.type
                        injectedValues["v${variableCount++}"] = try {
                          keyQueryJson.get("equals").asString
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.id.name}: {equals: 'Unexpected value for parameter'}}")
                        }
                      }
                    }
                    keyQueryJson.has("like") -> {
                      keyQuery += " AND ${valueAlias}.referencedVariable.id.superList = :v${variableCount} AND ${valueAlias}.referencedVariable.id.type = :v${variableCount + 1} AND ${valueAlias}.referencedVariable.id.name LIKE :v${variableCount + 2}"
                      injectedValues["v${variableCount++}"] = typePermission.id.type.id.organization.superList!!
                      injectedValues["v${variableCount++}"] = key.type
                      injectedValues["v${variableCount++}"] = try {
                        keyQueryJson.get("like").asString
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.id.name}: {like: 'Unexpected value for parameter'}}")
                      }
                    }
                    keyQueryJson.has("between") -> {
                      keyQuery += " AND ${valueAlias}.referencedVariable.id.superList = :v${variableCount} AND ${valueAlias}.referencedVariable.id.type = :v${variableCount + 1} AND ${valueAlias}.referencedVariable.id.name BETWEEN :v${variableCount + 2} AND :v${variableCount + 3}"
                      injectedValues["v${variableCount++}"] = typePermission.id.type.id.organization.superList!!
                      injectedValues["v${variableCount++}"] = key.type
                      if (!keyQueryJson.get("between").isJsonArray)
                        throw CustomJsonException("{${key.id.name}: {between: 'Unexpected value for parameter'}}")
                      else if (keyQueryJson.get("between").asJsonArray.size() != 2)
                        throw CustomJsonException("{${key.id.name}: {between: 'Unexpected value for parameter'}}")
                      keyQueryJson.get("between").asJsonArray.forEach {
                        injectedValues["v${variableCount++}"] = try {
                          it.asString
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.id.name}: {between: 'Unexpected value for parameter'}}")
                        }
                      }
                    }
                    keyQueryJson.has("notBetween") -> {
                      keyQuery += " AND ${valueAlias}.referencedVariable.id.superList = :v${variableCount} AND ${valueAlias}.referencedVariable.id.type = :v${variableCount + 1} AND ${valueAlias}.referencedVariable.id.name NOT BETWEEN :v${variableCount + 2} AND :v${variableCount + 3}"
                      injectedValues["v${variableCount++}"] = typePermission.id.type.id.organization.superList!!
                      injectedValues["v${variableCount++}"] = key.type
                      if (!keyQueryJson.get("notBetween").isJsonArray)
                        throw CustomJsonException("{${key.id.name}: {notBetween: 'Unexpected value for parameter'}}")
                      else if (keyQueryJson.get("notBetween").asJsonArray.size() != 2)
                        throw CustomJsonException("{${key.id.name}: {notBetween: 'Unexpected value for parameter'}}")
                      keyQueryJson.get("notBetween").asJsonArray.forEach {
                        injectedValues["v${variableCount++}"] = try {
                          it.asString
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.id.name}: {notBetween: 'Unexpected value for parameter'}}")
                        }
                      }
                    }
                    keyQueryJson.has("in") -> {
                      keyQuery += " AND ${valueAlias}.referencedVariable.id.superList = :v${variableCount} AND ${valueAlias}.referencedVariable.id.type = :v${variableCount + 1} AND ${valueAlias}.referencedVariable.id.name IN :v${variableCount + 2}"
                      injectedValues["v${variableCount++}"] = typePermission.id.type.id.organization.superList!!
                      injectedValues["v${variableCount++}"] = key.type
                      if (!keyQueryJson.get("in").isJsonArray)
                        throw CustomJsonException("{${key.id.name}: {in: 'Unexpected value for parameter'}}")
                      else if (keyQueryJson.get("in").asJsonArray.size() == 0)
                        throw CustomJsonException("{${key.id.name}: {in: 'Unexpected value for parameter'}}")
                      injectedValues["v${variableCount++}"] = try {
                        keyQueryJson.get("in").asJsonArray.map {
                          it.asString
                        }
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.id.name}: {in: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                }
              } else {
                if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.type.id.superTypeName)
                    || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
                  if (!valuesJson.get(key.id.name).isJsonObject)
                    throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter''}")
                  else {
                    val keyQueryJson: JsonObject = valuesJson.get(key.id.name).asJsonObject
                    val (generatedQuery, count, _) = try {
                      generateQuery(queryJson = keyQueryJson, injectedVariableCount = variableCount, injectedValues = injectedValues, parentValueAlias = valueAlias, username = username, typePermission = keyPermission.referencedTypePermission!!)
                    } catch (exception: CustomJsonException) {
                      throw CustomJsonException("{${key.id.name}: ${exception.message}}")
                    }
                    variableCount = count
                    keyQuery += " AND EXISTS (${generatedQuery})"
                  }
                } else {
                  if (!valuesJson.get(key.id.name).isJsonObject)
                    throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter''}")
                  else {
                    val keyQueryJson: JsonObject = valuesJson.get(key.id.name).asJsonObject
                    val (generatedQuery, count, _) = try {
                      generateQuery(queryJson = keyQueryJson, injectedVariableCount = variableCount, injectedValues = injectedValues, parentValueAlias = valueAlias, username = username, typePermission = userService.superimposeUserPermissions(jsonParams = JsonObject().apply {
                        addProperty("organization", key.id.parentType.id.organization.id)
                        addProperty("username", username)
                        addProperty("suerTypeName", key.type.id.superTypeName)
                        addProperty("typeName", key.type.id.name)
                      }))
                    } catch (exception: CustomJsonException) {
                      throw CustomJsonException("{${key.id.name}: ${exception.message}}")
                    }
                    variableCount = count
                    keyQuery += " AND EXISTS (${generatedQuery})"
                  }
                }
              }
            }
          }
          keyQueries.add("($keyQuery)")
        }
      }
    }
    return if (keyQueries.size != 0) {
      var hql = if (parentValueAlias != null)
        "SELECT DISTINCT $variableAlias FROM Variable $variableAlias WHERE EXISTS " + keyQueries.joinToString(separator = " AND EXISTS ") + " AND ${variableAlias}=${parentValueAlias}.referencedVariable"
      else
        "SELECT DISTINCT $variableAlias FROM Variable $variableAlias WHERE EXISTS " + keyQueries.joinToString(separator = " AND EXISTS ")
      if (queryJson.has("variableName")) {
        if (queryJson.get("variableName").isJsonObject) {
          val variableQueryJson: JsonObject = queryJson.get("variableName").asJsonObject
          when {
            variableQueryJson.has("equals") -> {
              hql += " AND ${variableAlias}.id.name = :v${variableCount}"
              injectedValues["v${variableCount++}"] = try {
                variableQueryJson.get("equals").asString
              } catch (exception: Exception) {
                throw CustomJsonException("{variableName: {equals: 'Unexpected value for parameter'}}")
              }
            }
            variableQueryJson.has("like") -> {
              hql += " AND ${variableAlias}.id.name LIKE :v${variableCount}"
              injectedValues["v${variableCount++}"] = try {
                variableQueryJson.get("like").asString
              } catch (exception: Exception) {
                throw CustomJsonException("{variableName: {like: 'Unexpected value for parameter'}}")
              }
            }
            variableQueryJson.has("between") -> {
              hql += " AND ${variableAlias}.id.name BETWEEN :v${variableCount} AND :v${variableCount + 1}"
              if (!variableQueryJson.get("between").isJsonArray)
                throw CustomJsonException("{variableName: {between: 'Unexpected value for parameter'}}")
              else if (variableQueryJson.get("between").asJsonArray.size() != 2)
                throw CustomJsonException("{variableName: {between: 'Unexpected value for parameter'}}")
              variableQueryJson.get("between").asJsonArray.forEach {
                injectedValues["v${variableCount++}"] = try {
                  it.asString
                } catch (exception: Exception) {
                  throw CustomJsonException("{variableName: {between: 'Unexpected value for parameter'}}")
                }
              }
            }
            variableQueryJson.has("notBetween") -> {
              hql += " AND ${variableAlias}.id.name NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
              if (!variableQueryJson.get("notBetween").isJsonArray)
                throw CustomJsonException("{variableName: {notBetween: 'Unexpected value for parameter'}}")
              else if (variableQueryJson.get("notBetween").asJsonArray.size() != 2)
                throw CustomJsonException("{variableName: {notBetween: 'Unexpected value for parameter'}}")
              variableQueryJson.get("notBetween").asJsonArray.forEach {
                injectedValues["v${variableCount++}"] = try {
                  it.asString
                } catch (exception: Exception) {
                  throw CustomJsonException("{variableName: {notBetween: 'Unexpected value for parameter'}}")
                }
              }
            }
            variableQueryJson.has("in") -> {
              hql += " AND ${variableAlias}.id.name IN :v${variableCount}"
              if (!variableQueryJson.get("in").isJsonArray)
                throw CustomJsonException("{variableName: {in: 'Unexpected value for parameter'}}")
              else if (variableQueryJson.get("in").asJsonArray.size() == 0)
                throw CustomJsonException("{variableName: {in: 'Unexpected value for parameter'}}")
              injectedValues["v${variableCount++}"] = try {
                variableQueryJson.get("in").asJsonArray.map {
                  it.asString
                }
              } catch (exception: Exception) {
                throw CustomJsonException("{variableName: {in: 'Unexpected value for parameter'}}")
              }
            }
          }
        } else {
          hql += " AND ${variableAlias}.id.name = :v${variableCount}"
          injectedValues["v${variableCount++}"] = try {
            queryJson.get("variableName").asString
          } catch (exception: Exception) {
            throw CustomJsonException("{variableName: 'Unexpected value for parameter'}")
          }
        }
      }
      Triple(hql, variableCount, injectedValues)
    } else {
      var hql = if (parentValueAlias != null)
        "SELECT DISTINCT $variableAlias  FROM Variable $variableAlias  WHERE ${variableAlias}.id.type = :v${variableCount} AND ${variableAlias}=${parentValueAlias}.referencedVariable"
      else
        "SELECT DISTINCT $variableAlias  FROM Variable $variableAlias  WHERE ${variableAlias}.id.type = :v${variableCount}"
      injectedValues["v${variableCount++}"] = typePermission.id.type
      if (queryJson.has("variableName")) {
        if (queryJson.get("variableName").isJsonObject) {
          val variableQueryJson: JsonObject = queryJson.get("variableName").asJsonObject
          when {
            variableQueryJson.has("equals") -> {
              hql += " AND ${variableAlias}.id.name = :v${variableCount}"
              injectedValues["v${variableCount++}"] = try {
                variableQueryJson.get("equals").asString
              } catch (exception: Exception) {
                throw CustomJsonException("{variableName: {equals: 'Unexpected value for parameter'}}")
              }
            }
            variableQueryJson.has("like") -> {
              hql += " AND ${variableAlias}.id.name LIKE :v${variableCount}"
              injectedValues["v${variableCount++}"] = try {
                variableQueryJson.get("like").asString
              } catch (exception: Exception) {
                throw CustomJsonException("{variableName: {like: 'Unexpected value for parameter'}}")
              }
            }
            variableQueryJson.has("between") -> {
              hql += " AND ${variableAlias}.id.name BETWEEN :v${variableCount} AND :v${variableCount + 1}"
              if (!variableQueryJson.get("between").isJsonArray)
                throw CustomJsonException("{variableName: {between: 'Unexpected value for parameter'}}")
              else if (variableQueryJson.get("between").asJsonArray.size() != 2)
                throw CustomJsonException("{variableName: {between: 'Unexpected value for parameter'}}")
              variableQueryJson.get("between").asJsonArray.forEach {
                injectedValues["v${variableCount++}"] = try {
                  it.asString
                } catch (exception: Exception) {
                  throw CustomJsonException("{variableName: {between: 'Unexpected value for parameter'}}")
                }
              }
            }
            variableQueryJson.has("notBetween") -> {
              hql += " AND ${variableAlias}.id.name NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
              if (!variableQueryJson.get("notBetween").isJsonArray)
                throw CustomJsonException("{variableName: {notBetween: 'Unexpected value for parameter'}}")
              else if (variableQueryJson.get("notBetween").asJsonArray.size() != 2)
                throw CustomJsonException("{variableName: {notBetween: 'Unexpected value for parameter'}}")
              variableQueryJson.get("notBetween").asJsonArray.forEach {
                injectedValues["v${variableCount++}"] = try {
                  it.asString
                } catch (exception: Exception) {
                  throw CustomJsonException("{variableName: {notBetween: 'Unexpected value for parameter'}}")
                }
              }
            }
            variableQueryJson.has("in") -> {
              hql += " AND ${variableAlias}.id.name IN :v${variableCount}"
              if (!variableQueryJson.get("in").isJsonArray)
                throw CustomJsonException("{variableName: {in: 'Unexpected value for parameter'}}")
              else if (variableQueryJson.get("in").asJsonArray.size() == 0)
                throw CustomJsonException("{variableName: {in: 'Unexpected value for parameter'}}")
              injectedValues["v${variableCount++}"] = try {
                variableQueryJson.get("in").asJsonArray.map {
                  it.asString
                }
              } catch (exception: Exception) {
                throw CustomJsonException("{variableName: {in: 'Unexpected value for parameter'}}")
              }
            }
          }
        } else {
          hql += " AND ${variableAlias}.id.name = :v${variableCount}"
          injectedValues["v${variableCount++}"] = try {
            queryJson.get("variableName").asString
          } catch (exception: Exception) {
            throw CustomJsonException("{variableName: 'Unexpected value for parameter'}")
          }
        }
      }
      Triple(hql, variableCount, injectedValues)
    }
  }
}
