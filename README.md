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
