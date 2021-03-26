/*
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.function

import com.pibity.erp.commons.constants.ApplicationConstants
import com.pibity.erp.entities.Key
import com.pibity.erp.entities.Organization
import com.pibity.erp.serializers.serialize
import java.io.Serializable
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "mapper", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["organization_id", "name"])])
data class Mapper(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mapper_generator")
  @SequenceGenerator(name = "mapper_generator", sequenceName = "mapper_sequence")
  val id: Long = -1,

  @ManyToOne
  @JoinColumn(name = "organization_id", nullable = false)
  val organization: Organization,

  @Column(name = "name", nullable = false)
  val name: String,

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp(System.currentTimeMillis()),

  @Column(name = "query", nullable = false)
  val query: Boolean = true,

  @ManyToMany
  @JoinTable(name = "mapping_mapper_query_params", schema = ApplicationConstants.SCHEMA,
    joinColumns = [JoinColumn(name = "mapper_id", referencedColumnName = "id")],
    inverseJoinColumns = [JoinColumn(name = "key_id", referencedColumnName = "id")])
  val queryParams: MutableSet<Key> = HashSet(),

  @ManyToOne
  @JoinColumn(name = "function_input_id", nullable = false)
  val functionInput: FunctionInput,

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
    other as Mapper
    return this.organization == other.organization && this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()

  override fun toString(): String = serialize(this).toString()
}
