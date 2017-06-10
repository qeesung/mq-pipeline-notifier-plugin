package org.jenkinsci.plugins.mqpipelinenotifier.MQPipelineNotifierConfig

/**
 * Created by qeesung on 2017/6/10.
 */

def f = namespace("/lib/form")
def l = "/plugin/mq-notifier/"

f.section(title: "MQ Pipeline Notifier") {

    f.entry(title: "MQ URI", field: "serverUri", help: l+"help-amqp-uri.html") {
        f.textbox("value":my.serverUri)
    }
    f.entry(title: "User name", field: "userName", help: l+"help-user-name.html") {
        f.textbox("value":my.userName)
    }
    f.entry(title: "Password", field: "userPassword", help: l+"help-user-password.html") {
        f.password("value":my.userPassword)
    }
    descriptor = my.descriptor
    f.validateButton(title: "Test Connection", progress: "Trying to connect...", method: "testConnection",
            with: "serverUri,userName,userPassword")

    f.entry(title: "Exchange Name", field: "exchangeName", help: l+"help-exchange-name.html") {
        f.textbox("value":my.exchangeName)
    }
    f.entry(title: "Virtual host", field: "virtualHost", help: l+"help-virtual-host.html") {
        f.textbox("value":my.virtualHost)
    }
    f.entry(title: "Routing Key", field: "routingKey", help: l+"help-routing-key.html") {
        f.textbox("value":my.routingKey)
    }
    f.entry(title: "Application Id", field: "appId", help: l+"help-application-id.html") {
        f.textbox("value":my.appId)
    }
    f.entry(title: "Persistent Delivery mode", help: l+"help-persistent-delivery.html") {
        f.checkbox(field: "persistentDelivery", checked: my.persistentDelivery)
    }
}

