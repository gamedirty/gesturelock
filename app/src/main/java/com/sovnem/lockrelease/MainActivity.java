package com.sovnem.lockrelease;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

	private static final int VERIFY = 1110;
	private String pwd;
	private TextView pwdView;
	private Button setButton, openButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		pwd = SecretPrefUtil.getSecretString(this);
		initView();
	}

	private void initView() {
		pwdView = (TextView) findViewById(R.id.textView_pwd);
		pwdView.setText(pwd);
		setButton = (Button) findViewById(R.id.button_set);
		openButton = (Button) findViewById(R.id.button_open);
		setButton.setOnClickListener(this);
		openButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_open:
			open();
			break;
		case R.id.button_set:
			set();
			break;
		}
	}

	private void set() {
		Intent intent = new Intent(this, SetsecretActivity.class);
		startActivityForResult(intent, SET);
	}

	private void open() {
		if ("".equals(pwd)) {
			Toast.makeText(this, "请先设置密码", Toast.LENGTH_LONG).show();
			set();
		} else {
			Intent intent = new Intent(this, VerifyActivity.class);
			startActivityForResult(intent, VERIFY);
		}
	}

	public static final int SET = 1109;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SET && resultCode == RESULT_OK) {
			if (data.getBooleanExtra("set", false)) {
				pwd = SecretPrefUtil.getSecretString(this);
				pwdView.setText(pwd);
			}
		} else if (requestCode == VERIFY && resultCode == RESULT_OK) {
			if (data.getBooleanExtra("verify", false)) {
				jump();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void jump() {
		Intent intent = new Intent(this, OtherActivity.class);
		startActivity(intent);
	}
}
