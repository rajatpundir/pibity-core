/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.function

import com.pibity.erp.entities.Organization
import com.pibity.erp.entities.permission.FunctionPermission
import com.pibity.erp.serializers.serialize
import java.io.Serializable
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "function", schema = "inventory", uniqueConstraints = [UniqueConstraint(columnNames = ["organization_id", "name"])])
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
    val version: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "symbol_paths", nullable = false)
    val symbolPaths: String,

    @OneToMany(mappedBy = "function", cascade = [CascadeType.ALL])
    val inputs: MutableSet<FunctionInput> = HashSet(),

    @OneToMany(mappedBy = "function", cascade = [CascadeType.ALL])
    val outputs: MutableSet<FunctionOutput> = HashSet(),

    @OneToMany(mappedBy = "function", cascade = [CascadeType.ALL])
    val permissions: MutableSet<FunctionPermission> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as Function
    return this.organization == other.organization && this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()

  override fun toString(): String = serialize(this).toString()
}
