package org.apache.skywalking.junit;

public @interface Dependency {

    String groupId();

    String artifactId();

    String[] versions();
}
