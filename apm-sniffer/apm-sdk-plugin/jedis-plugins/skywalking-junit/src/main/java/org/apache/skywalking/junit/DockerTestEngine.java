package org.apache.skywalking.junit;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class DockerTestEngine implements TestEngine {

    private JupiterTestEngine jupiterTestEngine;

    public DockerTestEngine() {
        this.jupiterTestEngine = new JupiterTestEngine();
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        return jupiterTestEngine.discover(discoveryRequest, uniqueId);
    }

    @Override
    public void execute(ExecutionRequest request) {
        List<ExecutionContent> executionContents = new ArrayList<>();
        Set<? extends TestDescriptor> children = request.getRootTestDescriptor().getChildren();
        Iterator<? extends TestDescriptor> iterator = children.iterator();
        if (iterator.hasNext()) {
            ClassTestDescriptor classTestDescriptor = (ClassTestDescriptor) iterator.next();

            Class<?> testClass = classTestDescriptor.getTestClass();
            SKywalkingPluginTest classTestPlugin = testClass.getAnnotation(SKywalkingPluginTest.class);
            //classpath
            String classPath = Objects.requireNonNull(testClass.getResource("/")).getPath();
            String className = testClass.getName();
            for (TestDescriptor descriptor : classTestDescriptor.getChildren()) {
                TestMethodTestDescriptor methodTestDescriptor = (TestMethodTestDescriptor) descriptor;
                Method testMethod = methodTestDescriptor.getTestMethod();
                SKywalkingPluginTest methodPlugin = testMethod.getAnnotation(SKywalkingPluginTest.class);
                SKywalkingPluginTest sKywalkingPluginTest = methodPlugin == null ? classTestPlugin : methodPlugin;
                if (sKywalkingPluginTest == null) {
                    return;
                }
                String methodName = testMethod.getName();
                try {
                    new MavenCachedExe(new ExecutionContent(classPath, className, methodName, sKywalkingPluginTest.dependencies())).exec();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        }

    }


    @Override
    public String getId() {
        return "org.apache.skywalking.junit";
    }

    @Override
    public Optional<String> getGroupId() {
        return Optional.of("org.apache.skywalking.junit");
    }

    @Override
    public Optional<String> getArtifactId() {
        return Optional.of("engine");
    }

    @Override
    public Optional<String> getVersion() {
        return Optional.of("aaaa");
    }


}
