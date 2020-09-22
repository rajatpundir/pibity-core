/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.api

import com.google.gson.JsonObject
import com.pibity.erp.commons.logger.Logger
import com.pibity.erp.commons.utils.getExpectedParams
import com.pibity.erp.commons.utils.getJsonParams
import com.pibity.erp.commons.utils.validateOrEvaluateExpression
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping(path = ["/api/lisp"], consumes = [MediaType.APPLICATION_JSON_VALUE])
class LispController {

  private val logger by Logger()

  private val expectedParams: Map<String, JsonObject> = mapOf(
      "evaluateExpression" to getExpectedParams("lisp", "evaluateExpression")
  )

  @PostMapping(path = ["/evaluate"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun evaluateLispExpression(@RequestBody request: String): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["evaluateExpression"] ?: JsonObject())
      ResponseEntity(validateOrEvaluateExpression(jsonParams = jsonParams, mode = "validate", symbols = JsonObject()).toString() + " " + validateOrEvaluateExpression(jsonParams = jsonParams, mode = "evaluate", symbols = JsonObject()).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
