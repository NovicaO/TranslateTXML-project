import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 */
public class TranslateTXML {
    public static String SOURCE_FLAG = "", OUTPUT_FLAG = "", SOURCE_LANGUAGE = "", TARGET_LANGUAGE = "",
            WORD_LINGO_PASSWORD = "7DC5xl94";
    public static boolean REPORT = false;
    public static int ERROR_STYLE = 1;
    public static int TOTAL_WORD_COUNT = 0;
    public static final HashSet<String> ALLOWED_LANGUAGES = new HashSet<String>(Arrays.asList("en", "ar", "de", "el", "es", "fr", "it", "ja", "ko",
            "nl", "pt", "ru", "sv", "zh_cn", "zh_tw"));
    public static HashMap<String, Long> UNIQUE_WORD_COUNT = new HashMap<String, Long>();
    public static HashMap<String, String> TRANSLTED_MAP = new HashMap<String, String>();

    /**
     * @implNote This application translates given txml file and outputs new txml file
     * Uses Worldlingo API to translate the text
     */
    public static void main(String[] args) {

        //check if user entered the command correctly
        boolean hasRequiredArgs = parseAndCheckFlags(args);
        if (!hasRequiredArgs) {
            return;
        }
        // checking if the language is supported if not app will exit
        if (!isLanguageSupported(SOURCE_LANGUAGE, TARGET_LANGUAGE)) {
            System.out.println("One of defined languages are not supported currently");
            return;
        }
        NodeList txml = translatesStoresAndCountUniqueWords();
        generateTranslatedDocument(txml, TRANSLTED_MAP);

    }

    /**
     * @implNote Geting relative path to current class
     * @return String
     */
    public static String getPath(){
        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath()+ "/";
        return s;
    }

    /**

     * @implNote This method traverse through XML document translates source text and stores it in HashMap. Counts number of unique words in original and translated version and stores in HashMap.
     * Returns the root NodeList element for document recreation.
     * @return NodeList
     */
    public static NodeList translatesStoresAndCountUniqueWords() {
        NodeList txml = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            try {
                // getting the document
                Document doc = builder.parse(SOURCE_FLAG);
                // getting the whole document so we can recreate it later in docBuilder method
                txml = doc.getElementsByTagName("txml");

                // Getting the nodelist and loop thru all the sentences
                NodeList segmentList = doc.getElementsByTagName("segment");
                for (int i = 0; i < segmentList.getLength(); i++) {
                    // Segment node
                    Node sl = segmentList.item(i);
                    if (sl.getNodeType() == Node.ELEMENT_NODE) {
                        Element currentSegment = (Element) sl;
                        NodeList sourceList = currentSegment.getChildNodes();
                        for (int j = 0; j < sourceList.getLength(); j++) {
                            // Sentence
                            Node source = sourceList.item(j);
                            if (source.getNodeType() == Node.ELEMENT_NODE) {
                                Element s = (Element) source;
                                // Creating custom URL
                                String url = createUrilToTranslate(s.getTextContent());
                                // Original text array for -e flag report
                                String[] originalTextArray = s.getTextContent().split(" ");
                                processStringWithWordCount(originalTextArray, UNIQUE_WORD_COUNT);
                                // Getting translation from worldlingo
                                String translated = getTranslation(url, "");
                                // Translated array for -e flag report
                                String[] translatedArray = translated.split(" ");
                                processStringWithWordCount(translatedArray, UNIQUE_WORD_COUNT);
                                // We are getting segmentid so we know where to push translation
                                // When we recreate it
                                String segmentId = currentSegment.getAttribute("segmentId");
                                TRANSLTED_MAP.put(segmentId, translated);
                            }
                        }
                    }
                }

            } catch (SAXException e) {
                System.out.println("XML Parse error!");

            } catch (IOException e) {
                System.out.println("File does not exist, please check if the file exists and try again!");
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        return txml;

    }


    /**
     * @implNote This method removes ?, !, . characters from the array so we can have unique words after spliting an array. Counts total word count so we can put it in
     * txml totalword atribute
     * Also checks if a word has a ?,!, . at the end so it does not count as a different word
     * Example: the, and the
     * and places it inside of hashmap for -e flag report
     * @param translatedArray
     * @param hashMapToModify
     */
    public static void processStringWithWordCount(String[] translatedArray, HashMap<String, Long > hashMapToModify){
        for (String st : translatedArray) {
            String st2 = st.toLowerCase().trim();
            st2 = st2.replace("?", "");
            st2 = st2.replace("!", "");
            st2 = st2.replace(".", "");

            if(st2.endsWith(",") || st2.endsWith("?") || st2.endsWith("!") || st.endsWith(":") || st2.endsWith(";")){
                st2.substring(0, st2.length()-1);
            }
            if (st2.isEmpty()) {
                continue;
            }
            if (!hashMapToModify.containsKey(st2)) {
                TOTAL_WORD_COUNT++;
                hashMapToModify.put(st2, 1l);
            } else if (hashMapToModify.containsKey(st2)) {
                TOTAL_WORD_COUNT++;
                hashMapToModify.put(st2, (hashMapToModify.get(st2) + 1));
            }
        }
    }

    /**
     * @implNote This method removes ?, !, . characters from the array so we can have unique words after spliting an array.
     * Also checks if a word has a ?,!, . at the end so it does not count as a different word
     * Example: the, and the
     *
     * @param translatedArray
     * @return
     */
    public static HashMap<String, Long>  processString(String[] translatedArray){
        HashMap<String, Long > listToModify = new HashMap<String, Long>();
        for (String st : translatedArray) {
            String st2 = st.toLowerCase().trim();
            st2 = st2.replace("?", "");
            st2 = st2.replace("!", "");
            st2 = st2.replace(".", "");
            if(st2.endsWith(",") || st2.endsWith("?") || st2.endsWith("!") || st.endsWith(":") || st2.endsWith(";")){
                st2.substring(0, st2.length()-1);
            }

            if (st2.isEmpty()) {
                continue;
            }
            if (!listToModify.containsKey(st2)) {
                listToModify.put(st2, 1l);
            } else if (listToModify.containsKey(st2)) {
                listToModify.put(st2, (listToModify.get(st2) + 1));
            }
        }
        return listToModify;
    }

    /**
     * @implNote This method creates custom URL from the given string.
     * @param source
     * @return String
     */
    public static String createUrilToTranslate(String source) {
        String targetUrl = "https://www.worldlingo.com/S11887.1/api?wl_password="
                + WORD_LINGO_PASSWORD + "&wl_errorstyle="
                + ERROR_STYLE + "&wl_srclang="
                + SOURCE_LANGUAGE + "&wl_trglang=" + TARGET_LANGUAGE + "&wl_data="
                + source.replaceAll(" ", "%20");
        return targetUrl;
    }


    /**
     * @param flag
     * @return boolean
     * @implNote flag This method checks if a user entered a valid flag for proper application function.
     * Available flags: -f, -o, -s, -t, -e
     */
    public static boolean isAValidFlag(String flag) {
        HashSet<String> flags = new HashSet<String>(Arrays.asList("-f", "-o", "-s", "-t", "-e"));
        for (String ar :
                flags) {
            if (ar.equals(flag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @implNote Checks if argument list is empty
     * @param args
     * @return boolean
     */
    public static boolean isEmptyArgumentList(String[] args) {
        return args.length == 0;
    }

    /**
     * @implNote Checks if minimum number of arguments is 8
     * @param args
     * @return boolean
     */
    public static boolean isMinimumArgumentNumber(String[] args) {
        return args.length >= 8;
    }

    /**
     * @implNote Checks if parameter list starts with a flag (-f, -s, -t, -o, -e)
     * Checks if every other argument is a flag
     * @param args
     * @return boolean
     */
    public static boolean validFlagStartsWithFLag(String[] args) {
        for (int st = 0; st < args.length; st += 2) {
            boolean isIt = isAValidFlag(args[st]);
            if (!isIt) {
                System.out.print("Invalid argument " + args[st]);
                return false;
            }
        }
        return true;
    }

    /**
     * @implNote checks if there are duplicate flags in the list
     * Available flags -f, -o, -s, -t, -e
     * @param args
     * @return boolean
     */
    public static boolean duplicateFLags(String[] args) {
        Set<String> duplicate = new HashSet<String>();
        for (int e = 0; e < args.length; e = e + 2) {
            if (duplicate.add(args[e]) == false) {
                System.out.println("There are duplicate flags!");
                return false;
            }
        }
        return true;
    }

    /**
     * @implNote checks if there is .txml in a string
     * @param sourceFlag
     * @param outputFlag
     * @return
     */
    public static boolean haveTxmlExtention(String sourceFlag, String outputFlag) {
        if (!sourceFlag.contains(".txml") || !outputFlag.contains(".txml")) {
            System.out.println("Doesn't have .txml extention");
            return false;
        }
        return true;
    }

    /**
     * @implNote Checks if it ends with  .txml extention
     * @param sourceFlag
     * @param outputFlag
     * @return boolean
     */
    public static boolean endWithTxmlExtention(String sourceFlag, String outputFlag) {
        if (!sourceFlag.endsWith(".txml") || !outputFlag.endsWith(".txml")) {
            System.out.println("Doesn't end with .txml extention");
            return false;
        }
        return true;
    }

    /**
     * @implNote Checks if file exists
     * @param sourceFlag
     * @return boolean
     */
    public static boolean fileExists(String sourceFlag) {
        File tempFile = new File(getPath() + sourceFlag);
        if (!tempFile.exists()) {
            System.out.println("File doesn't exist!");
            return false;
        }
        return true;
    }

    /**
     * @implNote This method checks user input and parse the available flags from the arguments
     * @param args
     * @return boolean

     */
    public static boolean parseAndCheckFlags(String[] args) {
        HashSet<String> requiredArgs = new HashSet<String>(Arrays.asList("-f", "-o", "-s", "-t"));
        // if empty
        if (isEmptyArgumentList(args)) {
            System.out.println("Empty argument list");
            return false;
        }
        // minimum 8 arguments for app to work
        if (!isMinimumArgumentNumber(args)) {
            System.out.println("Not enough arguments");
            return false;
        }
        // checks if a flag is valid eg. -f, -s, -t, -o, -e
        // it has to start with a flag
        // every 2 argument has to be a flag
        if (!validFlagStartsWithFLag(args)) {
            return false;
        }
        // if there are duplicate flags
        if (!duplicateFLags(args)) {
            return false;
        }
        for (int i = 0; i < args.length; i++) {

            switch (args[i]) {
                case "-f":
                    SOURCE_FLAG = args[i + 1];
                    break;
                case "-o":
                    OUTPUT_FLAG = args[i + 1];
                    break;
                case "-s":
                    SOURCE_LANGUAGE = args[i + 1].toLowerCase();
                    break;
                case "-t":
                    TARGET_LANGUAGE = args[i + 1].toLowerCase();
                    break;
                case "-e":
                    REPORT = true;
                    break;
            }
            requiredArgs.remove(args[i]);
        }

        // does it have .txml extention
        if (!haveTxmlExtention(SOURCE_FLAG, OUTPUT_FLAG)) {
            return false;
        }
        // does it end with txml
        if (!endWithTxmlExtention(SOURCE_FLAG, OUTPUT_FLAG)) {
            return false;
        }
        // does file exist
        if (!fileExists(SOURCE_FLAG)) {
            System.out.println("This file does not exist!");
            return false;
        }
        if (requiredArgs.size() == 0) {
            return true;
        }
        if (requiredArgs.size() > 0) {
            System.out.println("\n There are some missing arguments");
            for (String arg : requiredArgs) {
                System.out.println(arg);
            }
            return false;
        }
        return true;
    }

    /**
     * @implNote  Checks if the language is supported.
     * @param sourceLanguage, targetLanguage
     * @return boolean
     */
    public static boolean isLanguageSupported(String sourceLanguage, String targetLanguage){
        boolean flag1 = false;
        boolean flag2 = false;
        for(String allowedLang: ALLOWED_LANGUAGES){
            if(allowedLang.equals(sourceLanguage)){
                flag1 = true;
                continue;
            }
            if(allowedLang.equals(targetLanguage)){
                flag2 = true;
                continue;
            }
        }
        return flag1 && flag2;
    }

    /**
     * @implNote This method recreates given NodeList txml element and adds translation to <target></target> tag.
     * And creates new TXML file
     * @param txml
     * @param translatedMap
     * @exception ParserConfigurationException
     * @exception TransformerException
     */
    public static void generateTranslatedDocument(NodeList txml, Map<String, String> translatedMap) {
        try {

            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();

            //Going thru original document and recreating it
            for (int i = 0; i < txml.getLength(); i++) {
                Node sl = txml.item(i);

                if (sl.getNodeType() == Node.ELEMENT_NODE) {
                    // txml element - root
                    Element sEl = (Element) sl;
                    Element root = document.createElement(sEl.getTagName());
                    root.setAttribute("version", sEl.getAttribute("version"));
                    root.setAttribute("segtype", sEl.getAttribute("segtype"));
                    root.setAttribute("datatype", sEl.getAttribute("datatype"));
                    root.setAttribute("locale", sEl.getAttribute("locale"));
                    root.setAttribute("createdby", sEl.getAttribute("createdby"));
                    root.setAttribute("targetlocale", sEl.getAttribute("targetlocale"));
                    root.setAttribute("file_extension", sEl.getAttribute("file_extension"));
                    root.setAttribute("editedby", sEl.getAttribute("editedby"));
                    root.setAttribute("wordcount", TOTAL_WORD_COUNT + "");
                    document.appendChild(root);

                    // Translatable node
                    NodeList translatableList = sEl.getChildNodes();
                    for (int j = 0; j < translatableList.getLength(); j++) {
                        Node t = translatableList.item(j);
                        if (t.getNodeType() == Node.ELEMENT_NODE) {
                            Element translatable = (Element) t;
                            Element translatableElement = document.createElement(translatable.getTagName());
                            translatableElement.setAttribute("blockId", translatable.getAttribute("blockId"));
                            translatableElement.setAttribute("wordcount", translatable.getAttribute("wordcount"));
                            root.appendChild(translatableElement);
                            // Segment list node
                            NodeList segmentList = translatable.getChildNodes();

                            for (int k = 0; k < segmentList.getLength(); k++) {
                                Node idk = segmentList.item(k);
                                if (idk.getNodeType() == Node.ELEMENT_NODE) {
                                    Element segment = (Element) idk;
                                    Element segmentElement = document.createElement(segment.getTagName());
                                    segmentElement.setAttribute("blockId",
                                            segment.getAttribute("segmentId"));
                                    translatableElement.appendChild(segmentElement);
                                    // Source list node
                                    NodeList sourceList = segment.getChildNodes();
                                    for (int f = 0; f < sourceList.getLength(); f++) {
                                        Node idk2 = sourceList.item(f);

                                        // Creating and pushing translation to target element
                                        // Saving segmentid to know where to push translation
                                        if (idk2.getNodeType() == Node.ELEMENT_NODE) {
                                            Element source = (Element) idk2;
                                            Element sourceElement = document.createElement(source.getTagName());
                                            sourceElement.setTextContent(source.getTextContent());


                                            // Getting total word count for translatable element original + translated
                                            String[] originalWordCount = source.getTextContent().split(" ");
                                            String[] translatedWordCount = translatedMap.get(segment.getAttribute("segmentId")).split(" ");
                                            HashMap<String, Long> originalCountHashMap = processString(originalWordCount);
                                            HashMap<String, Long> translatedCountHashMap = processString(translatedWordCount);
                                            translatableElement.setAttribute("wordcount", originalCountHashMap.size() + translatedCountHashMap.size() + "");



                                            sourceElement.setAttribute("segmentId", segment.getAttribute("segmentId"));
                                            Element targetElement = document.createElement("target");
                                            targetElement
                                                    .setTextContent(translatedMap.get(segment.getAttribute("segmentId")).trim());
                                            segmentElement.appendChild(sourceElement);
                                            segmentElement.appendChild(targetElement);
                                        }
                                    }
                                }
                            }

                        }
                    }

                    // Creating XML File
                    // Transfer from DOM to XML
                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    Transformer transformer = transformerFactory.newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                    DOMSource domSource = new DOMSource(document);
                    StreamResult streamResult = new StreamResult(
                            new File(getPath() + OUTPUT_FLAG));

                    transformer.transform(domSource, streamResult);


                    System.out.println("\n");
                    System.out.println("\n");
                    System.out.println("\n");
                    System.out.println("\n");
                    System.out.println("\n");
                    System.out.println("TRANSLATED VERSION OF TXML FILE HAS BEEN CREATED UNDER " + OUTPUT_FLAG + " NAME!");
                    System.out.println("\n");

                    if(REPORT){
                        printReport(UNIQUE_WORD_COUNT, TOTAL_WORD_COUNT);
                    }


                }

            }
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }

    }


    /**
     * @implNote This method prints -e report
     * @param words
     * @param totalWordCount
     */
    public static void printReport(HashMap<String, Long> words, int totalWordCount){
        String reportString = "";
        int counter = 0;

        System.out.println("-------------------UNIQUE WORD REPORT-----------------------------");
        for (Map.Entry mapElement : words.entrySet()) {
            if (counter == 6) {
                reportString += "\n" + "" + mapElement.getKey() + " : " + mapElement.getValue() + ", ";
                counter = 0;
                continue;
            }
            reportString += mapElement.getKey() + ": " + mapElement.getValue() + ", ";
            counter++;
        }
        System.out.println(reportString.substring(0, reportString.length()-1));
        System.out.println("\n");
        System.out.println("\n");
        System.out.println("Total words: " + totalWordCount);
        System.out.println("-------------------UNIQUE WORD REPORT-----------------------------");
        System.out.println("\n");
        System.out.println("\n");
    }
    /**
     * @implNote This method makes a HTTP request to WorldLingo API with original language text and returns translated version of text.
     * @param targetURL
     * @param urlParameters
     * @return String
     * @exception Exception
     */
    public static String getTranslation(String targetURL, String urlParameters) {
        HttpURLConnection connection = null;
        try {
            // Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");


            //get all headers

            connection.setRequestProperty("Content-Length",
                    Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            // Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.close();

            // Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();


            int status = connection.getResponseCode();


            // Checking the error code and exiting the application
            Integer header;
            if (status == HttpURLConnection.HTTP_OK) {
                header = Integer.parseInt(connection.getHeaderField("X-WL-ERRORCODE"));
                if(header != 0){
                    System.out.println("Error in translation. Wordlingo API responded with the error code:" + header);
                    System.exit(0);
                }
            }


            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}