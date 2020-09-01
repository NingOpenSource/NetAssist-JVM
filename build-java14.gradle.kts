plugins {
    if (RuntimeUtlis.isSupportModuleVM) {
        id("org.openjfx.javafxplugin") version "0.0.9"
    }
}
if (RuntimeUtlis.isSupportModuleVM) {
    javafx {
        version = "14"
        modules("javafx.controls", "javafx.fxml", "javafx.web", "javafx.media", "javafx.swing")
        configuration = "api"
    }
}
