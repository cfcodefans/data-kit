package cf.data.kit.hub.entities

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.io.Serializable
import java.util.*
import javax.persistence.Column
import javax.persistence.Id

open class BaseEn : Serializable {

    @Id
    var id: Long? = null

    @Column(name = "name", nullable = false)
    var name: String? = null

    @CreatedDate
    var createdAt: Date = Date()

    @LastModifiedDate
    var updatedAt: Date? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseEn) return false

        if (id != other.id) return false
        if (name != other.name) return false
        return true
    }

    override fun hashCode(): Int = Objects.hash(id, name)

}