package com.sovnem.lockrelease;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.sovnem.lockrelease.GestureLockView.GestureLockCallback;
import com.sovnem.lockrelease.GestureLockView.Result;

/**
 * 设置手势密码界面
* @author monkey-d-wood
 */
public class SetsecretActivity extends Activity {
	String recordString;
	private String first = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setsecret);
		initView();
	}

	TextView tvDesc;
	GestureLockView glv;
	boolean hasSetFirst;

	private void initView() {
		tvDesc = (TextView) findViewById(R.id.textView_desc);
		glv = (GestureLockView) findViewById(R.id.lock);
		glv.setCallback(new GestureLockCallback() {

			@Override
			public void onFinish(String pwdString, Result result) {
				/*
				 * 手势设置
				 * 如果是第一次设置 记录密码并设置标记为true
				 * 
				 * 如果不是第一次设置 与记录的密码对比 如果正确 提示设置完成保存这个密码
				 * 如果不正确 提示两次设置不同  并把标记设置为false
				 */

				if (!hasSetFirst) {
					first = pwdString;
					hasSetFirst = true;
					result.setRight(true);
				} else {
					if (pwdString.equals(first)) {
						show("设置成功");
						SecretPrefUtil
								.saveSecret(first, SetsecretActivity.this);
						Intent data = getIntent();
						data.putExtra("set", true);
						setResult(RESULT_OK, data);
						finish();
					} else {
						show("两次设置的密码不一致");
						result.setRight(false);
						hasSetFirst = false;
					}

				}
			}
		});
	}

	void show(String msg) {
		Toast.makeText(SetsecretActivity.this, msg + "", Toast.LENGTH_LONG)
				.show();
	}
}
