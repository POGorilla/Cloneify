/** Clasa pentru controlul ferestrei principale;
 * navigare intre pagini, management playlisturi, incarcare discover,
 * player audio si interactiune cu API-ul backend
 * @author Mirica Alin-Marian
 * @version 2 Decembrie 2025
 */

package com.proiect.spotifyclone.controller;

import com.proiect.spotifyclone.dto.*;
import com.proiect.spotifyclone.util.AppSession;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.ListCell;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
public class MainController {
    @FXML private BorderPane mainContainer;
    @FXML private StackPane pageContainer;
    @FXML private ListView<PlaylistDTO> playlistsList;
    @FXML private Label currentSongLabel;
    @FXML private Label currentAuthorLabel;
    @FXML private Button playPauseButton;
    @FXML private StackPane nowPlayingCoverContainer;
    @FXML private Slider progressSlider;
    @FXML private ProgressBar progressFill;
    @FXML private Slider volumeSlider;
    @FXML private VBox sidebar;
    @FXML private Circle avatar;
    @FXML private Button addCurrentToPlaylistBtn;

    @Autowired
    private ApplicationContext springContext;

    private final RestClient apiClient = RestClient.create("http://localhost:8080");
    private final ObjectMapper mapper = new ObjectMapper();

    private MediaPlayer player;

    // daca se reda muzica sau nu
    private boolean isPlaying = false;

    private TrackDTO nowPlayingTrack;

    // Offset_uri pentru drag fereastra (pozitia mouse-ului)
    private double xOffset = 0;
    private double yOffset = 0;

    // Node pentru butonul de "back"
    private Node lastPageNode;

    private Parent discoverRoot;
    private DiscoverPageController discoverCtrl;

    private Parent aiPageRoot;
    private AiPageController aiPageController;

    private Parent searchRoot;
    private SearchPageController searchCtrl;

    private Parent playlistPageRoot;
    private PlaylistPageController playlistPageController;

    // pentru bara de progres
    private ChangeListener<Duration> timeListener;

    // initializare UI
    @FXML
    public void initialize() {
        System.out.println("Main View ok!");

        if (addCurrentToPlaylistBtn != null) {
            addCurrentToPlaylistBtn.setOnMouseEntered(e ->
                    addCurrentToPlaylistBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16; -fx-cursor: hand;")
            );
            addCurrentToPlaylistBtn.setOnMouseExited(e ->
                    addCurrentToPlaylistBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #b3b3b3; -fx-font-size: 16; -fx-cursor: hand;")
            );
        }

        setupWindowDrag();
        setupVolume();
        setupProgressSeek();

        setupPlaylistsListView();
        loadPlaylists();
        showDiscoverPage();

        Platform.runLater(this::loadAvatar);
    }

    // incarca imaginea pt profilul utilizatorului
    @FXML
    public void loadAvatar() {
        if (avatar == null) return;

        avatar.setFill(Color.web("#535353"));

        int size = (int) Math.max(avatar.getRadius() * 2.0, 64);

        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                String json = apiClient.get()
                        .uri("/api/users/me/avatar?size={s}&def=identicon", size)
                        .header("X-User-Id", String.valueOf(AppSession.userId))
                        .retrieve()
                        .body(String.class);

                AvatarDTO dto = mapper.readValue(json, AvatarDTO.class);
                if (dto == null || dto.url() == null || dto.url().isBlank()) return null;

                return new Image(dto.url(), size, size, true, true, false);
            }
        };

        task.setOnSucceeded(e -> {
            Image img = task.getValue();
            if (img == null) return;

            if (img.isError()) {
                System.out.println("Avatar load error: " + img.getException());
                return;
            }

            avatar.setFill(new ImagePattern(img));
        });

        task.setOnFailed(e -> {
            System.out.println("loadAvatar failed: " + task.getException());
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // incarca pagina Home / Discover
    @FXML
    public void onHomeClick() {
        showDiscoverPage();
    }

    // incarca pagina de search
    @FXML
    public void onSearchNavClick() {
        showSearchPage();
    }

    // inlocuieste complet continutul paginii cu node-ul primit
    private void showNode(Node n) {
        if (pageContainer == null) return;
        pageContainer.getChildren().setAll(n);
    }

    // implementare incarcare pagina de home / discover
    private void showDiscoverPage() {
        try {
            if (discoverRoot == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DiscoverPage.fxml"));
                loader.setControllerFactory(springContext::getBean);
                discoverRoot = loader.load();
                discoverCtrl = loader.getController();

                discoverCtrl.setOnOpen(this::openDiscoverPlaylist);
                discoverCtrl.loadDiscover();
            }
            showNode(discoverRoot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // implementare incarcare pagina de search
    private void showSearchPage() {
        try {
            if (searchRoot == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/SearchPage.fxml"));
                loader.setControllerFactory(springContext::getBean);
                searchRoot = loader.load();
                searchCtrl = loader.getController();

                searchCtrl.setOnSearch(this::doSearch);
                searchCtrl.setOnPlay(this::playPreview);
                searchCtrl.setPlaylistsProvider(() -> playlistsList.getItems());
                searchCtrl.setOnAddToPlaylist(this::addTrackToPlaylist);
            }
            showNode(searchRoot);
            searchCtrl.focusSearch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doSearch(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return;

        searchCtrl.showLoading();

        Task<List<TrackDTO>> task = new Task<>() {
            @Override
            protected List<TrackDTO> call() throws Exception {
                String json = apiClient.get()
                        .uri("/api/music/search?query={q}", q)
                        .retrieve()
                        .body(String.class);

                TrackDTO[] arr = mapper.readValue(json, TrackDTO[].class);
                return arr == null ? List.of() : Arrays.asList(arr);
            }
        };

        task.setOnSucceeded(e -> searchCtrl.renderResults(task.getValue()));
        task.setOnFailed(e -> {
            searchCtrl.showError("Search failed: " + task.getException().getMessage());
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void openPlaylistPage(PlaylistDTO p) {
        try {
            if (playlistPageRoot == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/PlaylistPage.fxml"));
                loader.setControllerFactory(springContext::getBean);
                playlistPageRoot = loader.load();
                playlistPageController = loader.getController();

                playlistPageController.setOnPlayRequested(this::playPreviewFromPlaylistTrack);
                playlistPageController.setPlaylistsProvider(() -> playlistsList.getItems());
            }

            lastPageNode = pageContainer.getChildren().isEmpty() ? null : pageContainer.getChildren().get(0);
            playlistPageController.setOnBack(this::goBackFromPlaylist);

            playlistPageController.setPlaylist(p);
            showNode(playlistPageRoot);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openDiscoverPlaylist(DiscoverPlaylistDTO p) {
        try {
            if (playlistPageRoot == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/PlaylistPage.fxml"));
                loader.setControllerFactory(springContext::getBean);
                playlistPageRoot = loader.load();
                playlistPageController = loader.getController();

                playlistPageController.setOnPlayRequested(this::playPreviewFromPlaylistTrack);
                playlistPageController.setPlaylistsProvider(() -> playlistsList.getItems());
            }

            lastPageNode = pageContainer.getChildren().isEmpty() ? null : pageContainer.getChildren().get(0);
            playlistPageController.setOnBack(this::goBackFromPlaylist);

            playlistPageController.showDiscover(p.id(), p.name());
            showNode(playlistPageRoot);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // back din playlist
    private void goBackFromPlaylist() {
        if (lastPageNode != null) showNode(lastPageNode);
        else showDiscoverPage();
    }

    private void setupPlaylistsListView() {
        playlistsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PlaylistDTO item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setContextMenu(null);
                    setStyle("");
                    return;
                }

                setText(item.name());
                setStyle("-fx-text-fill: #b3b3b3;");

                MenuItem rename = new MenuItem("Rename");
                rename.setOnAction(e -> renamePlaylist(item));

                MenuItem delete = new MenuItem("Delete");
                delete.setOnAction(e -> deletePlaylist(item));

                setContextMenu(new ContextMenu(rename, delete));
            }
        });

        playlistsList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                PlaylistDTO selected = playlistsList.getSelectionModel().getSelectedItem();
                if (selected != null) openPlaylistPage(selected);
            }
        });
    }

    // incarca playlist-urile
    private void loadPlaylists() {
        Task<List<PlaylistDTO>> task = new Task<>() {
            @Override
            protected List<PlaylistDTO> call() throws Exception {
                String json = apiClient.get()
                        .uri("/api/playlists")
                        .header("X-User-Id", String.valueOf(AppSession.userId))
                        .retrieve()
                        .body(String.class);

                PlaylistDTO[] arr = mapper.readValue(json, PlaylistDTO[].class);
                return arr == null ? List.of() : Arrays.asList(arr);
            }
        };

        task.setOnSucceeded(e -> {
            playlistsList.getItems().setAll(task.getValue());
            playlistsList.refresh();
        });

        task.setOnFailed(e -> task.getException().printStackTrace());

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // adaugare piesa in playlist
    private void addTrackToPlaylist(Long playlistId, TrackDTO t) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                AddTrackRequest req = new AddTrackRequest(
                        t.getId(),
                        t.getTitle(),
                        t.getArtist(),
                        t.getCoverUrl(),
                        t.getPreviewUrl()
                );

                String body = mapper.writeValueAsString(req);

                apiClient.post()
                        .uri("/api/playlists/{id}/tracks", playlistId)
                        .header("X-User-Id", String.valueOf(AppSession.userId))
                        .header("Content-Type", "application/json")
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();

                return null;
            }
        };

        task.setOnSucceeded(e -> {
            if (playlistPageController != null) {
                playlistPageController.reloadIfShowing(playlistId);
            }
        });

        task.setOnFailed(e -> task.getException().printStackTrace());

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    // play song
    private void playPreview(TrackDTO t) {
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            try { player.dispose(); } catch (Exception ignored) {}
            player = null;
        }

        currentSongLabel.setText(t.getTitle());
        currentAuthorLabel.setText(t.getArtist());

        nowPlayingTrack = t;
        if (addCurrentToPlaylistBtn != null)
            addCurrentToPlaylistBtn.setDisable(false);

        Image coverImg;
        if (t.getCoverUrl() == null || t.getCoverUrl().isBlank()) {
            coverImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/placeholder.png")));
        } else {
            coverImg = new Image(t.getCoverUrl(), true);
        }

        ImageView iv = new ImageView(coverImg);
        iv.setFitWidth(50);
        iv.setFitHeight(50);
        iv.setPreserveRatio(true);
        nowPlayingCoverContainer.getChildren().setAll(iv);

        Media media = new Media(t.getPreviewUrl());
        media.setOnError(() -> System.out.println("Media error: " + media.getError()));

        player = new MediaPlayer(media);
        player.setOnError(() -> System.out.println("Player error: " + player.getError()));

        hookPlayer(player);
        player.play();

        isPlaying = true;
        playPauseButton.setText("⏸");
    }

    // play dupa deezerid (nu dupa link)
    private void playByDeezerId(long deezerId, String title, String artist, String coverUrl) {
        currentSongLabel.setText(title);
        currentAuthorLabel.setText(artist);

        if (coverUrl != null && !coverUrl.isBlank()) {
            ImageView iv = new ImageView(new Image(coverUrl, true));
            iv.setFitWidth(50);
            iv.setFitHeight(50);
            iv.setPreserveRatio(true);
            nowPlayingCoverContainer.getChildren().setAll(iv);
        }

        Task<TrackDTO> task = new Task<>() {
            @Override
            protected TrackDTO call() throws Exception {
                String json = apiClient.get()
                        .uri("/api/music/track/{id}", deezerId)
                        .retrieve()
                        .body(String.class);

                return mapper.readValue(json, TrackDTO.class);
            }
        };

        task.setOnSucceeded(e -> playPreview(task.getValue()));
        task.setOnFailed(e -> task.getException().printStackTrace());

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    // play din playlist
    private void playPreviewFromPlaylistTrack(PlaylistTrackDTO t) {
        playByDeezerId(t.deezerId(), t.title(), t.artist(), t.coverUrl());
    }

    // logica pentru slidere (progres si volum)
    private void hookPlayer(MediaPlayer p) {
        p.setOnReady(() -> {
            double total = Math.max(p.getTotalDuration().toSeconds(), 0.1);
            progressSlider.setMax(total);
            progressSlider.setValue(0);
            if (progressFill != null) progressFill.setProgress(0);
        });

        if (timeListener != null && p != null) {}

        timeListener = (obs, oldValue, newValue) -> {
            if (!progressSlider.isValueChanging()) {
                progressSlider.setValue(newValue.toSeconds());
            }

            double total = Math.max(p.getTotalDuration().toSeconds(), 0.1);
            if (progressFill != null) progressFill.setProgress(newValue.toSeconds() / total);
        };

        p.currentTimeProperty().addListener(timeListener);

        if (volumeSlider != null) {
            p.setVolume(volumeSlider.getValue() / 100.0);
        }
    }

    // butonul de start / stop melodie
    @FXML
    public void onPlayPauseClick() {
        if (player == null) return;

        if (isPlaying) {
            player.pause();
            playPauseButton.setText("▶");
        } else {
            player.play();
            playPauseButton.setText("⏸");
        }
        isPlaying = !isPlaying;
    }

    // setup slider volum
    private void setupVolume() {
        if (volumeSlider == null) return;

        volumeSlider.setValue(50);
        volumeSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (player != null) {
                try {
                    player.setVolume(newV.doubleValue() / 100.0);
                } catch (Exception ignored) {}
            }
        });
    }

    // cand utilizatorul trage de slider-ul de progres
    private void setupProgressSeek() {
        if (progressSlider == null) return;

        progressSlider.valueChangingProperty().addListener((obs, was, is) -> {
            if (!is && player != null) {
                player.seek(Duration.seconds(progressSlider.getValue()));
            }
        });

        progressSlider.setOnMouseReleased(e -> {
            if (player != null) {
                player.seek(Duration.seconds(progressSlider.getValue()));
            }
        });
    }

    // inchidere app
    @FXML
    public void onCloseClick() {
        System.exit(0);
    }

    // minimizare
    @FXML
    public void onMinimizeClick() {
        Stage stage = (Stage) mainContainer.getScene().getWindow();
        stage.setIconified(true);
    }

    // logica pentru mutarea ferestrei (doar pe sidebar / partea stanga)
    private void setupWindowDrag() {
        if (sidebar == null) return;

        sidebar.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });

        sidebar.setOnMouseDragged(e -> {
            Stage stage = (Stage) sidebar.getScene().getWindow();
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });
    }

    // creare playlist
    @FXML
    public void onCreatePlaylist() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Create Playlist");
        d.setHeaderText(null);
        d.setContentText("Name:");

        styleDialog(d, false);
        enforcePlaylistNameValidation(d);

        d.showAndWait().ifPresent(name -> {
            String n = name.trim();
            if (n.isEmpty()) return;

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    apiClient.post()
                            .uri("/api/playlists?name={n}", n)
                            .header("X-User-Id", String.valueOf(AppSession.userId))
                            .retrieve()
                            .toBodilessEntity();
                    return null;
                }
            };

            task.setOnSucceeded(e -> loadPlaylists());
            task.setOnFailed(e -> task.getException().printStackTrace());

            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
        });
    }

    private void renamePlaylist(PlaylistDTO p) {
        TextInputDialog d = new TextInputDialog(p.name());
        d.setTitle("Rename Playlist");
        d.setHeaderText(null);
        d.setContentText("New name:");

        styleDialog(d, false);

        d.showAndWait().ifPresent(newName -> {
            String n = newName.trim();
            if (n.isEmpty()) return;

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    apiClient.put()
                            .uri("/api/playlists/{id}?name={n}", p.id(), n)
                            .header("X-User-Id", String.valueOf(AppSession.userId))
                            .retrieve()
                            .toBodilessEntity();
                    return null;
                }
            };

            task.setOnSucceeded(e -> loadPlaylists());
            task.setOnFailed(e -> task.getException().printStackTrace());

            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
        });
    }

    private void deletePlaylist(PlaylistDTO p) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Delete Playlist");
        a.setHeaderText(null);
        a.setContentText("Delete \"" + p.name() + "\"?");
        styleDialog(a, true);

        a.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    apiClient.delete()
                            .uri("/api/playlists/{id}", p.id())
                            .header("X-User-Id", String.valueOf(AppSession.userId))
                            .retrieve()
                            .toBodilessEntity();
                    return null;
                }
            };

            task.setOnSucceeded(e -> loadPlaylists());
            task.setOnFailed(e -> task.getException().printStackTrace());

            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
        });
    }

    // stilizare pentru casutele de prompt
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

    @FXML
    public void onAiNavClick() {
        showAiPage();
    }

    private void showAiPage() {
        try {
            if (aiPageRoot == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AiPage.fxml"));
                loader.setControllerFactory(springContext::getBean);
                aiPageRoot = loader.load();
                aiPageController = loader.getController();

                aiPageController.setOnPlayRequested(this::playPreview);
                aiPageController.setPlaylistsProvider(() -> playlistsList.getItems());
                aiPageController.setOnAddToPlaylist(this::addTrackToPlaylist);
                aiPageController.setOnPlaylistsChanged(this::loadPlaylists);
            }
            pageContainer.getChildren().setAll(aiPageRoot);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // buton pentru adaugarea melodiei curente (butonul +)
    @FXML
    public void onAddCurrentToPlaylist() {
        if (nowPlayingTrack == null) return;

        ContextMenu menu = new ContextMenu();
        Menu addTo = new Menu("Add to playlist");
        menu.getItems().add(addTo);

        menu.setOnShowing(ev -> {
            addTo.getItems().clear();

            for (PlaylistDTO p : playlistsList.getItems()) {
                MenuItem mi = new MenuItem(p.name());
                mi.setOnAction(ae -> addTrackToPlaylist(p.id(), nowPlayingTrack));
                addTo.getItems().add(mi);
            }

            if (addTo.getItems().isEmpty()) {
                MenuItem none = new MenuItem("(No playlists)");
                none.setDisable(true);
                addTo.getItems().add(none);
            }
        });

        menu.show(addCurrentToPlaylistBtn, javafx.geometry.Side.TOP, 0, 0);
    }

    // niste validari pentru numele din playlist
    private void enforcePlaylistNameValidation(TextInputDialog d) {
        DialogPane dp = d.getDialogPane();

        Label err = new Label();
        err.setStyle("-fx-text-fill: #ff5555;");

        Node content = dp.getContent();
        VBox box = new VBox(8);
        if (content != null) box.getChildren().add(content);
        box.getChildren().add(err);
        dp.setContent(box);

        Button ok = (Button) dp.lookupButton(ButtonType.OK);
        TextField editor = d.getEditor();

        BooleanBinding invalid = new BooleanBinding() {
            { bind(editor.textProperty()); }
            @Override protected boolean computeValue() {
                String s = editor.getText() == null ? "" : editor.getText().trim();
                if (s.isBlank()) {
                    err.setText("Name is required.");
                    return true;
                }
                if (s.length() > 40) {
                    err.setText("Max 40 characters.");
                    return true;
                }
                err.setText("");
                return false;
            }
        };

        ok.disableProperty().bind(invalid);

        ok.addEventFilter(ActionEvent.ACTION, ev -> {
            if (invalid.get()) ev.consume();
        });
    }
}
