/*
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.mappings.SpaceFunctionPermission
import com.pibity.core.entities.mappings.SpaceTypePermission
import java.io.Serializable
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.persistence.*

@Entity
@Table(name = "space", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["organization_id", "name"])])
data class Space(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "space_generator")
  @SequenceGenerator(name="space_generator", sequenceName = "space_sequence")
  val id: Long = -1,

  @ManyToOne
  @JoinColumn(name = "organization_id", nullable = false)
  val organization: Organization,

  @Column(name = "name", nullable = false)
  val name: String,

  @Column(name = "active", nullable = false)
  var active: Boolean,

  @OneToMany(mappedBy = "id.space", cascade = [CascadeType.ALL], orphanRemoval = true)
  val spaceTypePermissions: MutableSet<SpaceTypePermission> = HashSet(),

  @OneToMany(mappedBy = "id.space", cascade = [CascadeType.ALL], orphanRemoval = true)
  val spaceFunctionPermissions: MutableSet<SpaceFunctionPermission> = HashSet(),

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
    other as Space
    return this.organization == other.organization && this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
