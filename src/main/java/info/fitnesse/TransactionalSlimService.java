package info.fitnesse;

import fitnesse.socketservice.SocketService;
import util.CommandLine;

import java.util.Arrays;

public class TransactionalSlimService extends SocketService {
    public static TransactionalSlimService instance;
    public static boolean verbose;
    public static int port;

    public static void main(String[] args) throws Exception {
        if (parseCommandLine(args)) {
            new TransactionalSlimService(port, verbose);
        } else {
            System.err.println("Invalid command line arguments:" + Arrays.asList(args));
        }
    }

    static boolean parseCommandLine(String[] args) {
        CommandLine commandLine = new CommandLine("[-v] port");
        if (commandLine.parse(args)) {
            verbose = commandLine.hasOption("v");
            String portString = commandLine.getArgument("port");
            port = Integer.parseInt(portString);
            return true;
        }
        return false;
    }

    public TransactionalSlimService(int port, boolean verbose) throws Exception {
        super(port, new TransactionalSlimServer(verbose));
        instance = this;
    }
}
