package org.h2.util

import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.util.IOUtils.trace
import java.lang.ref.PhantomReference
import java.lang.ref.Reference
import java.lang.ref.ReferenceQueue

/**
 * This class deletes temporary files when they are not used any longer.
 */
class TempFileDeleter {
    val queue: ReferenceQueue<Any> = ReferenceQueue()
    val refMap: HashMap<PhantomReference<*>, Any> = HashMap()

    /**
     * Add a file or a closeable to the list of temporary objects to delete. the
     * file is deleted once the file object is garbage collected.
     * @param resource the file name or the closeable
     * @param monitor the object to monitor
     * @return the reference that can be used to stop deleting the file or closing the closeable
     */
    @Synchronized
    fun addFile(resource: Any, monitor: Any): Reference<*> {
        if (resource !is String && resource !is AutoCloseable) {
            throw DbException.getUnsupportedException("Unsupported resource $resource")
        }

        IOUtils.trace("TempFileDeleter.addFile",
                (if (resource is String) resource else "-"),
                monitor)
        val ref: PhantomReference<*> = PhantomReference(monitor, queue)
        refMap[ref] = resource
        deleteUnused()
        return ref
    }

    /**
     * Delete the given file or close the closeable now. This will remove the
     * reference from the list.
     * @param ref the reference as returned by addFile
     * @param resource the file name or closeable
     */
    @Synchronized
    fun deleteFile(ref: Reference<*>?, resource: Any?): Unit {
        var res: Any? = resource
        if (ref != null) {
            val f2: Any? = refMap.remove(ref)
            if (f2 != null) {
                if (SysProperties.CHECK && f2 != resource) {
                    DbException.throwInternalError("f2: $f2 f: $resource")
                }
                res = f2
            }
        }

        when (res) {
            is String -> {
                val fileName: String = res


            }
        }
    }

    /**
     * Delete all unused resources now.
     */
    fun deleteUnused(): Unit {
        while (true) {
            val ref: Reference<out Any> = queue.poll() ?: return
            deleteFile(ref, null)
        }
    }

    /**
     * This method is called if a file should no longer be deleted or a resource
     * should no longer be closed if the object is garbage collected.
     *
     * @param ref the reference as returned by addFile
     * @param resource file name or closeable
     */
    fun stopAutoDelete(ref: Reference<*>?, resource: Any) {
        trace("TempFileDeleter.stopAutoDelete", if (resource is String) resource else "-", ref)
        if (ref != null) {
            val f2 = refMap.remove(ref)
            if (SysProperties.CHECK) {
                if (f2 == null || f2 != resource) {
                    throw DbException.getInternalError("f2:$f2 ${f2 ?: ""} f:$resource")
                }
            }
        }
        deleteUnused()
    }
}