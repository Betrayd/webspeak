module webspeak.testapp.main {
    requires static lombok;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires net.betrayd.webspeak;
    requires org.jetbrains.annotations;
    requires org.slf4j;

    exports net.betrayd.webspeak.testapp.ui to javafx.fxml;
    exports net.betrayd.webspeak.testapp.ui.util to javafx.fxml;
}