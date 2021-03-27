/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.assertion.TypeAssertion
import com.pibity.core.entities.permission.TypePermission
import com.pibity.core.entities.uniqueness.TypeUniqueness
import com.pibity.core.serializers.serialize
import java.io.Serializable
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "type", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["organization_id", "name"])])
data class Type(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "type_generator")
  @SequenceGenerator(name="type_generator", sequenceName = "type_sequence")
  val id: Long = -1,

  @ManyToOne
  @JoinColumn(name = "organization_id", nullable = false)
  val organization: Organization,

  @Column(name = "name", nullable = false)
  val name: String,

  @Column(name = "auto_id", nullable = false)
  var autoId: Boolean = false,

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp(System.currentTimeMillis()),

  @Column(name = "primitive_type", nullable = false)
  var primitiveType: Boolean = false,

  @OneToMany(mappedBy = "parentType", cascade = [CascadeType.PERSIST, CascadeType.MERGE], orphanRemoval = true)
  val keys: MutableSet<Key> = HashSet(),

  @OneToMany(mappedBy = "type", cascade = [CascadeType.ALL])
  val referencingKeys: Set<Key> = HashSet(),

  @OneToMany(mappedBy = "type", cascade = [CascadeType.PERSIST, CascadeType.MERGE], orphanRemoval = true)
  val uniqueConstraints: MutableSet<TypeUniqueness> = HashSet(),

  @OneToMany(mappedBy = "type", cascade = [CascadeType.PERSIST, CascadeType.MERGE], orphanRemoval = true)
  val typeAssertions: MutableSet<TypeAssertion> = HashSet(),

  @OneToMany(mappedBy = "type", cascade = [CascadeType.ALL], orphanRemoval = true)
  val permissions: MutableSet<TypePermission> = HashSet(),

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
    other as Type
    return this.organization == other.organization && this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()

  override fun toString(): String = serialize(this).toString()
}
