package com.gdetotut.kundo

import java.io.*
import java.util.Base64
import java.util.Objects
import java.util.TreeMap
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * The UndoManager class is responsible for correct serialization and deserialization of entire UndoStack.
 *
 * Stack encodes to Base64 using the [URL and Filename safe](#url) type base64 encoding scheme.
 *
 * UndoManager has a number of useful properties to restore stack correctly:
 *
 *  * ID alows to save an unique identifier of stack's subject
 *  * VERSION can be very useful when saved version and new version of object are not equal so migration needed.
 *  * The map "extras" allows to save other extra parameters in the key-value form
 *
 */
class UndoManager
/**
 * Makes object with specific parameters.
 * @param id unique identifier allowing recognize subject on the deserializing side.
 * @param version version of subject for correct restoring in the possible case of object migration.
 * @param stack stack itself.
 */
(val ID: String, val VERSION: Int,
 /**
  * @return saved stack.
  */
 val stack: UndoStack) : Serializable {
    /**
     * @return extra parameters in the form of key-value.
     */
    val extras: Map<String, String> = TreeMap()

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as UndoManager?
        return VERSION == that!!.VERSION &&
                ID == that.ID &&
                stack == that.stack &&
                extras == that.extras
    }

    override fun hashCode(): Int {
        return Objects.hash(ID, VERSION, stack, extras)
    }

//    companion object {
//
//        /**
//         * Serializes manager to Base64 string.
//         * @param manager manager to serialize
//         * @param doZip flag for gzipping
//         * @return manager as base64 string
//         * @throws IOException when something goes wrong
//         */
//        @Throws(IOException::class)
//        fun serialize(manager: UndoManager, doZip: Boolean): String {
//            var baos = ByteArrayOutputStream()
//            ObjectOutputStream(baos).use { oos -> oos.writeObject(manager) }
//
//            if (doZip) {
//                val zippedBaos = ByteArrayOutputStream()
//                GZIPOutputStream(zippedBaos).use { gzip -> gzip.write(baos.toByteArray()) }
//                baos = zippedBaos
//            }
//            return Base64.getUrlEncoder().encodeToString(baos.toByteArray())
//        }
//
//        /**
//         * Deserialize base64 string back to manager
//         * @param base64 base64 string
//         * @return manager
//         * @throws IOException when something goes wrong
//         * @throws ClassNotFoundException when something goes wrong
//         */
//        @Throws(IOException::class, ClassNotFoundException::class)
//        fun deserialize(base64: String): UndoManager {
//
//            val data = Base64.getUrlDecoder().decode(base64)
//            val zipped = data[0] == GZIPInputStream.GZIP_MAGIC.toByte() && data[1] == (GZIPInputStream.GZIP_MAGIC shr 8).toByte()
//
//            (if (zipped)
//                ObjectInputStream(GZIPInputStream(ByteArrayInputStream(data)))
//            else
//                ObjectInputStream(ByteArrayInputStream(data))).use { ois -> return ois.readObject() as UndoManager }
//        }
//    }
}

@Throws(IOException::class)
fun UndoManager.serialize(manager: UndoManager, doZip: Boolean) : String {
    var baos = ByteArrayOutputStream()
    ObjectOutputStream(baos).use { oos -> oos.writeObject(manager) }

    if (doZip) {
        val zippedBaos = ByteArrayOutputStream()
        GZIPOutputStream(zippedBaos).use { gzip -> gzip.write(baos.toByteArray()) }
        baos = zippedBaos
    }
    return Base64.getUrlEncoder().encodeToString(baos.toByteArray())
}

@Throws(IOException::class, ClassNotFoundException::class)
fun UndoManager.deserialize(base64: String): UndoManager {

    val data = Base64.getUrlDecoder().decode(base64)
    val zipped = data[0] == GZIPInputStream.GZIP_MAGIC.toByte() && data[1] == (GZIPInputStream.GZIP_MAGIC shr 8).toByte()

    (if (zipped)
        ObjectInputStream(GZIPInputStream(ByteArrayInputStream(data)))
    else
        ObjectInputStream(ByteArrayInputStream(data))).use { ois -> return ois.readObject() as UndoManager }
}
