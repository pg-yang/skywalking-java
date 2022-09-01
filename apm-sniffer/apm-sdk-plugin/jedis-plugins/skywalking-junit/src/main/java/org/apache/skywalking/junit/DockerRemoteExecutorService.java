package org.apache.skywalking.junit;

import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutorService;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class DockerRemoteExecutorService implements HierarchicalTestExecutorService {

    private ExecutionRequest request;

    public DockerRemoteExecutorService(ExecutionRequest request) {
        this.request = request;
    }

    private ArrayBlockingQueue<TestTask> blockingQueue = new ArrayBlockingQueue<TestTask>(100);

    @Override
    public Future<Void> submit(TestTask testTask) {
        blockingQueue.offer(testTask);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void invokeAll(List<? extends TestTask> testTasks) {
        return;
    }

    private boolean executeTaskOnRemote(TestTask testTask) {
        return false;
    }


    @Override
    public void close() {

    }
}
