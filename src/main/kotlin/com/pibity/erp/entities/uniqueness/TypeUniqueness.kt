/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.uniqueness

import com.pibity.erp.entities.Type
import com.pibity.erp.serializers.serialize
import java.io.Serializable
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(
  name = "type_uniqueness",
  schema = "inventory",
  uniqueConstraints = [UniqueConstraint(columnNames = ["type_id", "name"])]
)
data class TypeUniqueness(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "type_uniqueness_generator")
  @SequenceGenerator(name = "type_uniqueness_generator", sequenceName = "type_uniqueness_sequence")
  @Column(name = "id", updatable = false, nullable = false)
  val id: Long = -1L,

  @ManyToOne
  @JoinColumns(*[JoinColumn(name = "type_id", referencedColumnName = "id")])
  val type: Type,

  @Column(name = "name", nullable = false)
  var name: String,

  @OneToMany(mappedBy = "typeUniqueness", cascade = [CascadeType.ALL])
  var keyUniquenessConstraints: MutableSet<KeyUniqueness> = HashSet(),

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp(System.currentTimeMillis())

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as TypeUniqueness
    return this.type == other.type && this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()

  override fun toString(): String = serialize(this).toString()
}
