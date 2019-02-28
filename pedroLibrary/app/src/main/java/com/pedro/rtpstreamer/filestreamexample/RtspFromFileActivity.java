package com.pedro.rtpstreamer.filestreamexample;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pedro.rtpstreamer.utils.PathUtils;
import com.pedro.rtplibrary.rtsp.RtspFromFile;
import com.pedro.encoder.input.decoder.VideoDecoderInterface;
import com.pedro.rtpstreamer.R;
import com.pedro.rtsp.utils.ConnectCheckerRtsp;

import java.io.IOException;

/**
 * More documentation see:
 * {@link com.pedro.rtplibrary.base.FromFileBase}
 * {@link com.pedro.rtplibrary.rtsp.RtspFromFile}
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class RtspFromFileActivity extends AppCompatActivity
    implements ConnectCheckerRtsp, View.OnClickListener, VideoDecoderInterface {

  private RtspFromFile rtspFromFile;
  private Button button, bSelectFile;
  private EditText etUrl;
  private TextView tvFile;
  private String filePath = "";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_from_file);
    button = findViewById(R.id.b_start_stop);
    bSelectFile = findViewById(R.id.b_select_file);
    button.setOnClickListener(this);
    bSelectFile.setOnClickListener(this);
    etUrl = findViewById(R.id.et_rtp_url);
    etUrl.setHint(R.string.hint_rtsp);
    tvFile = findViewById(R.id.tv_file);
    rtspFromFile = new RtspFromFile(this, this);
  }

  @Override
  public void onConnectionSuccessRtsp() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(RtspFromFileActivity.this, "Connection success", Toast.LENGTH_SHORT).show();
      }
    });
  }

  @Override
  public void onConnectionFailedRtsp(final String reason) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(RtspFromFileActivity.this, "Connection failed. " + reason,
            Toast.LENGTH_SHORT).show();
        rtspFromFile.stopStream();
        button.setText(R.string.start_button);
      }
    });
  }

  @Override
  public void onDisconnectRtsp() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(RtspFromFileActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
      }
    });
  }

  @Override
  public void onAuthErrorRtsp() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(RtspFromFileActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
      }
    });
  }

  @Override
  public void onAuthSuccessRtsp() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(RtspFromFileActivity.this, "Auth success", Toast.LENGTH_SHORT).show();
      }
    });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 5 && data != null) {
      filePath = PathUtils.getPath(this, data.getData());
      Toast.makeText(this, filePath, Toast.LENGTH_SHORT).show();
      tvFile.setText(filePath);
    }
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.b_start_stop:
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
          if (!rtspFromFile.isStreaming()) {
            try {
              if (rtspFromFile.prepareVideo(filePath, 1200 * 1024)) {
                button.setText(R.string.stop_button);
                rtspFromFile.startStream(etUrl.getText().toString());
              } else {
                button.setText(R.string.start_button);
                rtspFromFile.stopStream();
                /*This error could be 2 things.
                 Your device cant decode or encode this file or
                 the file is not supported for the library.
                The file need has h264 video codec and acc audio codec*/
                Toast.makeText(this, "Error: unsupported file", Toast.LENGTH_SHORT).show();
              }
            } catch (IOException e) {
              //Normally this error is for file not found or read permissions
              Toast.makeText(this, "Error: file not found", Toast.LENGTH_SHORT).show();
            }
          } else {
            button.setText(R.string.start_button);
            rtspFromFile.stopStream();
          }
        }
        break;
      case R.id.b_select_file:
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/mp4");
        startActivityForResult(intent, 5);
        break;
      default:
        break;
    }
  }

  @Override
  public void onVideoDecoderFinished() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (rtspFromFile.isStreaming()) {
          button.setText(R.string.start_button);
          Toast.makeText(RtspFromFileActivity.this, "Video stream finished", Toast.LENGTH_SHORT)
              .show();
          rtspFromFile.stopStream();
        }
      }
    });
  }
}
