/** Clasa pentru controlul paginii JavaFX 'Home/Discover';
 *  incarca carduri cu playlisturi si navigheaza catre pagina de playlist
 * @author Mirica Alin-Marian
 * @version 29 Decembrie 2025
 */

package com.proiect.spotifyclone.controller;

import com.proiect.spotifyclone.dto.DiscoverPlaylistDTO;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Component
public class DiscoverPageController {

    @FXML private FlowPane discoverPane;

    private final RestClient apiClient = RestClient.create("http://localhost:8080");
    private final ObjectMapper mapper = new ObjectMapper();

    private Consumer<DiscoverPlaylistDTO> onOpen;

    public void setOnOpen(Consumer<DiscoverPlaylistDTO> onOpen) {
        this.onOpen = onOpen;
    }

    public void loadDiscover() {
        if (discoverPane == null) return;
        discoverPane.getChildren().clear();

        Task<List<DiscoverPlaylistDTO>> task = new Task<>() {
            @Override
            protected List<DiscoverPlaylistDTO> call() throws Exception {
                String json = apiClient.get()
                        .uri("/api/discover")
                        .retrieve()
                        .body(String.class);

                DiscoverPlaylistDTO[] arr = mapper.readValue(json, DiscoverPlaylistDTO[].class);
                return arr == null ? List.of() : Arrays.asList(arr);
            }
        };

        task.setOnSucceeded(e -> render(task.getValue()));
        task.setOnFailed(e -> task.getException().printStackTrace());

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void render(List<DiscoverPlaylistDTO> items) {
        discoverPane.getChildren().clear();

        for (DiscoverPlaylistDTO p : items) {
            VBox card = new VBox(10);
            card.setPrefWidth(160);
            card.setStyle("""
                -fx-background-color: #181818;
                -fx-background-radius: 12;
                -fx-padding: 12;
                -fx-cursor: hand;
            """);

            ImageView iv = new ImageView();
            iv.setFitWidth(136);
            iv.setFitHeight(136);
            iv.setPreserveRatio(true);

            Image img = (p.coverUrl() == null || p.coverUrl().isBlank())
                    ? new Image(getClass().getResourceAsStream("/placeholder.png"))
                    : new Image(p.coverUrl(), true);

            iv.setImage(img);

            Label name = new Label(p.name());
            name.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

            Label hint = new Label("Top playlist");
            hint.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 11;");

            card.getChildren().addAll(iv, name, hint);

            card.setOnMouseEntered(e -> card.setStyle("""
                -fx-background-color: #202020;
                -fx-background-radius: 12;
                -fx-padding: 12;
                -fx-cursor: hand;
            """));

            card.setOnMouseExited(e -> card.setStyle("""
                -fx-background-color: #181818;
                -fx-background-radius: 12;
                -fx-padding: 12;
                -fx-cursor: hand;
            """));

            card.setOnMouseClicked(e -> {
                if (onOpen != null) onOpen.accept(p);
            });

            discoverPane.getChildren().add(card);
        }
    }
}
