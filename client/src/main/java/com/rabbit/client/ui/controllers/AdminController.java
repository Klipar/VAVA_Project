package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
        usersTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        loginColumn.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getNickname())));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getName())));
        emailColumn.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getEmail())));
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getRole() != null ? data.getValue().getRole().name() : "-"
        ));
        actionsColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(null));

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("\u270F");
            private final Button deleteButton = new Button("\uD83D\uDDD1");
            private final HBox actionsBox = new HBox(6, editButton, deleteButton);

            {
                editButton.getStyleClass().add("admin-row-action-button");
                deleteButton.getStyleClass().add("admin-row-action-button");
                editButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #7fd6ff; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 6; -fx-background-radius: 5;");
                deleteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #f08080; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 6; -fx-background-radius: 5;");

                editButton.setOnMouseEntered(e -> editButton.setStyle("-fx-background-color: rgba(90,153,195,0.22); -fx-text-fill: #7fd6ff; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 6; -fx-background-radius: 5;"));
                editButton.setOnMouseExited(e  -> editButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #7fd6ff; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 6; -fx-background-radius: 5;"));
                deleteButton.setOnMouseEntered(e -> deleteButton.setStyle("-fx-background-color: rgba(198,95,133,0.22); -fx-text-fill: #f08080; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 6; -fx-background-radius: 5;"));
                deleteButton.setOnMouseExited(e  -> deleteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #f08080; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 6; -fx-background-radius: 5;"));

                actionsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                editButton.setOnAction(event -> {
                    UserDto user = getCurrentTableRow();
                    if (user != null) openEditUserPopup(user);
                });

                deleteButton.setOnAction(event -> {
                    UserDto user = getCurrentTableRow();
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

        // left mouse drag panning for horizontal scroll
        final double[] lastX = new double[1];
        usersTable.setOnMousePressed(evt -> {
            if (evt.isPrimaryButtonDown()) lastX[0] = evt.getScreenX();
        });
        usersTable.setOnMouseDragged(evt -> {
            if (evt.isPrimaryButtonDown()) {
                ScrollBar h = findHorizontalScrollBar();
                if (h != null) {
                    double dx = evt.getScreenX() - lastX[0];
                    double range = h.getMax() - h.getMin();
                    if (range > 0) {
                        double delta = -dx / 2.0;
                        double newVal = Math.min(h.getMax(), Math.max(h.getMin(), h.getValue() + delta));
                        h.setValue(newVal);
                    }
                    lastX[0] = evt.getScreenX();
                    evt.consume();
                }
            }
        });
    }

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
                    if (searchByLoginRadio.isSelected()) {
                        fieldToCheck = user.getNickname();
                    } else if (searchByNameRadio.isSelected()) {
                        fieldToCheck = user.getName();
                    } else if (searchByEmailRadio.isSelected()) {
                        fieldToCheck = user.getEmail();
                    }
                    if (fieldToCheck != null && fieldToCheck.toLowerCase().contains(query)) {
                        filtered.add(user);
                    }
                }
                Platform.runLater(() -> users.setAll(filtered));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void openAddUserPopup() {
        openPopupOverlay("/com/rabbit/client/fxml/add-user-popup.fxml", loader -> {
            AddUserPopupController controller = loader.getController();
            controller.setMainController(mainController);
            controller.setup(() -> {
                loadAllUsers();
                if (mainController != null) {
                    mainController.showGlobalNotification("User created successfully", "#5db583");
                }
            });
        });
    }

    private void openEditUserPopup(UserDto user) {
        openPopupOverlay("/com/rabbit/client/fxml/edit-user-popup.fxml", loader -> {
            EditUserPopupController controller = loader.getController();
            controller.setMainController(mainController);
            controller.setup(user, updatedUser -> {
                if (updatedUser != null) {
                    // Find by ID
                    for (int i = 0; i < users.size(); i++) {
                        if (users.get(i).getId() != null && users.get(i).getId().equals(updatedUser.getId())) {
                            users.set(i, updatedUser);
                            break;
                        }
                    }
                    for (int i = 0; i < allUsers.size(); i++) {
                        if (allUsers.get(i).getId() != null && allUsers.get(i).getId().equals(updatedUser.getId())) {
                            allUsers.set(i, updatedUser);
                            break;
                        }
                    }
                }
                loadAllUsers();
                if (mainController != null) {
                    mainController.showGlobalNotification("User updated successfully", "#5db583");
                }
            });
        });
    }

    private void openDeleteUserPopup(UserDto user) {
        openPopupOverlay("/com/rabbit/client/fxml/delete-user-popup.fxml", loader -> {
            DeleteUserPopupController controller = loader.getController();
            controller.setMainController(mainController);
            controller.setup(user, () -> {
                loadAllUsers();
                if (mainController != null) {
                    mainController.showGlobalNotification("User deleted successfully", "#c65f85");
                }
            });
        });
    }

    /**
     * Loads the popup FXML and injects it directly into the scene's root Pane
     * as a full-window overlay — no new Stage/window is opened.
     *
     * Each popup FXML root is a StackPane with styleClass="overlay" (dark semi-transparent
     * background + centered card). Its own closePopup() removes it from its parent when dismissed.
     */
    private void openPopupOverlay(String fxmlPath, PopupConfigurer configurer) {
        try {
            java.net.URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                showAlert("Error", "FXML resource not found: " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent popupRoot = loader.load();
            configurer.configure(loader);

            Pane rootHost = getRootHost();
            if (rootHost != null) {
                // Stretch the overlay to cover the full root pane
                if (popupRoot instanceof Region region) {
                    region.prefWidthProperty().bind(rootHost.widthProperty());
                    region.prefHeightProperty().bind(rootHost.heightProperty());
                }
                rootHost.getChildren().add(popupRoot);
            } else {
                showAlert("Error", "Cannot find root pane to display popup.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorWithDetails("Error", "Failed to open popup: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the topmost Pane in the scene so we can inject full-window overlays.
     * Prefers the scene root (covers sidebar + content); falls back to walking parents.
     */
    private Pane getRootHost() {
        if (overlayPane != null && overlayPane.getScene() != null) {
            Node sceneRoot = overlayPane.getScene().getRoot();
            if (sceneRoot instanceof Pane pane) return pane;
        }
        // Walk up parent chain
        Node current = overlayPane;
        Pane topPane = null;
        while (current != null) {
            if (current instanceof Pane pane) topPane = pane;
            current = current.getParent();
        }
        return topPane;
    }

    private void showErrorWithDetails(String title, String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String exceptionText = sw.toString();

        javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        javafx.scene.layout.GridPane.setVgrow(textArea, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.GridPane.setHgrow(textArea, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.layout.GridPane expContent = new javafx.scene.layout.GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);

        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }

    private ScrollBar findHorizontalScrollBar() {
        for (Node n : usersTable.lookupAll(".scroll-bar")) {
            if (n instanceof ScrollBar sb) {
                if (sb.getOrientation() == javafx.geometry.Orientation.HORIZONTAL) return sb;
            }
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
                        } catch (Exception e) {
                            showAlert("Error", "Failed to parse users list: " + e.getMessage());
                        }
                    } else {
                        showAlert("Error", parseError(response.body()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Failed to load users: " + e.getMessage()));
            }
        }).start();
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String parseError(String responseBody) {
        try {
            java.util.Map<String, Object> errorMap = mapper.readValue(responseBody, new TypeReference<java.util.Map<String, Object>>() {});
            Object error = errorMap.get("error");
            if (error != null) return error.toString();
        } catch (Exception ignored) {}
        return "Request failed";
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FunctionalInterface
    private interface PopupConfigurer {
        void configure(FXMLLoader loader) throws Exception;
    }
}