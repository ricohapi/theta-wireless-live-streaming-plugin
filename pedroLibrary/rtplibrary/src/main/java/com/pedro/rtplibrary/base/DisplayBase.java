package com.pedro.rtplibrary.base;

import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.SurfaceView;
import com.pedro.encoder.audio.AudioEncoder;
import com.pedro.encoder.audio.GetAacData;
import com.pedro.encoder.input.audio.GetMicrophoneData;
import com.pedro.encoder.input.audio.MicrophoneManager;
import com.pedro.encoder.input.video.GetCameraData;
import com.pedro.encoder.video.FormatVideoEncoder;
import com.pedro.encoder.video.GetH264Data;
import com.pedro.encoder.video.VideoEncoder;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;

/**
 * Wrapper to stream display screen of your device and microphone.
 * Can be executed in background.
 *
 * API requirements:
 * API 21+.
 *
 * Created by pedro on 9/08/17.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public abstract class DisplayBase
    implements GetAacData, GetCameraData, GetH264Data, GetMicrophoneData {

  protected Context context;
  private MediaProjection mediaProjection;
  private MediaProjectionManager mediaProjectionManager;
  protected VideoEncoder videoEncoder;
  protected MicrophoneManager microphoneManager;
  protected AudioEncoder audioEncoder;
  private boolean streaming;
  protected SurfaceView surfaceView;
  private boolean videoEnabled = true;
  //record
  private MediaMuxer mediaMuxer;
  private int videoTrack = -1;
  private int audioTrack = -1;
  private boolean recording = false;
  private boolean canRecord = false;
  private MediaFormat videoFormat;
  private MediaFormat audioFormat;
  private int dpi = 320;

  public DisplayBase(Context context) {
    this.context = context;
    mediaProjectionManager =
        ((MediaProjectionManager) context.getSystemService(MEDIA_PROJECTION_SERVICE));
    this.surfaceView = null;
    videoEncoder = new VideoEncoder(this);
    microphoneManager = new MicrophoneManager(this);
    audioEncoder = new AudioEncoder(this);
    streaming = false;
  }

  /**
   * Basic auth developed to work with Wowza. No tested with other server
   *
   * @param user auth.
   * @param password auth.
   */
  public abstract void setAuthorization(String user, String password);

  /**
   * Call this method before use @startStream. If not you will do a stream without video.
   *
   * @param width resolution in px.
   * @param height resolution in px.
   * @param fps frames per second of the stream.
   * @param bitrate H264 in kb.
   * @param rotation could be 90, 180, 270 or 0 (Normally 0 if you are streaming in landscape or 90
   * if you are streaming in Portrait). This only affect to stream result. This work rotating with
   * encoder.
   * NOTE: Rotation with encoder is silence ignored in some devices.
   * @param dpi of your screen device.
   * @return true if success, false if you get a error (Normally because the encoder selected
   * doesn't support any configuration seated or your device hasn't a H264 encoder).
   */
  public boolean prepareVideo(int width, int height, int fps, int bitrate, int rotation, int dpi) {
    this.dpi = dpi;
    int imageFormat = ImageFormat.NV21; //supported nv21 and yv12
    videoEncoder.setImageFormat(imageFormat);
    boolean result = videoEncoder.prepareVideoEncoder(width, height, fps, bitrate, rotation, true,
        FormatVideoEncoder.SURFACE);
    return result;
  }

  protected abstract void prepareAudioRtp(boolean isStereo, int sampleRate);

  /**
   * Call this method before use @startStream. If not you will do a stream without audio.
   *
   * @param bitrate AAC in kb.
   * @param sampleRate of audio in hz. Can be 8000, 16000, 22500, 32000, 44100.
   * @param isStereo true if you want Stereo audio (2 audio channels), false if you want Mono audio
   * (1 audio channel).
   * @param echoCanceler true enable echo canceler, false disable.
   * @param noiseSuppressor true enable noise suppressor, false  disable.
   * @return true if success, false if you get a error (Normally because the encoder selected
   * doesn't support any configuration seated or your device hasn't a AAC encoder).
   */
  public boolean prepareAudio(int bitrate, int sampleRate, boolean isStereo, boolean echoCanceler,
      boolean noiseSuppressor) {
    microphoneManager.createMicrophone(sampleRate, isStereo, echoCanceler, noiseSuppressor);
    prepareAudioRtp(isStereo, sampleRate);
    return audioEncoder.prepareAudioEncoder(bitrate, sampleRate, isStereo);
  }

  /**
   * Same to call:
   * rotation = 0;
   * if (Portrait) rotation = 90;
   * prepareVideo(640, 480, 30, 1200 * 1024, true, 0);
   *
   * @return true if success, false if you get a error (Normally because the encoder selected
   * doesn't support any configuration seated or your device hasn't a H264 encoder).
   */
  public boolean prepareVideo() {
    return videoEncoder.prepareVideoEncoder(640, 480, 30, 1200 * 1024, 0, true,
        FormatVideoEncoder.SURFACE);
  }

  /**
   * Same to call:
   * prepareAudio(128 * 1024, 44100, true, false, false);
   *
   * @return true if success, false if you get a error (Normally because the encoder selected
   * doesn't support any configuration seated or your device hasn't a AAC encoder).
   */
  public boolean prepareAudio() {
    microphoneManager.createMicrophone();
    return audioEncoder.prepareAudioEncoder();
  }

  /**
   * Start record a MP4 video. Need be called while stream.
   *
   * @param path where file will be saved.
   * @throws IOException If you init it before start stream.
   */
  public void startRecord(String path) throws IOException {
    if (streaming) {
      mediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
      videoTrack = mediaMuxer.addTrack(videoFormat);
      audioTrack = mediaMuxer.addTrack(audioFormat);
      mediaMuxer.start();
      recording = true;
    } else {
      throw new IOException("Need be called while stream");
    }
  }

  /**
   * Stop record MP4 video started with @startRecord. If you don't call it file will be unreadable.
   */
  public void stopRecord() {
    recording = false;
    canRecord = false;
    if (mediaMuxer != null) {
      mediaMuxer.stop();
      mediaMuxer.release();
      mediaMuxer = null;
    }
    videoTrack = -1;
    audioTrack = -1;
  }

  protected abstract void startStreamRtp(String url);

  /**
   * Create Intent used to init screen capture with startActivityForResult.
   *
   * @return intent to startActivityForResult.
   */
  public Intent sendIntent() {
    return mediaProjectionManager.createScreenCaptureIntent();
  }

  /**
   * Need be called after @prepareVideo or/and @prepareAudio.
   *
   * @param url of the stream like:
   * protocol://ip:port/application/streamName
   *
   * RTSP: rtsp://192.168.1.1:1935/live/pedroSG94
   * RTSPS: rtsps://192.168.1.1:1935/live/pedroSG94
   * RTMP: rtmp://192.168.1.1:1935/live/pedroSG94
   * RTMPS: rtmps://192.168.1.1:1935/live/pedroSG94
   */
  public void startStream(String url, int resultCode, Intent data) {
    videoEncoder.start();
    audioEncoder.start();
    mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
    mediaProjection.createVirtualDisplay("Stream Display", videoEncoder.getWidth(),
        videoEncoder.getHeight(), dpi, 0, videoEncoder.getInputSurface(), null, null);
    microphoneManager.start();
    streaming = true;
    startStreamRtp(url);
  }

  protected abstract void stopStreamRtp();

  /**
   * Stop stream started with @startStream.
   */
  public void stopStream() {
    microphoneManager.stop();
    if (mediaProjection != null) {
      mediaProjection.stop();
    }
    stopStreamRtp();
    videoEncoder.stop();
    audioEncoder.stop();
    streaming = false;
  }

  /**
   * Mute microphone, can be called before, while and after stream.
   */
  public void disableAudio() {
    microphoneManager.mute();
  }

  /**
   * Enable a muted microphone, can be called before, while and after stream.
   */
  public void enableAudio() {
    microphoneManager.unMute();
  }

  /**
   * Get mute state of microphone.
   *
   * @return true if muted, false if enabled
   */
  public boolean isAudioMuted() {
    return microphoneManager.isMuted();
  }

  /**
   * Get video camera state
   *
   * @return true if disabled, false if enabled
   */
  public boolean isVideoEnabled() {
    return videoEnabled;
  }

  /**
   * Disable send camera frames and send a black image with low bitrate(to reduce bandwith used)
   * instance it.
   */
  public void disableVideo() {
    videoEncoder.startSendBlackImage();
    videoEnabled = false;
  }

  /**
   * Enable send display screen frames.
   */
  public void enableVideo() {
    videoEncoder.stopSendBlackImage();
    videoEnabled = true;
  }

  /**
   * Se video bitrate of H264 in kb while stream.
   *
   * @param bitrate H264 in kb.
   */
  public void setVideoBitrateOnFly(int bitrate) {
    if (Build.VERSION.SDK_INT >= 19) {
      videoEncoder.setVideoBitrateOnFly(bitrate);
    }
  }

  /**
   * Get stream state.
   *
   * @return true if streaming, false if not streaming.
   */
  public boolean isStreaming() {
    return streaming;
  }

  /**
   * Get record state.
   *
   * @return true if recording, false if not recoding.
   */
  public boolean isRecording() {
    return recording;
  }

  protected abstract void getAacDataRtp(ByteBuffer aacBuffer, MediaCodec.BufferInfo info);

  @Override
  public void getAacData(ByteBuffer aacBuffer, MediaCodec.BufferInfo info) {
    if (recording && canRecord) {
      mediaMuxer.writeSampleData(audioTrack, aacBuffer, info);
    }
    getAacDataRtp(aacBuffer, info);
  }

  protected abstract void onSPSandPPSRtp(ByteBuffer sps, ByteBuffer pps);

  @Override
  public void onSPSandPPS(ByteBuffer sps, ByteBuffer pps) {
    onSPSandPPSRtp(sps, pps);
  }

  protected abstract void getH264DataRtp(ByteBuffer h264Buffer, MediaCodec.BufferInfo info);

  @Override
  public void getH264Data(ByteBuffer h264Buffer, MediaCodec.BufferInfo info) {
    if (recording) {
      if (info.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) canRecord = true;
      if (canRecord) {
        mediaMuxer.writeSampleData(videoTrack, h264Buffer, info);
      }
    }
    getH264DataRtp(h264Buffer, info);
  }

  @Override
  public void inputPCMData(byte[] buffer, int size) {
    audioEncoder.inputPCMData(buffer, size);
  }

  @Override
  public void inputYUVData(byte[] buffer) {
    videoEncoder.inputYUVData(buffer);
  }

  @Override
  public void onVideoFormat(MediaFormat mediaFormat) {
    videoFormat = mediaFormat;
  }

  @Override
  public void onAudioFormat(MediaFormat mediaFormat) {
    audioFormat = mediaFormat;
  }
}

