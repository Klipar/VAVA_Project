package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.Config;
import com.rabbit.client.service.ApiClient;
import com.rabbit.common.dto.UserDto;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.RadioButton;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.control.ScrollBar;
import lombok.Setter;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

public class AdminController {

    @FXML private StackPane overlayPane;
    @FXML private TextField searchLoginField;
    @FXML private Button openAddUserButton;
    @FXML private Button searchButton;
    @FXML private TableView<UserDto> usersTable;
    @FXML private TableColumn<UserDto, String> loginColumn;
    @FXML private TableColumn<UserDto, String> nameColumn;
    @FXML private TableColumn<UserDto, String> emailColumn;
    @FXML private TableColumn<UserDto, String> roleColumn;
    @FXML private TableColumn<UserDto, Void> actionsColumn;
    @FXML private RadioButton searchByLoginRadio;
    @FXML private RadioButton searchByNameRadio;
    @FXML private RadioButton searchByEmailRadio;

    private final ApiClient apiClient = ApiClient.getInstance();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final ObservableList<UserDto> users = FXCollections.observableArrayList();
    private List<UserDto> allUsers = new ArrayList<>();

    private final ToggleGroup searchToggleGroup = new ToggleGroup();

    @Setter
    private MainController mainController;

    @FXML
    public void initialize() {
        var rb = Config.getInstance().getBundle();
        searchByLoginRadio.setText(rb.getString("admin_search_login"));
        searchByNameRadio.setText(rb.getString("admin_search_name"));
        searchByEmailRadio.setText(rb.getString("admin_search_email"));
        if (openAddUserButton != null) openAddUserButton.setText(rb.getString("admin_add_user"));
        if (searchLoginField != null) searchLoginField.setPromptText(rb.getString("admin_search_placeholder"));
        loginColumn.setText(rb.getString("admin_col_login"));
        nameColumn.setText(rb.getString("admin_col_full_name"));
        emailColumn.setText(rb.getString("admin_col_email"));
        roleColumn.setText(rb.getString("admin_col_role"));
        actionsColumn.setText(rb.getString("admin_col_actions"));

        searchToggleGroup.getToggles().addAll(searchByLoginRadio, searchByNameRadio, searchByEmailRadio);

        configureTable();
        if (searchButton != null) {
            searchButton.setOnAction(event -> handleSearchUser());
        }
        if (openAddUserButton != null) {
            openAddUserButton.setOnAction(event -> openAddUserPopup());
        }
        loadAllUsers();
    }

    private void configureTable() {
        // ВИПРАВЛЕННЯ: Розтягуємо колонки на всю доступну ширину
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // ВИПРАВЛЕННЯ: Збільшуємо загальний розмір тексту в таблиці
        usersTable.setStyle("-fx-font-size: 14px; -fx-selection-bar: rgba(127, 214, 255, 0.1);");

        loginColumn.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getNickname())));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getName())));
        emailColumn.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getEmail())));
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getRole() != null ? data.getValue().getRole().name() : "-"
        ));
        actionsColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(null));

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final ImageView editIcon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/rabbit/client/images/edit.png"))));
            private final ImageView deleteIcon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/rabbit/client/images/trash.png"))));

            private final Button editButton = new Button();
            private final Button deleteButton = new Button();
            private final HBox actionsBox = new HBox(12, editButton, deleteButton);

            {
                editIcon.setFitWidth(18);
                editIcon.setFitHeight(18);
                deleteIcon.setFitWidth(18);
                deleteIcon.setFitHeight(18);

                ColorAdjust makeWhite = new ColorAdjust();
                makeWhite.setBrightness(1.0);

                editIcon.setEffect(makeWhite);
                deleteIcon.setEffect(makeWhite);

                editButton.setGraphic(editIcon);
                deleteButton.setGraphic(deleteIcon);

                String baseButtonStyle = "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5; -fx-background-radius: 5;";
                editButton.setStyle(baseButtonStyle);
                deleteButton.setStyle(baseButtonStyle);

                editButton.setOnMouseEntered(e -> editButton.setStyle(baseButtonStyle + "-fx-background-color: rgba(127, 214, 255, 0.15);"));
                editButton.setOnMouseExited(e  -> editButton.setStyle(baseButtonStyle));

                deleteButton.setOnMouseEntered(e -> deleteButton.setStyle(baseButtonStyle + "-fx-background-color: rgba(240, 128, 128, 0.15);"));
                deleteButton.setOnMouseExited(e  -> deleteButton.setStyle(baseButtonStyle));

                actionsBox.setAlignment(javafx.geometry.Pos.CENTER);

                editButton.setOnAction(event -> {
                    UserDto user = getTableView().getItems().get(getIndex());
                    if (user != null) openEditUserPopup(user);
                });

                deleteButton.setOnAction(event -> {
                    UserDto user = getTableView().getItems().get(getIndex());
                    if (user != null) openDeleteUserPopup(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionsBox);
            }

            private UserDto getCurrentTableRow() {
                if (getIndex() < 0 || getIndex() >= usersTable.getItems().size()) return null;
                return usersTable.getItems().get(getIndex());
            }
        });

        usersTable.setItems(users);
    }

    // Решта методів (handleSearchUser, loadAllUsers тощо) залишаються без змін...

    @FXML
    private void handleSearchUser() {
        String query = searchLoginField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            users.setAll(allUsers);
            return;
        }
        new Thread(() -> {
            try {
                List<UserDto> filtered = new ArrayList<>();
                for (UserDto user : allUsers) {
                    String fieldToCheck = "";
                    if (searchByLoginRadio.isSelected()) fieldToCheck = user.getNickname();
                    else if (searchByNameRadio.isSelected()) fieldToCheck = user.getName();
                    else if (searchByEmailRadio.isSelected()) fieldToCheck = user.getEmail();
                    if (fieldToCheck != null && fieldToCheck.toLowerCase().contains(query)) filtered.add(user);
                }
                Platform.runLater(() -> users.setAll(filtered));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML
    private void openAddUserPopup() {
        openPopupOverlay("/com/rabbit/client/fxml/add-user-popup.fxml", loader -> {
            AddUserPopupController controller = loader.getController();
            controller.setMainController(mainController);
            controller.setup(() -> {
                loadAllUsers();
                if (mainController != null) mainController.showGlobalNotification("User created successfully", "#5db583");
            });
        });
    }

    private void openEditUserPopup(UserDto user) {
        openPopupOverlay("/com/rabbit/client/fxml/edit-user-popup.fxml", loader -> {
            EditUserPopupController controller = loader.getController();
            controller.setMainController(mainController);
            controller.setup(user, updatedUser -> {
                loadAllUsers();
                if (mainController != null) mainController.showGlobalNotification("User updated successfully", "#5db583");
            });
        });
    }

    private void openDeleteUserPopup(UserDto user) {
        openPopupOverlay("/com/rabbit/client/fxml/delete-user-popup.fxml", loader -> {
            DeleteUserPopupController controller = loader.getController();
            controller.setMainController(mainController);
            controller.setup(user, () -> {
                loadAllUsers();
                if (mainController != null) mainController.showGlobalNotification("User deleted successfully", "#c65f85");
            });
        });
    }

    private void openPopupOverlay(String fxmlPath, PopupConfigurer configurer) {
        try {
            java.net.URL url = getClass().getResource(fxmlPath);
            if (url == null) return;
            FXMLLoader loader = new FXMLLoader(url);
            Parent popupRoot = loader.load();
            configurer.configure(loader);
            Pane rootHost = getRootHost();
            if (rootHost != null) {
                if (popupRoot instanceof Region region) {
                    region.prefWidthProperty().bind(rootHost.widthProperty());
                    region.prefHeightProperty().bind(rootHost.heightProperty());
                }
                rootHost.getChildren().add(popupRoot);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private Pane getRootHost() {
        if (overlayPane != null && overlayPane.getScene() != null) {
            Node sceneRoot = overlayPane.getScene().getRoot();
            if (sceneRoot instanceof Pane pane) return pane;
        }
        return null;
    }

    private void loadAllUsers() {
        new Thread(() -> {
            try {
                HttpResponse<String> response = apiClient.get("/users/all");
                Platform.runLater(() -> {
                    if (apiClient.isSuccess(response)) {
                        try {
                            List<UserDto> loadedUsers = mapper.readValue(response.body(), new TypeReference<List<UserDto>>() {});
                            allUsers = loadedUsers;
                            users.setAll(loadedUsers);
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    @FunctionalInterface
    private interface PopupConfigurer {
        void configure(FXMLLoader loader) throws Exception;
    }
}