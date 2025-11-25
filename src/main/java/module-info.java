module com.mycompany.bankms {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.mycompany.bankms to javafx.fxml;
    exports com.mycompany.bankms;
}
