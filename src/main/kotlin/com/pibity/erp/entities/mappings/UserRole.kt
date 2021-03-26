/*
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.mappings

import com.pibity.erp.commons.constants.ApplicationConstants
import com.pibity.erp.entities.mappings.embeddables.UserRoleId
import java.io.Serializable
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "mapping_user_roles", schema = ApplicationConstants.SCHEMA)
data class UserRole(

  @EmbeddedId
  val id: UserRoleId,

  @Column(name = "created", nullable = false)
  val created: Timestamp = Timestamp(System.currentTimeMillis()),

  @Column(name = "updated")
  var updated: Timestamp? = null

) : Serializable {

  @PreUpdate
  fun setUpdatedTimestamp() {
    updated = Timestamp(System.currentTimeMillis())
  }

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as UserRole
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)
}
