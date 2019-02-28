package com.pedro.rtsp.rtsp;

import android.media.MediaCodec;
import android.util.Base64;
import android.util.Log;
import com.pedro.rtsp.rtp.packets.AacPacket;
import com.pedro.rtsp.rtp.packets.H264Packet;
import com.pedro.rtsp.utils.AuthUtil;
import com.pedro.rtsp.utils.ConnectCheckerRtsp;
import com.pedro.rtsp.utils.CreateSSLSocket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by pedro on 10/02/17.
 */

public class RtspClient {

  private final String TAG = "RtspClient";
  private static final Pattern rtspUrlPattern =
      Pattern.compile("^rtsp://([^/:]+)(:(\\d+))*/([^/]+)(/(.*))*$");
  private static final Pattern rtspsUrlPattern =
      Pattern.compile("^rtsps://([^/:]+)(:(\\d+))*/([^/]+)(/(.*))*$");

  private final long timestamp;
  private String host = "";
  private int port;
  private String path;
  private int sampleRate = 44100;
  private boolean isStereo = true;

  private final int trackVideo = 1;
  private final int trackAudio = 0;
  private Protocol protocol = Protocol.TCP;
  private int mCSeq = 0;
  private String authorization = null;
  private String user;
  private String password;
  private String sessionId;
  private ConnectCheckerRtsp connectCheckerRtsp;

  //sockets objects
  private Socket connectionSocket;
  private BufferedReader reader;
  private BufferedWriter writer;
  private Thread thread;
  private byte[] sps, pps;
  //default sps and pps to work only audio
  private String defaultSPS = "Z0KAHtoHgUZA";
  private String defaultPPS = "aM4NiA==";
  //for udp
  private int[] audioPorts = new int[] { 5000, 5001 };
  private int[] videoPorts = new int[] { 5002, 5003 };
  //for tcp
  private OutputStream outputStream;
  private volatile boolean streaming = false;
  //for secure transport
  private boolean tlsEnabled = false;
  //packets
  private H264Packet h264Packet;
  private AacPacket aacPacket;

  public RtspClient(ConnectCheckerRtsp connectCheckerRtsp) {
    this.connectCheckerRtsp = connectCheckerRtsp;
    long uptime = System.currentTimeMillis();
    timestamp = (uptime / 1000) << 32 & (((uptime - ((uptime / 1000) * 1000)) >> 32)
        / 1000); // NTP timestamp
  }

  public void setProtocol(Protocol protocol) {
    this.protocol = protocol;
  }

  public void setAuthorization(String user, String password) {
    this.user = user;
    this.password = password;
  }

  public boolean isStreaming() {
    return streaming;
  }

  public void setUrl(String url) {
    Matcher rtspMatcher = rtspUrlPattern.matcher(url);
    Matcher rtspsMatcher = rtspsUrlPattern.matcher(url);
    Matcher matcher;
    if (rtspMatcher.matches()) {
      matcher = rtspMatcher;
      tlsEnabled = false;
    } else if (rtspsMatcher.matches()) {
      matcher = rtspsMatcher;
      tlsEnabled = true;
    } else {
      streaming = false;
      connectCheckerRtsp.onConnectionFailedRtsp("Endpoint malformed, should be: rtsp://ip:port/appname/streamname");
      return;
    }
    host = matcher.group(1);
    port = Integer.parseInt((matcher.group(3) != null) ? matcher.group(3) : "1935");
    path = "/" + matcher.group(4) + "/" + matcher.group(6);
  }

  public OutputStream getOutputStream() {
    return outputStream;
  }

  public void setSampleRate(int sampleRate) {
    this.sampleRate = sampleRate;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getPath() {
    return path;
  }

  public ConnectCheckerRtsp getConnectCheckerRtsp() {
    return connectCheckerRtsp;
  }

  public void setSPSandPPS(ByteBuffer sps, ByteBuffer pps) {
    byte[] mSPS = new byte[sps.capacity() - 4];
    sps.position(4);
    sps.get(mSPS, 0, mSPS.length);
    byte[] mPPS = new byte[pps.capacity() - 4];
    pps.position(4);
    pps.get(mPPS, 0, mPPS.length);
    this.sps = mSPS;
    this.pps = mPPS;
  }

  public void setIsStereo(boolean isStereo) {
    this.isStereo = isStereo;
  }

  public void connect() {
    if (!streaming) {
      h264Packet = new H264Packet(this, protocol);
      if (sps != null && pps != null) {
        h264Packet.setSPSandPPS(sps, pps);
      }
      aacPacket = new AacPacket(this, protocol);
      aacPacket.setSampleRate(sampleRate);
      thread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            if (!tlsEnabled) {
              connectionSocket = new Socket();
              SocketAddress socketAddress = new InetSocketAddress(host, port);
              connectionSocket.connect(socketAddress, 3000);
            } else {
              connectionSocket = CreateSSLSocket.createSSlSocket(host, port);
            }
            reader = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            outputStream = connectionSocket.getOutputStream();
            writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            writer.write(sendOptions());
            writer.flush();
            getResponse(false, false);
            writer.write(sendAnnounce());
            writer.flush();
            //check if you need credential for stream, if you need try connect with credential
            String response = getResponse(false, false);
            int status = getResponseStatus(response);
            if (status == 403) {
              connectCheckerRtsp.onConnectionFailedRtsp("Error configure stream, access denied");
              Log.e(TAG, "Response 403, access denied");
              return;
            } else if (status == 401) {
              if (user == null || password == null) {
                connectCheckerRtsp.onAuthErrorRtsp();
                return;
              } else {
                writer.write(sendAnnounceWithAuth(response));
                writer.flush();
                int statusAuth = getResponseStatus(getResponse(false, false));
                if (statusAuth == 401) {
                  connectCheckerRtsp.onAuthErrorRtsp();
                  return;
                } else if (statusAuth == 200) {
                  connectCheckerRtsp.onAuthSuccessRtsp();
                } else {
                  connectCheckerRtsp.onConnectionFailedRtsp("Error configure stream, announce with auth failed");
                }
              }
            } else if (status != 200) {
              connectCheckerRtsp.onConnectionFailedRtsp("Error configure stream, announce failed");
            }
            writer.write(sendSetup(trackAudio, protocol));
            writer.flush();
            getResponse(true, true);
            writer.write(sendSetup(trackVideo, protocol));
            writer.flush();
            getResponse(false, true);
            writer.write(sendRecord());
            writer.flush();
            getResponse(false, true);

            h264Packet.updateDestinationVideo();
            aacPacket.updateDestinationAudio();
            streaming = true;
            connectCheckerRtsp.onConnectionSuccessRtsp();
          } catch (IOException | NullPointerException e) {
            Log.e(TAG, "connection error", e);
            connectCheckerRtsp.onConnectionFailedRtsp("Error configure stream, " + e.getMessage());
            streaming = false;
          }
        }
      });
      thread.start();
    }
  }

  public void disconnect() {
    if (streaming) {
      thread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            writer.write(sendTearDown());
            connectionSocket.close();
          } catch (IOException e) {
            Log.e(TAG, "disconnect error", e);
          }
          connectCheckerRtsp.onDisconnectRtsp();
          streaming = false;
        }
      });
      thread.start();
      if (h264Packet != null && aacPacket != null) {
        h264Packet.close();
        aacPacket.close();
      }
      mCSeq = 0;
      sps = null;
      pps = null;
      sessionId = null;
    }
  }

  private String sendAnnounce() {
    String body = createBody();
    String announce;
    if (authorization == null) {
      announce = "ANNOUNCE rtsp://"
          + host
          + ":"
          + port
          + path
          + " RTSP/1.0\r\n"
          + "CSeq: "
          + (++mCSeq)
          + "\r\n"
          + "Content-Length: "
          + body.length()
          + "\r\n"
          + "Content-Type: application/sdp\r\n\r\n"
          + body;
    } else {
      announce = "ANNOUNCE rtsp://"
          + host
          + ":"
          + port
          + path
          + " RTSP/1.0\r\n"
          + "CSeq: "
          + (++mCSeq)
          + "\r\n"
          + "Content-Length: "
          + body.length()
          + "\r\n"
          + "Authorization: "
          + authorization
          + "\r\n"
          + "Content-Type: application/sdp\r\n\r\n"
          + body;
    }
    Log.i(TAG, announce);
    return announce;
  }

  private String createBody() {
    String sSPS;
    String sPPS;
    if (sps != null && pps != null) {
      sSPS = Base64.encodeToString(sps, 0, sps.length, Base64.NO_WRAP);
      sPPS = Base64.encodeToString(pps, 0, pps.length, Base64.NO_WRAP);
    } else {
      sSPS = defaultSPS;
      sPPS = defaultPPS;
    }
    return "v=0\r\n"
        +
        // TODO: Add IPV6 support
        "o=- "
        + timestamp
        + " "
        + timestamp
        + " IN IP4 "
        + "127.0.0.1"
        + "\r\n"
        + "s=Unnamed\r\n"
        + "i=N/A\r\n"
        + "c=IN IP4 "
        + host
        + "\r\n"
        +
        // means the session is permanent
        "t=0 0\r\n"
        + "a=recvonly\r\n"
        + Body.createAudioBody(trackAudio, sampleRate, isStereo)
        + Body.createVideoBody(trackVideo, sSPS, sPPS);
  }

  private String sendSetup(int track, Protocol protocol) {
    String params =
        (protocol == Protocol.UDP) ? ("UDP;unicast;client_port=" + (5000 + 2 * track) + "-" + (5000
            + 2 * track
            + 1) + ";mode=record")
            : ("TCP;interleaved=" + 2 * track + "-" + (2 * track + 1) + ";mode=record");
    String setup = "SETUP rtsp://"
        + host
        + ":"
        + port
        + path
        + "/trackID="
        + track
        + " RTSP/1.0\r\n"
        + "Transport: RTP/AVP/"
        + params
        + "\r\n"
        + addHeaders(authorization);
    Log.i(TAG, setup);
    return setup;
  }

  private String sendOptions() {
    String options =
        "OPTIONS rtsp://" + host + ":" + port + path + " RTSP/1.0\r\n" + addHeaders(authorization);
    Log.i(TAG, options);
    return options;
  }

  private String sendRecord() {
    String record = "RECORD rtsp://"
        + host
        + ":"
        + port
        + path
        + " RTSP/1.0\r\n"
        + "Range: npt=0.000-\r\n"
        + addHeaders(authorization);
    Log.i(TAG, record);
    return record;
  }

  private String sendTearDown() {
    String teardown =
        "TEARDOWN rtsp://" + host + ":" + port + path + " RTSP/1.0\r\n" + addHeaders(authorization);
    Log.i(TAG, teardown);
    return teardown;
  }

  private String addHeaders(String authorization) {
    return "CSeq: "
        + (++mCSeq)
        + "\r\n"
        + "Content-Length: 0\r\n"
        + (sessionId != null ? "Session: " + sessionId + "\r\n" : "")
        // For some reason you may have to remove last "\r\n" in the next line to make the RTSP client work with your wowza server :/
        + (authorization != null ? "Authorization: " + authorization + "\r\n" : "")
        + "\r\n";
  }

  private String getResponse(boolean isAudio, boolean checkStatus) {
    try {
      String response = "";
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains("Session")) {
          Pattern rtspPattern = Pattern.compile("Session: (\\w+)");
          Matcher matcher = rtspPattern.matcher(line);
          if (matcher.find()) {
            sessionId = matcher.group(1);
          }
          sessionId = line.split(";")[0].split(":")[1].trim();
        }
        if (line.contains("server_port")) {
          Pattern rtspPattern = Pattern.compile("server_port=([0-9]+)-([0-9]+)");
          Matcher matcher = rtspPattern.matcher(line);
          if (matcher.find()) {
            if (isAudio) {
              audioPorts[0] = Integer.parseInt(matcher.group(1));
              audioPorts[1] = Integer.parseInt(matcher.group(2));
            } else {
              videoPorts[0] = Integer.parseInt(matcher.group(1));
              videoPorts[1] = Integer.parseInt(matcher.group(2));
            }
          }
        }
        response += line + "\n";
        //end of response
        if (line.length() < 3) break;
      }
      if (checkStatus && getResponseStatus(response) != 200) {
        connectCheckerRtsp.onConnectionFailedRtsp("Error configure stream, " + response);
      }
      Log.i(TAG, response);
      return response;
    } catch (IOException e) {
      Log.e(TAG, "read error", e);
      return null;
    }
  }

  private String sendAnnounceWithAuth(String authResponse) {
    authorization = createAuth(authResponse);
    Log.i("Auth", authorization);
    String body = createBody();
    String announce = "ANNOUNCE rtsp://"
        + host
        + ":"
        + port
        + path
        + " RTSP/1.0\r\n"
        + "CSeq: "
        + (++mCSeq)
        + "\r\n"
        + "Content-Length: "
        + body.length()
        + "\r\n"
        + "Authorization: "
        + authorization
        + "\r\n"
        + "Content-Type: application/sdp\r\n\r\n"
        + body;
    Log.i(TAG, announce);
    return announce;
  }

  private String createAuth(String authResponse) {
    Pattern authPattern =
        Pattern.compile("realm=\"(.+)\",\\s+nonce=\"(\\w+)\"", Pattern.CASE_INSENSITIVE);
    Matcher matcher = authPattern.matcher(authResponse);
    matcher.find();
    String realm = matcher.group(1);
    String nonce = matcher.group(2);
    String hash1 = AuthUtil.getMd5Hash(user + ":" + realm + ":" + password);
    String hash2 = AuthUtil.getMd5Hash("ANNOUNCE:rtsp://" + host + ":" + port + path);
    String hash3 = AuthUtil.getMd5Hash(hash1 + ":" + nonce + ":" + hash2);
    return "Digest username=\""
        + user
        + "\",realm=\""
        + realm
        + "\",nonce=\""
        + nonce
        + "\",uri=\"rtsp://"
        + host
        + ":"
        + port
        + path
        + "\",response=\""
        + hash3
        + "\"";
  }

  private int getResponseStatus(String response) {
    Matcher matcher =
        Pattern.compile("RTSP/\\d.\\d (\\d+) (\\w+)", Pattern.CASE_INSENSITIVE).matcher(response);
    if (matcher.find()) {
      return Integer.parseInt(matcher.group(1));
    } else {
      return -1;
    }
  }

  public int[] getAudioPorts() {
    return audioPorts;
  }

  public int[] getVideoPorts() {
    return videoPorts;
  }

  public void sendVideo(ByteBuffer h264Buffer, MediaCodec.BufferInfo info) {
    if (isStreaming()) {
      h264Packet.createAndSendPacket(h264Buffer, info);
    }
  }

  public void sendAudio(ByteBuffer aacBuffer, MediaCodec.BufferInfo info) {
    if (isStreaming()) {
      aacPacket.createAndSendPacket(aacBuffer, info);
    }
  }
}

