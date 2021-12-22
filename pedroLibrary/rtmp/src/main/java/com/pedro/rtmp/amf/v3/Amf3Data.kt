package com.pedro.rtmp.amf.v3

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.jvm.Throws

/**
 * Created by pedro on 20/04/21.
 */
abstract class Amf3Data {

  companion object {

    /**
     * Read unknown AmfData and convert it to specific class
     */
    @Throws(IOException::class)
    fun getAmf3Data(input: InputStream): Amf3Data {
      val amf3Data = when (val type = getMark3Type(input.read())) {
        Amf3Type.DOUBLE -> Amf3Double()
        Amf3Type.INTEGER -> Amf3Integer()
        Amf3Type.STRING -> Amf3String()
        Amf3Type.OBJECT -> Amf3Object()
        Amf3Type.NULL -> Amf3Null()
        Amf3Type.UNDEFINED -> Amf3Undefined()
        Amf3Type.ARRAY -> Amf3Array()
        Amf3Type.DICTIONARY -> Amf3Dictionary()
        Amf3Type.TRUE -> Amf3True()
        Amf3Type.FALSE -> Amf3False()
        else -> throw IOException("Unimplemented AMF3 data type: ${type.name}")
      }
      amf3Data.readBody(input)
      return amf3Data
    }

    fun getMark3Type(type: Int): Amf3Type {
      return Amf3Type.values().find { it.mark.toInt() == type } ?: Amf3Type.STRING
    }
  }

  @Throws(IOException::class)
  fun readHeader(input: InputStream): Amf3Type {
    return getMark3Type(input.read())
  }

  @Throws(IOException::class)
  fun writeHeader(output: OutputStream) {
    output.write(getType().mark.toInt())
  }

  @Throws(IOException::class)
  abstract fun readBody(input: InputStream)

  @Throws(IOException::class)
  abstract fun writeBody(output: OutputStream)

  abstract fun getType(): Amf3Type

  //Body size without header type
  abstract fun getSize(): Int
}