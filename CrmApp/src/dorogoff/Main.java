// Main.java
// главный класс
// Дорогов Алексей, 4081/11
package dorogoff;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.w3c.dom.Document;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class Main {

    static Connection conn;
    static Driver d;
    static String config[] = new String[6];
    static Statement state;
    static ResultSet resSet;

    public static void main(String argv[]) {
        if (argv.length < 2) {
            System.out.println("Please use -i filename.xml for import; \nPlease use -e filename.xml for export");
            return;
        }
        // получим найтроски для подключения к базе
        getConfig();

        // создадим подключение
        // jqybird скопировать в Java\jdk1.6.0_24\jre\lib\ext
        try {
            Class.forName(config[3]);
            conn = DriverManager.getConnection(config[0], config[1], config[2]);
            if (conn == null) {
                System.out.println("Cant open connection url: " + config[0] + "user: " + config[1] + "password: " + config[2]);
                return;
            }
            // установим авто коммит
            conn.setAutoCommit(true);
            state = conn.createStatement();
        } catch (SQLException e) {
            System.out.println("Error: sql : " + e.getMessage());
            return;
        } catch (ClassNotFoundException e) {
            System.out.println("Firebird JCA-JDBC driver not found in class path");
            System.out.println(e.getMessage());
        }


        // Если импорт
        if (argv[0].equals("-i")) {
            importMethod(argv[1].toString());
        }
        if (argv[0].equals("-e")) {
            exportMethod(argv[1].toString());
        }
    }

    // метод для импорта данных из файла укаанного в аргументах
    public static void importMethod(String filename) {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new File(filename));
            doc.getDocumentElement().normalize();
            // будем искать элемент meet
            NodeList listOfMeets = doc.getElementsByTagName("meet");
            int totalRows = listOfMeets.getLength();
            int sum = 0;
            System.out.println("Total rows to import: " + totalRows);
            for (int s = 0; s < totalRows; s++) {
                Node firstPersonNode = listOfMeets.item(s);
                if (firstPersonNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element Node = (Element) firstPersonNode;
                    String dateText = "", clientId = "", workerId = "", additString = "";
                    try {
                        // получим дату
                        NodeList dateList = Node.getElementsByTagName("date");
                        Element dateElement = (Element) dateList.item(0);
                        NodeList textDate = dateElement.getChildNodes();
                        dateText = ((Node) textDate.item(0)).getNodeValue().trim();
                        //получим идентификатор работника
                        NodeList workerList = Node.getElementsByTagName("worker");
                        Element workerElement = (Element) workerList.item(0);
                        NodeList textWorker = workerElement.getChildNodes();
                        workerId = ((Node) textWorker.item(0)).getNodeValue().trim();
                        // идентифкатор клиента
                        NodeList clientList = Node.getElementsByTagName("client");
                        Element clientElement = (Element) clientList.item(0);
                        NodeList textClient = clientElement.getChildNodes();
                        clientId = ((Node) textClient.item(0)).getNodeValue().trim();
                        // дополнительная информация
                        NodeList additList = Node.getElementsByTagName("addit");
                        Element additElement = (Element) additList.item(0);
                        NodeList textAddit = additElement.getChildNodes();
                        additString = ((Node) textAddit.item(0)).getNodeValue().trim();
                    } catch (NullPointerException e) {
                        System.out.println("Error: record error, some entries haven't all info");
                    }
                    // проверим не назначена ли уже эта встреча
                    try {
                        resSet = state.executeQuery("select meet_id from meetings where cl_man="
                                + clientId + " AND w_man=" + workerId + "");
                        int cnt = 0;
                        while (resSet.next()) {
                            cnt++;
                        }
                        //  System.out.println("cnt: " + cnt);
                        if (cnt == 0) {
                            // добавляем запись
                            state.execute("insert into meetings(datum, cl_man, w_man, addit) "
                                    + "values('" + dateText + "', '" + clientId + "', '" + workerId + "', '" + additString + "')");
                            System.out.println("Record insered.");
                            // инкрементируем общий счетчик добавленных записей
                            sum++;
                        } else {
                            System.out.println("Not records to import");
                        }
                    } catch (SQLException e) {
                        System.out.println("SQl error: " + e.getMessage());
                    }
                }
            }
            System.out.println("Import finished, added rows: " + sum);
        } catch (SAXException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (ParserConfigurationException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error: cannot open file" + e.getMessage());
        }

    }

    // экспорт данных о встрече в файл
    public static void exportMethod(String filename) {
        // config[4] -начало периода
        // config[5] - конец периода
        // проверим сущестувет ли уже такой файл
        if (new File(filename).exists()) {
            System.out.println("File " + filename + "already exist, you want to overwrite them?");
            System.out.print("Type yes to overwrite, or type new filename: ");
            // предложим поменять имя файла
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String answer = "";
            try {
                answer = reader.readLine();
            } catch (IOException e) {
                System.out.println("IO error");
            }
            if (!answer.equals("yes")) {
                System.out.println("Your select other file: " + answer);
                filename = answer;
            }
        }
        // формируем запрос на получение встреч
        String queryString = "SELECT * from meetings where datum between '" + config[4] + "' AND '" + config[5] + "'";
        try {
            // получим встречи
            resSet = state.executeQuery(queryString);
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(filename));
            // сформируем xmk файл
            out.write("<meetings>\n");
            while (resSet.next()) {
                // если что есть пишем в файл
                out.write("<meet>\n");
                out.write("\t<data>" + resSet.getString(2).substring(0, 19) + "</data>\n");
                out.write("\t<client>" + resSet.getString(3) + "</client>\n");
                out.write("\t<worker>" + resSet.getString(4) + "</worker>\n");
                out.write("\t<addit>" + resSet.getString(5) + "</addit>\n");
                out.write("</meet>\n");
            }
            out.write("</meetings>\n");
            out.close();
        } catch (SQLException e) {
            System.out.println("SQL Error: ошибка входых данных " + e.getMessage());
        } catch (FileNotFoundException e) {
            System.out.println("Error: file not found");
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // метод для получения настроек к БД из файла, формат:
    // url к базе
    // username
    // password
    // драйвер org.firebirdsql.jdbc.FBDriver
    // дата начала периода
    // дата конца периода
    public static void getConfig() {
        try {
            FileInputStream fStream = new FileInputStream("config.txt");
            DataInputStream in = new DataInputStream(fStream);
            BufferedReader buffer = new BufferedReader(new InputStreamReader(in));
            String tmp;
            int cnt = 0;
            while ((tmp = buffer.readLine()) != null) {
                config[cnt] = tmp;
                cnt++;
            }
            in.close();
        } catch (IOException e) {
            System.out.println("Error: cannot open file" + e.getMessage());
        }
    }
}
