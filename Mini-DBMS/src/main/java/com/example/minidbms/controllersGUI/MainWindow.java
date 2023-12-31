package com.example.minidbms.controllersGUI;

import com.example.minidbms.domain.*;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.bson.Document;
import org.bson.conversions.Bson;

import javax.print.Doc;

import static com.mongodb.client.model.Filters.eq;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        if (ProcessInsertData()) {
            return;
        }
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
        if (ProcessInsertIntoTable(sqlStatementTextArea.getText())){
            return;
        }
        if (ProcessDeleteFromTable()){
            return;
        }
        if (ProcessSelectFromTable()) {
            return;
        }
        if (ProcessInnerJoin()) {
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

        MongoDatabase database = mongoClient.getDatabase(crtDatabase.getDatabaseName());

        Table crtTable = crtDatabase.getTableByName(tableName);
        if (!CheckFKonDeleteCollection(crtTable, database)){
            return true;
        }

        DropTable(tableName);

        database.getCollection(tableName).drop();
        for(Index index : crtTable.getIndexes()) {
            database.getCollection(index.getIndexName()).drop();
        }
        resultTextArea.setText("Table " + tableName + " was dropped!");
        return true;
    }

    public void DropTable(String tableName) {
        crtDatabase.dropTable(tableName);
        saveDBMSToXML(myDBMS);
    }

    public boolean ProcessCreateTable() {
        String sql = sqlStatementTextArea.getText().toLowerCase();

        String createTablePattern = "create table ([a-zA-Z_$][a-zA-Z_$0-9]*)\\s*\\((.*?)\\);";
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

            List<Table> tableList = crtDatabase.getTables();
            if (tableList.stream().map(table -> table.getTableName().toLowerCase()).toList().contains(tableName.toLowerCase())) {
                resultTextArea.setText("Table name " + tableName + " already exists in " + crtDatabase.getDatabaseName() + " database. Try again!");
                return true;
            }

            Pattern columnPattern = Pattern.compile("\\(((.|\\n)*)(\\);)", Pattern.DOTALL);
            Matcher columnMatcher = columnPattern.matcher(tableDefinition);

            // Connecting to the database
            MongoDatabase database = mongoClient.getDatabase(crtDatabase.getDatabaseName());

            if (columnMatcher.find()) {
                String columnsDefinition = columnMatcher.group(1);
                String[] attributeDefinitions = columnsDefinition.split(",\\n");

                for (String attributeDefinition : attributeDefinitions) {
                    String[] parts = attributeDefinition.trim().split("\\s+");

                    if (parts.length < 2) {
                        resultTextArea.setText("Invalid attribute definition: " + attributeDefinition);
                        return true;
                    }

                    String attributeName = parts[0];
                    String attributeType = parts[1];
                    Boolean attributeIsUnique = null;
                    if (Arrays.stream(parts).toList().contains("unique")) {
                        attributeIsUnique = true;
                    }

                    Integer length = null;
                    if (attributeType.contains("(")) {
                        length = Integer.parseInt(attributeType.split("\\(")[1].replaceAll("\\)", ""));
                        attributeType = attributeType.split("\\(")[0];
                    }

                    if (!attributeDefinition.toLowerCase().trim().startsWith("primary key")) {
                        if (!ValidateDataType(attributeType)) {
                            resultTextArea.setText("Invalid dataType: " + attributeType);
                            return true;
                        }
                    }

                    if (attributeDefinition.toLowerCase().contains("primary key")) {
                        if (attributeDefinition.toLowerCase().trim().startsWith("primary key")) {
                            String primaryKeyColumns = attributeDefinition.substring(attributeDefinition.indexOf("(") + 1, attributeDefinition.lastIndexOf(")"));
                            String[] primaryKeyColumnNames = primaryKeyColumns.split(",");

                            List<String> primaryKeyColumnsList = Arrays.stream(primaryKeyColumnNames).map(String::trim).toList();

                            for (String primaryKeyColumn : primaryKeyColumnsList) {
                                primaryKeys.add(new PrimaryKey(primaryKeyColumn));
                            }
                        } else {
                            primaryKeys.add(new PrimaryKey(attributeName));
                        }
                    }

                    if (attributeDefinition.toLowerCase().contains("references")) {
                        attributeDefinition = attributeDefinition.replace("\n", "");
                        String[] fkParts = attributeDefinition.trim().split("\\s+");

                        if (fkParts.length >= 5) {
                            String referencedTable = fkParts[3];
                            String referencedAttribute = fkParts[4].replaceAll("\\)", "");

                            if (!crtDatabase.getTables().stream().map(table -> table.getTableName().toLowerCase()).toList().contains(referencedTable.toLowerCase())) {
                                resultTextArea.setText("Table name " + referencedTable + " does not exist in " + crtDatabase.getDatabaseName() + " database. Try again!");
                                return true;
                            }

                            Table crtTable = crtDatabase.getTableByName(referencedTable);
                            List<Column> tableColumns = crtTable.getColumns();
                            if (!tableColumns.stream().map(column -> column.getColumnName().toLowerCase()).toList().contains(referencedAttribute.replaceAll("\\(", "").toLowerCase(Locale.ROOT))) {
                                resultTextArea.setText("Index name " + referencedAttribute.replaceAll("\\(", "") + " does not exist in " + referencedTable + " table. Try again!");
                                return true;
                            }
                            foreignKeys.add(new ForeignKey(attributeName, referencedTable, referencedAttribute.replaceAll("\\(", "")));
                            if (attributeIsUnique == null) {
                                // Creating a collection
                                database.createCollection(tableName.toLowerCase() + "_" + attributeName.toLowerCase() + "_index");
                                indexes.add(new Index(tableName.toLowerCase() + "_" + attributeName.toLowerCase() + "_index", tableName, Collections.singletonList(attributeName), false));
                            }
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

                            indexes.add(new Index(indexName, indexTableName, Arrays.asList(indexedColumns), false));
                        } else {
                            resultTextArea.setText("Invalid index definition: " + attributeDefinition);
                            return true;
                        }
                    }

                    if (!attributeDefinition.toLowerCase().trim().startsWith("primary key")) {
                        Column column = new Column();
                        column.setColumnName(attributeName);
                        column.setType(attributeType);
                        if (length != null) {
                            column.setLength(length);
                        }
                        if (attributeIsUnique != null) {
                            column.setUnique(true);
                            // Creating a collection
                            database.createCollection(tableName.toLowerCase() + "_" + attributeName.toLowerCase() + "_index");
                            indexes.add(new Index(tableName.toLowerCase() + "_" + attributeName.toLowerCase() + "_index", tableName, Collections.singletonList(attributeName), true));
                        }
                        columns.add(column);
                    }
                }
            }

            Table newTable = new Table(tableName, columns, primaryKeys, foreignKeys);
            newTable.setIndexes(indexes);
            crtDatabase.createTable(newTable);
            saveDBMSToXML(myDBMS);
            // Creating a collection
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

        Index index = new Index(indexName, tableName, columnList, false);
        Table table = crtDatabase.getTableByName(tableName);
        table.createIndex(index);

        saveDBMSToXML(myDBMS);
        MongoDatabase database = mongoClient.getDatabase(crtDatabase.getDatabaseName());
        database.createCollection(indexName);
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
    }

    public boolean ProcessInsertIntoTable(String SqlStatement) {
        String insertPattern = "(insert into) (\\S+).*\\((.*?)\\).*(values).*\\((.*?)\\)(.*\\;?);";

        String tableName ;
        List <String> columnNames;
        List <String> columnValues;

        Pattern pattern = Pattern.compile(insertPattern);
        Matcher matcher = pattern.matcher(SqlStatement.toLowerCase());

        if (!matcher.matches()) {
            return false;
        }

        if (crtDatabase == null) {
            resultTextArea.setText("Please select a database to use first!");
            return false;
        }

        tableName = matcher.group(2).trim();
        columnNames = Arrays.stream(matcher.group(3).split(",")).map(String::trim).toList();
        columnValues = Arrays.stream(matcher.group(5).split(",")).map(String::trim).toList();

        List<Table> tableList = crtDatabase.getTables();
        if (tableList.stream().noneMatch(table -> table.getTableName().equalsIgnoreCase(tableName))) {
            resultTextArea.setText("Table " + tableName + " does not exist in the " + crtDatabase.getDatabaseName() + " database. Please try again.");
            return false;
        }

        Table crtTable = crtDatabase.getTableByName(tableName);
        for (String column : columnNames) {
            if ( crtTable.getColumnByName(column) == null ){
                resultTextArea.setText("Column " + column + " does not exist in the " + crtTable.getTableName() + " table. Please try again.");
                return false;
            }
        }

        String primaryKeys = null;
        String values = null;

        for (int i=0; i<columnNames.size();i++){
            if ( crtTable.getPrimaryKeys().stream().map(primaryKey -> primaryKey.getPkAttribute().toLowerCase(Locale.ROOT)).toList().contains(columnNames.get(i).toLowerCase()) ){
                if (primaryKeys == null){
                    primaryKeys = columnValues.get(i);
                }else{
                    primaryKeys = primaryKeys + "#" + columnValues.get(i);
                }
            } else{
                if (values == null){
                    values = columnValues.get(i);
                } else{
                    values = values + "#" + columnValues.get(i);
                }
            }
        }

        MongoDatabase database = mongoClient.getDatabase(crtDatabase.getDatabaseName());
        MongoCollection<Document> collection = database.getCollection(tableName);

        if (!isPrimaryKeyValid(crtTable, columnNames, primaryKeys, collection)) {
            return true;
        }

        // check FK
        if (!CheckFKonInsert(crtTable, columnNames, columnValues, database)) {
            return true;
        }

        // insert indexes
        if (!InsertIndexes(crtTable, columnNames, columnValues, primaryKeys, database)) {
            return true;
        }

        try {
            collection.insertOne(new Document().append("_id", primaryKeys).append("values", values));
        } catch (MongoException me) {
            resultTextArea.setText("Unable to insert due to an error: " + me);
        }

        resultTextArea.setText("Data was successfully inserted into the table " + tableName);
        return true;
    }

    private boolean isPrimaryKeyValid(Table table, List <String> columnNames, String primaryKeyString, MongoCollection<Document> collection) {
        List <PrimaryKey> primaryKeys = table.getPrimaryKeys();
        for (PrimaryKey primaryKey: primaryKeys) {
            if (!columnNames.stream().map(String::toLowerCase).toList().contains(primaryKey.getPkAttribute().toLowerCase()) ){
                resultTextArea.setText("Invalid list of columns. List of columns must contains all primary key fields.");
                return false;
            }
        }

        Document doc = collection.find(eq("_id", primaryKeyString)).first();
        if (doc != null){
            resultTextArea.setText("Primary key violation: A record with the same primary key already exists.");
            return false;
        }

        return true;
    }

    public boolean ProcessDeleteFromTable() {
        String deletePattern = "(delete(\\s+)from)(\\s+)(\\S+)(\\s+)(where)(\\s+)(\\S+)(\\s+)(=)(\\s+)(\\S+).*;";

        String tableName;

        Pattern pattern = Pattern.compile(deletePattern);
        Matcher matcher = pattern.matcher(sqlStatementTextArea.getText().toLowerCase());

        if (!matcher.matches()) {
            return false;
        }

        if (crtDatabase == null) {
            resultTextArea.setText("Please select a database to use first!");
            return false;
        }

        tableName = matcher.group(4).trim();
        String columnPK = matcher.group(8).trim();
        String columnValue = matcher.group(12).trim();

        List<Table> tableList = crtDatabase.getTables();
        if (tableList.stream().noneMatch(table -> table.getTableName().equalsIgnoreCase(tableName))) {
            resultTextArea.setText("Table " + tableName + " does not exist in the " + crtDatabase.getDatabaseName() + " database. Please try again.");
            return false;
        }

        Table crtTable = crtDatabase.getTableByName(tableName);

        if (crtTable.getColumns().stream().noneMatch(column -> column.getColumnName().equalsIgnoreCase(columnPK))) {
            resultTextArea.setText("Column " + columnPK + " does not exist in the " + crtTable.getTableName() + " table. Please try again.");
            return false;
        }

        MongoDatabase database = mongoClient.getDatabase(crtDatabase.getDatabaseName());
        MongoCollection<Document> collection = database.getCollection(tableName);

        // check FK
        if (!CheckFKonDeleteDocument(crtTable, columnPK, columnValue, database)) {
            return true;
        }

        // delete indexes
        DeleteIndexes(crtTable, columnPK, columnValue, database);

        collection.deleteOne(Filters.eq("_id", columnValue));
        resultTextArea.setText("Data was successfully deleted from the table " + tableName);
        return true;
    }

    private boolean InsertIndexes(Table crtTable, List<String> columnNames, List<String> columnValues, String primaryKeys, MongoDatabase database) {
        for (Index index : crtTable.getIndexes()) {
            String indexID = null;
            for(String column : index.getColumns()) {
                for (int i = 0; i < columnNames.size(); i++) {
                    if (columnNames.get(i).equalsIgnoreCase(column)) {
                        if (indexID != null) {
                            indexID = indexID + "#" + columnValues.get(i);
                        } else {
                            indexID = columnValues.get(i);
                        }
                    }
                }
            }
            MongoCollection<Document> collection = database.getCollection(index.getIndexName());
            Document doc = collection.find(eq("_id", indexID)).first();
            if (doc != null) {
                if (index.isUnique()) {
                    resultTextArea.setText("Unique key violation: A record with the same value already exists.");
                    return false;
                }
                String values = (String) doc.get("values");
                Bson updates = Updates.combine(Updates.set("values", values + "$" + primaryKeys));
                UpdateOptions options = new UpdateOptions().upsert(true);
                collection.updateOne(doc, updates, options);
            } else {
                collection.insertOne(new Document()
                        .append("_id", indexID)
                        .append("values", primaryKeys));
            }
        }

        return true;
    }

    private boolean CheckFKonInsert(Table crtTable, List<String> columnNames, List<String> columnValues, MongoDatabase database) {
        if (crtTable.getForeignKeys().size() == 0) {
            return true;
        }
        boolean found;
        for(ForeignKey foreignKey : crtTable.getForeignKeys()) {
            found = false;
            for (int i = 0; i < columnNames.size(); i++) {
                if (columnNames.get(i).equalsIgnoreCase(foreignKey.getFkAttribute())) {
                    Table refTable = crtDatabase.getTableByName(foreignKey.getRefTable());
                    MongoCollection<Document> collection = database.getCollection(refTable.getTableName());
                    Document doc = collection.find(eq("_id", columnValues.get(i))).first();
                    if (doc != null) {
                        found = true;
                    }
                    for (Index index : refTable.getIndexes()) {
                        collection = database.getCollection(index.getIndexName());
                        doc = collection.find(eq("_id", columnValues.get(i))).first();
                        if (doc != null) {
                            found = true;
                        }
                    }
                }
            }
            if (!found) {
                resultTextArea.setText("Foreign Key constraint failure.");
                return false;
            }
        }

        return true;
    }

    private void DeleteIndexes(Table crtTable, String columnName, String columnValue, MongoDatabase database) {
        for (Index index : crtTable.getIndexes()) {
            MongoCollection<Document> collection = database.getCollection(index.getIndexName());
            Document doc = collection.find(eq("values", columnValue)).first();
            if (doc != null) {
                collection.deleteOne(Filters.eq("values", columnValue));
                return;
            }
            doc = collection.find(Filters.regex("values",
                    ".*" + columnValue + ".*")).first();
            if (doc != null) {
                String values = (String) doc.get("values");
                values = values.replaceAll(columnValue, "");
                values = values.replaceAll("^\\$", "");
                values = values.replaceAll("\\$$", "");
                values = values.replaceAll("\\$\\$$", "\\$$");
                Bson updates = Updates.combine(Updates.set("values", values));
                UpdateOptions options = new UpdateOptions().upsert(true);
                collection.updateOne(doc, updates, options);
                return;
            }
        }
    }

    private boolean CheckFKonDeleteDocument(Table crtTable, String columnName, String columnValue, MongoDatabase database) {
        for (Table table : crtDatabase.getTables()) {
            for (ForeignKey foreignKey : table.getForeignKeys()) {
                if (foreignKey.getRefTable().equalsIgnoreCase(crtTable.getTableName())) {
                    if (foreignKey.getRefAttribute().equalsIgnoreCase(columnName)) {
                        for (Index index : table.getIndexes()) {
                            MongoCollection<Document> collection = database.getCollection(index.getIndexName());
                            Document doc = collection.find(eq("_id", columnValue)).first();
                            if (doc != null) {
                                resultTextArea.setText("Foreign Key constraint failure.");
                                return false;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    private boolean CheckFKonDeleteCollection(Table crtTable, MongoDatabase database) {
        for (Table table : crtDatabase.getTables()) {
            for (ForeignKey foreignKey : table.getForeignKeys()) {
                if (foreignKey.getRefTable().equalsIgnoreCase(crtTable.getTableName())) {
                    resultTextArea.setText("Foreign Key constraint failure.");
                    return false;
                }
            }
        }

        return true;
    }

    public boolean ProcessSelectFromTable() {
        String selectPattern = "^\\s*(select)(\\s+)(distinct)?(\\s+)?((?:\\w+\\.\\w+|[\\w\\*]+(?:\\s*,\\s*\\w+\\.\\w+|\\s*,\\s*[\\w\\*]+)*))(\\s+)(from)(\\s+)(\\w+\\s*(?:,\\s*\\w+\\s*)*)(\\s*)(?:(where)(\\s+)((.|\\n)*))?;";

        Pattern pattern = Pattern.compile(selectPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sqlStatementTextArea.getText().toLowerCase());

        if (!matcher.matches()) {
            return false;
        }

        if (crtDatabase == null) {
            resultTextArea.setText("Please select a database to use first!");
            return true;
        }

        // is distinct
        String isDistinctString = matcher.group(3);
        boolean isDistinct = false;
        if (isDistinctString != null) {
            isDistinct = true;
        }

        // tables in which to search
        String tablesInWhichToSearch = matcher.group(9);
        if (tablesInWhichToSearch != null) {
            tablesInWhichToSearch = tablesInWhichToSearch.trim();
        }
        List<String> tablesInWhichToSearchList = Arrays.stream(tablesInWhichToSearch.split(",")).map(s -> s.trim()).toList();

        // columns to show
        String columnsToShow = matcher.group(5);
        if (columnsToShow != null) {
            columnsToShow = columnsToShow.trim();
        }
        List<String> columnsToShowAux = Arrays.stream(columnsToShow.split(",")).map(String::trim).toList();
        List<Pair<String,String>> columnAndTableList = new ArrayList<>();
        for (String column: columnsToShowAux) {
            if (column.contains(".")) {
                List<String> columnAndTable = List.of(column.split("\\."));
                columnAndTableList.add(new Pair<>(columnAndTable.get(0), columnAndTable.get(1)));
            } else {
                columnAndTableList.add(new Pair<>(column.trim(), tablesInWhichToSearchList.get(0)));
            }
        }

        // where clauses
        String whereClause = matcher.group(13);
        if (whereClause != null) {
            whereClause = whereClause.trim();
        }

        for (String tableName: tablesInWhichToSearchList) {
            Table crtTable = crtDatabase.getTableByName(tableName);
            if (crtTable == null) {
                resultTextArea.setText("Table " + tableName + " does not exist in the " + crtDatabase.getDatabaseName() + " database.");
                return true;
            }
        }

        MongoDatabase database = mongoClient.getDatabase(crtDatabase.getDatabaseName());

        List<Document> resultDocuments = null;
        List<Document> resultDocumentsList = null;
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            resultDocumentsList = ExecuteMultipleWhereCondition(whereClause.toLowerCase(), tablesInWhichToSearchList, database);
        } else {
            for (String tableName: tablesInWhichToSearchList) {
                MongoCollection<Document> collection = database.getCollection(tableName);
                resultDocuments = collection.find().into(new ArrayList<>());

                if (resultDocumentsList == null) {
                    resultDocumentsList = resultDocuments;
                } else {
                    resultDocumentsList.addAll(resultDocuments);
                }
            }
        }

        // Display query results
        DisplayQueryResults(resultDocumentsList, columnAndTableList, isDistinct);

        return true;
    }

    private List<Document> ExecuteMultipleWhereCondition(String whereClause, List<String> tablesInWhichToSearchList, MongoDatabase database) {
        List<String> whereClauses = List.of(whereClause.split("and"));
        List<Document> documents;
        List<Document> finalDocuments = new ArrayList<>();
        for(String clause : whereClauses) {
            documents = ExecuteWhereCondition(clause, tablesInWhichToSearchList, database);
            if (finalDocuments.isEmpty()) {
                finalDocuments = documents;
            } else {
                finalDocuments = finalDocuments.stream()
                        .distinct()
                        .filter(documents::contains)
                        .collect(Collectors.toSet()).stream().toList();
            }
        }

        return finalDocuments;
    }

    private List<Document> ExecuteWhereCondition(String whereClause, List<String> tablesInWhichToSearchList, MongoDatabase database) {
        // Implement logic to parse and execute WHERE conditions
        // For simplicity, let's assume a basic condition, e.g., "columnName = 'value'"
        String condition = null;
        if (whereClause.contains("=")) {
            condition = "=";
        }
        if (whereClause.contains("like")) {
            condition = "like";
        }
        if (whereClause.contains("<")) {
            condition = "<";
        }
        if (whereClause.contains(">")) {
            condition = ">";
        }
        if (whereClause.contains("<=")) {
            condition = "<=";
        }
        if (whereClause.contains(">=")) {
            condition = ">=";
        }
        String[] parts = whereClause.split(condition);
        List<Document> resultDocuments = new ArrayList<>();

        if (parts.length == 2) {
            String columnNameMaybeTable = parts[0].trim();
            String value = parts[1].trim().replaceAll("'", "");
            String columnName;
            String tableName;

            if (columnNameMaybeTable.contains(".")) {
                columnName = columnNameMaybeTable.split(".")[0];
                tableName = columnNameMaybeTable.split(".")[1];
            } else {
                columnName = columnNameMaybeTable;
                tableName = tablesInWhichToSearchList.get(0);
            }

            Table crtTable = crtDatabase.getTableByName(tableName);
            List<Document> docs = new ArrayList<>();
            boolean thereIsIndex = false;
            for(Index index : crtTable.getIndexes()) {
                if (index.getColumns().get(0).equalsIgnoreCase(columnName)) {
                    MongoCollection<Document> collection = database.getCollection(tableName + "_" + columnName + "_index");
                    switch (condition) {
                        case "=", "like" -> docs.addAll(collection.find(Filters.eq("_id", value)).into(new ArrayList<>()));
                        case "<" -> docs.addAll(collection.find(Filters.lt("_id", value)).into(new ArrayList<>()));
                        case ">" -> docs.addAll(collection.find(Filters.gt("_id", value)).into(new ArrayList<>()));
                        case "<=" -> docs.addAll(collection.find(Filters.lte("_id", value)).into(new ArrayList<>()));
                        case ">=" -> docs.addAll(collection.find(Filters.gte("_id", value)).into(new ArrayList<>()));
                    }
                    thereIsIndex = true;
                }
            }
            Map<String, String> columnValueMap;
            if (!thereIsIndex) {
                MongoCollection<Document> collection = database.getCollection(tableName);
                for (Document document : collection.find()) {
                    columnValueMap = getColumnValueMap(document, crtTable);
                    if (columnValueMap.get(columnName) != null) {
                        if (columnValueMap.get(columnName).equalsIgnoreCase(value)) {
                            resultDocuments.add(document);
                        }
                    }
                }
            }

            if (thereIsIndex) {
                for (Document doc : docs) {
                    MongoCollection<Document> collection = database.getCollection(tableName);
                    resultDocuments.addAll(collection.find(Filters.eq("_id", doc.get("values"))).into(new ArrayList<>()));
                }
            }
        }

        // Handle more complex conditions as needed

        return resultDocuments;
    }

    private Map<String, String> getColumnValueMap(Document document, Table crtTable) {
        Map<String, String> ColumnValueMap = new HashMap<>();
        int index = 0;
        for (PrimaryKey pk : crtTable.getPrimaryKeys()) {
            ColumnValueMap.put(pk.getPkAttribute(), document.get("_id").toString().split("#")[index]);
            index++;
        }
        index = 0;
        for (Column col : crtTable.getColumns()) {
            if (ColumnValueMap.get(col.getColumnName()) == null) {
                ColumnValueMap.put(col.getColumnName(), document.get("values").toString().split("#")[index]);
                index++;
            }
        }

        return  ColumnValueMap;
    }

    private void DisplayQueryResults(List<Document> resultDocuments, List<Pair<String, String>> selectedColumns, boolean isDistinct) {
        // Implement logic to display query results
        StringBuilder resultStringBuilder = new StringBuilder();
        List<String> result = new ArrayList<>();

        if (resultDocuments == null) {
            resultTextArea.setText("No records found!");
            return;
        }

        for (Document document : resultDocuments) {
            // Display selected columns
            resultStringBuilder = new StringBuilder();
            for (Pair<String, String> column : selectedColumns) {
                Table crtTable = crtDatabase.getTableByName(column.getValue());

                Map<String, String> columnValueMap = getColumnValueMap(document, crtTable);
                if (!column.getKey().trim().equals("*")) {
                    for (Map.Entry<String,String> entry : columnValueMap.entrySet()) {
                        if (Objects.equals(entry.getKey(), column.getKey())) {
                            resultStringBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
                        }
                    }
                } else {
                    for (Map.Entry<String,String> entry : columnValueMap.entrySet()) {
                        resultStringBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
                    }
                }
            }
            resultStringBuilder.setLength(resultStringBuilder.length() - 2); // Remove trailing comma and space
            resultStringBuilder.append("\n");
            result.add(resultStringBuilder.toString());
        }

        if (isDistinct) {
            result = result.stream().distinct().toList();
        }

        resultTextArea.setText(result.toString()
                .replace("[","")
                .replace("]", "")
                .replace(", ", ""));
    }

    public boolean ProcessInsertData() {
        String dropTablePatter = "(insert data)";

        Pattern pattern = Pattern.compile(dropTablePatter);
        Matcher matcher = pattern.matcher(sqlStatementTextArea.getText().toLowerCase());

        if (!matcher.matches()) {
            return false;
        }

        if (crtDatabase == null) {
            resultTextArea.setText("Please select a database to use first!");
            return true;
        }

        String SqlStatement;
        int n = 100;

        // collection disciplines
        for (int i = 0; i < n; i++) {
            SqlStatement = "insert into disciplines (DiscID,DName,CreditNr) values (" +
                    "DiscID_" + i + "," +
                    "DName_" + i + "," +
                    i + ");";
            ProcessInsertIntoTable(SqlStatement);
        }

        // collection specialization
        for (int i = 0; i < n; i++) {
            SqlStatement = "insert into specialization (SpecID,SpecName,Language) values (" +
                    "SpecID_" + i + "," +
                    "SpecName_" + i + "," +
                     "Language_" + i + ");";
            ProcessInsertIntoTable(SqlStatement);
        }

        // collection groups
        for (int i = 0; i < n; i++) {
            SqlStatement = "insert into groups (GroupId,SpecID) values (" +
                    i + "," +
                    "SpecID_" + i + ");";
            ProcessInsertIntoTable(SqlStatement);
        }

        // collection students
        for (int i = 0; i < n; i++) {
            SqlStatement = "insert into students (StudID,GroupId,StudName,Email) values (" +
                    i + "," +
                    i + "," +
                    "StudName_" + i + "," +
                    "Email_" + i + ");";
            ProcessInsertIntoTable(SqlStatement);
        }

        // collection marks
        for (int i = 0; i < n; i++) {
            SqlStatement = "insert into marks (StudID,DiscID,Mark) values (" +
                    i + "," +
                    "DiscID_" + i + "," +
                    i + ");";
            ProcessInsertIntoTable(SqlStatement);
        }

        resultTextArea.setText("Data successfully inserted;");
        return true;
    }

    public boolean ProcessInnerJoin() {
        String selectPattern = "^\\s*(select)(\\s+)(distinct)?(\\s+)?((?:\\w+\\.\\w+|[\\w\\*]+\\s*,\\s*\\w+\\.\\w+|\\s*,\\s*[\\w\\*]+)*)(\\s+)(from)(\\s+)(\\w+\\s*(?:,\\s*\\w+\\s*)*)(\\s*)(?:(where)(\\s+)((.|\\n)*))?(inner join)(\\s+)(\\w+\\s*(?:,\\s*\\w+\\s*)*)(\\s*)(on)(\\s+)((.|\\n)*);";

        Pattern pattern = Pattern.compile(selectPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sqlStatementTextArea.getText().toLowerCase());

        if (!matcher.matches()) {
            return false;
        }

        if (crtDatabase == null) {
            resultTextArea.setText("Please select a database to use first!");
            return true;
        }

        // is distinct
        String isDistinctString = matcher.group(3);
        boolean isDistinct = false;
        if (isDistinctString != null) {
            isDistinct = true;
        }

        // table1 and table2
        String table1String = matcher.group(9).trim();
        String table2String = matcher.group(17).trim();

        Table table1 = crtDatabase.getTableByName(table1String);
        Table table2 = crtDatabase.getTableByName(table2String);

        if (table1 == null) {
            resultTextArea.setText("Table " + table1String + " does not exist in the " + crtDatabase.getDatabaseName() + " database.");
            return true;
        }
        if (table2 == null) {
            resultTextArea.setText("Table " + table2String + " does not exist in the " + crtDatabase.getDatabaseName() + " database.");
            return true;
        }

        // indexes + columns + pks
        List<Index> indexList1 = table1.getIndexes();
        List<String> columnList1 = table1.getColumnsNotPartOfPK().stream().map(Column::getColumnName).toList();
        List<String> pkList1 = table1.getPrimaryKeys().stream().map(PrimaryKey::getPkAttribute).toList();

        List<Index> indexList2 = table2.getIndexes();
        List<String> columnList2 = table2.getColumnsNotPartOfPK().stream().map(Column::getColumnName).toList();;
        List<String> pkList2 = table2.getPrimaryKeys().stream().map(PrimaryKey::getPkAttribute).toList();;

        // columns
        String OnCondition = matcher.group(21);
        String condition = "";
        if (OnCondition.contains("=")) {
            condition = "=";
        }
        if (OnCondition.contains("like")) {
            condition = "like";
        }
        if (OnCondition.contains("<")) {
            condition = "<";
        }
        if (OnCondition.contains(">")) {
            condition = ">";
        }
        if (OnCondition.contains("<=")) {
            condition = "<=";
        }
        if (OnCondition.contains(">=")) {
            condition = ">=";
        }

        String columnName1 = OnCondition.split(condition)[0].trim().split("\\.")[1];
        String columnName2 = OnCondition.split(condition)[1].trim().split("\\.")[1];

        boolean hasIndex1 = false;
        for (Index index : indexList1) {
            if (index.getColumns().contains(columnName1)) {
                hasIndex1 = true;
                break;
            }
        }
        boolean hasIndex2 = false;
        for (Index index : indexList2) {
            if (index.getColumns().contains(columnName2)) {
                hasIndex2 = true;
                break;
            }
        }

        // where clauses
        String whereClause = matcher.group(13);
        if (whereClause != null) {
            whereClause = whereClause.trim();
        }

        List<List<Document>> result = null;
        MongoDatabase database = mongoClient.getDatabase(crtDatabase.getDatabaseName());

        // sort merge join
        if (!hasIndex1 && !hasIndex2) {
            result = sortMergeJoin(table1, table2, columnList1, pkList1, columnList2, pkList2, columnName1, columnName2, database, 0, 1);
        // indexed nested loops
        } else if (hasIndex1) {
            result = indexedNestedLoop(table2, table1, indexList1, columnList2, pkList2, 0, 1, columnName2, columnName1, database);
        } else if (hasIndex2) {
            result = indexedNestedLoop(table1, table2, indexList2, columnList1, pkList1, 1, 0, columnName1, columnName2, database);
        }
        result = finalJoinOfData(result);
        result = parseConditionsForJoin(result, condition, table1, table2, columnName1, columnName2);

        // Display query results
        // columns to show
        String columnsToShow = matcher.group(5);
        if (columnsToShow != null) {
            columnsToShow = columnsToShow.trim();
        }
        List<String> columnsToShowAux = Arrays.stream(columnsToShow.split(",")).map(s -> s.trim()).toList();
        List<Pair<String,String>> columnAndTableList = new ArrayList<>();
        for (String column: columnsToShowAux) {
            if (column.contains(".")) {
                List<String> columnAndTable = List.of(column.split("\\."));
                columnAndTableList.add(new Pair<>(columnAndTable.get(1), columnAndTable.get(0)));
            } else {
                columnAndTableList.add(new Pair<>(column.trim(), table1String));
            }
        }

        DisplayResults(result, columnAndTableList, table1, table2, isDistinct);

        return true;
    }

    public List<List<Document>> sortMergeJoin(Table table1, Table table2, List<String> columnList1, List<String> pkList1, List<String> columnList2, List<String> pkList2, String columnName1, String columnName2, MongoDatabase database, int i1, int i2) {
        List<List<Document>> result = new ArrayList<>();
        result.add(new ArrayList<>());
        result.add(new ArrayList<>());

        MongoCollection<Document> collection1 = database.getCollection(table1.getTableName());
        List<Document> list1 = collection1.find().into(new ArrayList<>());
        MongoCollection<Document> collection2 = database.getCollection(table2.getTableName());
        List<Document> list2 = collection2.find().into(new ArrayList<>());

        List<String> l1v = new ArrayList<>();
        List<String> l2v = new ArrayList<>();

        if (columnList1.contains(columnName1)) {
            list1.sort(Comparator.comparing(item -> item.get("values").toString().split("#")[columnList1.indexOf(columnName1)]));
            l1v = list1.stream().map(item -> item.get("values").toString().split("#")[columnList1.indexOf(columnName1)]).toList();
        } else if (pkList1.contains(columnName1)) {
            list1.sort(Comparator.comparing(item -> item.get("_id").toString().split("#")[pkList1.indexOf(columnName1)]));
            l1v = list1.stream().map(item -> item.get("_id").toString().split("#")[pkList1.indexOf(columnName1)]).toList();
        }
        if (columnList2.contains(columnName2)) {
            list2.sort(Comparator.comparing(item -> item.get("values").toString().split("#")[columnList2.indexOf(columnName2)]));
            l2v = list2.stream().map(item -> item.get("values").toString().split("#")[columnList2.indexOf(columnName2)]).toList();
        } else if (pkList2.contains(columnName2)) {
            list2.sort(Comparator.comparing(item -> item.get("_id").toString().split("#")[pkList2.indexOf(columnName2)]));
            l2v = list2.stream().map(item -> item.get("_id").toString().split("#")[pkList2.indexOf(columnName2)]).toList();
        }

        int j1 = 0, j2 = 0;
        while (j1 < list1.size() && j2 < list2.size()) {
            while (l1v.get(j1).equals(l2v.get(j2))) {
                result.get(i2).add(list2.get(j2));
                result.get(i1).add(list1.get(j1));

                j1++;
                j2++;

                if (j1 == list1.size() || j2 == list2.size()) {
                    break;
                }
            }
            if (j1 < list1.size() && j2 < list2.size()) {
                while (l1v.get(j1).compareTo(l2v.get(j2)) < 0) {
                    result.get(i1).add(list1.get(j1));
                    j1++;
                    if (j1 == list1.size()) {
                        break;
                    }
                }
            }
            if (j1 < list1.size() && j2 < list2.size()) {
                while (l1v.get(j1).compareTo(l2v.get(j2)) > 0) {
                    result.get(i2).add(list2.get(j2));
                    j2++;
                    if (j2 == list2.size()) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    private List<List<Document>> finalJoinOfData(List<List<Document>> result) {
        List<List<Document>> interm = null;
        List<Document> aux = new ArrayList<>(result.get(0));

        for (int i = 1; i < result.size(); i++) {
            interm = cartesianProduct(aux, result.get(i));
        }

        return interm;
    }

    private static List<List<Document>> cartesianProduct(List<Document> list1, List<Document> list2) {
        List<List<Document>> result = new ArrayList<>();

        for (Document item : list1) {
            for (Document element : list2) {
                List<Document> newItem = new ArrayList<>();
                newItem.add(item);
                newItem.add(element);
                result.add(newItem);
            }
        }

        return result;
    }

    public List<List<Document>> parseConditionsForJoin(List<List<Document>> before, String condition, Table table1, Table table2, String columnName1, String columnName2) {
        List<List<Document>> result = new ArrayList<>();

        for (List<Document> item : before) {
            if (checkCond(condition, item, table1, table2, columnName1, columnName2)) {
                result.add(item);
            }
        }

        return result;
    }

    public boolean checkCond(String condition, List<Document> documents, Table table1, Table table2, String columnName1, String columnName2) {
        Map<String, String> columnValueMap1 = getColumnValueMap(documents.get(0), table1);
        Map<String, String> columnValueMap2 = getColumnValueMap(documents.get(1), table2);

        switch (condition) {
            case "=" : {
                if(Objects.equals(columnValueMap1.get(columnName1), columnValueMap2.get(columnName2))) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        return false;
    }

    private void DisplayResults(List<List<Document>> resultDocuments, List<Pair<String, String>> selectedColumns, Table table1, Table table2, boolean isDistinct) {
        // Implement logic to display query results
        StringBuilder resultStringBuilder = new StringBuilder();
        List<String> result = new ArrayList<>();

        if (resultDocuments == null) {
            resultTextArea.setText("No records found!");
            return;
        }

        for (List<Document> documents : resultDocuments) {
            // Display selected columns
            resultStringBuilder = new StringBuilder();
            for (Pair<String, String> column : selectedColumns) {
                Table crtTable = crtDatabase.getTableByName(column.getValue());

                Map<String, String> columnValueMap;
                if (Objects.equals(crtTable.getTableName(), table1.getTableName())) {
                    columnValueMap = getColumnValueMap(documents.get(0), crtTable);
                } else {
                    columnValueMap = getColumnValueMap(documents.get(1), crtTable);
                }
                if (!column.getKey().trim().equals("*")) {
                    for (Map.Entry<String,String> entry : columnValueMap.entrySet()) {
                        if (Objects.equals(entry.getKey(), column.getKey())) {
                            resultStringBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
                        }
                    }
                } else {
                    for (Map.Entry<String,String> entry : columnValueMap.entrySet()) {
                        resultStringBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
                    }
                }
            }
            resultStringBuilder.setLength(resultStringBuilder.length() - 2); // Remove trailing comma and space
            resultStringBuilder.append("\n");
            result.add(resultStringBuilder.toString());
        }

        if (isDistinct) {
            result = result.stream().distinct().toList();
        }

        resultTextArea.setText(result.toString()
                .replace("[","")
                .replace("]", "")
                .replace(", ", ""));
    }

    public List<List<Document>> indexedNestedLoop(Table table1, Table table2, List<Index> indexList, List<String> columnList, List<String> pkList, int i1, int i2, String columnName1, String columnName2, MongoDatabase database) {
        List<List<Document>> result = new ArrayList<>();
        result.add(new ArrayList<>());
        result.add(new ArrayList<>());

        MongoCollection<Document> collection1 = database.getCollection(table1.getTableName());
        List<Document> list1 = collection1.find().into(new ArrayList<>());

        for (Document doc : list1) {
            List<String> resValues = Arrays.stream(doc.getString("values").split("#")).filter(value -> !value.trim().isEmpty()).toList();
            List<String> resPk = Arrays.stream(doc.getString("_id").split("#")).filter(pkValue -> !pkValue.trim().isEmpty()).toList();

            for (int attributeListIndex = 0; attributeListIndex < columnList.size(); attributeListIndex++) {
                String att = columnList.get(attributeListIndex);
                if (att.equals(columnName1)) {
                    result = indexSearchInTableJoin(result, indexList, table2, i1, columnName2, resValues.get(attributeListIndex), database);
                    result.get(i2).add(doc);

                    break;
                }
            }

            for (int pkIndex = 0; pkIndex < pkList.size(); pkIndex++) {
                String pk = pkList.get(pkIndex);
                if (pk.equals(columnName1)) {
                    result = indexSearchInTableJoin(result, indexList, table2, i1, columnName2, resPk.get(pkIndex), database);
                    result.get(i2).add(doc);

                    break;
                }
            }
        }
        return result;
    }

    public List<List<Document>> indexSearchInTableJoin(List<List<Document>> result, List<Index> indexList, Table table, int i, String columnName, String expectedValue, MongoDatabase database) {
        List<Document> docs = new ArrayList<>();

        for (Index index : indexList) {
            if (index.getIndexName().contains(columnName)) {
                MongoCollection<Document> collection = database.getCollection(table.getTableName().toLowerCase() + "_" + columnName.toLowerCase() + "_index");
                docs.addAll(collection.find(Filters.eq("_id", expectedValue)).into(new ArrayList<>()));
                for (Document doc : docs) {
                    MongoCollection<Document> collection2 = database.getCollection(table.getTableName());
                    List<String> values = Arrays.stream(doc.get("values").toString().split("\\$")).toList();
                    for (String value : values) {
                        result.get(i).addAll(collection2.find(Filters.eq("_id", value)).into(new ArrayList<>()));
                    }
                }
            }
        }
        return result;
    }
}
