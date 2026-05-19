package com.example.greenchallenger;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLogin, btnRegister, btnEmailLogin, btnNaverLogin;
    private View btnGoogleLogin;
    private LinearLayout emailLoginForm;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account);
                } catch (ApiException e) {
                    Toast.makeText(this, "Google 로그인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnEmailLogin = findViewById(R.id.btnEmailLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnNaverLogin = findViewById(R.id.btnNaverLogin);
        emailLoginForm = findViewById(R.id.emailLoginForm);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        setupGoogleSignIn();

        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
        btnEmailLogin.setOnClickListener(v -> emailLoginForm.setVisibility(View.VISIBLE));
        btnGoogleLogin.setOnClickListener(v -> startGoogleSignIn());
        btnNaverLogin.setOnClickListener(v -> signInWithNaverDemoAccount());

        if (getIntent().getBooleanExtra("startGoogleLogin", false)) {
            startGoogleSignIn();
        }
        if (getIntent().getBooleanExtra("startNaverLogin", false)) {
            signInWithNaverDemoAccount();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (auth == null) {
            auth = FirebaseAuth.getInstance();
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        if (canEnterMain(currentUser)) {
            moveToMain();
        } else {
            auth.signOut();
            Toast.makeText(this, "이메일 인증 후 로그인해 주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, options);
    }

    private void startGoogleSignIn() {
        googleSignInClient.signOut().addOnCompleteListener(task ->
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent())
        );
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        Toast.makeText(this, "Google 계정 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveSocialUser(user, buildNickname(user), user.getEmail(), user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "", "google");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Firebase Google 로그인 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void signInWithNaverDemoAccount() {
        auth.signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        Toast.makeText(this, "네이버 로그인 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveSocialUser(user, "네이버 사용자", "", "", "naver_demo");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "네이버 로그인 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void saveSocialUser(FirebaseUser firebaseUser, String nickname, String email, String profileImageUrl, String provider) {
        Map<String, Object> user = new HashMap<>();
        user.put("uid", firebaseUser.getUid());
        user.put("email", email != null ? email : "");
        user.put("nickname", nickname != null && !nickname.trim().isEmpty() ? nickname : "그린챌린저");
        user.put("ecoPoints", 0);
        user.put("growthStage", 1);
        user.put("attendanceCount", 0);
        user.put("missionCompletedCount", 0);
        user.put("friendCount", 0);
        user.put("profileImageUrl", profileImageUrl != null ? profileImageUrl : "");
        user.put("loginProvider", provider);

        db.collection("users")
                .document(firebaseUser.getUid())
                .set(user, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, provider.startsWith("naver") ? "네이버 로그인 성공!" : "Google 로그인 성공!", Toast.LENGTH_SHORT).show();
                    moveToMain();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "사용자 정보 저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private String buildNickname(FirebaseUser firebaseUser) {
        if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().trim().isEmpty()) {
            return firebaseUser.getDisplayName();
        }

        String email = firebaseUser.getEmail();
        if (email != null && email.contains("@")) {
            return email.split("@")[0];
        }

        return "그린챌린저";
    }

    private void loginUser() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "이메일과 비밀번호를 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        Toast.makeText(this, "로그인 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    user.reload().addOnCompleteListener(task -> {
                        FirebaseUser reloadedUser = auth.getCurrentUser();
                        if (reloadedUser != null && canEnterMain(reloadedUser)) {
                            Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                            moveToMain();
                        } else {
                            sendVerificationAgain(reloadedUser);
                            auth.signOut();
                            Toast.makeText(this, "이메일 인증이 필요합니다. 인증 메일을 확인해 주세요.", Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "로그인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private boolean canEnterMain(FirebaseUser user) {
        return user.isAnonymous() || user.isEmailVerified() || isSocialAccount(user);
    }

    private boolean isSocialAccount(FirebaseUser user) {
        for (UserInfo profile : user.getProviderData()) {
            String providerId = profile.getProviderId();
            if (!"firebase".equals(providerId) && !"password".equals(providerId)) {
                return true;
            }
        }
        return false;
    }

    private void sendVerificationAgain(FirebaseUser user) {
        if (user != null && !user.isEmailVerified()) {
            user.sendEmailVerification();
        }
    }

    private void moveToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
