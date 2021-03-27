/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.commons.exceptions

import com.google.gson.JsonParser
import com.pibity.core.commons.logger.Logger

data class CustomJsonException(val json: String) : RuntimeException() {

  private val logger by Logger()

  override val message: String
    get() = try {
      JsonParser.parseString(json).asJsonObject.toString()
    } catch (exception: Exception) {
      logger.error("Unable to parse string to JSON: $json")
      "Unable to process your request"
    }
}
