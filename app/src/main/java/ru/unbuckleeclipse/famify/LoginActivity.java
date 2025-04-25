package ru.unbuckleeclipse.famify;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "GoogleSignIn";
    private static final int RC_SIGN_IN = 9001;
    private static final String PREFS_NAME = "FamifyPrefs";
    private static final String PREF_SIGNED_IN = "isSignedIn";

    private GoogleSignInClient mGoogleSignInClient;
    private SharedPreferences sharedPreferences;
    private SignInButton signInButton; // Изменённый тип для кнопки входа

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Проверяем, вошел ли пользователь ранее
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isSignedIn = sharedPreferences.getBoolean(PREF_SIGNED_IN, false);

        if (isSignedIn) {
            // Если пользователь уже вошел, переходим в MainActivity
            startMainActivity();
            return;
        }

        // Настройка логотипа Famify
        TextView logoTextView = findViewById(R.id.logoTextView);
        logoTextView.setText("FAMIFY");

        // Настройка кнопки входа через Google
        signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD); // Установка размера кнопки
        signInButton.setOnClickListener(v -> signIn());

        // Конфигурация входа через Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Сохраняем статус входа
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(PREF_SIGNED_IN, true);
            editor.apply();

            // Переходим в MainActivity
            startMainActivity();

        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(this, "Ошибка входа", Toast.LENGTH_SHORT).show();
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Закрываем LoginActivity чтобы нельзя было вернуться назад
    }
}