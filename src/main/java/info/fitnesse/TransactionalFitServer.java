package info.fitnesse;

import fit.*;
import util.StreamReader;
import util.CommandLine;

import java.io.*;
import java.net.Socket;

public class TransactionalFitServer {
    public class DocumentRunner implements Runnable {
        private int size;

        public DocumentRunner(int size) {
            this.size = size;
        }

        public void run() {
            try {
                print("processing document of size: " + size + "\n");
                String document = FitProtocol.readDocument(socketReader, size);
                Parse tables = new Parse(document);
                newFixture().doTables(tables);
                print("\tresults: " + fixture.counts() + "\n");
            } catch (Exception e) {
                exception(e);
            }
        }
    }

    public Fixture fixture = new Fixture();

    public FixtureListener fixtureListener = new TablePrintingFixtureListener();

    private OutputStream socketOutput;

    private StreamReader socketReader;

    private boolean verbose = false;

    private String host;

    private int port;

    private int socketToken;

    private Socket socket;

    public static void main(String argv[]) throws Exception {
        TransactionalFitServer fitServer = new TransactionalFitServer();
        fitServer.run(argv);
        System.exit(fitServer.exitCode());
    }

    public void run(String argv[]) throws Exception {
        args(argv);
        establishConnection();
        validateConnection();
        process();
        closeConnection();
        exit();
    }

    public void closeConnection() throws IOException {
        socket.close();
    }

    public void process() {

        RollbackIntf rollbackProcessingBean = FitnesseSpringContext.getRollbackBean();
        fixture.listener = fixtureListener;
        try {
            int size;
            while ((size = FitProtocol.readSize(socketReader)) != 0) {
                try {
                    rollbackProcessingBean.process(new DocumentRunner(size));
                } catch (RollbackNow rn) {
                    print("rolling back now" + "\n");
                }
            }
            print("completion signal received" + "\n");
        } catch (Exception e) {
            exception(e);
        }
    }

    protected Fixture newFixture() {
        fixture = new Fixture();
        fixture.listener = fixtureListener;
        return fixture;
    }

    public void args(String[] argv) {
        CommandLine commandLine = new CommandLine("[-v] host port socketToken");
        if (commandLine.parse(argv)) {
            host = commandLine.getArgument("host");
            port = Integer.parseInt(commandLine.getArgument("port"));
            socketToken = Integer.parseInt(commandLine
                    .getArgument("socketToken"));
            verbose = commandLine.hasOption("v");
        } else
            usage();
    }

    private void usage() {
        System.out
                .println("usage: java test.RollbackServer [-v] host port socketTicket");
        System.out.println("\t-v\tverbose");
        System.exit(-1);
    }

    protected void exception(Exception e) {
        print("Exception occurred!" + "\n");
        print("\t" + e.getMessage() + "\n");
        Parse tables = new Parse("span", "Exception occurred: ", null, null);
        fixture.exception(tables, e);
        fixture.listener.tableFinished(tables);
        fixture.listener.tablesFinished(fixture.counts);
    }

    public void exit() throws Exception {
        print("exiting" + "\n");
        print("\tend results: " + fixture.counts.toString() + "\n");
    }

    public int exitCode() {
        return fixture.counts.wrong + fixture.counts.exceptions;
    }

    public void establishConnection() throws Exception {
        establishConnection(makeHttpRequest());
    }

    public void establishConnection(String httpRequest) throws Exception {
        socket = new Socket(host, port);
        socketOutput = socket.getOutputStream();
        socketReader = new StreamReader(socket.getInputStream());
        byte[] bytes = httpRequest.getBytes("UTF-8");
        socketOutput.write(bytes);
        socketOutput.flush();
        print("http request sent" + "\n");
    }

    private String makeHttpRequest() {
        return "GET /?responder=socketCatcher&ticket=" + socketToken
                + " HTTP/1.1\r\n\r\n";
    }

    public void validateConnection() throws Exception {
        print("validating connection...");
        int statusSize = FitProtocol.readSize(socketReader);
        if (statusSize == 0)
            print("...ok" + "\n");
        else {
            String errorMessage = FitProtocol.readDocument(socketReader,
                    statusSize);
            print("...failed because: " + errorMessage + "\n");
            System.out.println("An error occurred while connecting to client.");
            System.out.println(errorMessage);
            System.exit(-1);
        }
    }

    private void print(String message) {
        if (verbose)
            System.out.print(message);
    }

    public static byte[] readTable(Parse table) throws Exception {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        OutputStreamWriter streamWriter = new OutputStreamWriter(byteBuffer,
                "UTF-8");
        PrintWriter writer = new PrintWriter(streamWriter);
        Parse more = table.more;
        table.more = null;
        if (table.trailer == null)
            table.trailer = "";
        table.print(writer);
        table.more = more;
        writer.close();
        return byteBuffer.toByteArray();
    }

    class TablePrintingFixtureListener implements FixtureListener {
        public void tableFinished(Parse table) {
            try {
                byte[] bytes = readTable(table);
                if (bytes.length > 0)
                    FitProtocol.writeData(bytes, socketOutput);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void tablesFinished(Counts count) {
            try {
                FitProtocol.writeCounts(count, socketOutput);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
