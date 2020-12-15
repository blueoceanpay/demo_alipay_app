package com.bopay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


import com.alipay.sdk.app.PayTask;

import com.bopay.api.RetrofitFactory;
import com.bopay.bean.PayReqBean;
import com.bopay.bean.PayRspBean;
import com.bopay.utils.SignInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText et_pay_sum;
    private Button bt_alipay, bt_value;
    private static final String AppKey = "";//商户Key
    private static final String AppId = "";//商户id
    private static final String TAG = "MainActivity";
    private static final String Payment = "alipay.app";//支付宝app支付
    private static final String Wallet = "CN";//钱包 CN/HK
    private static final int SDK_PAY_FLAG = 1;
    private String orderInfo;//发起支付宝支付参数

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        et_pay_sum = findViewById(R.id.et_pay_sum);
        bt_alipay = findViewById(R.id.bt_alipay);
        bt_value = findViewById(R.id.bt_value);
        bt_alipay.setOnClickListener(this);
        bt_value.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_value:
                String money = et_pay_sum.getText().toString();
                getPayInfo(money);
                break;
            case R.id.bt_alipay:
                aliPay(orderInfo);
                break;
        }
    }

    /**
     * 请求支付信息
     *
     * @param money
     */
    private void getPayInfo(String money) {
        final PayReqBean payReqBean = new PayReqBean();
        payReqBean.setAppid(AppId);//商户id
        payReqBean.setPayment(Payment);//支付宝app支付
        payReqBean.setTotal_fee(money);//支付金额 单位分
        payReqBean.setWallet(Wallet);//钱包 CN/HK
        payReqBean.setBody("测试商品");
//        payReqBean.setStore_id("");  //选填
        payReqBean.setSign(SignInfo.createSign(SignInfo.getParametersMap(payReqBean), AppKey));
        Gson gson = new Gson();
        String route = gson.toJson(payReqBean);//通过Gson将Bean转化为Json字符串形式
        Log.d(TAG, "请求报文" + route);
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), route);
        RetrofitFactory.getInstence().getAPiService().pay(body)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<PayRspBean>() {

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        showAlert(MainActivity.this, "接口请求失败" + e.toString());
                    }

                    @Override
                    public void onNext(final PayRspBean payRspBean) {
                        orderInfo = payRspBean.getData().getApp_format();
                        showAlert(MainActivity.this, "获取支付宝支付参数" + payRspBean.getData().getApp_format());//接口返回未处理格式的支付参数
                    }
                });
    }

    /**
     * 支付宝支付
     *
     * @param orderInfo
     */
    private void aliPay(final String orderInfo) {
        final Runnable payRunnable = new Runnable() {
            @Override
            public void run() {
                // 构造PayTask 对象
                PayTask alipay = new PayTask(MainActivity.this);
                // 调用支付接口，获取支付结果
//                final Map<String, String> result = alipay.payV2(orderInfo, true);
                Log.d(TAG, "aliPay: " + orderInfo);
                final String result = alipay.pay(orderInfo, true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showAlert(MainActivity.this, "支付结果" + result);
                    }
                });
            }
        };
        // 必须异步调用
        Thread payThread = new Thread(payRunnable);
        payThread.start();
    }

    private static void showAlert(Context ctx, String info) {
        showAlert(ctx, info, null);
    }

    private static void showAlert(Context ctx, String info, DialogInterface.OnDismissListener onDismiss) {
        new AlertDialog.Builder(ctx)
                .setMessage(info)
                .setPositiveButton("确认", null)
                .setOnDismissListener(onDismiss)
                .show();
    }

}
