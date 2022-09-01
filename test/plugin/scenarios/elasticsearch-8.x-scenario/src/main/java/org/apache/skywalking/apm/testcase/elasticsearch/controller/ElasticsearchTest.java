package org.apache.skywalking.apm.testcase.elasticsearch.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.CreateRequest;

public class ElasticsearchTest {

    private final ElasticsearchClient elasticsearchClient;

    public ElasticsearchTest(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }


    public void execTest() {
        CreateRequest<SkywalkingDoc> createRequest = new CreateRequest.Builder().build();
        elasticsearchClient.create()

    }







}
