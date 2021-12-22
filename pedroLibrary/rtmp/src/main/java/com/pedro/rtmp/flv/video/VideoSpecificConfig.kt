package com.pedro.rtmp.flv.video

import java.nio.ByteBuffer

/**
 * Created by pedro on 29/04/21.
 *
 * 5 bytes sps/pps header:
 * 1 byte configurationVersion (always 1), 1 byte AVCProfileIndication, 1 byte profile_compatibility,
 * 1 byte AVCLevelIndication, 1 byte lengthSizeMinusOne (always 3)
 * 3 bytes size of sps:
 * 1 byte numOfSequenceParameterSets (always 1), 2 bytes sequenceParameterSetLength(2B) (sps size)
 * N bytes of sps.
 * sequenceParameterSetNALUnit (sps data)
 * 3 bytes size of pps:
 * 1 byte numOfPictureParameterSets (always 1), 2 bytes pictureParameterSetLength (pps size)
 * N bytes of pps:
 * pictureParameterSetNALUnit (pps data)
 */
class VideoSpecificConfig(private val sps: ByteArray, private val pps: ByteArray, private val profileIop: ProfileIop) {

  var size = calculateSize(sps, pps)

  fun write(buffer: ByteArray, offset: Int) {
    val data = ByteBuffer.wrap(buffer, offset, size)
    //5 bytes sps/pps header
    data.put(0x01)
    val profileIdc = sps[1]
    data.put(profileIdc)
    data.put(profileIop.value)
    val levelIdc = sps[3]
    data.put(levelIdc)
    data.put(0x03)
    //3 bytes size of sps
    data.put(0x01)
    data.putShort(sps.size.toShort())
    //N bytes of sps
    data.put(sps)
    //3 bytes size of pps
    data.put(0x01)
    data.putShort(pps.size.toShort())
    //N bytes of pps
    data.put(pps)
  }

  private fun calculateSize(sps: ByteArray, pps: ByteArray): Int {
    return 5 + 3 + sps.size + 3 + pps.size
  }
}