package net.ossrs.rtmp;

/**
 * Created by pedro on 25/01/17.
 */

public interface ConnectCheckerRtmp {

    void onConnectionSuccessRtmp();

    void onConnectionFailedRtmp(String reason);

    void onDisconnectRtmp();

    void onAuthErrorRtmp();

    void onAuthSuccessRtmp();
}
