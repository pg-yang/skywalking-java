package org.apache.skywalking.junit;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

public class MavenCachedExe {

    private ExecutionContent executionContent;

    public MavenCachedExe(ExecutionContent executionContent) {
        this.executionContent = executionContent;
    }

    public void exec() throws Exception {
        String home = System.getenv("HOME");
        Dependency dependencies = executionContent.getDependencies();
        for (String version : dependencies.versions()) {
            String dependencyLibrary = String.format(home + "/.m2/repository/%s/%s/%s/%s-%s.jar", dependencies.groupId().replace(".", "/"), dependencies.artifactId(), version, dependencies.artifactId(), version);
            String mainPath = home + "/ideaProjects/skywalking-java/apm-sniffer/apm-sdk-plugin/jedis-plugins/skywalking-junit/target/classes";
            String classPath = String.format("%s:%s:%s", dependencyLibrary, executionContent.getClasspath(), mainPath);
            InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("Main.class");

            byte[] bytes = new byte[resourceAsStream.available()];
            IOUtils.read(resourceAsStream, bytes);
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.inheritIO();
            processBuilder.command(
                    "java",
                    "-cp", classPath,
                    "Main", executionContent.getClassName(), executionContent.getMethodName());

            Process process = processBuilder.start();
            inheritIO(process.getInputStream(), System.out);
            System.out.println("classpath -> " + classPath);
            System.out.println(process.waitFor());
        }

    }

    private static void inheritIO(final InputStream src, final PrintStream dest) {
        new Thread(new Runnable() {
            public void run() {
                Scanner sc = new Scanner(src);
                while (sc.hasNextLine()) {
                    dest.println(sc.nextLine());
                }
            }
        }).start();
    }

}
