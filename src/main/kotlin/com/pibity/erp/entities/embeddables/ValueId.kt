/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.embeddables

import com.pibity.erp.entities.Key
import com.pibity.erp.entities.Variable
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Embeddable
data class ValueId(

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "variable_organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "variable_super_list_id", referencedColumnName = "super_list_id"),
      JoinColumn(name = "variable_super_type_name", referencedColumnName = "super_type_name"),
      JoinColumn(name = "variable_type_name", referencedColumnName = "type_name"),
      JoinColumn(name = "variable_name", referencedColumnName = "variable_name")])
    val variable: Variable,

    @OneToOne
    @JoinColumns(*[JoinColumn(name = "key_organization_id", referencedColumnName = "parent_type_organization_id"),
      JoinColumn(name = "key_super_type_name", referencedColumnName = "parent_super_type_name"),
      JoinColumn(name = "key_type_name", referencedColumnName = "parent_type_name"),
      JoinColumn(name = "key_name", referencedColumnName = "key_name")])
    val key: Key

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as ValueId
    return this.variable == other.variable && this.key == other.key
  }

  override fun hashCode(): Int = Objects.hash(variable, key)
}
