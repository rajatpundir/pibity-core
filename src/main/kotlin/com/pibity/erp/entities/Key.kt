/*
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import com.pibity.erp.entities.assertion.TypeAssertion
import com.pibity.erp.entities.function.FunctionInput
import com.pibity.erp.entities.function.FunctionInputKey
import com.pibity.erp.entities.function.FunctionOutput
import com.pibity.erp.entities.function.FunctionOutputKey
import com.pibity.erp.entities.permission.KeyPermission
import com.pibity.erp.entities.uniqueness.TypeUniqueness
import com.pibity.erp.serializers.serialize
import java.io.Serializable
import java.math.BigDecimal
import java.sql.Blob
import java.sql.Time
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "keys", schema = "inventory",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["parent_type_id", "name"]),
        UniqueConstraint(columnNames = ["parent_type_id", "key_order"])
    ])
data class Key(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "key_generator")
    @SequenceGenerator(name = "key_generator", sequenceName = "key_sequence")
    val id: Long = -1,

    @ManyToOne
    @JoinColumns(JoinColumn(name = "parent_type_id", referencedColumnName = "id"))
    val parentType: Type,

    @Column(name = "name", nullable = false)
    val name: String,

    @Version
    @Column(name = "version", nullable = false)
    val version: Timestamp = Timestamp(System.currentTimeMillis()),

    @ManyToOne
    @JoinColumns(JoinColumn(name = "type_id", referencedColumnName = "id"))
    val type: Type,

    @OneToMany(mappedBy = "key", cascade = [CascadeType.ALL])
    val permissions: Set<KeyPermission> = HashSet(),

    @Column(name = "key_order")
    var keyOrder: Int = 0,

    @Column(name = "value_string")
    var defaultStringValue: String? = null,

    @Column(name = "value_long")
    var defaultLongValue: Long? = null,

    @Column(name = "value_decimal")
    var defaultDecimalValue: BigDecimal? = null,

    @Column(name = "value_boolean")
    var defaultBooleanValue: Boolean? = null,

    @Column(name = "value_date")
    var defaultDateValue: Date? = null,

    @Column(name = "value_timestamp")
    var defaultTimestampValue: Timestamp? = null,

    @Column(name = "value_time")
    var defaultTimeValue: Time? = null,

    @Lob
    @Column(name = "value_blob")
    var defaultBlobValue: Blob? = null,

    @ManyToOne(cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    @JoinColumns(JoinColumn(name = "value_referenced_variable_id", referencedColumnName = "id"))
    var referencedVariable: Variable? = null,

    @OneToOne(cascade = [CascadeType.PERSIST, CascadeType.REMOVE])
    @JoinColumns(JoinColumn(name = "formula_id", referencedColumnName = "id"))
    var formula: Formula? = null,

    @Column(name = "is_formula_dependency", nullable = false)
    var isFormulaDependency: Boolean = false,

    @ManyToMany(mappedBy = "keyDependencies")
    val dependentFormulas: Set<Formula> = HashSet(),

    @Column(name = "is_assertion_dependency", nullable = false)
    var isAssertionDependency: Boolean = false,

    @ManyToMany(mappedBy = "keyDependencies")
    val dependentAssertions: Set<TypeAssertion> = HashSet(),

    @Column(name = "is_uniqueness_dependency", nullable = false)
    var isUniquenessDependency: Boolean = false,

    @ManyToMany(mappedBy = "keys")
    val dependentTypeUniqueness: Set<TypeUniqueness> = HashSet(),

    @ManyToMany(mappedBy = "variableNameKeyDependencies")
    val dependentFunctionInputVariableNames: Set<FunctionInput> = HashSet(),

    @ManyToMany(mappedBy = "keyDependencies")
    val dependentFunctionInputKeys: Set<FunctionInputKey> = HashSet(),

    @ManyToMany(mappedBy = "variableNameKeyDependencies")
    val dependentFunctionOutputVariableNames: Set<FunctionOutput> = HashSet(),

    @ManyToMany(mappedBy = "keyDependencies")
    val dependentFunctionOutputKeys: Set<FunctionOutputKey> = HashSet(),

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
        other as Key
        return this.parentType == other.parentType && this.name == other.name
    }

    override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()

    override fun toString(): String = serialize(this).toString()
}
