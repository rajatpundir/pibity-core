/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.*
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.utils.*
import com.pibity.erp.entities.*
import com.pibity.erp.entities.function.FunctionInput
import com.pibity.erp.entities.function.Mapper
import com.pibity.erp.repositories.function.FunctionRepository
import com.pibity.erp.repositories.jpa.OrganizationJpaRepository
import com.pibity.erp.repositories.function.MapperRepository
import com.pibity.erp.repositories.function.jpa.MapperJpaRepository
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.sql.Timestamp

@Service
class MapperService(
  val organizationJpaRepository: OrganizationJpaRepository,
  val functionRepository: FunctionRepository,
  val functionService: FunctionService,
  val mapperRepository: MapperRepository,
  val mapperJpaRepository: MapperJpaRepository,
  val queryService: QueryService
) {

  fun createMapper(jsonParams: JsonObject, defaultTimestamp: Timestamp): Mapper {
    val functionInput: FunctionInput = functionRepository.findFunctionInput(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, functionName = jsonParams.get(FunctionConstants.FUNCTION_NAME).asString, name = jsonParams.get(MapperConstants.FUNCTION_INPUT).asString)
      ?: throw CustomJsonException("{${FunctionConstants.FUNCTION_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    return mapperJpaRepository.save(Mapper(organization = functionInput.function.organization, name = validateMapperName(jsonParams.get(MapperConstants.MAPPER_NAME).asString), query = jsonParams.get(MapperConstants.QUERY).asBoolean, functionInput = functionInput, created = defaultTimestamp).apply {
      if (query)
        queryParams.apply {
          val queryParams: List<String> = jsonParams.get(MapperConstants.QUERY_PARAMS).asJsonArray.map { it.asString }
          functionInput.type.keys.filter { key -> queryParams.contains(key.name) }.forEach { key -> add(key) }
        }
    })
  }

  fun getMapperDetails(jsonParams: JsonObject): Mapper {
    return mapperRepository.findMapper(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get(MapperConstants.MAPPER_NAME).asString)
      ?: throw CustomJsonException("{${MapperConstants.MAPPER_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
  }

  fun executeMapper(jsonParams: JsonObject, files: MutableList<MultipartFile>, defaultTimestamp: Timestamp): JsonArray {
    val mapper: Mapper = mapperRepository.findMapper(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong, name = jsonParams.get(MapperConstants.MAPPER_NAME).asString)
      ?: throw CustomJsonException("{${MapperConstants.MAPPER_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
    val args: JsonArray = validateMapperArgs(args = jsonParams.get(MapperConstants.ARGS).asJsonArray, mapper = mapper, defaultTimestamp = defaultTimestamp, files = files)
    if (args.size() == 0)
      throw CustomJsonException("{${MapperConstants.ARGS}: ${MessageConstants.UNEXPECTED_VALUE}}")
    return if (mapper.query) {
      val queryParams: JsonObject = validateQueryParamsForExecution(jsonParams = jsonParams.get("${MapperConstants.QUERY_PARAMS}?").asJsonObject, queryParams = mapper.queryParams, files = files)
      val (variables: List<Variable>, _) = queryService.queryVariables(jsonParams = JsonObject().apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, mapper.organization.id)
        addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
        addProperty(OrganizationConstants.TYPE_NAME, mapper.functionInput.type.name)
        addProperty(QueryConstants.LIMIT, 10000)
        addProperty(QueryConstants.OFFSET, 0)
        add(QueryConstants.QUERY, JsonObject().apply { add(VariableConstants.VALUES, queryParams) })
      }, defaultTimestamp = defaultTimestamp)
      if (variables.size > args.size())
        repeat(variables.size - args.size()) { args.add(args.last()) }
      variables.foldIndexed(JsonArray()) { index, acc, variable ->
        acc.apply {
          add(functionService.executeFunction(jsonParams = args.get(index).asJsonObject.apply { addProperty(mapper.functionInput.name, variable.name) }, defaultTimestamp = defaultTimestamp, files = files))
        }
      }
    } else {
      args.fold(JsonArray()) { acc, arg ->
        acc.apply {
          add(functionService.executeFunction(jsonParams = arg.asJsonObject, defaultTimestamp = defaultTimestamp, files = files))
        }
      }
    }
  }
}
