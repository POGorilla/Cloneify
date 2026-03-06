/** Clasa pentru controlul paginii AI Playlist;
 * trimite prompt la backend, afiseaza piesele generate
 * si permite salvarea/adaugarea lor in playlisturi
 * @author Mirica Alin-Marian
 * @version 3 Ianuarie 2026
 */

package com.proiect.spotifyclone.controller;

import com.proiect.spotifyclone.dto.AddTrackRequest;
import com.proiect.spotifyclone.dto.PlaylistDTO;
import com.proiect.spotifyclone.dto.TrackDTO;
import com.proiect.spotifyclone.dto.ai.AiPlaylistRequest;
import com.proiect.spotifyclone.dto.ai.AiPlaylistResponse;
import com.proiect.spotifyclone.util.AppSession;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
public class AiPageController {

    @FXML private TextField promptField;
    @FXML private Label playlistNameLabel;
    @FXML private VBox resultsContainer;
    @FXML private Button saveAiPlaylistBtn;
    @FXML private Label statusLabel;
    @FXML private Button generateBtn;

    // Callback play pentru TrackDTO
    private Consumer<TrackDTO> onPlayRequested;

    // Provider lista playlisturi user
    private Supplier<List<PlaylistDTO>> playlistsProvider;

    // Callback add-to-playlist (folosit in context menu pe fiecare track)
    private BiConsumer<Long, TrackDTO> onAddToPlaylist;

    // Callback de refresh playlisturi dupa ce salvam un playlist AI
    private Runnable onPlaylistsChanged;

    // Ultimul playlist generat (il folosim la Save)
    private AiPlaylistResponse lastGenerated;

    private final RestClient api = RestClient.create("http://localhost:8080");
    private final ObjectMapper mapper = new ObjectMapper();

    public void setOnPlayRequested(Consumer<TrackDTO> cb) { this.onPlayRequested = cb; }
    public void setPlaylistsProvider(Supplier<List<PlaylistDTO>> sp) { this.playlistsProvider = sp; }
    public void setOnAddToPlaylist(BiConsumer<Long, TrackDTO> cb) { this.onAddToPlaylist = cb; }
    public void setOnPlaylistsChanged(Runnable r) { this.onPlaylistsChanged = r; }

    @FXML
    public void initialize() {
        saveAiPlaylistBtn.setDisable(true);
        playlistNameLabel.setText("");

        if (statusLabel != null)
            statusLabel.setText("");
    }

    // POST si apoi render rezultate
    @FXML
    public void onGenerate() {
        String prompt = promptField.getText() == null ? "" : promptField.getText().trim();

        if (prompt.isBlank()) {
            setStatus("Type something first.", false);
            lastGenerated = null;
            saveAiPlaylistBtn.setDisable(true);
            playlistNameLabel.setText("");
            resultsContainer.getChildren().clear();
            return;
        }

        setStatus("", false);

        lastGenerated = null;
        saveAiPlaylistBtn.setDisable(true);
        playlistNameLabel.setText("");
        resultsContainer.getChildren().setAll(labelMuted("Generating..."));

        Task<AiPlaylistResponse> task = new Task<>() {
            @Override protected AiPlaylistResponse call() throws Exception {
                AiPlaylistRequest req = new AiPlaylistRequest(prompt);
                String body = mapper.writeValueAsString(req);

                String json = api.post()
                        .uri("/api/ai/playlist?limit=15")
                        .header("Content-Type", "application/json")
                        .body(body)
                        .retrieve()
                        .body(String.class);

                return mapper.readValue(json, AiPlaylistResponse.class);
            }
        };

        task.setOnSucceeded(e -> render(task.getValue()));
        task.setOnFailed(e -> {
            setStatus("AI failed: " + task.getException().getMessage(), true);
            resultsContainer.getChildren().setAll(labelError("AI failed: " + task.getException().getMessage()));
            task.getException().printStackTrace();
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    // afiseaza in UI numele playlistului + lista de track-uri
    private void render(AiPlaylistResponse resp) {
        setStatus("", false);
        resultsContainer.getChildren().clear();

        if (resp == null || resp.tracks() == null || resp.tracks().isEmpty()) {
            lastGenerated = null;
            saveAiPlaylistBtn.setDisable(true);
            playlistNameLabel.setText("");
            resultsContainer.getChildren().add(labelMuted("No results. Try another prompt."));
            return;
        }

        lastGenerated = resp;
        playlistNameLabel.setText(resp.name() == null ? "" : resp.name());

        saveAiPlaylistBtn.setDisable(false);

        for (TrackDTO t : resp.tracks()) {
            if (t == null) continue;
            resultsContainer.getChildren().add(makeRow(t));
        }
    }

    // creeaza playlist nou + adauga track-urile generate
    @FXML
    public void onSaveAiPlaylist() {
        if (lastGenerated == null || lastGenerated.tracks() == null || lastGenerated.tracks().isEmpty()) return;

        TextInputDialog d = new TextInputDialog(
                (lastGenerated.name() == null || lastGenerated.name().isBlank()) ? "AI Playlist" : lastGenerated.name()
        );
        d.setTitle("Save AI playlist");
        d.setHeaderText(null);
        d.setContentText("Playlist name:");
        styleDialog(d, false);

        d.showAndWait().ifPresent(nameInput -> {
            String playlistName = nameInput == null ? "" : nameInput.trim();
            if (playlistName.isBlank()) {
                setStatus("Playlist name cannot be empty.", true);
                return;
            }

            saveAiPlaylistBtn.setDisable(true);
            setStatus("Saving playlist...", false);

            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    long newPlaylistId = createPlaylist(playlistName);
                    for (TrackDTO t : lastGenerated.tracks()) {
                        if (t == null) continue;
                        addTrackToPlaylist(newPlaylistId, t);
                    }
                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                saveAiPlaylistBtn.setDisable(false);

                if (onPlaylistsChanged != null) onPlaylistsChanged.run();

                flashStatus("Saved as playlist.", false, 2.0);
            });

            task.setOnFailed(e -> {
                saveAiPlaylistBtn.setDisable(false);
                setStatus("Save failed: " + task.getException().getMessage(), true);
                task.getException().printStackTrace();
            });

            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
        });
    }

    // creeaza playlist nou in backend si intoarce id-ul
    private long createPlaylist(String name) throws Exception {
        String json = api.post()
                .uri("/api/playlists?name={n}", name)
                .header("X-User-Id", String.valueOf(AppSession.userId))
                .retrieve()
                .body(String.class);

        JsonNode node = mapper.readTree(json);
        if (node == null || node.get("id") == null) {
            throw new IllegalStateException("Create playlist returned no id");
        }
        return node.get("id").asLong();
    }

    private void addTrackToPlaylist(long playlistId, TrackDTO t) throws Exception {
        AddTrackRequest req = new AddTrackRequest(
                t.getId(),
                t.getTitle(),
                t.getArtist(),
                t.getCoverUrl(),
                t.getPreviewUrl()
        );

        String body = mapper.writeValueAsString(req);

        api.post()
                .uri("/api/playlists/{id}/tracks", playlistId)
                .header("X-User-Id", String.valueOf(AppSession.userId))
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    // construieste rand UI pentru un track AI (click play + context menu add)
    private HBox makeRow(TrackDTO t) {
        ImageView cover = new ImageView();
        cover.setFitWidth(48);
        cover.setFitHeight(48);
        cover.setPreserveRatio(true);

        Image img = (t.getCoverUrl() == null || t.getCoverUrl().isBlank())
                ? new Image(Objects.requireNonNull(getClass().getResourceAsStream("/placeholder.png")))
                : new Image(t.getCoverUrl(), true);
        cover.setImage(img);

        Label title = new Label(t.getTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Label artist = new Label(t.getArtist());
        artist.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 11;");

        VBox text = new VBox(title, artist);
        text.setSpacing(2);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, cover, text, spacer);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 8; -fx-cursor: hand;");

        row.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            if (onPlayRequested != null) onPlayRequested.accept(t);
        });

        row.setOnMouseEntered(e ->
                row.setStyle("-fx-padding: 8; -fx-background-color: #1a1a1a; -fx-background-radius: 8; -fx-cursor: hand;")
        );
        row.setOnMouseExited(e ->
                row.setStyle("-fx-padding: 8; -fx-cursor: hand;")
        );

        ContextMenu menu = new ContextMenu();
        Menu addTo = new Menu("Add to playlist");
        menu.getItems().add(addTo);

        menu.setOnShowing(ev -> {
            addTo.getItems().clear();
            var pls = playlistsProvider == null ? List.<PlaylistDTO>of() : playlistsProvider.get();

            for (PlaylistDTO p : pls) {
                MenuItem mi = new MenuItem(p.name());
                mi.setOnAction(ae -> {
                    if (onAddToPlaylist != null) onAddToPlaylist.accept(p.id(), t);
                });
                addTo.getItems().add(mi);
            }

            if (addTo.getItems().isEmpty()) {
                MenuItem none = new MenuItem("(No playlists)");
                none.setDisable(true);
                addTo.getItems().add(none);
            }
        });

        row.setOnContextMenuRequested(ev -> {
            menu.show(row, ev.getScreenX(), ev.getScreenY());
            ev.consume();
        });

        return row;
    }

    private Label labelMuted(String s) {
        Label l = new Label(s);
        l.setStyle("-fx-text-fill: #b3b3b3; -fx-padding: 8;");
        return l;
    }

    private Label labelError(String s) {
        Label l = new Label(s);
        l.setStyle("-fx-text-fill: #ff5555; -fx-padding: 8;");
        return l;
    }

    private void styleDialog(Dialog<?> d, boolean dangerOk) {
        DialogPane dp = d.getDialogPane();

        dp.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/style/dark-theme.css")).toExternalForm()
        );
        dp.getStyleClass().add("dark-dialog");

        d.setGraphic(null);
        d.setHeaderText(null);

        Button ok = (Button) dp.lookupButton(ButtonType.OK);
        if (ok != null) ok.getStyleClass().add(dangerOk ? "btn-danger" : "btn-primary");

        Button cancel = (Button) dp.lookupButton(ButtonType.CANCEL);
        if (cancel != null) cancel.getStyleClass().add("btn-secondary");
    }

    // Seteaza textul de status (si eventual culoare)
    private void setStatus(String text, boolean error) {
        if (statusLabel == null) return;

        String t = (text == null) ? "" : text.trim();
        if (t.isBlank()) {
            statusLabel.setText("");
            statusLabel.setVisible(false);
            statusLabel.setManaged(false);
            return;
        }

        statusLabel.setText(t);
        statusLabel.setStyle(error
                ? "-fx-text-fill: #ff5555; -fx-font-size: 12; -fx-padding: 0 0 0 8;"
                : "-fx-text-fill: #b3b3b3; -fx-font-size: 12; -fx-padding: 0 0 0 8;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    // Afiseaza un status temporar si apoi il sterge
    private void flashStatus(String text, boolean error, double seconds) {
        setStatus(text, error);
        PauseTransition pt = new PauseTransition(Duration.seconds(seconds));
        pt.setOnFinished(e -> setStatus("", false));
        pt.play();
    }
}