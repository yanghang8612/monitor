package me.asuka.monitor.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;

import me.asuka.monitor.databinding.ActivityLoginBinding;
import me.asuka.monitor.util.AppSingleton;
import me.asuka.monitor.util.NetUtils;
import me.asuka.monitor.util.PreferenceUtils;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private ActivityLoginBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;
        final Button loginButton = binding.login;
        final Button registerButton = binding.register;
        final ProgressBar loadingProgressBar = binding.loading;

        loginButton.setOnClickListener(v -> {
            if (usernameEditText.getText().toString().length() != 11) {
                usernameEditText.setError("请输入11位正确手机号");
                usernameEditText.requestFocus();
            } else if (passwordEditText.getText().toString().length() == 0) {
                passwordEditText.setError("密码不能为空");
                passwordEditText.requestFocus();
            } else {
                loadingProgressBar.setVisibility(View.VISIBLE);
                login(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        });

        registerButton.setOnClickListener(v -> {
            // Turn to RegisterActivity
            startActivity(new Intent().setClass(
                    LoginActivity.this,
                    RegisterActivity.class));
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (PreferenceUtils.getInt("user_id", -1) != -1) {
            long expireTime = PreferenceUtils.getLong("timestamp", 0);
            if (expireTime > System.currentTimeMillis()) {
                PreferenceUtils.saveLong("timestamp", System.currentTimeMillis() + 3600_000);
                new Handler().post(() -> {
                    // Turn to MainActivity
                    startActivity(new Intent().setClass(
                            LoginActivity.this,
                            MainActivity.class));
                    finish();
                });
            }
        }
    }

    private void login(String username, String password) {
        AppSingleton.getInstance(this).addToRequestQueue(
                NetUtils.getLoginParam(username, password, response -> {
                    try {
                        if (!response.getBoolean("result")) {
                            showToast(response.getString("message"));
                        } else {
                            PreferenceUtils.saveInt("user_id", response.getJSONObject("data").getInt("id"));
                            PreferenceUtils.saveLong("timestamp", System.currentTimeMillis() + 3600_000);

                            new Handler().post(() -> {
                                // Turn to MainActivity
                                startActivity(new Intent().setClass(
                                        LoginActivity.this,
                                        MainActivity.class));
                                finish();
                            });
                        }
                    } catch (JSONException e) {
                        Log.d(TAG, e.toString());
                        showToast("登录失败：" + e.getMessage());
                    }
                }));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
