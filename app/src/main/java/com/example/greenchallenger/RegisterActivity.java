package com.example.greenchallenger;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnRegisterSubmit, btnNaverRegister;
    private View btnGoogleRegister;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtEmail = findViewById(R.id.edtRegisterEmail);
        edtPassword = findViewById(R.id.edtRegisterPassword);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        btnGoogleRegister = findViewById(R.id.btnGoogleRegister);
        btnNaverRegister = findViewById(R.id.btnNaverRegister);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnRegisterSubmit.setOnClickListener(v -> registerUser());
        btnGoogleRegister.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.putExtra("startGoogleLogin", true);
            startActivity(intent);
            finish();
        });
        btnNaverRegister.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.putExtra("startNaverLogin", true);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "이메일과 비밀번호를 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "비밀번호는 6자 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = auth.getCurrentUser();

                    if (firebaseUser == null) {
                        Toast.makeText(this, "회원가입은 되었지만 사용자 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = firebaseUser.getUid();
                    String nickname = email.split("@")[0];

                    User user = new User(
                            uid,
                            email,
                            nickname,
                            0,
                            1,
                            0,
                            0,
                            0,
                            ""
                    );

                    db.collection("users")
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(unused ->
                                    firebaseUser.sendEmailVerification()
                                            .addOnCompleteListener(task -> {
                                                auth.signOut();
                                                Toast.makeText(
                                                        this,
                                                        "회원가입 완료! 이메일 인증 후 로그인해 주세요.",
                                                        Toast.LENGTH_LONG
                                                ).show();
                                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                startActivity(intent);
                                                finish();
                                            })
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "회원 정보 저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .addOnFailureListener(error ->
                        Toast.makeText(this, "회원가입 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
