package server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

public class JavaHTTPServer {

    // file prepared
    static final File WEB_ROOT = new File(".");
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final String METHOD_NOT_SUPPORTED = "notSupported.html";

    // port to listen connection
    static final int PORT = 8080;

    // JavaHTTPServer.VERBOSE mode
    static final boolean VERBOSE = true;

    public static void main(String[] args) {
        try {
            try (ServerSocket serverListen = new ServerSocket(PORT)) {
                System.out.println("Server started.\n");
                System.out.println("Listening for connections on port : " + PORT + " ...\n");

                // listen until user halts server execution
                while (true) {
                    HandlerAsVirtualServer virtualServerForConnectedClient = new HandlerAsVirtualServer(
                            serverListen.accept());

                    if (JavaHTTPServer.VERBOSE) {
                        System.out.println("Connecton opened. (" + new Date() + ")");
                    }
                    
                    // create dedicated thread to manage the client connection
                    Thread thread = new Thread(virtualServerForConnectedClient);
                    thread.start();
                    System.out.println("why twice\n");
                }
            }

        } catch (IOException e) {
            System.err.println("Server Connection error : " + e.getMessage());
        }
    }

    public static byte[] readFileDataInServer(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }

        return fileData;
    }

    // return supported MIME Types
    public static String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".htm") || fileRequested.endsWith(".html"))
            return "text/html";
        else
            return "text/plain";
    }

}

// Each Client Connection will be managed in a dedicated Thread
class HandlerAsVirtualServer implements Runnable {

    // Client Connection via Socket Class
    private Socket connect;

    HandlerAsVirtualServer(Socket c) {
        connect = c;
    }

    @Override
    public void run() { // manage our particular client connection

        try(// creat IO stream
            // read characters from the client via input stream on the socket
                BufferedReader in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            // get character output stream to client (for headers)
                PrintWriter headerOut = new PrintWriter(connect.getOutputStream());
            // get binary output stream to client (for requested data)
                BufferedOutputStream dataOut = new BufferedOutputStream(connect.getOutputStream())) {
                
            // analyse the request
            // get first line of the request from the client
            String input = in.readLine();
            // parse the request with a string tokenizer
            StringTokenizer parse = new StringTokenizer(input);
            String method = parse.nextToken().toUpperCase(); // get the HTTP method of query from the client
            String fileRequested = parse.nextToken().toLowerCase(); // get file name requested

            // case1: not support the requested method
            // support only GET and HEAD methods, check the query
            if (!method.equals("GET") && !method.equals("HEAD")) {

                if (JavaHTTPServer.VERBOSE) {
                    System.out.println("501 Not Implemented : " + method + " method.");
                }

                // return the notSupported file to the client
                // mime type of the file
                String contentMimeType = "text/html";
                // get data of the file
                File file = new File(JavaHTTPServer.WEB_ROOT, JavaHTTPServer.METHOD_NOT_SUPPORTED);
                int fileLength = (int) file.length();
                byte[] fileData = JavaHTTPServer.readFileDataInServer(file, fileLength);

                // send HTTP Headers with data to client
                // header
                headerOut.println("HTTP/1.1 501 Not Implemented");
                headerOut.println("Server: Java HTTP Server from Zhao Qihan : 1.0");
                headerOut.println("Date: " + new Date());
                headerOut.println("Content-type: " + contentMimeType);
                headerOut.println("Content-length: " + fileLength);
                headerOut.println(); // blank line between headers and content, very important !
                headerOut.flush(); // flush character output stream buffer
                // file
                dataOut.write(fileData, 0, fileLength);
                dataOut.flush();

            }
            else { 
            // case2: method supported && file exists
                try {   // GET or HEAD method
                    // end with '/' means the root
                    if (fileRequested.endsWith("/")) {
                        fileRequested += JavaHTTPServer.DEFAULT_FILE;
                    }

                    File file = new File(JavaHTTPServer.WEB_ROOT, fileRequested);
                    int fileLength = (int) file.length();
                    String content = JavaHTTPServer.getContentType(fileRequested);

                    if (method.equals("GET")) { // GET method so we return content
                        
                        // send HTTP Headers
                        headerOut.println("HTTP/1.1 200 OK");
                        headerOut.println("Server: Java HTTP Server from Zhao Qihan : 1.0");
                        headerOut.println("Date: " + new Date());
                        headerOut.println("Content-type: " + content);
                        headerOut.println("Content-length: " + fileLength);
                        headerOut.println("Connection: Keep-Alive");
                        
                        headerOut.println(); // blank line between headers and content, very important !
                        headerOut.flush(); // flush character output stream buffer
                        
                        // send HTTP Body
                        byte[] fileData = JavaHTTPServer.readFileDataInServer(file, fileLength);
                        dataOut.write(fileData, 0, fileLength);
                        dataOut.flush();

                        if (JavaHTTPServer.VERBOSE) {
                            System.out.println("File " + fileRequested + " of type " + content + " returned" + "\n");
                        }

                    }else if(method.equals("HEAD")) { // GET method so we return content

                        // send HTTP Headers
                        headerOut.println("HTTP/1.1 200 OK");
                        headerOut.println("Server: Java HTTP Server from Zhao Qihan : 1.0");
                        headerOut.println("Date: " + new Date());
                        headerOut.println("Content-type: " + content);
                        headerOut.println("Content-length: " + fileLength);
                        headerOut.println("Connection: Keep-Alive");
                        
                        headerOut.println(); // blank line between headers and content, very important !
                        headerOut.flush(); // flush character output stream buffer

                        if (JavaHTTPServer.VERBOSE) {
                            System.out.println("Head of File " + fileRequested + " of type " + content + " returned" + "\n");
                        }

                    }

                    
                } 
            // case3: method supported && file notFound
                catch (FileNotFoundException fnfe) {
                    try {
                        File file = new File(JavaHTTPServer.WEB_ROOT, JavaHTTPServer.FILE_NOT_FOUND);
                        int fileLength = (int) file.length();
                        String content = "text/html";
                        byte[] fileData = JavaHTTPServer.readFileDataInServer(file, fileLength);

                        headerOut.println("HTTP/1.1 404 File Not Found");
                        headerOut.println("Server: Java HTTP Server from Zhao Qihan : 1.0");
                        headerOut.println("Date: " + new Date());
                        headerOut.println("Content-type: " + content);
                        headerOut.println("Content-length: " + fileLength);
                        headerOut.println(); // blank line between headers and content, very important !
                        headerOut.flush(); // flush character output stream buffer

                        dataOut.write(fileData, 0, fileLength);
                        dataOut.flush();

                        if (JavaHTTPServer.VERBOSE) {
                            System.out.println("File " + fileRequested + " not found");
                        }
                    } catch (IOException ioe) {
                        System.err.println("Error with file not found exception : " + ioe.getMessage());
                    }
                }

            }
        } catch (IOException ioe) {
                System.err.println("Server error : " + ioe);
        } finally {
            try {
                connect.close(); // close socket connection
                if (JavaHTTPServer.VERBOSE) {
                    System.out.println("Connection closed.\n");
                }
            } catch (Exception e) {
                System.err.println("Error closing stream : " + e.getMessage());
            }

        }

    }

}