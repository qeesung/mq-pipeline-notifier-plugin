# mq-pipeline-notifier-plugin
[![Build Status](https://travis-ci.org/qeesung/mq-pipeline-notifier-plugin.svg?branch=master)](https://travis-ci.org/qeesung/mq-pipeline-notifier-plugin)

A Jenkins plugin that can send pipeline status message to RabbitMQ when pipeline status changed, such as stage changed, or step changed

## Installation

This plugin is not published to the jenkins official website, you need to install this plugin manually:

1. download latest jenkins plugin from [here](https://github.com/qeesung/mq-pipeline-notifier-plugin/releases)
2. upload this plugin to the jenkins (more detail -> [How to install a plugin in Jenkins manually?](https://stackoverflow.com/questions/14950408/how-to-install-a-plugin-in-jenkins-manually) )
    
## Usage

Config the rabbitmq address, username, password, exchange, and routing in global jenkins configurations page

![global config](https://github.com/qeesung/mq-pipeline-notifier-plugin/blob/master/images/global-config.png?raw=true)


## Example 

### Jenkinsfile
```groovy
node{
    stage("hello") {
        echo "hello"
    }
    
    stage("hi") {
        echo "hi"
    }
}
```

### RabbitMQ client

Here is a simple rabbit client write in nodejs

```nodejs
const amqp = require('amqplib/callback_api');
amqp.connect('amqp://username:password@localhost:32771', function(err, conn) {
  if(err) {
    console.log(err);
    return;
  }
  conn.createChannel(function(err, ch) {
    var ex = 'codeflow';

    ch.assertExchange(ex, 'fanout', {durable: true});

    ch.assertQueue('', {exclusive: true}, function(err, q) {
      console.log(" [*] Waiting for messages in %s. To exit press CTRL+C", q.queue);
      ch.bindQueue(q.queue, ex, '');

      ch.consume(q.queue, function(msg) {
        console.log(" [x] %s", msg.content.toString());
      }, {noAck: true});
    });
  });
});
```
### output message

```json
{
  "jenkins_event_uuid": "15fc01ee-bf8b-499e-994e-87541ecdf0bb",
  "jenkins_event": "pipeline_start",
  "jenkins_channel": "pipeline",
  "pipeline_run_id": "1",
  "pipeline_job_name": "greeting"
}
{
  "pipeline_step_flownode_id": "4",
  "jenkins_event_uuid": "cfb7bfed-6163-41fb-b164-f94ff710eef0",
  "jenkins_event": "pipeline_block_start",
  "jenkins_channel": "pipeline",
  "pipeline_step_name": "node",
  "pipeline_run_id": "1",
  "pipeline_job_name": "greeting",
  "pipeline_context": "4"
}
{
  "jenkins_event_uuid": "64b5b3b0-bf12-4623-8366-44426c73d927",
  "pipeline_job_name": "greeting",
  "pipeline_run_id": "1",
  "pipeline_step_name": "stage",
  "pipeline_step_stage_id": "6",
  "pipeline_step_flownode_id": "6",
  "pipeline_step_stage_name": "hello",
  "jenkins_event": "pipeline_stage",
  "pipeline_context": "4",
  "jenkins_channel": "pipeline"
}
{
  "jenkins_event_uuid": "df592bcd-ba4a-4efc-b336-c871fca2a738",
  "pipeline_job_name": "greeting",
  "pipeline_run_id": "1",
  "pipeline_step_name": "echo",
  "pipeline_step_stage_id": "6",
  "pipeline_step_is_paused": "false",
  "pipeline_step_flownode_id": "7",
  "pipeline_step_stage_name": "hello",
  "jenkins_event": "pipeline_step",
  "pipeline_context": "4/6",
  "jenkins_channel": "pipeline"
}
{
  "jenkins_event_uuid": "2b705d4d-8f98-4ce6-a3b1-18aab6ef5e77",
  "pipeline_job_name": "greeting",
  "pipeline_run_id": "1",
  "pipeline_step_name": "stage",
  "pipeline_step_stage_id": "6",
  "pipeline_step_flownode_id": "8",
  "pipeline_step_stage_name": "hello",
  "jenkins_event": "pipeline_block_end",
  "pipeline_context": "4/6",
  "jenkins_channel": "pipeline"
}
{
  "jenkins_event_uuid": "ca1af3fe-489a-45c0-8a37-b055ac4a8719",
  "pipeline_job_name": "greeting",
  "pipeline_run_id": "1",
  "pipeline_step_name": "stage",
  "pipeline_step_stage_id": "11",
  "pipeline_step_flownode_id": "11",
  "pipeline_step_stage_name": "hi",
  "jenkins_event": "pipeline_stage",
  "pipeline_context": "",
  "jenkins_channel": "pipeline"
}
{
  "jenkins_event_uuid": "0b9800da-8ea2-4f1f-9364-70063d6418a2",
  "pipeline_job_name": "greeting",
  "pipeline_run_id": "1",
  "pipeline_step_name": "echo",
  "pipeline_step_stage_id": "11",
  "pipeline_step_is_paused": "false",
  "pipeline_step_flownode_id": "12",
  "pipeline_step_stage_name": "hi",
  "jenkins_event": "pipeline_step",
  "pipeline_context": "11",
  "jenkins_channel": "pipeline"
}
{
  "jenkins_event_uuid": "0fae018b-6af1-4fc1-a7bc-fd66f4fc7c3f",
  "pipeline_job_name": "greeting",
  "pipeline_run_id": "1",
  "pipeline_step_name": "stage",
  "pipeline_step_stage_id": "11",
  "pipeline_step_flownode_id": "13",
  "pipeline_step_stage_name": "hi",
  "jenkins_event": "pipeline_block_end",
  "pipeline_context": "11",
  "jenkins_channel": "pipeline"
}
{
  "jenkins_event_uuid": "1eef8663-3ba4-4bde-8ed9-fcc66d5384d9",
  "pipeline_job_name": "greeting",
  "pipeline_run_id": "1",
  "pipeline_step_name": "node",
  "pipeline_step_stage_id": "11",
  "pipeline_step_flownode_id": "15",
  "pipeline_step_stage_name": "hi",
  "jenkins_event": "pipeline_block_end",
  "pipeline_context": "4",
  "jenkins_channel": "pipeline"
}
{
  "jenkins_event_uuid": "a370d1de-b296-4689-b5c2-c02837152f20",
  "jenkins_event": "pipeline_end",
  "jenkins_channel": "pipeline",
  "pipeline_run_id": "1",
  "pipeline_job_name": "greeting"
}
```