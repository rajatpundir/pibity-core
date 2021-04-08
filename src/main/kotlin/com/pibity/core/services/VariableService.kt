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
import com.pibity.core.commons.constants.*
import com.pibity.core.commons.CustomJsonException
import com.pibity.core.entities.*
import com.pibity.core.entities.accumulator.TypeAccumulator
import com.pibity.core.entities.accumulator.ValueAccumulator
import com.pibity.core.entities.accumulator.VariableAccumulator
import com.pibity.core.entities.assertion.VariableAssertion
import com.pibity.core.entities.permission.TypePermission
import com.pibity.core.entities.uniqueness.TypeUniqueness
import com.pibity.core.entities.uniqueness.VariableUniqueness
import com.pibity.core.repositories.jpa.*
import com.pibity.core.repositories.query.VariableAccumulatorRepository
import com.pibity.core.repositories.query.VariableRepository
import com.pibity.core.repositories.query.VariableUniquenessRepository
import com.pibity.core.utils.*
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.sql.Blob
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
  val variableUniquenessRepository: VariableUniquenessRepository,
  val variableAccumulatorJpaRepository: VariableAccumulatorJpaRepository,
  val variableAccumulatorRepository: VariableAccumulatorRepository,
  val valueAccumulatorJpaRepository: ValueAccumulatorJpaRepository
) {

  fun executeQueue(jsonParams: JsonObject, files: List<MultipartFile>, defaultTimestamp: Timestamp): JsonObject {
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
      }, defaultTimestamp = defaultTimestamp)
      else -> throw CustomJsonException("{${VariableConstants.OPERATION}: ${MessageConstants.UNEXPECTED_VALUE}}")
    }
    return serialize(variable = variable, typePermission = typePermission)
  }

  @Suppress("UNCHECKED_CAST")
  fun createVariable(jsonParams: JsonObject, files: List<MultipartFile>, defaultTimestamp: Timestamp): Pair<Variable, TypePermission> {
    val typePermission: TypePermission = userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
        addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
        addProperty(OrganizationConstants.TYPE_NAME, jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
      }, defaultTimestamp = defaultTimestamp)
    return if (!typePermission.creatable)
      throw CustomJsonException("{${OrganizationConstants.ERROR}: ${MessageConstants.UNAUTHORIZED_ACCESS}}")
    else {
      val valuesJson: JsonObject = validateVariableValues(values = jsonParams.get(VariableConstants.VALUES).asJsonObject, typePermission = typePermission, defaultTimestamp = defaultTimestamp, files = files)
      val variable: Variable = try {
        variableJpaRepository.save(Variable(type = typePermission.type, name = jsonParams.get(VariableConstants.VARIABLE_NAME).asString, created = defaultTimestamp))
      } catch (exception: Exception) {
        throw CustomJsonException("${VariableConstants.VARIABLE_NAME}: ${MessageConstants.VARIABLE_NOT_SAVED}")
      }
      variable.values.addAll(typePermission.type.keys.filter { it.type.name != TypeConstants.FORMULA }
        .sortedBy { it.keyOrder }
        .map { key ->
        valueJpaRepository.save(when(key.type.name) {
          TypeConstants.TEXT -> Value(variable = variable, key = key, stringValue = valuesJson.get(key.name).asString!!, created = defaultTimestamp)
          TypeConstants.NUMBER -> Value(variable = variable, key = key, longValue = valuesJson.get(key.name).asLong, created = defaultTimestamp)
          TypeConstants.DECIMAL -> Value(variable = variable, key = key, decimalValue = valuesJson.get(key.name).asBigDecimal!!, created = defaultTimestamp)
          TypeConstants.BOOLEAN -> Value(variable = variable, key = key, booleanValue = valuesJson.get(key.name).asBoolean, created = defaultTimestamp)
          TypeConstants.DATE -> Value(variable = variable, key = key, dateValue = Date(valuesJson.get(key.name).asLong), created = defaultTimestamp)
          TypeConstants.TIMESTAMP -> Value(variable = variable, key = key, timestampValue = Timestamp(valuesJson.get(key.name).asLong), created = defaultTimestamp)
          TypeConstants.TIME -> Value(variable = variable, key = key, timeValue = Time(valuesJson.get(key.name).asLong), created = defaultTimestamp)
          TypeConstants.BLOB -> if (valuesJson.has(key.name))
            Value(variable = variable, key = key, blobValue = BlobProxy.generateProxy(files[valuesJson.get(key.name).asInt].bytes), created = defaultTimestamp)
          else
            Value(variable = variable, key = key, blobValue = key.defaultBlobValue!!, created = defaultTimestamp)
          TypeConstants.FORMULA -> throw CustomJsonException("{}")
          else -> Value(variable = variable, key = key, referencedVariable = (variableRepository.findVariable(type = key.type, name = valuesJson.get(key.name).asString!!)
            ?: throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")).apply {
            if (!active)
              throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
          }, created = defaultTimestamp)
        })
      })
      variable.values.addAll(typePermission.type.keys.filter { it.type.name == TypeConstants.FORMULA }
        .sortedBy { it.keyOrder }
        .map { key ->
          val valueDependencies: MutableSet<Value> = mutableSetOf()
          val symbols: JsonObject = getSymbolValues(variable = variable, symbolPaths = gson.fromJson(key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(),
            valueDependencies = valueDependencies, excludeTopLevelFormulas = true)
          val evaluatedArg = validateOrEvaluateExpression(expression = gson.fromJson(key.formula!!.expression, JsonObject::class.java),
            symbols = symbols, mode = LispConstants.EVALUATE, expectedReturnType = key.formula!!.returnType.name)
          valueJpaRepository.save(when(key.formula!!.returnType.name) {
            TypeConstants.TEXT -> Value(variable = variable, key = key, stringValue = evaluatedArg as String, valueDependencies = valueDependencies, created = defaultTimestamp)
            TypeConstants.NUMBER -> Value(variable = variable, key = key, longValue = evaluatedArg as Long, valueDependencies = valueDependencies, created = defaultTimestamp)
            TypeConstants.DECIMAL -> Value(variable = variable, key = key, decimalValue = evaluatedArg as BigDecimal, valueDependencies = valueDependencies, created = defaultTimestamp)
            TypeConstants.BOOLEAN -> Value(variable = variable, key = key, booleanValue = evaluatedArg as Boolean, valueDependencies = valueDependencies, created = defaultTimestamp)
            TypeConstants.DATE -> Value(variable = variable, key = key, dateValue = evaluatedArg as Date, valueDependencies = valueDependencies, created = defaultTimestamp)
            TypeConstants.TIMESTAMP -> Value(variable = variable, key = key, timestampValue = evaluatedArg as Timestamp, valueDependencies = valueDependencies, created = defaultTimestamp)
            TypeConstants.TIME -> Value(variable = variable, key = key, timeValue = evaluatedArg as Time, valueDependencies = valueDependencies, created = defaultTimestamp)
            TypeConstants.BLOB -> Value(variable = variable, key = key, blobValue = BlobProxy.generateProxy(evaluatedArg as ByteArray), valueDependencies = valueDependencies, created = defaultTimestamp)
            else -> throw CustomJsonException("{${key.name}: ${MessageConstants.UNEXPECTED_VALUE}}")
          })
      })
      variable.variableUniqueness.addAll(variable.type.uniqueConstraints.map { typeUniqueness ->
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
          variableUniquenessJpaRepository.save(VariableUniqueness(typeUniqueness = typeUniqueness, variable = variable, hash = computedHash, created = defaultTimestamp))
        } catch (exception: Exception) {
          resolveHashConflict(variableUniqueness = VariableUniqueness(typeUniqueness = typeUniqueness, variable = variable, hash = computedHash, created = defaultTimestamp), variableUniquenessToUpdate = setOf())
        }
      })
      variable.variableAssertions.addAll(variable.type.typeAssertions.map { typeAssertion ->
        val valueDependencies: MutableSet<Value> = mutableSetOf()
        val symbols: JsonObject = getSymbolValues(
          variable = variable,
          symbolPaths = gson.fromJson(typeAssertion.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(),
          valueDependencies = valueDependencies,
          excludeTopLevelFormulas = false
        )
        val result: Boolean = validateOrEvaluateExpression(expression = gson.fromJson(typeAssertion.expression, JsonObject::class.java),
          symbols = symbols, mode = LispConstants.EVALUATE, expectedReturnType = TypeConstants.BOOLEAN) as Boolean
        if (!result)
          throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: 'Failed to assert ${variable.type.name}:${typeAssertion.name}'}")
        else
          variableAssertionJpaRepository.save(VariableAssertion(typeAssertion = typeAssertion, variable = variable, valueDependencies = valueDependencies, created = defaultTimestamp))
      })
      variable.type.uniqueConstraints.forEach { typeUniqueness ->
        typeUniqueness.accumulators.forEach { typeAccumulator ->
          val computedHash: String = computeHash(typeAccumulator.keys.sortedBy { it.id }.fold("") { acc, key ->
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
              TypeConstants.FORMULA -> throw CustomJsonException("{}")
              else -> value.referencedVariable!!.id
            }
          })
          var rootVariableAccumulator: VariableAccumulator? = variableAccumulatorRepository.findVariableAccumulator(typeAccumulator = typeAccumulator, level = 0, hash = computedHash)
          if (rootVariableAccumulator != null) {
            val matchingVariableAccumulator: VariableAccumulator? = getMatchingVariableAccumulator(variable = variable, typeAccumulator = typeAccumulator, variableAccumulator = rootVariableAccumulator)
            if (matchingVariableAccumulator != null) {
              val symbols: JsonObject = JsonObject().apply {
                add("acc", JsonObject().apply {
                  addProperty(SymbolConstants.SYMBOL_TYPE, if(typeAccumulator.type.name in primitiveTypes) typeAccumulator.type.name else TypeConstants.TEXT)
                  when(typeAccumulator.type.name) {
                    TypeConstants.TEXT -> addProperty(SymbolConstants.SYMBOL_VALUE, matchingVariableAccumulator.stringValue!!)
                    TypeConstants.NUMBER -> addProperty(SymbolConstants.SYMBOL_VALUE, matchingVariableAccumulator.longValue!!)
                    TypeConstants.DECIMAL -> addProperty(SymbolConstants.SYMBOL_VALUE, matchingVariableAccumulator.decimalValue!!)
                    TypeConstants.BOOLEAN -> addProperty(SymbolConstants.SYMBOL_VALUE, matchingVariableAccumulator.booleanValue!!)
                    TypeConstants.DATE -> addProperty(SymbolConstants.SYMBOL_VALUE, matchingVariableAccumulator.dateValue!!.time)
                    TypeConstants.TIMESTAMP -> addProperty(SymbolConstants.SYMBOL_VALUE, matchingVariableAccumulator.timestampValue!!.time)
                    TypeConstants.TIME -> addProperty(SymbolConstants.SYMBOL_VALUE, matchingVariableAccumulator.timeValue!!.time)
                    TypeConstants.BLOB -> addProperty(SymbolConstants.SYMBOL_VALUE, Base64.getEncoder().encodeToString(matchingVariableAccumulator.blobValue!!.getBytes(1 ,matchingVariableAccumulator.blobValue!!.length().toInt())))
                    TypeConstants.FORMULA -> throw CustomJsonException("{}")
                    else -> {
                      addProperty(SymbolConstants.SYMBOL_VALUE, matchingVariableAccumulator.referencedVariable!!.name)
                      add(SymbolConstants.SYMBOL_VALUES, getSymbolValues(variable = matchingVariableAccumulator.referencedVariable!!, symbolPaths = gson.fromJson(typeAccumulator.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), prefix = "acc.", excludeTopLevelFormulas = false))
                    }
                  }
                })
                add("it", JsonObject().apply {
                  addProperty(SymbolConstants.SYMBOL_TYPE, TypeConstants.TEXT)
                  addProperty(SymbolConstants.SYMBOL_VALUE, variable.name)
                  add(SymbolConstants.SYMBOL_VALUES, getSymbolValues(variable = variable, symbolPaths = gson.fromJson(typeAccumulator.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), prefix = "it.", excludeTopLevelFormulas = false))
                })
              }
              val evaluatedValue = validateOrEvaluateExpression(expression = gson.fromJson(typeAccumulator.forwardExpression, JsonObject::class.java), symbols = symbols,
                mode = LispConstants.EVALUATE, expectedReturnType = if (typeAccumulator.type.name in primitiveTypes) typeAccumulator.type.name else TypeConstants.TEXT)
              when(typeAccumulator.type.name) {
                TypeConstants.TEXT -> matchingVariableAccumulator.stringValue = evaluatedValue as String
                TypeConstants.NUMBER -> matchingVariableAccumulator.longValue = evaluatedValue as Long
                TypeConstants.DECIMAL -> matchingVariableAccumulator.decimalValue = evaluatedValue as BigDecimal
                TypeConstants.BOOLEAN -> matchingVariableAccumulator.booleanValue = evaluatedValue as Boolean
                TypeConstants.DATE -> matchingVariableAccumulator.dateValue = evaluatedValue as Date
                TypeConstants.TIMESTAMP -> matchingVariableAccumulator.timestampValue = evaluatedValue as Timestamp
                TypeConstants.TIME -> matchingVariableAccumulator.timeValue = evaluatedValue as Time
                TypeConstants.BLOB -> matchingVariableAccumulator.blobValue = BlobProxy.generateProxy(evaluatedValue as ByteArray)
                TypeConstants.FORMULA -> throw CustomJsonException("{}")
                else -> {
                  matchingVariableAccumulator.referencedVariable = variableRepository.findVariable(type = typeAccumulator.type, name = evaluatedValue as String)
                    ?: throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
              }
              variableAccumulatorJpaRepository.save(matchingVariableAccumulator)
            } else {
              val symbols: JsonObject = JsonObject().apply {
                add("acc", JsonObject().apply {
                  addProperty(SymbolConstants.SYMBOL_TYPE, if(typeAccumulator.type.name in primitiveTypes) typeAccumulator.type.name else TypeConstants.TEXT)
                  when(typeAccumulator.type.name) {
                    TypeConstants.TEXT -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialStringValue!!)
                    TypeConstants.NUMBER -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialLongValue!!)
                    TypeConstants.DECIMAL -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialDecimalValue!!)
                    TypeConstants.BOOLEAN -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialBooleanValue!!)
                    TypeConstants.DATE -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialDateValue!!.time)
                    TypeConstants.TIMESTAMP -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialTimestampValue!!.time)
                    TypeConstants.TIME -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialTimeValue!!.time)
                    TypeConstants.BLOB -> addProperty(SymbolConstants.SYMBOL_VALUE, Base64.getEncoder().encodeToString(typeAccumulator.initialBlobValue!!.getBytes(1 ,typeAccumulator.initialBlobValue!!.length().toInt())))
                    TypeConstants.FORMULA -> throw CustomJsonException("{}")
                    else -> {
                      addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.referencedVariable!!.name)
                      add(SymbolConstants.SYMBOL_VALUES, getSymbolValues(variable = typeAccumulator.referencedVariable!!, symbolPaths = gson.fromJson(typeAccumulator.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), prefix = "acc.", excludeTopLevelFormulas = false))
                    }
                  }
                })
                add("it", JsonObject().apply {
                  addProperty(SymbolConstants.SYMBOL_TYPE, TypeConstants.TEXT)
                  addProperty(SymbolConstants.SYMBOL_VALUE, variable.name)
                  add(SymbolConstants.SYMBOL_VALUES, getSymbolValues(variable = variable, symbolPaths = gson.fromJson(typeAccumulator.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), prefix = "it.", excludeTopLevelFormulas = false))
                })
              }
              var maxLevel = rootVariableAccumulator.level
              while (rootVariableAccumulator!!.nextVariableAccumulator != null) {
                if (rootVariableAccumulator.level > maxLevel)
                  maxLevel = rootVariableAccumulator.level
                rootVariableAccumulator = rootVariableAccumulator.nextVariableAccumulator
              }
              val evaluatedValue = validateOrEvaluateExpression(expression = gson.fromJson(typeAccumulator.forwardExpression, JsonObject::class.java), symbols = symbols,
                mode = LispConstants.EVALUATE, expectedReturnType = if (typeAccumulator.type.name in primitiveTypes) typeAccumulator.type.name else TypeConstants.TEXT)
              variableAccumulatorJpaRepository.save(VariableAccumulator(typeAccumulator = typeAccumulator,level = 1 + maxLevel, hash = computedHash, created = defaultTimestamp).apply {
                when(typeAccumulator.type.name) {
                  TypeConstants.TEXT -> stringValue = evaluatedValue as String
                  TypeConstants.NUMBER -> longValue = evaluatedValue as Long
                  TypeConstants.DECIMAL -> decimalValue = evaluatedValue as BigDecimal
                  TypeConstants.BOOLEAN -> booleanValue = evaluatedValue as Boolean
                  TypeConstants.DATE -> dateValue = evaluatedValue as Date
                  TypeConstants.TIMESTAMP -> timestampValue = evaluatedValue as Timestamp
                  TypeConstants.TIME -> timeValue = evaluatedValue as Time
                  TypeConstants.BLOB -> blobValue = BlobProxy.generateProxy(evaluatedValue as ByteArray)
                  TypeConstants.FORMULA -> throw CustomJsonException("{}")
                  else -> {
                    referencedVariable = variableRepository.findVariable(type = typeAccumulator.type, name = evaluatedValue as String)
                      ?: throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
                  }
                }
              }).apply {
                values.addAll(valueAccumulatorJpaRepository.saveAll(typeAccumulator.keys.map { key ->
                  ValueAccumulator(variableAccumulator = this, key = key, created = defaultTimestamp).apply {
                    when(this.key.type.name) {
                      TypeConstants.TEXT -> stringValue = variable.values.single { it.key == this.key }.stringValue!!
                      TypeConstants.NUMBER -> longValue = variable.values.single { it.key == this.key }.longValue!!
                      TypeConstants.DECIMAL -> decimalValue = variable.values.single { it.key == this.key }.decimalValue!!
                      TypeConstants.BOOLEAN -> booleanValue = variable.values.single { it.key == this.key }.booleanValue!!
                      TypeConstants.DATE -> dateValue = variable.values.single { it.key == this.key }.dateValue!!
                      TypeConstants.TIMESTAMP -> timestampValue = variable.values.single { it.key == this.key }.timestampValue!!
                      TypeConstants.TIME -> timeValue = variable.values.single { it.key == this.key }.timeValue!!
                      TypeConstants.BLOB -> blobValue = variable.values.single { it.key == this.key }.blobValue!!
                      TypeConstants.FORMULA -> throw CustomJsonException("{}")
                      else -> referencedVariable = variable.values.single { it.key == this.key }.referencedVariable!!
                    }
                  }
                }))
              }
            }
          } else {
            val symbols: JsonObject = JsonObject().apply {
              add("acc", JsonObject().apply {
                addProperty(SymbolConstants.SYMBOL_TYPE, if(typeAccumulator.type.name in primitiveTypes) typeAccumulator.type.name else TypeConstants.TEXT)
                when(typeAccumulator.type.name) {
                  TypeConstants.TEXT -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialStringValue!!)
                  TypeConstants.NUMBER -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialLongValue!!)
                  TypeConstants.DECIMAL -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialDecimalValue!!)
                  TypeConstants.BOOLEAN -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialBooleanValue!!)
                  TypeConstants.DATE -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialDateValue!!.time)
                  TypeConstants.TIMESTAMP -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialTimestampValue!!.time)
                  TypeConstants.TIME -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialTimeValue!!.time)
                  TypeConstants.BLOB -> addProperty(SymbolConstants.SYMBOL_VALUE, Base64.getEncoder().encodeToString(typeAccumulator.initialBlobValue!!.getBytes(1 ,typeAccumulator.initialBlobValue!!.length().toInt())))
                  TypeConstants.FORMULA -> throw CustomJsonException("{}")
                  else -> {
                    addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.referencedVariable!!.name)
                    add(SymbolConstants.SYMBOL_VALUES, getSymbolValues(variable = typeAccumulator.referencedVariable!!, symbolPaths = gson.fromJson(typeAccumulator.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), prefix = "acc.", excludeTopLevelFormulas = false))
                  }
                }
              })
              add("it", JsonObject().apply {
                addProperty(SymbolConstants.SYMBOL_TYPE, TypeConstants.TEXT)
                addProperty(SymbolConstants.SYMBOL_VALUE, variable.name)
                add(SymbolConstants.SYMBOL_VALUES, getSymbolValues(variable = variable, symbolPaths = gson.fromJson(typeAccumulator.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), prefix = "it.", excludeTopLevelFormulas = false))
              })
            }
            val evaluatedValue = validateOrEvaluateExpression(expression = gson.fromJson(typeAccumulator.forwardExpression, JsonObject::class.java), symbols = symbols,
              mode = LispConstants.EVALUATE, expectedReturnType = if (typeAccumulator.type.name in primitiveTypes) typeAccumulator.type.name else TypeConstants.TEXT)
            variableAccumulatorJpaRepository.save(VariableAccumulator(typeAccumulator = typeAccumulator, level = 0, hash = computedHash, created = defaultTimestamp).apply {
              when(typeAccumulator.type.name) {
                TypeConstants.TEXT -> stringValue = evaluatedValue as String
                TypeConstants.NUMBER -> longValue = evaluatedValue as Long
                TypeConstants.DECIMAL -> decimalValue = evaluatedValue as BigDecimal
                TypeConstants.BOOLEAN -> booleanValue = evaluatedValue as Boolean
                TypeConstants.DATE -> dateValue = evaluatedValue as Date
                TypeConstants.TIMESTAMP -> timestampValue = evaluatedValue as Timestamp
                TypeConstants.TIME -> timeValue = evaluatedValue as Time
                TypeConstants.BLOB -> blobValue = BlobProxy.generateProxy(evaluatedValue as ByteArray)
                TypeConstants.FORMULA -> throw CustomJsonException("{}")
                else -> {
                  referencedVariable = variableRepository.findVariable(type = typeAccumulator.type, name = evaluatedValue as String)
                    ?: throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
              }
            }).apply {
              values.addAll(valueAccumulatorJpaRepository.saveAll(typeAccumulator.keys.map { key ->
                ValueAccumulator(variableAccumulator = this, key = key, created = defaultTimestamp).apply {
                  when(this.key.type.name) {
                    TypeConstants.TEXT -> stringValue = variable.values.single { it.key == this.key }.stringValue!!
                    TypeConstants.NUMBER -> longValue = variable.values.single { it.key == this.key }.longValue!!
                    TypeConstants.DECIMAL -> decimalValue = variable.values.single { it.key == this.key }.decimalValue!!
                    TypeConstants.BOOLEAN -> booleanValue = variable.values.single { it.key == this.key }.booleanValue!!
                    TypeConstants.DATE -> dateValue = variable.values.single { it.key == this.key }.dateValue!!
                    TypeConstants.TIMESTAMP -> timestampValue = variable.values.single { it.key == this.key }.timestampValue!!
                    TypeConstants.TIME -> timeValue = variable.values.single { it.key == this.key }.timeValue!!
                    TypeConstants.BLOB -> blobValue = variable.values.single { it.key == this.key }.blobValue!!
                    TypeConstants.FORMULA -> throw CustomJsonException("{}")
                    else -> referencedVariable = variable.values.single { it.key == this.key }.referencedVariable!!
                  }
                }
              }))
            }
          }
        }
      }
      Pair(variable, typePermission)
    }
  }

  @Suppress("UNCHECKED_CAST")
  fun updateVariable(jsonParams: JsonObject, files: List<MultipartFile>, defaultTimestamp: Timestamp): Pair<Variable, TypePermission> {
    val typePermission: TypePermission = userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
        addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
        addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
        addProperty(OrganizationConstants.TYPE_NAME, jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
      }, defaultTimestamp = defaultTimestamp)
    val variable: Variable = (variableRepository.findVariable(type = typePermission.type, name = jsonParams.get(VariableConstants.VARIABLE_NAME).asString)
      ?: throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.VARIABLE_NOT_FOUND}}")).apply {
      if (jsonParams.has("${VariableConstants.ACTIVE}?"))
        active = jsonParams.get("${VariableConstants.ACTIVE}?").asBoolean
    }
    val valuesJson: JsonObject = validateUpdatedVariableValues(values = jsonParams.get(VariableConstants.VALUES).asJsonObject, typePermission = typePermission, defaultTimestamp = defaultTimestamp, files = files)
    val dependentUniqueness: MutableSet<VariableUniqueness> = mutableSetOf()
    val dependentFormulaValues: MutableSet<Value> = mutableSetOf()
    val dependentAssertions: MutableSet<VariableAssertion> = mutableSetOf()
    val dependentTypeAccumulators: MutableMap<TypeAccumulator, MutableSet<Pair<Key, Any>>> = mutableMapOf()
    variable.values.filter { it.key.type.name != TypeConstants.FORMULA }.forEach { value ->
      if (valuesJson.has(value.key.name)) {
        if (value.key.isAccumulatorDependency) {
          value.key.dependentTypeAccumulators.forEach { typeAccumulator ->
            if (typeAccumulator in dependentTypeAccumulators)
              dependentTypeAccumulators[typeAccumulator]!!.add(Pair(value.key, when(value.key.type.name) {
                TypeConstants.TEXT -> value.stringValue!!
                TypeConstants.NUMBER -> value.longValue!!
                TypeConstants.DECIMAL -> value.decimalValue!!
                TypeConstants.BOOLEAN -> value.booleanValue!!
                TypeConstants.DATE -> value.dateValue!!
                TypeConstants.TIMESTAMP -> value.timestampValue!!
                TypeConstants.TIME -> value.timeValue!!
                TypeConstants.BLOB -> value.blobValue!!
                TypeConstants.FORMULA -> throw CustomJsonException("{}")
                else -> value.referencedVariable!!
              }))
            else
              dependentTypeAccumulators[typeAccumulator] = mutableSetOf(Pair(value.key, when(value.key.type.name) {
                TypeConstants.TEXT -> value.stringValue!!
                TypeConstants.NUMBER -> value.longValue!!
                TypeConstants.DECIMAL -> value.decimalValue!!
                TypeConstants.BOOLEAN -> value.booleanValue!!
                TypeConstants.DATE -> value.dateValue!!
                TypeConstants.TIMESTAMP -> value.timestampValue!!
                TypeConstants.TIME -> value.timeValue!!
                TypeConstants.BLOB -> value.blobValue!!
                TypeConstants.FORMULA -> throw CustomJsonException("{}")
                else -> value.referencedVariable!!
              }))
          }
        }
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
            value.referencedVariable = (variableRepository.findVariable(type = typePermission.type, name = valuesJson.get(value.key.name).asString!!)
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
          dependentUniqueness.addAll(value.key.dependentTypeUniqueness.map { typeUniqueness -> variable.variableUniqueness.single { it.typeUniqueness == typeUniqueness } })
      }
    }
    variable.values.filter { it.key.type.name == TypeConstants.FORMULA }.forEach { value ->
      if (dependentFormulaValues.contains(value)) {
        val updatedValueDependencies: MutableSet<Value> = mutableSetOf()
        val evaluatedValue = validateOrEvaluateExpression(expression = gson.fromJson(value.key.formula!!.expression, JsonObject::class.java),
          symbols = getSymbolValues(variable = variable,
            symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(),
            valueDependencies = updatedValueDependencies,
            excludeTopLevelFormulas = true
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
          dependentUniqueness.addAll(value.key.dependentTypeUniqueness.map { typeUniqueness -> variable.variableUniqueness.single { it.typeUniqueness == typeUniqueness } })
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
    // Same applies to VariableUniqueness and VariableAccumulators.
    evaluateAssertions(dependentAssertions = dependentAssertions)
    recomputeHashesForVariableUniqueness(dependentUniqueness = dependentUniqueness)
    // Update Accumulators
    for((typeAccumulator, accumulatorValues) in dependentTypeAccumulators)
      updateVariableAccumulator(variable = variable, typeAccumulator = typeAccumulator, prevName = jsonParams.get(VariableConstants.VARIABLE_NAME).asString, accumulatorValues = accumulatorValues, defaultTimestamp = defaultTimestamp)
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
          excludeTopLevelFormulas = true),
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
        dependentUniqueness.addAll(value.key.dependentTypeUniqueness.map { typeUniqueness -> value.variable.variableUniqueness.single { it.typeUniqueness == typeUniqueness } })
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
          excludeTopLevelFormulas = false),
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

  fun getMatchingVariableAccumulator(variable: Variable, typeAccumulator: TypeAccumulator, variableAccumulator: VariableAccumulator): VariableAccumulator? {
    return if (variableAccumulator.values.all { valueAccumulator ->
        when(valueAccumulator.key.type.name) {
          TypeConstants.TEXT -> variable.values.any { value -> value.key == valueAccumulator.key && value.stringValue!! == valueAccumulator.stringValue!! }
          TypeConstants.NUMBER -> variable.values.any { value -> value.key == valueAccumulator.key && value.longValue!! == valueAccumulator.longValue!! }
          TypeConstants.DECIMAL -> variable.values.any { value -> value.key == valueAccumulator.key && value.decimalValue!! == valueAccumulator.decimalValue!! }
          TypeConstants.BOOLEAN -> variable.values.any { value -> value.key == valueAccumulator.key && value.booleanValue!! == valueAccumulator.booleanValue!! }
          TypeConstants.DATE -> variable.values.any { value -> value.key == valueAccumulator.key && value.dateValue!! == valueAccumulator.dateValue!! }
          TypeConstants.TIMESTAMP -> variable.values.any { value -> value.key == valueAccumulator.key && value.timestampValue!! == valueAccumulator.timestampValue!! }
          TypeConstants.TIME -> variable.values.any { value -> value.key == valueAccumulator.key && value.timeValue!! == valueAccumulator.timeValue!! }
          TypeConstants.BLOB -> variable.values.any { value -> value.key == valueAccumulator.key && value.blobValue!! == valueAccumulator.blobValue!! }
          TypeConstants.FORMULA -> throw CustomJsonException("{}")
          else -> variable.values.any { value -> value.key == valueAccumulator.key && value.referencedVariable!! == valueAccumulator.referencedVariable!! }
        }
      }) variableAccumulator
    else if (variableAccumulator.nextVariableAccumulator != null) {
      getMatchingVariableAccumulator(variable = variable, typeAccumulator = typeAccumulator, variableAccumulator = variableAccumulator.nextVariableAccumulator!!)
    } else null
  }

  fun getMatchingBackwardVariableAccumulator(variable: Variable, typeAccumulator: TypeAccumulator, variableAccumulator: VariableAccumulator, accumulatorValues: Set<Pair<Key, Any>>): VariableAccumulator {
    return if (variableAccumulator.values.all { valueAccumulator ->
        if (accumulatorValues.any { (k, _) -> k == valueAccumulator.key }) {
          when(valueAccumulator.key.type.name) {
            TypeConstants.TEXT -> accumulatorValues.any { (k, v) -> k == valueAccumulator.key && (v as String) == valueAccumulator.stringValue!! }
            TypeConstants.NUMBER -> accumulatorValues.any { (k, v) -> k == valueAccumulator.key && (v as Long) == valueAccumulator.longValue!! }
            TypeConstants.DECIMAL -> accumulatorValues.any { (k, v) -> k == valueAccumulator.key && (v as BigDecimal) == valueAccumulator.decimalValue!! }
            TypeConstants.BOOLEAN -> accumulatorValues.any { (k, v) -> k == valueAccumulator.key && (v as Boolean) == valueAccumulator.booleanValue!! }
            TypeConstants.DATE -> accumulatorValues.any { (k, v) -> k == valueAccumulator.key && (v as Date) == valueAccumulator.dateValue!! }
            TypeConstants.TIMESTAMP -> accumulatorValues.any { (k, v) -> k == valueAccumulator.key && (v as Timestamp) == valueAccumulator.timestampValue!! }
            TypeConstants.TIME -> accumulatorValues.any { (k, v) -> k == valueAccumulator.key && (v as Time) == valueAccumulator.timeValue!! }
            TypeConstants.BLOB -> accumulatorValues.any { (k, v) -> k == valueAccumulator.key && (v as Blob) == valueAccumulator.blobValue!! }
            TypeConstants.FORMULA -> throw CustomJsonException("{}")
            else -> accumulatorValues.any { (k, v) -> k == valueAccumulator.key && (v as Variable) == valueAccumulator.referencedVariable!! }
          }
        } else {
          when(valueAccumulator.key.type.name) {
            TypeConstants.TEXT -> variable.values.any { value -> value.key == valueAccumulator.key && value.stringValue!! == valueAccumulator.stringValue!! }
            TypeConstants.NUMBER -> variable.values.any { value -> value.key == valueAccumulator.key && value.longValue!! == valueAccumulator.longValue!! }
            TypeConstants.DECIMAL -> variable.values.any { value -> value.key == valueAccumulator.key && value.decimalValue!! == valueAccumulator.decimalValue!! }
            TypeConstants.BOOLEAN -> variable.values.any { value -> value.key == valueAccumulator.key && value.booleanValue!! == valueAccumulator.booleanValue!! }
            TypeConstants.DATE -> variable.values.any { value -> value.key == valueAccumulator.key && value.dateValue!! == valueAccumulator.dateValue!! }
            TypeConstants.TIMESTAMP -> variable.values.any { value -> value.key == valueAccumulator.key && value.timestampValue!! == valueAccumulator.timestampValue!! }
            TypeConstants.TIME -> variable.values.any { value -> value.key == valueAccumulator.key && value.timeValue!! == valueAccumulator.timeValue!! }
            TypeConstants.BLOB -> variable.values.any { value -> value.key == valueAccumulator.key && value.blobValue!! == valueAccumulator.blobValue!! }
            TypeConstants.FORMULA -> throw CustomJsonException("{}")
            else -> variable.values.any { value -> value.key == valueAccumulator.key && value.referencedVariable!! == valueAccumulator.referencedVariable!! }
          }
        }
      }) variableAccumulator
    else getMatchingBackwardVariableAccumulator(variable = variable, typeAccumulator = typeAccumulator, variableAccumulator = variableAccumulator.nextVariableAccumulator!!, accumulatorValues = accumulatorValues)
  }

  fun updateVariableAccumulator(variable: Variable, typeAccumulator: TypeAccumulator, prevName: String, accumulatorValues: Set<Pair<Key, Any>>, defaultTimestamp: Timestamp) {
    // Run backward expression with previous values
    val backwardComputedHash: String = computeHash(typeAccumulator.keys.sortedBy { it.id }.fold("") { acc, key ->
      acc + if(accumulatorValues.any { (k, _) -> k == key  }) {
        val value = accumulatorValues.single { (k, _) -> k == key }.second
        when (key.type.name) {
          TypeConstants.TEXT -> value as String
          TypeConstants.NUMBER -> (value as Long).toString()
          TypeConstants.DECIMAL -> (value as BigDecimal).toString()
          TypeConstants.BOOLEAN -> (value as Boolean).toString()
          TypeConstants.DATE -> (value as Date).toString()
          TypeConstants.TIMESTAMP -> (value as Timestamp).toString()
          TypeConstants.TIME -> (value as Time).toString()
          TypeConstants.BLOB -> Base64.getEncoder().encodeToString((value as Blob).getBytes(1, value.length().toInt()))
          TypeConstants.FORMULA -> throw CustomJsonException("{}")
          else -> (value as Variable).id
        }
      } else {
        val value: Value = variable.values.single { it.key == key }
        when (value.key.type.name) {
          TypeConstants.TEXT -> value.stringValue!!
          TypeConstants.NUMBER -> value.longValue.toString()
          TypeConstants.DECIMAL -> value.decimalValue.toString()
          TypeConstants.BOOLEAN -> value.booleanValue.toString()
          TypeConstants.DATE -> value.dateValue.toString()
          TypeConstants.TIMESTAMP -> value.timestampValue.toString()
          TypeConstants.TIME -> value.timeValue.toString()
          TypeConstants.BLOB -> Base64.getEncoder().encodeToString(value.blobValue!!.getBytes(1, value.blobValue!!.length().toInt()))
          TypeConstants.FORMULA -> throw CustomJsonException("{}")
          else -> value.referencedVariable!!.id
        }
      }
    })
    val backwardVariableAccumulator: VariableAccumulator = getMatchingBackwardVariableAccumulator(variable = variable, typeAccumulator = typeAccumulator, accumulatorValues = accumulatorValues,
      variableAccumulator = variableAccumulatorRepository.findVariableAccumulator(typeAccumulator = typeAccumulator, level = 0, hash = backwardComputedHash)!!)
    val backwardSymbols: JsonObject = JsonObject().apply {
      add("acc", JsonObject().apply {
        addProperty(SymbolConstants.SYMBOL_TYPE, if(typeAccumulator.type.name in primitiveTypes) typeAccumulator.type.name else TypeConstants.TEXT)
        when(typeAccumulator.type.name) {
          TypeConstants.TEXT -> addProperty(SymbolConstants.SYMBOL_VALUE, backwardVariableAccumulator.stringValue!!)
          TypeConstants.NUMBER -> addProperty(SymbolConstants.SYMBOL_VALUE, backwardVariableAccumulator.longValue!!)
          TypeConstants.DECIMAL -> addProperty(SymbolConstants.SYMBOL_VALUE, backwardVariableAccumulator.decimalValue!!)
          TypeConstants.BOOLEAN -> addProperty(SymbolConstants.SYMBOL_VALUE, backwardVariableAccumulator.booleanValue!!)
          TypeConstants.DATE -> addProperty(SymbolConstants.SYMBOL_VALUE, backwardVariableAccumulator.dateValue!!.time)
          TypeConstants.TIMESTAMP -> addProperty(SymbolConstants.SYMBOL_VALUE, backwardVariableAccumulator.timestampValue!!.time)
          TypeConstants.TIME -> addProperty(SymbolConstants.SYMBOL_VALUE, backwardVariableAccumulator.timeValue!!.time)
          TypeConstants.BLOB -> addProperty(SymbolConstants.SYMBOL_VALUE, Base64.getEncoder().encodeToString(backwardVariableAccumulator.blobValue!!.getBytes(1 ,backwardVariableAccumulator.blobValue!!.length().toInt())))
          TypeConstants.FORMULA -> throw CustomJsonException("{}")
          else -> {
            addProperty(SymbolConstants.SYMBOL_VALUE, backwardVariableAccumulator.referencedVariable!!.name)
            add(SymbolConstants.SYMBOL_VALUES, getSymbolValues(variable = backwardVariableAccumulator.referencedVariable!!, symbolPaths = gson.fromJson(typeAccumulator.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), prefix = "acc.", excludeTopLevelFormulas = false))
          }
        }
      })
      add("it", JsonObject().apply {
        addProperty(SymbolConstants.SYMBOL_TYPE, TypeConstants.TEXT)
        addProperty(SymbolConstants.SYMBOL_VALUE, prevName)
        add(SymbolConstants.SYMBOL_VALUES, getBackwardSymbolValuesForAccumulator(variable = variable, symbolPaths = gson.fromJson(typeAccumulator.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), prefix = "it.", excludeTopLevelFormulas = false, accumulatorValues = accumulatorValues))
      })
    }
    val backwardEvaluatedValue = validateOrEvaluateExpression(expression = gson.fromJson(typeAccumulator.backwardExpression, JsonObject::class.java), symbols = backwardSymbols,
      mode = LispConstants.EVALUATE, expectedReturnType = if (typeAccumulator.type.name in primitiveTypes) typeAccumulator.type.name else TypeConstants.TEXT)
    when(typeAccumulator.type.name) {
      TypeConstants.TEXT -> backwardVariableAccumulator.stringValue = backwardEvaluatedValue as String
      TypeConstants.NUMBER -> backwardVariableAccumulator.longValue = backwardEvaluatedValue as Long
      TypeConstants.DECIMAL -> backwardVariableAccumulator.decimalValue = backwardEvaluatedValue as BigDecimal
      TypeConstants.BOOLEAN -> backwardVariableAccumulator.booleanValue = backwardEvaluatedValue as Boolean
      TypeConstants.DATE -> backwardVariableAccumulator.dateValue = backwardEvaluatedValue as Date
      TypeConstants.TIMESTAMP -> backwardVariableAccumulator.timestampValue = backwardEvaluatedValue as Timestamp
      TypeConstants.TIME -> backwardVariableAccumulator.timeValue = backwardEvaluatedValue as Time
      TypeConstants.BLOB -> backwardVariableAccumulator.blobValue = BlobProxy.generateProxy(backwardEvaluatedValue as ByteArray)
      TypeConstants.FORMULA -> throw CustomJsonException("{}")
      else -> {
        backwardVariableAccumulator.referencedVariable = variableRepository.findVariable(type = typeAccumulator.type, name = backwardEvaluatedValue as String)
          ?: throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
      }
    }
    variableAccumulatorJpaRepository.save(backwardVariableAccumulator)
    // Run forward expression with updated values
    val forwardComputedHash: String = computeHash(typeAccumulator.keys.sortedBy { it.id }.fold("") { acc, key ->
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
        TypeConstants.FORMULA -> throw CustomJsonException("{}")
        else -> value.referencedVariable!!.id
      }
    })
    var rootForwardVariableAccumulator: VariableAccumulator? = variableAccumulatorRepository.findVariableAccumulator(typeAccumulator = typeAccumulator, level = 0, hash = forwardComputedHash)
    if (rootForwardVariableAccumulator != null) {
      val forwardVariableAccumulator: VariableAccumulator? = getMatchingVariableAccumulator(variable = variable, typeAccumulator = typeAccumulator, variableAccumulator = rootForwardVariableAccumulator)
      if (forwardVariableAccumulator != null) {
        val forwardSymbols: JsonObject = JsonObject().apply {
          add("acc", JsonObject().apply {
            addProperty(SymbolConstants.SYMBOL_TYPE, if(typeAccumulator.type.name in primitiveTypes) typeAccumulator.type.name else TypeConstants.TEXT)
            when(typeAccumulator.type.name) {
              TypeConstants.TEXT -> addProperty(SymbolConstants.SYMBOL_VALUE, forwardVariableAccumulator.stringValue!!)
              TypeConstants.NUMBER -> addProperty(SymbolConstants.SYMBOL_VALUE, forwardVariableAccumulator.longValue!!)
              TypeConstants.DECIMAL -> addProperty(SymbolConstants.SYMBOL_VALUE, forwardVariableAccumulator.decimalValue!!)
              TypeConstants.BOOLEAN -> addProperty(SymbolConstants.SYMBOL_VALUE, forwardVariableAccumulator.booleanValue!!)
              TypeConstants.DATE -> addProperty(SymbolConstants.SYMBOL_VALUE, forwardVariableAccumulator.dateValue!!.time)
              TypeConstants.TIMESTAMP -> addProperty(SymbolConstants.SYMBOL_VALUE, forwardVariableAccumulator.timestampValue!!.time)
              TypeConstants.TIME -> addProperty(SymbolConstants.SYMBOL_VALUE, forwardVariableAccumulator.timeValue!!.time)
              TypeConstants.BLOB -> addProperty(SymbolConstants.SYMBOL_VALUE, Base64.getEncoder().encodeToString(forwardVariableAccumulator.blobValue!!.getBytes(1 ,forwardVariableAccumulator.blobValue!!.length().toInt())))
              TypeConstants.FORMULA -> throw CustomJsonException("{}")
              else -> {
                addProperty(SymbolConstants.SYMBOL_VALUE, forwardVariableAccumulator.referencedVariable!!.name)
                add(SymbolConstants.SYMBOL_VALUES, getSymbolValues(variable = forwardVariableAccumulator.referencedVariable!!, symbolPaths = gson.fromJson(typeAccumulator.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), prefix = "acc.", excludeTopLevelFormulas = false))
              }
            }
          })
          add("it", JsonObject().apply {
            addProperty(SymbolConstants.SYMBOL_TYPE, TypeConstants.TEXT)
            addProperty(SymbolConstants.SYMBOL_VALUE, variable.name)
            add(SymbolConstants.SYMBOL_VALUES, getSymbolValues(variable = variable, symbolPaths = gson.fromJson(typeAccumulator.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), prefix = "it.", excludeTopLevelFormulas = false))
          })
        }
        val evaluatedValue = validateOrEvaluateExpression(expression = gson.fromJson(typeAccumulator.forwardExpression, JsonObject::class.java), symbols = forwardSymbols,
          mode = LispConstants.EVALUATE, expectedReturnType = if (typeAccumulator.type.name in primitiveTypes) typeAccumulator.type.name else TypeConstants.TEXT)
        when(typeAccumulator.type.name) {
          TypeConstants.TEXT -> forwardVariableAccumulator.stringValue = evaluatedValue as String
          TypeConstants.NUMBER -> forwardVariableAccumulator.longValue = evaluatedValue as Long
          TypeConstants.DECIMAL -> forwardVariableAccumulator.decimalValue = evaluatedValue as BigDecimal
          TypeConstants.BOOLEAN -> forwardVariableAccumulator.booleanValue = evaluatedValue as Boolean
          TypeConstants.DATE -> forwardVariableAccumulator.dateValue = evaluatedValue as Date
          TypeConstants.TIMESTAMP -> forwardVariableAccumulator.timestampValue = evaluatedValue as Timestamp
          TypeConstants.TIME -> forwardVariableAccumulator.timeValue = evaluatedValue as Time
          TypeConstants.BLOB -> forwardVariableAccumulator.blobValue = BlobProxy.generateProxy(evaluatedValue as ByteArray)
          TypeConstants.FORMULA -> throw CustomJsonException("{}")
          else -> {
            forwardVariableAccumulator.referencedVariable = variableRepository.findVariable(type = typeAccumulator.type, name = evaluatedValue as String)
              ?: throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
          }
        }
        variableAccumulatorJpaRepository.save(forwardVariableAccumulator)
      } else {
        val forwardSymbols: JsonObject = JsonObject().apply {
          add("acc", JsonObject().apply {
            addProperty(SymbolConstants.SYMBOL_TYPE, if(typeAccumulator.type.name in primitiveTypes) typeAccumulator.type.name else TypeConstants.TEXT)
            when(typeAccumulator.type.name) {
              TypeConstants.TEXT -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialStringValue!!)
              TypeConstants.NUMBER -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialLongValue!!)
              TypeConstants.DECIMAL -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialDecimalValue!!)
              TypeConstants.BOOLEAN -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialBooleanValue!!)
              TypeConstants.DATE -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialDateValue!!.time)
              TypeConstants.TIMESTAMP -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialTimestampValue!!.time)
              TypeConstants.TIME -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialTimeValue!!.time)
              TypeConstants.BLOB -> addProperty(SymbolConstants.SYMBOL_VALUE, Base64.getEncoder().encodeToString(typeAccumulator.initialBlobValue!!.getBytes(1 ,typeAccumulator.initialBlobValue!!.length().toInt())))
              TypeConstants.FORMULA -> throw CustomJsonException("{}")
              else -> {
                addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.referencedVariable!!.name)
                add(SymbolConstants.SYMBOL_VALUES, getSymbolValues(variable = typeAccumulator.referencedVariable!!, symbolPaths = gson.fromJson(typeAccumulator.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), prefix = "acc.", excludeTopLevelFormulas = false))
              }
            }
          })
          add("it", JsonObject().apply {
            addProperty(SymbolConstants.SYMBOL_TYPE, TypeConstants.TEXT)
            addProperty(SymbolConstants.SYMBOL_VALUE, variable.name)
            add(SymbolConstants.SYMBOL_VALUES, getSymbolValues(variable = variable, symbolPaths = gson.fromJson(typeAccumulator.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), prefix = "it.", excludeTopLevelFormulas = false))
          })
        }
        var maxLevel = rootForwardVariableAccumulator.level
        while (rootForwardVariableAccumulator!!.nextVariableAccumulator != null) {
          if (rootForwardVariableAccumulator.level > maxLevel)
            maxLevel = rootForwardVariableAccumulator.level
          rootForwardVariableAccumulator = rootForwardVariableAccumulator.nextVariableAccumulator
        }
        val evaluatedValue = validateOrEvaluateExpression(expression = gson.fromJson(typeAccumulator.forwardExpression, JsonObject::class.java), symbols = forwardSymbols,
          mode = LispConstants.EVALUATE, expectedReturnType = if (typeAccumulator.type.name in primitiveTypes) typeAccumulator.type.name else TypeConstants.TEXT)
        variableAccumulatorJpaRepository.save(VariableAccumulator(typeAccumulator = typeAccumulator,level = 1 + maxLevel, hash = forwardComputedHash, created = defaultTimestamp).apply {
          when(typeAccumulator.type.name) {
            TypeConstants.TEXT -> stringValue = evaluatedValue as String
            TypeConstants.NUMBER -> longValue = evaluatedValue as Long
            TypeConstants.DECIMAL -> decimalValue = evaluatedValue as BigDecimal
            TypeConstants.BOOLEAN -> booleanValue = evaluatedValue as Boolean
            TypeConstants.DATE -> dateValue = evaluatedValue as Date
            TypeConstants.TIMESTAMP -> timestampValue = evaluatedValue as Timestamp
            TypeConstants.TIME -> timeValue = evaluatedValue as Time
            TypeConstants.BLOB -> blobValue = BlobProxy.generateProxy(evaluatedValue as ByteArray)
            TypeConstants.FORMULA -> throw CustomJsonException("{}")
            else -> {
              referencedVariable = variableRepository.findVariable(type = typeAccumulator.type, name = evaluatedValue as String)
                ?: throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
            }
          }
        }).apply {
          values.addAll(valueAccumulatorJpaRepository.saveAll(typeAccumulator.keys.map { key ->
            ValueAccumulator(variableAccumulator = this, key = key, created = defaultTimestamp).apply {
              when(this.key.type.name) {
                TypeConstants.TEXT -> stringValue = variable.values.single { it.key == this.key }.stringValue!!
                TypeConstants.NUMBER -> longValue = variable.values.single { it.key == this.key }.longValue!!
                TypeConstants.DECIMAL -> decimalValue = variable.values.single { it.key == this.key }.decimalValue!!
                TypeConstants.BOOLEAN -> booleanValue = variable.values.single { it.key == this.key }.booleanValue!!
                TypeConstants.DATE -> dateValue = variable.values.single { it.key == this.key }.dateValue!!
                TypeConstants.TIMESTAMP -> timestampValue = variable.values.single { it.key == this.key }.timestampValue!!
                TypeConstants.TIME -> timeValue = variable.values.single { it.key == this.key }.timeValue!!
                TypeConstants.BLOB -> blobValue = variable.values.single { it.key == this.key }.blobValue!!
                TypeConstants.FORMULA -> throw CustomJsonException("{}")
                else -> referencedVariable = variable.values.single { it.key == this.key }.referencedVariable!!
              }
            }
          }))
        }
      }
    } else {
      val forwardSymbols: JsonObject = JsonObject().apply {
        add("acc", JsonObject().apply {
          addProperty(SymbolConstants.SYMBOL_TYPE, if(typeAccumulator.type.name in primitiveTypes) typeAccumulator.type.name else TypeConstants.TEXT)
          when(typeAccumulator.type.name) {
            TypeConstants.TEXT -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialStringValue!!)
            TypeConstants.NUMBER -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialLongValue!!)
            TypeConstants.DECIMAL -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialDecimalValue!!)
            TypeConstants.BOOLEAN -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialBooleanValue!!)
            TypeConstants.DATE -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialDateValue!!.time)
            TypeConstants.TIMESTAMP -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialTimestampValue!!.time)
            TypeConstants.TIME -> addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.initialTimeValue!!.time)
            TypeConstants.BLOB -> addProperty(SymbolConstants.SYMBOL_VALUE, Base64.getEncoder().encodeToString(typeAccumulator.initialBlobValue!!.getBytes(1 ,typeAccumulator.initialBlobValue!!.length().toInt())))
            TypeConstants.FORMULA -> throw CustomJsonException("{}")
            else -> {
              addProperty(SymbolConstants.SYMBOL_VALUE, typeAccumulator.referencedVariable!!.name)
              add(SymbolConstants.SYMBOL_VALUES, getSymbolValues(variable = typeAccumulator.referencedVariable!!, symbolPaths = gson.fromJson(typeAccumulator.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), prefix = "acc.", excludeTopLevelFormulas = false))
            }
          }
        })
        add("it", JsonObject().apply {
          addProperty(SymbolConstants.SYMBOL_TYPE, TypeConstants.TEXT)
          addProperty(SymbolConstants.SYMBOL_VALUE, variable.name)
          add(SymbolConstants.SYMBOL_VALUES, getSymbolValues(variable = variable, symbolPaths = gson.fromJson(typeAccumulator.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), prefix = "it.", excludeTopLevelFormulas = false))
        })
      }
      val evaluatedValue = validateOrEvaluateExpression(expression = gson.fromJson(typeAccumulator.forwardExpression, JsonObject::class.java), symbols = forwardSymbols,
        mode = LispConstants.EVALUATE, expectedReturnType = if (typeAccumulator.type.name in primitiveTypes) typeAccumulator.type.name else TypeConstants.TEXT)
      variableAccumulatorJpaRepository.save(VariableAccumulator(typeAccumulator = typeAccumulator, level = 0, hash = forwardComputedHash, created = defaultTimestamp).apply {
        when(typeAccumulator.type.name) {
          TypeConstants.TEXT -> stringValue = evaluatedValue as String
          TypeConstants.NUMBER -> longValue = evaluatedValue as Long
          TypeConstants.DECIMAL -> decimalValue = evaluatedValue as BigDecimal
          TypeConstants.BOOLEAN -> booleanValue = evaluatedValue as Boolean
          TypeConstants.DATE -> dateValue = evaluatedValue as Date
          TypeConstants.TIMESTAMP -> timestampValue = evaluatedValue as Timestamp
          TypeConstants.TIME -> timeValue = evaluatedValue as Time
          TypeConstants.BLOB -> blobValue = BlobProxy.generateProxy(evaluatedValue as ByteArray)
          TypeConstants.FORMULA -> throw CustomJsonException("{}")
          else -> {
            referencedVariable = variableRepository.findVariable(type = typeAccumulator.type, name = evaluatedValue as String)
              ?: throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
          }
        }
      }).apply {
        values.addAll(valueAccumulatorJpaRepository.saveAll(typeAccumulator.keys.map { key ->
          ValueAccumulator(variableAccumulator = this, key = key, created = defaultTimestamp).apply {
            when(this.key.type.name) {
              TypeConstants.TEXT -> stringValue = variable.values.single { it.key == this.key }.stringValue!!
              TypeConstants.NUMBER -> longValue = variable.values.single { it.key == this.key }.longValue!!
              TypeConstants.DECIMAL -> decimalValue = variable.values.single { it.key == this.key }.decimalValue!!
              TypeConstants.BOOLEAN -> booleanValue = variable.values.single { it.key == this.key }.booleanValue!!
              TypeConstants.DATE -> dateValue = variable.values.single { it.key == this.key }.dateValue!!
              TypeConstants.TIMESTAMP -> timestampValue = variable.values.single { it.key == this.key }.timestampValue!!
              TypeConstants.TIME -> timeValue = variable.values.single { it.key == this.key }.timeValue!!
              TypeConstants.BLOB -> blobValue = variable.values.single { it.key == this.key }.blobValue!!
              TypeConstants.FORMULA -> throw CustomJsonException("{}")
              else -> referencedVariable = variable.values.single { it.key == this.key }.referencedVariable!!
            }
          }
        }))
      }
    }
  }

  fun deleteVariable(jsonParams: JsonObject, defaultTimestamp: Timestamp): Pair<Variable, TypePermission> {
    val typePermission: TypePermission = userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
      addProperty(OrganizationConstants.ORGANIZATION_ID, jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString)
      addProperty(OrganizationConstants.USERNAME, jsonParams.get(OrganizationConstants.USERNAME).asString)
      addProperty(OrganizationConstants.TYPE_NAME, jsonParams.get(OrganizationConstants.TYPE_NAME).asString)
    }, defaultTimestamp = defaultTimestamp)
    return if (!typePermission.deletable)
      throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.VARIABLE_NOT_REMOVED}}")
    else {
      val variable: Variable = variableRepository.findVariable(type = typePermission.type, name = jsonParams.get(VariableConstants.VARIABLE_NAME).asString)
        ?: throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.VARIABLE_NOT_FOUND}}")
      variable.variableUniqueness.forEach { variableUniqueness ->
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
      variable.type.uniqueConstraints.forEach { typeUniqueness ->
        typeUniqueness.accumulators.forEach { typeAccumulator ->
          val computedHash: String = computeHash(typeAccumulator.keys.sortedBy { it.id }.fold("") { acc, key ->
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
              TypeConstants.FORMULA -> throw CustomJsonException("{}")
              else -> value.referencedVariable!!.id
            }
          })
          val rootVariableAccumulator: VariableAccumulator? = variableAccumulatorRepository.findVariableAccumulator(typeAccumulator = typeAccumulator, level = 0, hash = computedHash)
          if (rootVariableAccumulator != null) {
            val matchingVariableAccumulator: VariableAccumulator? = getMatchingVariableAccumulator(variable = variable, typeAccumulator = typeAccumulator, variableAccumulator = rootVariableAccumulator)
            if (matchingVariableAccumulator != null) {
              val symbols: JsonObject = JsonObject().apply {
                add("acc", JsonObject().apply {
                  addProperty(SymbolConstants.SYMBOL_TYPE, if(typeAccumulator.type.name in primitiveTypes) typeAccumulator.type.name else TypeConstants.TEXT)
                  when(typeAccumulator.type.name) {
                    TypeConstants.TEXT -> addProperty(SymbolConstants.SYMBOL_VALUE, matchingVariableAccumulator.stringValue!!)
                    TypeConstants.NUMBER -> addProperty(SymbolConstants.SYMBOL_VALUE, matchingVariableAccumulator.longValue!!)
                    TypeConstants.DECIMAL -> addProperty(SymbolConstants.SYMBOL_VALUE, matchingVariableAccumulator.decimalValue!!)
                    TypeConstants.BOOLEAN -> addProperty(SymbolConstants.SYMBOL_VALUE, matchingVariableAccumulator.booleanValue!!)
                    TypeConstants.DATE -> addProperty(SymbolConstants.SYMBOL_VALUE, matchingVariableAccumulator.dateValue!!.time)
                    TypeConstants.TIMESTAMP -> addProperty(SymbolConstants.SYMBOL_VALUE, matchingVariableAccumulator.timestampValue!!.time)
                    TypeConstants.TIME -> addProperty(SymbolConstants.SYMBOL_VALUE, matchingVariableAccumulator.timeValue!!.time)
                    TypeConstants.BLOB -> addProperty(SymbolConstants.SYMBOL_VALUE, Base64.getEncoder().encodeToString(matchingVariableAccumulator.blobValue!!.getBytes(1 ,matchingVariableAccumulator.blobValue!!.length().toInt())))
                    TypeConstants.FORMULA -> throw CustomJsonException("{}")
                    else -> {
                      addProperty(SymbolConstants.SYMBOL_VALUE, matchingVariableAccumulator.referencedVariable!!.name)
                      add(SymbolConstants.SYMBOL_VALUES, getSymbolValues(variable = matchingVariableAccumulator.referencedVariable!!, symbolPaths = gson.fromJson(typeAccumulator.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), prefix = "acc.", excludeTopLevelFormulas = false))
                    }
                  }
                })
                add("it", JsonObject().apply {
                  addProperty(SymbolConstants.SYMBOL_TYPE, TypeConstants.TEXT)
                  addProperty(SymbolConstants.SYMBOL_VALUE, variable.name)
                  add(SymbolConstants.SYMBOL_VALUES, getSymbolValues(variable = variable, symbolPaths = gson.fromJson(typeAccumulator.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), prefix = "it.", excludeTopLevelFormulas = false))
                })
              }
              val evaluatedValue = validateOrEvaluateExpression(expression = gson.fromJson(typeAccumulator.backwardExpression, JsonObject::class.java), symbols = symbols,
                mode = LispConstants.EVALUATE, expectedReturnType = if (typeAccumulator.type.name in primitiveTypes) typeAccumulator.type.name else TypeConstants.TEXT)
              when(typeAccumulator.type.name) {
                TypeConstants.TEXT -> matchingVariableAccumulator.stringValue = evaluatedValue as String
                TypeConstants.NUMBER -> matchingVariableAccumulator.longValue = evaluatedValue as Long
                TypeConstants.DECIMAL -> matchingVariableAccumulator.decimalValue = evaluatedValue as BigDecimal
                TypeConstants.BOOLEAN -> matchingVariableAccumulator.booleanValue = evaluatedValue as Boolean
                TypeConstants.DATE -> matchingVariableAccumulator.dateValue = evaluatedValue as Date
                TypeConstants.TIMESTAMP -> matchingVariableAccumulator.timestampValue = evaluatedValue as Timestamp
                TypeConstants.TIME -> matchingVariableAccumulator.timeValue = evaluatedValue as Time
                TypeConstants.BLOB -> matchingVariableAccumulator.blobValue = BlobProxy.generateProxy(evaluatedValue as ByteArray)
                TypeConstants.FORMULA -> throw CustomJsonException("{}")
                else -> {
                  matchingVariableAccumulator.referencedVariable = variableRepository.findVariable(type = typeAccumulator.type, name = evaluatedValue as String)
                    ?: throw CustomJsonException("{${VariableConstants.VARIABLE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
                }
              }
              variableAccumulatorJpaRepository.save(matchingVariableAccumulator)
            }
          }
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
      addProperty(OrganizationConstants.ORGANIZATION_ID, variable.type.organization.id)
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
