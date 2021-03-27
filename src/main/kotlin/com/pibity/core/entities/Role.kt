/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.mappings.GroupRole
import com.pibity.core.entities.mappings.RoleFunctionPermission
import com.pibity.core.entities.mappings.RoleTypePermission
import com.pibity.core.entities.mappings.UserRole
import com.pibity.core.serializers.serialize
import java.io.Serializable
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "role", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["organization_id", "name"])])
data class Role(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_generator")
  @SequenceGenerator(name = "role_generator", sequenceName = "role_sequence")
  val id: Long = -1,

  @ManyToOne
  @JoinColumn(name = "organization_id", nullable = false)
  val organization: Organization,

  @Column(name = "name", nullable = false)
  val name: String,

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp(System.currentTimeMillis()),

  @OneToMany(mappedBy = "id.role", cascade = [CascadeType.ALL], orphanRemoval = true)
  val roleTypePermissions: MutableSet<RoleTypePermission> = HashSet(),

  @OneToMany(mappedBy = "id.role", cascade = [CascadeType.ALL], orphanRemoval = true)
  val roleFunctionPermissions: MutableSet<RoleFunctionPermission> = HashSet(),

  @OneToMany(mappedBy = "id.role", orphanRemoval = true)
  val roleGroups: Set<GroupRole> = HashSet(),

  @OneToMany(mappedBy = "id.role", orphanRemoval = true)
  val roleUsers: Set<UserRole> = HashSet(),

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
    other as Role
    return this.organization == other.organization && this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()

  override fun toString(): String = serialize(this).toString()
}
