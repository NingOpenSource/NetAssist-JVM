module org.ning1994.net_assist {
    requires transitive javafx.base;
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.web;
    requires transitive javafx.media;
    requires transitive javafx.swing;
    requires transitive javafx.graphics;
    requires transitive kotlin.stdlib;
    requires transitive kotlin.reflect;
    requires transitive io.netty.all;
    requires transitive tornadofx;
//    requires tornadofx;
    opens org.ning1994.net_assist;
    exports org.ning1994.net_assist;
}