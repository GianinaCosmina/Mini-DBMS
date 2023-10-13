//package com.example.minidbms.domain;
//
//import java.io.File;
//import javax.xml.bind.JAXBContext;
//import javax.xml.bind.JAXBException;
//import javax.xml.bind.Marshaller;
//import javax.xml.bind.Unmarshaller;
//
//public class MiniDBMS {
//    private static final String DBMS_XML_FILE = "Catalog.xml";
//    public static void main(String[] args) {
//        // Create a new DBMS
//        Databases myDBMS = loadDBMSFromXML(); // Try to load existing DBMS from XML
//        if (myDBMS == null) {
//            myDBMS = new Databases(); // Create a new DBMS if not found
//        }
////        // Create a new database
////        Database myDatabase = new Database("MyDatabase");
////
////        // Create a table
////        Table usersTable = new Table("Users");
////        usersTable.createIndex("Username"); // Create an index
////        usersTable.createIndex("Email");    // Create another index
////
////        // Define columns and primary keys
////        usersTable.getColumns().add(new Column("ID", "int", true));
////        usersTable.getColumns().add(new Column("Username", "varchar", false));
////        usersTable.getColumns().add(new Column("Email", "varchar", false));
////        usersTable.getPrimaryKeys().add("ID");
////
////        // Add the table to the database
////        myDatabase.createTable(usersTable);
////
////        // Save the database structure to an XML file
////        saveDatabaseToXML(myDatabase, "database.xml");
//
//        // Create a new DBMS
////        DBMS myDBMS = new DBMS();
//        Database db = new Database("Database2");
//
//        // Create databases
//        myDBMS.createDatabase("Database1");
//        myDBMS.addDatabase(db);
////        myDBMS.createDatabase("Database2");
//
//        // Create a table
//        Table usersTable = new Table("Users");
////        usersTable.createIndex("Username"); // Create an index
////        usersTable.createIndex("Email");    // Create another index
//
//        // Define columns and primary keys
//        usersTable.getColumns().add(new Column("ID", "int", true));
//        usersTable.getColumns().add(new Column("Username", "varchar", false));
//        usersTable.getColumns().add(new Column("Email", "varchar", false));
////        usersTable.getPrimaryKeys().add("ID");
//
//        // Add the table to the database
//        db.createTable(usersTable);
//
//        // Save the structure of the DBMS to an XML file
////        saveDBMSToXML(myDBMS, "dbms.xml");
//        saveDBMSToXML(myDBMS);
//    }
//
////    public static void saveDBMSToXML(Databases dbms, String fileName) {
////        try {
////            File xmlFile = new File(fileName);
////            JAXBContext context = JAXBContext.newInstance(Databases.class);
////            Marshaller marshaller = context.createMarshaller();
////            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
////            marshaller.marshal(dbms, xmlFile);
////            System.out.println("DBMS structure saved to " + fileName);
////        } catch (JAXBException e) {
////            e.printStackTrace();
////        }
////    }
//
//    public static Databases loadDBMSFromXML() {
//        File xmlFile = new File(DBMS_XML_FILE);
//        if (xmlFile.exists()) {
//            try {
//                JAXBContext context = JAXBContext.newInstance(Databases.class);
//                Unmarshaller unmarshaller = context.createUnmarshaller();
//                return (Databases) unmarshaller.unmarshal(xmlFile);
//            } catch (JAXBException e) {
//                e.printStackTrace();
//            }
//        }
//        return new Databases();
//    }
//
//    public static void saveDBMSToXML(Databases dbms) {
//        try {
//            File xmlFile = new File(DBMS_XML_FILE);
//            JAXBContext context = JAXBContext.newInstance(Databases.class);
//            Marshaller marshaller = context.createMarshaller();
//            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
//            marshaller.marshal(dbms, xmlFile);
//            System.out.println("DBMS structure saved to " + DBMS_XML_FILE);
//        } catch (JAXBException e) {
//            e.printStackTrace();
//        }
//    }
//}
