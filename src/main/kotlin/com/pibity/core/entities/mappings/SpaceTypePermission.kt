/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities.mappings

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.mappings.embeddables.SpaceTypePermissionId
import java.io.Serializable
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "mapping_space_type_permissions", schema = ApplicationConstants.SCHEMA)
data class SpaceTypePermission(

  @EmbeddedId
  val id: SpaceTypePermissionId,

  @Column(name = "created", nullable = false)
  val created: Timestamp,

  @Column(name = "updated")
  var updated: Timestamp? = null

) : Serializable {

  @PreUpdate
  fun onUpdate() {
    updated = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime())
  }

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as SpaceTypePermission
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)
}
