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
import com.pibity.erp.entities.assertion.VariableAssertion
import com.pibity.erp.entities.permission.TypePermission
import com.pibity.erp.entities.uniqueness.TypeUniqueness
import com.pibity.erp.entities.uniqueness.VariableUniqueness
import com.pibity.erp.repositories.jpa.ValueJpaRepository
import com.pibity.erp.repositories.jpa.VariableAssertionJpaRepository
import com.pibity.erp.repositories.jpa.VariableJpaRepository
import com.pibity.erp.repositories.jpa.VariableUniquenessJpaRepository
import com.pibity.erp.repositories.query.VariableRepository
import com.pibity.erp.repositories.query.VariableUniquenessRepository
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
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

  fun executeQueue(jsonParams: JsonObject, files: List<MultipartFile>, defaultTimestamp: Timestamp = Timestamp(System.currentTimeMillis())): JsonObject {
    return jsonParams.get(VariableConstants.QUEUE).asJsonObject.entrySet().fold(JsonObject()) { acc, (queueName, queue) ->
      acc.apply {
        try {
          add(queueName, queue.asJsonArray.foldIndexed(JsonArray()) { index, acc1, variableJson ->
            acc1.apply {
              try {
                add(mutateVariablesAtomically(variableJson = variableJson.asJsonObject,
                  orgId = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asLong,
                  username = jsonParams.get(OrganizationConstants.USERNAME).asString,
                  defaultTimestamp = defaultTimestamp,
                  files = files))
              } catch (exception: Exception) {
                if (exception is CustomJsonException)
                  throw CustomJsonException(gson.fromJson(exception.message, JsonObject::class.java).apply { addProperty(VariableConstants.STEP, index) }.toString())
                else
                  throw CustomJsonException("{${VariableConstants.STEP}: $index")
              }
            }
          })
        } catch (exception: Exception) {
          if (exception is CustomJsonException)
            add(queueName, gson.fromJson(exception.message, JsonObject::class.java))
          else
            addProperty(queueName, MessageConstants.UNEXPECTED_VALUE)
        }
      }
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun mutateVariablesAtomically(variableJson: JsonObject, orgId: Long, username: String, files: List<MultipartFile>, defaultTimestamp: Timestamp): JsonObject {
    val (variable, typePermission) =  when (variableJson.get(VariableConstants.OPERATION).asString) {
      VariableConstants.UPDATE -> updateVariable(jsonParams = variableJson.apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, orgId)
        addProperty(OrganizationConstants.USERNAME, username)
      }, defaultTimestamp = defaultTimestamp, files = files)
      VariableConstants.CREATE -> createVariable(jsonParams = variableJson.apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, orgId)
        addProperty(OrganizationConstants.USERNAME, username)
      }, defaultTimestamp = defaultTimestamp, files = files)
      VariableConstants.DELETE -> deleteVariable(jsonParams = variableJson.apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, orgId)
        addProperty(OrganizationConstants.USERNAME, username)
      })
      else -> throw CustomJsonException("{${VariableConstants.OPERATION}: ${MessageConstants.UNEXPECTED_VALUE}}")
    }
    return serialize(variable = variable, typePermission = typePermission)
  }

  @Suppress("UNCHECKED_CAST")
  fun createVariable(jsonParams: JsonObject, defaultTimestamp: Timestamp, files: List<MultipartFile>): Pair<Variable, TypePermission> {
    val typePermission: TypePermission = userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
        addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
        addProperty(OrganizationConstants.TYPE_NAME, jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
      })
    return if (!typePermission.creatable)
      throw CustomJsonException("{${OrganizationConstants.ERROR}: ${MessageConstants.UNAUTHORIZED_ACCESS}}")
    else {
      val valuesJson: JsonObject = validateVariableValues(values = jsonParams.get(VariableConstants.VALUES).asJsonObject, typePermission = typePermission, defaultTimestamp = defaultTimestamp, files = files)
      val variable: Variable = try {
        variableJpaRepository.save(Variable(type = typePermission.type, name = jsonParams.get(VariableConstants.VARIABLE_NAME).asString))
      } catch (exception: Exception) {
        throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: ${MessageConstants.VARIABLE_NOT_SAVED}")
      }
      variable.values.addAll(typePermission.type.keys.filter { it.type.name != TypeConstants.FORMULA }
        .sortedBy { it.keyOrder }
        .map { key ->
        valueJpaRepository.save(when(key.type.name) {
          TypeConstants.TEXT -> Value(variable = variable, key = key, stringValue = valuesJson.get(key.name).asString!!)
          TypeConstants.NUMBER -> Value(variable = variable, key = key, longValue = valuesJson.get(key.name).asLong)
          TypeConstants.DECIMAL -> Value(variable = variable, key = key, decimalValue = valuesJson.get(key.name).asBigDecimal!!)
          TypeConstants.BOOLEAN -> Value(variable = variable, key = key, booleanValue = valuesJson.get(key.name).asBoolean)
          TypeConstants.DATE -> Value(variable = variable, key = key, dateValue = Date(valuesJson.get(key.name).asLong))
          TypeConstants.TIMESTAMP -> Value(variable = variable, key = key, timestampValue = Timestamp(valuesJson.get(key.name).asLong))
          TypeConstants.TIME -> Value(variable = variable, key = key, timeValue = Time(valuesJson.get(key.name).asLong))
          TypeConstants.BLOB -> if (valuesJson.has(key.name))
            Value(variable = variable, key = key, blobValue = BlobProxy.generateProxy(files[valuesJson.get(key.name).asInt].bytes))
          else
            Value(variable = variable, key = key, blobValue = key.defaultBlobValue!!)
          TypeConstants.FORMULA -> throw CustomJsonException("{}")
          else -> Value(variable = variable, key = key, referencedVariable = (variableRepository.findByTypeAndName(type = key.type, name = valuesJson.get(key.name).asString!!)
            ?: throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")).apply {
            if (!active)
              throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
          })
        })
      })
      variable.values.addAll(typePermission.type.keys.filter { it.type.name == TypeConstants.FORMULA }
        .sortedBy { it.keyOrder }
        .map { key ->
          val valueDependencies: MutableSet<Value> = mutableSetOf()
          val symbols: JsonObject = getSymbolValues(variable = variable, symbolPaths = gson.fromJson(key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(),
            valueDependencies = valueDependencies, symbolsForFormula = true)
          val evaluatedArg = validateOrEvaluateExpression(expression = gson.fromJson(key.formula!!.expression, JsonObject::class.java),
            symbols = symbols, mode = LispConstants.EVALUATE, expectedReturnType = key.formula!!.returnType.name)
          valueJpaRepository.save(when(key.formula!!.returnType.name) {
            TypeConstants.TEXT -> Value(variable = variable, key = key, stringValue = evaluatedArg as String, valueDependencies = valueDependencies)
            TypeConstants.NUMBER -> Value(variable = variable, key = key, longValue = evaluatedArg as Long, valueDependencies = valueDependencies)
            TypeConstants.DECIMAL -> Value(variable = variable, key = key, decimalValue = evaluatedArg as BigDecimal, valueDependencies = valueDependencies)
            TypeConstants.BOOLEAN -> Value(variable = variable, key = key, booleanValue = evaluatedArg as Boolean, valueDependencies = valueDependencies)
            TypeConstants.DATE -> Value(variable = variable, key = key, dateValue = evaluatedArg as Date, valueDependencies = valueDependencies)
            TypeConstants.TIMESTAMP -> Value(variable = variable, key = key, timestampValue = evaluatedArg as Timestamp, valueDependencies = valueDependencies)
            TypeConstants.TIME -> Value(variable = variable, key = key, timeValue = evaluatedArg as Time, valueDependencies = valueDependencies)
            TypeConstants.BLOB -> Value(variable = variable, key = key, blobValue = BlobProxy.generateProxy(evaluatedArg as ByteArray), valueDependencies = valueDependencies)
            else -> throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
          })
      })
      variable.variableUniquenesses.addAll(variable.type.uniqueConstraints.map { typeUniqueness ->
        val computedHash: String = computeHash(typeUniqueness.keys.sortedBy { it.id }.fold("") { acc, key ->
          val value: Value = variable.values.single { it.key == key }
          acc + when (value.key.type.name) {
            TypeConstants.TEXT -> value.stringValue!!
            TypeConstants.NUMBER -> value.longValue.toString()
            TypeConstants.DECIMAL -> value.decimalValue.toString()
            TypeConstants.BOOLEAN -> value.booleanValue.toString()
            TypeConstants.DATE -> value.dateValue.toString()
            TypeConstants.TIMESTAMP -> value.timestampValue.toString()
            TypeConstants.TIME -> value.timeValue.toString()
            TypeConstants.BLOB -> Base64.getEncoder().encodeToString(value.blobValue!!.getBytes(1, value.blobValue!!.length().toInt()))
            TypeConstants.FORMULA -> when (value.key.formula!!.returnType.name) {
              TypeConstants.TEXT -> value.stringValue!!
              TypeConstants.NUMBER -> value.longValue.toString()
              TypeConstants.DECIMAL -> value.decimalValue.toString()
              TypeConstants.BOOLEAN -> value.booleanValue.toString()
              TypeConstants.DATE -> value.dateValue.toString()
              TypeConstants.TIMESTAMP -> value.timestampValue.toString()
              TypeConstants.TIME -> value.timeValue.toString()
              TypeConstants.BLOB -> Base64.getEncoder().encodeToString(value.blobValue!!.getBytes(1, value.blobValue!!.length().toInt()))
              else -> throw CustomJsonException("{}")
            }
            else -> value.referencedVariable!!.id
          }
        })
        try {
          variableUniquenessJpaRepository.save(VariableUniqueness(typeUniqueness = typeUniqueness, variable = variable, hash = computedHash))
        } catch (exception: Exception) {
          resolveHashConflict(variableUniqueness = VariableUniqueness(typeUniqueness = typeUniqueness, variable = variable, hash = computedHash), variableUniquenessToUpdate = setOf())
        }
      })
      variable.variableAssertions.addAll(variable.type.typeAssertions.map { typeAssertion ->
        val valueDependencies: MutableSet<Value> = mutableSetOf()
        val symbols: JsonObject = getSymbolValues(
          variable = variable,
          symbolPaths = gson.fromJson(typeAssertion.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(),
          valueDependencies = valueDependencies,
          symbolsForFormula = false
        )
        val result: Boolean = validateOrEvaluateExpression(expression = gson.fromJson(typeAssertion.expression, JsonObject::class.java),
          symbols = symbols, mode = LispConstants.EVALUATE, expectedReturnType = TypeConstants.BOOLEAN) as Boolean
        if (!result)
          throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: 'Failed to assert ${variable.type.name}:${typeAssertion.name}'}")
        else
          variableAssertionJpaRepository.save(VariableAssertion(typeAssertion = typeAssertion, variable = variable, valueDependencies = valueDependencies))
      })
      Pair(variable, typePermission)
    }
  }

  @Suppress("UNCHECKED_CAST")
  fun updateVariable(jsonParams: JsonObject, defaultTimestamp: Timestamp, files: List<MultipartFile>): Pair<Variable, TypePermission> {
    val typePermission: TypePermission = userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
        addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
        addProperty(OrganizationConstants.TYPE_NAME, jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
      })
    val variable: Variable = (variableRepository.findByTypeAndName(type = typePermission.type, name = jsonParams.get(VariableConstants.VARIABLE_NAME).asString)
      ?: throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.VARIABLE_NOT_FOUND}}")).apply {
      if (jsonParams.has("${VariableConstants.ACTIVE}?"))
        active = jsonParams.get("${VariableConstants.ACTIVE}?").asBoolean
    }
    val valuesJson: JsonObject = validateUpdatedVariableValues(values = jsonParams.get(VariableConstants.VALUES).asJsonObject, typePermission = typePermission, defaultTimestamp = defaultTimestamp, files = files)
    val dependentUniqueness: MutableSet<VariableUniqueness> = mutableSetOf()
    val dependentFormulaValues: MutableSet<Value> = mutableSetOf()
    val dependentAssertions: MutableSet<VariableAssertion> = mutableSetOf()
    variable.values.filter { it.key.type.name != TypeConstants.FORMULA }.forEach { value ->
      if (valuesJson.has(value.key.name)) {
        when(value.key.type.name) {
          TypeConstants.TEXT -> value.stringValue = valuesJson.get(value.key.name).asString!!
          TypeConstants.NUMBER -> value.longValue = valuesJson.get(value.key.name).asLong
          TypeConstants.DECIMAL -> value.decimalValue = valuesJson.get(value.key.name).asBigDecimal!!
          TypeConstants.BOOLEAN -> value.booleanValue = valuesJson.get(value.key.name).asBoolean
          TypeConstants.DATE -> value.dateValue = Date(valuesJson.get(value.key.name).asLong)
          TypeConstants.TIMESTAMP -> value.timestampValue = Timestamp(valuesJson.get(value.key.name).asLong)
          TypeConstants.TIME -> value.timeValue = Time(valuesJson.get(value.key.name).asLong)
          TypeConstants.BLOB -> value.blobValue = BlobProxy.generateProxy(files[valuesJson.get(value.key.name).asInt].bytes)
          TypeConstants.FORMULA -> throw CustomJsonException("{}")
          else -> {
            value.referencedVariable = (variableRepository.findByTypeAndName(type = typePermission.type, name = jsonParams.get(VariableConstants.VARIABLE_NAME).asString)
              ?: throw CustomJsonException("{${VariableConstants.VALUES}: {${value.key.name}: ${MessageConstants.VARIABLE_NOT_FOUND}}}")).apply {
              if (!active)
                throw CustomJsonException("{${VariableConstants.VALUES}: {${value.key.name}: ${MessageConstants.VARIABLE_NOT_FOUND}}}")
            }
          }
        }
        if (value.key.isFormulaDependency)
          dependentFormulaValues.addAll(value.dependentValues)
        if (value.key.isAssertionDependency)
          dependentAssertions.addAll(value.dependentVariableAssertions)
        if (value.key.isUniquenessDependency)
          dependentUniqueness.addAll(value.key.dependentTypeUniquenesses.map { typeUniqueness -> variable.variableUniquenesses.single { it.typeUniqueness == typeUniqueness } })
      }
    }
    variable.values.filter { it.key.type.name == TypeConstants.FORMULA }.forEach { value ->
      if (dependentFormulaValues.contains(value)) {
        val updatedValueDependencies: MutableSet<Value> = mutableSetOf()
        val evaluatedValue = validateOrEvaluateExpression(expression = gson.fromJson(value.key.formula!!.expression, JsonObject::class.java),
          symbols = getSymbolValues(variable = variable,
            symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(),
            valueDependencies = updatedValueDependencies,
            symbolsForFormula = true
          ),
          mode = LispConstants.EVALUATE,
          expectedReturnType = value.key.formula!!.returnType.name
        )
        value.valueDependencies = updatedValueDependencies
        when(value.key.formula!!.returnType.name) {
          TypeConstants.TEXT -> value.stringValue = evaluatedValue as String
          TypeConstants.NUMBER -> value.longValue = evaluatedValue as Long
          TypeConstants.DECIMAL -> value.decimalValue = evaluatedValue as BigDecimal
          TypeConstants.BOOLEAN -> value.booleanValue = evaluatedValue as Boolean
          TypeConstants.DATE -> value.dateValue = evaluatedValue as Date
          TypeConstants.TIMESTAMP -> value.timestampValue = evaluatedValue as Timestamp
          TypeConstants.TIME -> value.timeValue = evaluatedValue as Time
          TypeConstants.BLOB -> value.blobValue = BlobProxy.generateProxy(evaluatedValue as ByteArray)
          else -> throw CustomJsonException("{}")
        }
        dependentFormulaValues.remove(value)
        if (value.key.isFormulaDependency)
          dependentFormulaValues.addAll(value.dependentValues)
        if (value.key.isAssertionDependency)
          dependentAssertions.addAll(value.dependentVariableAssertions)
        if (value.key.isUniquenessDependency)
          dependentUniqueness.addAll(value.key.dependentTypeUniquenesses.map { typeUniqueness -> variable.variableUniquenesses.single { it.typeUniqueness == typeUniqueness } })
      }
    }
    try {
      if (jsonParams.has("${VariableConstants.UPDATED_VARIABLE_NAME}?")) {
        variableJpaRepository.save(variable.apply {
          name = jsonParams.get("${VariableConstants.UPDATED_VARIABLE_NAME}?").asString
          referencingValues.forEach { referencingValue ->
            if (referencingValue.key.isFormulaDependency)
              dependentFormulaValues.addAll(referencingValue.dependentValues)
          }
          referencingValues.forEach { referencingValue ->
            if (referencingValue.key.isAssertionDependency)
              dependentAssertions.addAll(referencingValue.dependentVariableAssertions)
          }
        })
      }
    } catch (exception: Exception) {
      throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.VARIABLE_NOT_SAVED}}")
    }
    recomputeDependentFormulaValues(affectedFormulaValues = dependentFormulaValues, dependentAssertions = dependentAssertions, dependentUniqueness = dependentUniqueness)
    // Note: Assertions should be run at last when all affected values have been updated.
    // Otherwise it may lead to incorrect results if assertion depends on a value that is not yet updated.
    // Same applies to VariableUniqueness.
    evaluateAssertions(dependentAssertions = dependentAssertions)
    recomputeHashesForVariableUniqueness(dependentUniqueness = dependentUniqueness)
    return Pair(variable, typePermission)
  }

  fun recomputeHashesForVariableUniqueness(dependentUniqueness: Set<VariableUniqueness>) {
    dependentUniqueness.forEach { variableUniqueness ->
      variableUniqueness.hash = computeHash(variableUniqueness.typeUniqueness.keys.sortedBy { it.id }.fold("") { acc, key ->
        val value: Value = variableUniqueness.variable.values.single { it.key == key }
        acc + when (value.key.type.name) {
          TypeConstants.TEXT -> value.stringValue!!
          TypeConstants.NUMBER -> value.longValue.toString()
          TypeConstants.DECIMAL -> value.decimalValue.toString()
          TypeConstants.BOOLEAN -> value.booleanValue.toString()
          TypeConstants.DATE -> value.dateValue.toString()
          TypeConstants.TIMESTAMP -> value.timestampValue.toString()
          TypeConstants.TIME -> value.timeValue.toString()
          TypeConstants.BLOB -> Base64.getEncoder().encodeToString(value.blobValue!!.getBytes(1, value.blobValue!!.length().toInt()))
          TypeConstants.FORMULA -> when (value.key.formula!!.returnType.name) {
            TypeConstants.TEXT -> value.stringValue!!
            TypeConstants.NUMBER -> value.longValue.toString()
            TypeConstants.DECIMAL -> value.decimalValue.toString()
            TypeConstants.BOOLEAN -> value.booleanValue.toString()
            TypeConstants.DATE -> value.dateValue.toString()
            TypeConstants.TIMESTAMP -> value.timestampValue.toString()
            TypeConstants.TIME -> value.timeValue.toString()
            TypeConstants.BLOB -> Base64.getEncoder().encodeToString(value.blobValue!!.getBytes(1, value.blobValue!!.length().toInt()))
            else -> throw CustomJsonException("{}")
          }
          else -> value.referencedVariable!!.id
        }
      })
      val previousVariableUniqueness: VariableUniqueness? = if (variableUniqueness.level == 0) null
      else variableUniquenessRepository.findVariableUniqueness(typeUniqueness = variableUniqueness.typeUniqueness, level = variableUniqueness.level - 1, hash = variableUniqueness.hash)!!
      val variableUniquenessToUpdate: MutableSet<VariableUniqueness> = mutableSetOf()
      if (previousVariableUniqueness != null)
        variableUniquenessToUpdate.add(previousVariableUniqueness.apply { nextVariableUniqueness = variableUniqueness.nextVariableUniqueness })
      var nextVariableUniqueness: VariableUniqueness? = variableUniqueness.nextVariableUniqueness
      while (nextVariableUniqueness != null) {
        variableUniquenessToUpdate.add(nextVariableUniqueness!!.apply { level -= 1 })
        nextVariableUniqueness = nextVariableUniqueness!!.nextVariableUniqueness
      }
      try {
        variableUniquenessJpaRepository.saveAll(mutableSetOf<VariableUniqueness>().apply {
          addAll(variableUniquenessToUpdate)
          add(variableUniqueness.apply {
            level = 0
            nextVariableUniqueness = null
          })
        })
      } catch (exception: Exception) {
        resolveHashConflict(variableUniqueness = variableUniqueness, variableUniquenessToUpdate = variableUniquenessToUpdate)
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  fun recomputeDependentFormulaValues(affectedFormulaValues: Set<Value>, dependentAssertions: MutableSet<VariableAssertion>, dependentUniqueness: MutableSet<VariableUniqueness>) {
    val dependentFormulaValues: MutableSet<Value> = mutableSetOf()
    affectedFormulaValues.forEach { value ->
      val updatedValueDependencies: MutableSet<Value> = mutableSetOf()
      val evaluatedValue = validateOrEvaluateExpression(expression = gson.fromJson(value.key.formula!!.expression, JsonObject::class.java),
        symbols = getSymbolValues(variable = value.variable,
          symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(),
          valueDependencies = updatedValueDependencies,
          symbolsForFormula = true),
        mode = LispConstants.EVALUATE, expectedReturnType = value.key.formula!!.returnType.name)
      value.valueDependencies = updatedValueDependencies
      when(value.key.formula!!.returnType.name) {
        TypeConstants.TEXT -> value.stringValue = evaluatedValue as String
        TypeConstants.NUMBER -> value.longValue = evaluatedValue as Long
        TypeConstants.DECIMAL -> value.decimalValue = evaluatedValue as BigDecimal
        TypeConstants.BOOLEAN -> value.booleanValue = evaluatedValue as Boolean
        TypeConstants.DATE -> value.dateValue = evaluatedValue as Date
        TypeConstants.TIMESTAMP -> value.timestampValue = evaluatedValue as Timestamp
        TypeConstants.TIME -> value.timeValue = evaluatedValue as Time
        TypeConstants.BLOB -> value.blobValue = BlobProxy.generateProxy(evaluatedValue as ByteArray)
        else -> throw CustomJsonException("{}")
      }
      if (value.key.isFormulaDependency)
        dependentFormulaValues.addAll(value.dependentValues)
      if (value.key.isAssertionDependency)
        dependentAssertions.addAll(value.dependentVariableAssertions)
      if (value.key.isUniquenessDependency)
        dependentUniqueness.addAll(value.key.dependentTypeUniquenesses.map { typeUniqueness -> value.variable.variableUniquenesses.single { it.typeUniqueness == typeUniqueness } })
    }
    if (dependentFormulaValues.isNotEmpty())
      recomputeDependentFormulaValues(affectedFormulaValues = dependentFormulaValues, dependentAssertions = dependentAssertions, dependentUniqueness = dependentUniqueness)
  }

  @Suppress("UNCHECKED_CAST")
  fun evaluateAssertions(dependentAssertions: Set<VariableAssertion>) {
    for (variableAssertion in dependentAssertions) {
      val updatedValueDependencies: MutableSet<Value> = mutableSetOf()
      val result: Boolean = validateOrEvaluateExpression(
        expression = gson.fromJson(variableAssertion.typeAssertion.expression, JsonObject::class.java),
        symbols = getSymbolValues(variable = variableAssertion.variable,
          symbolPaths = gson.fromJson(variableAssertion.typeAssertion.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(),
          valueDependencies = updatedValueDependencies,
          symbolsForFormula = false),
        mode = LispConstants.EVALUATE,
        expectedReturnType = TypeConstants.BOOLEAN
      ) as Boolean
      if (!result)
        throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: 'Failed to assert ${variableAssertion.variable.type.name}:${variableAssertion.typeAssertion.name}'}")
      try {
        variableAssertionJpaRepository.save(variableAssertion.apply { valueDependencies = updatedValueDependencies })
      } catch (exception: Exception) {
        throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.VARIABLE_NOT_SAVED}")
      }
    }
  }

  fun deleteVariable(jsonParams: JsonObject): Pair<Variable, TypePermission> {
    val typePermission: TypePermission = userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
      addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
      addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
      addProperty(OrganizationConstants.TYPE_NAME, jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
    })
    return if (!typePermission.deletable)
      throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.VARIABLE_NOT_REMOVED}}")
    else {
      val variable: Variable = variableRepository.findByTypeAndName(type = typePermission.type, name = jsonParams.get(VariableConstants.VARIABLE_NAME).asString)
        ?: throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.VARIABLE_NOT_FOUND}}")
      variable.variableUniquenesses.forEach { variableUniqueness ->
        val previousVariableUniqueness: VariableUniqueness? = if (variableUniqueness.level == 0) null
        else variableUniquenessRepository.findVariableUniqueness(typeUniqueness = variableUniqueness.typeUniqueness, level = variableUniqueness.level - 1, hash = variableUniqueness.hash)!!
        val variableUniquenessToUpdate: MutableSet<VariableUniqueness> = mutableSetOf()
        if (previousVariableUniqueness != null)
          variableUniquenessToUpdate.add(previousVariableUniqueness.apply { nextVariableUniqueness = variableUniqueness.nextVariableUniqueness })
        var nextVariableUniqueness: VariableUniqueness? = variableUniqueness.nextVariableUniqueness
        while (nextVariableUniqueness != null) {
          variableUniquenessToUpdate.add(nextVariableUniqueness!!.apply { level -= 1 })
          nextVariableUniqueness = nextVariableUniqueness!!.nextVariableUniqueness
        }
        try {
          variableUniquenessJpaRepository.delete(variableUniquenessJpaRepository.saveAll(variableUniquenessToUpdate.apply {
            add(variableUniqueness.apply {
              level = if (nextVariableUniqueness != null) nextVariableUniqueness!!.level + 1 else level
              nextVariableUniqueness = null
            })
          }).single { it.id == variableUniqueness.id })
        } catch (exception: Exception) {
          throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.VARIABLE_NOT_REMOVED}}")
        }
      }
      Pair(variable, typePermission)
    }
  }

  fun isTrueConflict(typeUniqueness: TypeUniqueness, variable: Variable, conflictedVariable: Variable): Boolean {
    return typeUniqueness.keys.fold(true) { acc, key ->
      val value: Value = variable.values.single { it.key == key }
      val conflictedValue: Value = conflictedVariable.values.single { it.key == key }
      acc && when (value.key.type.name) {
        TypeConstants.TEXT -> value.stringValue == conflictedValue.stringValue
        TypeConstants.NUMBER -> value.longValue == conflictedValue.longValue
        TypeConstants.DECIMAL -> value.decimalValue == conflictedValue.decimalValue
        TypeConstants.BOOLEAN -> value.booleanValue == conflictedValue.booleanValue
        TypeConstants.DATE -> value.dateValue == conflictedValue.dateValue
        TypeConstants.TIMESTAMP -> value.timestampValue == conflictedValue.timestampValue
        TypeConstants.TIME -> value.timeValue == conflictedValue.timeValue
        TypeConstants.BLOB ->
          Base64.getEncoder().encodeToString(value.blobValue!!.getBytes(1, value.blobValue!!.length().toInt())) == Base64.getEncoder().encodeToString(conflictedValue.blobValue!!.getBytes(1, conflictedValue.blobValue!!.length().toInt()))
        TypeConstants.FORMULA -> when (value.key.formula!!.returnType.name) {
          TypeConstants.TEXT -> value.stringValue == conflictedValue.stringValue
          TypeConstants.NUMBER -> value.longValue == conflictedValue.longValue
          TypeConstants.DECIMAL -> value.decimalValue == conflictedValue.decimalValue
          TypeConstants.BOOLEAN -> value.booleanValue == conflictedValue.booleanValue
          TypeConstants.DATE -> value.dateValue == conflictedValue.dateValue
          TypeConstants.TIMESTAMP -> value.timestampValue == conflictedValue.timestampValue
          TypeConstants.TIME -> value.timeValue == conflictedValue.timeValue
          TypeConstants.BLOB ->
            Base64.getEncoder().encodeToString(value.blobValue!!.getBytes(1, value.blobValue!!.length().toInt())) == Base64.getEncoder().encodeToString(conflictedValue.blobValue!!.getBytes(1, conflictedValue.blobValue!!.length().toInt()))
          else -> throw CustomJsonException("{}")
        }
        else -> value.referencedVariable!! == conflictedValue.referencedVariable!!
      }
    }
  }

  fun resolveHashConflict(variableUniqueness: VariableUniqueness, variableUniquenessToUpdate: Set<VariableUniqueness>): VariableUniqueness {
    var conflictedVariableUniqueness: VariableUniqueness = variableUniquenessRepository.findVariableUniqueness(typeUniqueness = variableUniqueness.typeUniqueness, hash = variableUniqueness.hash, level = 0)
      ?: throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.VARIABLE_NOT_SAVED}}")
    if (isTrueConflict( typeUniqueness = variableUniqueness.typeUniqueness, variable = variableUniqueness.variable, conflictedVariable = conflictedVariableUniqueness.variable))
      throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.UNIQUE_CONSTRAINT_VIOLATION}}")
    while (conflictedVariableUniqueness.nextVariableUniqueness != null) {
      conflictedVariableUniqueness = conflictedVariableUniqueness.nextVariableUniqueness!!
      if (isTrueConflict( typeUniqueness = variableUniqueness.typeUniqueness, variable = variableUniqueness.variable, conflictedVariable = conflictedVariableUniqueness.variable))
        throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.UNIQUE_CONSTRAINT_VIOLATION}}")
    }
    variableUniqueness.level = conflictedVariableUniqueness.level + 1
    return try {
      variableUniquenessJpaRepository.saveAll(mutableSetOf<VariableUniqueness>().apply {
        addAll(variableUniquenessToUpdate)
        add(conflictedVariableUniqueness.apply { nextVariableUniqueness = variableUniqueness })
      }).single { it.id == conflictedVariableUniqueness.id }.nextVariableUniqueness!!
    } catch (exception : Exception) {
      throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.UNIQUE_CONSTRAINT_VIOLATION}}")
    }
  }

  fun serialize(variable: Variable, typePermission: TypePermission): JsonObject {
    return JsonObject().apply {
      addProperty("organization", variable.type.organization.id)
      addProperty(OrganizationConstants.TYPE_NAME, variable.type.name)
      addProperty(VariableConstants.VARIABLE_NAME, variable.name)
      addProperty(VariableConstants.ACTIVE, variable.active)
      add(VariableConstants.VALUES, variable.values.fold(JsonObject()) { acc, value ->
        acc.apply {
          when (value.key.type.name) {
            TypeConstants.TEXT -> if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
              addProperty(value.key.name, value.stringValue!!)
            TypeConstants.NUMBER -> if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
              addProperty(value.key.name, value.longValue!!)
            TypeConstants.DECIMAL -> if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
              addProperty(value.key.name, value.decimalValue!!)
            TypeConstants.BOOLEAN -> if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
              addProperty(value.key.name, value.booleanValue!!)
            TypeConstants.DATE -> if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
              addProperty(value.key.name, value.dateValue!!.time)
            TypeConstants.TIMESTAMP -> if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
              addProperty(value.key.name, value.timestampValue!!.time)
            TypeConstants.TIME -> if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
              addProperty(value.key.name, value.timeValue!!.time)
            TypeConstants.BLOB -> if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
              addProperty(value.key.name, Base64.getEncoder().encodeToString(value.blobValue!!.getBytes(1, value.blobValue!!.length().toInt())))
            TypeConstants.FORMULA -> if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS) {
              when (value.key.formula!!.returnType.name) {
                TypeConstants.TEXT -> addProperty(value.key.name, value.stringValue!!)
                TypeConstants.NUMBER -> addProperty(value.key.name, value.longValue!!)
                TypeConstants.DECIMAL -> addProperty(value.key.name, value.decimalValue!!)
                TypeConstants.BOOLEAN -> addProperty(value.key.name, value.booleanValue!!)
                TypeConstants.DATE -> addProperty(value.key.name, value.dateValue!!.time)
                TypeConstants.TIMESTAMP -> addProperty(value.key.name, value.timestampValue!!.time)
                TypeConstants.TIME -> addProperty(value.key.name, value.timeValue!!.time)
                TypeConstants.BLOB -> addProperty(value.key.name, Base64.getEncoder().encodeToString(value.blobValue!!.getBytes(1, value.blobValue!!.length().toInt())))
              }
            }
            else -> if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
              addProperty(value.key.name, value.referencedVariable!!.name)
          }
        }
      })
    }
  }

  fun serialize(variables: Set<Variable>, typePermission: TypePermission): JsonArray {
    return variables.fold(JsonArray()) { acc, variable ->
      acc.apply { add(serialize(variable = variable, typePermission = typePermission)) }
    }
  }
}
