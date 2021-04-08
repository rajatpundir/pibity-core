/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities.function

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.Organization
import com.pibity.core.entities.permission.FunctionPermission
import com.pibity.core.serializers.serialize
import java.io.Serializable
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "function", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["organization_id", "name"])])
data class Function(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "function_generator")
  @SequenceGenerator(name="function_generator", sequenceName = "function_sequence")
  val id: Long = -1,

  @ManyToOne
  @JoinColumn(name = "organization_id", nullable = false)
  val organization: Organization,

  @Column(name = "name", nullable = false)
  val name: String,

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()),

  @Lob
  @Column(name = "symbol_paths", nullable = false)
  val symbolPaths: String,

  @OneToMany(mappedBy = "function", cascade = [CascadeType.ALL], orphanRemoval = true)
  val inputs: MutableSet<FunctionInput> = HashSet(),

  @OneToMany(mappedBy = "function", cascade = [CascadeType.ALL], orphanRemoval = true)
  val outputs: MutableSet<FunctionOutput> = HashSet(),

  @OneToMany(mappedBy = "function", cascade = [CascadeType.ALL], orphanRemoval = true)
  val permissions: MutableSet<FunctionPermission> = HashSet(),

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
    other as Function
    return this.organization == other.organization && this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()

  override fun toString(): String = serialize(this).toString()
}
