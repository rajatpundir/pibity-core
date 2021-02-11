/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.PermissionConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.Variable
import com.pibity.erp.entities.permission.TypePermission
import com.pibity.erp.repositories.query.TypePermissionRepository
import com.pibity.erp.repositories.query.ValueRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.text.SimpleDateFormat

@Service
class QueryService(
    val valueRepository: ValueRepository,
    val userService: UserService,
    val typePermissionRepository: TypePermissionRepository,
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun queryVariables(jsonParams: JsonObject): List<Variable> {
    val typePermission: TypePermission = userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
      addProperty("orgId", jsonParams.get("orgId").asString)
      addProperty("username", jsonParams.get("username").asString)
      addProperty("typeName", jsonParams.get("typeName").asString)
    })
    val (generatedQuery, _, injectedValues) = try {
      generateQuery(jsonParams.get("query").asJsonObject, username = jsonParams.get("username").asString, typePermission = typePermission)
    } catch (exception: CustomJsonException) {
      throw CustomJsonException("{query : ${exception.message}}")
    }
    return valueRepository.queryVariables(generatedQuery, injectedValues, limit = jsonParams.get("limit").asInt, offset = jsonParams.get("offset").asInt)
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun queryPublicVariables(jsonParams: JsonObject): List<Variable> {
    val typePermission: TypePermission = typePermissionRepository.findTypePermission(organizationId = jsonParams.get("orgId").asLong, typeName = jsonParams.get("typeName").asString, name = "PUBLIC")
        ?: throw CustomJsonException("{typeName: 'Type cannot be determined'}")
    val (generatedQuery, _, injectedValues) = try {
      generateQuery(jsonParams.get("query").asJsonObject, username = "PUBLIC", typePermission = typePermission)
    } catch (exception: CustomJsonException) {
      throw CustomJsonException("{query : ${exception.message}}")
    }
    return valueRepository.queryVariables(generatedQuery, injectedValues, limit = jsonParams.get("limit").asInt, offset = jsonParams.get("offset").asInt)
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
      if (keyPermission.accessLevel > PermissionConstants.NO_ACCESS) {
        val key = keyPermission.key
        if (valuesJson.has(key.name) && key.type.name != TypeConstants.FORMULA) {
          val valueAlias = "v${variableCount++}"
          var keyQuery = "SELECT $valueAlias FROM Value $valueAlias WHERE ${valueAlias}.variable = $variableAlias AND ${valueAlias}.key = :v${variableCount}"
          injectedValues["v${variableCount++}"] = key
          when (key.type.name) {
            TypeConstants.TEXT -> {
              if (!valuesJson.get(key.name).isJsonObject) {
                keyQuery += " AND ${valueAlias}.stringValue = :v${variableCount}"
                injectedValues["v${variableCount++}"] = try {
                  valuesJson.get(key.name).asString
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
                }
              } else {
                val keyQueryJson: JsonObject = valuesJson.get(key.name).asJsonObject
                when {
                  keyQueryJson.has("equals") -> {
                    keyQuery += " AND ${valueAlias}.stringValue = :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("equals").asString
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {equals: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("like") -> {
                    keyQuery += " AND ${valueAlias}.stringValue LIKE :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("like").asString
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {like: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("between") -> {
                    keyQuery += " AND ${valueAlias}.stringValue BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get("between").isJsonArray)
                      throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("between").asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                    keyQueryJson.get("between").asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        it.asString
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                  keyQueryJson.has("notBetween") -> {
                    keyQuery += " AND ${valueAlias}.stringValue NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get("notBetween").isJsonArray)
                      throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("notBetween").asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                    keyQueryJson.get("notBetween").asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        it.asString
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                  keyQueryJson.has("in") -> {
                    keyQuery += " AND ${valueAlias}.stringValue IN :v${variableCount}"
                    if (!keyQueryJson.get("in").isJsonArray)
                      throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("in").asJsonArray.size() == 0)
                      throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("in").asJsonArray.map {
                        it.asString
                      }
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                    }
                  }
                }
              }
            }
            TypeConstants.NUMBER -> {
              if (!valuesJson.get(key.name).isJsonObject) {
                keyQuery += " AND ${valueAlias}.longValue = :v${variableCount}"
                injectedValues["v${variableCount++}"] = try {
                  valuesJson.get(key.name).asLong
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
                }
              } else {
                val keyQueryJson: JsonObject = valuesJson.get(key.name).asJsonObject
                when {
                  keyQueryJson.has("equals") -> {
                    keyQuery += " AND ${valueAlias}.longValue = :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("equals").asLong
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("greaterThanEquals") -> {
                    keyQuery += " AND ${valueAlias}.longValue >= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("greaterThanEquals").asLong
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("lessThanEquals") -> {
                    keyQuery += " AND ${valueAlias}.longValue <= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("lessThanEquals").asLong
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("greaterThan") -> {
                    keyQuery += " AND ${valueAlias}.longValue > :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("greaterThan").asLong
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("lessThan") -> {
                    keyQuery += " AND ${valueAlias}.longValue < :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("lessThan").asLong
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("between") -> {
                    keyQuery += " AND ${valueAlias}.longValue BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get("between").isJsonArray)
                      throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("between").asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                    keyQueryJson.get("between").asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        it.asLong
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                  keyQueryJson.has("notBetween") -> {
                    keyQuery += " AND ${valueAlias}.longValue NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get("notBetween").isJsonArray)
                      throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("notBetween").asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                    keyQueryJson.get("notBetween").asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        it.asLong
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                  keyQueryJson.has("in") -> {
                    keyQuery += " AND ${valueAlias}.longValue IN :v${variableCount}"
                    if (!keyQueryJson.get("in").isJsonArray)
                      throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("in").asJsonArray.size() == 0)
                      throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("in").asJsonArray.map {
                        it.asLong
                      }
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                    }
                  }
                }
              }
            }
            TypeConstants.DECIMAL -> {
              if (!valuesJson.get(key.name).isJsonObject) {
                keyQuery += " AND ${valueAlias}.decimalValue = :v${variableCount}"
                injectedValues["v${variableCount++}"] = try {
                  valuesJson.get(key.name).asBigDecimal
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
                }
              } else {
                val keyQueryJson: JsonObject = valuesJson.get(key.name).asJsonObject
                when {
                  keyQueryJson.has("equals") -> {
                    keyQuery += " AND ${valueAlias}.decimalValue = :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("equals").asBigDecimal
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {equals:'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("greaterThanEquals") -> {
                    keyQuery += " AND ${valueAlias}.decimalValue >= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("greaterThanEquals").asBigDecimal
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("lessThanEquals") -> {
                    keyQuery += " AND ${valueAlias}.decimalValue <= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("lessThanEquals").asBigDecimal
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("greaterThan") -> {
                    keyQuery += " AND ${valueAlias}.decimalValue > :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("greaterThan").asBigDecimal
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("lessThan") -> {
                    keyQuery += " AND ${valueAlias}.decimalValue < :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("lessThan").asBigDecimal
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("between") -> {
                    keyQuery += " AND ${valueAlias}.decimalValue BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get("between").isJsonArray)
                      throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("between").asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                    keyQueryJson.get("between").asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        it.asBigDecimal
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                  keyQueryJson.has("notBetween") -> {
                    keyQuery += " AND ${valueAlias}.decimalValue NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get("notBetween").isJsonArray)
                      throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("notBetween").asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                    keyQueryJson.get("notBetween").asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        it.asBigDecimal
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                  keyQueryJson.has("in") -> {
                    keyQuery += " AND ${valueAlias}.decimalValue IN :v${variableCount}"
                    if (!keyQueryJson.get("in").isJsonArray)
                      throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("in").asJsonArray.size() == 0)
                      throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("in").asJsonArray.map {
                        it.asBigDecimal
                      }
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                    }
                  }
                }
              }
            }
            TypeConstants.BOOLEAN -> {
              if (!valuesJson.get(key.name).isJsonObject) {
                keyQuery += " AND ${valueAlias}.booleanValue = :v${variableCount}"
                injectedValues["v${variableCount++}"] = try {
                  valuesJson.get(key.name).asBoolean
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
                }
              } else {
                val keyQueryJson: JsonObject = valuesJson.get(key.name).asJsonObject
                if (keyQueryJson.has("equals")) {
                  keyQuery += " AND ${valueAlias}.booleanValue = :v${variableCount}"
                  injectedValues["v${variableCount++}"] = try {
                    keyQueryJson.get("equals").asBoolean
                  } catch (exception: Exception) {
                    throw CustomJsonException("{${key.name}: {equals: 'Unexpected value for parameter'}}")
                  }
                }
              }
            }
            TypeConstants.DATE -> {
              val dateFormat = SimpleDateFormat("yyyy-MM-dd")
              if (!valuesJson.get(key.name).isJsonObject) {
                keyQuery += " AND ${valueAlias}.dateValue = :v${variableCount}"
                injectedValues["v${variableCount++}"] = try {
                  java.sql.Date(dateFormat.parse(valuesJson.get(key.name).asString).time)
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
                }
              } else {
                val keyQueryJson: JsonObject = valuesJson.get(key.name).asJsonObject
                when {
                  keyQueryJson.has("equals") -> {
                    keyQuery += " AND ${valueAlias}.dateValue = :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Date(dateFormat.parse(keyQueryJson.get("equals").asString).time)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {equals:'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("greaterThanEquals") -> {
                    keyQuery += " AND ${valueAlias}.dateValue >= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Date(dateFormat.parse(keyQueryJson.get("greaterThanEquals").asString).time)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("lessThanEquals") -> {
                    keyQuery += " AND ${valueAlias}.dateValue <= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Date(dateFormat.parse(keyQueryJson.get("lessThanEquals").asString).time)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("greaterThan") -> {
                    keyQuery += " AND ${valueAlias}.dateValue > :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Date(dateFormat.parse(keyQueryJson.get("greaterThan").asString).time)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("lessThan") -> {
                    keyQuery += " AND ${valueAlias}.dateValue < :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Date(dateFormat.parse(keyQueryJson.get("lessThan").asString).time)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("between") -> {
                    keyQuery += " AND ${valueAlias}.dateValue BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get("between").isJsonArray)
                      throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("between").asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                    keyQueryJson.get("between").asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        java.sql.Date(dateFormat.parse(it.asString).time)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                  keyQueryJson.has("notBetween") -> {
                    keyQuery += " AND ${valueAlias}.dateValue NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get("notBetween").isJsonArray)
                      throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("notBetween").asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                    keyQueryJson.get("notBetween").asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        java.sql.Date(dateFormat.parse(it.asString).time)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                  keyQueryJson.has("in") -> {
                    keyQuery += " AND ${valueAlias}.dateValue IN :v${variableCount}"
                    if (!keyQueryJson.get("in").isJsonArray)
                      throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("in").asJsonArray.size() == 0)
                      throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("in").asJsonArray.map {
                        java.sql.Date(dateFormat.parse(it.asString).time)
                      }
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                    }
                  }
                }
              }
            }
            TypeConstants.TIME -> {
              if (!valuesJson.get(key.name).isJsonObject) {
                keyQuery += " AND ${valueAlias}.timeValue = :v${variableCount}"
                injectedValues["v${variableCount++}"] = try {
                  java.sql.Time(valuesJson.get(key.name).asLong)
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
                }
              } else {
                val keyQueryJson: JsonObject = valuesJson.get(key.name).asJsonObject
                when {
                  keyQueryJson.has("equals") -> {
                    keyQuery += " AND ${valueAlias}.timeValue = :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Time(keyQueryJson.get("equals").asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {equals:'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("greaterThanEquals") -> {
                    keyQuery += " AND ${valueAlias}.timeValue >= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Time(keyQueryJson.get("greaterThanEquals").asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("lessThanEquals") -> {
                    keyQuery += " AND ${valueAlias}.timeValue <= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Time(keyQueryJson.get("lessThanEquals").asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("greaterThan") -> {
                    keyQuery += " AND ${valueAlias}.timeValue > :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Time(keyQueryJson.get("greaterThan").asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("lessThan") -> {
                    keyQuery += " AND ${valueAlias}.timeValue < :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Time(keyQueryJson.get("lessThan").asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("between") -> {
                    keyQuery += " AND ${valueAlias}.timeValue BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get("between").isJsonArray)
                      throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("between").asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                    keyQueryJson.get("between").asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        java.sql.Time(it.asLong)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                  keyQueryJson.has("notBetween") -> {
                    keyQuery += " AND ${valueAlias}.timeValue NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get("notBetween").isJsonArray)
                      throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("notBetween").asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                    keyQueryJson.get("notBetween").asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        java.sql.Time(it.asLong)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                  keyQueryJson.has("in") -> {
                    keyQuery += " AND ${valueAlias}.timeValue IN :v${variableCount}"
                    if (!keyQueryJson.get("in").isJsonArray)
                      throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("in").asJsonArray.size() == 0)
                      throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("in").asJsonArray.map {
                        java.sql.Time(it.asLong)
                      }
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                    }
                  }
                }
              }
            }
            TypeConstants.TIMESTAMP -> {
              if (!valuesJson.get(key.name).isJsonObject) {
                keyQuery += " AND ${valueAlias}.timestampValue = :v${variableCount}"
                injectedValues["v${variableCount++}"] = try {
                  Timestamp(valuesJson.get(key.name).asLong)
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
                }
              } else {
                val keyQueryJson: JsonObject = valuesJson.get(key.name).asJsonObject
                when {
                  keyQueryJson.has("equals") -> {
                    keyQuery += " AND ${valueAlias}.timestampValue = :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      Timestamp(keyQueryJson.get("equals").asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {equals:'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("greaterThanEquals") -> {
                    keyQuery += " AND ${valueAlias}.timestampValue >= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      Timestamp(keyQueryJson.get("greaterThanEquals").asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("lessThanEquals") -> {
                    keyQuery += " AND ${valueAlias}.timestampValue <= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      Timestamp(keyQueryJson.get("lessThanEquals").asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("greaterThan") -> {
                    keyQuery += " AND ${valueAlias}.timestampValue > :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      Timestamp(keyQueryJson.get("greaterThan").asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("lessThan") -> {
                    keyQuery += " AND ${valueAlias}.timestampValue < :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      Timestamp(keyQueryJson.get("lessThan").asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}}")
                    }
                  }
                  keyQueryJson.has("between") -> {
                    keyQuery += " AND ${valueAlias}.timestampValue BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get("between").isJsonArray)
                      throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("between").asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                    keyQueryJson.get("between").asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        Timestamp(it.asLong)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                  keyQueryJson.has("notBetween") -> {
                    keyQuery += " AND ${valueAlias}.timestampValue NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get("notBetween").isJsonArray)
                      throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("notBetween").asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                    keyQueryJson.get("notBetween").asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        Timestamp(it.asLong)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                      }
                    }
                  }
                  keyQueryJson.has("in") -> {
                    keyQuery += " AND ${valueAlias}.timestampValue IN :v${variableCount}"
                    if (!keyQueryJson.get("in").isJsonArray)
                      throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                    else if (keyQueryJson.get("in").asJsonArray.size() == 0)
                      throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get("in").asJsonArray.map {
                        Timestamp(it.asLong)
                      }
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                    }
                  }
                }
              }
            }
            TypeConstants.FORMULA -> {
            }
            else -> {
                  if (!valuesJson.get(key.name).isJsonObject) {
                  keyQuery += " AND ${valueAlias}.referencedVariable.type = :v${variableCount + 1} AND ${valueAlias}.referencedVariable.name = :v${variableCount + 2}"
                  injectedValues["v${variableCount++}"] = key.type
                  injectedValues["v${variableCount++}"] = try {
                    valuesJson.get(key.name).asString
                  } catch (exception: Exception) {
                    throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
                  }
                } else {
                  val keyQueryJson: JsonObject = valuesJson.get(key.name).asJsonObject
                  when {
                    keyQueryJson.has("equals") -> {
                      if (keyQueryJson.get("equals").isJsonObject) {
                        val (generatedQuery, count, _) = try {
                          generateQuery(queryJson = keyQueryJson.get("equals").asJsonObject, injectedVariableCount = variableCount, injectedValues = injectedValues, parentValueAlias = valueAlias, username = username,
                              typePermission = if (username == "PUBLIC") {
                                typePermissionRepository.findTypePermission(organizationId = key.parentType.organization.id,
                                   typeName = key.type.name,
                                    name = "PUBLIC")
                                    ?: throw CustomJsonException("{typeName: 'Type cannot be determined'}")
                              } else {
                                userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
                                  addProperty("organization", key.parentType.organization.id)
                                  addProperty("username", username)
                                  addProperty("typeName", key.type.name)
                                })
                              })
                        } catch (exception: CustomJsonException) {
                          throw CustomJsonException("{${key.name}: {equals: ${exception.message}}}")
                        }
                        variableCount = count
                        keyQuery += " AND EXISTS (${generatedQuery})"
                      } else {
                        keyQuery += " AND ${valueAlias}.referencedVariable.type = :v${variableCount + 1} AND ${valueAlias}.referencedVariable.name = :v${variableCount + 2}"
                        injectedValues["v${variableCount++}"] = key.type
                        injectedValues["v${variableCount++}"] = try {
                          keyQueryJson.get("equals").asString
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.name}: {equals: 'Unexpected value for parameter'}}")
                        }
                      }
                    }
                    keyQueryJson.has("like") -> {
                      keyQuery += " AND ${valueAlias}.referencedVariable.type = :v${variableCount + 1} AND ${valueAlias}.referencedVariable.name LIKE :v${variableCount + 2}"
                      injectedValues["v${variableCount++}"] = key.type
                      injectedValues["v${variableCount++}"] = try {
                        keyQueryJson.get("like").asString
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {like: 'Unexpected value for parameter'}}")
                      }
                    }
                    keyQueryJson.has("between") -> {
                      keyQuery += " AND ${valueAlias}.referencedVariable.type = :v${variableCount + 1} AND ${valueAlias}.referencedVariable.name BETWEEN :v${variableCount + 2} AND :v${variableCount + 3}"
                      injectedValues["v${variableCount++}"] = key.type
                      if (!keyQueryJson.get("between").isJsonArray)
                        throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                      else if (keyQueryJson.get("between").asJsonArray.size() != 2)
                        throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                      keyQueryJson.get("between").asJsonArray.forEach {
                        injectedValues["v${variableCount++}"] = try {
                          it.asString
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.name}: {between: 'Unexpected value for parameter'}}")
                        }
                      }
                    }
                    keyQueryJson.has("notBetween") -> {
                      keyQuery += " AND ${valueAlias}.referencedVariable.type = :v${variableCount + 1} AND ${valueAlias}.referencedVariable.name NOT BETWEEN :v${variableCount + 2} AND :v${variableCount + 3}"
                      injectedValues["v${variableCount++}"] = key.type
                      if (!keyQueryJson.get("notBetween").isJsonArray)
                        throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                      else if (keyQueryJson.get("notBetween").asJsonArray.size() != 2)
                        throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                      keyQueryJson.get("notBetween").asJsonArray.forEach {
                        injectedValues["v${variableCount++}"] = try {
                          it.asString
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.name}: {notBetween: 'Unexpected value for parameter'}}")
                        }
                      }
                    }
                    keyQueryJson.has("in") -> {
                      keyQuery += " AND ${valueAlias}.referencedVariable.type = :v${variableCount + 1} AND ${valueAlias}.referencedVariable.name IN :v${variableCount + 2}"
                      injectedValues["v${variableCount++}"] = key.type
                      if (!keyQueryJson.get("in").isJsonArray)
                        throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                      else if (keyQueryJson.get("in").asJsonArray.size() == 0)
                        throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                      injectedValues["v${variableCount++}"] = try {
                        keyQueryJson.get("in").asJsonArray.map {
                          it.asString
                        }
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {in: 'Unexpected value for parameter'}}")
                      }
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
              hql += " AND ${variableAlias}.name = :v${variableCount}"
              injectedValues["v${variableCount++}"] = try {
                variableQueryJson.get("equals").asString
              } catch (exception: Exception) {
                throw CustomJsonException("{variableName: {equals: 'Unexpected value for parameter'}}")
              }
            }
            variableQueryJson.has("like") -> {
              hql += " AND ${variableAlias}.name LIKE :v${variableCount}"
              injectedValues["v${variableCount++}"] = try {
                variableQueryJson.get("like").asString
              } catch (exception: Exception) {
                throw CustomJsonException("{variableName: {like: 'Unexpected value for parameter'}}")
              }
            }
            variableQueryJson.has("between") -> {
              hql += " AND ${variableAlias}.name BETWEEN :v${variableCount} AND :v${variableCount + 1}"
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
              hql += " AND ${variableAlias}.name NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
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
              hql += " AND ${variableAlias}.name IN :v${variableCount}"
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
          hql += " AND ${variableAlias}.name = :v${variableCount}"
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
        "SELECT DISTINCT $variableAlias  FROM Variable $variableAlias  WHERE ${variableAlias}.type = :v${variableCount} AND ${variableAlias}=${parentValueAlias}.referencedVariable"
      else
        "SELECT DISTINCT $variableAlias  FROM Variable $variableAlias  WHERE ${variableAlias}.type = :v${variableCount}"
      injectedValues["v${variableCount++}"] = typePermission.type
      if (queryJson.has("variableName")) {
        if (queryJson.get("variableName").isJsonObject) {
          val variableQueryJson: JsonObject = queryJson.get("variableName").asJsonObject
          when {
            variableQueryJson.has("equals") -> {
              hql += " AND ${variableAlias}.name = :v${variableCount}"
              injectedValues["v${variableCount++}"] = try {
                variableQueryJson.get("equals").asString
              } catch (exception: Exception) {
                throw CustomJsonException("{variableName: {equals: 'Unexpected value for parameter'}}")
              }
            }
            variableQueryJson.has("like") -> {
              hql += " AND ${variableAlias}.name LIKE :v${variableCount}"
              injectedValues["v${variableCount++}"] = try {
                variableQueryJson.get("like").asString
              } catch (exception: Exception) {
                throw CustomJsonException("{variableName: {like: 'Unexpected value for parameter'}}")
              }
            }
            variableQueryJson.has("between") -> {
              hql += " AND ${variableAlias}.name BETWEEN :v${variableCount} AND :v${variableCount + 1}"
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
              hql += " AND ${variableAlias}.name NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
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
              hql += " AND ${variableAlias}.name IN :v${variableCount}"
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
          hql += " AND ${variableAlias}.name = :v${variableCount}"
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
