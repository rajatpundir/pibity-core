/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.erp.commons.*
import com.pibity.erp.commons.constants.KeyConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.constants.primitiveTypes
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.*
import com.pibity.erp.entities.embeddables.ValueId
import com.pibity.erp.entities.embeddables.VariableId
import com.pibity.erp.repositories.OrganizationRepository
import com.pibity.erp.repositories.TypeRepository
import com.pibity.erp.repositories.VariableListRepository
import com.pibity.erp.repositories.VariableRepository
import org.codehaus.janino.ExpressionEvaluator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VariableService(
    val organizationRepository: OrganizationRepository,
    val typeRepository: TypeRepository,
    val variableRepository: VariableRepository,
    val variableListRepository: VariableListRepository
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createVariable(jsonParams: JsonObject, variableOrganization: Organization? = null, variableType: Type? = null, variableSuperList: VariableList? = null): Variable {
    val variableName: String = jsonParams.get("variableName").asString
    val organization: Organization = variableOrganization
        ?: organizationRepository.getById(jsonParams.get("organization").asString)
        ?: throw CustomJsonException("{organization: 'Organization could not be found'}")
    val type: Type = variableType
        ?: typeRepository.findType(organization = organization, superTypeName = variableType?.id?.superTypeName
            ?: "Any", name = jsonParams.get("typeName").asString)
        ?: throw CustomJsonException("{typeName: 'Type could not be determined'}")
    val superList: VariableList = variableSuperList ?: organization.superList!!
    val values: JsonObject = validateVariableValues(values = jsonParams.get("values").asJsonObject, type = type)
    val variable = Variable(id = VariableId(superList = superList, type = type, name = if (type.id.superTypeName == "Any" && type.autoAssignId) (type.autoIncrementId + 1).toString() else variableName), autoGeneratedId = type.autoIncrementId + 1)
    variable.id.type.autoIncrementId += 1
    variable.id.type.variableCount += 1
    // Process non-formula type values
    type.keys.filter { it.type.id.name != TypeConstants.FORMULA }.forEach {
      when (it.type.id.name) {
        TypeConstants.TEXT -> variable.values.add(Value(id = ValueId(variable = variable, key = it), stringValue = values.get(it.id.name).asString))
        TypeConstants.NUMBER -> variable.values.add(Value(id = ValueId(variable = variable, key = it), longValue = values.get(it.id.name).asLong))
        TypeConstants.DECIMAL -> variable.values.add(Value(id = ValueId(variable = variable, key = it), doubleValue = values.get(it.id.name).asDouble))
        TypeConstants.BOOLEAN -> variable.values.add(Value(id = ValueId(variable = variable, key = it), booleanValue = values.get(it.id.name).asBoolean))
        TypeConstants.FORMULA -> {/* Formulas may depend on provided values, so they will be computed separately */
        }
        TypeConstants.LIST -> {
          val jsonArray: JsonArray = values.get(it.id.name).asJsonArray
          val list: VariableList = try {
            variableListRepository.save(VariableList(listType = it.list!!))
          } catch (exception: Exception) {
            throw CustomJsonException("{${it.id.name}: 'Unable to create List'}")
          }
          for (ref in jsonArray.iterator()) {

            if (it.list!!.type.id.superTypeName == "Any") {
              val referencedVariable: Variable = variableRepository.findByTypeAndName(superList = organization.superList!!, type = it.list!!.type, name = ref.asString)
                  ?: throw CustomJsonException("{${it.id.name}: 'Unable to find referenced global variable'}")
              referencedVariable.referenceCount += 1
              if (referencedVariable.id.type.multiplicity != 0L && referencedVariable.referenceCount > referencedVariable.id.type.multiplicity)
                throw CustomJsonException("{${it.id.name}: 'Unable to reference variable due to variable's reference limit'}")
              if (!referencedVariable.active)
                throw CustomJsonException("{${it.id.name}: 'Unable to reference variable as it is inactive'}")
              list.variables.add(referencedVariable)
              list.size += 1
            } else {
              if ((it.id.parentType.id.superTypeName == "Any" && it.id.parentType.id.name == it.list!!.type.id.superTypeName)
                  || (it.id.parentType.id.superTypeName != "Any" && it.id.parentType.id.superTypeName == it.list!!.type.id.superTypeName)) {
                val referencedVariable: Variable = try {
                  createVariable(JsonObject().apply {
                    addProperty("variableName", ref.asJsonObject.get("variableName").asString)
                    add("values", ref.asJsonObject.get("values").asJsonObject)
                  }, variableOrganization = organization, variableType = it.list!!.type, variableSuperList = list)
                } catch (exception: CustomJsonException) {
                  throw CustomJsonException("{${it.id.name}: ${exception.message}}")
                }
                referencedVariable.referenceCount += 1
                list.variables.add(referencedVariable)
                list.size += 1
              } else {
                val referencedVariable: Variable = variableRepository.findVariable(organization = organization, superTypeName = it.list!!.type.id.superTypeName, typeName = it.list!!.type.id.name, superList = ref.asJsonObject.get("context").asLong, name = ref.asJsonObject.get("variableName").asString)
                    ?: throw CustomJsonException("{${it.id.name}: 'Unable to find referenced local field of global variable'}")
                referencedVariable.referenceCount += 1
                if (referencedVariable.id.type.multiplicity != 0L && referencedVariable.referenceCount > referencedVariable.id.type.multiplicity)
                  throw CustomJsonException("{${it.id.name}: 'Unable to reference variable due to variable's reference limit'}")
                if (!referencedVariable.active)
                  throw CustomJsonException("{${it.id.name}: 'Unable to reference variable as it is inactive'}")
                list.variables.add(referencedVariable)
                list.size += 1
              }
            }
          }
          if (list.size < list.listType.min)
            throw CustomJsonException("{${it.id.name}: 'List cannot contain less than ${list.listType.min} variables'}")
          if (list.listType.max != 0 && list.size > list.listType.max)
            throw CustomJsonException("{${it.id.name}: 'List cannot contain more than ${list.listType.max} variables'}")
          variable.values.add(Value(id = ValueId(variable = variable, key = it), list = list))
        }
        else -> {

          if (it.type.id.superTypeName == "Any") {
            val referencedVariable: Variable = variableRepository.findByTypeAndName(superList = organization.superList!!, type = it.type, name = values.get(it.id.name).asString)
                ?: throw CustomJsonException("{${it.id.name}: 'Unable to find referenced global variable'}")
            referencedVariable.referenceCount += 1
            if (referencedVariable.id.type.multiplicity != 0L && referencedVariable.referenceCount > referencedVariable.id.type.multiplicity)
              throw CustomJsonException("{${it.id.name}: 'Unable to reference variable due to variable's reference limit'}")
            if (!referencedVariable.active)
              throw CustomJsonException("{${it.id.name}: 'Unable to reference variable as it is inactive'}")
            variable.values.add(Value(id = ValueId(variable = variable, key = it), referencedVariable = referencedVariable))
          } else {
            if ((it.id.parentType.id.superTypeName == "Any" && it.id.parentType.id.name == it.type.id.superTypeName)
                || (it.id.parentType.id.superTypeName != "Any" && it.id.parentType.id.superTypeName == it.type.id.superTypeName)) {
              val referencedVariable: Variable = try {
                createVariable(JsonObject().apply {
                  addProperty("variableName", values.get(it.id.name).asJsonObject.get("variableName").asString)
                  add("values", values.get(it.id.name).asJsonObject.get("values").asJsonObject)
                }, variableOrganization = organization, variableType = it.type, variableSuperList = superList)
              } catch (exception: CustomJsonException) {
                throw CustomJsonException("{${it.id.name}: ${exception.message}}")
              }
              referencedVariable.referenceCount += 1
              variable.values.add(Value(id = ValueId(variable = variable, key = it), referencedVariable = referencedVariable))
            } else {
              val referencedVariable: Variable = variableRepository.findVariable(organization = organization, superTypeName = it.type.id.superTypeName, typeName = it.type.id.name, superList = values.get(it.id.name).asJsonObject.get("context").asLong, name = values.get(it.id.name).asJsonObject.get("variableName").asString)
                  ?: throw CustomJsonException("{${it.id.name}: 'Unable to find referenced local field of global variable'}")
              referencedVariable.referenceCount += 1
              if (referencedVariable.id.type.multiplicity != 0L && referencedVariable.referenceCount > referencedVariable.id.type.multiplicity)
                throw CustomJsonException("{${it.id.name}: 'Unable to reference variable due to variable's reference limit'}")
              if (!referencedVariable.active)
                throw CustomJsonException("{${it.id.name}: 'Unable to reference variable as it is inactive'}")
              variable.values.add(Value(id = ValueId(variable = variable, key = it), referencedVariable = referencedVariable))
            }
          }
        }
      }
    }
    // Process formula type values
    type.keys.filter { it.type.id.name == TypeConstants.FORMULA }.forEach {
      val expression: String = it.formula?.expression
          ?: throw CustomJsonException("{${it.id.name}: 'Unable to process formula'}")
      val returnTypeName: String = it.formula!!.returnType.id.name
      val leafKeyTypeAndValues: Map<String, Map<String, String>> = getLeafNameTypeValues(prefix = null, keys = mutableMapOf(), variable = variable, depth = 0)
      val injectedVariables: Array<String?> = arrayOfNulls(leafKeyTypeAndValues.size)
      val injectedClasses: Array<Class<*>?> = arrayOfNulls(leafKeyTypeAndValues.size)
      val injectedObjects: Array<Any?> = arrayOfNulls(leafKeyTypeAndValues.size)
      var index = 0
      for ((key, value) in leafKeyTypeAndValues) {
        injectedVariables[index] = key
        when (value[KeyConstants.KEY_TYPE]) {
          TypeConstants.TEXT -> {
            injectedClasses[index] = String::class.javaObjectType
            injectedObjects[index] = value[KeyConstants.VALUE]
          }
          TypeConstants.NUMBER -> {
            injectedClasses[index] = Long::class.javaObjectType
            injectedObjects[index] = value[KeyConstants.VALUE]?.toLong()
          }
          TypeConstants.DECIMAL -> {
            injectedClasses[index] = Double::class.javaObjectType
            injectedObjects[index] = value[KeyConstants.VALUE]?.toDouble()
          }
          TypeConstants.BOOLEAN -> {
            injectedClasses[index] = Boolean::class.javaObjectType
            injectedObjects[index] = value[KeyConstants.VALUE]?.toBoolean()
          }
        }
        index += 1
      }
      val evaluator = ExpressionEvaluator()
      evaluator.setParameters(injectedVariables, injectedClasses)
      when (returnTypeName) {
        TypeConstants.TEXT -> {
          evaluator.setExpressionType(String::class.javaObjectType)
          // Check expression for any compilation errors
          try {
            evaluator.cook(expression)
          } catch (exception: Exception) {
            throw CustomJsonException("{${it.id.name}: 'Unable to parse formula expression'}")
          }
          // Evaluate expression and get back result
          try {
            variable.values.add(Value(id = ValueId(variable = variable, key = it), stringValue = FormulaUtils.evaluateExpression(evaluator, injectedObjects) as String))
          } catch (exception: Exception) {
            throw CustomJsonException("{${it.id.name}: 'Formula expression resulted in error'}")
          }
        }
        TypeConstants.NUMBER -> {
          evaluator.setExpressionType(Long::class.javaObjectType)
          try {
            evaluator.cook(expression)
          } catch (exception: Exception) {
            throw CustomJsonException("{${it.id.name}: 'Unable to parse formula expression'}")
          }
          try {
            variable.values.add(Value(id = ValueId(variable = variable, key = it), longValue = FormulaUtils.evaluateExpression(evaluator, injectedObjects) as Long))
          } catch (exception: Exception) {
            throw CustomJsonException("{${it.id.name}: 'Formula expression resulted in error'}")
          }
        }
        TypeConstants.DECIMAL -> {
          evaluator.setExpressionType(Double::class.javaObjectType)
          try {
            evaluator.cook(expression)
          } catch (exception: Exception) {
            throw CustomJsonException("{${it.id.name}: 'Unable to parse formula expression'}")
          }
          try {
            variable.values.add(Value(id = ValueId(variable = variable, key = it), doubleValue = FormulaUtils.evaluateExpression(evaluator, injectedObjects) as Double))
          } catch (exception: Exception) {
            throw CustomJsonException("{${it.id.name}: 'Formula expression resulted in error'}")
          }
        }
        TypeConstants.BOOLEAN -> {
          evaluator.setExpressionType(Boolean::class.javaObjectType)
          try {
            evaluator.cook(expression)
          } catch (exception: Exception) {
            throw CustomJsonException("{${it.id.name}: 'Unable to parse formula expression'}")
          }
          try {
            variable.values.add(Value(id = ValueId(variable = variable, key = it), booleanValue = FormulaUtils.evaluateExpression(evaluator, injectedObjects) as Boolean))
          } catch (exception: Exception) {
            throw CustomJsonException("{${it.id.name}: 'Formula expression resulted in error'}")
          }
        }
      }
    }
    return try {
      // TODO: This should fail if variable is already present.
      // But that is not the case right now.
      variableRepository.save(variable)
    } catch (exception: Exception) {
      throw CustomJsonException("{variableName: 'Variable could not be created'}")
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun getVariableDetails(jsonParams: JsonObject): Variable {
    val organization: Organization = organizationRepository.getById(jsonParams.get("organization").asString)
        ?: throw CustomJsonException("{organization: 'Organization not found'}")
    val typeName: String = jsonParams.get("typeName").asString
    val variableName: String = jsonParams.get("variableName").asString
    return variableRepository.findVariable(organization = organization, superTypeName = "Any", typeName = typeName, superList = organization.superList!!.id, name = variableName)
        ?: throw CustomJsonException("{variableName: 'Unable to find referenced variable'}")
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun updateLocalVariableNames(variable: Variable, updatedVariableName: String) {
    variable.id.name = updatedVariableName
    variable.values.filter { it.id.key.type.id.name !in primitiveTypes }.forEach { value ->
      when (value.id.key.type.id.name) {
        TypeConstants.LIST -> {
          if (value.list!!.listType.type.id.superTypeName != "Any") {
            if ((value.id.key.id.parentType.id.superTypeName == "Any" && value.id.key.id.parentType.id.name == value.list!!.listType.type.id.superTypeName)
                || (value.id.key.id.parentType.id.superTypeName != "Any" && value.id.key.id.parentType.id.superTypeName == value.list!!.listType.type.id.superTypeName)) {
              value.list!!.variables.forEach {
                updateLocalVariableNames(variable = it, updatedVariableName = updatedVariableName)
              }
            }
          }
        }
        else -> {
          if ((value.id.key.id.parentType.id.superTypeName == "Any" && value.id.key.id.parentType.id.name == value.id.key.type.id.superTypeName)
              || (value.id.key.id.parentType.id.superTypeName != "Any" && value.id.key.id.parentType.id.superTypeName == value.id.key.type.id.superTypeName)) {
            updateLocalVariableNames(variable = value.referencedVariable!!, updatedVariableName = updatedVariableName)
          }
        }
      }
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun updateVariable(jsonParams: JsonObject, variableOrganization: Organization? = null, variableType: Type? = null, variableSuperList: VariableList? = null): Variable {
    val variableName: String = jsonParams.get("variableName").asString
    val organization: Organization = variableOrganization
        ?: organizationRepository.getById(jsonParams.get("organization").asString)
        ?: throw CustomJsonException("{organization: 'Organization could not be found'}")
    val type: Type = variableType
        ?: typeRepository.findType(organization = organization, superTypeName = variableType?.id?.superTypeName
            ?: "Any", name = jsonParams.get("typeName").asString)
        ?: throw CustomJsonException("{typeName: 'Type could not be determined'}")
    val superList: VariableList = variableSuperList ?: organization.superList!!
    val variable: Variable = variableRepository.findVariable(organization = organization, superTypeName = type.id.superTypeName, typeName = type.id.name, superList = superList.id, name = variableName)
        ?: throw CustomJsonException("{variableName: 'Unable to find referenced variable'}")
    if (jsonParams.has("active?"))
      variable.active = jsonParams.get("active?").asBoolean
    val values: JsonObject = if (jsonParams.has("values")) validateUpdatedVariableValues(values = jsonParams.get("values").asJsonObject, type = variable.id.type) else JsonObject()
    // Process non-formula type values
    variable.values.filter { it.id.key.type.id.name != TypeConstants.FORMULA }.forEach { value ->
      if (values.has(value.id.key.id.name)) {
        when (value.id.key.type.id.name) {
          TypeConstants.TEXT -> value.stringValue = values.get(value.id.key.id.name).asString
          TypeConstants.NUMBER -> value.longValue = values.get(value.id.key.id.name).asLong
          TypeConstants.DECIMAL -> value.doubleValue = values.get(value.id.key.id.name).asDouble
          TypeConstants.BOOLEAN -> value.booleanValue = values.get(value.id.key.id.name).asBoolean
          TypeConstants.FORMULA -> {/* Formulas may depend on provided values, so they will be computed separately */
          }
          TypeConstants.LIST -> {
            val listValues: JsonObject = values.get(value.id.key.id.name).asJsonObject
            if (value.list!!.listType.type.id.superTypeName == "Any") {
              if (listValues.has("add")) {
                listValues.get("add").asJsonArray.forEach {
                  val referencedVariable = variableRepository.findVariable(organization = organization, superTypeName = value.list!!.listType.type.id.superTypeName, typeName = value.list!!.listType.type.id.name, superList = organization.superList!!.id, name = it.asString)
                      ?: throw CustomJsonException("{${value.id.key.id.name}: {add: 'Unable to insert ${it.asString} referenced in list'}}")
                  if (!value.list!!.variables.contains(referencedVariable)) {
                    referencedVariable.referenceCount += 1
                    if (referencedVariable.id.type.multiplicity != 0L && referencedVariable.referenceCount > referencedVariable.id.type.multiplicity)
                      throw CustomJsonException("{${value.id.key.id.name}: 'Unable to reference variable due to variable's reference limit'}")
                    if (!referencedVariable.active)
                      throw CustomJsonException("{${value.id.key.id.name}: 'Unable to reference variable as it is inactive'}")
                    value.list!!.variables.add(referencedVariable)
                    value.list!!.size += 1
                  } else throw CustomJsonException("{${value.id.key.id.name}: {add: '${it.asString} is already present in list'}}")
                }
              }
              if (listValues.has("remove")) {
                listValues.get("remove").asJsonArray.forEach {
                  val referencedVariable = variableRepository.findVariable(organization = organization, superTypeName = value.list!!.listType.type.id.superTypeName, typeName = value.list!!.listType.type.id.name, superList = organization.superList!!.id, name = it.asString)
                      ?: throw CustomJsonException("{${value.id.key.id.name}: {remove: 'Unable to remove ${it.asString} referenced in list'}}")
                  if (value.list!!.variables.contains(referencedVariable)) {
                    referencedVariable.referenceCount -= 1
                    value.list!!.variables.remove(referencedVariable)
                    value.list!!.size -= 1
                    variableRepository.save(referencedVariable)
                  } else throw CustomJsonException("{${value.id.key.id.name}: {remove: '${it.asString} is not present in list'}}")
                }
              }
            } else {
              if ((value.id.key.id.parentType.id.superTypeName == "Any" && value.id.key.id.parentType.id.name == value.id.key.list!!.type.id.superTypeName)
                  || (value.id.key.id.parentType.id.superTypeName != "Any" && value.id.key.id.parentType.id.superTypeName == value.id.key.list!!.type.id.superTypeName)) {
                if (listValues.has("add")) {
                  listValues.get("add").asJsonArray.forEach {
                    val referencedVariable = try {
                      createVariable(jsonParams = it.asJsonObject, variableOrganization = organization, variableType = value.list!!.listType.type, variableSuperList = value.list!!)
                    } catch (exception: CustomJsonException) {
                      throw CustomJsonException("{${value.id.key.id.name}: {${it.asJsonObject.get("variableName").asString}: {add: 'Unable to add variable to list'}}}")
                    }
                    if (!value.list!!.variables.contains(referencedVariable)) {
                      referencedVariable.referenceCount += 1
                      if (referencedVariable.id.type.multiplicity != 0L && referencedVariable.referenceCount > referencedVariable.id.type.multiplicity)
                        throw CustomJsonException("{${value.id.key.id.name}: 'Unable to reference variable due to variable's reference limit'}")
                      value.list!!.variables.add(referencedVariable)
                      value.list!!.size += 1
                    } else throw CustomJsonException("{${value.id.key.id.name}: {add: '${it.asJsonObject.get("variableName").asString} is already present in list'}}")
                  }
                }
                if (listValues.has("remove")) {
                  listValues.get("remove").asJsonArray.forEach {
                    val referencedVariable = variableRepository.findVariable(organization = organization, superTypeName = value.list!!.listType.type.id.superTypeName, typeName = value.list!!.listType.type.id.name, superList = value.list!!.id, name = it.asString)
                        ?: throw CustomJsonException("{${value.id.key.id.name}: {remove: 'Unable to find ${it.asString} referenced in list'}}")
                    if (value.list!!.variables.contains(referencedVariable)) {
                      referencedVariable.referenceCount -= 1
                      value.list!!.variables.remove(referencedVariable)
                      value.list!!.size -= 1
                      if (referencedVariable.referenceCount == 0L) {
                        referencedVariable.id.type.variableCount -= 1
                        variableRepository.delete(referencedVariable)
                      } else
                        variableRepository.save(referencedVariable)
                    } else throw CustomJsonException("{${value.id.key.id.name}: {remove: '${it.asString} is not present in list'}}")
                  }
                }
                if (listValues.has("update")) {
                  listValues.get("update").asJsonArray.forEach {
                    val variableToUpdate: Variable = variableRepository.findVariable(organization = organization, superTypeName = value.list!!.listType.type.id.superTypeName, typeName = value.list!!.listType.type.id.name, superList = value.list!!.id, name = it.asJsonObject.get("variableName").asString)
                        ?: throw CustomJsonException("{${value.id.key.id.name}: {update: 'Unable to find referenced variable in list'}}")
                    value.list!!.variables.remove(variableToUpdate)
                    val updatedVariable: Variable = try {
                      updateVariable(jsonParams = it.asJsonObject, variableOrganization = organization, variableType = value.list!!.listType.type, variableSuperList = value.list!!)
                    } catch (exception: CustomJsonException) {
                      throw CustomJsonException("{${value.id.key.id.name}: {update: ${exception.message}}}")
                    }
                    value.list!!.variables.add(updatedVariable)
                  }
                }
              } else {
                if (listValues.has("add")) {
                  listValues.get("add").asJsonArray.forEach {
                    val referencedVariable = variableRepository.findVariable(organization = organization, superTypeName = value.list!!.listType.type.id.superTypeName, typeName = value.list!!.listType.type.id.name, superList = it.asJsonObject.get("context").asLong, name = it.asJsonObject.get("variableName").asString)
                        ?: throw CustomJsonException("{${value.id.key.id.name}: {add: 'Unable to insert ${it.asJsonObject.get("variableName").asString} referenced in list'}}")
                    if (!value.list!!.variables.contains(referencedVariable)) {
                      referencedVariable.referenceCount += 1
                      if (referencedVariable.id.type.multiplicity != 0L && referencedVariable.referenceCount > referencedVariable.id.type.multiplicity)
                        throw CustomJsonException("{${value.id.key.id.name}: 'Unable to reference variable due to variable's reference limit'}")
                      if (!referencedVariable.active)
                        throw CustomJsonException("{${value.id.key.id.name}: 'Unable to reference variable as it is inactive'}")
                      value.list!!.variables.add(referencedVariable)
                      value.list!!.size += 1
                    } else throw CustomJsonException("{${value.id.key.id.name}: {add: '${it.asJsonObject.get("variableName").asString} is already present in list'}}")
                  }
                }
                if (listValues.has("remove")) {
                  listValues.get("remove").asJsonArray.forEach {
                    val referencedVariable = variableRepository.findVariable(organization = organization, superTypeName = value.list!!.listType.type.id.superTypeName, typeName = value.list!!.listType.type.id.name, superList = it.asJsonObject.get("context").asLong, name = it.asJsonObject.get("variableName").asString)
                        ?: throw CustomJsonException("{${value.id.key.id.name}: {remove: 'Unable to remove ${it.asJsonObject.get("variableName").asString} referenced in list'}}")
                    if (value.list!!.variables.contains(referencedVariable)) {
                      referencedVariable.referenceCount -= 1
                      value.list!!.variables.remove(referencedVariable)
                      value.list!!.size -= 1
                      variableRepository.save(referencedVariable)
                    } else throw CustomJsonException("{${value.id.key.id.name}: {remove: '${it.asJsonObject.get("variableName").asString} is not present in list'}}")
                  }
                }
              }
            }
            if (value.list!!.size < value.list!!.listType.min)
              throw CustomJsonException("{${value.id.key.id.name}: 'List cannot contain less than ${value.list!!.listType.min} variables'}")
            if (value.list!!.listType.max != 0 && value.list!!.size > value.list!!.listType.max)
              throw CustomJsonException("{${value.id.key.id.name}: 'List cannot contain more than ${value.list!!.listType.max} variables'}")
          }
          else -> {
            if (value.id.key.type.id.superTypeName == "Any") {
              value.referencedVariable!!.referenceCount -= 1
              variableRepository.save(value.referencedVariable!!)
              val referencedVariable = variableRepository.findVariable(organization = organization, superTypeName = value.id.key.type.id.superTypeName, typeName = value.id.key.type.id.name, superList = organization.superList!!.id, name = values.get(value.id.key.id.name).asString)
                  ?: throw CustomJsonException("{${value.id.key.id.name}: 'Unable to find referenced variable ${values.get(value.id.key.id.name).asString}'}")
              referencedVariable.referenceCount += 1
              if (referencedVariable.id.type.multiplicity != 0L && referencedVariable.referenceCount > referencedVariable.id.type.multiplicity)
                throw CustomJsonException("{${value.id.key.id.name}: 'Unable to reference variable due to variable's reference limit'}")
              if (!referencedVariable.active)
                throw CustomJsonException("{${value.id.key.id.name}: 'Unable to reference variable as it is inactive'}")
              value.referencedVariable = referencedVariable
            } else {
              if ((value.id.key.id.parentType.id.superTypeName == "Any" && value.id.key.id.parentType.id.name == value.id.key.type.id.superTypeName)
                  || (value.id.key.id.parentType.id.superTypeName != "Any" && value.id.key.id.parentType.id.superTypeName == value.id.key.type.id.superTypeName)) {
                value.referencedVariable = try {
                  updateVariable(jsonParams = values.get(value.id.key.id.name).asJsonObject, variableOrganization = organization, variableType = value.id.key.type, variableSuperList = value.referencedVariable!!.id.superList)
                } catch (exception: CustomJsonException) {
                  throw CustomJsonException("{${value.id.key.id.name}: ${exception.message}}")
                }
              } else {
                value.referencedVariable!!.referenceCount -= 1
                variableRepository.save(value.referencedVariable!!)
                val referencedVariable = variableRepository.findVariable(organization = organization, superTypeName = value.id.key.type.id.superTypeName, typeName = value.id.key.type.id.name, superList = values.get(value.id.key.id.name).asJsonObject.get("context").asLong, name = values.get(value.id.key.id.name).asJsonObject.get("variableName").asString)
                    ?: throw CustomJsonException("{${value.id.key.id.name}: 'Unable to find referenced variable ${values.get(value.id.key.id.name).asJsonObject.get("variableName").asString}'}")
                referencedVariable.referenceCount += 1
                if (referencedVariable.id.type.multiplicity != 0L && referencedVariable.referenceCount > referencedVariable.id.type.multiplicity)
                  throw CustomJsonException("{${value.id.key.id.name}: 'Unable to reference variable due to variable's reference limit'}")
                if (!referencedVariable.active)
                  throw CustomJsonException("{${value.id.key.id.name}: 'Unable to reference variable as it is inactive'}")
                value.referencedVariable = referencedVariable
              }
            }
          }
        }
      }
    }
    // Process formula type values
    // Recompute formula values
    // TODO: To be optimized in future so that formula is computed only upon change in dependent fields
    variable.values.filter { it.id.key.type.id.name == TypeConstants.FORMULA }.forEach {
      val expression: String = it.id.key.formula!!.expression
      val returnTypeName: String = it.id.key.formula!!.returnType.id.name
      val leafKeyTypeAndValues: Map<String, Map<String, String>> = getLeafNameTypeValues(prefix = null, keys = mutableMapOf(), variable = variable, depth = 0)
      val injectedVariables: Array<String?> = arrayOfNulls(leafKeyTypeAndValues.size)
      val injectedClasses: Array<Class<*>?> = arrayOfNulls(leafKeyTypeAndValues.size)
      val injectedObjects: Array<Any?> = arrayOfNulls(leafKeyTypeAndValues.size)
      var index = 0
      for ((key, value) in leafKeyTypeAndValues) {
        injectedVariables[index] = key
        when (value[KeyConstants.KEY_TYPE]) {
          TypeConstants.TEXT -> {
            injectedClasses[index] = String::class.javaObjectType
            injectedObjects[index] = value[KeyConstants.VALUE]
          }
          TypeConstants.NUMBER -> {
            injectedClasses[index] = Long::class.javaObjectType
            injectedObjects[index] = value[KeyConstants.VALUE]?.toLong()
          }
          TypeConstants.DECIMAL -> {
            injectedClasses[index] = Double::class.javaObjectType
            injectedObjects[index] = value[KeyConstants.VALUE]?.toDouble()
          }
          TypeConstants.BOOLEAN -> {
            injectedClasses[index] = Boolean::class.javaObjectType
            injectedObjects[index] = value[KeyConstants.VALUE]?.toBoolean()
          }
        }
        index += 1
      }
      val evaluator = ExpressionEvaluator()
      evaluator.setParameters(injectedVariables, injectedClasses)
      when (returnTypeName) {
        TypeConstants.TEXT -> {
          evaluator.setExpressionType(String::class.javaObjectType)
          // Check expression for any compilation errors
          try {
            evaluator.cook(expression)
          } catch (exception: Exception) {
            throw CustomJsonException("{${it.id.key.id.name}: 'Unable to parse formula expression'}")
          }
          // Evaluate expression and get back result
          try {
            it.stringValue = FormulaUtils.evaluateExpression(evaluator, injectedObjects) as String
          } catch (exception: Exception) {
            throw CustomJsonException("{${it.id.key.id.name}: 'Formula expression resulted in error'}")
          }
        }
        TypeConstants.NUMBER -> {
          evaluator.setExpressionType(Long::class.javaObjectType)
          try {
            evaluator.cook(expression)
          } catch (exception: Exception) {
            throw CustomJsonException("{${it.id.key.id.name}: 'Unable to parse formula expression'}")
          }
          try {
            it.longValue = FormulaUtils.evaluateExpression(evaluator, injectedObjects) as Long
          } catch (exception: Exception) {
            throw CustomJsonException("{${it.id.key.id.name}: 'Formula expression resulted in error'}")
          }
        }
        TypeConstants.DECIMAL -> {
          evaluator.setExpressionType(Double::class.javaObjectType)
          try {
            evaluator.cook(expression)
          } catch (exception: Exception) {
            throw CustomJsonException("{${it.id.key.id.name}: 'Unable to parse formula expression'}")
          }
          try {
            it.doubleValue = FormulaUtils.evaluateExpression(evaluator, injectedObjects) as Double
          } catch (exception: Exception) {
            throw CustomJsonException("{${it.id.key.id.name}: 'Formula expression resulted in error'}")
          }
        }
        TypeConstants.BOOLEAN -> {
          evaluator.setExpressionType(Boolean::class.javaObjectType)
          try {
            evaluator.cook(expression)
          } catch (exception: Exception) {
            throw CustomJsonException("{${it.id.key.id.name}: 'Unable to parse formula expression'}")
          }
          try {
            it.booleanValue = FormulaUtils.evaluateExpression(evaluator, injectedObjects) as Boolean
          } catch (exception: Exception) {
            throw CustomJsonException("{${it.id.key.id.name}: 'Formula expression resulted in error'}")
          }
        }
      }
    }
    if (jsonParams.has("updatedVariableName?"))
      updateLocalVariableNames(variable = variable, updatedVariableName = jsonParams.get("updatedVariableName?").asString)
    try {
      variableRepository.save(variable)
    } catch (exception: Exception) {
      throw CustomJsonException("{variableName: 'Variable could not be updated'}")
    }
    return variable
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun queryVariables(jsonParams: JsonObject): String {
    val organizationName: String = jsonParams.get("organization").asString
    val typeName: String = jsonParams.get("typeName").asString
    val variableName: String = jsonParams.get("variableName").asString
    val organization: Organization = organizationRepository.getById(organizationName)
        ?: throw CustomJsonException("{organization: 'Organization could not be found'}")
    val type: Type = typeRepository.findType(organization = organization, superTypeName = "Any", name = typeName)
        ?: throw CustomJsonException("{typeName: 'Type could not be determined'}")
    val (generatedQuery, _, _) = generateQuery(jsonParams.get("query").asJsonObject, type)
    return generatedQuery
//        return variableRepository.findBySimilarNames(superList = organization.superList!!, type = type, name = variableName)
  }
}
