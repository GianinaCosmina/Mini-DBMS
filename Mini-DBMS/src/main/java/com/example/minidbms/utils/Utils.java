package com.example.minidbms.utils;

import com.example.minidbms.domain.DBMS;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;

/**
 * Defines a class Utils that contains util functions
 */
public class Utils {
    private static double xOffset = 0;
    private static double yOffset = 0;
    private static final String DBMS_XML_FILE = "Catalog.xml";

    public static void makeWindowDraggable(Scene scene, Stage stage) {
        scene.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        scene.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    public static void setScenePosition(Scene scene, Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double x = (bounds.getWidth() - scene.getWidth()) / 2;
        double y = (bounds.getHeight() - scene.getHeight()) / 2;
        stage.setX(x);
        stage.setY(y);
    }

    public static DBMS loadDBMSFromXML() {
        File xmlFile = new File(DBMS_XML_FILE);
        if (xmlFile.exists()) {
            try {
                JAXBContext context = JAXBContext.newInstance(DBMS.class);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                return (DBMS) unmarshaller.unmarshal(xmlFile);
            } catch (JAXBException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void saveDBMSToXML(DBMS dbms) {
        try {
            File xmlFile = new File(DBMS_XML_FILE);
            JAXBContext context = JAXBContext.newInstance(DBMS.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(dbms, xmlFile);
            System.out.println("DBMS structure saved to " + DBMS_XML_FILE);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}