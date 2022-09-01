package org.apache.skywalking.junit;

public class InvokerMain {
    private ExecutionContent executionContent;

    public  void  exec(){
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("java");
        processBuilder.command("-javaagent");
        processBuilder.command("-cp");
        processBuilder.command(executionContent.getClasspath());
        processBuilder.command("Main");





    }


}
