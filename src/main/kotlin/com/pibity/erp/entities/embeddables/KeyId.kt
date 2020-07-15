/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.embeddables

import com.pibity.erp.entities.Type
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Embeddable
data class KeyId(

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "parent_type_organization_id", referencedColumnName = "organization_id"),
        JoinColumn(name = "parent_super_type_name", referencedColumnName = "super_type_name"),
        JoinColumn(name = "parent_type_name", referencedColumnName = "type_name")])
    val parentType: Type,

    @Column(name = "key_name", nullable = false)
    val name: String

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as KeyId
    return this.parentType == other.parentType && this.name == other.name
  }

  override fun hashCode(): Int = Objects.hash(parentType, name)
}
