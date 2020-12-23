/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.mappings

import com.pibity.erp.entities.mappings.embeddables.UserRoleId
import java.io.Serializable
import java.util.*
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "mapping_user_roles", schema = "inventory")
data class UserRole(

    @EmbeddedId
    val id: UserRoleId

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as UserRole
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)
}
