package com.sovnem.lockrelease;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.sovnem.lockrelease.GestureLockView.GestureLockCallback;
import com.sovnem.lockrelease.GestureLockView.Result;

/**
 * 验证手势密码界面
* @author monkey-d-wood
 */
public class VerifyActivity extends Activity {
	String passwordString;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.verify);
		passwordString = SecretPrefUtil.getSecretString(this);
		initView();
	}

	TextView tvDesc;
	GestureLockView glv;

	private void initView() {
		tvDesc = (TextView) findViewById(R.id.textView_desc);
		glv = (GestureLockView) findViewById(R.id.lock);
		glv.setCallback(new GestureLockCallback() {

			@Override
			public void onFinish(String pwdString, Result result) {
				if (pwdString.equals(passwordString)) {
					Intent data = getIntent();
					data.putExtra("verify", true);
					setResult(RESULT_OK, data);
					result.setRight(true);
					finish();
				} else {
					Toast.makeText(VerifyActivity.this, "密码不正确",
							Toast.LENGTH_LONG).show();
					result.setRight(false);
				}
			}
		});
	}
}
