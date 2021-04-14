/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.serializers.mappings

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.core.entities.mappings.UserSubspace
import com.pibity.core.serializers.serialize

fun serialize(userSubspace: UserSubspace): JsonObject = serialize(userSubspace.id.subspace)

fun serialize(entities: Set<UserSubspace>): JsonArray = entities.fold(JsonArray()) { acc, entity -> acc.apply { add(serialize(entity)) } }
