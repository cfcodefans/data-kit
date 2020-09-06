package cf.data.kit.hub.entities

import java.io.Serializable
import java.util.*
import javax.persistence.Embeddable

@Embeddable
open class UrlAccount : Serializable {
    var username: String? = null
    var password: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UrlAccount) return false

        return Objects.equals(username, other.username)
                && Objects.equals(password, password)
    }

    override fun hashCode(): Int = Objects.hash(username, password)
}