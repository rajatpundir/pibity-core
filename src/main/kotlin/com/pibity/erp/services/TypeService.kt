/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.KeyConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.constants.formulaReturnTypes
import com.pibity.erp.commons.constants.primitiveTypes
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.validateFormulaTypeKeys
import com.pibity.erp.commons.validateSuperTypeName
import com.pibity.erp.commons.validateTypeKeys
import com.pibity.erp.commons.validateTypeName
import com.pibity.erp.entities.*
import com.pibity.erp.entities.embeddables.KeyId
import com.pibity.erp.entities.embeddables.TypeId
import com.pibity.erp.repositories.OrganizationRepository
import com.pibity.erp.repositories.TypeRepository
import com.pibity.erp.repositories.VariableRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TypeService(
    val organizationRepository: OrganizationRepository,
    val typeRepository: TypeRepository,
    val variableRepository: VariableRepository,
    val variableService: VariableService
) {

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createType(jsonParams: JsonObject, globalTypes: MutableSet<Type>? = null, localTypes: MutableSet<Type>? = null): Type {
    val organizationName: String = jsonParams.get("organization").asString
    val displayName: String = jsonParams.get("displayName").asString
    val typeName: String = validateTypeName(jsonParams.get("typeName").asString)
    val superTypeName: String = if (jsonParams.has("superTypeName")) validateSuperTypeName(jsonParams.get("superTypeName").asString) else "Any"
    val autoAssignId: Boolean = if (jsonParams.has("autoId?")) jsonParams.get("autoId?").asBoolean else false
    val multiplicity: Long = if (jsonParams.has("multiplicity?")) jsonParams.get("multiplicity?").asLong else 0
    val keys: JsonObject = validateTypeKeys(jsonParams.get("keys").asJsonObject)
    val organization: Organization = organizationRepository.getById(organizationName)
        ?: throw CustomJsonException("{organization: 'Organization could not be found'}")
    val type = Type(TypeId(organization = organization, superTypeName = superTypeName, name = typeName), autoAssignId = autoAssignId, displayName = displayName, multiplicity = multiplicity)
    val validGlobalTypes: MutableSet<Type> = globalTypes
        ?: typeRepository.findGlobalTypes(organization = organization) as MutableSet<Type>
    val validLocalTypes: MutableSet<Type> = localTypes ?: mutableSetOf()
    // Raise exception if type with same name already exists
    if (superTypeName == "Any" && validGlobalTypes.any { it.id.name == typeName })
      throw CustomJsonException("{typeName: 'Type with same name already exists'}")
    for ((keyName, json) in keys.entrySet()) {
      val keyJson = json.asJsonObject
      val keyType: Type =
          if (keyJson.get(KeyConstants.KEY_TYPE).isJsonObject) {
            val nestedType: Type = try {
              createType(keyJson.get(KeyConstants.KEY_TYPE).asJsonObject.apply {
                addProperty("organization", organizationName)
                addProperty("superTypeName", typeName)
              }, validGlobalTypes, validLocalTypes)
            } catch (exception: CustomJsonException) {
              throw CustomJsonException("{keys: {$keyName: {${KeyConstants.KEY_TYPE}: '${exception.message}'}}}")
            }
            validLocalTypes.add(nestedType)
            nestedType
          } else {
            if (keyJson.get(KeyConstants.KEY_TYPE).asString.contains("::")) {
              val keySuperTypeName: String = keyJson.get(KeyConstants.KEY_TYPE).asString.split("::").first()
              val keyTypeName: String = keyJson.get(KeyConstants.KEY_TYPE).asString.split("::").last()
              val referentialLocalType: Type = typeRepository.findType(organization = organization, superTypeName = keySuperTypeName, name = keyTypeName)
                  ?: throw CustomJsonException("{keys: {$keyName: {${KeyConstants.KEY_TYPE}: 'Key type is not valid'}}}")
              referentialLocalType
            } else {
              // key refers to global type
              try {
                validGlobalTypes.first { it.id.name == keyJson.get(KeyConstants.KEY_TYPE).asString }
              } catch (exception: Exception) {
                throw CustomJsonException("{keys: {$keyName: {${KeyConstants.KEY_TYPE}: 'Key type is not valid'}}}")
              }
            }
          }
      val keyOrder: Int = keyJson.get(KeyConstants.ORDER).asInt
      val key = Key(id = KeyId(parentType = type, name = keyName), keyOrder = keyOrder, type = keyType)
      if (keyJson.has(KeyConstants.DISPLAY_NAME))
        key.displayName = keyJson.get(KeyConstants.DISPLAY_NAME).asString
      when (keyType.id.name) {
        TypeConstants.LIST -> {
          val listType: Type =
              if (keyJson.get(KeyConstants.LIST_TYPE).isJsonObject) {
                val nestedType: Type = try {
                  createType(keyJson.get(KeyConstants.LIST_TYPE).asJsonObject.apply {
                    addProperty("organization", organizationName)
                    addProperty("superTypeName", typeName)
                  }, validGlobalTypes, validLocalTypes)
                } catch (exception: CustomJsonException) {
                  throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_TYPE}: '${exception.message}'}}}")
                }
                validLocalTypes.add(nestedType)
                nestedType
              } else {
                if (keyJson.get(KeyConstants.LIST_TYPE).asString.contains("::")) {
                  val keySuperTypeName: String = keyJson.get(KeyConstants.LIST_TYPE).asString.split("::").first()
                  val keyTypeName: String = keyJson.get(KeyConstants.LIST_TYPE).asString.split("::").last()
                  if (keySuperTypeName == "") {
                    // key refers to some local type
                    try {
                      validLocalTypes.first { it.id.name == keyTypeName }
                    } catch (exception: Exception) {
                      throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_TYPE}: 'List type is not valid'}}}")
                    }
                  } else {
                    // key refers to local type inside some other global type
                    val referentialLocalType: Type = typeRepository.findType(organization = organization, superTypeName = keySuperTypeName, name = keyTypeName)
                        ?: throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_TYPE}: 'List type is not valid'}}}")
                    referentialLocalType
                  }
                } else {
                  // key refers to global type
                  try {
                    validGlobalTypes.first { it.id.name == keyJson.get(KeyConstants.LIST_TYPE).asString }
                  } catch (exception: Exception) {
                    throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_TYPE}: 'List type is not valid'}}}")
                  }
                }
              }
          if (!primitiveTypes.contains(listType.id.name))
            key.list = TypeList(type = listType, min = keyJson.get(KeyConstants.LIST_MIN_SIZE).asInt, max = keyJson.get(KeyConstants.LIST_MAX_SIZE).asInt)
          else
            throw CustomJsonException("{keys: {$keyName: {${KeyConstants.LIST_TYPE}: 'List type cannot be a primitive type'}}}")
        }
        TypeConstants.FORMULA -> {
          val expression: String = keyJson.get(KeyConstants.FORMULA_EXPRESSION).asString
          when (keyJson.get(KeyConstants.FORMULA_RETURN_TYPE).asString) {
            in formulaReturnTypes -> {
              val returnType: Type = try {
                validGlobalTypes.first { it.id.name == keyJson.get(KeyConstants.FORMULA_RETURN_TYPE).asString }
              } catch (exception: Exception) {
                throw CustomJsonException("{keys: {$keyName: {${KeyConstants.FORMULA_RETURN_TYPE}: 'Return type is not valid'}}}")
              }
              key.formula = Formula(expression = expression, returnType = returnType)
            }
            else -> throw CustomJsonException("{keys: {$keyName: {${KeyConstants.FORMULA_RETURN_TYPE}: 'Return type is not valid'}}}")
          }
        }
        else -> setValueForKey(key = key, keyJson = keyJson)
      }
      type.keys.add(key)
    }
    // Validate expressions for keys of Formula type
    validateFormulaTypeKeys(type)
    type.depth = type.keys.map { 1 + it.type.depth }.max() ?: 0
    if (type.depth > 12)
      throw CustomJsonException("{$typeName: 'Type could not be saved due to high nestedness'}")
    try {
      typeRepository.save(type)
    } catch (exception: Exception) {
      throw CustomJsonException("{$typeName: 'Type could not be saved'}")
    }
    if (jsonParams.has("variables?"))
      createVariablesForType(jsonParams = jsonParams)
    return type
  }

  private fun setValueForKey(key: Key, keyJson: JsonObject) {
    val defaultValue: JsonElement? = if (keyJson.has("default")) keyJson.get("default") else null
    when (key.type.id.name) {
      TypeConstants.TEXT -> key.defaultStringValue = defaultValue?.asString ?: ""
      TypeConstants.NUMBER -> key.defaultLongValue = defaultValue?.asLong ?: 0
      TypeConstants.DECIMAL -> key.defaultDoubleValue = defaultValue?.asDouble ?: 0.0
      TypeConstants.BOOLEAN -> key.defaultBooleanValue = defaultValue?.asBoolean ?: false
      TypeConstants.LIST, TypeConstants.FORMULA -> {
      }
      else -> {
        // Default values for variable references only makes sense when they refer a Global variable
        if (key.type.id.superTypeName == "Any" && defaultValue != null) {
          key.referencedVariable = variableRepository.findByTypeAndName(superList = key.id.parentType.id.organization.superList!!, type = key.type, name = defaultValue.asString)
              ?: throw CustomJsonException("{keys: {${key.id.name}: {default: 'Variable reference is not correct'}}}")
        }
      }
    }
  }

  @Transactional(rollbackFor = [CustomJsonException::class])
  fun createVariablesForType(jsonParams: JsonObject) {
    for ((variableName, values) in jsonParams.get("variables?").asJsonObject.entrySet()) {
      val jsonVariableParams = JsonObject()
      jsonVariableParams.apply {
        addProperty("organization", jsonParams.get("organization").asString)
        addProperty("typeName", jsonParams.get("typeName").asString)
        addProperty("variableName", variableName)
        try {
          add("values", values.asJsonObject)
        } catch (exception: Exception) {
          throw CustomJsonException("{variables: {$variableName: {values: 'Unexpected value for parameter'}}}")
        }
      }
      try {
        variableService.createVariable(jsonParams = jsonVariableParams)
      } catch (exception: CustomJsonException) {
        throw CustomJsonException("{variables: {$variableName: ${exception.message}}}")
      }
    }
  }

  fun getTypeDetails(jsonParams: JsonObject): Type {
    val organizationName: String = jsonParams.get("organization").asString
    val typeName: String = jsonParams.get("typeName").asString
    val organization: Organization = organizationRepository.getById(organizationName)
        ?: throw CustomJsonException("{organization: 'Organization could not be found'}")
    return typeRepository.findType(organization = organization, superTypeName = "Any", name = typeName)
        ?: throw CustomJsonException("{typeName: 'Type could not be determined'}")
  }
}