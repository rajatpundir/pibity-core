/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons.utils

import com.google.gson.*
import com.pibity.erp.commons.constants.MessageConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken
import org.keycloak.representations.AccessToken
import java.io.File
import java.security.MessageDigest

data class Quadruple<T1, T2, T3, T4>(val t1: T1, val t2: T2, val t3: T3, val t4: T4)

val gson: Gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()

fun getExpectedParams(controller: String, filename: String): JsonObject {
  return gson.fromJson(File("src/main/resources/requests/$controller/$filename.json").readText(), JsonObject::class.java)
}

fun getJsonParams(request: String, expectedParams: JsonObject): JsonObject {
  val json: JsonObject = try {
    JsonParser.parseString(request).asJsonObject
  } catch (exception: Exception) {
    throw CustomJsonException("{'error': 'Unable to parse request body'}")
  }
  return expectedParams.entrySet().fold(JsonObject()) { acc, (param, paramType) ->
    acc.apply {
      if (param.endsWith("?")) {
        val optionalParam: String = param.substringBefore("?")
        if (json.has(optionalParam)) {
          try {
            when (paramType.asJsonObject.get("type").asString) {
              "String" -> addProperty(param, json[optionalParam].asString)
              "Int" -> addProperty(param, json[optionalParam].asInt)
              "Long" -> addProperty(param, json[optionalParam].asLong)
              "Double" -> addProperty(param, json[optionalParam].asDouble)
              "Boolean" -> addProperty(param, json[optionalParam].asBoolean)
              "Object" -> add(param, json[optionalParam].asJsonObject)
              "Array" -> add(param, json[optionalParam].asJsonArray)
            }
          } catch (exception: Exception) {
            throw CustomJsonException("{'$optionalParam': ${MessageConstants.UNEXPECTED_VALUE}}")
          }
        }
      } else {
        if (!json.has(param))
          throw CustomJsonException("{'$param': ${MessageConstants.MISSING_FIELD}}")
        else {
          try {
            when (paramType.asJsonObject.get("type").asString) {
              "String" -> addProperty(param, json[param].asString)
              "Int" -> addProperty(param, json[param].asInt)
              "Long" -> addProperty(param, json[param].asLong)
              "Double" -> addProperty(param, json[param].asDouble)
              "Boolean" -> addProperty(param, json[param].asBoolean)
              "Object" -> add(param, json[param].asJsonObject)
              "Array" -> add(param, json[param].asJsonArray)
            }
          } catch (exception: Exception) {
            throw CustomJsonException("{'$param': ${MessageConstants.UNEXPECTED_VALUE}}")
          }
        }
      }
    }
  }
}

// TODO. If subGroupName is USER, then ADMIN will also suffice, but not vice versa.
@Suppress("UNCHECKED_CAST")
fun validateOrganizationClaim(authentication: KeycloakAuthenticationToken, jsonParams: JsonObject, subGroupName: String) {
  val token: AccessToken = (authentication.details as SimpleKeycloakAccount).keycloakSecurityContext.token
  val claims: Map<String, String> = token.otherClaims as Map<String, String>
  val groups: Set<String> = if (!claims.containsKey("groups"))
    throw CustomJsonException("{orgId: 'Organization cannot be determined'}")
  else
    gson.fromJson(gson.toJson(claims["groups"]), JsonArray::class.java).map { it.asString }.toSet()
  if (!groups.contains(listOf("", jsonParams.get("orgId").asString, subGroupName).joinToString(separator = "/")))
    throw CustomJsonException("{orgId: 'Organization could not be found'}")
}

fun computeHash(input: String): String {
  val bytes = input.toByteArray()
  val md = MessageDigest.getInstance("SHA-256")
  val digest = md.digest(bytes)
  return digest.fold("", { str, it -> str + "%02x".format(it) })
}
