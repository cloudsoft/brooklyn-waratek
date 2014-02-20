package com.example;

import java.net.ServerSocket;
import java.net.Socket;

public final class MemoryWebServer {

    /**
     * Simple single-threaded HTTP server that allocates memory based on
     * GET requests.
     *
     * Listens on all interfaces on port 8088 by default. A different port can
     * be specified by passing it as the first argument.
     *
     * URL Format:
     *      /Memory?add=100 - This will consume 100MB of memory.
     *      /Memory?remove=100 - This will remove 100MB of consumed memory.
     *      /Memory?clear=yes - This will remove all consumed memory.
     *
     * A 200 response code is returned when the URL is parsed successfully.
     * A 404 response code is returned when parsing fails.
     */
    public static void main(String argv[]) throws Exception {

        Memory mem = new Memory();

        // Listens on port 8088 by default
        int port = 8088;
        if ( argv.length > 0 )
            port = Integer.parseInt(argv[0]);

        // Establish the listen socket.
        ServerSocket WebSocket = new ServerSocket(port);
        while (true) {
            // Listen for a TCP connection request.
            Socket connectionSocket = WebSocket.accept();
            //Construct object to process HTTP request message
            HttpRequest request = new HttpRequest(connectionSocket);
            if (request.processRequest()) {
                if ( request.getOperation().equals("add") ) {
                    if ( request.getAmount() != 0 ) {
                        mem.addMemory(request.getAmount());
                    }
                }
                else if ( request.getOperation().equals("remove") ) {
                    if ( request.getAmount() != 0 ) {
                        try {
                            mem.removeMemory(request.getAmount());
                        } catch ( IndexOutOfBoundsException e ) {
                            // Do nothing
                        }
                    }
                }
                else if ( request.getOperation().equals("clear") ) {
                    if ( request.getClear() ) {
                        mem.releaseMemory();
                    }
                }
            }
        }
    }
}
