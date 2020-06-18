package com.deepblue.librariesx

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.deepblue.logd.Log
import com.deepblue.owlrtmpplayer.widget.OwlVideoView

class VideoPlayer : AppCompatActivity() {
    var mVideoLinearLayout: LinearLayout? = null
    //新播放器类
    var mNewVideoPlayer: OwlVideoView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        mVideoLinearLayout = findViewById(R.id.video)

        mNewVideoPlayer = OwlVideoView(this)
        addVideoView()
        mNewVideoPlayer!!.setVideoPath("rtmp://58.200.131.2:1935/livetv/hunantv")
        mNewVideoPlayer!!.start()
    }

    fun addVideoView() {
        if (mNewVideoPlayer != null) {
            mVideoLinearLayout!!.removeAllViews()
            mVideoLinearLayout!!.addView(mNewVideoPlayer, FeverLinearLayoutParams())
        } else {
            Log.w("MainActivity", "add video view failed!")
        }
    }
    private fun FeverLinearLayoutParams(): LinearLayout.LayoutParams? {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
    }

    override fun onPause() {
        super.onPause()
        if (mNewVideoPlayer != null) {
            mNewVideoPlayer!!.fakePause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (mNewVideoPlayer != null) {
            mNewVideoPlayer!!.fakeResume()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mNewVideoPlayer != null) {
            mNewVideoPlayer!!.release(true)
        }
    }
}
