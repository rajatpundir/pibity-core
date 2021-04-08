/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.services

import com.google.gson.JsonObject
import com.pibity.core.commons.constants.*
import com.pibity.core.commons.CustomJsonException
import com.pibity.core.entities.Variable
import com.pibity.core.entities.permission.TypePermission
import com.pibity.core.repositories.query.TypePermissionRepository
import com.pibity.core.repositories.query.ValueRepository
import org.springframework.stereotype.Service
import java.sql.Timestamp

@Service
class VariableQueryService(
    val valueRepository: ValueRepository,
    val userService: UserService,
    val typePermissionRepository: TypePermissionRepository,
) {

  fun queryVariables(jsonParams: JsonObject, defaultTimestamp: Timestamp): Pair<List<Variable>, TypePermission> {
    val typePermission: TypePermission = userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
      addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
      addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
      addProperty(OrganizationConstants.TYPE_NAME, jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
    }, defaultTimestamp = defaultTimestamp)
    val (generatedQuery, _, injectedValues) = try {
      generateQuery(jsonParams.get(QueryConstants.QUERY).asJsonObject, username = jsonParams.get(OrganizationConstants.USERNAME).asString, typePermission = typePermission, defaultTimestamp = defaultTimestamp)
    } catch (exception: CustomJsonException) {
      throw CustomJsonException("{${QueryConstants.QUERY} : ${exception.message}}")
    }
    return Pair(valueRepository.queryVariables(hql = generatedQuery, injectedValues = injectedValues, limit = jsonParams.get(QueryConstants.LIMIT).asInt, offset = jsonParams.get(QueryConstants.OFFSET).asInt), typePermission)
  }

  fun queryPublicVariables(jsonParams: JsonObject, defaultTimestamp: Timestamp): Pair<List<Variable>, TypePermission> {
    val typePermission: TypePermission = typePermissionRepository.findTypePermission(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, typeName = jsonParams.get(OrganizationConstants.TYPE_NAME).asString, name = QueryConstants.PUBLIC_USER)
        ?: throw CustomJsonException("{${OrganizationConstants.TYPE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}")
    val (generatedQuery, _, injectedValues) = try {
      generateQuery(jsonParams.get(QueryConstants.QUERY).asJsonObject, username = QueryConstants.PUBLIC_USER, typePermission = typePermission, defaultTimestamp = defaultTimestamp)
    } catch (exception: CustomJsonException) {
      throw CustomJsonException("{${QueryConstants.QUERY} : ${exception.message}}")
    }
    return Pair(valueRepository.queryVariables(hql = generatedQuery, injectedValues = injectedValues, limit = jsonParams.get(QueryConstants.LIMIT).asInt, offset = jsonParams.get(QueryConstants.OFFSET).asInt), typePermission)
  }

  fun generateQuery(queryJson: JsonObject, username: String, typePermission: TypePermission, injectedVariableCount: Int = 0, injectedValues: MutableMap<String, Any> = mutableMapOf(), parentValueAlias: String? = null, defaultTimestamp: Timestamp): Triple<String, Int, MutableMap<String, Any>> {
    val valuesJson: JsonObject = if (!queryJson.has(VariableConstants.VALUES))
      throw CustomJsonException("${VariableConstants.VALUES}: ${MessageConstants.MISSING_FIELD}}")
    else {
      try {
        queryJson.get(VariableConstants.VALUES).asJsonObject
      } catch (exception: Exception) {
        throw CustomJsonException("${VariableConstants.VALUES}: ${MessageConstants.UNEXPECTED_VALUE}}")
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
                  throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
              } else {
                val keyQueryJson: JsonObject = valuesJson.get(key.name).asJsonObject
                when {
                  keyQueryJson.has(QueryOperators.EQUALS) -> {
                    keyQuery += " AND ${valueAlias}.stringValue = :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get(QueryOperators.EQUALS).asString
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {${QueryOperators.EQUALS}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.LIKE) -> {
                    keyQuery += " AND ${valueAlias}.stringValue LIKE :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get(QueryOperators.LIKE).asString
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {${QueryOperators.LIKE}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.BETWEEN) -> {
                    keyQuery += " AND ${valueAlias}.stringValue BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get(QueryOperators.BETWEEN).isJsonArray)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    else if (keyQueryJson.get(QueryOperators.BETWEEN).asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    keyQueryJson.get(QueryOperators.BETWEEN).asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        it.asString
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      }
                    }
                  }
                  keyQueryJson.has(QueryOperators.NOT_BETWEEN) -> {
                    keyQuery += " AND ${valueAlias}.stringValue NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get(QueryOperators.NOT_BETWEEN).isJsonArray)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    else if (keyQueryJson.get(QueryOperators.NOT_BETWEEN).asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    keyQueryJson.get(QueryOperators.NOT_BETWEEN).asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        it.asString
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      }
                    }
                  }
                  keyQueryJson.has(QueryOperators.IN) -> {
                    keyQuery += " AND ${valueAlias}.stringValue IN :v${variableCount}"
                    if (!keyQueryJson.get(QueryOperators.IN).isJsonArray)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    else if (keyQueryJson.get(QueryOperators.IN).asJsonArray.size() == 0)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get(QueryOperators.IN).asJsonArray.map {
                        it.asString
                      }
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
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
                  throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
              } else {
                val keyQueryJson: JsonObject = valuesJson.get(key.name).asJsonObject
                when {
                  keyQueryJson.has(QueryOperators.EQUALS) -> {
                    keyQuery += " AND ${valueAlias}.longValue = :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get(QueryOperators.EQUALS).asLong
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.GREATER_THAN_EQUALS) -> {
                    keyQuery += " AND ${valueAlias}.longValue >= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get(QueryOperators.GREATER_THAN_EQUALS).asLong
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.LESS_THAN_EQUALS) -> {
                    keyQuery += " AND ${valueAlias}.longValue <= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get(QueryOperators.LESS_THAN_EQUALS).asLong
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.GREATER_THAN) -> {
                    keyQuery += " AND ${valueAlias}.longValue > :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get(QueryOperators.GREATER_THAN).asLong
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.LESS_THAN) -> {
                    keyQuery += " AND ${valueAlias}.longValue < :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get(QueryOperators.LESS_THAN).asLong
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.BETWEEN) -> {
                    keyQuery += " AND ${valueAlias}.longValue BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get(QueryOperators.BETWEEN).isJsonArray)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    else if (keyQueryJson.get(QueryOperators.BETWEEN).asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    keyQueryJson.get(QueryOperators.BETWEEN).asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        it.asLong
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      }
                    }
                  }
                  keyQueryJson.has(QueryOperators.NOT_BETWEEN) -> {
                    keyQuery += " AND ${valueAlias}.longValue NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get(QueryOperators.NOT_BETWEEN).isJsonArray)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    else if (keyQueryJson.get(QueryOperators.NOT_BETWEEN).asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    keyQueryJson.get(QueryOperators.NOT_BETWEEN).asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        it.asLong
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      }
                    }
                  }
                  keyQueryJson.has(QueryOperators.IN) -> {
                    keyQuery += " AND ${valueAlias}.longValue IN :v${variableCount}"
                    if (!keyQueryJson.get(QueryOperators.IN).isJsonArray)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    else if (keyQueryJson.get(QueryOperators.IN).asJsonArray.size() == 0)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get(QueryOperators.IN).asJsonArray.map {
                        it.asLong
                      }
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
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
                  throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
              } else {
                val keyQueryJson: JsonObject = valuesJson.get(key.name).asJsonObject
                when {
                  keyQueryJson.has(QueryOperators.EQUALS) -> {
                    keyQuery += " AND ${valueAlias}.decimalValue = :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get(QueryOperators.EQUALS).asBigDecimal
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {${QueryOperators.EQUALS}:${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.GREATER_THAN_EQUALS) -> {
                    keyQuery += " AND ${valueAlias}.decimalValue >= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get(QueryOperators.GREATER_THAN_EQUALS).asBigDecimal
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.LESS_THAN_EQUALS) -> {
                    keyQuery += " AND ${valueAlias}.decimalValue <= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get(QueryOperators.LESS_THAN_EQUALS).asBigDecimal
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.GREATER_THAN) -> {
                    keyQuery += " AND ${valueAlias}.decimalValue > :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get(QueryOperators.GREATER_THAN).asBigDecimal
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.LESS_THAN) -> {
                    keyQuery += " AND ${valueAlias}.decimalValue < :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get(QueryOperators.LESS_THAN).asBigDecimal
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.BETWEEN) -> {
                    keyQuery += " AND ${valueAlias}.decimalValue BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get(QueryOperators.BETWEEN).isJsonArray)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    else if (keyQueryJson.get(QueryOperators.BETWEEN).asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    keyQueryJson.get(QueryOperators.BETWEEN).asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        it.asBigDecimal
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      }
                    }
                  }
                  keyQueryJson.has(QueryOperators.NOT_BETWEEN) -> {
                    keyQuery += " AND ${valueAlias}.decimalValue NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get(QueryOperators.NOT_BETWEEN).isJsonArray)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    else if (keyQueryJson.get(QueryOperators.NOT_BETWEEN).asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    keyQueryJson.get(QueryOperators.NOT_BETWEEN).asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        it.asBigDecimal
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      }
                    }
                  }
                  keyQueryJson.has(QueryOperators.IN) -> {
                    keyQuery += " AND ${valueAlias}.decimalValue IN :v${variableCount}"
                    if (!keyQueryJson.get(QueryOperators.IN).isJsonArray)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    else if (keyQueryJson.get(QueryOperators.IN).asJsonArray.size() == 0)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get(QueryOperators.IN).asJsonArray.map {
                        it.asBigDecimal
                      }
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
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
                  throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
              } else {
                val keyQueryJson: JsonObject = valuesJson.get(key.name).asJsonObject
                if (keyQueryJson.has(QueryOperators.EQUALS)) {
                  keyQuery += " AND ${valueAlias}.booleanValue = :v${variableCount}"
                  injectedValues["v${variableCount++}"] = try {
                    keyQueryJson.get(QueryOperators.EQUALS).asBoolean
                  } catch (exception: Exception) {
                    throw CustomJsonException("{${key.name}: {${QueryOperators.EQUALS}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                  }
                }
              }
            }
            TypeConstants.DATE -> {
              if (!valuesJson.get(key.name).isJsonObject) {
                keyQuery += " AND ${valueAlias}.dateValue = :v${variableCount}"
                injectedValues["v${variableCount++}"] = try {
                  java.sql.Date(valuesJson.get(key.name).asLong)
                } catch (exception: Exception) {
                  throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
              } else {
                val keyQueryJson: JsonObject = valuesJson.get(key.name).asJsonObject
                when {
                  keyQueryJson.has(QueryOperators.EQUALS) -> {
                    keyQuery += " AND ${valueAlias}.dateValue = :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Date(keyQueryJson.get(QueryOperators.EQUALS).asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {${QueryOperators.EQUALS}:${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.GREATER_THAN_EQUALS) -> {
                    keyQuery += " AND ${valueAlias}.dateValue >= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Date(keyQueryJson.get(QueryOperators.GREATER_THAN_EQUALS).asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.LESS_THAN_EQUALS) -> {
                    keyQuery += " AND ${valueAlias}.dateValue <= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Date(keyQueryJson.get(QueryOperators.LESS_THAN_EQUALS).asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.GREATER_THAN) -> {
                    keyQuery += " AND ${valueAlias}.dateValue > :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Date(keyQueryJson.get(QueryOperators.GREATER_THAN).asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.LESS_THAN) -> {
                    keyQuery += " AND ${valueAlias}.dateValue < :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Date(keyQueryJson.get(QueryOperators.LESS_THAN).asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.BETWEEN) -> {
                    keyQuery += " AND ${valueAlias}.dateValue BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get(QueryOperators.BETWEEN).isJsonArray)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    else if (keyQueryJson.get(QueryOperators.BETWEEN).asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    keyQueryJson.get(QueryOperators.BETWEEN).asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        java.sql.Date(it.asLong)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      }
                    }
                  }
                  keyQueryJson.has(QueryOperators.NOT_BETWEEN) -> {
                    keyQuery += " AND ${valueAlias}.dateValue NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get(QueryOperators.NOT_BETWEEN).isJsonArray)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    else if (keyQueryJson.get(QueryOperators.NOT_BETWEEN).asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    keyQueryJson.get(QueryOperators.NOT_BETWEEN).asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        java.sql.Date(it.asLong)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      }
                    }
                  }
                  keyQueryJson.has(QueryOperators.IN) -> {
                    keyQuery += " AND ${valueAlias}.dateValue IN :v${variableCount}"
                    if (!keyQueryJson.get(QueryOperators.IN).isJsonArray)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    else if (keyQueryJson.get(QueryOperators.IN).asJsonArray.size() == 0)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get(QueryOperators.IN).asJsonArray.map {
                        java.sql.Date(it.asLong)
                      }
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
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
                  throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
              } else {
                val keyQueryJson: JsonObject = valuesJson.get(key.name).asJsonObject
                when {
                  keyQueryJson.has(QueryOperators.EQUALS) -> {
                    keyQuery += " AND ${valueAlias}.timeValue = :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Time(keyQueryJson.get(QueryOperators.EQUALS).asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {${QueryOperators.EQUALS}:${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.GREATER_THAN_EQUALS) -> {
                    keyQuery += " AND ${valueAlias}.timeValue >= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Time(keyQueryJson.get(QueryOperators.GREATER_THAN_EQUALS).asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.LESS_THAN_EQUALS) -> {
                    keyQuery += " AND ${valueAlias}.timeValue <= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Time(keyQueryJson.get(QueryOperators.LESS_THAN_EQUALS).asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.GREATER_THAN) -> {
                    keyQuery += " AND ${valueAlias}.timeValue > :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Time(keyQueryJson.get(QueryOperators.GREATER_THAN).asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.LESS_THAN) -> {
                    keyQuery += " AND ${valueAlias}.timeValue < :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      java.sql.Time(keyQueryJson.get(QueryOperators.LESS_THAN).asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.BETWEEN) -> {
                    keyQuery += " AND ${valueAlias}.timeValue BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get(QueryOperators.BETWEEN).isJsonArray)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    else if (keyQueryJson.get(QueryOperators.BETWEEN).asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    keyQueryJson.get(QueryOperators.BETWEEN).asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        java.sql.Time(it.asLong)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      }
                    }
                  }
                  keyQueryJson.has(QueryOperators.NOT_BETWEEN) -> {
                    keyQuery += " AND ${valueAlias}.timeValue NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get(QueryOperators.NOT_BETWEEN).isJsonArray)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    else if (keyQueryJson.get(QueryOperators.NOT_BETWEEN).asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    keyQueryJson.get(QueryOperators.NOT_BETWEEN).asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        java.sql.Time(it.asLong)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      }
                    }
                  }
                  keyQueryJson.has(QueryOperators.IN) -> {
                    keyQuery += " AND ${valueAlias}.timeValue IN :v${variableCount}"
                    if (!keyQueryJson.get(QueryOperators.IN).isJsonArray)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    else if (keyQueryJson.get(QueryOperators.IN).asJsonArray.size() == 0)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get(QueryOperators.IN).asJsonArray.map {
                        java.sql.Time(it.asLong)
                      }
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
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
                  throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
              } else {
                val keyQueryJson: JsonObject = valuesJson.get(key.name).asJsonObject
                when {
                  keyQueryJson.has(QueryOperators.EQUALS) -> {
                    keyQuery += " AND ${valueAlias}.timestampValue = :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      Timestamp(keyQueryJson.get(QueryOperators.EQUALS).asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {${QueryOperators.EQUALS}:${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.GREATER_THAN_EQUALS) -> {
                    keyQuery += " AND ${valueAlias}.timestampValue >= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      Timestamp(keyQueryJson.get(QueryOperators.GREATER_THAN_EQUALS).asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.LESS_THAN_EQUALS) -> {
                    keyQuery += " AND ${valueAlias}.timestampValue <= :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      Timestamp(keyQueryJson.get(QueryOperators.LESS_THAN_EQUALS).asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.GREATER_THAN) -> {
                    keyQuery += " AND ${valueAlias}.timestampValue > :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      Timestamp(keyQueryJson.get(QueryOperators.GREATER_THAN).asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.LESS_THAN) -> {
                    keyQuery += " AND ${valueAlias}.timestampValue < :v${variableCount}"
                    injectedValues["v${variableCount++}"] = try {
                      Timestamp(keyQueryJson.get(QueryOperators.LESS_THAN).asLong)
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    }
                  }
                  keyQueryJson.has(QueryOperators.BETWEEN) -> {
                    keyQuery += " AND ${valueAlias}.timestampValue BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get(QueryOperators.BETWEEN).isJsonArray)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    else if (keyQueryJson.get(QueryOperators.BETWEEN).asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    keyQueryJson.get(QueryOperators.BETWEEN).asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        Timestamp(it.asLong)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      }
                    }
                  }
                  keyQueryJson.has(QueryOperators.NOT_BETWEEN) -> {
                    keyQuery += " AND ${valueAlias}.timestampValue NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
                    if (!keyQueryJson.get(QueryOperators.NOT_BETWEEN).isJsonArray)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    else if (keyQueryJson.get(QueryOperators.NOT_BETWEEN).asJsonArray.size() != 2)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    keyQueryJson.get(QueryOperators.NOT_BETWEEN).asJsonArray.forEach {
                      injectedValues["v${variableCount++}"] = try {
                        Timestamp(it.asLong)
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      }
                    }
                  }
                  keyQueryJson.has(QueryOperators.IN) -> {
                    keyQuery += " AND ${valueAlias}.timestampValue IN :v${variableCount}"
                    if (!keyQueryJson.get(QueryOperators.IN).isJsonArray)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    else if (keyQueryJson.get(QueryOperators.IN).asJsonArray.size() == 0)
                      throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                    injectedValues["v${variableCount++}"] = try {
                      keyQueryJson.get(QueryOperators.IN).asJsonArray.map {
                        Timestamp(it.asLong)
                      }
                    } catch (exception: Exception) {
                      throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
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
                    throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
                  }
                } else {
                  val keyQueryJson: JsonObject = valuesJson.get(key.name).asJsonObject
                  when {
                    keyQueryJson.has(QueryOperators.EQUALS) -> {
                      if (keyQueryJson.get(QueryOperators.EQUALS).isJsonObject) {
                        val (generatedQuery, count, _) = try {
                          generateQuery(queryJson = keyQueryJson.get(QueryOperators.EQUALS).asJsonObject, injectedVariableCount = variableCount, injectedValues = injectedValues, parentValueAlias = valueAlias, username = username,
                              typePermission = if (username == QueryConstants.PUBLIC_USER) {
                                typePermissionRepository.findTypePermission(orgId = key.parentType.organization.id,
                                   typeName = key.type.name,
                                    name = QueryConstants.PUBLIC_USER)
                                    ?: throw CustomJsonException("{${OrganizationConstants.TYPE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
                              } else {
                                userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
                                  addProperty(OrganizationConstants.ORGANIZATION_ID, key.parentType.organization.id)
                                  addProperty(OrganizationConstants.USERNAME, username)
                                  addProperty(OrganizationConstants.TYPE_NAME, key.type.name)
                                }, defaultTimestamp = defaultTimestamp)
                              }, defaultTimestamp = defaultTimestamp)
                        } catch (exception: CustomJsonException) {
                          throw CustomJsonException("{${key.name}: {${QueryOperators.EQUALS}: ${exception.message}}}")
                        }
                        variableCount = count
                        keyQuery += " AND EXISTS (${generatedQuery})"
                      } else {
                        keyQuery += " AND ${valueAlias}.referencedVariable.type = :v${variableCount + 1} AND ${valueAlias}.referencedVariable.name = :v${variableCount + 2}"
                        injectedValues["v${variableCount++}"] = key.type
                        injectedValues["v${variableCount++}"] = try {
                          keyQueryJson.get(QueryOperators.EQUALS).asString
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.name}: {${QueryOperators.EQUALS}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                        }
                      }
                    }
                    keyQueryJson.has(QueryOperators.LIKE) -> {
                      keyQuery += " AND ${valueAlias}.referencedVariable.type = :v${variableCount + 1} AND ${valueAlias}.referencedVariable.name LIKE :v${variableCount + 2}"
                      injectedValues["v${variableCount++}"] = key.type
                      injectedValues["v${variableCount++}"] = try {
                        keyQueryJson.get(QueryOperators.LIKE).asString
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {${QueryOperators.LIKE}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      }
                    }
                    keyQueryJson.has(QueryOperators.BETWEEN) -> {
                      keyQuery += " AND ${valueAlias}.referencedVariable.type = :v${variableCount + 1} AND ${valueAlias}.referencedVariable.name BETWEEN :v${variableCount + 2} AND :v${variableCount + 3}"
                      injectedValues["v${variableCount++}"] = key.type
                      if (!keyQueryJson.get(QueryOperators.BETWEEN).isJsonArray)
                        throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      else if (keyQueryJson.get(QueryOperators.BETWEEN).asJsonArray.size() != 2)
                        throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      keyQueryJson.get(QueryOperators.BETWEEN).asJsonArray.forEach {
                        injectedValues["v${variableCount++}"] = try {
                          it.asString
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.name}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                        }
                      }
                    }
                    keyQueryJson.has(QueryOperators.NOT_BETWEEN) -> {
                      keyQuery += " AND ${valueAlias}.referencedVariable.type = :v${variableCount + 1} AND ${valueAlias}.referencedVariable.name NOT BETWEEN :v${variableCount + 2} AND :v${variableCount + 3}"
                      injectedValues["v${variableCount++}"] = key.type
                      if (!keyQueryJson.get(QueryOperators.NOT_BETWEEN).isJsonArray)
                        throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      else if (keyQueryJson.get(QueryOperators.NOT_BETWEEN).asJsonArray.size() != 2)
                        throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      keyQueryJson.get(QueryOperators.NOT_BETWEEN).asJsonArray.forEach {
                        injectedValues["v${variableCount++}"] = try {
                          it.asString
                        } catch (exception: Exception) {
                          throw CustomJsonException("{${key.name}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                        }
                      }
                    }
                    keyQueryJson.has(QueryOperators.IN) -> {
                      keyQuery += " AND ${valueAlias}.referencedVariable.type = :v${variableCount + 1} AND ${valueAlias}.referencedVariable.name IN :v${variableCount + 2}"
                      injectedValues["v${variableCount++}"] = key.type
                      if (!keyQueryJson.get(QueryOperators.IN).isJsonArray)
                        throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      else if (keyQueryJson.get(QueryOperators.IN).asJsonArray.size() == 0)
                        throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                      injectedValues["v${variableCount++}"] = try {
                        keyQueryJson.get(QueryOperators.IN).asJsonArray.map {
                          it.asString
                        }
                      } catch (exception: Exception) {
                        throw CustomJsonException("{${key.name}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
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
        "SELECT DISTINCT $variableAlias FROM Variable $variableAlias WHERE ${variableAlias}.type = :v${variableCount} AND  EXISTS " + keyQueries.joinToString(separator = " AND EXISTS ") + " AND ${variableAlias}=${parentValueAlias}.referencedVariable"
      else
        "SELECT DISTINCT $variableAlias FROM Variable $variableAlias WHERE ${variableAlias}.type = :v${variableCount} AND  EXISTS " + keyQueries.joinToString(separator = " AND EXISTS ")
      injectedValues["v${variableCount++}"] = typePermission.type
      if (queryJson.has(VariableConstants.VARIABLE_NAME)) {
        if (queryJson.get(VariableConstants.VARIABLE_NAME).isJsonObject) {
          val variableQueryJson: JsonObject = queryJson.get(VariableConstants.VARIABLE_NAME).asJsonObject
          when {
            variableQueryJson.has(QueryOperators.EQUALS) -> {
              hql += " AND ${variableAlias}.name = :v${variableCount}"
              injectedValues["v${variableCount++}"] = try {
                variableQueryJson.get(QueryOperators.EQUALS).asString
              } catch (exception: Exception) {
                throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.EQUALS}: ${MessageConstants.UNEXPECTED_VALUE}}}")
              }
            }
            variableQueryJson.has(QueryOperators.LIKE) -> {
              hql += " AND ${variableAlias}.name LIKE :v${variableCount}"
              injectedValues["v${variableCount++}"] = try {
                variableQueryJson.get(QueryOperators.LIKE).asString
              } catch (exception: Exception) {
                throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.LIKE}: ${MessageConstants.UNEXPECTED_VALUE}}}")
              }
            }
            variableQueryJson.has(QueryOperators.BETWEEN) -> {
              hql += " AND ${variableAlias}.name BETWEEN :v${variableCount} AND :v${variableCount + 1}"
              if (!variableQueryJson.get(QueryOperators.BETWEEN).isJsonArray)
                throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
              else if (variableQueryJson.get(QueryOperators.BETWEEN).asJsonArray.size() != 2)
                throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
              variableQueryJson.get(QueryOperators.BETWEEN).asJsonArray.forEach {
                injectedValues["v${variableCount++}"] = try {
                  it.asString
                } catch (exception: Exception) {
                  throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                }
              }
            }
            variableQueryJson.has(QueryOperators.NOT_BETWEEN) -> {
              hql += " AND ${variableAlias}.name NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
              if (!variableQueryJson.get(QueryOperators.NOT_BETWEEN).isJsonArray)
                throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
              else if (variableQueryJson.get(QueryOperators.NOT_BETWEEN).asJsonArray.size() != 2)
                throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
              variableQueryJson.get(QueryOperators.NOT_BETWEEN).asJsonArray.forEach {
                injectedValues["v${variableCount++}"] = try {
                  it.asString
                } catch (exception: Exception) {
                  throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                }
              }
            }
            variableQueryJson.has(QueryOperators.IN) -> {
              hql += " AND ${variableAlias}.name IN :v${variableCount}"
              if (!variableQueryJson.get(QueryOperators.IN).isJsonArray)
                throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
              else if (variableQueryJson.get(QueryOperators.IN).asJsonArray.size() == 0)
                throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
              injectedValues["v${variableCount++}"] = try {
                variableQueryJson.get(QueryOperators.IN).asJsonArray.map {
                  it.asString
                }
              } catch (exception: Exception) {
                throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
              }
            }
          }
        } else {
          hql += " AND ${variableAlias}.name = :v${variableCount}"
          injectedValues["v${variableCount++}"] = try {
            queryJson.get(VariableConstants.VARIABLE_NAME).asString
          } catch (exception: Exception) {
            throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
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
      if (queryJson.has(VariableConstants.VARIABLE_NAME)) {
        if (queryJson.get(VariableConstants.VARIABLE_NAME).isJsonObject) {
          val variableQueryJson: JsonObject = queryJson.get(VariableConstants.VARIABLE_NAME).asJsonObject
          when {
            variableQueryJson.has(QueryOperators.EQUALS) -> {
              hql += " AND ${variableAlias}.name = :v${variableCount}"
              injectedValues["v${variableCount++}"] = try {
                variableQueryJson.get(QueryOperators.EQUALS).asString
              } catch (exception: Exception) {
                throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.EQUALS}: ${MessageConstants.UNEXPECTED_VALUE}}}")
              }
            }
            variableQueryJson.has(QueryOperators.LIKE) -> {
              hql += " AND ${variableAlias}.name LIKE :v${variableCount}"
              injectedValues["v${variableCount++}"] = try {
                variableQueryJson.get(QueryOperators.LIKE).asString
              } catch (exception: Exception) {
                throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.LIKE}: ${MessageConstants.UNEXPECTED_VALUE}}}")
              }
            }
            variableQueryJson.has(QueryOperators.BETWEEN) -> {
              hql += " AND ${variableAlias}.name BETWEEN :v${variableCount} AND :v${variableCount + 1}"
              if (!variableQueryJson.get(QueryOperators.BETWEEN).isJsonArray)
                throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
              else if (variableQueryJson.get(QueryOperators.BETWEEN).asJsonArray.size() != 2)
                throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
              variableQueryJson.get(QueryOperators.BETWEEN).asJsonArray.forEach {
                injectedValues["v${variableCount++}"] = try {
                  it.asString
                } catch (exception: Exception) {
                  throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                }
              }
            }
            variableQueryJson.has(QueryOperators.NOT_BETWEEN) -> {
              hql += " AND ${variableAlias}.name NOT BETWEEN :v${variableCount} AND :v${variableCount + 1}"
              if (!variableQueryJson.get(QueryOperators.NOT_BETWEEN).isJsonArray)
                throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
              else if (variableQueryJson.get(QueryOperators.NOT_BETWEEN).asJsonArray.size() != 2)
                throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
              variableQueryJson.get(QueryOperators.NOT_BETWEEN).asJsonArray.forEach {
                injectedValues["v${variableCount++}"] = try {
                  it.asString
                } catch (exception: Exception) {
                  throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.NOT_BETWEEN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
                }
              }
            }
            variableQueryJson.has(QueryOperators.IN) -> {
              hql += " AND ${variableAlias}.name IN :v${variableCount}"
              if (!variableQueryJson.get(QueryOperators.IN).isJsonArray)
                throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
              else if (variableQueryJson.get(QueryOperators.IN).asJsonArray.size() == 0)
                throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
              injectedValues["v${variableCount++}"] = try {
                variableQueryJson.get(QueryOperators.IN).asJsonArray.map {
                  it.asString
                }
              } catch (exception: Exception) {
                throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: {${QueryOperators.IN}: ${MessageConstants.UNEXPECTED_VALUE}}}")
              }
            }
          }
        } else {
          hql += " AND ${variableAlias}.name = :v${variableCount}"
          injectedValues["v${variableCount++}"] = try {
            queryJson.get(VariableConstants.VARIABLE_NAME).asString
          } catch (exception: Exception) {
            throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
          }
        }
      }
      Triple(hql, variableCount, injectedValues)
    }
  }
}
