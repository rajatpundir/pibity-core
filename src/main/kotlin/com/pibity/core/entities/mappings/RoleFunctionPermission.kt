/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities.mappings

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.mappings.embeddables.RoleFunctionPermissionId
import java.io.Serializable
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "mapping_role_function_permissions", schema = ApplicationConstants.SCHEMA)
data class RoleFunctionPermission(

  @EmbeddedId
  val id: RoleFunctionPermissionId,

  @Column(name = "created", nullable = false)
  val created: Timestamp,

  @Column(name = "updated")
  var updated: Timestamp? = null

) : Serializable {

  @PreUpdate
  fun onUpdate() {
    updated = Timestamp(System.currentTimeMillis())
  }

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as RoleFunctionPermission
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)
}
