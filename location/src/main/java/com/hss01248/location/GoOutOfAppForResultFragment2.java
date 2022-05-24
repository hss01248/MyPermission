package com.hss01248.location;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.hss01248.activityresult.ActivityResultListener;
import com.hss01248.activityresult.StartActivityUtil;
import com.hss01248.transfrag.BaseTransFragment;

import java.util.Random;

/**
 * @Despciption todo
 * @Author hss
 * @Date 25/03/2022 17:26
 * @Version 1.0
 */
public class GoOutOfAppForResultFragment2 extends BaseTransFragment<Intent> {

    int requestCode;
    ActivityResultListener listener;
    boolean startWaitingResult;
    boolean consumed;

    public GoOutOfAppForResultFragment2() {
        super();
    }

    public GoOutOfAppForResultFragment2(FragmentActivity activity, Intent intent) {
        super(activity, intent);
    }

    public void goOutApp(ActivityResultListener listener) {
        requestCode = new Random().nextInt(8799);
        this.listener = listener;
        try {
            if (!listener.onInterceptStartIntent(this, bean, requestCode)) {
                startActivityForResult(bean, requestCode);
            }
        } catch (Throwable throwable) {
            listener.onActivityNotFound(throwable);
            finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        startWaitingResult = true;
    }


    /**
     * 如果真有,那么比onStart()先执行
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (startWaitingResult) {
            consumed = true;
            if (StartActivityUtil.debugable) {
                Log.i("onActivityResult", "req:" + requestCode + ",result:" + resultCode + ",data:" + data);
            }
            onStartOfResultBack(requestCode, resultCode, data);

            //不会在回来时回调,而是一跳出去就回调,resultcode是cancel,所以这个方法不能用
        }
        //consumed = true;
        //listener.onActivityResult(requestCode,resultCode,data);
        //不会在回来时回调,而是一跳出去就回调,resultcode是cancel,所以这个方法不能用

    }

    @Override
    public void onResume() {
        super.onResume();
        if (startWaitingResult && !consumed) {
            onStartOfResultBack(requestCode, 66, null);
            startWaitingResult = false;
        }
    }

    protected void onStartOfResultBack(int requestCode, int resultCode, @Nullable Intent data) {
        listener.onActivityResult(requestCode, resultCode, data);
        if (StartActivityUtil.debugable) {
            Log.i("onActivityResult2", "req:" + requestCode + ",result:onStartOfResultBack,data:null");
        }
        finish();

    }
}
