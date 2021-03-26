/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.serializers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.OrganizationConstants
import com.pibity.erp.commons.utils.gson
import com.pibity.erp.entities.assertion.TypeAssertion

fun serialize(typeAssertion: TypeAssertion): JsonObject = JsonObject().apply {
  addProperty(OrganizationConstants.ORGANIZATION_ID, typeAssertion.type.organization.id)
  addProperty(OrganizationConstants.TYPE_NAME, typeAssertion.type.name)
  addProperty("assertionName", typeAssertion.name)
  add("expression", gson.fromJson(typeAssertion.expression, JsonObject::class.java))
}

fun serialize(entities: Set<TypeAssertion>): JsonArray = entities.fold(JsonArray()) { acc, entity -> acc.apply { add(serialize(entity)) } }
