/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.serializers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.core.commons.constants.FunctionConstants
import com.pibity.core.commons.constants.MapperConstants
import com.pibity.core.commons.constants.OrganizationConstants
import com.pibity.core.entities.function.Mapper

fun serialize(mapper: Mapper): JsonObject = JsonObject().apply {
  addProperty(OrganizationConstants.ORGANIZATION_ID, mapper.organization.id)
  addProperty(MapperConstants.MAPPER_NAME, mapper.name)
  addProperty(MapperConstants.QUERY, mapper.query)
  add(MapperConstants.QUERY_PARAMS, mapper.queryParams.fold(JsonArray()) { acc, key ->
    acc.apply { key.name }
  })
  addProperty(FunctionConstants.FUNCTION_NAME, mapper.functionInput.function.name)
  addProperty(MapperConstants.FUNCTION_INPUT, mapper.functionInput.name)
}

fun serialize(entities: Set<Mapper>): JsonArray = entities.fold(JsonArray()) { acc, entity -> acc.apply { add(serialize(entity)) } }
