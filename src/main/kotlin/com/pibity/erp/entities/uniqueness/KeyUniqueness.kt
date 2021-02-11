/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.uniqueness

import com.pibity.erp.entities.Key
import java.io.Serializable
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(
  name = "key_uniqueness",
  schema = "inventory",
  uniqueConstraints = [UniqueConstraint(columnNames = ["type_uniqueness_id", "key_id"])]
)
data class KeyUniqueness(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "uniqueness_generator")
  @SequenceGenerator(name = "uniqueness_generator", sequenceName = "uniqueness_sequence")
  @Column(name = "id", updatable = false, nullable = false)
  val id: Long = -1L,

  @ManyToOne
  @JoinColumns(*[JoinColumn(name = "type_uniqueness_id", referencedColumnName = "id")])
  val typeUniqueness: TypeUniqueness,

  @ManyToOne
  @JoinColumns(*[JoinColumn(name = "key_id", referencedColumnName = "id")])
  val key: Key,

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp(System.currentTimeMillis())

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as KeyUniqueness
    return this.typeUniqueness == other.typeUniqueness && this.key == other.key
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
