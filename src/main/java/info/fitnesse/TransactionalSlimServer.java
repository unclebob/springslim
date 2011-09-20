package info.fitnesse;

import fitnesse.slim.*;
import fitnesse.socketservice.SocketServer;
import util.StreamReader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.List;

public class TransactionalSlimServer implements SocketServer {
    private StreamReader reader;
    private BufferedWriter writer;
    private ListExecutor executor;
    private boolean verbose;
    private RollbackIntf rollbackProcessingBean;

    public TransactionalSlimServer(boolean verbose) {
        this.verbose = verbose;
    }

    public void serve(Socket s) {
        try {
            rollbackProcessingBean = FitnesseSpringContext.getRollbackBean();
        } catch (Throwable e) {
            e.printStackTrace();
            closeEnclosingServiceInSeparateThread();
        }
        try {
            tryProcessInstructions(s);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            close();
            closeEnclosingServiceInSeparateThread();
        }
    }

    private void closeEnclosingServiceInSeparateThread() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    TransactionalSlimService.instance.close();
                } catch (Exception e) {

                }
            }
        }
        ).start();
    }

    private void tryProcessInstructions(Socket s) throws Exception {
        initialize(s);
        boolean more = true;
        while (more) {
            if (verbose) System.err.println("processing instruction set");
            more = processOneSetOfInstructions();
            if (verbose) System.err.println("instruction set processed");
        }
    }

    private void initialize(Socket s) throws IOException {
        try {
            executor = new JavaSlimFactory().getListExecutor(verbose);
            reader = new StreamReader(s.getInputStream());
            writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            writer.write(String.format("Slim -- %s\n", SlimVersion.VERSION));
            writer.flush();
        }
        catch (Exception e) {
            System.err.println("fail! " + e.getMessage());
        }
    }

    private boolean processOneSetOfInstructions() throws Exception {
        String instructions = getInstructionsFromClient();
        return instructions == null || processTheInstructions(instructions);
    }

    private class InstructionRunner implements Runnable {
        private String instructions;

        public InstructionRunner(String instructions) {
            this.instructions = instructions;
        }

        public void run() {
            List<Object> results = executeInstructions(instructions);
            try {
                sendResultsToClient(results);
            } catch (IOException iex) {
                throw new Error(iex);
            }
        }
    }

    private boolean processTheInstructions(String instructions) throws IOException {
        if (instructions.equalsIgnoreCase("bye")) {
            return false;
        }
        else {
            try {
                rollbackProcessingBean.process(new InstructionRunner(instructions));
            }
            catch (RollbackNow rn) {
                if (verbose) System.err.println("rolling back now" + "\n");
            }
            return true;
        }
    }

    private String getInstructionsFromClient() throws Exception {
        int instructionLength = Integer.parseInt(reader.read(6));
        reader.read(1);
        return reader.read(instructionLength);
    }

    private List<Object> executeInstructions(String instructions) {
        List<Object> statements = ListDeserializer.deserialize(instructions);
        return executor.execute(statements);
    }

    private void sendResultsToClient(List<Object> results) throws IOException {
        String resultString = ListSerializer.serialize(results);
        writer.write(String.format("%06d:%s", resultString.length(), resultString));
        writer.flush();
    }

    private void close() {
        try {
            reader.close();
            writer.close();
        }
        catch (Exception e) {

        }
    }
}
