/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.function.embeddables

import com.pibity.erp.entities.Key
import com.pibity.erp.entities.function.FunctionInputType
import java.io.Serializable
import java.util.*
import javax.persistence.Embeddable
import javax.persistence.JoinColumn
import javax.persistence.JoinColumns
import javax.persistence.ManyToOne

@Embeddable
data class FunctionInputKeyId(

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "function_organization_id", referencedColumnName = "function_organization_id"),
      JoinColumn(name = "function_name", referencedColumnName = "function_name"),
      JoinColumn(name = "function_input_name", referencedColumnName = "function_input_name"),
      JoinColumn(name = "organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "super_type_name", referencedColumnName = "super_type_name"),
      JoinColumn(name = "type_name", referencedColumnName = "type_name")])
    val functionInputType: FunctionInputType,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "key_organization_id", referencedColumnName = "parent_type_organization_id"),
      JoinColumn(name = "key_super_type_name", referencedColumnName = "parent_super_type_name"),
      JoinColumn(name = "key_type_name", referencedColumnName = "parent_type_name"),
      JoinColumn(name = "key_name", referencedColumnName = "key_name")])
    val key: Key

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as FunctionInputKeyId
    return this.functionInputType == other.functionInputType && this.key == other.key
  }

  override fun hashCode(): Int = Objects.hash(functionInputType, key)
}
