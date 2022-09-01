package org.apache.skywalking.junit;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ExecutionContent {
    private String classpath;
    private String className;
    private String methodName;
    private Dependency dependencies;
}
