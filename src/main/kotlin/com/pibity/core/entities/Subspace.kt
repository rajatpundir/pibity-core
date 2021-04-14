/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.mappings.GroupSubspace
import com.pibity.core.entities.mappings.UserSubspace
import java.io.Serializable
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.persistence.*

@Entity
@Table(name = "subspace", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["space_id", "name"])])
data class Subspace(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subspace_generator")
  @SequenceGenerator(name="subspace_generator", sequenceName = "subspace_sequence")
  val id: Long = -1,

  @ManyToOne
  @JoinColumn(name = "space_id", nullable = false)
  val space: Space,

  @Column(name = "name", nullable = false)
  val name: String,

  @OneToMany(mappedBy = "id.subspace", orphanRemoval = true)
  val subspaceGroups: Set<GroupSubspace> = HashSet(),

  @OneToMany(mappedBy = "id.subspace", orphanRemoval = true)
  val subspaceUsers: Set<UserSubspace> = HashSet(),

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
    other as Subspace
    return this.space == other.space && this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
