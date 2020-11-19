/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.mappings.embeddables

import com.pibity.erp.entities.Group
import com.pibity.erp.entities.Role
import java.io.Serializable
import java.util.*
import javax.persistence.Embeddable
import javax.persistence.JoinColumn
import javax.persistence.JoinColumns
import javax.persistence.ManyToOne

@Embeddable
data class GroupRoleId(

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "group_id", referencedColumnName = "id")])
    val group: Group,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "role_id", referencedColumnName = "id")])
    val role: Role

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as GroupRoleId
    return this.group == other.group && this.role == other.role
  }

  override fun hashCode(): Int = Objects.hash(group, role)
}
