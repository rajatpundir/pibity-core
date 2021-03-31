/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.api

import com.google.gson.JsonObject
import com.pibity.core.commons.constants.LispConstants
import com.pibity.core.commons.exceptions.CustomJsonException
import com.pibity.core.commons.logger.Logger
import com.pibity.core.utils.getExpectedParams
import com.pibity.core.utils.getJsonParams
import com.pibity.core.utils.validateOrEvaluateExpression
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
      ResponseEntity(
        validateOrEvaluateExpression(expression = jsonParams, symbols = JsonObject(), mode = LispConstants.VALIDATE, expectedReturnType = jsonParams.get(LispConstants.EXPECTED_RETURN_TYPE).asString).toString()
          + " " + validateOrEvaluateExpression(expression = jsonParams, symbols = JsonObject(), mode = LispConstants.EVALUATE, expectedReturnType = jsonParams.get(LispConstants.EXPECTED_RETURN_TYPE).asString).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
