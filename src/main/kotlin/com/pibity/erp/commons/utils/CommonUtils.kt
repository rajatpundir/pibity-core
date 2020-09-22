/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons.utils

import com.google.gson.*
import com.pibity.erp.commons.exceptions.CustomJsonException
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken
import org.keycloak.representations.AccessToken
import java.io.FileReader

val gson: Gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()

fun getExpectedParams(controller: String, filename: String): JsonObject = gson.fromJson(FileReader("src/main/resources/requests/$controller/$filename.json"), JsonObject::class.java)

// Parses JSON from request to generate JSON Object with whitelisted properties
// Raises exception if expected property is missing or of incorrect type
fun getJsonParams(request: String, expectedParams: JsonObject): JsonObject {
  // Parse request body to get a JsonObject
  val json: JsonObject = try {
    JsonParser.parseString(request).asJsonObject
  } catch (exception: Exception) {
    throw CustomJsonException("{'error': 'Unable to parse request body'}")
  }
  // Get whitelisted params into another JsonObject
  // Raise exception if expected parameter is missing or of incorrect type
  val jsonParams = JsonObject()
  for ((param, paramType) in expectedParams.entrySet()) {
    if (param.endsWith("?")) {
      val optionalParam: String = param.substringBefore("?")
      if (json.has(optionalParam)) {
        try {
          when (paramType.asJsonObject.get("type").asString) {
            "String" -> jsonParams.addProperty(param, json[optionalParam].asString)
            "Int" -> jsonParams.addProperty(param, json[optionalParam].asInt)
            "Long" -> jsonParams.addProperty(param, json[optionalParam].asLong)
            "Double" -> jsonParams.addProperty(param, json[optionalParam].asDouble)
            "Boolean" -> jsonParams.addProperty(param, json[optionalParam].asBoolean)
            "Object" -> jsonParams.add(param, json[optionalParam].asJsonObject)
            "Array" -> jsonParams.add(param, json[optionalParam].asJsonArray)
          }
        } catch (exception: Exception) {
          throw CustomJsonException("{'$optionalParam': 'Unexpected value for parameter'}")
        }
      }
    } else {
      if (!json.has(param))
        throw CustomJsonException("{'$param': 'Field is missing in request body'}")
      else {
        try {
          when (paramType.asJsonObject.get("type").asString) {
            "String" -> jsonParams.addProperty(param, json[param].asString)
            "Int" -> jsonParams.addProperty(param, json[param].asInt)
            "Long" -> jsonParams.addProperty(param, json[param].asLong)
            "Double" -> jsonParams.addProperty(param, json[param].asDouble)
            "Boolean" -> jsonParams.addProperty(param, json[param].asBoolean)
            "Object" -> jsonParams.add(param, json[param].asJsonObject)
            "Array" -> jsonParams.add(param, json[param].asJsonArray)
          }
        } catch (exception: Exception) {
          throw CustomJsonException("{'$param': 'Unexpected value for parameter'}")
        }
      }
    }
  }
  return jsonParams
}

fun validateOrganizationClaim(authentication: KeycloakAuthenticationToken, jsonParams: JsonObject, subGroupName: String) {
  val token: AccessToken = (authentication.details as SimpleKeycloakAccount).keycloakSecurityContext.token
  val claims: Map<String, String> = token.otherClaims as Map<String, String>
  val groups: Set<String> = if (!claims.containsKey("groups"))
    throw CustomJsonException("{organization: 'Organization cannot be determined'}")
  else
    gson.fromJson(gson.toJson(claims["groups"]), JsonArray::class.java).map { it.asString }.toSet()
  if (!groups.contains(listOf("", jsonParams.get("organization").asString, subGroupName).joinToString(separator = "/")))
    throw CustomJsonException("{organization: 'Organization could not be found'}")
}
