package com.example.javajxproject;

import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.sql.Date;
import java.time.LocalDate;
import java.sql.Connection;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.DatePicker;


import java.sql.*;

public class HelloApplication extends Application {

    private TextField idField, numberField, priceField, minStayField, maxStayField, searchField;
    private ComboBox<String> capacityComboBox, roomTypeComboBox;
    private TableView<Room> roomTableView;
    private DatePicker arrivalDatePicker;
    private DatePicker departureDatePicker;

    private Connection connectDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:C:\\Users\\ALIMZHAN-PC\\Desktop\\OOP Java\\javajxproject_3test\\sqlforjavafxproject5.db");
            String createTableSQL = """
        CREATE TABLE IF NOT EXISTS rooms (
            id TEXT PRIMARY KEY,
            number TEXT,
            capacity TEXT,
            price REAL,
            room_type TEXT,
            min_stay INTEGER,
            max_stay INTEGER,
            room_status TEXT,
            arrival_date DATE,
            departure_date DATE
        );
        """;
            Statement stmt = conn.createStatement();
            stmt.execute(createTableSQL);
            return conn;
        } catch (SQLException e) {
            System.err.println("Ошибка подключения к базе данных: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            System.err.println("Драйвер SQLite не найден: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    private boolean isIdUnique(String id) {
        try (Connection conn = connectDatabase();
             PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM rooms WHERE id = ?")) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void addRoomToDatabase(String id, String number, String capacity, double price, String roomType,
                                   Integer minStay, Integer maxStay, String roomStatus, LocalDate arrivalDate, LocalDate departureDate) {
        if (id == null || id.isEmpty()) {
            id = "не указано"; // Значение по умолчанию
        }

        try (Connection conn = connectDatabase()) {
            String insertSQL = """
        INSERT INTO rooms (id, number, capacity, price, room_type, min_stay, max_stay, room_status, arrival_date, departure_date) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
            PreparedStatement pstmt = conn.prepareStatement(insertSQL);
            pstmt.setString(1, id);
            pstmt.setString(2, number);
            pstmt.setString(3, capacity);
            pstmt.setDouble(4, price);
            pstmt.setString(5, roomType);
            pstmt.setObject(6, minStay);
            pstmt.setObject(7, maxStay);
            pstmt.setString(8, roomStatus);

            // Устанавливаем даты по умолчанию, если статус "Свободно"
            if ("Свободно".equals(roomStatus)) {
                pstmt.setObject(9, Date.valueOf("2024-01-01"));
                pstmt.setObject(10, Date.valueOf("2024-01-01"));
            } else {
                pstmt.setObject(9, arrivalDate != null ? Date.valueOf(arrivalDate) : null);
                pstmt.setObject(10, departureDate != null ? Date.valueOf(departureDate) : null);
            }

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    private void loadRoomsFromDatabase() {
        roomTableView.getItems().clear();
        try (Connection conn = connectDatabase();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rooms")) {

            while (rs.next()) {
                Room room = new Room(
                        rs.getString("id"),
                        rs.getString("number"),
                        rs.getString("capacity"),
                        rs.getDouble("price"),
                        rs.getString("room_type"),
                        rs.getInt("min_stay"),
                        rs.getInt("max_stay"),
                        rs.getString("room_status"),
                        rs.getDate("arrival_date") != null ? rs.getDate("arrival_date").toLocalDate() : null,
                        rs.getDate("departure_date") != null ? rs.getDate("departure_date").toLocalDate() : null
                );
                roomTableView.getItems().add(room);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void deleteRoomFromDatabase(String id) {
        try (Connection conn = connectDatabase();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM rooms WHERE id = ?")) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createLoginsTable() {
        try (Connection conn = connectDatabase()) {
            String createTableSQL = """
        CREATE TABLE IF NOT EXISTS logins_info (
            login TEXT PRIMARY KEY,
            password TEXT,
            number TEXT,
            FIO TEXT,
            room_number TEXT DEFAULT 'не указано',
            arrival_date DATE DEFAULT '2024-01-01',
            departure_date DATE DEFAULT '2024-01-01',
            total_amount TEXT DEFAULT 'не указано'
        );
        """;
            Statement stmt = conn.createStatement();
            stmt.execute(createTableSQL);

            System.out.println("Таблица logins_info успешно создана!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void start(Stage primaryStage) {
        createLoginsTable(); // Создаем таблицу logins_info, если ее нет

        primaryStage.setTitle("Hotel Management");

        // Создаем начальное окно
        VBox homeVBox = new VBox(10);
        homeVBox.setPadding(new Insets(10));
        homeVBox.getStyleClass().add("root-login"); // Применяем стиль фона

        Button adminLoginButton = new Button("Admin Login");
        adminLoginButton.getStyleClass().add("button-login"); // Применяем стиль кнопки
        adminLoginButton.setOnAction(e -> showAdminLoginWindow(primaryStage));

        Button customerLoginButton = new Button("Customer Login");
        customerLoginButton.getStyleClass().add("button-login"); // Применяем стиль кнопки
        customerLoginButton.setOnAction(e -> showCustomerLoginWindow(primaryStage));

        homeVBox.getChildren().addAll(adminLoginButton, customerLoginButton);

        Scene homeScene = new Scene(homeVBox, 300, 200);
        homeScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm()); // Подключаем CSS
        primaryStage.setScene(homeScene);
        primaryStage.show();
    }


    private void showHomeWindow(Stage primaryStage) {
        VBox homeVBox = new VBox(10);
        homeVBox.setPadding(new Insets(10));
        homeVBox.getStyleClass().add("root-login"); // Применяем стиль фона

        Button adminLoginButton = new Button("Admin Login");
        adminLoginButton.getStyleClass().add("button-login"); // Стиль кнопки
        adminLoginButton.setOnAction(e -> showAdminLoginWindow(primaryStage));

        Button customerLoginButton = new Button("Customer Login");
        customerLoginButton.getStyleClass().add("button-login"); // Стиль кнопки
        customerLoginButton.setOnAction(e -> showCustomerLoginWindow(primaryStage));

        homeVBox.getChildren().addAll(adminLoginButton, customerLoginButton);

        Scene homeScene = new Scene(homeVBox, 300, 200);
        homeScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm()); // Подключаем CSS
        primaryStage.setScene(homeScene);
    }

    private void showAdminLoginWindow(Stage primaryStage) {
        VBox adminLoginVBox = new VBox(10);
        adminLoginVBox.setPadding(new Insets(10));
        adminLoginVBox.getStyleClass().add("root-login");

        Label loginLabel = new Label("Login: Admin");
        loginLabel.getStyleClass().add("label-login");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("password-field-login");

        Button loginButton = new Button("Login");
        loginButton.getStyleClass().add("button-login");
        loginButton.setOnAction(e -> {
            if ("alimzhan22!".equals(passwordField.getText())) {
                showMainWindow(primaryStage);
            } else {
                showAlert("Ошибка входа", "Неверный пароль.");
            }
        });

        adminLoginVBox.getChildren().addAll(loginLabel, passwordField, loginButton);

        Scene adminLoginScene = new Scene(adminLoginVBox, 300, 200);
        adminLoginScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setScene(adminLoginScene);
    }


    private void showCustomerLoginWindow(Stage primaryStage) {
        VBox customerLoginVBox = new VBox(10);
        customerLoginVBox.setPadding(new Insets(10));
        customerLoginVBox.getStyleClass().add("root-login"); // Применяем стиль фона

        TextField loginField = new TextField();
        loginField.setPromptText("Login");
        loginField.getStyleClass().add("text-field-login");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("password-field-login");

        Button loginButton = new Button("Login");
        loginButton.getStyleClass().add("button-login");
        loginButton.setOnAction(e -> {
            if (validateCustomerLogin(loginField.getText(), passwordField.getText())) {
                showBookingWindow(primaryStage, loginField.getText()); // Переход в окно бронирования
            } else {
                showAlert("Ошибка входа", "Неверный логин или пароль.");
            }
        });

        // Кнопка "Создать пользователя"
        Button createUserButton = new Button("Создать пользователя");
        createUserButton.getStyleClass().add("button-login");
        createUserButton.setOnAction(e -> showCreateUserWindow());

        customerLoginVBox.getChildren().addAll(loginField, passwordField, loginButton, createUserButton);

        Scene customerLoginScene = new Scene(customerLoginVBox, 300, 250);
        customerLoginScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setScene(customerLoginScene);
    }
    private void showBookingWindow(Stage primaryStage, String currentUserLogin) {
        TableView<Room> bookingTableView = new TableView<>();

        // Убираем столбцы ID, Мин. прибывание и Макс. прибывание
        TableColumn<Room, String> numberColumn = new TableColumn<>("Номер");
        numberColumn.setCellValueFactory(cellData -> cellData.getValue().numberProperty());

        TableColumn<Room, String> capacityColumn = new TableColumn<>("Вместимость");
        capacityColumn.setCellValueFactory(cellData -> cellData.getValue().capacityProperty());

        TableColumn<Room, Double> priceColumn = new TableColumn<>("Цена");
        priceColumn.setCellValueFactory(cellData -> cellData.getValue().priceProperty().asObject());

        TableColumn<Room, String> typeColumn = new TableColumn<>("Тип номера");
        typeColumn.setCellValueFactory(cellData -> cellData.getValue().roomTypeProperty());

        bookingTableView.getColumns().addAll(numberColumn, capacityColumn, priceColumn, typeColumn);

        // Стили для таблицы
        bookingTableView.getStyleClass().add("table-view");
        bookingTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(bookingTableView, javafx.scene.layout.Priority.ALWAYS);

        // Загрузка только свободных номеров
        try (Connection conn = connectDatabase();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM rooms WHERE room_status = 'Свободно'")) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Room room = new Room(
                        rs.getString("id"),
                        rs.getString("number"),
                        rs.getString("capacity"),
                        rs.getDouble("price"),
                        rs.getString("room_type"),
                        null,
                        null,
                        rs.getString("room_status"),
                        rs.getDate("arrival_date") != null ? rs.getDate("arrival_date").toLocalDate() : null,
                        rs.getDate("departure_date") != null ? rs.getDate("departure_date").toLocalDate() : null
                );
                bookingTableView.getItems().add(room);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Поля для бронирования
        ComboBox<String> roomComboBox = new ComboBox<>();
        roomComboBox.setPromptText("Выберите номер");
        roomComboBox.getStyleClass().add("combo-box");

        try (Connection conn = connectDatabase();
             PreparedStatement pstmt = conn.prepareStatement("SELECT number FROM rooms WHERE room_status = 'Свободно'")) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                roomComboBox.getItems().add(rs.getString("number"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        DatePicker arrivalDatePicker = new DatePicker();
        arrivalDatePicker.setPromptText("Дата приезда");
        arrivalDatePicker.getStyleClass().add("text-field");

        DatePicker departureDatePicker = new DatePicker();
        departureDatePicker.setPromptText("Дата отъезда");
        departureDatePicker.getStyleClass().add("text-field");

        Button bookButton = new Button("Забронировать");
        bookButton.getStyleClass().add("button");
        bookButton.setOnAction(e -> {
            String selectedRoom = roomComboBox.getValue(); // Получаем выбранный номер
            LocalDate arrivalDate = arrivalDatePicker.getValue();
            LocalDate departureDate = departureDatePicker.getValue();

            if (selectedRoom == null || arrivalDate == null || departureDate == null) {
                showAlert("Ошибка ввода", "Пожалуйста, заполните все поля.");
                return;
            }

            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(arrivalDate, departureDate);
            if (daysBetween <= 0) {
                showAlert("Ошибка ввода", "Дата отъезда должна быть позже даты приезда.");
                return;
            }

            try (Connection conn = connectDatabase()) {
                conn.setAutoCommit(false); // Отключаем автокоммит для транзакции

                // Обновляем таблицу rooms
                String updateRoomSQL = """
            UPDATE rooms SET room_status = 'Занято', ID = ?, arrival_date = ?, departure_date = ? 
            WHERE number = ?
        """;
                try (PreparedStatement pstmt1 = conn.prepareStatement(updateRoomSQL)) {
                    pstmt1.setString(1, currentUserLogin);
                    pstmt1.setDate(2, Date.valueOf(arrivalDate));
                    pstmt1.setDate(3, Date.valueOf(departureDate));
                    pstmt1.setString(4, selectedRoom);
                    pstmt1.executeUpdate();
                }

                // Получаем цену комнаты для расчета итоговой суммы
                double pricePerDay = bookingTableView.getItems().stream()
                        .filter(room -> room.getNumber().equals(selectedRoom))
                        .findFirst()
                        .map(Room::getPrice)
                        .orElse(0.0);
                double totalAmount = pricePerDay * daysBetween;

                // Обновляем таблицу logins_info
                String updateUserSQL = """
            UPDATE logins_info SET room_number = ?, arrival_date = ?, departure_date = ?, total_amount = ? 
            WHERE login = ?
        """;
                try (PreparedStatement pstmt2 = conn.prepareStatement(updateUserSQL)) {
                    pstmt2.setString(1, selectedRoom);
                    pstmt2.setDate(2, Date.valueOf(arrivalDate));
                    pstmt2.setDate(3, Date.valueOf(departureDate));
                    pstmt2.setString(4, String.valueOf(totalAmount));
                    pstmt2.setString(5, currentUserLogin);
                    pstmt2.executeUpdate();
                }

                conn.commit(); // Подтверждаем транзакцию
                showAlert("Успех", "Бронирование успешно завершено!");
                bookingTableView.getItems().removeIf(room -> room.getNumber().equals(selectedRoom)); // Убираем забронированную комнату из таблицы
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Ошибка", "Произошла ошибка при бронировании.");
            }
        });

        VBox bookingVBox = new VBox(10, bookingTableView, roomComboBox, arrivalDatePicker, departureDatePicker, bookButton);
        bookingVBox.setPadding(new Insets(10));
        bookingVBox.getStyleClass().add("root"); // Фон из CSS

        Scene bookingScene = new Scene(bookingVBox, 1024, 768);
        bookingScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setScene(bookingScene);
        primaryStage.show();
    }




    private boolean validateCustomerLogin(String login, String password) {
        try (Connection conn = connectDatabase();
             PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM logins_info WHERE login = ? AND password = ?")) {
            pstmt.setString(1, login);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void showCreateUserWindow() {
        Stage createUserStage = new Stage();
        createUserStage.setTitle("Создать пользователя");

        VBox createUserVBox = new VBox(10);
        createUserVBox.setPadding(new Insets(10));
        createUserVBox.getStyleClass().add("root"); // Фон из CSS

        TextField fioField = new TextField();
        fioField.setPromptText("ФИО");
        fioField.getStyleClass().add("text-field");

        TextField numberField = new TextField();
        numberField.setPromptText("Номер телефона");
        numberField.getStyleClass().add("text-field");

        TextField loginField = new TextField();
        loginField.setPromptText("Логин");
        loginField.getStyleClass().add("text-field");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Пароль");
        passwordField.getStyleClass().add("text-field");

        Button createButton = new Button("Создать");
        createButton.getStyleClass().add("button");
        createButton.setOnAction(e -> {
            createUser(fioField.getText(), numberField.getText(), loginField.getText(), passwordField.getText());
            createUserStage.close();
            showAlert("Успех", "Пользователь успешно создан!");
        });

        createUserVBox.getChildren().addAll(fioField, numberField, loginField, passwordField, createButton);

        Scene createUserScene = new Scene(createUserVBox, 300, 250);
        createUserScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        createUserStage.setScene(createUserScene);
        createUserStage.show();
    }


    private void createUser(String fio, String number, String login, String password) {
        try (Connection conn = connectDatabase();
             PreparedStatement pstmt = conn.prepareStatement("""
             INSERT INTO logins_info (login, password, number, FIO, room_number, arrival_date, departure_date, total_amount)
             VALUES (?, ?, ?, ?, 'не указано', '2024-01-01', '2024-01-01', 'не указано')
         """)) {
            pstmt.setString(1, login);
            pstmt.setString(2, password);
            pstmt.setString(3, number);
            pstmt.setString(4, fio);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void showMainWindow(Stage primaryStage) {
        // Вкладка "Add rooms"
        idField = new TextField();
        numberField = new TextField();
        priceField = new TextField();
        minStayField = new TextField();
        maxStayField = new TextField();

        capacityComboBox = new ComboBox<>();
        capacityComboBox.getItems().addAll("Одноместный номер", "Двухместный номер", "Трехместный номер", "Семейный номер", "Апартаменты");

        roomTypeComboBox = new ComboBox<>();
        roomTypeComboBox.getItems().addAll("Стандарт", "Полулюкс", "Люкс");
        roomTypeComboBox.setOnAction(e -> {
            String selectedType = roomTypeComboBox.getValue();
            boolean isLuxury = "Люкс".equals(selectedType);
            minStayField.setDisable(!isLuxury);
            maxStayField.setDisable(!isLuxury);
        });

        // Поля для новой информации
        ComboBox<String> roomStatusComboBox = new ComboBox<>();
        roomStatusComboBox.getItems().addAll("Свободно", "Занято");
        roomStatusComboBox.setPromptText("Выберите статус");
        roomStatusComboBox.setOnAction(e -> {
            String selectedStatus = roomStatusComboBox.getValue();
            if ("Свободно".equals(selectedStatus)) {
                arrivalDatePicker.setValue(LocalDate.of(2024, 1, 1));
                departureDatePicker.setValue(LocalDate.of(2024, 1, 1));
                arrivalDatePicker.setDisable(true);
                departureDatePicker.setDisable(true);
            } else {
                arrivalDatePicker.setDisable(false);
                departureDatePicker.setDisable(false);
            }
        });


        arrivalDatePicker = new DatePicker();
        arrivalDatePicker.setPromptText("Дата приезда");

        departureDatePicker = new DatePicker();
        departureDatePicker.setPromptText("Дата отъезда");

        // Обновление кнопки "Add Room"
        Button addRoomButton = new Button("Add Room");
        addRoomButton.setOnAction(e -> {
            try {
                String id = idField.getText();
                int number = Integer.parseInt(numberField.getText());
                if (number < 1 || number > 1000) {
                    throw new NumberFormatException("Введите номер комнаты от 1 до 1000");
                }
                double price = Double.parseDouble(priceField.getText());
                if (price < 1 || price > 1000000) {
                    throw new NumberFormatException("Введите цену от 1 до 1000000");
                }
                String capacity = capacityComboBox.getValue();
                String roomType = roomTypeComboBox.getValue();
                String roomStatus = roomStatusComboBox.getValue();
                LocalDate arrivalDate = arrivalDatePicker.getValue();
                LocalDate departureDate = departureDatePicker.getValue();

                Integer minStay = "Люкс".equals(roomType) ? Integer.parseInt(minStayField.getText()) : null;
                Integer maxStay = "Люкс".equals(roomType) ? Integer.parseInt(maxStayField.getText()) : null;

                addRoomToDatabase(id, String.valueOf(number), capacity, price, roomType, minStay, maxStay, roomStatus, arrivalDate, departureDate);
                loadRoomsFromDatabase();
            } catch (NumberFormatException ex) {
                showAlert("Ошибка ввода", ex.getMessage());
            }
        });

        GridPane inputGrid = new GridPane();
        inputGrid.setPadding(new Insets(10));
        inputGrid.setHgap(10);
        inputGrid.setVgap(10);

        inputGrid.add(new Label("ID комнаты:"), 0, 0);
        inputGrid.add(idField, 1, 0);
        inputGrid.add(new Label("Номер комнаты:"), 0, 1);
        inputGrid.add(numberField, 1, 1);
        inputGrid.add(new Label("Вместимость номера:"), 0, 2);
        inputGrid.add(capacityComboBox, 1, 2);
        inputGrid.add(new Label("Цена номера:"), 0, 3);
        inputGrid.add(priceField, 1, 3);
        inputGrid.add(new Label("Тип номера:"), 0, 4);
        inputGrid.add(roomTypeComboBox, 1, 4);
        inputGrid.add(new Label("Мин. прибывание:"), 0, 5);
        inputGrid.add(minStayField, 1, 5);
        inputGrid.add(new Label("Макс. прибывание:"), 0, 6);
        inputGrid.add(maxStayField, 1, 6);
        inputGrid.add(new Label("Статус номера:"), 0, 7);
        inputGrid.add(roomStatusComboBox, 1, 7);
        inputGrid.add(new Label("Дата приезда:"), 0, 8);
        inputGrid.add(arrivalDatePicker, 1, 8);
        inputGrid.add(new Label("Дата отъезда:"), 0, 9);
        inputGrid.add(departureDatePicker, 1, 9);

        minStayField.setDisable(true);
        maxStayField.setDisable(true);

        VBox addRoomVBox = new VBox(10, inputGrid, addRoomButton);
        addRoomVBox.setPadding(new Insets(10));

        // Вкладка "View rooms"
        roomTableView = new TableView<>();
        TableColumn<Room, String> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        TableColumn<Room, String> numberColumn = new TableColumn<>("Номер");
        numberColumn.setCellValueFactory(cellData -> cellData.getValue().numberProperty());
        TableColumn<Room, String> capacityColumn = new TableColumn<>("Вместимость");
        capacityColumn.setCellValueFactory(cellData -> cellData.getValue().capacityProperty());
        TableColumn<Room, Double> priceColumn = new TableColumn<>("Цена");
        priceColumn.setCellValueFactory(cellData -> cellData.getValue().priceProperty().asObject());
        TableColumn<Room, String> typeColumn = new TableColumn<>("Тип номера");
        typeColumn.setCellValueFactory(cellData -> cellData.getValue().roomTypeProperty());
        TableColumn<Room, Integer> minStayColumn = new TableColumn<>("Мин. прибывание");
        minStayColumn.setCellValueFactory(cellData -> cellData.getValue().minStayProperty().asObject());
        TableColumn<Room, Integer> maxStayColumn = new TableColumn<>("Макс. прибывание");
        maxStayColumn.setCellValueFactory(cellData -> cellData.getValue().maxStayProperty().asObject());


        // Новые столбцы
        TableColumn<Room, String> statusColumn = new TableColumn<>("Статус");
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().roomStatusProperty());

        TableColumn<Room, LocalDate> arrivalDateColumn = new TableColumn<>("Дата приезда");
        arrivalDateColumn.setCellValueFactory(cellData -> cellData.getValue().arrivalDateProperty());

        TableColumn<Room, LocalDate> departureDateColumn = new TableColumn<>("Дата отъезда");
        departureDateColumn.setCellValueFactory(cellData -> cellData.getValue().departureDateProperty());

        // Установка ширины новых столбцов
        statusColumn.setPrefWidth(0.1 * 1024); // 10% ширины
        arrivalDateColumn.setPrefWidth(0.1 * 1024); // 15% ширины
        departureDateColumn.setPrefWidth(0.1 * 1024); // 15% ширины

        idColumn.setPrefWidth(0.1 * 1024);       // 10% от ширины окна
        numberColumn.setPrefWidth(0.1 * 1024);   // 10% от ширины окна
        capacityColumn.setPrefWidth(0.1 * 1024); // 20% от ширины окна
        priceColumn.setPrefWidth(0.1 * 1024);   // 15% от ширины окна
        typeColumn.setPrefWidth(0.1 * 1024);     // 20% от ширины окна
        minStayColumn.setPrefWidth(0.1 * 1024);// 12.5% от ширины окна
        maxStayColumn.setPrefWidth(0.1 * 1024);// 12.5% от ширины окна

        roomTableView.getColumns().addAll(idColumn, numberColumn, capacityColumn, priceColumn, typeColumn, minStayColumn, maxStayColumn, statusColumn, arrivalDateColumn, departureDateColumn);
        roomTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        roomTableView.setMaxHeight(Double.MAX_VALUE);

        Button loadRoomsButton = new Button("Load Rooms");
        loadRoomsButton.setOnAction(e -> loadRoomsFromDatabase());

        Button deleteRoomButton = new Button("Удалить");
        deleteRoomButton.setOnAction(e -> {
            Room selectedRoom = roomTableView.getSelectionModel().getSelectedItem();
            if (selectedRoom != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Подтверждение удаления");
                alert.setHeaderText(null);
                alert.setContentText("Вы хотите удалить строку с ID " + selectedRoom.getId() + "?");

                ButtonType yesButton = new ButtonType("Да");
                ButtonType noButton = new ButtonType("Нет", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(yesButton, noButton);

                alert.showAndWait().ifPresent(type -> {
                    if (type == yesButton) {
                        deleteRoomFromDatabase(selectedRoom.getId());
                        loadRoomsFromDatabase();
                    }
                });
            } else {
                showAlert("Ошибка", "Пожалуйста, выберите строку для удаления.");
            }
        });

        searchField = new TextField();
        searchField.setPromptText("Введите ID для поиска");

        Button searchRoomButton = new Button("Найти");
        searchRoomButton.setOnAction(e -> {
            String searchId = searchField.getText();
            if (!searchId.isEmpty()) {
                for (Room room : roomTableView.getItems()) {
                    if (room.getId().equals(searchId)) {
                        roomTableView.getSelectionModel().select(room);
                        roomTableView.scrollTo(room);
                        break;
                    }
                }
            } else {
                showAlert("Ошибка", "Пожалуйста, введите ID для поиска.");
            }
        });

        VBox viewRoomsVBox = new VBox(10, loadRoomsButton, new VBox(10, searchField, searchRoomButton), roomTableView, deleteRoomButton);
        viewRoomsVBox.setPadding(new Insets(10));
        VBox.setVgrow(roomTableView, javafx.scene.layout.Priority.ALWAYS); // Устанавливаем заполнение по высоте

        // Создаем вкладки
        TabPane tabPane = new TabPane();
        Tab addRoomTab = new Tab("Добавить Номер", addRoomVBox);
        Tab viewRoomsTab = new Tab("Просмотр Номеров", viewRoomsVBox);
        addRoomTab.setClosable(false);
        viewRoomsTab.setClosable(false);
        tabPane.getTabs().addAll(addRoomTab, viewRoomsTab);

        // Создаем корневой элемент с фоновым изображением
        StackPane root = new StackPane(tabPane);
        root.getStyleClass().add("root");

        // Создаем сцену и подключаем CSS
        Scene scene = new Scene(root, 1024, 768); // Увеличиваем размер окна для Full HD
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static class Room {
        private final SimpleStringProperty id;
        private final SimpleStringProperty number;
        private final SimpleStringProperty capacity;
        private final SimpleDoubleProperty price;
        private final SimpleStringProperty roomType;
        private final SimpleIntegerProperty minStay;
        private final SimpleIntegerProperty maxStay;
        private final SimpleStringProperty roomStatus;
        private final SimpleObjectProperty<LocalDate> arrivalDate;
        private final SimpleObjectProperty<LocalDate> departureDate;

        public Room(String id, String number, String capacity, double price, String roomType, Integer minStay,
                    Integer maxStay, String roomStatus, LocalDate arrivalDate, LocalDate departureDate) {
            this.id = new SimpleStringProperty(id);
            this.number = new SimpleStringProperty(number);
            this.capacity = new SimpleStringProperty(capacity);
            this.price = new SimpleDoubleProperty(price);
            this.roomType = new SimpleStringProperty(roomType);
            this.minStay = new SimpleIntegerProperty(minStay != null ? minStay : 0);
            this.maxStay = new SimpleIntegerProperty(maxStay != null ? maxStay : 0);
            this.roomStatus = new SimpleStringProperty(roomStatus);
            this.arrivalDate = new SimpleObjectProperty<>(arrivalDate);
            this.departureDate = new SimpleObjectProperty<>(departureDate);
        }

        public String getId() {
            return id.get();
        }

        public SimpleStringProperty idProperty() {
            return id;
        }

        public String getNumber() {
            return number.get();
        }

        public SimpleStringProperty numberProperty() {
            return number;
        }

        public String getCapacity() {
            return capacity.get();
        }

        public SimpleStringProperty capacityProperty() {
            return capacity;
        }

        public double getPrice() {
            return price.get();
        }

        public SimpleDoubleProperty priceProperty() {
            return price;
        }

        public String getRoomType() {
            return roomType.get();
        }

        public SimpleStringProperty roomTypeProperty() {
            return roomType;
        }

        public int getMinStay() {
            return minStay.get();
        }

        public SimpleIntegerProperty minStayProperty() {
            return minStay;
        }

        public int getMaxStay() {
            return maxStay.get();
        }

        public SimpleIntegerProperty maxStayProperty() {
            return maxStay;
        }


        public String getRoomStatus() {
            return roomStatus.get();
        }

        public SimpleStringProperty roomStatusProperty() {
            return roomStatus;
        }

        public LocalDate getArrivalDate() {
            return arrivalDate.get();
        }

        public SimpleObjectProperty<LocalDate> arrivalDateProperty() {
            return arrivalDate;
        }

        public LocalDate getDepartureDate() {
            return departureDate.get();
        }

        public SimpleObjectProperty<LocalDate> departureDateProperty() {
            return departureDate;
        }
    }
}