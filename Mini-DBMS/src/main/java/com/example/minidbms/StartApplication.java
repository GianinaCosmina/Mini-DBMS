package com.example.minidbms;

import com.example.minidbms.controllersGUI.MainWindow;
import com.example.minidbms.domain.DBMS;
import com.example.minidbms.utils.Utils;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import static com.example.minidbms.utils.Utils.loadDBMSFromXML;

public class StartApplication extends Application {
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws IOException, ParserConfigurationException, SAXException {
        initView(primaryStage);
        primaryStage.show();
    }

    private void initView(Stage primaryStage) throws IOException, ParserConfigurationException, SAXException {
        FXMLLoader fxmlLoader = new FXMLLoader(StartApplication.class.getResource("main-window-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 850, 550);
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setScene(scene);
        Utils.makeWindowDraggable(scene, primaryStage);
        Utils.setScenePosition(scene, primaryStage);

        // initialize or create databases
        DBMS myDBMS = loadDBMSFromXML();

        MainWindow mainWindow = fxmlLoader.getController();
        mainWindow.SetDatabases(myDBMS);
    }
}