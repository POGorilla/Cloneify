/** Clasa pentru controlul paginii de cautare;
 *  executa cautarea, afiseaza rezultate
 *  si permite adaugarea unei melodii in playlisturi
 * @author Mirica Alin-Marian
 * @version 29 Decembrie 2025
 */

package com.proiect.spotifyclone.controller;

import com.proiect.spotifyclone.dto.PlaylistDTO;
import com.proiect.spotifyclone.dto.TrackDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
public class SearchPageController {

    @FXML private TextField searchField;
    @FXML private VBox resultsContainer;

    private Consumer<String> onSearch;
    private Consumer<TrackDTO> onPlay;

    // Provider cu playlisturi (pentru context menu “Add to playlist”
    private Supplier<List<PlaylistDTO>> playlistsProvider = List::of;

    // Callback “Add to playlist” (playlistId + track)
    private BiConsumer<Long, TrackDTO> onAddToPlaylist;

    public void setOnSearch(Consumer<String> cb) { this.onSearch = cb; }
    public void setOnPlay(Consumer<TrackDTO> cb) { this.onPlay = cb; }

    public void setPlaylistsProvider(Supplier<List<PlaylistDTO>> provider) {
        this.playlistsProvider = provider == null ? List::of : provider;
    }

    public void setOnAddToPlaylist(BiConsumer<Long, TrackDTO> cb) {
        this.onAddToPlaylist = cb;
    }

    @FXML
    public void initialize() {
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onSearchClick();
        });
    }

    @FXML
    public void onSearchClick() {
        if (onSearch == null) return;
        onSearch.accept(searchField.getText());
    }

    // TextField-ul de search primeste focus (cursorul sa fie in el) si muta cursorul la finalul textului
    public void focusSearch() {
        Platform.runLater(() -> {
            searchField.requestFocus();
            searchField.positionCaret(searchField.getText().length());
        });
    }

    public void showLoading() {
        resultsContainer.getChildren().setAll(labelMuted("Loading..."));
    }

    public void showError(String msg) {
        Label l = new Label(msg);
        l.setStyle("-fx-text-fill: #ff5555; -fx-padding: 8;");
        resultsContainer.getChildren().setAll(l);
    }

    // randare rezultate
    public void renderResults(List<TrackDTO> tracks) {
        resultsContainer.getChildren().clear();

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
            row.setStyle("-fx-padding: 8; -fx-cursor: hand;");

            // doar click stanga pentru play
            row.setOnMouseClicked(e -> {
                if (e.getButton() != MouseButton.PRIMARY) return;
                if (onPlay != null) onPlay.accept(t);
            });

            // hover
            row.setOnMouseEntered(e ->
                    row.setStyle("-fx-padding: 8; -fx-background-color: #1a1a1a; -fx-background-radius: 8; -fx-cursor: hand;")
            );
            row.setOnMouseExited(e ->
                    row.setStyle("-fx-padding: 8; -fx-cursor: hand;")
            );

            // afisare meniu doar pe click dreapta
            ContextMenu menu = new ContextMenu();
            Menu addTo = new Menu("Add to playlist");
            menu.getItems().add(addTo);

            menu.setOnShowing(ev -> {
                addTo.getItems().clear();

                List<PlaylistDTO> pls = playlistsProvider.get();
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

            resultsContainer.getChildren().add(row);
        }

        if (tracks.isEmpty()) {
            resultsContainer.getChildren().add(labelMuted("No results."));
        }
    }

    private Label labelMuted(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #b3b3b3; -fx-padding: 8;");
        return l;
    }
}
