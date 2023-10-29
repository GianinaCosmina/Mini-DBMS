package com.example.minidbms.controllersGUI;

import com.example.minidbms.domain.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.util.*;
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
    private MongoClient mongoClient;

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
        if(ProcessCreateTable()) {
            return;
        }
        if (ProcessDropTable()) {
            return;
        }
        if (ProcessCreateIndex()) {
            return;
        }
        if (ProcessDropIndex()) {
            return;
        }
        else {
            resultTextArea.setText("SQL Statement unknown!");
        }
    }

    public void SetDatabases(DBMS myDBMS) {
        this.myDBMS = myDBMS;
    }

    public  void SetMongoClient(MongoClient mongoClient){
        this.mongoClient = mongoClient;
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
        mongoClient.getDatabase(databaseName);
    }

    public void DropDatabase(String databaseName) {
        myDBMS.dropDatabase(databaseName);
        saveDBMSToXML(myDBMS);
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        database.drop();
    }

    public boolean ProcessDropTable() {
        String dropTablePatter = "(drop table) [a-zA-Z_$][a-zA-Z_$0-9]*;";
        String tableName;

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
        //Connecting to the database
        MongoDatabase database = mongoClient.getDatabase(crtDatabase.getDatabaseName());
        // drop table
        database.getCollection(tableName).drop();
        resultTextArea.setText("Table " + tableName + " was dropped!");
        return true;
    }

    public void DropTable(String tableName) {
        crtDatabase.dropTable(tableName);
        saveDBMSToXML(myDBMS);
    }

    public boolean ProcessCreateTable() {
        String sql = sqlStatementTextArea.getText().toLowerCase();

        String createTablePattern = "create table [a-zA-Z_$][a-zA-Z_$0-9]*\\s*\\([^;]+?\\);";
        String tableName = "";
        List<Column> columns = new ArrayList<>();
        List<PrimaryKey> primaryKeys = new ArrayList<>();
        List<ForeignKey> foreignKeys = new ArrayList<>();
        List<Index> indexes = new ArrayList<>();
        Pattern pattern = Pattern.compile(createTablePattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(sql);

        if (matcher.find()) {
            String tableDefinition = matcher.group();

            Pattern tableNamePattern = Pattern.compile("create table ([a-zA-Z_$][a-zA-Z_$0-9]*)", Pattern.CASE_INSENSITIVE);
            Matcher tableNameMatcher = tableNamePattern.matcher(tableDefinition);

            if (crtDatabase == null) {
                resultTextArea.setText("Please select a database to use first!");
                return true;
            }

            if (tableNameMatcher.find()) {
                tableName = tableNameMatcher.group(1);
            }

            List<Table> tableList =  crtDatabase.getTables();
            if (tableList.stream().map(table -> table.getTableName().toLowerCase()).toList().contains(tableName.toLowerCase())) {
                resultTextArea.setText("Table name " + tableName +" already exist in " + crtDatabase.getDatabaseName() +" database. Try again!");
                return true;
            }

            Pattern columnPattern = Pattern.compile("\\(((.|\\n)*)(\\);)", Pattern.DOTALL);
            Matcher columnMatcher = columnPattern.matcher(tableDefinition);

            if (columnMatcher.find()) {
                String columnsDefinition = columnMatcher.group(1);
                String[] attributeDefinitions = columnsDefinition.split(",");

                for (String attributeDefinition : attributeDefinitions) {
                    String[] parts = attributeDefinition.trim().split("\\s+");

                    if (parts.length < 2) {
                        resultTextArea.setText("Invalid attribute definition: " + attributeDefinition);
                        return true;
                    }

                    String attributeName = parts[0];
                    String attributeType = parts[1];
                    List<String> attributeTypeElems = new ArrayList<>();
                    Integer length = null;
                    boolean hasLength;
                    if ( attributeType.contains("(")){
                        length = Integer.parseInt(attributeType.split("\\(")[1].replaceAll("\\)",""));
                        attributeType = attributeType.split("\\(")[0];
                    }

                    if (!ValidateDataType(attributeType)) {
                        resultTextArea.setText("Invalid dataType: " + attributeType);
                        return true;
                    }

                    if (attributeDefinition.toLowerCase().contains("primary key")) {
                        primaryKeys.add(new PrimaryKey(attributeName));
                    }

                    if (attributeDefinition.toLowerCase().contains("references")) {
                        attributeDefinition = attributeDefinition.replace("\n", "");
                        String[] fkParts = attributeDefinition.trim().split("\\s+");

                        if (fkParts.length >= 5) {
                            String referencedTable = fkParts[3];
                            String referencedAttribute = fkParts[4].replaceAll("\\)", "");


                            if (!crtDatabase.getTables().stream().map(table -> table.getTableName().toLowerCase()).toList().contains(referencedTable.toLowerCase())) {
                                resultTextArea.setText("Table name " + referencedTable +" do not exist in " + crtDatabase.getDatabaseName() +" database. Try again!");
                                return true;
                            }

                            Table crtTable = crtDatabase.getTableByName(referencedTable);
                            List<Column> tableColumns = crtTable.getColumns();
                            if (!tableColumns.stream().map(column -> column.getColumnName().toLowerCase()).toList().contains(referencedAttribute.replaceAll("\\(", "").toLowerCase(Locale.ROOT))){
                                resultTextArea.setText("Index name " + referencedAttribute.replaceAll("\\(", "") + " do not exist in " + referencedTable + " table.Try again!");
                                return true;
                            }
                            foreignKeys.add(new ForeignKey(attributeName, referencedTable, referencedAttribute.replaceAll("\\(", "")));
                        } else {
                            resultTextArea.setText("Invalid foreign key definition: " + attributeDefinition);
                            return true;
                        }
                    }

                    if (attributeDefinition.toLowerCase().contains("create index")) {
                        String[] indexParts = attributeDefinition.split(" ");
                        if (indexParts.length >= 7) {
                            String indexName = indexParts[3];
                            String indexTableName = indexParts[5];
                            String columnsList = attributeDefinition.substring(attributeDefinition.indexOf("(") + 1, attributeDefinition.lastIndexOf(")"));
                            String[] indexedColumns = columnsList.split(",\\s*");

                            indexes.add(new Index(indexName, indexTableName, Arrays.asList(indexedColumns)));
                        } else {
                            resultTextArea.setText("Invalid index definition: " + attributeDefinition);
                            return true;
                        }
                    }

                    Column column = new Column();
                    column.setColumnName(attributeName);
                    column.setType(attributeType);
                    if (length != null){
                        column.setLength(length);
                    }
                    columns.add(column);
                }
            }

            Table newTable = new Table(tableName, columns, primaryKeys, foreignKeys);
            newTable.setIndexes(indexes);
            crtDatabase.createTable(newTable);
            saveDBMSToXML(myDBMS);
            //Connecting to the database
            MongoDatabase database = mongoClient.getDatabase(crtDatabase.getDatabaseName());
            //Creating a collection
            database.createCollection(tableName);
            resultTextArea.setText("Table " + tableName + " created successfully!");
            return true;
        } else {
            return false;
        }
    }



    public boolean ProcessCreateIndex() {
        String createIndexPattern = "create index [a-zA-Z_$][a-zA-Z_$0-9]* on [a-zA-Z_$][a-zA-Z_$0-9]* \\([^)]+\\);";
        String indexName = "";
        String tableName = "";
        List<String> columnList = new ArrayList<>();

        Pattern pattern = Pattern.compile(createIndexPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sqlStatementTextArea.getText());


        if (!matcher.matches()) {
            return false;
        }

        if (crtDatabase == null) {
            resultTextArea.setText("Please select a database to use first!");
            return true;
        }

        String[] parts = sqlStatementTextArea.getText().split(" ");
        indexName = parts[2];
        tableName = parts[4];
        String columnListStr = sqlStatementTextArea.getText().substring(sqlStatementTextArea.getText().indexOf("(") + 1, sqlStatementTextArea.getText().indexOf(")"));
        String[] columnNames = columnListStr.split(",\\s*");
        for (String columnName : columnNames) {
            columnList.add(columnName.trim());
        }

        List<Table> tableList =  crtDatabase.getTables();
        if (!tableList.stream().map(table -> table.getTableName().toLowerCase()).toList().contains(tableName.toLowerCase())) {
            resultTextArea.setText("Table name " + tableName +" do not exist in " + crtDatabase.getDatabaseName() +" database. Try again!");
            return true;
        }

        Index index = new Index(indexName, tableName, columnList);
        Table table = crtDatabase.getTableByName(tableName);
        table.createIndex(index);

        saveDBMSToXML(myDBMS);
        resultTextArea.setText("Index " + indexName + " created on table " + tableName + " with columns: " + columnList);
        return true;
    }

    public boolean ProcessDropIndex() {
        String dropIndexPatter = "drop index [a-zA-Z_$][a-zA-Z_$0-9]* on [a-zA-Z_$][a-zA-Z_$0-9]*;";
        String indexName = "";
        String tableName= "";

        Pattern pattern = Pattern.compile(dropIndexPatter);
        Matcher matcher = pattern.matcher(sqlStatementTextArea.getText().toLowerCase());

        if (!matcher.matches()) {
            return false;
        }

        if (crtDatabase == null) {
            resultTextArea.setText("Please select a database to use first!");
            return true;
        }

        String[] parts = sqlStatementTextArea.getText().split(" ");
        indexName = parts[2];
        tableName = parts[4].replace(";","");



        List<Table> tableList =  crtDatabase.getTables();
        if (!tableList.stream().map(table -> table.getTableName().toLowerCase()).toList().contains(tableName.toLowerCase())) {
            resultTextArea.setText("Table name " + tableName +" do not exist in " + crtDatabase.getDatabaseName() +" database. Try again!");
            return true;
        }



        Table crtTable = crtDatabase.getTableByName(tableName);
        List<Index> indexList = crtTable.getIndexes();
        if (!indexList.stream().map(index -> index.getIndexName().toLowerCase()).toList().contains(indexName.toLowerCase(Locale.ROOT))){
            resultTextArea.setText("Index name " + indexName + " do not exist in " + tableName + " table.Try again!");
            return true;
        }



        DropIndex(crtTable,indexName);
        resultTextArea.setText("Index " + indexName + " was dropped!");
        return true;
    }

    public void DropIndex(Table table, String indexName) {
        table.dropIndex(indexName);
        saveDBMSToXML(myDBMS);
    }

    public boolean ValidateDataType(String attributeType){
        List<String> dataTypes = Arrays.asList("int", "varchar","char");
        if (dataTypes.contains(attributeType.toLowerCase(Locale.ROOT))){
            return true;
        }
        return false;
        //if (dataTypes.stream().map(dataType -> {dataType.contains() }))
    }

}
