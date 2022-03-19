package com.example.boardgame_ghost.common;

import android.os.CountDownTimer;
import android.widget.TextView;

public class GameTimer extends CountDownTimer {
    TextView textView;

    public GameTimer(long millisInFuture, long countDownInterval, TextView textView){
        super(millisInFuture, countDownInterval);
        this.textView = textView;
    }

    @Override
    public void onTick(long millisUntilFinished) {
        textView.setText("남은시간: " + Long.toString(millisUntilFinished/1000));
    }

    @Override
    public void onFinish() {
        textView.setText("남은시간: 00");
    }
}
