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
import com.pibity.erp.commons.constants.GLOBAL_TYPE
import com.pibity.erp.commons.constants.PermissionConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.utils.*
import com.pibity.erp.entities.*
import com.pibity.erp.entities.permission.TypePermission
import com.pibity.erp.repositories.jpa.ValueJpaRepository
import com.pibity.erp.repositories.jpa.VariableAssertionJpaRepository
import com.pibity.erp.repositories.jpa.VariableJpaRepository
import com.pibity.erp.repositories.jpa.VariableListJpaRepository
import com.pibity.erp.repositories.query.VariableRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.Timestamp

@Service
class VariableService(
    val variableRepository: VariableRepository,
    val variableJpaRepository: VariableJpaRepository,
    val variableListJpaRepository: VariableListJpaRepository,
    val userService: UserService,
    val valueJpaRepository: ValueJpaRepository,
    val variableAssertionJpaRepository: VariableAssertionJpaRepository
) {

  fun executeQueue(jsonParams: JsonObject): JsonArray {
    val multiLevelQueue = JsonArray()
    for (queue in jsonParams.get("queue").asJsonArray)
      if (!queue.isJsonArray)
        throw CustomJsonException("{queue: 'Unexpected value for parameter'}")
    for (queue in jsonParams.get("queue").asJsonArray) {
      try {
        multiLevelQueue.apply {
          add(mutateVariablesAtomically(jsonParams = queue.asJsonArray, orgId = jsonParams.get("orgId").asLong, username = jsonParams.get("username").asString))
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
      throw CustomJsonException(gson.fromJson(exception.message, JsonObject::class.java).apply { addProperty("step", step) }.toString())
    }
    return mutatedVariables
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createVariable(jsonParams: JsonObject, variableSuperList: VariableList? = null, variableSubList: VariableList? = null, variableTypePermission: TypePermission? = null): Pair<Variable, TypePermission> {
    val superList: VariableList
    val type: Type
    val typePermission: TypePermission
    if (jsonParams.has("context?")) {
      superList = variableSuperList ?: variableListJpaRepository.getById(jsonParams.get("context?").asLong)
          ?: throw CustomJsonException("{context: 'Unable to determine context'}")
      type = superList.listType.type
      typePermission = variableTypePermission
          ?: userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
            addProperty("orgId", jsonParams.get("orgId").asString)
            addProperty("username", jsonParams.get("username").asString)
            addProperty("superTypeName", type.superTypeName)
            addProperty("typeName", type.name)
          })
    } else {
      typePermission = variableTypePermission
          ?: userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
            addProperty("orgId", jsonParams.get("orgId").asString)
            addProperty("username", jsonParams.get("username").asString)
            addProperty("typeName", jsonParams.get("typeName").asString)
          })
      type = typePermission.type
      superList = variableSuperList ?: type.superList!!
    }
    if (typePermission.type.superTypeName == GLOBAL_TYPE && !typePermission.creatable)
      throw CustomJsonException("{error: 'Unauthorized Access'}")
    val variableName: String = jsonParams.get("variableName").asString
    val values: JsonObject = validateVariableValues(values = jsonParams.get("values").asJsonObject, typePermission = typePermission)
    var variable = Variable(superList = superList, type = type, name = if (type.superTypeName == GLOBAL_TYPE && type.autoAssignId) (type.autoIncrementId + 1).toString() else variableName, autoGeneratedId = type.autoIncrementId + 1, subList = variableSubList
        ?: variableListJpaRepository.save(VariableList(listType = superList.listType)))
    variable.type.autoIncrementId += 1
    variable.type.variableCount += 1
    // Process non-formula type values
    typePermission.keyPermissions.filter { it.key.type.name != TypeConstants.FORMULA }.forEach { keyPermission ->
      val key = keyPermission.key
      when (key.type.name) {
        TypeConstants.TEXT -> variable.values.add(Value(variable = variable, key = key, stringValue = values.get(key.name).asString))
        TypeConstants.NUMBER -> variable.values.add(Value(variable = variable, key = key, longValue = values.get(key.name).asLong))
        TypeConstants.DECIMAL -> variable.values.add(Value(variable = variable, key = key, decimalValue = values.get(key.name).asBigDecimal))
        TypeConstants.BOOLEAN -> variable.values.add(Value(variable = variable, key = key, booleanValue = values.get(key.name).asBoolean))
        TypeConstants.DATE -> variable.values.add(Value(variable = variable, key = key, dateValue = java.sql.Date(dateFormat.parse(values.get(key.name).asString).time)))
        TypeConstants.TIME -> variable.values.add(Value(variable = variable, key = key, timeValue = java.sql.Time(values.get(key.name).asLong)))
        TypeConstants.TIMESTAMP -> variable.values.add(Value(variable = variable, key = key, timestampValue = Timestamp(values.get(key.name).asLong)))
        TypeConstants.LIST -> {
          val jsonArray: JsonArray = values.get(key.name).asJsonArray
          val list: VariableList = try {
            variableListJpaRepository.save(VariableList(listType = key.list!!))
          } catch (exception: Exception) {
            throw CustomJsonException("{${key.name}: 'Unable to create List'}")
          }
          for (ref in jsonArray.iterator()) {
            if (key.list!!.type.superTypeName == GLOBAL_TYPE) {
              val referencedVariable: Variable = variableRepository.findByTypeAndName(superList = key.list!!.type.superList!!, type = key.list!!.type, name = ref.asString)
                  ?: throw CustomJsonException("{${key.name}: 'Unable to find referenced global variable'}")
              referencedVariable.referenceCount += 1
              if (referencedVariable.type.multiplicity != 0L && referencedVariable.referenceCount > referencedVariable.type.multiplicity)
                throw CustomJsonException("{${key.name}: 'Unable to reference variable due to variable's reference limit'}")
              if (!referencedVariable.active)
                throw CustomJsonException("{${key.name}: 'Unable to reference variable as it is inactive'}")
              list.variables.add(referencedVariable)
              list.size += 1
            } else {
              if ((key.parentType.superTypeName == GLOBAL_TYPE && key.parentType.name == key.list!!.type.superTypeName)
                  || (key.parentType.superTypeName != GLOBAL_TYPE && key.parentType.superTypeName == key.list!!.type.superTypeName)) {
                val (referencedVariable: Variable, _) = try {
                  createVariable(JsonObject().apply {
                    addProperty("variableName", ref.asJsonObject.get("variableName").asString)
                    add("values", ref.asJsonObject.get("values").asJsonObject)
                  }, variableTypePermission = keyPermission.referencedTypePermission!!,
                      variableSuperList = list,
                      variableSubList = variableListJpaRepository.save(VariableList(listType = list.listType)))
                } catch (exception: CustomJsonException) {
                  throw CustomJsonException("{${key.name}: ${exception.message}}")
                }
                referencedVariable.referenceCount += 1
                list.variables.add(referencedVariable)
                list.size += 1
              } else {
                val referencedVariable: Variable = variableRepository.findVariable(organizationId = type.organization.id, superTypeName = key.list!!.type.superTypeName, typeName = key.list!!.type.name, superList = ref.asJsonObject.get("context").asLong, name = ref.asJsonObject.get("variableName").asString)
                    ?: throw CustomJsonException("{${key.name}: 'Unable to find referenced local field of global variable'}")
                referencedVariable.referenceCount += 1
                if (referencedVariable.type.multiplicity != 0L && referencedVariable.referenceCount > referencedVariable.type.multiplicity)
                  throw CustomJsonException("{${key.name}: 'Unable to reference variable due to variable's reference limit'}")
                if (!referencedVariable.active)
                  throw CustomJsonException("{${key.name}: 'Unable to reference variable as it is inactive'}")
                list.variables.add(referencedVariable)
                list.size += 1
              }
            }
          }
          if (list.size < list.listType.min)
            throw CustomJsonException("{${key.name}: 'List cannot contain less than ${list.listType.min} variables'}")
          if (list.listType.max != 0 && list.size > list.listType.max)
            throw CustomJsonException("{${key.name}: 'List cannot contain more than ${list.listType.max} variables'}")
          variable.values.add(Value(variable = variable, key = key, list = list))
        }
        else -> {
          if (key.type.superTypeName == GLOBAL_TYPE) {
            val referencedVariable: Variable = variableRepository.findByTypeAndName(superList = key.type.superList!!, type = key.type, name = values.get(key.name).asString)
                ?: throw CustomJsonException("{${key.name}: 'Unable to find referenced global variable'}")
            referencedVariable.referenceCount += 1
            if (referencedVariable.type.multiplicity != 0L && referencedVariable.referenceCount > referencedVariable.type.multiplicity)
              throw CustomJsonException("{${key.name}: 'Unable to reference variable due to variable's reference limit'}")
            if (!referencedVariable.active)
              throw CustomJsonException("{${key.name}: 'Unable to reference variable as it is inactive'}")
            variable.values.add(Value(variable = variable, key = key, referencedVariable = referencedVariable))
          } else {
            if ((key.parentType.superTypeName == GLOBAL_TYPE && key.parentType.name == key.type.superTypeName)
                || (key.parentType.superTypeName != GLOBAL_TYPE && key.parentType.superTypeName == key.type.superTypeName)) {
              val (referencedVariable: Variable, _) = try {
                createVariable(JsonObject().apply {
                  addProperty("variableName", "")
                  add("values", values.get(key.name).asJsonObject.get("values").asJsonObject)
                }, variableTypePermission = keyPermission.referencedTypePermission!!, variableSuperList = variable.subList, variableSubList = variable.subList)
              } catch (exception: CustomJsonException) {
                throw CustomJsonException("{${key.name}: ${exception.message}}")
              }
              referencedVariable.referenceCount += 1
              variable.values.add(Value(variable = variable, key = key, referencedVariable = referencedVariable))
            } else {
              val referencedVariable: Variable = variableRepository.findVariable(organizationId = type.organization.id, superTypeName = key.type.superTypeName, typeName = key.type.name, superList = values.get(key.name).asJsonObject.get("context").asLong, name = values.get(key.name).asJsonObject.get("variableName").asString)
                  ?: throw CustomJsonException("{${key.name}: 'Unable to find referenced local field of global variable'}")
              referencedVariable.referenceCount += 1
              if (referencedVariable.type.multiplicity != 0L && referencedVariable.referenceCount > referencedVariable.type.multiplicity)
                throw CustomJsonException("{${key.name}: 'Unable to reference variable due to variable's reference limit'}")
              if (!referencedVariable.active)
                throw CustomJsonException("{${key.name}: 'Unable to reference variable as it is inactive'}")
              variable.values.add(Value(variable = variable, key = key, referencedVariable = referencedVariable))
            }
          }
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
            stringValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(key.formula!!.expression, JsonObject::class.java).apply { addProperty("expectedReturnType", key.formula!!.returnType.name) },
                symbols = getSymbolValuesAndUpdateDependencies(variable = variable, symbolPaths = gson.fromJson(key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), valueDependencies = valueDependencies, variableDependencies = variableDependencies), mode = "evaluate") as String)
        TypeConstants.NUMBER -> Value(variable = variable, key = key,
            longValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(key.formula!!.expression, JsonObject::class.java).apply { addProperty("expectedReturnType", key.formula!!.returnType.name) },
                symbols = getSymbolValuesAndUpdateDependencies(variable = variable, symbolPaths = gson.fromJson(key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), valueDependencies = valueDependencies, variableDependencies = variableDependencies), mode = "evaluate") as Long)
        TypeConstants.DECIMAL -> Value(variable = variable, key = key,
            decimalValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(key.formula!!.expression, JsonObject::class.java).apply { addProperty("expectedReturnType", key.formula!!.returnType.name) },
                symbols = getSymbolValuesAndUpdateDependencies(variable = variable, symbolPaths = gson.fromJson(key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), valueDependencies = valueDependencies, variableDependencies = variableDependencies), mode = "evaluate") as BigDecimal)
        TypeConstants.BOOLEAN -> Value(variable = variable, key = key,
            booleanValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(key.formula!!.expression, JsonObject::class.java).apply { addProperty("expectedReturnType", key.formula!!.returnType.name) },
                symbols = getSymbolValuesAndUpdateDependencies(variable = variable, symbolPaths = gson.fromJson(key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), valueDependencies = valueDependencies, variableDependencies = variableDependencies), mode = "evaluate") as Boolean)
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
    if (variable.type.hasAssertions) {
      for (typeAssertion in variable.type.typeAssertions) {
        val valueDependencies: MutableSet<Value> = mutableSetOf()
        val variableDependencies: MutableSet<Variable> = mutableSetOf()
        val variableAssertion = VariableAssertion(variable = variable,
            typeAssertion = typeAssertion,
            result = validateOrEvaluateExpression(jsonParams = gson.fromJson(typeAssertion.expression, JsonObject::class.java).apply { addProperty("expectedReturnType", TypeConstants.BOOLEAN) },
                symbols = getSymbolValuesAndUpdateDependencies(variable = variable, symbolPaths = gson.fromJson(typeAssertion.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), valueDependencies = valueDependencies, variableDependencies = variableDependencies, symbolsForFormula = false), mode = "evaluate") as Boolean)
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
  fun updateVariable(jsonParams: JsonObject, referencedKeyVariable: Variable? = null, variableTypePermission: TypePermission? = null, variableSuperList: VariableList? = null): Quadruple<Variable, TypePermission, Map<Value, MutableSet<Value>>, Map<VariableAssertion, MutableSet<Value>>> {
    val superList: VariableList
    val type: Type
    val typePermission: TypePermission
    if (jsonParams.has("context?")) {
      superList = variableSuperList ?: variableListJpaRepository.getById(jsonParams.get("context?").asLong)
          ?: throw CustomJsonException("{context: 'Unable to determine context'}")
      type = superList.listType.type
      typePermission = variableTypePermission
          ?: userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
            addProperty("orgId", jsonParams.get("orgId").asString)
            addProperty("username", jsonParams.get("username").asString)
            addProperty("superTypeName", type.superTypeName)
            addProperty("typeName", type.name)
          })
    } else {
      typePermission = variableTypePermission
          ?: userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
            addProperty("orgId", jsonParams.get("orgId").asString)
            addProperty("username", jsonParams.get("username").asString)
            addProperty("typeName", jsonParams.get("typeName").asString)
          })
      type = typePermission.type
      superList = variableSuperList ?: type.superList!!
    }
    val variable: Variable = referencedKeyVariable
        ?: variableRepository.findVariable(organizationId = type.organization.id, superTypeName = type.superTypeName, typeName = type.name, superList = superList.id, name = jsonParams.get("variableName").asString)
        ?: throw CustomJsonException("{variableName: 'Unable to find referenced variable'}")
    if (jsonParams.has("active?"))
      variable.active = jsonParams.get("active?").asBoolean
    val values: JsonObject = if (jsonParams.has("values")) validateUpdatedVariableValues(values = jsonParams.get("values").asJsonObject, typePermission = typePermission) else JsonObject()
    val dependentFormulaValues: MutableMap<Value, MutableSet<Value>> = mutableMapOf()
    val dependentAssertions: MutableMap<VariableAssertion, MutableSet<Value>> = mutableMapOf()
    // Process non-formula type values
    variable.values.filter { it.key.type.name != TypeConstants.FORMULA }.forEach { value ->
      if (values.has(value.key.name)) {
        when (value.key.type.name) {
          TypeConstants.TEXT -> {
            value.stringValue = values.get(value.key.name).asString
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
          TypeConstants.LIST -> {
            val listValues: JsonObject = values.get(value.key.name).asJsonObject
            if (value.list!!.listType.type.superTypeName == GLOBAL_TYPE) {
              if (listValues.has("add")) {
                listValues.get("add").asJsonArray.forEach {
                  val referencedVariable = variableRepository.findVariable(organizationId = type.organization.id, superTypeName = value.list!!.listType.type.superTypeName, typeName = value.list!!.listType.type.name, superList = value.list!!.listType.type.superList!!.id, name = it.asString)
                      ?: throw CustomJsonException("{${value.key.name}: {add: 'Unable to insert ${it.asString} referenced in list'}}")
                  if (!value.list!!.variables.contains(referencedVariable)) {
                    referencedVariable.referenceCount += 1
                    if (referencedVariable.type.multiplicity != 0L && referencedVariable.referenceCount > referencedVariable.type.multiplicity)
                      throw CustomJsonException("{${value.key.name}: 'Unable to reference variable due to variable's reference limit'}")
                    if (!referencedVariable.active)
                      throw CustomJsonException("{${value.key.name}: 'Unable to reference variable as it is inactive'}")
                    value.list!!.variables.add(referencedVariable)
                    value.list!!.size += 1
                  } else throw CustomJsonException("{${value.key.name}: {add: '${it.asString} is already present in list'}}")
                }
              }
              if (listValues.has("remove")) {
                listValues.get("remove").asJsonArray.forEach {
                  val referencedVariable = variableRepository.findVariable(organizationId = type.organization.id, superTypeName = value.list!!.listType.type.superTypeName, typeName = value.list!!.listType.type.name, superList = value.list!!.listType.type.superList!!.id, name = it.asString)
                      ?: throw CustomJsonException("{${value.key.name}: {remove: 'Unable to remove ${it.asString} referenced in list'}}")
                  if (value.list!!.variables.contains(referencedVariable)) {
                    referencedVariable.referenceCount -= 1
                    value.list!!.variables.remove(referencedVariable)
                    value.list!!.size -= 1
                    variableJpaRepository.save(referencedVariable)
                  } else throw CustomJsonException("{${value.key.name}: {remove: '${it.asString} is not present in list'}}")
                }
              }
            } else {
              if ((value.key.parentType.superTypeName == GLOBAL_TYPE && value.key.parentType.name == value.key.list!!.type.superTypeName)
                  || (value.key.parentType.superTypeName != GLOBAL_TYPE && value.key.parentType.superTypeName == value.key.list!!.type.superTypeName)) {
                if (listValues.has("add")) {
                  listValues.get("add").asJsonArray.forEach {
                    val (referencedVariable: Variable, _) = try {
                      createVariable(jsonParams = it.asJsonObject, variableTypePermission = typePermission.keyPermissions.single { keyPermission -> keyPermission.key == value.key }.referencedTypePermission!!, variableSuperList = value.list!!, variableSubList = variableListJpaRepository.save(VariableList(listType = value.list!!.listType)))
                    } catch (exception: CustomJsonException) {
                      throw CustomJsonException("{${value.key.name}: {${it.asJsonObject.get("variableName").asString}: {add: 'Unable to add variable to list'}}}")
                    }
                    if (!value.list!!.variables.contains(referencedVariable)) {
                      referencedVariable.referenceCount += 1
                      if (referencedVariable.type.multiplicity != 0L && referencedVariable.referenceCount > referencedVariable.type.multiplicity)
                        throw CustomJsonException("{${value.key.name}: 'Unable to reference variable due to variable's reference limit'}")
                      value.list!!.variables.add(referencedVariable)
                      value.list!!.size += 1
                    } else throw CustomJsonException("{${value.key.name}: {add: '${it.asJsonObject.get("variableName").asString} is already present in list'}}")
                  }
                }
                if (listValues.has("remove")) {
                  listValues.get("remove").asJsonArray.forEach {
                    val referencedVariable = variableRepository.findVariable(organizationId = type.organization.id, superTypeName = value.list!!.listType.type.superTypeName, typeName = value.list!!.listType.type.name, superList = value.list!!.id, name = it.asString)
                        ?: throw CustomJsonException("{${value.key.name}: {remove: 'Unable to find ${it.asString} referenced in list'}}")
                    if (value.list!!.variables.contains(referencedVariable)) {
                      referencedVariable.referenceCount -= 1
                      value.list!!.variables.remove(referencedVariable)
                      value.list!!.size -= 1
                      if (referencedVariable.referenceCount == 0L) {
                        referencedVariable.type.variableCount -= 1
                        variableJpaRepository.delete(referencedVariable)
                      } else
                        variableJpaRepository.save(referencedVariable)
                    } else throw CustomJsonException("{${value.key.name}: {remove: '${it.asString} is not present in list'}}")
                  }
                }
                if (listValues.has("update")) {
                  listValues.get("update").asJsonArray.forEach {
                    val variableToUpdate: Variable = variableRepository.findVariable(organizationId = type.organization.id, superTypeName = value.list!!.listType.type.superTypeName, typeName = value.list!!.listType.type.name, superList = value.list!!.id, name = it.asJsonObject.get("variableName").asString)
                        ?: throw CustomJsonException("{${value.key.name}: {update: 'Unable to find referenced variable in list'}}")
                    value.list!!.variables.remove(variableToUpdate)
                    val updatedVariable: Variable = try {
                      val (subVariable, _, subDependentFormulaValues, subDependentAssertions) = updateVariable(jsonParams = it.asJsonObject, variableTypePermission = typePermission.keyPermissions.single { keyPermission -> keyPermission.key.name == value.key.name }.referencedTypePermission!!, variableSuperList = value.list!!)
                      subDependentFormulaValues.forEach { (k, v) ->
                        if (!dependentFormulaValues.containsKey(k))
                          dependentFormulaValues[k] = v
                        else
                          dependentFormulaValues[k]!!.addAll(v)
                        // Test below
                        if (k.key.isAssertionDependency) {
                          k.dependentVariableAssertions.forEach { variableAssertion ->
                            if (!dependentAssertions.containsKey(variableAssertion))
                              dependentAssertions[variableAssertion] = mutableSetOf(k)
                            else
                              dependentAssertions[variableAssertion]!!.add(k)
                          }
                        }
                      }
                      subDependentAssertions.forEach { (k, v) ->
                        if (!dependentAssertions.containsKey(k))
                          dependentAssertions[k] = v
                        else
                          dependentAssertions[k]!!.addAll(v)
                      }
                      subVariable
                    } catch (exception: CustomJsonException) {
                      throw CustomJsonException("{${value.key.name}: {update: ${exception.message}}}")
                    }
                    value.list!!.variables.add(updatedVariable)
                  }
                }
              } else {
                if (listValues.has("add")) {
                  listValues.get("add").asJsonArray.forEach {
                    val referencedVariable = variableRepository.findVariable(organizationId = type.organization.id, superTypeName = value.list!!.listType.type.superTypeName, typeName = value.list!!.listType.type.name, superList = it.asJsonObject.get("context").asLong, name = it.asJsonObject.get("variableName").asString)
                        ?: throw CustomJsonException("{${value.key.name}: {add: 'Unable to insert ${it.asJsonObject.get("variableName").asString} referenced in list'}}")
                    if (!value.list!!.variables.contains(referencedVariable)) {
                      referencedVariable.referenceCount += 1
                      if (referencedVariable.type.multiplicity != 0L && referencedVariable.referenceCount > referencedVariable.type.multiplicity)
                        throw CustomJsonException("{${value.key.name}: 'Unable to reference variable due to variable's reference limit'}")
                      if (!referencedVariable.active)
                        throw CustomJsonException("{${value.key.name}: 'Unable to reference variable as it is inactive'}")
                      value.list!!.variables.add(referencedVariable)
                      value.list!!.size += 1
                    } else throw CustomJsonException("{${value.key.name}: {add: '${it.asJsonObject.get("variableName").asString} is already present in list'}}")
                  }
                }
                if (listValues.has("remove")) {
                  listValues.get("remove").asJsonArray.forEach {
                    val referencedVariable = variableRepository.findVariable(organizationId = type.organization.id, superTypeName = value.list!!.listType.type.superTypeName, typeName = value.list!!.listType.type.name, superList = it.asJsonObject.get("context").asLong, name = it.asJsonObject.get("variableName").asString)
                        ?: throw CustomJsonException("{${value.key.name}: {remove: 'Unable to remove ${it.asJsonObject.get("variableName").asString} referenced in list'}}")
                    if (value.list!!.variables.contains(referencedVariable)) {
                      referencedVariable.referenceCount -= 1
                      value.list!!.variables.remove(referencedVariable)
                      value.list!!.size -= 1
                      variableJpaRepository.save(referencedVariable)
                    } else throw CustomJsonException("{${value.key.name}: {remove: '${it.asJsonObject.get("variableName").asString} is not present in list'}}")
                  }
                }
              }
            }
            if (value.list!!.size < value.list!!.listType.min)
              throw CustomJsonException("{${value.key.name}: 'List cannot contain less than ${value.list!!.listType.min} variables'}")
            if (value.list!!.listType.max != 0 && value.list!!.size > value.list!!.listType.max)
              throw CustomJsonException("{${value.key.name}: 'List cannot contain more than ${value.list!!.listType.max} variables'}")
          }
          else -> {
            if (value.key.type.superTypeName == GLOBAL_TYPE) {
              value.referencedVariable!!.referenceCount -= 1
              variableJpaRepository.save(value.referencedVariable!!)
              val referencedVariable = variableRepository.findVariable(organizationId = type.organization.id, superTypeName = value.key.type.superTypeName, typeName = value.key.type.name, superList = value.key.type.superList!!.id, name = values.get(value.key.name).asString)
                  ?: throw CustomJsonException("{${value.key.name}: 'Unable to find referenced variable ${values.get(value.key.name).asString}'}")
              referencedVariable.referenceCount += 1
              if (referencedVariable.type.multiplicity != 0L && referencedVariable.referenceCount > referencedVariable.type.multiplicity)
                throw CustomJsonException("{${value.key.name}: 'Unable to reference variable due to variable's reference limit'}")
              if (!referencedVariable.active)
                throw CustomJsonException("{${value.key.name}: 'Unable to reference variable as it is inactive'}")
              value.referencedVariable = referencedVariable
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
            } else {
              if ((value.key.parentType.superTypeName == GLOBAL_TYPE && value.key.parentType.name == value.key.type.superTypeName)
                  || (value.key.parentType.superTypeName != GLOBAL_TYPE && value.key.parentType.superTypeName == value.key.type.superTypeName)) {
                value.referencedVariable = try {
                  val (subVariable, _, subDependentFormulaValues, subDependentAssertions) = updateVariable(jsonParams = values.get(value.key.name).asJsonObject, referencedKeyVariable = value.referencedVariable!!, variableTypePermission = typePermission.keyPermissions.single { keyPermission -> keyPermission.key.name == value.key.name }.referencedTypePermission!!, variableSuperList = value.referencedVariable!!.superList)
                  subDependentFormulaValues.forEach { (k, v) ->
                    if (!dependentFormulaValues.containsKey(k))
                      dependentFormulaValues[k] = v
                    else
                      dependentFormulaValues[k]!!.addAll(v)
                    // Test below
                    if (k.key.isAssertionDependency) {
                      k.dependentVariableAssertions.forEach { variableAssertion ->
                        if (!dependentAssertions.containsKey(variableAssertion))
                          dependentAssertions[variableAssertion] = mutableSetOf(k)
                        else
                          dependentAssertions[variableAssertion]!!.add(k)
                      }
                    }
                  }
                  subDependentAssertions.forEach { (k, v) ->
                    if (!dependentAssertions.containsKey(k))
                      dependentAssertions[k] = v
                    else
                      dependentAssertions[k]!!.addAll(v)
                  }
                  subVariable
                } catch (exception: CustomJsonException) {
                  throw CustomJsonException("{${value.key.name}: ${exception.message}}")
                }
              } else {
                value.referencedVariable!!.referenceCount -= 1
                variableJpaRepository.save(value.referencedVariable!!)
                val referencedVariable = variableRepository.findVariable(organizationId = type.organization.id, superTypeName = value.key.type.superTypeName, typeName = value.key.type.name, superList = values.get(value.key.name).asJsonObject.get("context").asLong, name = values.get(value.key.name).asJsonObject.get("variableName").asString)
                    ?: throw CustomJsonException("{${value.key.name}: 'Unable to find referenced variable ${values.get(value.key.name).asJsonObject.get("variableName").asString}'}")
                referencedVariable.referenceCount += 1
                if (referencedVariable.type.multiplicity != 0L && referencedVariable.referenceCount > referencedVariable.type.multiplicity)
                  throw CustomJsonException("{${value.key.name}: 'Unable to reference variable due to variable's reference limit'}")
                if (!referencedVariable.active)
                  throw CustomJsonException("{${value.key.name}: 'Unable to reference variable as it is inactive'}")
                value.referencedVariable = referencedVariable
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
      }
    }
    variable.values.filter { it.key.type.name == TypeConstants.FORMULA }.forEach { value ->
      if (dependentFormulaValues.contains(value)) {
        when (value.key.formula!!.returnType.name) {
          TypeConstants.TEXT -> {
            val evaluatedValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(value.key.formula!!.expression, JsonObject::class.java).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
                symbols = getSymbolValues(variable = variable, symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet()), mode = "evaluate") as String
            if (value.stringValue != evaluatedValue) {
              value.stringValue = evaluatedValue
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
            val evaluatedValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(value.key.formula!!.expression, JsonObject::class.java).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
                symbols = getSymbolValues(variable = variable, symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet()), mode = "evaluate") as Long
            if (value.longValue != evaluatedValue) {
              value.longValue = evaluatedValue
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
            val evaluatedValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(value.key.formula!!.expression, JsonObject::class.java).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
                symbols = getSymbolValues(variable = variable, symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet()), mode = "evaluate") as BigDecimal
            if (value.decimalValue != evaluatedValue) {
              value.decimalValue = evaluatedValue
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
            val evaluatedValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(value.key.formula!!.expression, JsonObject::class.java).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
                symbols = getSymbolValues(variable = variable, symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet()), mode = "evaluate") as Boolean
            if (value.booleanValue != evaluatedValue) {
              value.booleanValue = evaluatedValue
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
    if (variable.type.superTypeName == GLOBAL_TYPE) {
      if (dependentFormulaValues.isNotEmpty())
        recomputeDependentFormulaValues(dependentFormulaValues, dependentAssertions)
      else if (dependentAssertions.isNotEmpty())
        evaluateAssertions(dependentAssertions)
    }
    return Quadruple(variable, typePermission, dependentFormulaValues, dependentAssertions)
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun recomputeDependentFormulaValues(dependentFormulaValues: MutableMap<Value, MutableSet<Value>>, dependentAssertions: Map<VariableAssertion, MutableSet<Value>>) {
    val higherDependentFormulaValues: MutableMap<Value, MutableSet<Value>> = mutableMapOf()
    dependentFormulaValues.forEach { (value, dependencies) ->
      when (value.key.formula!!.returnType.name) {
        TypeConstants.TEXT -> {
          val reconstructDependencies: Boolean = dependencies.fold(false) { acc, v -> acc || v.key.isVariableDependency }
          if (reconstructDependencies) {
            val valueDependencies: MutableSet<Value> = mutableSetOf()
            val variableDependencies: MutableSet<Variable> = mutableSetOf()
            value.stringValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(value.key.formula!!.expression, JsonObject::class.java).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
                symbols = getSymbolValuesAndUpdateDependencies(variable = value.variable, symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), valueDependencies = valueDependencies, variableDependencies = variableDependencies), mode = "evaluate") as String
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
            val evaluatedValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(value.key.formula!!.expression, JsonObject::class.java).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
                symbols = getSymbolValues(variable = value.variable, symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet()), mode = "evaluate") as String
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
          val reconstructDependencies: Boolean = dependencies.fold(false) { acc, v -> acc || v.key.isVariableDependency }
          if (reconstructDependencies) {
            val valueDependencies: MutableSet<Value> = mutableSetOf()
            val variableDependencies: MutableSet<Variable> = mutableSetOf()
            value.longValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(value.key.formula!!.expression, JsonObject::class.java).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
                symbols = getSymbolValuesAndUpdateDependencies(variable = value.variable, symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), valueDependencies = valueDependencies, variableDependencies = variableDependencies), mode = "evaluate") as Long
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
            val evaluatedValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(value.key.formula!!.expression, JsonObject::class.java).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
                symbols = getSymbolValues(variable = value.variable, symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet()), mode = "evaluate") as Long
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
          val reconstructDependencies: Boolean = dependencies.fold(false) { acc, v -> acc || v.key.isVariableDependency }
          if (reconstructDependencies) {
            val valueDependencies: MutableSet<Value> = mutableSetOf()
            val variableDependencies: MutableSet<Variable> = mutableSetOf()
            value.decimalValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(value.key.formula!!.expression, JsonObject::class.java).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
                symbols = getSymbolValuesAndUpdateDependencies(variable = value.variable, symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), valueDependencies = valueDependencies, variableDependencies = variableDependencies), mode = "evaluate") as BigDecimal
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
            val evaluatedValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(value.key.formula!!.expression, JsonObject::class.java).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
                symbols = getSymbolValues(variable = value.variable, symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet()), mode = "evaluate") as BigDecimal
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
          val reconstructDependencies: Boolean = dependencies.fold(false) { acc, v -> acc || v.key.isVariableDependency }
          if (reconstructDependencies) {
            val valueDependencies: MutableSet<Value> = mutableSetOf()
            val variableDependencies: MutableSet<Variable> = mutableSetOf()
            value.booleanValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(value.key.formula!!.expression, JsonObject::class.java).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
                symbols = getSymbolValuesAndUpdateDependencies(variable = value.variable, symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), valueDependencies = valueDependencies, variableDependencies = variableDependencies), mode = "evaluate") as Boolean
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
            val evaluatedValue = validateOrEvaluateExpression(jsonParams = gson.fromJson(value.key.formula!!.expression, JsonObject::class.java).apply { addProperty("expectedReturnType", value.key.formula!!.returnType.name) },
                symbols = getSymbolValues(variable = value.variable, symbolPaths = gson.fromJson(value.key.formula!!.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet()), mode = "evaluate") as Boolean
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
      variableAssertion.result = validateOrEvaluateExpression(jsonParams = gson.fromJson(variableAssertion.typeAssertion.expression, JsonObject::class.java).apply { addProperty("expectedReturnType", TypeConstants.BOOLEAN) },
          symbols = getSymbolValuesAndUpdateDependencies(variable = variableAssertion.variable, symbolPaths = gson.fromJson(variableAssertion.typeAssertion.symbolPaths, JsonArray::class.java).map { it.asString }.toMutableSet(), valueDependencies = valueDependencies, variableDependencies = variableDependencies, symbolsForFormula = false), mode = "evaluate") as Boolean
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
    val superList: VariableList
    val type: Type
    val typePermission: TypePermission
    if (jsonParams.has("context?")) {
      superList = variableListJpaRepository.getById(jsonParams.get("context?").asLong)
          ?: throw CustomJsonException("{context: 'Unable to determine context'}")
      type = superList.listType.type
      typePermission = userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
        addProperty("orgId", jsonParams.get("orgId").asString)
        addProperty("username", jsonParams.get("username").asString)
        addProperty("superTypeName", type.superTypeName)
        addProperty("typeName", type.name)
      })
    } else {
      typePermission = userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
        addProperty("orgId", jsonParams.get("orgId").asString)
        addProperty("username", jsonParams.get("username").asString)
        addProperty("typeName", jsonParams.get("typeName").asString)
      })
      type = typePermission.type
      superList = type.superList!!
    }
    val variable: Variable = variableRepository.findVariable(organizationId = type.organization.id, superTypeName = type.superTypeName, typeName = type.name, superList = superList.id, name = jsonParams.get("variableName").asString)
        ?: throw CustomJsonException("{variableName: 'Unable to find referenced variable'}")
    if (!typePermission.deletable)
      throw CustomJsonException("{variableName: 'Unable to delete referenced variable'}")
    return try {
      variableJpaRepository.delete(variable)
      variableListJpaRepository.save(superList.apply { size -= 1 })
      Pair(variable, typePermission)
    } catch (exception: Exception) {
      throw CustomJsonException("{variableName: 'Unable to delete referenced variable'}")
    }
  }

  fun serialize(variable: Variable, typePermission: TypePermission, username: String): JsonObject {
    val json = JsonObject()
    if (variable.type.superTypeName == "Any") {
      json.addProperty("organization", variable.type.organization.id)
      json.addProperty("typeName", variable.type.name)
      json.addProperty("active", variable.active)
    } else
      json.addProperty("context", variable.superList.id)
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
        TypeConstants.LIST -> {
          if (value.list!!.listType.type.superTypeName == "Any") {
            if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS) {
              val jsonArray = JsonArray()
              value.list!!.variables.forEach {
                jsonArray.add(it.name)
              }
              jsonValues.add(value.key.name, jsonArray)
            }
          } else {
            if ((value.key.parentType.superTypeName == GLOBAL_TYPE && value.key.parentType.name == value.key.list!!.type.superTypeName)
                || (value.key.parentType.superTypeName != GLOBAL_TYPE && value.key.parentType.superTypeName == value.key.list!!.type.superTypeName)) {
              jsonValues.add(value.key.name, serialize(value.list!!.variables, typePermission.keyPermissions.single { it.key == value.key }.referencedTypePermission!!, username))
            } else {
              if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS)
                jsonValues.add(value.key.name, serialize(value.list!!.variables, userService.superimposeUserTypePermissions(jsonParams = JsonObject().apply {
                  addProperty("orgId", variable.type.organization.id)
                  addProperty("username", username)
                  addProperty("superTypeName", value.list!!.listType.type.superTypeName)
                  addProperty("typeName", value.list!!.listType.type.name)
                }), username))
            }
          }
        }
        else -> {
          if (value.referencedVariable!!.type.superTypeName == "Any") {
            if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS) {
              jsonValues.addProperty(value.key.name, value.referencedVariable!!.name)
            }
          } else {
            if ((value.key.parentType.superTypeName == GLOBAL_TYPE && value.key.parentType.name == value.key.type.superTypeName)
                || (value.key.parentType.superTypeName != GLOBAL_TYPE && value.key.parentType.superTypeName == value.key.type.superTypeName)) {
              val variableJson: JsonObject = serialize(value.referencedVariable!!, typePermission.keyPermissions.single { it.key == value.key }.referencedTypePermission!!, username)
              if (variableJson.get("values").asJsonObject.size() != 0)
                jsonValues.add(value.key.name, variableJson)
            } else {
              if (typePermission.keyPermissions.single { it.key == value.key }.accessLevel > PermissionConstants.NO_ACCESS) {
                jsonValues.add(value.key.name, com.pibity.erp.serializers.serialize(value.referencedVariable!!))
              }
            }
          }
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
