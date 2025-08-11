module net.betrayd.webspeak.testapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens net.betrayd.webspeak.testapp to javafx.fxml;
    exports net.betrayd.webspeak.testapp;
}