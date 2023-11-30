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
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.bson.Document;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.eq;

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
        List<String> columnsToShowAux = Arrays.stream(columnsToShow.split(",")).map(s -> s.trim()).toList();
        List<Pair<String,String>> columnAndTableList = new ArrayList<>();
        for (String column: columnsToShowAux) {
            if (column.contains(".")) {
                List<String> columnAndTable = List.of(column.split("."));
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

//        MongoCollection<Document> collection = database.getCollection(tableName);
//
//        List<Document> resultDocuments;
//        if (whereClause != null && !whereClause.trim().isEmpty()) {
//            // Implement logic to parse and execute WHERE conditions
//            resultDocuments = ExecuteWhereCondition(collection, whereClause);
//        } else {
//            resultDocuments = collection.find().into(new ArrayList<>());
//        }
//
//        // Display query results
//        DisplayQueryResults(resultDocuments, columnAndTableList);

        MongoDatabase database = mongoClient.getDatabase(crtDatabase.getDatabaseName());

        List<Document> resultDocuments = null;
        List<Document> resultDocumentsList = null;
        if (whereClause != null && !whereClause.trim().isEmpty()) {
//             Implement logic to parse and execute WHERE conditions // TODO
//            resultDocuments = ExecuteWhereCondition(collection, whereClause);
        } else {
            for (String tableName: tablesInWhichToSearchList) {
                MongoCollection<Document> collection = database.getCollection(tableName);
                resultDocuments = collection.find().into(new ArrayList<>());
            }
            if (resultDocumentsList == null) {
                resultDocumentsList = resultDocuments;
            } else {
                resultDocumentsList.addAll(resultDocuments);
            }
        }

        // Display query results
        DisplayQueryResults(resultDocumentsList, columnAndTableList, isDistinct);

        return true;
    }

    private List<Document> ExecuteWhereCondition(MongoCollection<Document> collection, String whereClause) {
        // Implement logic to parse and execute WHERE conditions
        // For simplicity, let's assume a basic condition, e.g., "columnName = 'value'"
        String[] parts = whereClause.split("=");
        if (parts.length == 2) {
            String columnName = parts[0].trim();
            String value = parts[1].trim().replaceAll("'", "");

            return collection.find(Filters.eq(columnName, value)).into(new ArrayList<>());
        }

        // Handle more complex conditions as needed

        return new ArrayList<>();
    }

    private void DisplayQueryResults(List<Document> resultDocuments, List<Pair<String, String>> selectedColumns, boolean isDistinct) {
        // Implement logic to display query results
        StringBuilder resultStringBuilder = new StringBuilder();
        List<String> result = new ArrayList<>();

        for (Document document : resultDocuments) {
            // Display selected columns
            resultStringBuilder = new StringBuilder();
            for (Pair<String, String> column : selectedColumns) {
                List<String> ids = Arrays.stream(document.get("_id").toString().split("#")).toList();
                List<String> values = Arrays.stream(document.get("values").toString().split("#")).toList();
                Table crtTable = crtDatabase.getTableByName(column.getValue());

                if (!column.getKey().trim().equals("*")) {
                    int index = 0;
                    boolean valueSet = false;
                    List<String> columnsAlreadyChecked = new ArrayList<>();
                    for (PrimaryKey pk : crtTable.getPrimaryKeys()) {
                        columnsAlreadyChecked.add(pk.getPkAttribute());
                        if (Objects.equals(pk.getPkAttribute(), column.getKey())) {
                            resultStringBuilder.append(column.getKey()).append(": ").append(ids.get(index)).append("; ");
                            valueSet = true;
                            break;
                        }
                        index++;
                    }
                    index = 0;
                    if (!valueSet) {
                        for (Column col : crtTable.getColumns()) {
                            if (!columnsAlreadyChecked.contains(col.getColumnName())) {
                                if (Objects.equals(col.getColumnName(), column.getKey())) {
                                    resultStringBuilder.append(column.getKey()).append(": ").append(values.get(index)).append("; ");
                                    break;
                                }
                                index++;
                            }
                        }
                    }
                } else {
                    int index = 0;
                    List<String> columnsAlreadyChecked = new ArrayList<>();
                    for (PrimaryKey pk : crtTable.getPrimaryKeys()) {
                        columnsAlreadyChecked.add(pk.getPkAttribute());
                        resultStringBuilder.append(pk.getPkAttribute()).append(": ").append(ids.get(index)).append("; ");
                        index++;
                    }
                    index = 0;
                    for (Column col : crtTable.getColumns()) {
                        if (!columnsAlreadyChecked.contains(col.getColumnName())) {
                            resultStringBuilder.append(col.getColumnName()).append(": ").append(values.get(index)).append("; ");
                            index++;
                        }
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
}
