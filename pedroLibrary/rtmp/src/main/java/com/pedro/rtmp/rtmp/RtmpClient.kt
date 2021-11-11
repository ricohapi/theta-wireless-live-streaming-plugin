package com.pedro.rtmp.rtmp

import android.media.MediaCodec
import android.util.Log
import com.pedro.rtmp.amf.v0.AmfNumber
import com.pedro.rtmp.amf.v0.AmfObject
import com.pedro.rtmp.amf.v0.AmfString
import com.pedro.rtmp.flv.video.ProfileIop
import com.pedro.rtmp.rtmp.message.*
import com.pedro.rtmp.rtmp.message.command.Command
import com.pedro.rtmp.rtmp.message.control.Type
import com.pedro.rtmp.rtmp.message.control.UserControl
import com.pedro.rtmp.utils.AuthUtil
import com.pedro.rtmp.utils.ConnectCheckerRtmp
import com.pedro.rtmp.utils.CreateSSLSocket
import com.pedro.rtmp.utils.RtmpConfig
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Created by pedro on 8/04/21.
 */
class RtmpClient(private val connectCheckerRtmp: ConnectCheckerRtmp) {

  private val TAG = "RtmpClient"
  private val rtmpUrlPattern = Pattern.compile("^rtmps?://([^/:]+)(?::(\\d+))*/([^/]+)/?([^*]*)$")

  private var connectionSocket: Socket? = null
  private var reader: BufferedInputStream? = null
  private var writer: BufferedOutputStream? = null
  private var thread: ExecutorService? = null
  private val commandsManager = CommandsManager()
  private val rtmpSender = RtmpSender(connectCheckerRtmp, commandsManager)

  @Volatile
  var isStreaming = false
    private set

  private var url: String? = null
  private var tlsEnabled = false

  private var doingRetry = false
  private var numRetry = 0
  private var reTries = 0
  private var handler: ScheduledExecutorService? = null
  private var runnable: Runnable? = null
  private var publishPermitted = false

  val droppedAudioFrames: Long
    get() = rtmpSender.droppedAudioFrames
  val droppedVideoFrames: Long
    get() = rtmpSender.droppedVideoFrames

  val cacheSize: Int
    get() = rtmpSender.getCacheSize()
  val sentAudioFrames: Long
    get() = rtmpSender.getSentAudioFrames()
  val sentVideoFrames: Long
    get() = rtmpSender.getSentVideoFrames()

  fun setOnlyAudio(onlyAudio: Boolean) {
    commandsManager.isOnlyAudio = onlyAudio
  }

  fun forceAkamaiTs(enabled: Boolean) {
    commandsManager.akamaiTs = enabled
  }

  fun setAuthorization(user: String?, password: String?) {
    commandsManager.setAuth(user, password)
  }

  fun setReTries(reTries: Int) {
    numRetry = reTries
    this.reTries = reTries
  }

  fun shouldRetry(reason: String): Boolean {
    val validReason = doingRetry && !reason.contains("Endpoint malformed")
    return validReason && reTries > 0
  }

  fun setAudioInfo(sampleRate: Int, isStereo: Boolean) {
    commandsManager.setAudioInfo(sampleRate, isStereo)
    rtmpSender.setAudioInfo(sampleRate, isStereo)
  }

  fun setVideoInfo(sps: ByteBuffer, pps: ByteBuffer, vps: ByteBuffer?) {
    Log.i(TAG, "send sps and pps")
    rtmpSender.setVideoInfo(sps, pps, vps)
  }

  fun setProfileIop(profileIop: ProfileIop) {
    rtmpSender.setProfileIop(profileIop)
  }

  fun setVideoResolution(width: Int, height: Int) {
    commandsManager.setVideoResolution(width, height)
  }

  @JvmOverloads
  fun connect(url: String?, isRetry: Boolean = false) {
    if (!isRetry) doingRetry = true
    if (url == null) {
      isStreaming = false
      connectCheckerRtmp.onConnectionFailedRtmp(
          "Endpoint malformed, should be: rtmp://ip:port/appname/streamname")
      return
    }
    if (!isStreaming || isRetry) {
      this.url = url
      connectCheckerRtmp.onConnectionStartedRtmp(url)
      val rtmpMatcher = rtmpUrlPattern.matcher(url)
      if (rtmpMatcher.matches()) {
        tlsEnabled = (rtmpMatcher.group(0) ?: "").startsWith("rtmps")
      } else {
        connectCheckerRtmp.onConnectionFailedRtmp(
            "Endpoint malformed, should be: rtmp://ip:port/appname/streamname")
        return
      }

      commandsManager.host = rtmpMatcher.group(1) ?: ""
      val portStr = rtmpMatcher.group(2)
      commandsManager.port = portStr?.toInt() ?: 1935
      commandsManager.appName = getAppName(rtmpMatcher.group(3) ?: "", rtmpMatcher.group(4) ?: "")
      commandsManager.streamName = getStreamName(rtmpMatcher.group(4) ?: "")
      commandsManager.tcUrl = getTcUrl((rtmpMatcher.group(0)
          ?: "").substring(0, (rtmpMatcher.group(0)
          ?: "").length - commandsManager.streamName.length))

      isStreaming = true
      thread = Executors.newSingleThreadExecutor()
      thread?.execute post@{
        try {
          if (!establishConnection()) {
            connectCheckerRtmp.onConnectionFailedRtmp("Handshake failed")
            return@post
          }
          val writer = this.writer ?: throw IOException("Invalid writer, Connection failed")
          commandsManager.sendConnect("", writer)
          //read packets until you did success connection to server and you are ready to send packets
          while (!Thread.interrupted() && !publishPermitted) {
            //Handle all command received and send response for it.
            handleMessages()
          }
          //read packet because maybe server want send you something while streaming
          handleServerPackets()
        } catch (e: Exception) {
          Log.e(TAG, "connection error", e)
          connectCheckerRtmp.onConnectionFailedRtmp("Error configure stream, ${e.message}")
          return@post
        }
      }
    }
  }

  private fun handleServerPackets() {
    while (!Thread.interrupted()) {
      try {
        handleMessages()
      } catch (ignored: SocketTimeoutException) {
        //new packet not found
      } catch (e: Exception) {
        Thread.currentThread().interrupt()
      }
    }
  }

  private fun getAppName(app: String, name: String): String {
    return if (!name.contains("/")) {
      app
    } else {
      app + "/" + name.substring(0, name.indexOf("/"))
    }
  }

  private fun getStreamName(name: String): String {
    return if (!name.contains("/")) {
      name
    } else {
      name.substring(name.indexOf("/") + 1)
    }
  }

  private fun getTcUrl(url: String): String {
    return if (url.endsWith("/")) {
      url.substring(0, url.length - 1)
    } else {
      url
    }
  }

  @Throws(IOException::class)
  private fun establishConnection(): Boolean {
    val socket: Socket
    if (!tlsEnabled) {
      socket = Socket()
      val socketAddress: SocketAddress = InetSocketAddress(commandsManager.host, commandsManager.port)
      socket.connect(socketAddress, 5000)
    } else {
      socket = CreateSSLSocket.createSSlSocket(commandsManager.host, commandsManager.port) ?: throw IOException("Socket creation failed")
    }
    socket.soTimeout = 5000
    val reader = BufferedInputStream(socket.getInputStream())
    val writer = BufferedOutputStream(socket.getOutputStream())
    val timestamp = System.currentTimeMillis() / 1000
    val handshake = Handshake()
    if (!handshake.sendHandshake(reader, writer)) return false
    commandsManager.timestamp = timestamp.toInt()
    commandsManager.startTs = System.nanoTime() / 1000
    connectionSocket = socket
    this.reader = reader
    this.writer = writer
    return true
  }

  /**
   * Read all messages from server and response to it
   */
  @Throws(IOException::class)
  private fun handleMessages() {
    val reader = this.reader ?: throw IOException("Invalid reader, Connection failed")
    var writer = this.writer ?: throw IOException("Invalid writer, Connection failed")

    val message = commandsManager.readMessageResponse(reader)
    when (message.getType()) {
      MessageType.SET_CHUNK_SIZE -> {
        val setChunkSize = message as SetChunkSize
        commandsManager.readChunkSize = setChunkSize.chunkSize
        Log.i(TAG, "chunk size configured to ${setChunkSize.chunkSize}")
      }
      MessageType.ACKNOWLEDGEMENT -> {
        val acknowledgement = message as Acknowledgement
      }
      MessageType.WINDOW_ACKNOWLEDGEMENT_SIZE -> {
        val windowAcknowledgementSize = message as WindowAcknowledgementSize
        RtmpConfig.acknowledgementWindowSize = windowAcknowledgementSize.acknowledgementWindowSize
      }
      MessageType.SET_PEER_BANDWIDTH -> {
        val setPeerBandwidth = message as SetPeerBandwidth
        commandsManager.sendWindowAcknowledgementSize(writer)
      }
      MessageType.ABORT -> {
        val abort = message as Abort
      }
      MessageType.AGGREGATE -> {
        val aggregate = message as Aggregate
      }
      MessageType.USER_CONTROL -> {
        val userControl = message as UserControl
        when (val type = userControl.type) {
          Type.PING_REQUEST -> {
            commandsManager.sendPong(userControl.event, writer)
          }
          else -> {
            Log.i(TAG, "user control command $type ignored")
          }
        }
      }
      MessageType.COMMAND_AMF0, MessageType.COMMAND_AMF3 -> {
        val command = message as Command
        val commandName = commandsManager.sessionHistory.getName(command.commandId)
        when (command.name) {
          "_result" -> {
            when (commandName) {
              "connect" -> {
                if (commandsManager.onAuth) {
                  connectCheckerRtmp.onAuthSuccessRtmp()
                  commandsManager.onAuth = false
                }
                commandsManager.createStream(writer)
              }
              "createStream" -> {
                try {
                  commandsManager.streamId = (command.data[3] as AmfNumber).value.toInt()
                  commandsManager.sendPublish(writer)
                } catch (e: ClassCastException) {
                  Log.e(TAG, "error parsing _result createStream", e)
                }
              }
            }
            Log.i(TAG, "success response received from ${commandName ?: "unknown command"}")
          }
          "_error" -> {
            try {
              val description = ((command.data[3] as AmfObject).getProperty("description") as AmfString).value
              when (commandName) {
                "connect" -> {
                  if (description.contains("reason=authfail") || description.contains("reason=nosuchuser")) {
                    connectCheckerRtmp.onAuthErrorRtmp()
                  } else if (commandsManager.user != null && commandsManager.password != null &&
                      description.contains("challenge=") && description.contains("salt=") //adobe response
                      || description.contains("nonce=")) { //llnw response
                    closeConnection()
                    establishConnection()
                    writer = this.writer ?: throw IOException("Invalid writer, Connection failed")
                    commandsManager.onAuth = true
                    if (description.contains("challenge=") && description.contains("salt=")) { //create adobe auth
                      val salt = AuthUtil.getSalt(description)
                      val challenge = AuthUtil.getChallenge(description)
                      val opaque = AuthUtil.getOpaque(description)
                      commandsManager.sendConnect(AuthUtil.getAdobeAuthUserResult(commandsManager.user
                          ?: "", commandsManager.password ?: "",
                          salt, challenge, opaque), writer)
                    } else if (description.contains("nonce=")) { //create llnw auth
                      val nonce = AuthUtil.getNonce(description)
                      commandsManager.sendConnect(AuthUtil.getLlnwAuthUserResult(commandsManager.user
                          ?: "", commandsManager.password ?: "",
                          nonce, commandsManager.appName), writer)
                    }
                  } else if (description.contains("code=403")) {
                    if (description.contains("authmod=adobe")) {
                      closeConnection()
                      establishConnection()
                      writer = this.writer ?: throw IOException("Invalid writer, Connection failed")
                      Log.i(TAG, "sending auth mode adobe")
                      commandsManager.sendConnect("?authmod=adobe&user=${commandsManager.user}", writer)
                    } else if (description.contains("authmod=llnw")) {
                      Log.i(TAG, "sending auth mode llnw")
                      commandsManager.sendConnect("?authmod=llnw&user=${commandsManager.user}", writer)
                    }
                  } else {
                    connectCheckerRtmp.onAuthErrorRtmp()
                  }
                }
                else -> {
                  connectCheckerRtmp.onConnectionFailedRtmp(description)
                }
              }
            } catch (e: ClassCastException) {
              Log.e(TAG, "error parsing _error command", e)
            }
          }
          "onStatus" -> {
            try {
              when (val code = ((command.data[3] as AmfObject).getProperty("code") as AmfString).value) {
                "NetStream.Publish.Start" -> {
                  commandsManager.sendMetadata(writer)
                  connectCheckerRtmp.onConnectionSuccessRtmp()

                  rtmpSender.output = writer
                  rtmpSender.start()
                  publishPermitted = true
                }
                "NetConnection.Connect.Rejected", "NetStream.Publish.BadName" -> {
                  connectCheckerRtmp.onConnectionFailedRtmp("onStatus: $code")
                }
                else -> {
                  Log.i(TAG, "onStatus $code response received from ${commandName ?: "unknown command"}")
                }
              }
            } catch (e: ClassCastException) {
              Log.e(TAG, "error parsing onStatus command", e)
            }
          }
          else -> {
            Log.i(TAG, "unknown ${command.name} response received from ${commandName ?: "unknown command"}")
          }
        }
      }
      MessageType.VIDEO, MessageType.AUDIO, MessageType.DATA_AMF0, MessageType.DATA_AMF3,
      MessageType.SHARED_OBJECT_AMF0, MessageType.SHARED_OBJECT_AMF3 -> {
        Log.e(TAG, "unimplemented response for ${message.getType()}. Ignored")
      }
    }
  }

  private fun closeConnection() {
    connectionSocket?.close()
    commandsManager.reset()
  }

  @JvmOverloads
  fun reConnect(delay: Long, backupUrl: String? = null) {
    reTries--
    disconnect(false)
    runnable = Runnable {
      val reconnectUrl = backupUrl ?: url
      connect(reconnectUrl, true)
    }
    runnable?.let {
      handler = Executors.newSingleThreadScheduledExecutor()
      handler?.schedule(it, delay, TimeUnit.MILLISECONDS)
    }
  }

  fun disconnect() {
    runnable?.let { handler?.shutdownNow() }
    disconnect(true)
  }

  private fun disconnect(clear: Boolean) {
    if (isStreaming) rtmpSender.stop(clear)
    thread?.shutdownNow()
    try {
      reader?.close()
      reader = null
      writer?.flush()
      thread?.awaitTermination(100, TimeUnit.MILLISECONDS)
    } catch (e: Exception) { }
    thread = Executors.newSingleThreadExecutor()
    thread?.execute post@{
      try {
        writer?.let { writer ->
          commandsManager.sendClose(writer)
        }
        writer?.close()
        writer = null
        closeConnection()
      } catch (e: IOException) {
        Log.e(TAG, "disconnect error", e)
      }
    }
    try {
      thread?.shutdownNow()
      thread?.awaitTermination(200, TimeUnit.MILLISECONDS)
      thread = null
    } catch (e: Exception) { }
    if (clear) {
      reTries = numRetry
      doingRetry = false
      isStreaming = false
      connectCheckerRtmp.onDisconnectRtmp()
    }
    publishPermitted = false
    commandsManager.reset()
  }

  fun sendVideo(h264Buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
    rtmpSender.sendVideoFrame(h264Buffer, info)
  }

  fun sendAudio(aacBuffer: ByteBuffer, info: MediaCodec.BufferInfo) {
    rtmpSender.sendAudioFrame(aacBuffer, info)
  }

  fun hasCongestion(): Boolean {
    return rtmpSender.hasCongestion()
  }

  fun resetSentAudioFrames() {
    rtmpSender.resetSentAudioFrames()
  }

  fun resetSentVideoFrames() {
    rtmpSender.resetSentVideoFrames()
  }

  fun resetDroppedAudioFrames() {
    rtmpSender.resetDroppedAudioFrames()
  }

  fun resetDroppedVideoFrames() {
    rtmpSender.resetDroppedVideoFrames()
  }

  @Throws(RuntimeException::class)
  fun resizeCache(newSize: Int) {
    rtmpSender.resizeCache(newSize)
  }

  fun setLogs(enable: Boolean) {
    rtmpSender.setLogs(enable)
  }
}