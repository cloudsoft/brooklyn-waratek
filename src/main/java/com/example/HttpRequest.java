package com.example;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HttpRequest{

    private final static String CRLF = "\r\n";
    private final String MEMORY_TYPE = "Memory";
    private final String ADD_OP = "add";
    private final String REMOVE_OP = "remove";
    private final String CLEAR_OP = "clear";

    private Socket socket;
    private DataOutputStream os;
    private BufferedReader br;
    private InputStream is;

    private String type;
    private String operation;
    private int amount;
    private boolean clear;

    // Constructor
    public HttpRequest(Socket socket) {
        this.socket = socket;
    }

    public boolean processRequest() throws IOException {
        is = socket.getInputStream();
        os = new DataOutputStream(socket.getOutputStream());
        br = new BufferedReader(new InputStreamReader(is));

        String requestLine = br.readLine();
        StringTokenizer tokens = new StringTokenizer(requestLine);

        String requestType = tokens.nextToken();
        String requestURL = tokens.nextToken();
        String requestProto = tokens.nextToken();

        type = getType( requestURL );
        if ( type == null || ! type.equals(MEMORY_TYPE) )
            return error( "Couldn't parse the type from: " + requestURL );

        operation = getOperation( requestURL );
        if (operation == null ||
            ! operation.equals(ADD_OP) &&
            ! operation.equals(REMOVE_OP) ) {
            clear = getClear( requestURL );
            if ( ! clear )
                return error(
                    "Couldn't parse the operation from: " + requestURL );
        }

        amount = getAmount( requestURL );
        if ( amount == 0 && ! clear )
            return error ( "Couldn't parse the amount from: " + requestURL );

        return success();
    }

    public String getOperation() {
        return operation;
    }

    public String getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public boolean getClear() {
        return clear;
    }

    private String getType( String url ) {
        return parse(url, "\\w*(?=\\?)");
    }

    private String getOperation( String url ) {
        return parse(url, "\\w*(?=\\=)");
    }

    private int getAmount( String url ) {
        int amount = 0;
        try {
            return Integer.parseInt(parse(url, "\\d*$"));
        } catch (NumberFormatException e) {
            return amount;
        }
    }

    private boolean getClear( String url ) {
        String res = parse( url, "clear=yes$" );
        if ( res != null && res.equals("clear=yes") )
            return true;
        else
            return false;
    }

    private String parse( String input, String regex ) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(input);
        if (m.find())
            return m.group(0);
        else
            return null;
    }

    private boolean success() throws IOException {
        return success( "Success" );
    }

    private boolean success( String msg ) throws IOException {
        sendResponse( msg, true );
        return true;
    }

    private boolean error( String msg ) throws IOException {
        sendResponse( msg, false );
        return false;
    }

    private void sendResponse( String msg, boolean success )
        throws IOException {
        String statusLine, contentTypeLine, entityBody;
        if ( success ) {
            statusLine = "HTTP/1.1 200 OK: ";
            contentTypeLine = "Content-Type: text/html" + CRLF;
            entityBody = "<html>" + "<head><title>Success</title><body>"
                + msg + "</body></head></html>";
        }
        else {
            statusLine = "HTTP/1.1 404 Not Found: ";
            contentTypeLine = "Content-Type: text/html" + CRLF;
            entityBody = "<html>" + "<head><title>Error</title></head><body>"
                + msg + "</body></html>";
        }

        os.writeBytes(statusLine);
        os.writeBytes(contentTypeLine);
        os.writeBytes(CRLF);
        os.writeBytes(entityBody);

        //Close streams and socket.
        os.close();
        br.close();
        is.close();
        socket.close();
    }

}
