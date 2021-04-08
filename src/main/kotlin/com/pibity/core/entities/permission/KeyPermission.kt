/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities.permission

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.Key
import java.io.Serializable
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.persistence.*

@Entity
@Table(name = "key_permission", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["type_permission_id", "key_id"])])
data class KeyPermission(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "key_permission_generator")
  @SequenceGenerator(name="key_permission_generator", sequenceName = "key_permission_sequence")
  val id: Long = -1,

  @ManyToOne
  @JoinColumns(JoinColumn(name = "type_permission_id", referencedColumnName = "id"))
  val typePermission: TypePermission,

  @ManyToOne
  @JoinColumns(JoinColumn(name = "key_id", referencedColumnName = "id"))
  val key: Key,

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()),

  @Column(name = "access_level", nullable = false)
  var accessLevel: Int = 0,

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
    other as KeyPermission
    return this.typePermission == other.typePermission && this.key == other.key
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
