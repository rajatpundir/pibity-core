/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.function.embeddables

import com.pibity.erp.entities.function.FunctionInput
import com.pibity.erp.entities.Type
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Embeddable
data class FunctionInputTypeId(

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "function_organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "function_name", referencedColumnName = "function_name"),
      JoinColumn(name = "function_input_name", referencedColumnName = "input_name")])
    val functionInput: FunctionInput,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "super_type_name", referencedColumnName = "super_type_name"),
      JoinColumn(name = "type_name", referencedColumnName = "type_name")])
    val type: Type

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as FunctionInputTypeId
    return this.functionInput == other.functionInput && this.type == other.type
  }

  override fun hashCode(): Int = Objects.hash(functionInput, type)
}
