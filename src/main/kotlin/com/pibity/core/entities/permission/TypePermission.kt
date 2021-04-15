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
import com.pibity.core.entities.Type
import com.pibity.core.entities.mappings.SpaceTypePermission
import com.pibity.core.serializers.serialize
import java.io.Serializable
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "type_permission", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["type_id", "name"])])
data class TypePermission(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "type_permission_generator")
  @SequenceGenerator(name = "type_permission_generator", sequenceName = "type_permission_sequence")
  val id: Long = -1,

  @ManyToOne
  @JoinColumns(JoinColumn(name = "type_id", referencedColumnName = "id"))
  val type: Type,

  @Column(name = "name", nullable = false)
  val name: String,

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()),

  @Column(name = "permission_type", nullable = false)
  val permissionType: String,

  @ManyToMany
  @JoinTable(name = "mapping_type_permission_keys", schema = ApplicationConstants.SCHEMA,
    joinColumns = [JoinColumn(name = "type_permission_id", referencedColumnName = "id")],
    inverseJoinColumns = [JoinColumn(name = "key_id", referencedColumnName = "id")])
  var keys: MutableSet<Key> = HashSet(),

  @OneToMany(mappedBy = "id.permission", cascade = [CascadeType.ALL], orphanRemoval = true)
  val permissionSpaces: MutableSet<SpaceTypePermission> = HashSet(),

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
    other as TypePermission
    return this.type == other.type && this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()

  override fun toString(): String = serialize(this).toString()
}
