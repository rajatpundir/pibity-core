/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.commons.utils

import com.google.gson.JsonObject
import com.pibity.core.commons.constants.*
import com.pibity.core.commons.exceptions.CustomJsonException
import com.pibity.core.entities.Key
import com.pibity.core.entities.Type
import org.springframework.web.multipart.MultipartFile
import java.sql.Timestamp
import java.util.regex.Pattern

val typeIdentifierPattern: Pattern = Pattern.compile("^[A-Z][a-zA-Z0-9]*$")

val keyIdentifierPattern: Pattern = Pattern.compile("[a-z][a-zA-Z0-9]*(_[a-z][a-zA-Z0-9]*)*$")

fun validateTypeName(typeName: String): String {
  return if (!typeIdentifierPattern.matcher(typeName).matches())
    throw CustomJsonException("{${OrganizationConstants.TYPE_NAME}: ${MessageConstants.UNEXPECTED_VALUE}}")
  else typeName
}

fun validateTypeKeys(keysJson: JsonObject, validTypes: Set<Type>, files: List<MultipartFile>): JsonObject {
  return keysJson.entrySet()
    .sortedBy { it.value.asJsonObject.get(KeyConstants.ORDER).asInt }
    .foldIndexed(JsonObject()) { index, acc, (keyName, json) ->
    acc.apply {
      if (!keyIdentifierPattern.matcher(keyName).matches())
        throw CustomJsonException("{keys: {$keyName: 'Key name is not a valid identifier'}}")
      val keyJson: JsonObject = json.asJsonObject
      add(keyName, JsonObject().apply {
        addProperty(KeyConstants.ORDER, index)
        val keyType: Type = try {
          validTypes.single { it.name == keyJson.get(KeyConstants.KEY_TYPE).asString }
        } catch (exception: Exception) {
          throw CustomJsonException("{keys: {$keyName: {${KeyConstants.KEY_TYPE}: ${MessageConstants.UNEXPECTED_VALUE}}}}")
        }
        addProperty(KeyConstants.KEY_TYPE, keyType.name)
        try {
          if (keyJson.has(KeyConstants.DEFAULT)) {
            when (keyType.name) {
              TypeConstants.TEXT -> addProperty(KeyConstants.DEFAULT, keyJson.get(KeyConstants.DEFAULT).asString)
              TypeConstants.NUMBER -> addProperty(KeyConstants.DEFAULT, keyJson.get(KeyConstants.DEFAULT).asLong)
              TypeConstants.DECIMAL -> addProperty(KeyConstants.DEFAULT, keyJson.get(KeyConstants.DEFAULT).asBigDecimal)
              TypeConstants.BOOLEAN -> addProperty(KeyConstants.DEFAULT, keyJson.get(KeyConstants.DEFAULT).asBoolean)
              TypeConstants.DATE -> addProperty(KeyConstants.DEFAULT, java.sql.Date(keyJson.get(KeyConstants.DEFAULT).asLong).time)
              TypeConstants.TIMESTAMP -> addProperty(KeyConstants.DEFAULT, Timestamp(keyJson.get(KeyConstants.DEFAULT).asLong).time)
              TypeConstants.TIME -> addProperty(KeyConstants.DEFAULT, java.sql.Time(keyJson.get(KeyConstants.DEFAULT).asLong).time)
              TypeConstants.BLOB -> {
                val fileIndex: Int = keyJson.get(KeyConstants.DEFAULT).asInt
                if (fileIndex < 0 && fileIndex > (files.size - 1))
                  throw CustomJsonException("{}")
                else
                  addProperty(KeyConstants.DEFAULT, fileIndex)
              }
              TypeConstants.FORMULA -> {}
              else -> addProperty(KeyConstants.DEFAULT, keyJson.get(KeyConstants.DEFAULT).asString)
            }
          }
        } catch (exception: Exception) {
          throw CustomJsonException("{keys: {$keyName: {${KeyConstants.DEFAULT}: 'Default value for key is not valid'}}}")
        }
        if (keyType.name == TypeConstants.FORMULA) {
          try {
            addProperty(KeyConstants.FORMULA_RETURN_TYPE, validTypes.single { it.name == primitiveTypes.single { formulaReturnTypeName -> keyJson.get(KeyConstants.FORMULA_RETURN_TYPE).asString == formulaReturnTypeName } }.name)
          } catch (exception: Exception) {
            throw CustomJsonException("{keys: {$keyName: {${KeyConstants.FORMULA_RETURN_TYPE}: 'Return Type is not valid'}}}")
          }
          try {
            add(KeyConstants.FORMULA_EXPRESSION, keyJson.get(KeyConstants.FORMULA_EXPRESSION).asJsonObject)
          } catch (exception: Exception) {
            throw CustomJsonException("{keys: {$keyName: {${KeyConstants.FORMULA_EXPRESSION}: 'Formula expression is not valid'}}}")
          }
        }
      })
    }
  }
}

fun getSymbols(type: Type, symbolPaths: MutableSet<String>, keyDependencies: MutableSet<Key> = mutableSetOf(), symbolsForFormula: Boolean, prefix: String = "", level: Int = 0): JsonObject {
  return type.keys.fold(JsonObject()) { acc, key ->
    acc.apply {
      if (symbolPaths.any { it.startsWith(prefix = prefix + key.name) }) {
        when (key.type.name) {
          in primitiveTypes -> add(key.name, JsonObject().apply { addProperty(SymbolConstants.SYMBOL_TYPE, key.type.name) })
          TypeConstants.FORMULA -> if (!symbolsForFormula || level != 0)
            add(key.name, JsonObject().apply { addProperty(SymbolConstants.SYMBOL_TYPE, key.formula!!.returnType.name) })
          else -> add(key.name, JsonObject().apply {
            addProperty(SymbolConstants.SYMBOL_TYPE, TypeConstants.TEXT)
            if (key.referencedVariable != null && symbolPaths.any { it.startsWith(prefix = prefix + key.name + ".") })
              add(SymbolConstants.SYMBOL_VALUES,  getSymbols(prefix = prefix + key.name + ".", level = level + 1, symbolPaths = symbolPaths,
                type = key.referencedVariable!!.type, keyDependencies = keyDependencies, symbolsForFormula = symbolsForFormula))
          })
        }
        keyDependencies.add(key.apply { if (symbolsForFormula) isFormulaDependency = true else isAssertionDependency = true })
        symbolPaths.remove(prefix + key.name)
      }
    }
  }
}
