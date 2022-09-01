package org.apache.skywalking.junit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SKywalkingPluginTest {

    Dependency dependencies();

}
