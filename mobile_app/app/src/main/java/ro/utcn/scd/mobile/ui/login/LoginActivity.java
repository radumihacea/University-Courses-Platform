package ro.utcn.scd.mobile.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import ro.utcn.scd.mobile.MainActivity;
import ro.utcn.scd.mobile.R;
import ro.utcn.scd.mobile.data.ApiClient;
import ro.utcn.scd.mobile.data.Session;
import ro.utcn.scd.mobile.data.model.AuthResponse;
import ro.utcn.scd.mobile.ui.common.InsetsHelper;

/**
 * Ecranul de autentificare. Aplicatia mobila este pentru studenti si profesori;
 * contul de admin se foloseste in panoul web.
 */
public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_EXPIRED = "expired";

    private Session session;
    private TextInputEditText email;
    private TextInputEditText password;
    private TextInputEditText server;
    private TextInputLayout serverLayout;
    private TextView error;
    private MaterialButton loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = Session.get(this);

        // Deja autentificat: direct la ecranul principal
        if (session.isLoggedIn()) {
            openMain();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        InsetsHelper.padForSystemBarsAndKeyboard(findViewById(R.id.login_root));

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        server = findViewById(R.id.server);
        serverLayout = findViewById(R.id.server_layout);
        error = findViewById(R.id.error);
        loginButton = findViewById(R.id.login_button);

        server.setText(session.serverUrl());
        String lastEmail = session.email();
        if (!lastEmail.isEmpty()) {
            email.setText(lastEmail);
        }

        findViewById(R.id.server_toggle).setOnClickListener(v ->
                serverLayout.setVisibility(serverLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        loginButton.setOnClickListener(v -> login());
        password.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login();
                return true;
            }
            return false;
        });

        if (getIntent().getBooleanExtra(EXTRA_EXPIRED, false)) {
            showError("Sesiunea a expirat. Autentifică-te din nou.");
        }
    }

    private void login() {
        String emailText = text(email);
        String passwordText = text(password);
        if (emailText.isEmpty() || passwordText.isEmpty()) {
            showError("Completează emailul și parola.");
            return;
        }
        String serverText = text(server);
        if (!serverText.isEmpty()) {
            session.setServerUrl(serverText);
        }

        setLoading(true);
        ApiClient.get(this).login(emailText, passwordText, new ApiClient.Callback<AuthResponse>() {
            @Override
            public void onSuccess(AuthResponse result) {
                setLoading(false);
                if ("ADMIN".equals(result.role)) {
                    showError("Contul de administrator se folosește în panoul web. "
                            + "Aplicația mobilă este pentru studenți și profesori.");
                    return;
                }
                session.save(result);
                openMain();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                showError(message);
            }
        });
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        loginButton.setText(loading ? "Se conectează..." : "Autentificare");
        if (loading) {
            error.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        error.setText(message);
        error.setVisibility(View.VISIBLE);
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private static String text(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
