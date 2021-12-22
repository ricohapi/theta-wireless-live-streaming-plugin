package com.pedro.rtplibrary.network;

/*
 *  Copyright (c) 2015, Facebook, Inc.
 *  All rights reserved.
 *
 *  This source code is licensed under the BSD-style license found in the
 *  LICENSE file in the root directory of this source tree. An additional grant
 *  of patent rights can be found in the PATENTS file in the same directory.
 *
 */

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class used to read from the file {@code /proc/net/xt_qtaguid/stats} periodically, in order to
 * determine a ConnectionClass.
 */

@Deprecated
public class UploadBandwidthSampler {

  /**
   * Time between polls in ms.
   */
  static final long SAMPLE_TIME = 1000;

  /**
   * The DownloadBandwidthManager that keeps track of the moving average and ConnectionClass.
   */
  private final ConnectionClassManager mConnectionClassManager;

  private AtomicInteger mSamplingCounter;

  private Handler mHandler;
  private HandlerThread mThread;

  private long mLastTimeReading;

  // Singleton.
  private static class UploadBandwidthSamplerHolder {
    public static final UploadBandwidthSampler instance =
        new UploadBandwidthSampler(ConnectionClassManager.getInstance());
  }

  /**
   * Retrieval method for the DeviceBandwidthSampler singleton.
   *
   * @return The singleton instance of DeviceBandwidthSampler.
   */
  public static UploadBandwidthSampler getInstance() {
    return UploadBandwidthSamplerHolder.instance;
  }

  private UploadBandwidthSampler(ConnectionClassManager connectionClassManager) {
    mConnectionClassManager = connectionClassManager;
    mSamplingCounter = new AtomicInteger();
    mThread = new HandlerThread("ParseThread");
    mThread.start();
    mHandler = new SamplingHandler(mThread.getLooper());
  }

  /**
   * Method call to start sampling for download bandwidth.
   */
  public void startSampling() {
    if (mSamplingCounter.getAndIncrement() == 0) {
      mHandler.sendEmptyMessage(SamplingHandler.MSG_START);
      mLastTimeReading = SystemClock.elapsedRealtime();
    }
  }

  /**
   * Finish sampling and prevent further changes to the
   * ConnectionClass until another timer is started.
   */
  public void stopSampling() {
    if (mSamplingCounter.decrementAndGet() == 0) {
      mHandler.sendEmptyMessage(SamplingHandler.MSG_STOP);
    }
  }

  private class SamplingHandler extends Handler {
    static final int MSG_START = 1;
    static final int MSG_STOP = 2;

    public SamplingHandler(Looper looper) {
      super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_START:
          addSample();
          sendEmptyMessageDelayed(MSG_START, SAMPLE_TIME);
          break;
        case MSG_STOP:
          addSample();
          removeMessages(MSG_START);
          break;
        default:
          throw new IllegalArgumentException("Unknown what=" + msg.what);
      }
    }

    /**
     * Method for polling for the change in total bytes since last update and
     * adding it to the BandwidthManager.
     */
    private void addSample() {
      long byteDiff = QTagTxParser.getInstance().parseDataUsageForUidAndTag(Process.myUid());
      synchronized (this) {
        long curTimeReading = SystemClock.elapsedRealtime();
        if (byteDiff != -1) {
          mConnectionClassManager.addBandwidth(byteDiff, curTimeReading - mLastTimeReading);
        }
        mLastTimeReading = curTimeReading;
      }
    }
  }

  /**
   * @return True if there are still threads which are sampling, false otherwise.
   */
  public boolean isSampling() {
    return (mSamplingCounter.get() != 0);
  }
}