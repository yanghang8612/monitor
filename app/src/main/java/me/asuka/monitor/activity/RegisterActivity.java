package me.asuka.monitor.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;

import me.asuka.monitor.databinding.ActivityRegisterBinding;
import me.asuka.monitor.util.AppSingleton;
import me.asuka.monitor.util.NetUtils;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = RegisterActivity.class.getSimpleName();

    private ActivityRegisterBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;
        final EditText idEditText = binding.id;
        final Button registerConfirmButton = binding.registerConfirm;
        final ProgressBar loadingProgressBar = binding.loading;

        registerConfirmButton.setOnClickListener(v -> {
            if (usernameEditText.getText().toString().length() != 11) {
                usernameEditText.setError("请输入11位正确手机号");
                usernameEditText.requestFocus();
            } else if (passwordEditText.getText().toString().length() < 6) {
                passwordEditText.setError("密码长度至少为6位");
                passwordEditText.requestFocus();
            } else if (idEditText.getText().toString().length() != 18) {
                idEditText.setError("请输18位正确身份证号");
                idEditText.requestFocus();
            } else {
                loadingProgressBar.setVisibility(View.VISIBLE);
                register(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString(),
                        idEditText.getText().toString());
            }
        });
    }

    private void register(String username, String password, String id) {
        AppSingleton.getInstance(this).addToRequestQueue(
                NetUtils.getRegisterParam(username, password, id, response -> {
                    try {
                        if (!response.getBoolean("result")) {
                            showToast(response.getString("message"));
                        } else {
                            binding.loading.setVisibility(View.INVISIBLE);
                            showToast("注册成功！");
                            new Handler().postDelayed(() -> {
                                // Turn to LoginActivity
                                startActivity(new Intent().setClass(
                                        RegisterActivity.this,
                                        LoginActivity.class));
                                finish();
                            }, 1000);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
