/** Clasa pentru controlul ecranului JavaFX de login/register si verificare email;
 * gestioneaza tranzitia catre fereastra principala
 * @author Mirica Alin-Marian
 * @version 2 Decembrie 2025
 */

package com.proiect.spotifyclone.controller;

import com.proiect.spotifyclone.dto.AuthRequest;
import com.proiect.spotifyclone.dto.LoginResponse;
import com.proiect.spotifyclone.dto.RegisterConfirmRequest;
import com.proiect.spotifyclone.util.AppSession;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;
import static java.util.Objects.requireNonNull;

@Component
public class LoginController {
    @Autowired
    private ApplicationContext springContext;

    private final RestClient api = RestClient.create("http://localhost:8080");
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private TextField codeField;
    @FXML private Button confirmCodeButton;
    @FXML private Button loginButton;

    // inregistrare user
    @FXML
    public void onRegisterButtonClick() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        statusLabel.setText("Sending verification code...");
        statusLabel.setStyle("-fx-text-fill: #b3b3b3;");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                String payload = mapper.writeValueAsString(new AuthRequest(email, password));
                return api.post()
                        .uri("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(String.class);
            }
        };

        task.setOnSucceeded(e -> {
            statusLabel.setText("The code has been sent. Check your inbox!");
            statusLabel.setStyle("-fx-text-fill: #1DB954;");
            enableVerificationMode();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = (ex instanceof RestClientResponseException r) ? r.getResponseBodyAsString() : ex.getMessage();
            statusLabel.setText(msg);
            statusLabel.setStyle("-fx-text-fill: #ff5555;");
            ex.printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // confirmare cod register
    @FXML
    public void onConfirmCodeClick() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String code = codeField.getText().trim();

        statusLabel.setText("Checking...");
        statusLabel.setStyle("-fx-text-fill: #b3b3b3;");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                String payload = mapper.writeValueAsString(new RegisterConfirmRequest(email, password, code));
                return api.post()
                        .uri("/api/auth/register/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(String.class);
            }
        };

        task.setOnSucceeded(e -> {
            statusLabel.setText("Registered successfully!");
            statusLabel.setStyle("-fx-text-fill: #1DB954; -fx-font-weight: bold;");
            resetToLoginMode();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = (ex instanceof RestClientResponseException r) ? r.getResponseBodyAsString() : ex.getMessage();
            statusLabel.setText(msg);
            statusLabel.setStyle("-fx-text-fill: #ff5555;");
            ex.printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // implementare verificare la register
    private void enableVerificationMode() {
        codeField.setVisible(true);
        codeField.setManaged(true);
        confirmCodeButton.setVisible(true);
        confirmCodeButton.setManaged(true);
    }

    // reset la login
    private void resetToLoginMode() {
        codeField.setVisible(false);
        codeField.setManaged(false);
        confirmCodeButton.setVisible(false);
        confirmCodeButton.setManaged(false);
        passwordField.clear();
        codeField.clear();
    }

    // login user
    @FXML
    public void onLoginButtonClick() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        statusLabel.setText("Logging In...");
        statusLabel.setStyle("-fx-text-fill: #b3b3b3;");

        Task<LoginResponse> task = new Task<LoginResponse>() {
            @Override
            protected LoginResponse call() throws Exception {
                String payload = mapper.writeValueAsString(new AuthRequest(email, password));
                String json = api.post()
                        .uri("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(String.class);

                return mapper.readValue(json, LoginResponse.class);
            }
        };

        task.setOnSucceeded(event -> {
            LoginResponse loginResponse = task.getValue();

            statusLabel.setText("Logged in! Loading...");
            statusLabel.setStyle("-fx-text-fill: #1DB954; -fx-font-weight: bold;");

            AppSession.userId = loginResponse.userId();
            AppSession.email = loginResponse.email();

            startMainApp();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex instanceof RestClientResponseException r) {
                if (r.getStatusCode().value() == 401) {
                    statusLabel.setText("Wrong email or password!");
                } else {
                    statusLabel.setText("Error: " + r.getResponseBodyAsString());
                }
            } else {
                statusLabel.setText("Login error: " + ex.getMessage());
            }
            statusLabel.setStyle("-fx-text-fill: #ff5555;");
            ex.printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // exit
    @FXML
    public void onCloseButtonClick() { System.exit(0); }

    // login ok -> deschide aplicatia propriu zisa
    private void startMainApp() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
            fxmlLoader.setControllerFactory(springContext::getBean);
            Parent root = fxmlLoader.load();

            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setTitle("Cloneify");
            stage.setResizable(false);

            Scene  scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/style/app.css").toExternalForm());
            scene.getStylesheets().add(requireNonNull(getClass().getResource("/style/dark-theme.css")).toExternalForm());
            scene.setFill(Color.TRANSPARENT);

            stage.setScene(scene);
            stage.show();


            Stage loginStage = (Stage) emailField.getScene().getWindow();
            loginStage.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}