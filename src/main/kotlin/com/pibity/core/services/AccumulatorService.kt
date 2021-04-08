/*
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.services

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.core.commons.CustomJsonException
import com.pibity.core.commons.constants.*
import com.pibity.core.entities.Key
import com.pibity.core.entities.Type
import com.pibity.core.entities.accumulator.TypeAccumulator
import com.pibity.core.entities.accumulator.VariableAccumulator
import com.pibity.core.entities.permission.TypePermission
import com.pibity.core.entities.uniqueness.TypeUniqueness
import com.pibity.core.repositories.jpa.OrganizationJpaRepository
import com.pibity.core.repositories.jpa.TypeAccumulatorJpaRepository
import com.pibity.core.repositories.query.TypeRepository
import com.pibity.core.repositories.query.TypeUniquenessRepository
import com.pibity.core.repositories.query.VariableRepository
import com.pibity.core.utils.gson
import com.pibity.core.utils.validateAccumulator
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.sql.Time
import java.sql.Timestamp
import java.util.*

@Service
class AccumulatorService(
    val organizationJpaRepository: OrganizationJpaRepository,
    val typeRepository: TypeRepository,
    val typeUniquenessRepository: TypeUniquenessRepository,
    val variableRepository: VariableRepository,
    val typeAccumulatorJpaRepository: TypeAccumulatorJpaRepository
) {

  fun createAccumulator(jsonParams: JsonObject, files: List<MultipartFile>, defaultTimestamp: Timestamp): TypeAccumulator {
    val typeUniqueness: TypeUniqueness = typeUniquenessRepository.findTypeUniqueness(orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong,
      typeName = jsonParams.get(OrganizationConstants.TYPE_NAME).asString, name = jsonParams.get(UniquenessConstants.UNIQUE_CONSTRAINT_NAME).asString)
      ?: throw CustomJsonException("{${OrganizationConstants.TYPE_NAME} : ${MessageConstants.UNEXPECTED_VALUE}}")
    val types: Set<Type> = typeRepository.findTypes(orgId = typeUniqueness.type.id)
    val symbolPaths: MutableSet<String> = mutableSetOf()
    val keyDependencies: MutableSet<Key> = mutableSetOf()
    val accumulatorJson: JsonObject = validateAccumulator(jsonParams = jsonParams, types = types, typeUniqueness = typeUniqueness, symbolPaths = symbolPaths, keyDependencies = keyDependencies, files = files)
    return typeAccumulatorJpaRepository.save(TypeAccumulator(typeUniqueness = typeUniqueness, name = accumulatorJson.get(AccumulatorConstants.ACCUMULATOR_NAME).asString,
      keys = jsonParams.get("keys").asJsonArray.map { keyName -> typeUniqueness.keys.single { it.name == keyName.asString } }.toMutableSet(),
      type = types.single { it.name == accumulatorJson.get(AccumulatorConstants.ACCUMULATOR_TYPE).asString },
      symbolPaths = gson.toJson(symbolPaths),
      forwardExpression = accumulatorJson.get(AccumulatorConstants.FORWARD_EXPRESSION).asJsonObject.toString(),
      backwardExpression = accumulatorJson.get(AccumulatorConstants.BACKWARD_EXPRESSION).asJsonObject.toString(),
      created = defaultTimestamp
    ).apply {
      when(this.type.name) {
        TypeConstants.TEXT -> initialStringValue = jsonParams.get(AccumulatorConstants.INITIAL_VALUE).asString
        TypeConstants.NUMBER -> initialLongValue = jsonParams.get(AccumulatorConstants.INITIAL_VALUE).asLong
        TypeConstants.DECIMAL -> initialDecimalValue = jsonParams.get(AccumulatorConstants.INITIAL_VALUE).asBigDecimal
        TypeConstants.BOOLEAN -> initialBooleanValue = jsonParams.get(AccumulatorConstants.INITIAL_VALUE).asBoolean
        TypeConstants.DATE -> if(jsonParams.has(AccumulatorConstants.INITIAL_VALUE)) initialDateValue = Date(jsonParams.get(AccumulatorConstants.INITIAL_VALUE).asLong)
        TypeConstants.TIMESTAMP -> if(jsonParams.has(AccumulatorConstants.INITIAL_VALUE)) initialTimestampValue = Timestamp(jsonParams.get(AccumulatorConstants.INITIAL_VALUE).asLong)
        TypeConstants.TIME -> if(jsonParams.has(AccumulatorConstants.INITIAL_VALUE)) initialTimeValue = Time(jsonParams.get(AccumulatorConstants.INITIAL_VALUE).asLong)
        TypeConstants.BLOB -> initialBlobValue = BlobProxy.generateProxy(files[jsonParams.get(AccumulatorConstants.INITIAL_VALUE).asInt].bytes)
        TypeConstants.FORMULA -> throw CustomJsonException("{}")
        else -> {
          referencedVariable = variableRepository.findVariable(type = this.type, name = jsonParams.get(AccumulatorConstants.INITIAL_VALUE).asString)
            ?: throw CustomJsonException("{${AccumulatorConstants.INITIAL_VALUE}: ${MessageConstants.UNEXPECTED_VALUE}}")
        }
      }
    })
  }

  fun serialize(variableAccumulator: VariableAccumulator, typePermission: TypePermission): JsonObject {
    return JsonObject().apply {
      addProperty(OrganizationConstants.ORGANIZATION_ID, variableAccumulator.typeAccumulator.typeUniqueness.type.id)
      addProperty(OrganizationConstants.TYPE_NAME, variableAccumulator.typeAccumulator.typeUniqueness.type.name)
      addProperty(UniquenessConstants.UNIQUE_CONSTRAINT_NAME, variableAccumulator.typeAccumulator.typeUniqueness.name)
      addProperty(AccumulatorConstants.ACCUMULATOR_NAME, variableAccumulator.typeAccumulator.name)
      addProperty(AccumulatorConstants.ACCUMULATOR_TYPE, variableAccumulator.typeAccumulator.type.name)
      if (variableAccumulator.values.any { valueAccumulator -> typePermission.keyPermissions.all { it.key == valueAccumulator.key && it.accessLevel > PermissionConstants.NO_ACCESS } }) {
        add(VariableConstants.VALUES, variableAccumulator.values.fold(JsonObject()) { acc, valueAccumulator ->
            acc.apply {
              when(valueAccumulator.key.type.name) {
                TypeConstants.TEXT -> addProperty(valueAccumulator.key.name, valueAccumulator.stringValue!!)
                TypeConstants.NUMBER -> addProperty(valueAccumulator.key.name, valueAccumulator.longValue!!)
                TypeConstants.DECIMAL -> addProperty(valueAccumulator.key.name, valueAccumulator.decimalValue!!)
                TypeConstants.BOOLEAN -> addProperty(valueAccumulator.key.name, valueAccumulator.booleanValue!!)
                TypeConstants.DATE -> addProperty(valueAccumulator.key.name, valueAccumulator.dateValue!!.time)
                TypeConstants.TIMESTAMP -> addProperty(valueAccumulator.key.name, valueAccumulator.timestampValue!!.time)
                TypeConstants.TIME -> addProperty(valueAccumulator.key.name, valueAccumulator.timeValue!!.time)
                TypeConstants.BLOB -> addProperty(valueAccumulator.key.name, Base64.getEncoder().encodeToString(valueAccumulator.blobValue!!.getBytes(1, valueAccumulator.blobValue!!.length().toInt())))
                TypeConstants.FORMULA -> throw CustomJsonException("{}")
                else -> addProperty(valueAccumulator.key.name, valueAccumulator.referencedVariable!!.name)
              }
            }
          })
        when(variableAccumulator.typeAccumulator.type.name) {
          TypeConstants.TEXT -> addProperty(AccumulatorConstants.ACCUMULATOR_VALUE, variableAccumulator.stringValue!!)
          TypeConstants.NUMBER -> addProperty(AccumulatorConstants.ACCUMULATOR_VALUE, variableAccumulator.longValue!!)
          TypeConstants.DECIMAL -> addProperty(AccumulatorConstants.ACCUMULATOR_VALUE, variableAccumulator.decimalValue!!)
          TypeConstants.BOOLEAN -> addProperty(AccumulatorConstants.ACCUMULATOR_VALUE, variableAccumulator.booleanValue!!)
          TypeConstants.DATE -> addProperty(AccumulatorConstants.ACCUMULATOR_VALUE, variableAccumulator.dateValue!!.time)
          TypeConstants.TIMESTAMP -> addProperty(AccumulatorConstants.ACCUMULATOR_VALUE, variableAccumulator.timestampValue!!.time)
          TypeConstants.TIME -> addProperty(AccumulatorConstants.ACCUMULATOR_VALUE, variableAccumulator.timeValue!!.time)
          TypeConstants.BLOB -> addProperty(AccumulatorConstants.ACCUMULATOR_VALUE, Base64.getEncoder().encodeToString(variableAccumulator.blobValue!!.getBytes(1, variableAccumulator.blobValue!!.length().toInt())))
          TypeConstants.FORMULA -> throw CustomJsonException("{}")
          else -> addProperty(AccumulatorConstants.ACCUMULATOR_VALUE, variableAccumulator.referencedVariable!!.name)
        }
      }
    }
  }

  fun serialize(variableAccumulators: Set<VariableAccumulator>, typePermission: TypePermission): JsonArray {
    return variableAccumulators.fold(JsonArray()) { acc, variableAccumulator ->
      acc.apply { add(serialize(variableAccumulator = variableAccumulator, typePermission = typePermission)) }
    }
  }
}
