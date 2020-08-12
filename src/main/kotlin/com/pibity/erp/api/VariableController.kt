/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.api

import com.google.gson.JsonObject
import com.pibity.erp.commons.getExpectedParams
import com.pibity.erp.commons.getJsonParams
import com.pibity.erp.commons.gson
import com.pibity.erp.commons.logger.Logger
import com.pibity.erp.services.VariableService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping(path = ["/api/variable"], consumes = [MediaType.APPLICATION_JSON_VALUE])
class VariableController(val variableService: VariableService) {

  private val logger by Logger()

  private val expectedParams: Map<String, JsonObject> = mapOf(
      "createVariable" to getExpectedParams("variable", "createVariable"),
      "updateVariable" to getExpectedParams("variable", "updateVariable"),
      "queryVariables" to getExpectedParams("variable", "queryVariables")
  )

  @PostMapping(path = ["/create"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun createVariable(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(variableService.createVariable(jsonParams = getJsonParams(request, expectedParams["createVariable"]
          ?: JsonObject()))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/update"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun updateVariable(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(variableService.updateVariable(jsonParams = getJsonParams(request, expectedParams["updateVariable"]
          ?: JsonObject()))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/query"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun queryVariables(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(variableService.queryVariables(jsonParams = getJsonParams(request, expectedParams["queryVariables"]
          ?: JsonObject()))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
