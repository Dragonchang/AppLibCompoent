package com.deepblue.owlrtmpplayer.view;

import android.content.Context;
import android.util.AttributeSet;

import com.deepblue.owlrtmpplayer.R;
import com.deepblue.owlrtmpplayer.utils.Wave;


public class BufferProgress extends androidx.appcompat.widget.AppCompatTextView {

    private Wave mWaveDrawable;
    public BufferProgress(Context context) {
        this(context, null, 0);
    }

    public BufferProgress(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BufferProgress(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mWaveDrawable = new Wave();
        mWaveDrawable.setBounds(0, 0, getResources().getDimensionPixelSize(R.dimen.buffer_progress_size),  getResources().getDimensionPixelSize(R.dimen.buffer_progress_size));
        mWaveDrawable.setColor(getResources().getColor(R.color.colorAccent));
        setCompoundDrawables(mWaveDrawable, null, null, null);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void showBufferProgress() {
        setVisibility(VISIBLE);
        if(mWaveDrawable != null) {
            mWaveDrawable.start();
        }
    }

    public void hideBufferProgress() {
        setVisibility(GONE);
        if(mWaveDrawable != null) {
            mWaveDrawable.stop();
        }
    }
}
