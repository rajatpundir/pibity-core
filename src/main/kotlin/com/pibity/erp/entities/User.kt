/*
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import com.pibity.erp.commons.constants.ApplicationConstants
import com.pibity.erp.entities.mappings.UserGroup
import com.pibity.erp.entities.mappings.UserRole
import com.pibity.erp.serializers.serialize
import java.io.Serializable
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "user", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [
  UniqueConstraint(columnNames = ["organization_id", "username"]),
  UniqueConstraint(columnNames = ["organization_id", "email"])
])
data class User(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_generator")
  @SequenceGenerator(name = "user_generator", sequenceName = "user_sequence")
  val id: Long = -1,

  @ManyToOne
  @JoinColumn(name = "organization_id", nullable = false)
  val organization: Organization,

  @Column(name = "username", nullable = false)
  val username: String,

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp(System.currentTimeMillis()),

  @Column(name = "active", nullable = false)
  var active: Boolean,

  @Column(name = "email", nullable = false)
  var email: String,

  @Column(name = "first_name", nullable = false)
  var firstName: String,

  @Column(name = "last_name", nullable = false)
  var lastName: String,

  @OneToOne(cascade = [CascadeType.REMOVE])
  var details: Variable? = null,

  @OneToMany(mappedBy = "id.user", cascade = [CascadeType.ALL], orphanRemoval = true)
  val userRoles: MutableSet<UserRole> = HashSet(),

  @OneToMany(mappedBy = "id.user", cascade = [CascadeType.ALL], orphanRemoval = true)
  val userGroups: MutableSet<UserGroup> = HashSet(),

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
    other as User
    return this.organization == other.organization && this.username == other.username
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()

  override fun toString(): String = serialize(this).toString()
}
