package com.deepblue.owlrtmpplayer.widget;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.deepblue.owlrtmpplayer.R;
import com.deepblue.owlrtmpplayer.render.IRenderView;
import com.deepblue.owlrtmpplayer.render.SurfaceRenderView;
import com.deepblue.owlrtmpplayer.render.TextureRenderView;
import com.deepblue.owlrtmpplayer.utils.MediaPlayerCompat;
import com.deepblue.owlrtmpplayer.view.BufferProgress;
import com.deepblue.owlrtmpplayer.view.StartPlayView;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;

public class OwlVideoView extends FrameLayout implements View.OnClickListener, MediaController.MediaPlayerControl {
    private String TAG = "OwlVideoView";
    // settable by the client
    private Uri mUri;
    private Map<String, String> mHeaders;

    // all possible internal states
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    // mCurrentState is a VideoView object's current state.
    // mTargetState is the state that a method caller intends to reach.
    // For instance, regardless the VideoView object's current state,
    // calling pause() intends to bring the object to a target state
    // of STATE_PAUSED.
    private int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;

    // All the stuff we need for playing and showing a video
    private IRenderView.ISurfaceHolder mSurfaceHolder = null;
    private IjkMediaPlayer mMediaPlayer = null;
    // private int         mAudioSession;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mVideoRotationDegree;
    private MediaController mMediaController;
    private IMediaPlayer.OnCompletionListener mOnCompletionListener;
    private IMediaPlayer.OnPreparedListener mOnPreparedListener;
    private int mCurrentBufferPercentage;
    private IMediaPlayer.OnErrorListener mOnErrorListener;
    private IMediaPlayer.OnInfoListener mOnInfoListener;
    private IMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private IjkMediaPlayer.OnNativeInvokeListener mOnNativeInvokeListener;
    private int mSeekWhenPrepared;  // recording the seek position while preparing
    private boolean mCanPause = true;
    private boolean mCanSeekBack = true;
    private boolean mCanSeekForward = true;

    private int bufferSize = -1;
    private boolean isAutoPlay = true;

    private static final int MSG_CACHE_DRU = 20160101;

    private static final int CACHE_WATER = 1 * 1000;

    private long mPrepareStartTime = 0;
    private long mPrepareEndTime = 0;

    private long mSeekStartTime = 0;
    private long mSeekEndTime = 0;

    private Context mAppContext;
    private IRenderView mRenderView;
    //渲染控件父类
    private ViewGroup mTextureViewContainer;
    //缓冲的动画
    private BufferProgress mBufferProgress;
    //缓冲的动画
    private StartPlayView mStartPlayView;
    // 用于界面恢复后展示上一次播放的最后一帧画面
    private ImageView mImageView;
    private int mVideoSarNum;
    private int mVideoSarDen;
    private boolean mRenderWithTextureView;
    private boolean isFullState;
    private ViewGroup.LayoutParams mRawParams;
    private float playSpeed = .0f;

    //所有缓冲花费的时间总和
    private long mBufferTimeTotal;
    //一次缓冲花费的时间
    private long mBufferTime;
    //每次缓冲开始的时间
    private long mStartBufferTime;
    //累积缓冲时间阀
    private static int mBufferTotalTime = 1300;
    //是否支持缓冲超时重连
    private boolean isAutoPursue = true;
    //缓冲超时错误
    public static final int ERROR_BUFFER_TIMEOUT = -1001;
    //缓冲超时间隔
    private static final int BUFFER_TIMEOUT = 30000;
    private String ua = null;// user agent

    public boolean isFullState() {
        return isFullState;
    }

    public OwlVideoView(Context context) {
        super(context);
        init(context, null);
    }

    public OwlVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public OwlVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public OwlVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }


    private void init(Context context, AttributeSet attrs) {
        mAppContext = context.getApplicationContext();
        initInflate(mAppContext);
        initVideoView(mAppContext, attrs);
    }

    private void initInflate(Context context) {
        try {
            View.inflate(context, R.layout.owl_video_base, this);
        } catch (InflateException e) {
            e.printStackTrace();
        }
    }

    private void initVideoView(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.up_video_view);
            mRenderWithTextureView = ta.getBoolean(R.styleable.up_video_view_render_with_texture, false);
            ta.recycle();
        }

        IRenderView renderView;
        if (mRenderWithTextureView) {
            Log.i(TAG, "RenderWithTextureView true");
            renderView = new TextureRenderView(getContext());
        } else {
            Log.i(TAG, "RenderWithTextureView false");
            renderView = new SurfaceRenderView(getContext());
        }
        mTextureViewContainer = (ViewGroup) findViewById(R.id.surface_container);
        mBufferProgress = findViewById(R.id.buffer_progress);
        mStartPlayView = findViewById(R.id.start);
        mStartPlayView.setOnClickListener(this);
        setRenderView(renderView);

        mVideoWidth = 0;
        mVideoHeight = 0;
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        mCurrentState = STATE_IDLE;
        mTargetState = STATE_IDLE;

        addImageView();
    }

    private void setRenderView(IRenderView renderView) {
        if (mRenderView != null) {
            if (mMediaPlayer != null)
                mMediaPlayer.setSurface(null);

            View renderUIView = mRenderView.getView();
            mRenderView.removeRenderCallback(mSHCallback);
            mRenderView = null;
            removeView(renderUIView);
        }

        if (renderView == null)
            return;

        mRenderView = renderView;
        renderView.setAspectRatio(mCurrentAspectRatio);
        if (mVideoWidth > 0 && mVideoHeight > 0)
            renderView.setVideoSize(mVideoWidth, mVideoHeight);
        if (mVideoSarNum > 0 && mVideoSarDen > 0)
            renderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);

        View renderUIView = mRenderView.getView();
        LayoutParams lp = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        mTextureViewContainer.addView(renderUIView);

        mRenderView.addRenderCallback(mSHCallback);
        mRenderView.setVideoRotation(mVideoRotationDegree);
    }

    private void addImageView() {
        // 用于界面恢复后展示上一次播放的最后一帧画面
        if (mImageView != null) {
            return;
        }
        mImageView = new ImageView(getContext());
        LayoutParams lp = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        mImageView.setLayoutParams(lp);
        addView(mImageView);
        mImageView.setVisibility(View.GONE);
    }


    /**
     * Sets video path.
     *
     * @param path the path of the video.
     */
    public void setVideoPath(String path) {
        if(mStartPlayView != null) {
            mStartPlayView.setVisibility(GONE);
        }
        setVideoURI(Uri.parse(path));
    }

    /**
     * Sets video URI.
     *
     * @param uri the URI of the video.
     */
    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    /**
     * Sets video URI using specific headers.
     *
     * @param uri     the URI of the video.
     * @param headers the headers for the URI request.
     *                Note that the cross domain redirection is allowed by default, but that can be
     *                changed with key/value pairs through the headers parameter with
     *                "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     *                to disallow or allow cross domain redirection.
     */
    private void setVideoURI(Uri uri, Map<String, String> headers) {
        mUri = uri;
        showBufferProgress();
        if (uri.toString().startsWith("http")) {
            if (headers == null) {
                headers = new HashMap<>();
            }
            headers.put("X-Accept-Video-Encoding", "h265");
        }

        mHeaders = headers;
        mSeekWhenPrepared = 0;
        openVideo();
        requestLayout();
        invalidate();
    }

    // REMOVED: addSubtitleSource
    // REMOVED: mPendingSubtitleTracks

    public void stopPlayback() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            drawBlack();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            mTargetState = STATE_IDLE;
            AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
    }

    private void drawBlack() {
        if (mSurfaceHolder != null) {
            Surface surface;
            if (mSurfaceHolder.getSurfaceHolder() != null) {
                Canvas canvas = mSurfaceHolder.getSurfaceHolder().lockCanvas();
                if (canvas != null) {
                    canvas.drawColor(Color.BLACK);
                    mSurfaceHolder.getSurfaceHolder().unlockCanvasAndPost(canvas);
                }
            } else if ((surface = mSurfaceHolder.openSurface()) != null) {
                Canvas canvas = surface.lockCanvas(null);
                if (canvas != null) {
                    canvas.drawColor(Color.BLACK);
                    surface.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void openVideo() {
        mBufferTimeTotal = 0;
        if (mUri == null || mSurfaceHolder == null) {
            // not ready for playback just yet, will try again later
            return;
        }
        // we shouldn't clear the target state, because somebody might have
        // called start() previously
        release(false);

        AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        try {
            mMediaPlayer = new IjkMediaPlayer();
            mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
            mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
            mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
            mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
            mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", isAutoPlay ? 1 : 0);
            mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-buffer-size", 1024 * 400);
            mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
            mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
            mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 1024 * 5);
            mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec_mpeg4", 1);

            mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "reconnect", 3);
            mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "timeout", 20000);
            mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1);
            mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "an", 1);
            mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 10);
            mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzemaxduration", 100);
            mMediaPlayer.setSpeed(1.04f);

            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnNativeInvokeListener(mNativeInvokeListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mCurrentBufferPercentage = 0;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mMediaPlayer.setDataSource(mAppContext, mUri, mHeaders);
            } else {
                mMediaPlayer.setDataSource(mUri.toString());
            }
            bindSurfaceHolder(mMediaPlayer, mSurfaceHolder);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mPrepareStartTime = System.currentTimeMillis();
            mMediaPlayer.prepareAsync();

            mCurrentState = STATE_PREPARING;
            attachMediaController();
        } catch (IOException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } finally {
            // REMOVED: mPendingSubtitleTracks.clear();
        }
    }

    public void setMediaController(MediaController controller) {
        if (mMediaController != null) {
            mMediaController.hide();
        }
        mMediaController = controller;
        attachMediaController();
    }

    private void attachMediaController() {
        if (mMediaPlayer != null && mMediaController != null) {
            mMediaController.setMediaPlayer(this);
            View anchorView = this.getParent() instanceof View ?
                    (View) this.getParent() : this;
            mMediaController.setAnchorView(anchorView);
            mMediaController.setEnabled(isInPlaybackState());
        }
    }

    IMediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
            new IMediaPlayer.OnVideoSizeChangedListener() {
                public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sarNum, int sarDen) {
                    if (mOnVideoSizeChangedListener != null) {
                        mOnVideoSizeChangedListener.onVideoSizeChanged(mp, width, height, sarNum, sarDen);
                    }
                    mVideoWidth = mp.getVideoWidth();
                    mVideoHeight = mp.getVideoHeight();
                    mVideoSarNum = mp.getVideoSarNum();
                    mVideoSarDen = mp.getVideoSarDen();
                    if (mVideoWidth != 0 && mVideoHeight != 0) {
                        if (mRenderView != null) {
                            mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                            mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
                        }
                        // REMOVED: getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                        requestLayout();
                    }
                }
            };

    IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener() {
        public void onPrepared(IMediaPlayer mp) {
            Log.d(TAG,"onPrepared");
            mPrepareEndTime = System.currentTimeMillis();
            mCurrentState = STATE_PREPARED;
            mMediaPlayer.pause();

            // Get the capabilities of the player for this stream
            // REMOVED: Metadata

            if (mOnPreparedListener != null) {
                mOnPreparedListener.onPrepared(mMediaPlayer);
            }
            if (mMediaController != null) {
                mMediaController.setEnabled(true);
            }
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            Log.d(TAG,"onPrepared mVideoWidth: "+mVideoWidth+" mVideoHeight: "+mVideoHeight);

            int seekToPosition = mSeekWhenPrepared;  // mSeekWhenPrepared may be changed after seekTo() call
            if (seekToPosition != 0) {
                seekTo(seekToPosition);
            }
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                Log.i("@@@@", "video size: " + mVideoWidth +"/"+ mVideoHeight);
                // REMOVED: getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                if (mRenderView != null) {
                    mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                    mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
                    if (!mRenderView.shouldWaitForResize() || mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
                        // We didn't actually change the size (it was already at the size
                        // we need), so we won't get a "surface changed" callback, so
                        // start the video here instead of in the callback.
                        if (mTargetState == STATE_PLAYING) {
                            start();
                            if (mMediaController != null) {
                                mMediaController.show();
                            }
                        } else if (!isPlaying() &&
                                (seekToPosition != 0 || getCurrentPosition() > 0)) {
                            if (mMediaController != null) {
                                // Show the media controls when we're paused into a video and make 'em stick.
                                mMediaController.show(0);
                            }
                        }
                    }
                }
            } else {
                // We don't know the video size yet, but should start anyway.
                // The video size might be reported to us later.
                if (mTargetState == STATE_PLAYING) {
                    start();
                }
            }
        }
    };

    private IMediaPlayer.OnCompletionListener mCompletionListener =
            new IMediaPlayer.OnCompletionListener() {
                public void onCompletion(IMediaPlayer mp) {
                    Log.i(TAG, "onCompletion");
                    mCurrentState = STATE_PLAYBACK_COMPLETED;
                    mTargetState = STATE_PLAYBACK_COMPLETED;
                    if (mMediaController != null) {
                        mMediaController.hide();
                    }
                    if (mOnCompletionListener != null) {
                        mOnCompletionListener.onCompletion(mMediaPlayer);
                    }
                }
            };

    private IjkMediaPlayer.OnNativeInvokeListener mNativeInvokeListener = new IjkMediaPlayer.OnNativeInvokeListener() {
        @Override
        public boolean onNativeInvoke(int what, Bundle args) {
            Log.i(TAG, "onNativeInvoke:" + what);
            if (mOnNativeInvokeListener != null) {
                mOnNativeInvokeListener.onNativeInvoke(what, args);
            }
            return false;
        }
    };

    private IMediaPlayer.OnInfoListener mInfoListener =
            new IMediaPlayer.OnInfoListener() {
                public boolean onInfo(IMediaPlayer mp, int arg1, int arg2) {
                    if (mOnInfoListener != null) {
                        mOnInfoListener.onInfo(mp, arg1, arg2);
                    }
                    Log.d(TAG, "OnInfoListener :"+arg1);
                    switch (arg1) {
                        case IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                            Log.d(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                            Log.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START:");
                            hideBufferProgress();
                            if(mStartPlayView != null) {
                                mStartPlayView.setVisibility(GONE);
                            }
                            dismissLastFrame();
                            break;
                        case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                            Log.e(TAG, "开始缓冲-已经卡顿时间：" + mBufferTimeTotal);
                            mStartBufferTime = System.currentTimeMillis();
                            mBufferTime = 0L;
                            showBufferProgress();
                            if (isAutoPursue) {
                                reportError();
                            }
                            Log.d(TAG, "MEDIA_INFO_BUFFERING_START: **END");
                            break;
                        case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                            Log.e(TAG, "结束缓冲 -已经卡顿时间：" + mBufferTimeTotal+" mStartBufferTime: "+mStartBufferTime);
                            if (mStartBufferTime != 0 && isAutoPursue) {
                                mBufferTime = System.currentTimeMillis() - mStartBufferTime;
                                mBufferTimeTotal = mBufferTimeTotal + mBufferTime;
                                Log.e(TAG, "结束缓冲 -已经卡顿时间：：" + mBufferTimeTotal+" mStartBufferTime: "+mStartBufferTime+" mBufferTime: "+mBufferTime);
                                if (mBufferTimeTotal > mBufferTotalTime) {
                                    Log.e(TAG, "结束缓冲 重新连接到最新流*******************");
                                    mBufferTimeTotal = 0;
                                    resume();
                                } else {
                                    speedPlay(4, mBufferTimeTotal);
                                    cancelShowBufferProgress();
                                }
                            }
                            cancelReport();
                            Log.d(TAG, "MEDIA_INFO_BUFFERING_END:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
//                            recorder.setBandwidth(arg2);
                            Log.d(TAG, "MEDIA_INFO_NETWORK_BANDWIDTH: " + arg2);
                            break;
                        case IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                            Log.d(TAG, "MEDIA_INFO_BAD_INTERLEAVING:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                            Log.d(TAG, "MEDIA_INFO_NOT_SEEKABLE:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                            Log.d(TAG, "MEDIA_INFO_METADATA_UPDATE:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
                            Log.d(TAG, "MEDIA_INFO_UNSUPPORTED_SUBTITLE:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                            Log.d(TAG, "MEDIA_INFO_SUBTITLE_TIMED_OUT:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                            mVideoRotationDegree = arg2;
                            Log.d(TAG, "MEDIA_INFO_VIDEO_ROTATION_CHANGED: " + arg2);
                            if (mRenderView != null)
                                mRenderView.setVideoRotation(arg2);
                            break;
                        case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
                            Log.d(TAG, "MEDIA_INFO_AUDIO_RENDERING_START:");
                            break;

                        case IMediaPlayer.MEDIA_INFO_TIMED_TEXT_ERROR:
                            Log.d(TAG, "MEDIA_INFO_TIMED_TEXT_ERROR:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_AUDIO_DECODED_START:
                            Log.d(TAG, "MEDIA_INFO_AUDIO_DECODED_START:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_VIDEO_DECODED_START:
                            Log.d(TAG, "MEDIA_INFO_VIDEO_DECODED_START:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_OPEN_INPUT:
                            Log.d(TAG, "MEDIA_INFO_OPEN_INPUT:");
                            break;

                        case IMediaPlayer.MEDIA_INFO_FIND_STREAM_INFO:
                            Log.d(TAG, "MEDIA_INFO_FIND_STREAM_INFO:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_COMPONENT_OPEN:
                            Log.d(TAG, "MEDIA_INFO_COMPONENT_OPEN:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_VIDEO_SEEK_RENDERING_START:
                            Log.d(TAG, "MEDIA_INFO_VIDEO_SEEK_RENDERING_START:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_AUDIO_SEEK_RENDERING_START:
                            Log.d(TAG, "MEDIA_INFO_AUDIO_SEEK_RENDERING_START:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_MEDIA_ACCURATE_SEEK_COMPLETE:
                            Log.d(TAG, "MEDIA_INFO_MEDIA_ACCURATE_SEEK_COMPLETE:");
                            break;
                    }
                    return true;
                }
            };

    private void dismissLastFrame() {
        mImageView.setVisibility(View.GONE);
    }

    private IMediaPlayer.OnErrorListener mErrorListener =
            new IMediaPlayer.OnErrorListener() {
                public boolean onError(IMediaPlayer mp, int framework_err, int impl_err) {
                    Log.e(TAG, "Error: " + framework_err + "," + impl_err);

                    mCurrentState = STATE_ERROR;
                    mTargetState = STATE_ERROR;
                    if (mMediaController != null) {
                        mMediaController.hide();
                    }
                    /* If an error handler has been supplied, use it and finish. */
                    if (mOnErrorListener != null) {
                        if (mOnErrorListener.onError(mMediaPlayer, framework_err, impl_err)) {
                            return true;
                        }
                    }
                    //show play button
                    hideBufferProgress();
                    if(mStartPlayView != null) {
                        mStartPlayView.setVisibility(VISIBLE);
                    }
                    String messageId;

                    if (framework_err == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
                        messageId = "Invalid progressive playback";
                    } else {
                        messageId = "Unknown: "+framework_err;
                    }
                    Toast toast = Toast.makeText(getContext(), null, Toast.LENGTH_SHORT);
                    toast.setText(getResources().getString(R.string.error_toast));
                    toast.show();
                    return true;
                }
            };

    private IMediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
            new IMediaPlayer.OnBufferingUpdateListener() {
                public void onBufferingUpdate(IMediaPlayer mp, int percent) {
                    //Log.d(TAG, "onBufferingUpdate : "+percent);
                    mCurrentBufferPercentage = percent;
                }
            };

    private IMediaPlayer.OnSeekCompleteListener mSeekCompleteListener = new IMediaPlayer.OnSeekCompleteListener() {

        @Override
        public void onSeekComplete(IMediaPlayer mp) {
            mSeekEndTime = System.currentTimeMillis();
        }
    };

    /**
     * Register a callback to be invoked when the media file
     * is loaded and ready to go.
     *
     * @param l The callback that will be run
     */
    public void setOnPreparedListener(IMediaPlayer.OnPreparedListener l) {
        mOnPreparedListener = l;
    }

    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     *
     * @param l The callback that will be run
     */
    public void setOnCompletionListener(IMediaPlayer.OnCompletionListener l) {
        mOnCompletionListener = l;
    }

    /**
     * Register a callback to be invoked when an error occurs
     * during playback or setup.  If no listener is specified,
     * or if the listener returned false, VideoView will inform
     * the user of any errors.
     *
     * @param l The callback that will be run
     */
    public void setOnErrorListener(IMediaPlayer.OnErrorListener l) {
        mOnErrorListener = l;
    }

    /**
     * Register a callback to be invoked when an informational event
     * occurs during playback or setup.
     *
     * @param l The callback that will be run
     */
    public void setOnInfoListener(IMediaPlayer.OnInfoListener l) {
        mOnInfoListener = l;
    }


    public void setOnVideoSizeListener(IMediaPlayer.OnVideoSizeChangedListener l) {
        mOnVideoSizeChangedListener = l;
    }

    /**
     * @param l
     */
    public void setOnNativeInvokeListener(IjkMediaPlayer.OnNativeInvokeListener l) {
        mOnNativeInvokeListener = l;
    }

    // REMOVED: mSHCallback
    private void bindSurfaceHolder(IMediaPlayer mp, IRenderView.ISurfaceHolder holder) {
        if (mp == null)
            return;

        if (holder == null) {
            mp.setSurface(null);
            return;
        }

        holder.bindToMediaPlayer(mp);
    }


    IRenderView.IRenderCallback mSHCallback = new IRenderView.IRenderCallback() {
        @Override
        public void onSurfaceChanged(@NonNull IRenderView.ISurfaceHolder holder, int format, int w, int h) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceChanged: unmatched render callback\n");
                return;
            }
            Log.i(TAG, "onSurfaceChanged");
            mSurfaceWidth = w;
            mSurfaceHeight = h;
            boolean isValidState = (mTargetState == STATE_PLAYING);
            boolean hasValidSize = !mRenderView.shouldWaitForResize() || (mVideoWidth == w && mVideoHeight == h);
            if (mMediaPlayer != null && isValidState && hasValidSize) {
                if (mSeekWhenPrepared != 0) {
                    seekTo(mSeekWhenPrepared);
                }
                start();
            }
        }

        @Override
        public void onSurfaceCreated(@NonNull IRenderView.ISurfaceHolder holder, int width, int height) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceCreated: unmatched render callback\n");
                return;
            }
            Log.i(TAG, "onSurfaceCreated");
            mSurfaceHolder = holder;
            if (mMediaPlayer != null) {
                bindSurfaceHolder(mMediaPlayer, holder);
            } else {
                // onSurfaceCreated在调用过start之后触发
                if (mTargetState == STATE_PLAYING) {
                    openVideo();
                    if (mMediaPlayer != null) {
                        bindSurfaceHolder(mMediaPlayer, holder);
                        mCurrentState = STATE_PLAYING;
                        start();
                    }
                }
            }

            Bitmap lastFrame = mRenderView.getLastFrame();
            if (lastFrame != null) {
                mImageView.setImageBitmap(lastFrame);
                mImageView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onSurfaceDestroyed(@NonNull IRenderView.ISurfaceHolder holder) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceDestroyed: unmatched render callback\n");
                return;
            }
            Log.i(TAG, "onSurfaceDestroyed");
            // after we return from this we can't use the surface any more
            mSurfaceHolder = null;
            if (mMediaController != null) mMediaController.hide();
            releaseWithoutStop();
        }
    };

    public void releaseWithoutStop() {
        if (mMediaPlayer != null){
            mMediaPlayer.setSurface(null);
        }
    }

    /*
     * release the media player in any state
     */
    public void release(boolean cleartargetstate) {

        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            if (cleartargetstate) {
                mTargetState = STATE_IDLE;
            }
            AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isInPlaybackState() && mMediaController != null) {
            toggleMediaControlsVisiblity();
        }
        return false;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        if (isInPlaybackState() && mMediaController != null) {
            toggleMediaControlsVisiblity();
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK &&
                keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                keyCode != KeyEvent.KEYCODE_VOLUME_MUTE &&
                keyCode != KeyEvent.KEYCODE_MENU &&
                keyCode != KeyEvent.KEYCODE_CALL &&
                keyCode != KeyEvent.KEYCODE_ENDCALL;
        if (isInPlaybackState() && isKeyCodeSupported && mMediaController != null) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (mMediaPlayer.isPlaying()) {
                    pause();
                    mMediaController.show();
                } else {
                    start();
                    mMediaController.hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                if (!mMediaPlayer.isPlaying()) {
                    start();
                    mMediaController.hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                if (mMediaPlayer.isPlaying()) {
                    pause();
                    mMediaController.show();
                }
                return true;
            } else {
                toggleMediaControlsVisiblity();
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void toggleMediaControlsVisiblity() {
        if (mMediaController.isShowing()) {
            mMediaController.hide();
        } else {
            mMediaController.show();
        }
    }

    @Override
    public void start() {
        if (isInPlaybackState()) {
            dismissLastFrame();
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
            mRenderView.getView().setBackgroundDrawable(null);
        } else {
            Log.i(TAG, "start isInPlaybackState false mMediaPlayer == null:" + (mMediaPlayer == null) + " mCurrentState" + mCurrentState);
        }
        mTargetState = STATE_PLAYING;
    }

    public void fakePause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stayAwake(false);
            }
        }
    }

    public void fakeResume() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stayAwake(true);
            }
        }
    }

    @Override
    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
        mTargetState = STATE_PAUSED;
    }

    public void suspend() {
        release(false);
    }

    public void resume() {
        openVideo();
    }

    @Override
    public int getDuration() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getDuration();
        }

        return -1;
    }

    @Override
    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            if (mCurrentState == STATE_PLAYBACK_COMPLETED) {
                return (int) mMediaPlayer.getDuration();
            }
            return (int) mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public void seekTo(int msec) {
        if (isInPlaybackState()) {
            mSeekStartTime = System.currentTimeMillis();
            mMediaPlayer.seekTo(msec);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = msec;
        }
    }

    @Override
    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    @Override
    public boolean canPause() {
        return mCanPause;
    }

    @Override
    public boolean canSeekBackward() {
        return mCanSeekBack;
    }

    @Override
    public boolean canSeekForward() {
        return mCanSeekForward;
    }

    @Override
    public int getAudioSessionId() {
        return mMediaPlayer.getAudioSessionId();
    }

    private static final int[] s_allAspectRatio = {
            IRenderView.AR_ASPECT_FIT_PARENT,
            IRenderView.AR_ASPECT_FILL_PARENT,
            IRenderView.AR_ASPECT_WRAP_CONTENT,
            IRenderView.AR_MATCH_PARENT,
            IRenderView.AR_16_9_FIT_PARENT,
            IRenderView.AR_4_3_FIT_PARENT};
    private int mCurrentAspectRatioIndex = 0;
    private int mCurrentAspectRatio = s_allAspectRatio[0];

    public int toggleAspectRatio() {
        mCurrentAspectRatioIndex++;
        mCurrentAspectRatioIndex %= s_allAspectRatio.length;

        mCurrentAspectRatio = s_allAspectRatio[mCurrentAspectRatioIndex];
        if (mRenderView != null)
            mRenderView.setAspectRatio(mCurrentAspectRatio);
        return mCurrentAspectRatio;
    }

    public void setAspectRatio(int aspectRatio) {
        mCurrentAspectRatio = aspectRatio;
        if (mRenderView != null)
            mRenderView.setAspectRatio(mCurrentAspectRatio);
    }

    public ITrackInfo[] getTrackInfo() {
        if (mMediaPlayer == null)
            return null;

        return mMediaPlayer.getTrackInfo();
    }

    public void selectTrack(int stream) {
        MediaPlayerCompat.selectTrack(mMediaPlayer, stream);
    }

    public void deselectTrack(int stream) {
        MediaPlayerCompat.deselectTrack(mMediaPlayer, stream);
    }

    public int getSelectedTrack(int trackType) {
        return MediaPlayerCompat.getSelectedTrack(mMediaPlayer, trackType);
    }

    public void fullScreen(Activity activity) {
        if (!isFullState) {
            if (activity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
            activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mRawParams = getLayoutParams();
            ViewGroup.LayoutParams fullParams;
            if (mRawParams instanceof RelativeLayout.LayoutParams) {
                fullParams = new RelativeLayout.LayoutParams(metrics.widthPixels, metrics.heightPixels);
            } else if (mRawParams instanceof LinearLayout.LayoutParams) {
                fullParams = new LinearLayout.LayoutParams(metrics.widthPixels, metrics.heightPixels);
            } else if (mRawParams instanceof LayoutParams) {
                fullParams = new LayoutParams(metrics.widthPixels, metrics.heightPixels);
            } else {
                new AlertDialog.Builder(getContext())
                        .setMessage("nonsupport parent layout, please do it by yourself")
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                    }
                                })
                        .setCancelable(false)
                        .show();
                return;
            }
            setLayoutParams(fullParams);
            isFullState = true;
        }
    }

    public void exitFullScreen(Activity activity) {

        if (isFullState) {
            if (activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            setLayoutParams(mRawParams);
            isFullState = false;
        }
    }


    // TODO: 16/5/20 设置播放前至少缓存时间
    private void setCacheDuration(long cacheDuration) {
        this.cacheDuration = cacheDuration;
    }

    public long cacheDuration;

    /**
     * 设置默认背景图片
     *
     * @param rec
     */
    public void setImage(int rec) {
        if (mRenderView != null) {
            mRenderView.getView().setBackgroundResource(rec);
        }
    }

    public void setImage(Drawable background) {
        if (mRenderView != null) {
            mRenderView.getView().setBackgroundDrawable(background);
        }
    }

    /**
     * 设置缓冲区大小 单位(B) 默认 15MB
     *
     * @param size
     */
    public void setBufferSize(int size) {
        bufferSize = size;
    }

    /**
     * 设置是否自动开始播放 默认false
     *
     * @param autoPlay
     */
    public void setAutoStart(boolean autoPlay) {
        isAutoPlay = autoPlay;
    }


    /**
     * 设置播放速度
     */
    public void setSpeed(float speed) {
        playSpeed = speed;
        if (mMediaPlayer != null) {
            mMediaPlayer.setSpeed(speed);
        }
    }

    public float getSpeed(float speed) {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getSpeed(.0f);
        }
        return .0f;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.start) {
            if(mStartPlayView != null) {
                mStartPlayView.setVisibility(GONE);
                if(mUri != null) {
                    setVideoURI(mUri);
                    start();
                }
            }

        }
    }

    private void speedPlay(float speed, long timeout) {
        Log.e(TAG, "reportError");
        mMediaPlayer.setSpeed(speed);
        postDelayed(setNormalSpeed, timeout);
    }

    private Runnable setNormalSpeed = new Runnable() {
        @Override
        public void run() {
            mMediaPlayer.setSpeed(1.04f);
        }
    };

    private void reportError() {
        Log.e(TAG, "reportError");
        postDelayed(reconnection, BUFFER_TIMEOUT);
    }

    private void cancelReport() {
        Log.e(TAG, "cancelReport");
        removeCallbacks(reconnection);
    }

    private Runnable reconnection = new Runnable() {
        @Override
        public void run() {
            release(true);
            mErrorListener.onError(mMediaPlayer, ERROR_BUFFER_TIMEOUT, 0);
        }
    };

    private void showBufferProgress() {
        postDelayed(showBufferProgress, mBufferTotalTime);
    }

    private void cancelShowBufferProgress() {
        removeCallbacks(showBufferProgress);
    }

    private void hideBufferProgress() {
        removeCallbacks(showBufferProgress);
        if(mBufferProgress != null) {
            mBufferProgress.hideBufferProgress();
        }
    }

    private Runnable showBufferProgress = new Runnable() {
        @Override
        public void run() {
            if(mBufferProgress != null) {
                mBufferProgress.showBufferProgress();
            }
        }
    };
}
