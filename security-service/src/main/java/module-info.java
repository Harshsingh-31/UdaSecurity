module security.service {
    requires java.desktop;
    requires com.google.gson;
    requires com.google.common;
    requires com.miglayout.swing;
    requires image.service;
    requires java.prefs;
    opens com.udacity.catpoint.security.data to com.google.gson;
}
