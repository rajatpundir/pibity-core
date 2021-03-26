/*
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import com.pibity.erp.commons.constants.ApplicationConstants
import com.pibity.erp.entities.assertion.VariableAssertion
import java.io.Serializable
import java.math.BigDecimal
import java.sql.Blob
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "value", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["parent_variable_id", "key_id"])])
data class Value(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "value_generator")
    @SequenceGenerator(name = "value_generator", sequenceName = "value_sequence")
    val id: Long = -1,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "parent_variable_id", referencedColumnName = "id")])
    val variable: Variable,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "key_id", referencedColumnName = "id")])
    val key: Key,

    @Version
    @Column(name = "version", nullable = false)
    val version: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "value_string", length = 1024)
    var stringValue: String? = null,

    @Column(name = "value_long")
    var longValue: Long? = null,

    @Column(name = "value_decimal")
    var decimalValue: BigDecimal? = null,

    @Column(name = "value_boolean")
    var booleanValue: Boolean? = null,

    @Column(name = "value_date")
    var dateValue: Date? = null,

    @Column(name = "value_timestamp")
    var timestampValue: Timestamp? = null,

    @Column(name = "value_time")
    var timeValue: Time? = null,

    @Lob
    @Column(name = "value_blob")
    var blobValue: Blob? = null,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "value_referenced_variable_id", referencedColumnName = "id")])
    var referencedVariable: Variable? = null,

    @ManyToMany
    @JoinTable(name = "mapping_value_dependencies", schema = ApplicationConstants.SCHEMA, joinColumns = [JoinColumn(name = "value_id", referencedColumnName = "id")], inverseJoinColumns = [JoinColumn(name = "dependency_value_id", referencedColumnName = "id")])
    var valueDependencies: MutableSet<Value> = HashSet(),

    @ManyToMany(mappedBy = "valueDependencies")
    val dependentValues: MutableSet<Value> = HashSet(),

    @ManyToMany(mappedBy = "valueDependencies")
    val dependentVariableAssertions: MutableSet<VariableAssertion> = HashSet(),

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
        other as Value
        return this.variable == other.variable && this.key == other.key
    }

    override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
