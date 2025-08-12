module net.betrayd.webspeak.testapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires net.betrayd.webspeak;
    requires org.slf4j;
    requires org.jetbrains.annotations;
    requires static lombok;


    opens net.betrayd.webspeak.testapp to javafx.fxml;
    exports net.betrayd.webspeak.testapp;
}