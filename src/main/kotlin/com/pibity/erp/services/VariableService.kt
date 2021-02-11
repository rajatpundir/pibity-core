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
import com.pibity.erp.commons.constants.PermissionConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.utils.*
import com.pibity.erp.entities.*
import com.pibity.erp.entities.permission.TypePermission
import com.pibity.erp.entities.uniqueness.TypeUniqueness
import com.pibity.erp.entities.uniqueness.VariableUniqueness
import com.pibity.erp.repositories.jpa.ValueJpaRepository
import com.pibity.erp.repositories.jpa.VariableAssertionJpaRepository
import com.pibity.erp.repositories.jpa.VariableJpaRepository
import com.pibity.erp.repositories.jpa.VariableUniquenessJpaRepository
import com.pibity.erp.repositories.query.VariableRepository
import com.pibity.erp.repositories.query.VariableUniquenessRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*

@Service
class VariableService(
  val variableRepository: VariableRepository,
  val variableJpaRepository: VariableJpaRepository,
  val userService: UserService,
  val valueJpaRepository: ValueJpaRepository,
  val variableAssertionJpaRepository: VariableAssertionJpaRepository,
  val variableUniquenessJpaRepository: VariableUniquenessJpaRepository,
  val variableUniquenessRepository: VariableUniquenessRepository
) {

  fun executeQueue(jsonParams: JsonObject): JsonArray {
    val multiLevelQueue = JsonArray()
    for (queue in jsonParams.get("queue").asJsonArray)
      if (!queue.isJsonArray)
        throw CustomJsonException("{queue: 'Unexpected value for parameter'}")
    for (queue in jsonParams.get("queue").asJsonArray) {
      try {
        multiLevelQueue.apply {
          add(
            mutateVariablesAtomically(
              jsonParams = queue.asJsonArray,
              orgId = jsonParams.get("orgId").asLong,
              username = jsonParams.get("username").asString
            )
          )
          add(JsonObject())
        }
      } catch (exception: CustomJsonException) {
        multiLevelQueue.add(JsonArray().apply { add(gson.fromJson(exception.message, JsonObject::class.java)) })
      }
    }
    return multiLevelQueue
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun mutateVariablesAtomically(jsonParams: JsonArray, orgId: Long, username: String): JsonArray {
    var step = 0
    val mutatedVariables = JsonArray()
    try {
      for (variableJson in validateMutatedVariables(jsonParams = jsonParams)) {
        when (variableJson.asJsonObject.get("op").asString) {
          "update" -> {
            val (variable, typePermission, _, _) = updateVariable(jsonParams = variableJson.asJsonObject.apply {
              addProperty("orgId", orgId)
              addProperty("username", username)
            })
            mutatedVariables.add(serialize(variable = variable, typePermission = typePermission, username = username))
          }
          "create" -> {
            val (variable, typePermission) = createVariable(jsonParams = variableJson.asJsonObject.apply {
              addProperty("orgId", orgId)
              addProperty("username", username)
            })
            mutatedVariables.add(serialize(variable = variable, typePermission = typePermission, username = username))
          }
          "delete" -> {
            val (variable, typePermission) = deleteVariable(jsonParams = variableJson.asJsonObject.apply {
              addProperty("orgId", orgId)
              addProperty("username", username)
            })
            mutatedVariables.add(serialize(variable = variable, typePermission = typePermission, username = username))
          }
          else -> throw CustomJsonException("{op: 'Unexpected value for parameter'}")
        }
        step += 1
      }
    } catch (exception: CustomJsonException) {
      throw CustomJsonException(
        gson.fromJson(exception.message, JsonObject::class.java).apply { addProperty("step", step) }.toString()
      )
    }
    return mutatedVariables
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createVariable(
    jsonParams: JsonObject,
    variableTypePermission: TypePermission? = null
  ): Pair<Variable, TypePermission> {
    val typePermission: TypePermission = variableTypePermission
      ?: userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
        addProperty("orgId", jsonParams.get("orgId").asString)
        addProperty("username", jsonParams.get("username").asString)
        addProperty("typeName", jsonParams.get("typeName").asString)
      })
    val type: Type = typePermission.type
    if (!typePermission.creatable)
      throw CustomJsonException("{error: 'Unauthorized Access'}")
    val variableName: String = jsonParams.get("variableName").asString
    val values: JsonObject =
      validateVariableValues(values = jsonParams.get("values").asJsonObject, typePermission = typePermission)
    var variable = Variable(type = type, name = if (type.autoId) UUID.randomUUID().toString() else variableName)
    // Process non-formula type values
    typePermission.keyPermissions.filter { it.key.type.name != TypeConstants.FORMULA }.forEach { keyPermission ->
      val key = keyPermission.key
      when (key.type.name) {
        TypeConstants.TEXT -> variable.values.add(
          Value(
            variable = variable,
            key = key,
            stringValue = values.get(key.name).asString
          )
        )
        TypeConstants.NUMBER -> variable.values.add(
          Value(
            variable = variable,
            key = key,
            longValue = values.get(key.name).asLong
          )
        )
        TypeConstants.DECIMAL -> variable.values.add(
          Value(
            variable = variable,
            key = key,
            decimalValue = values.get(key.name).asBigDecimal
          )
        )
        TypeConstants.BOOLEAN -> variable.values.add(
          Value(
            variable = variable,
            key = key,
            booleanValue = values.get(key.name).asBoolean
          )
        )
        TypeConstants.DATE -> variable.values.add(
          Value(
            variable = variable,
            key = key,
            dateValue = java.sql.Date(dateFormat.parse(values.get(key.name).asString).time)
          )
        )
        TypeConstants.TIME -> variable.values.add(
          Value(
            variable = variable,
            key = key,
            timeValue = java.sql.Time(values.get(key.name).asLong)
          )
        )
        TypeConstants.TIMESTAMP -> variable.values.add(
          Value(
            variable = variable,
            key = key,
            timestampValue = Timestamp(values.get(key.name).asLong)
          )
        )
        else -> {
          val referencedVariable: Variable =
            variableRepository.findByTypeAndName(type = key.type, name = values.get(key.name).asString)
              ?: throw CustomJsonException("{${key.name}: 'Unable to find referenced global variable'}")
          if (!referencedVariable.active)
            throw CustomJsonException("{${key.name}: 'Unable to reference variable as it is inactive'}")
          variable.values.add(Value(variable = variable, key = key, referencedVariable = referencedVariable))
        }
      }
    }
    variable = try {
      variableJpaRepository.save(variable)
    } catch (exception: Exception) {
      throw CustomJsonException("{variableName: 'Variable could not be created'}")
    }
    val allVariableDependencies: MutableSet<Variable> = mutableSetOf()
    typePermission.keyPermissions.filter { it.key.type.name == TypeConstants.FORMULA }.forEach { keyPermission ->
      val key: Key = keyPermission.key
      val valueDependencies: MutableSet<Value> = mutableSetOf()
      val variableDependencies: MutableSet<Variable> = mutableSetOf()
      val value: Value = when (key.formula!!.returnType.name) {
        TypeConstants.TEXT -> Value(variable = variable, key = key,
          stringValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(
            key.formula!!.expression,
            JsonObject::class.java
          ).apply { addProperty("expectedReturnType", key.formula!!.returnType.name) },
            symbols = getSymbolValuesAndUpdateDependencies(
              variable = variable,
              symbolPaths = gson.fromJson(key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }
                .toMutableSet(),
              valueDependencies = valueDependencies,
              variableDependencies = variableDependencies
            ), mode = "evaluate"
          ) as String
        )
        TypeConstants.NUMBER -> Value(variable = variable, key = key,
          longValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(
            key.formula!!.expression,
            JsonObject::class.java
          ).apply { addProperty("expectedReturnType", key.formula!!.returnType.name) },
            symbols = getSymbolValuesAndUpdateDependencies(
              variable = variable,
              symbolPaths = gson.fromJson(key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }
                .toMutableSet(),
              valueDependencies = valueDependencies,
              variableDependencies = variableDependencies
            ), mode = "evaluate"
          ) as Long
        )
        TypeConstants.DECIMAL -> Value(variable = variable, key = key,
          decimalValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(
            key.formula!!.expression,
            JsonObject::class.java
          ).apply { addProperty("expectedReturnType", key.formula!!.returnType.name) },
            symbols = getSymbolValuesAndUpdateDependencies(
              variable = variable,
              symbolPaths = gson.fromJson(key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }
                .toMutableSet(),
              valueDependencies = valueDependencies,
              variableDependencies = variableDependencies
            ), mode = "evaluate"
          ) as BigDecimal
        )
        TypeConstants.BOOLEAN -> Value(variable = variable, key = key,
          booleanValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(
            key.formula!!.expression,
            JsonObject::class.java
          ).apply { addProperty("expectedReturnType", key.formula!!.returnType.name) },
            symbols = getSymbolValuesAndUpdateDependencies(
              variable = variable,
              symbolPaths = gson.fromJson(key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }
                .toMutableSet(),
              valueDependencies = valueDependencies,
              variableDependencies = variableDependencies
            ), mode = "evaluate"
          ) as Boolean
        )
        else -> throw CustomJsonException("{${key.name}: 'Unable to compute formula value'}")
      }
      value.valueDependencies.addAll(valueDependencies)
      value.variableDependencies.addAll(variableDependencies)
      allVariableDependencies.addAll(variableDependencies)
      variable.values.add(value)
    }
    variable = try {
      variableJpaRepository.save(variable)
    } catch (exception: Exception) {
      throw CustomJsonException("{variableName: 'Variable could not be created'}")
    }
    variable.type.uniqueConstraints.forEach { typeUniqueness ->
      var input = ""
      typeUniqueness.keyUniquenessConstraints.sortedBy { it.id }.forEach { keyUniqueness ->
        val value: Value = variable.values.single { it.key == keyUniqueness.key }
        input += computeHash(
          when (value.key.type.name) {
            TypeConstants.TEXT -> value.stringValue!!
            TypeConstants.NUMBER -> value.longValue.toString()
            TypeConstants.DECIMAL -> value.decimalValue.toString()
            TypeConstants.BOOLEAN -> value.booleanValue.toString()
            TypeConstants.TIMESTAMP -> value.timestampValue.toString()
            TypeConstants.DATE -> value.dateValue.toString()
            TypeConstants.TIME -> value.timeValue.toString()
            TypeConstants.BLOB -> value.blobValue.toString()
            TypeConstants.FORMULA -> when (value.key.formula!!.returnType.name) {
              TypeConstants.TEXT -> value.stringValue!!
              TypeConstants.NUMBER -> value.longValue.toString()
              TypeConstants.DECIMAL -> value.decimalValue.toString()
              TypeConstants.BOOLEAN -> value.booleanValue.toString()
              else -> throw CustomJsonException("{}")
            }
            else -> value.referencedVariable!!.toString()
          }
        )
      }
      val computedHash: String = computeHash(input)
      try {
        variableUniquenessJpaRepository.save(
          VariableUniqueness(
            typeUniqueness = typeUniqueness,
            variable = variable,
            hash = computedHash
          )
        )
      } catch (exception: Exception) {
        resolveHashConflict(variable = variable, typeUniqueness = typeUniqueness, hash = computedHash)
      }
    }
    if (variable.type.hasAssertions) {
      for (typeAssertion in variable.type.typeAssertions) {
        val valueDependencies: MutableSet<Value> = mutableSetOf()
        val variableDependencies: MutableSet<Variable> = mutableSetOf()
        val variableAssertion = VariableAssertion(variable = variable,
          typeAssertion = typeAssertion,
          result = validateOrEvaluateExpression(jsonParams = gson.fromJson(
            typeAssertion.expression,
            JsonObject::class.java
          ).apply { addProperty("expectedReturnType", TypeConstants.BOOLEAN) },
            symbols = getSymbolValuesAndUpdateDependencies(
              variable = variable,
              symbolPaths = gson.fromJson(typeAssertion.symbolPaths, JsonArray::class.java).map { it.asString }
                .toMutableSet(),
              valueDependencies = valueDependencies,
              variableDependencies = variableDependencies,
              symbolsForFormula = false
            ), mode = "evaluate"
          ) as Boolean
        )
        variableAssertion.valueDependencies = valueDependencies
        variableAssertion.variableDependencies = variableDependencies
        if (!variableAssertion.result)
          throw CustomJsonException("{variableName: 'Failed to assert ${variableAssertion.variable.type.name}:${typeAssertion.name}'}")
        try {
          variable.variableAssertions.add(variableAssertionJpaRepository.save(variableAssertion))
        } catch (exception: Exception) {
          throw CustomJsonException("{variableName: 'Variable could not be created'}")
        }
      }
    }
    return try {
      Pair(variable, typePermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{variableName: 'Variable could not be created'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun updateVariable(
    jsonParams: JsonObject,
    referencedKeyVariable: Variable? = null,
    variableTypePermission: TypePermission? = null
  ): Quadruple<Variable, TypePermission, Map<Value, MutableSet<Value>>, Map<VariableAssertion, MutableSet<Value>>> {
    val typePermission: TypePermission = variableTypePermission
      ?: userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
        addProperty("orgId", jsonParams.get("orgId").asString)
        addProperty("username", jsonParams.get("username").asString)
        addProperty("typeName", jsonParams.get("typeName").asString)
      })
    val type: Type = typePermission.type
    val variable: Variable = referencedKeyVariable
      ?: variableRepository.findVariable(
        organizationId = type.organization.id,
        typeName = type.name,
        name = jsonParams.get("variableName").asString
      )
      ?: throw CustomJsonException("{variableName: 'Unable to find referenced variable'}")
    if (jsonParams.has("active?"))
      variable.active = jsonParams.get("active?").asBoolean
    val values: JsonObject = if (jsonParams.has("values")) validateUpdatedVariableValues(
      values = jsonParams.get("values").asJsonObject,
      typePermission = typePermission
    ) else JsonObject()
    val variableUniquenesses: MutableSet<VariableUniqueness> = mutableSetOf()
    val dependentFormulaValues: MutableMap<Value, MutableSet<Value>> = mutableMapOf()
    val dependentAssertions: MutableMap<VariableAssertion, MutableSet<Value>> = mutableMapOf()
    // Process non-formula type values
    variable.values.filter { it.key.type.name != TypeConstants.FORMULA }.forEach { value ->
      if (values.has(value.key.name)) {
        when (value.key.type.name) {
          TypeConstants.TEXT -> {
            value.stringValue = values.get(value.key.name).asString
            if (value.key.dependentKeyUniquenesses.isNotEmpty()) {
              value.key.dependentKeyUniquenesses.forEach { keyUniqueness ->
                variableUniquenesses.add(variable.variableUniquenesses.single { it.typeUniqueness == keyUniqueness.typeUniqueness })
              }
            }
            if (value.key.isFormulaDependency) {
              value.dependentValues.forEach {
                if (!dependentFormulaValues.containsKey(it))
                  dependentFormulaValues[it] = mutableSetOf(value)
                else
                  dependentFormulaValues[it]!!.add(value)
              }
            }
            if (value.key.isAssertionDependency) {
              value.dependentVariableAssertions.forEach {
                if (!dependentAssertions.containsKey(it))
                  dependentAssertions[it] = mutableSetOf(value)
                else
                  dependentAssertions[it]!!.add(value)
              }
            }
          }
          TypeConstants.NUMBER -> {
            value.longValue = values.get(value.key.name).asLong
            if (value.key.dependentKeyUniquenesses.isNotEmpty()) {
              value.key.dependentKeyUniquenesses.forEach { keyUniqueness ->
                variableUniquenesses.add(variable.variableUniquenesses.single { it.typeUniqueness == keyUniqueness.typeUniqueness })
              }
            }
            if (value.key.isFormulaDependency) {
              value.dependentValues.forEach {
                if (!dependentFormulaValues.containsKey(it))
                  dependentFormulaValues[it] = mutableSetOf(value)
                else
                  dependentFormulaValues[it]!!.add(value)
              }
            }
            if (value.key.isAssertionDependency) {
              value.dependentVariableAssertions.forEach {
                if (!dependentAssertions.containsKey(it))
                  dependentAssertions[it] = mutableSetOf(value)
                else
                  dependentAssertions[it]!!.add(value)
              }
            }
          }
          TypeConstants.DECIMAL -> {
            value.decimalValue = values.get(value.key.name).asBigDecimal
            if (value.key.dependentKeyUniquenesses.isNotEmpty()) {
              value.key.dependentKeyUniquenesses.forEach { keyUniqueness ->
                variableUniquenesses.add(variable.variableUniquenesses.single { it.typeUniqueness == keyUniqueness.typeUniqueness })
              }
            }
            if (value.key.isFormulaDependency) {
              value.dependentValues.forEach {
                if (!dependentFormulaValues.containsKey(it))
                  dependentFormulaValues[it] = mutableSetOf(value)
                else
                  dependentFormulaValues[it]!!.add(value)
              }
            }
            if (value.key.isAssertionDependency) {
              value.dependentVariableAssertions.forEach {
                if (!dependentAssertions.containsKey(it))
                  dependentAssertions[it] = mutableSetOf(value)
                else
                  dependentAssertions[it]!!.add(value)
              }
            }
          }
          TypeConstants.BOOLEAN -> {
            value.booleanValue = values.get(value.key.name).asBoolean
            if (value.key.dependentKeyUniquenesses.isNotEmpty()) {
              value.key.dependentKeyUniquenesses.forEach { keyUniqueness ->
                variableUniquenesses.add(variable.variableUniquenesses.single { it.typeUniqueness == keyUniqueness.typeUniqueness })
              }
            }
            if (value.key.isFormulaDependency) {
              value.dependentValues.forEach {
                if (!dependentFormulaValues.containsKey(it))
                  dependentFormulaValues[it] = mutableSetOf(value)
                else
                  dependentFormulaValues[it]!!.add(value)
              }
            }
            if (value.key.isAssertionDependency) {
              value.dependentVariableAssertions.forEach {
                if (!dependentAssertions.containsKey(it))
                  dependentAssertions[it] = mutableSetOf(value)
                else
                  dependentAssertions[it]!!.add(value)
              }
            }
          }
          TypeConstants.DATE -> {
            value.dateValue = java.sql.Date(dateFormat.parse(values.get(value.key.name).asString).time)
            if (value.key.dependentKeyUniquenesses.isNotEmpty()) {
              value.key.dependentKeyUniquenesses.forEach { keyUniqueness ->
                variableUniquenesses.add(variable.variableUniquenesses.single { it.typeUniqueness == keyUniqueness.typeUniqueness })
              }
            }
            if (value.key.isFormulaDependency) {
              value.dependentValues.forEach {
                if (!dependentFormulaValues.containsKey(it))
                  dependentFormulaValues[it] = mutableSetOf(value)
                else
                  dependentFormulaValues[it]!!.add(value)
              }
            }
            if (value.key.isAssertionDependency) {
              value.dependentVariableAssertions.forEach {
                if (!dependentAssertions.containsKey(it))
                  dependentAssertions[it] = mutableSetOf(value)
                else
                  dependentAssertions[it]!!.add(value)
              }
            }
          }
          TypeConstants.TIME -> {
            value.timeValue = java.sql.Time(values.get(value.key.name).asLong)
            if (value.key.dependentKeyUniquenesses.isNotEmpty()) {
              value.key.dependentKeyUniquenesses.forEach { keyUniqueness ->
                variableUniquenesses.add(variable.variableUniquenesses.single { it.typeUniqueness == keyUniqueness.typeUniqueness })
              }
            }
            if (value.key.isFormulaDependency) {
              value.dependentValues.forEach {
                if (!dependentFormulaValues.containsKey(it))
                  dependentFormulaValues[it] = mutableSetOf(value)
                else
                  dependentFormulaValues[it]!!.add(value)
              }
            }
            if (value.key.isAssertionDependency) {
              value.dependentVariableAssertions.forEach {
                if (!dependentAssertions.containsKey(it))
                  dependentAssertions[it] = mutableSetOf(value)
                else
                  dependentAssertions[it]!!.add(value)
              }
            }
          }
          TypeConstants.TIMESTAMP -> {
            value.timestampValue = Timestamp(values.get(value.key.name).asLong)
            if (value.key.dependentKeyUniquenesses.isNotEmpty()) {
              value.key.dependentKeyUniquenesses.forEach { keyUniqueness ->
                variableUniquenesses.add(variable.variableUniquenesses.single { it.typeUniqueness == keyUniqueness.typeUniqueness })
              }
            }
            if (value.key.isFormulaDependency) {
              value.dependentValues.forEach {
                if (!dependentFormulaValues.containsKey(it))
                  dependentFormulaValues[it] = mutableSetOf(value)
                else
                  dependentFormulaValues[it]!!.add(value)
              }
            }
            if (value.key.isAssertionDependency) {
              value.dependentVariableAssertions.forEach {
                if (!dependentAssertions.containsKey(it))
                  dependentAssertions[it] = mutableSetOf(value)
                else
                  dependentAssertions[it]!!.add(value)
              }
            }
          }
          TypeConstants.BLOB -> {
            value.blobValue = (values.get(value.key.name).asString).toByteArray()
            if (value.key.dependentKeyUniquenesses.isNotEmpty()) {
              value.key.dependentKeyUniquenesses.forEach { keyUniqueness ->
                variableUniquenesses.add(variable.variableUniquenesses.single { it.typeUniqueness == keyUniqueness.typeUniqueness })
              }
            }
            if (value.key.isFormulaDependency) {
              value.dependentValues.forEach {
                if (!dependentFormulaValues.containsKey(it))
                  dependentFormulaValues[it] = mutableSetOf(value)
                else
                  dependentFormulaValues[it]!!.add(value)
              }
            }
            if (value.key.isAssertionDependency) {
              value.dependentVariableAssertions.forEach {
                if (!dependentAssertions.containsKey(it))
                  dependentAssertions[it] = mutableSetOf(value)
                else
                  dependentAssertions[it]!!.add(value)
              }
            }
          }
          TypeConstants.FORMULA -> {/* Formulas may depend on provided values, so they will be computed separately */
          }
          else -> {
            variableJpaRepository.save(value.referencedVariable!!)
            val referencedVariable = variableRepository.findVariable(
              organizationId = type.organization.id,
              typeName = value.key.type.name,
              name = values.get(value.key.name).asString
            )
              ?: throw CustomJsonException("{${value.key.name}: 'Unable to find referenced variable ${values.get(value.key.name).asString}'}")
            if (!referencedVariable.active)
              throw CustomJsonException("{${value.key.name}: 'Unable to reference variable as it is inactive'}")
            value.referencedVariable = referencedVariable
            if (value.key.dependentKeyUniquenesses.isNotEmpty()) {
              value.key.dependentKeyUniquenesses.forEach { keyUniqueness ->
                variableUniquenesses.add(variable.variableUniquenesses.single { it.typeUniqueness == keyUniqueness.typeUniqueness })
              }
            }
            if (value.key.isVariableDependency) {
              value.dependentValues.forEach {
                if (!dependentFormulaValues.containsKey(it))
                  dependentFormulaValues[it] = mutableSetOf(value)
                else
                  dependentFormulaValues[it]!!.add(value)
              }
            }
            if (value.key.isAssertionDependency) {
              value.dependentVariableAssertions.forEach {
                if (!dependentAssertions.containsKey(it))
                  dependentAssertions[it] = mutableSetOf(value)
                else
                  dependentAssertions[it]!!.add(value)
              }
            }
          }
        }
      }
    }
    variable.values.filter { it.key.type.name == TypeConstants.FORMULA }.forEach { value ->
      if (dependentFormulaValues.contains(value)) {
        when (value.key.formula!!.returnType.name) {
          TypeConstants.TEXT -> {
            val evaluatedValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(
              value.key.formula!!.expression,
              JsonObject::class.java
            ).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
              symbols = getSymbolValues(
                variable = variable,
                symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }
                  .toMutableSet()
              ), mode = "evaluate"
            ) as String
            if (value.stringValue != evaluatedValue) {
              value.stringValue = evaluatedValue
              if (value.key.dependentKeyUniquenesses.isNotEmpty()) {
                value.key.dependentKeyUniquenesses.forEach { keyUniqueness ->
                  variableUniquenesses.add(variable.variableUniquenesses.single { it.typeUniqueness == keyUniqueness.typeUniqueness })
                }
              }
              if (value.key.isFormulaDependency) {
                value.dependentValues.forEach {
                  if (!dependentFormulaValues.containsKey(it))
                    dependentFormulaValues[it] = mutableSetOf(value)
                  else
                    dependentFormulaValues[it]!!.add(value)
                }
              }
              if (value.key.isAssertionDependency) {
                value.dependentVariableAssertions.forEach {
                  if (!dependentAssertions.containsKey(it))
                    dependentAssertions[it] = mutableSetOf(value)
                  else
                    dependentAssertions[it]!!.add(value)
                }
              }
            }
          }
          TypeConstants.NUMBER -> {
            val evaluatedValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(
              value.key.formula!!.expression,
              JsonObject::class.java
            ).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
              symbols = getSymbolValues(
                variable = variable,
                symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }
                  .toMutableSet()
              ), mode = "evaluate"
            ) as Long
            if (value.longValue != evaluatedValue) {
              value.longValue = evaluatedValue
              if (value.key.dependentKeyUniquenesses.isNotEmpty()) {
                value.key.dependentKeyUniquenesses.forEach { keyUniqueness ->
                  variableUniquenesses.add(variable.variableUniquenesses.single { it.typeUniqueness == keyUniqueness.typeUniqueness })
                }
              }
              if (value.key.isFormulaDependency) {
                value.dependentValues.forEach {
                  if (!dependentFormulaValues.containsKey(it))
                    dependentFormulaValues[it] = mutableSetOf(value)
                  else
                    dependentFormulaValues[it]!!.add(value)
                }
              }
              if (value.key.isAssertionDependency) {
                value.dependentVariableAssertions.forEach {
                  if (!dependentAssertions.containsKey(it))
                    dependentAssertions[it] = mutableSetOf(value)
                  else
                    dependentAssertions[it]!!.add(value)
                }
              }
            }
          }
          TypeConstants.DECIMAL -> {
            val evaluatedValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(
              value.key.formula!!.expression,
              JsonObject::class.java
            ).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
              symbols = getSymbolValues(
                variable = variable,
                symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }
                  .toMutableSet()
              ), mode = "evaluate"
            ) as BigDecimal
            if (value.decimalValue != evaluatedValue) {
              value.decimalValue = evaluatedValue
              if (value.key.dependentKeyUniquenesses.isNotEmpty()) {
                value.key.dependentKeyUniquenesses.forEach { keyUniqueness ->
                  variableUniquenesses.add(variable.variableUniquenesses.single { it.typeUniqueness == keyUniqueness.typeUniqueness })
                }
              }
              if (value.key.isFormulaDependency) {
                value.dependentValues.forEach {
                  if (!dependentFormulaValues.containsKey(it))
                    dependentFormulaValues[it] = mutableSetOf(value)
                  else
                    dependentFormulaValues[it]!!.add(value)
                }
              }
              if (value.key.isAssertionDependency) {
                value.dependentVariableAssertions.forEach {
                  if (!dependentAssertions.containsKey(it))
                    dependentAssertions[it] = mutableSetOf(value)
                  else
                    dependentAssertions[it]!!.add(value)
                }
              }
            }
          }
          TypeConstants.BOOLEAN -> {
            val evaluatedValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(
              value.key.formula!!.expression,
              JsonObject::class.java
            ).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
              symbols = getSymbolValues(
                variable = variable,
                symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }
                  .toMutableSet()
              ), mode = "evaluate"
            ) as Boolean
            if (value.booleanValue != evaluatedValue) {
              value.booleanValue = evaluatedValue
              if (value.key.dependentKeyUniquenesses.isNotEmpty()) {
                value.key.dependentKeyUniquenesses.forEach { keyUniqueness ->
                  variableUniquenesses.add(variable.variableUniquenesses.single { it.typeUniqueness == keyUniqueness.typeUniqueness })
                }
              }
              if (value.key.isFormulaDependency) {
                value.dependentValues.forEach {
                  if (!dependentFormulaValues.containsKey(it))
                    dependentFormulaValues[it] = mutableSetOf(value)
                  else
                    dependentFormulaValues[it]!!.add(value)
                }
              }
              if (value.key.isAssertionDependency) {
                value.dependentVariableAssertions.forEach {
                  if (!dependentAssertions.containsKey(it))
                    dependentAssertions[it] = mutableSetOf(value)
                  else
                    dependentAssertions[it]!!.add(value)
                }
              }
            }
          }
          else -> throw CustomJsonException("{${value.key.name}: 'Unable to compute formula value'}")
        }
        dependentFormulaValues.remove(value)
      }
    }
    variableUniquenesses.forEach { variableUniqueness ->
      var uniqueness = variableUniqueness
      while (uniqueness.nextVariableUniqueness != null) {
        uniqueness.nextVariableUniqueness!!.level = uniqueness.level
        uniqueness = uniqueness.nextVariableUniqueness!!
        try {
          variableUniquenessJpaRepository.save(uniqueness)
        } catch (exception: Exception) {
          throw CustomJsonException("{variableName: 'Unable to delete referenced variable'}")
        }
      }
      var input = ""
      variableUniqueness.typeUniqueness.keyUniquenessConstraints.sortedBy { it.id }.forEach { keyUniqueness ->
        val value: Value = variable.values.single { it.key == keyUniqueness.key }
        input += computeHash(
          when (value.key.type.name) {
            TypeConstants.TEXT -> value.stringValue!!
            TypeConstants.NUMBER -> value.longValue.toString()
            TypeConstants.DECIMAL -> value.decimalValue.toString()
            TypeConstants.BOOLEAN -> value.booleanValue.toString()
            TypeConstants.TIMESTAMP -> value.timestampValue.toString()
            TypeConstants.DATE -> value.dateValue.toString()
            TypeConstants.TIME -> value.timeValue.toString()
            TypeConstants.BLOB -> value.blobValue.toString()
            TypeConstants.FORMULA -> when (value.key.formula!!.returnType.name) {
              TypeConstants.TEXT -> value.stringValue!!
              TypeConstants.NUMBER -> value.longValue.toString()
              TypeConstants.DECIMAL -> value.decimalValue.toString()
              TypeConstants.BOOLEAN -> value.booleanValue.toString()
              else -> throw CustomJsonException("{}")
            }
            else -> value.referencedVariable!!.toString()
          }
        )
      }
      val computedHash: String = computeHash(input)
      try {
        variableUniquenessJpaRepository.save(variableUniqueness.apply {
          level = 0
          hash = computedHash
        })
      } catch (exception: Exception) {
        resolveHashConflict(variable = variable, typeUniqueness = variableUniqueness.typeUniqueness, hash = computedHash,
          updatedVariableUniqueness = variableUniqueness.apply {
            nextVariableUniqueness = null
        })
      }
    }
    if (jsonParams.has("updatedVariableName?")) {
      variable.name = jsonParams.get("updatedVariableName?").asString
      if (variable.type.isFormulaDependency) {
        variable.dependentValues.forEach { formulaValue ->
          if (!dependentFormulaValues.containsKey(formulaValue))
            dependentFormulaValues[formulaValue] = mutableSetOf()
        }
      }
      if (variable.type.isAssertionDependency) {
        variable.variableAssertions.forEach { variableAssertion ->
          if (!dependentAssertions.containsKey(variableAssertion))
            dependentAssertions[variableAssertion] = mutableSetOf()
        }
      }
    }
    try {
      variableJpaRepository.save(variable)
    } catch (exception: Exception) {
      throw CustomJsonException("{variableName: 'Variable could not be updated'}")
    }
    if (dependentFormulaValues.isNotEmpty())
      recomputeDependentFormulaValues(dependentFormulaValues, dependentAssertions)
    else if (dependentAssertions.isNotEmpty())
      evaluateAssertions(dependentAssertions)
    return Quadruple(variable, typePermission, dependentFormulaValues, dependentAssertions)
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun recomputeDependentFormulaValues(
    dependentFormulaValues: MutableMap<Value, MutableSet<Value>>,
    dependentAssertions: Map<VariableAssertion, MutableSet<Value>>
  ) {
    val higherDependentFormulaValues: MutableMap<Value, MutableSet<Value>> = mutableMapOf()
    dependentFormulaValues.forEach { (value, dependencies) ->
      when (value.key.formula!!.returnType.name) {
        TypeConstants.TEXT -> {
          val reconstructDependencies: Boolean =
            dependencies.fold(false) { acc, v -> acc || v.key.isVariableDependency }
          if (reconstructDependencies) {
            val valueDependencies: MutableSet<Value> = mutableSetOf()
            val variableDependencies: MutableSet<Variable> = mutableSetOf()
            value.stringValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(
              value.key.formula!!.expression,
              JsonObject::class.java
            ).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
              symbols = getSymbolValuesAndUpdateDependencies(
                variable = value.variable,
                symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }
                  .toMutableSet(),
                valueDependencies = valueDependencies,
                variableDependencies = variableDependencies
              ), mode = "evaluate"
            ) as String
            value.valueDependencies = valueDependencies
            valueJpaRepository.save(value)
            if (value.key.isFormulaDependency) {
              value.dependentValues.forEach {
                if (!higherDependentFormulaValues.containsKey(it))
                  higherDependentFormulaValues[it] = mutableSetOf(value)
                else
                  higherDependentFormulaValues[it]!!.add(value)
              }
            }
          } else {
            val evaluatedValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(
              value.key.formula!!.expression,
              JsonObject::class.java
            ).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
              symbols = getSymbolValues(
                variable = value.variable,
                symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }
                  .toMutableSet()
              ), mode = "evaluate"
            ) as String
            if (value.stringValue != evaluatedValue) {
              value.stringValue = evaluatedValue
              if (value.key.isFormulaDependency) {
                value.dependentValues.forEach {
                  if (!higherDependentFormulaValues.containsKey(it))
                    higherDependentFormulaValues[it] = mutableSetOf(value)
                  else
                    higherDependentFormulaValues[it]!!.add(value)
                }
              }
            }
          }
        }
        TypeConstants.NUMBER -> {
          val reconstructDependencies: Boolean =
            dependencies.fold(false) { acc, v -> acc || v.key.isVariableDependency }
          if (reconstructDependencies) {
            val valueDependencies: MutableSet<Value> = mutableSetOf()
            val variableDependencies: MutableSet<Variable> = mutableSetOf()
            value.longValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(
              value.key.formula!!.expression,
              JsonObject::class.java
            ).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
              symbols = getSymbolValuesAndUpdateDependencies(
                variable = value.variable,
                symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }
                  .toMutableSet(),
                valueDependencies = valueDependencies,
                variableDependencies = variableDependencies
              ), mode = "evaluate"
            ) as Long
            value.valueDependencies = valueDependencies
            valueJpaRepository.save(value)
            if (value.key.isFormulaDependency) {
              value.dependentValues.forEach {
                if (!higherDependentFormulaValues.containsKey(it))
                  higherDependentFormulaValues[it] = mutableSetOf(value)
                else
                  higherDependentFormulaValues[it]!!.add(value)
              }
            }
          } else {
            val evaluatedValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(
              value.key.formula!!.expression,
              JsonObject::class.java
            ).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
              symbols = getSymbolValues(
                variable = value.variable,
                symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }
                  .toMutableSet()
              ), mode = "evaluate"
            ) as Long
            if (value.longValue != evaluatedValue) {
              value.longValue = evaluatedValue
              if (value.key.isFormulaDependency) {
                value.dependentValues.forEach {
                  if (!higherDependentFormulaValues.containsKey(it))
                    higherDependentFormulaValues[it] = mutableSetOf(value)
                  else
                    higherDependentFormulaValues[it]!!.add(value)
                }
              }
            }
          }
        }
        TypeConstants.DECIMAL -> {
          val reconstructDependencies: Boolean =
            dependencies.fold(false) { acc, v -> acc || v.key.isVariableDependency }
          if (reconstructDependencies) {
            val valueDependencies: MutableSet<Value> = mutableSetOf()
            val variableDependencies: MutableSet<Variable> = mutableSetOf()
            value.decimalValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(
              value.key.formula!!.expression,
              JsonObject::class.java
            ).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
              symbols = getSymbolValuesAndUpdateDependencies(
                variable = value.variable,
                symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }
                  .toMutableSet(),
                valueDependencies = valueDependencies,
                variableDependencies = variableDependencies
              ), mode = "evaluate"
            ) as BigDecimal
            value.valueDependencies = valueDependencies
            valueJpaRepository.save(value)
            if (value.key.isFormulaDependency) {
              value.dependentValues.forEach {
                if (!higherDependentFormulaValues.containsKey(it))
                  higherDependentFormulaValues[it] = mutableSetOf(value)
                else
                  higherDependentFormulaValues[it]!!.add(value)
              }
            }
          } else {
            val evaluatedValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(
              value.key.formula!!.expression,
              JsonObject::class.java
            ).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
              symbols = getSymbolValues(
                variable = value.variable,
                symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }
                  .toMutableSet()
              ), mode = "evaluate"
            ) as BigDecimal
            if (value.decimalValue != evaluatedValue) {
              value.decimalValue = evaluatedValue
              if (value.key.isFormulaDependency) {
                value.dependentValues.forEach {
                  if (!higherDependentFormulaValues.containsKey(it))
                    higherDependentFormulaValues[it] = mutableSetOf(value)
                  else
                    higherDependentFormulaValues[it]!!.add(value)
                }
              }
            }
          }
        }
        TypeConstants.BOOLEAN -> {
          val reconstructDependencies: Boolean =
            dependencies.fold(false) { acc, v -> acc || v.key.isVariableDependency }
          if (reconstructDependencies) {
            val valueDependencies: MutableSet<Value> = mutableSetOf()
            val variableDependencies: MutableSet<Variable> = mutableSetOf()
            value.booleanValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(
              value.key.formula!!.expression,
              JsonObject::class.java
            ).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
              symbols = getSymbolValuesAndUpdateDependencies(
                variable = value.variable,
                symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }
                  .toMutableSet(),
                valueDependencies = valueDependencies,
                variableDependencies = variableDependencies
              ), mode = "evaluate"
            ) as Boolean
            value.valueDependencies = valueDependencies
            valueJpaRepository.save(value)
            if (value.key.isFormulaDependency) {
              value.dependentValues.forEach {
                if (!higherDependentFormulaValues.containsKey(it))
                  higherDependentFormulaValues[it] = mutableSetOf(value)
                else
                  higherDependentFormulaValues[it]!!.add(value)
              }
            }
          } else {
            val evaluatedValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(
              value.key.formula!!.expression,
              JsonObject::class.java
            ).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
              symbols = getSymbolValues(
                variable = value.variable,
                symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }
                  .toMutableSet()
              ), mode = "evaluate"
            ) as Boolean
            if (value.booleanValue != evaluatedValue) {
              value.booleanValue = evaluatedValue
              if (value.key.isFormulaDependency) {
                value.dependentValues.forEach {
                  if (!higherDependentFormulaValues.containsKey(it))
                    higherDependentFormulaValues[it] = mutableSetOf(value)
                  else
                    higherDependentFormulaValues[it]!!.add(value)
                }
              }
            }
          }
        }
        else -> throw CustomJsonException("{error: 'Unable to compute dependent formula value'}")
      }
    }
    valueJpaRepository.saveAll(dependentFormulaValues.keys)
    if (higherDependentFormulaValues.isNotEmpty())
      recomputeDependentFormulaValues(higherDependentFormulaValues, dependentAssertions)
    else if (dependentAssertions.isNotEmpty())
      evaluateAssertions(dependentAssertions)
  }

  fun evaluateAssertions(dependentAssertions: Map<VariableAssertion, MutableSet<Value>>) {
    for ((variableAssertion, _) in dependentAssertions) {
      val valueDependencies: MutableSet<Value> = mutableSetOf()
      val variableDependencies: MutableSet<Variable> = mutableSetOf()
      variableAssertion.result = validateOrEvaluateExpression(
        jsonParams = gson.fromJson(
          variableAssertion.typeAssertion.expression,
          JsonObject::class.java
        ).apply { addProperty("expectedReturnType", TypeConstants.BOOLEAN) },
        symbols = getSymbolValuesAndUpdateDependencies(
          variable = variableAssertion.variable,
          symbolPaths = gson.fromJson(variableAssertion.typeAssertion.symbolPaths, JsonArray::class.java)
            .map { it.asString }.toMutableSet(),
          valueDependencies = valueDependencies,
          variableDependencies = variableDependencies,
          symbolsForFormula = false
        ), mode = "evaluate"
      ) as Boolean
      if (!variableAssertion.result)
        throw CustomJsonException("{variableName: 'Failed to assert ${variableAssertion.variable.type.name}:${variableAssertion.typeAssertion.name}'}")
      try {
        variableAssertion.valueDependencies = valueDependencies
        variableAssertion.variableDependencies = variableDependencies
        variableAssertionJpaRepository.save(variableAssertion)
      } catch (exception: Exception) {
        throw CustomJsonException("{variableName: 'Variable could not be updated'}")
      }
    }
  }

  fun deleteVariable(jsonParams: JsonObject): Pair<Variable, TypePermission> {
    val type: Type
    val typePermission: TypePermission = userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
      addProperty("orgId", jsonParams.get("orgId").asString)
      addProperty("username", jsonParams.get("username").asString)
      addProperty("typeName", jsonParams.get("typeName").asString)
    })
    type = typePermission.type
    val variable: Variable = variableRepository.findVariable(
      organizationId = type.organization.id,
      typeName = type.name,
      name = jsonParams.get("variableName").asString
    )
      ?: throw CustomJsonException("{variableName: 'Unable to find referenced variable'}")
    if (!typePermission.deletable)
      throw CustomJsonException("{variableName: 'Unable to delete referenced variable'}")
    variable.variableUniquenesses.forEach { variableUniqueness ->
      var uniqueness = variableUniqueness
      while (uniqueness.nextVariableUniqueness != null) {
        uniqueness.nextVariableUniqueness!!.level = uniqueness.level
        uniqueness = uniqueness.nextVariableUniqueness!!
        try {
          variableUniquenessJpaRepository.save(uniqueness)
        } catch (exception: Exception) {
          throw CustomJsonException("{variableName: 'Unable to delete referenced variable'}")
        }
      }
    }
    return try {
      variableJpaRepository.delete(variable)
      Pair(variable, typePermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{variableName: 'Unable to delete referenced variable'}")
    }
  }

  fun resolveHashConflict(variable: Variable, typeUniqueness: TypeUniqueness, hash: String, updatedVariableUniqueness: VariableUniqueness? = null): VariableUniqueness {
    var conflictedVariableUniqueness: VariableUniqueness =
      variableUniquenessRepository.findVariableUniqueness(typeUniqueness = typeUniqueness, hash = hash)
        ?: throw CustomJsonException("{variableName: 'Variable could not be created'}")
    var conflictedVariable: Variable = conflictedVariableUniqueness.variable
    val conflict: Boolean = typeUniqueness.keyUniquenessConstraints.fold(true) { acc, keyUniqueness ->
      val value: Value = variable.values.single { it.key == keyUniqueness.key }
      val conflictedValue: Value = conflictedVariable.values.single { it.key == keyUniqueness.key }
      acc && when (value.key.type.name) {
        TypeConstants.TEXT -> value.stringValue == conflictedValue.stringValue
        TypeConstants.NUMBER -> value.longValue == conflictedValue.longValue
        TypeConstants.DECIMAL -> value.decimalValue == conflictedValue.decimalValue
        TypeConstants.BOOLEAN -> value.booleanValue == conflictedValue.booleanValue
        TypeConstants.TIMESTAMP -> value.timestampValue == conflictedValue.timestampValue
        TypeConstants.DATE -> value.dateValue == conflictedValue.dateValue
        TypeConstants.TIME -> value.timeValue == conflictedValue.timeValue
        TypeConstants.BLOB -> value.toString() == conflictedValue.toString()
        TypeConstants.FORMULA -> when (value.key.formula!!.returnType.name) {
          TypeConstants.TEXT -> value.stringValue == conflictedValue.stringValue
          TypeConstants.NUMBER -> value.longValue == conflictedValue.longValue
          TypeConstants.DECIMAL -> value.decimalValue == conflictedValue.decimalValue
          TypeConstants.BOOLEAN -> value.booleanValue == conflictedValue.booleanValue
          else -> throw CustomJsonException("{}")
        }
        else -> value.referencedVariable!! == conflictedValue.referencedVariable!!
      }
    }
    if (conflict)
      throw CustomJsonException("{variableName: 'Unique Constraint violation'}")
    else {
      var level = 0
      while (conflictedVariableUniqueness.nextVariableUniqueness != null) {
        conflictedVariableUniqueness = conflictedVariableUniqueness.nextVariableUniqueness!!
        conflictedVariable = conflictedVariableUniqueness.variable
        level = conflictedVariableUniqueness.level
      }
      return try {
        val variableUniqueness: VariableUniqueness = updatedVariableUniqueness?.apply {
          level += 1
          this.hash = hash
        }
          ?: variableUniquenessJpaRepository.save(
            VariableUniqueness(
              variable = variable,
              typeUniqueness = typeUniqueness,
              level = level + 1,
              hash = hash
            )
          )
        variableUniquenessJpaRepository.save(conflictedVariableUniqueness.apply {
          nextVariableUniqueness = variableUniqueness
        })
        variableUniqueness
      } catch (exception: Exception) {
        throw CustomJsonException("{variableName: 'Variable could not be created'}")
      }
    }
  }

  fun serialize(variable: Variable, typePermission: TypePermission, username: String): JsonObject {
    val json = JsonObject()
    json.addProperty("organization", variable.type.organization.id)
    json.addProperty("typeName", variable.type.name)
    json.addProperty("active", variable.active)
    json.addProperty("variableName", variable.name)
    val jsonValues = JsonObject()
    for (value in variable.values) {
      when (value.key.type.name) {
        TypeConstants.TEXT -> {
          if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
            jsonValues.addProperty(value.key.name, value.stringValue!!)
        }
        TypeConstants.NUMBER -> {
          if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
            jsonValues.addProperty(value.key.name, value.longValue!!)
        }
        TypeConstants.DECIMAL -> {
          if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
            jsonValues.addProperty(value.key.name, value.decimalValue!!)
        }
        TypeConstants.BOOLEAN -> {
          if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
            jsonValues.addProperty(value.key.name, value.booleanValue!!)
        }
        TypeConstants.DATE -> {
          if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
            jsonValues.addProperty(value.key.name, value.dateValue!!.toString())
        }
        TypeConstants.TIME -> {
          if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
            jsonValues.addProperty(value.key.name, value.timeValue!!.toString())
        }
        TypeConstants.TIMESTAMP -> {
          if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
            jsonValues.addProperty(value.key.name, value.timestampValue!!.toString())
        }
        TypeConstants.BLOB -> {
          if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
            jsonValues.addProperty(value.key.name, value.blobValue!!.toString())
        }
        TypeConstants.FORMULA -> {
          if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS) {
            when (value.key.formula!!.returnType.name) {
              TypeConstants.TEXT -> jsonValues.addProperty(value.key.name, value.stringValue!!)
              TypeConstants.NUMBER -> jsonValues.addProperty(value.key.name, value.longValue!!)
              TypeConstants.DECIMAL -> jsonValues.addProperty(value.key.name, value.decimalValue!!)
              TypeConstants.BOOLEAN -> jsonValues.addProperty(value.key.name, value.booleanValue!!)
            }
          }
        }
        else -> if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS) {
          jsonValues.addProperty(value.key.name, value.referencedVariable!!.name)
        }
      }
    }
    json.add("values", jsonValues)
    return json
  }

  fun serialize(variables: Set<Variable>, typePermission: TypePermission, username: String): JsonArray {
    val json = JsonArray()
    for (variable in variables)
      json.add(serialize(variable = variable, typePermission = typePermission, username = username))
    return json
  }
}
