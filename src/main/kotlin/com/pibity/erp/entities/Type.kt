/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import com.pibity.erp.entities.permission.TypePermission
import com.pibity.erp.serializers.serialize
import java.io.Serializable
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "type", schema = "inventory", uniqueConstraints = [UniqueConstraint(columnNames = ["organization_id", "super_type_name", "name"])])
data class Type(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "type_generator")
    @SequenceGenerator(name="type_generator", sequenceName = "type_sequence")
    val id: Long = -1,

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    val organization: Organization,

    @Column(name = "super_type_name", nullable = false)
    val superTypeName: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @Version
    @Column(name = "version", nullable = false)
    val version: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "auto_increment_id", nullable = false)
    var autoIncrementId: Int = 0,

    @Column(name = "auto_assign_id", nullable = false)
    var autoAssignId: Boolean = false,

    @Column(name = "multiplicity", nullable = false)
    val multiplicity: Long,

    @Column(name = "variable_count", nullable = false)
    var variableCount: Long = 0,

    @Column(name = "depth", nullable = false)
    var depth: Int = 0,

    @Column(name = "primitive_type", nullable = false)
    var primitiveType: Boolean = false,

    @Column(name = "is_formula_dependency", nullable = false)
    var isFormulaDependency: Boolean = false,

    @Column(name = "has_assertion_dependency", nullable = false)
    var isAssertionDependency: Boolean = false,

    @Column(name = "has_assertions", nullable = false)
    var hasAssertions: Boolean = false,

    @OneToMany(mappedBy = "parentType", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    val keys: MutableSet<Key> = HashSet(),

    @OneToMany(mappedBy = "type", cascade = [CascadeType.ALL])
    val referencingKeys: Set<Key> = HashSet(),

    @OneToMany(mappedBy = "type", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    val typeAssertions: MutableSet<TypeAssertion> = HashSet(),

    @OneToMany(mappedBy = "type", cascade = [CascadeType.ALL])
    val permissions: MutableSet<TypePermission> = HashSet(),

    @OneToOne
    var superList: VariableList? = null

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as Type
    return this.organization == other.organization && this.superTypeName == other.superTypeName && this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()

  override fun toString(): String = serialize(this).toString()
}
