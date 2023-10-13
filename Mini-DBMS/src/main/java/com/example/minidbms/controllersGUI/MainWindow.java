package com.example.minidbms.controllersGUI;

import com.example.minidbms.domain.DBMS;
import com.example.minidbms.domain.Database;
import com.example.minidbms.domain.Table;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.minidbms.utils.Utils.saveDBMSToXML;

public class MainWindow {
    @FXML
    private Button closeButton, runSqlButton;
    @FXML
    private Label databaseUsedLbl;
    @FXML
    private TextArea sqlStatementTextArea, resultTextArea;
    private DBMS myDBMS;
    private Database crtDatabase;

    @FXML
    public void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void runSqlStatement() {
        ProcessSqlStatement();
    }

    public void ProcessSqlStatement() {
        if (ProcessUseDatabase()) {
            return;
        }
        if (ProcessCreateDatabase()) {
            return;
        }
        if (ProcessDropDatabase()) {
            return;
        }
        if (ProcessDropTable()) {
            return;
        }
        else {
            resultTextArea.setText("SQL Statement unknown!");
        }
    }

    public void SetDatabases(DBMS myDBMS) {
        this.myDBMS = myDBMS;
    }

    public boolean ProcessUseDatabase() {
        String useDatabasePatter = "(use) [a-zA-Z_$][a-zA-Z_$0-9]*;";
        String databaseName = "";

        Pattern pattern = Pattern.compile(useDatabasePatter);
        Matcher matcher = pattern.matcher(sqlStatementTextArea.getText().toLowerCase());

        if (!matcher.matches()) {
            return false;
        }

        databaseName = sqlStatementTextArea.getText().substring(4, sqlStatementTextArea.getText().length() -1);

        List<Database> databaseList = myDBMS.listDatabases();
        if (!databaseList.stream().map(database -> database.getDatabaseName().toLowerCase()).toList().contains(databaseName.toLowerCase())) {
            resultTextArea.setText("This database name do not exist. Try again!");
            return true;
        }

        resultTextArea.setText("Using " + databaseName + " database!");
        crtDatabase = myDBMS.getDatabaseByName(databaseName);
        databaseUsedLbl.setText(databaseName);
        return true;
    }

    public boolean ProcessCreateDatabase() {
        String createDatabasePatter = "(create database) [a-zA-Z_$][a-zA-Z_$0-9]*;";
        String databaseName = "";

        Pattern pattern = Pattern.compile(createDatabasePatter);
        Matcher matcher = pattern.matcher(sqlStatementTextArea.getText().toLowerCase());

        if (!matcher.matches()) {
            return false;
        }

        databaseName = sqlStatementTextArea.getText().substring(16, sqlStatementTextArea.getText().length() -1);

        List<Database> databaseList = myDBMS.listDatabases();
        if (databaseList.stream().map(database -> database.getDatabaseName().toLowerCase()).toList().contains(databaseName.toLowerCase())) {
            resultTextArea.setText("This database name already exist. Try again!");
            return true;
        }

        CreateDatabase(databaseName);
        resultTextArea.setText("Database " + databaseName + " was created!");
        crtDatabase = myDBMS.getDatabaseByName(databaseName);
        databaseUsedLbl.setText(databaseName);
        return true;
    }

    public boolean ProcessDropDatabase() {
        String dropDatabasePatter = "(drop database) [a-zA-Z_$][a-zA-Z_$0-9]*;";
        String databaseName = "";

        Pattern pattern = Pattern.compile(dropDatabasePatter);
        Matcher matcher = pattern.matcher(sqlStatementTextArea.getText().toLowerCase());

        if (!matcher.matches()) {
            return false;
        }

        databaseName = sqlStatementTextArea.getText().substring(14, sqlStatementTextArea.getText().length() -1);

        List<Database> databaseList = myDBMS.listDatabases();
        if (!databaseList.stream().map(database -> database.getDatabaseName().toLowerCase()).toList().contains(databaseName.toLowerCase())) {
            resultTextArea.setText("This database name do not exist. Try again!");
            return true;
        }

        DropDatabase(databaseName);
        resultTextArea.setText("Database " + databaseName + " was dropped!");
        crtDatabase = null;
        databaseUsedLbl.setText("");
        return true;
    }

    public void CreateDatabase(String databaseName) {
        myDBMS.createDatabase(databaseName);
        saveDBMSToXML(myDBMS);
    }

    public void DropDatabase(String databaseName) {
        myDBMS.dropDatabase(databaseName);
        saveDBMSToXML(myDBMS);
    }

    public boolean ProcessDropTable() {
        String dropTablePatter = "(drop table) [a-zA-Z_$][a-zA-Z_$0-9]*;";
        String tableName = "";

        Pattern pattern = Pattern.compile(dropTablePatter);
        Matcher matcher = pattern.matcher(sqlStatementTextArea.getText().toLowerCase());

        if (!matcher.matches()) {
            return false;
        }

        if (crtDatabase == null) {
            resultTextArea.setText("Please select a database to use first!");
            return true;
        }

        tableName = sqlStatementTextArea.getText().substring(11, sqlStatementTextArea.getText().length() -1);

        List<Table> tableList =  crtDatabase.getTables();
        if (!tableList.stream().map(table -> table.getTableName().toLowerCase()).toList().contains(tableName.toLowerCase())) {
            resultTextArea.setText("Table name " + tableName +" do not exist in " + crtDatabase.getDatabaseName() +" database. Try again!");
            return true;
        }

        DropTable(tableName);
        resultTextArea.setText("Table " + tableName + " was dropped!");
        return true;
    }

    public void DropTable(String tableName) {
        crtDatabase.dropTable(tableName);
        saveDBMSToXML(myDBMS);
    }
}
