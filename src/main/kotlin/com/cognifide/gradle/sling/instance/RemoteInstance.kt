package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.pkg.deploy.ListResponse
import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.io.Serializable

class RemoteInstance private constructor() : Instance, Serializable {

    override lateinit var httpUrl: String

    override lateinit var user: String

    override lateinit var password: String

    override lateinit var typeName: String

    override lateinit var environment: String

    @Transient
    @get:JsonIgnore
    override var packages: ListResponse? = null

    override fun toString(): String {
        return "RemoteInstance(httpUrl='$httpUrl', user='$user', password='$hiddenPassword', environment='$environment', typeName='$typeName')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RemoteInstance

        return EqualsBuilder()
                .append(name, other.name)
                .append(httpUrl, other.httpUrl)
                .isEquals
    }

    override fun hashCode(): Int {
        return HashCodeBuilder()
                .append(name)
                .append(httpUrl)
                .toHashCode()
    }

    companion object {

        fun create(httpUrl: String, configurer: RemoteInstance.() -> Unit): RemoteInstance {
            return RemoteInstance().apply {
                val instanceUrl = InstanceUrl.parse(httpUrl)

                this.httpUrl = instanceUrl.httpUrl
                this.user = instanceUrl.user
                this.password = instanceUrl.password
                this.typeName = instanceUrl.typeName
                this.environment = Instance.ENVIRONMENT_CMD

                this.apply(configurer)
            }
        }

        fun create(httpUrl: String): RemoteInstance {
            return create(httpUrl, {})
        }

    }

}