module com.example.minidbms {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires java.sql;
    requires java.desktop;

    // for Apache PDF
    requires org.apache.pdfbox;
    // for WordUtils
    requires org.apache.commons.text;
    requires java.xml.bind;

    opens com.example.minidbms.domain to java.xml.bind;

    opens com.example.minidbms to javafx.fxml;
    exports com.example.minidbms;
    exports com.example.minidbms.controllersGUI;
    opens com.example.minidbms.controllersGUI to javafx.fxml;
}