/** Clasa pentru controlul paginii de playlist;
 *  afiseaza piesele dintr-un playlist (user/discover/ai playlist)
 *  si gestioneaza actiuni (play, delete, sort, add)
 * @author Mirica Alin-Marian
 * @version 27 Decembrie 2025
 */

package com.proiect.spotifyclone.controller;

import com.proiect.spotifyclone.dto.PlaylistDTO;
import com.proiect.spotifyclone.dto.PlaylistTrackDTO;
import com.proiect.spotifyclone.dto.TrackDTO;
import com.proiect.spotifyclone.util.AppSession;
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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Comparator.comparing;

@Component
public class PlaylistPageController {

    @FXML private Label playlistTitle;
    @FXML private VBox tracksContainer;
    @FXML private Button sortTitleBtn;
    @FXML private Button sortArtistBtn;

    // Provider cu playlisturile userului (folosit la “Add to playlist” menu)
    private Supplier<List<PlaylistDTO>> playlistsProvider;

    private enum SortMode { NONE, TITLE, ARTIST }
    private SortMode sortMode = SortMode.NONE;

    private boolean showingDiscover = false;
    private List<PlaylistTrackDTO> currentPlaylistTracks = List.of();
    private List<TrackDTO> currentDiscoverTracks = List.of();

    public void setPlaylistsProvider(java.util.function.Supplier<List<PlaylistDTO>> provider) {
        this.playlistsProvider = provider;
    }

    // Callback “Back” (ca sa revenim in MainController)
    private Runnable onBack;

    // Callback “Play track” (trimite track la MainController)
    private Consumer<PlaylistTrackDTO> onPlayRequested;

    private boolean discoverMode = false;

    private PlaylistDTO playlist;

    private final RestClient apiClient = RestClient.create("http://localhost:8080");
    private final ObjectMapper mapper = new ObjectMapper();

    // Seteaza callback pentru butonul back
    public void setOnBack(Runnable onBack) { this.onBack = onBack; }

    // Seteaza callback pentru play (cand dai click pe un rand)
    public void setOnPlayRequested(Consumer<PlaylistTrackDTO> cb) { this.onPlayRequested = cb; }

    public void setPlaylist(PlaylistDTO p) {
        this.playlist = p;
        this.discoverMode = false;
        playlistTitle.setText(p.name());
        loadTracks();
    }

    @FXML
    public void onBack() {
        if (onBack != null) onBack.run();
    }

    private void loadTracks() {
        tracksContainer.getChildren().setAll(new Label("Loading..."));

        Task<List<PlaylistTrackDTO>> task = new Task<>() {
            @Override
            protected List<PlaylistTrackDTO> call() throws Exception {
                String json = apiClient.get()
                        .uri("/api/playlists/{id}/tracks", playlist.id())
                        .header("X-User-Id", String.valueOf(AppSession.userId))
                        .retrieve()
                        .body(String.class);

                PlaylistTrackDTO[] arr = mapper.readValue(json, PlaylistTrackDTO[].class);
                return arr == null ? List.of() : Arrays.asList(arr);
            }
        };

        task.setOnSucceeded(e -> {
            showingDiscover = false;
            currentPlaylistTracks = task.getValue();
            applySortAndRender();
        });

        task.setOnFailed(e -> task.getException().printStackTrace());

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // afisare tracks
    private void renderTracks(List<PlaylistTrackDTO> tracks) {
        tracksContainer.getChildren().clear();

        for (PlaylistTrackDTO t : tracks) {
            ImageView cover = new ImageView();
            cover.setFitWidth(48);
            cover.setFitHeight(48);
            cover.setPreserveRatio(true);

            Image img;
            if (t.coverUrl() == null || t.coverUrl().isBlank()) {
                img = new Image(getClass().getResourceAsStream("/placeholder.png"));
            } else {
                img = new Image(t.coverUrl(), true);
            }
            cover.setImage(img);

            Label title = new Label(t.title());
            title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

            Label artist = new Label(t.artist());
            artist.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 11;");

            VBox textBox = new VBox(title, artist);
            textBox.setSpacing(2);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button deleteBtn = new Button("🗑");
            deleteBtn.setFocusTraversable(false);
            deleteBtn.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #b3b3b3;
            -fx-font-size: 14;
            -fx-cursor: hand;
        """);

            deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: white;
            -fx-font-size: 14;
            -fx-cursor: hand;
        """));

            deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #b3b3b3;
            -fx-font-size: 14;
            -fx-cursor: hand;
        """));

            deleteBtn.setOnAction(e -> {
                e.consume();
                confirmAndDeleteTrack(t);
            });

            HBox row = new HBox(12, cover, textBox, spacer, deleteBtn);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 8; -fx-cursor: hand; -fx-background-color: transparent;");

            row.setOnMouseClicked(e -> {
                if (e.getButton() != MouseButton.PRIMARY) return;
                if (onPlayRequested != null) onPlayRequested.accept(t);
            });

            row.setOnMouseEntered(e ->
                    row.setStyle("-fx-padding: 8; -fx-background-color: #1a1a1a; -fx-background-radius: 8; -fx-cursor: hand;")
            );
            row.setOnMouseExited(e ->
                    row.setStyle("-fx-padding: 8; -fx-cursor: hand; -fx-background-color: transparent;")
            );

            ContextMenu menu = new ContextMenu();
            MenuItem remove = new MenuItem("Remove from playlist");
            remove.setOnAction(ev -> confirmAndDeleteTrack(t));
            menu.getItems().add(remove);

            row.setOnContextMenuRequested(ev -> {
                menu.show(row, ev.getScreenX(), ev.getScreenY());
                ev.consume();
            });

            tracksContainer.getChildren().add(row);
        }
    }

    private void confirmAndDeleteTrack(PlaylistTrackDTO t) {
        if (discoverMode) return;
        if (playlist == null || t.id() == null) return;

        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Remove track");
        a.setHeaderText(null);
        a.setContentText("Remove \"" + t.title() + "\" from \"" + playlist.name() + "\"?");

        a.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    apiClient.delete()
                            .uri("/api/playlists/{id}/tracks/{trackId}", playlist.id(), t.id())
                            .header("X-User-Id", String.valueOf(AppSession.userId))
                            .retrieve()
                            .toBodilessEntity();
                    return null;
                }
            };

            task.setOnSucceeded(e -> loadTracks());
            task.setOnFailed(e -> task.getException().printStackTrace());

            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
        });
    }

    // reload la playlist la modificari
    public void reloadIfShowing(Long playlistId) {
        if (playlist != null && playlist.id().equals(playlistId)) {
            loadTracks();
        }
    }

    // arata pagina de discover
    public void showDiscover(long deezerPlaylistId, String title) {
        if (playlistTitle != null)
            playlistTitle.setText(title);
        showingDiscover = true;
        loadDiscoverTracks(deezerPlaylistId);
    }

    // incarca piesele din discover (playlist-urile sunt hardcodate)
    private void loadDiscoverTracks(long id) {
        Task<List<TrackDTO>> task = new Task<>() {
            @Override
            protected List<TrackDTO> call() throws Exception {
                String json = apiClient.get()
                        .uri("/api/discover/{id}/tracks?limit=1000", id)
                        .retrieve()
                        .body(String.class);

                TrackDTO[] arr = mapper.readValue(json, TrackDTO[].class);
                return arr == null ? List.of() : Arrays.asList(arr);
            }
        };

        task.setOnSucceeded(e -> {
            currentDiscoverTracks = task.getValue();
            applySortAndRender();
        });

        task.setOnFailed(e -> task.getException().printStackTrace());

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void renderDiscoverTracks(List<TrackDTO> tracks) {
        tracksContainer.getChildren().clear();

        for (TrackDTO t : tracks) {
            ImageView cover = new ImageView();
            cover.setFitWidth(48);
            cover.setFitHeight(48);
            cover.setPreserveRatio(true);

            Image img;
            if (t.getCoverUrl() == null || t.getCoverUrl().isBlank()) {
                img = new Image(getClass().getResourceAsStream("/placeholder.png"));
            } else {
                img = new Image(t.getCoverUrl(), true);
            }
            cover.setImage(img);

            Label title = new Label(t.getTitle());
            title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

            Label artist = new Label(t.getArtist());
            artist.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 11;");

            VBox textBox = new VBox(title, artist);
            textBox.setSpacing(2);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(12, cover, textBox, spacer);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 8; -fx-cursor: hand; -fx-background-color: transparent;");

            row.setOnMouseClicked(e -> {
                if (e.getButton() != MouseButton.PRIMARY) return;
                if (onPlayRequested == null) return;

                onPlayRequested.accept(new PlaylistTrackDTO(
                        null,
                        t.getId(),
                        t.getTitle(),
                        t.getArtist(),
                        t.getCoverUrl(),
                        t.getPreviewUrl(),
                        0
                ));
            });

            ContextMenu menu = new ContextMenu();
            Menu addTo = new Menu("Add to playlist");
            menu.getItems().add(addTo);

            menu.setOnShowing(ev -> {
                addTo.getItems().clear();

                List<PlaylistDTO> pls = (playlistsProvider == null) ? List.of() : playlistsProvider.get();
                for (PlaylistDTO p : pls) {
                    MenuItem mi = new MenuItem(p.name());
                    mi.setOnAction(ae -> addTrackToPlaylist(p.id(), t));
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

            row.setOnMouseEntered(e -> row.setStyle("-fx-padding: 8; -fx-background-color: #1a1a1a; -fx-background-radius: 8; -fx-cursor: hand;"));
            row.setOnMouseExited(e -> row.setStyle("-fx-padding: 8; -fx-cursor: hand; -fx-background-color: transparent;"));

            tracksContainer.getChildren().add(row);
        }
    }

    private void addTrackToPlaylist(long playlistId, TrackDTO t) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                var req = new com.proiect.spotifyclone.dto.AddTrackRequest(
                        t.getId(), t.getTitle(), t.getArtist(), t.getCoverUrl(), t.getPreviewUrl()
                );

                apiClient.post()
                        .uri("/api/playlists/{id}/tracks", playlistId)
                        .header("X-User-Id", String.valueOf(AppSession.userId))
                        .body(req)
                        .retrieve()
                        .toBodilessEntity();

                return null;
            }
        };

        task.setOnFailed(e -> task.getException().printStackTrace());
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    @FXML
    public void onSortTitle() {
        sortMode = SortMode.TITLE;
        applySortAndRender();
    }

    @FXML
    public void onSortArtist() {
        sortMode = SortMode.ARTIST;
        applySortAndRender();
    }

    private void updateSortButtons() {
        if (sortTitleBtn == null || sortArtistBtn == null) return;

        String off = "-fx-background-color: #2a2a2a; -fx-text-fill: white; -fx-background-radius: 16;"
                + " -fx-padding: 6 12; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12;";
        String on  = "-fx-background-color: #1db954; -fx-text-fill: black; -fx-background-radius: 16;"
                + " -fx-padding: 6 12; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12;";

        sortTitleBtn.setStyle(sortMode == SortMode.TITLE ? on : off);
        sortArtistBtn.setStyle(sortMode == SortMode.ARTIST ? on : off);
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    // aplicare sortare
    private void applySortAndRender() {
        updateSortButtons();

        if (showingDiscover) {
            var list = new ArrayList<>(currentDiscoverTracks == null ? List.<TrackDTO>of() : currentDiscoverTracks);

            if (sortMode == SortMode.TITLE) {
                list.sort(comparing(t -> safeLower(t.getTitle())));
            } else if (sortMode == SortMode.ARTIST) {
                list.sort(comparing(t -> safeLower(t.getArtist())));
            }

            renderDiscoverTracks(list);
        } else {
            var list = new ArrayList<>(currentPlaylistTracks == null ? List.<PlaylistTrackDTO>of() : currentPlaylistTracks);

            if (sortMode == SortMode.TITLE) {
                list.sort(comparing(t -> safeLower(t.title())));
            } else if (sortMode == SortMode.ARTIST) {
                list.sort(comparing(t -> safeLower(t.artist())));
            }

            renderTracks(list);
        }
    }
}
