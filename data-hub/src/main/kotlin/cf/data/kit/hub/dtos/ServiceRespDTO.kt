package cf.data.kit.hub.dtos

class ServiceRespDTO<T> : ServiceDTO {
    var status: Status = Status.success
    var msg: String = "ok"

    var data: T

    enum class Status {
        success, fail
    }

    constructor(data: T) {
        this.data = data
    }

    constructor(id: String, data: T) : super(id) {
        this.data = data
    }

    companion object {
        fun fail(e: Throwable, msg: String): ServiceRespDTO<Map<String, String>> {
            val resp: ServiceRespDTO<Map<String, String>> = ServiceRespDTO(mapOf("exception" to e.javaClass.name))
            resp.status = Status.fail
            resp.msg = msg
            return resp
        }

        fun <T> fail(msg: String): ServiceRespDTO<T> {
            val resp: ServiceRespDTO<T> = ServiceRespDTO<T>(null as T)
            resp.status = Status.fail
            resp.msg = msg
            return resp
        }
    }
}