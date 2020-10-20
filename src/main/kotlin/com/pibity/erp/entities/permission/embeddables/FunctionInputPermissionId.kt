/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.permission.embeddables

import com.pibity.erp.entities.function.FunctionInput
import com.pibity.erp.entities.permission.FunctionPermission
import java.io.Serializable
import java.util.*
import javax.persistence.Embeddable
import javax.persistence.JoinColumn
import javax.persistence.JoinColumns
import javax.persistence.ManyToOne

@Embeddable
data class FunctionInputPermissionId(

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "function_name", referencedColumnName = "function_name"),
      JoinColumn(name = "permission_name", referencedColumnName = "permission_name")])
    val functionPermission: FunctionPermission,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "function_input_organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "function_input_function_name", referencedColumnName = "function_name"),
      JoinColumn(name = "function_input_name", referencedColumnName = "input_name")])
    val functionInput: FunctionInput

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as FunctionInputPermissionId
    return this.functionPermission == other.functionPermission && this.functionInput == other.functionInput
  }

  override fun hashCode(): Int = Objects.hash(functionPermission, functionInput)
}
