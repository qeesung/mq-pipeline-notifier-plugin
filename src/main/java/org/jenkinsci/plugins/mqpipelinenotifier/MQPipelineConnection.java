package org.jenkinsci.plugins.mqpipelinenotifier;

/**
 * Created by qeesung on 2017/6/10.
 */
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import hudson.util.Secret;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Creates an MQ connection.
 *
 * @author Örjan Percy &lt;orjan.percy@sonymobile.com&gt;
 */
public final class MQPipelineConnection implements ShutdownListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MQPipelineConnection.class);
    private static final int HEARTBEAT_INTERVAL = 30;
    private static final int MESSAGE_QUEUE_SIZE = 1000;
    private static final int SENDMESSAGE_TIMEOUT = 100;
    private static final int CONNECTION_WAIT = 10000;

    private String userName;
    private Secret userPassword;
    private String serverUri;
    private String virtualHost;
    private Connection connection = null;
    private Channel channel = null;

    private static LinkedBlockingQueue messageQueue;
    private static Thread messageQueueThread;

    /**
     * Lazy-loaded singleton using the initialization-on-demand holder pattern.
     */
    private MQPipelineConnection() { }

    /**
     * Is only executed on {@link #getInstance()} invocation.
     */
    private static class LazyRabbit {
        private static final MQPipelineConnection INSTANCE = new MQPipelineConnection();
        private static final ConnectionFactory CF = new ConnectionFactory();
    }

    /**
     * Gets the instance.
     *
     * @return the instance
     */
    public static MQPipelineConnection getInstance() {
        return LazyRabbit.INSTANCE;
    }

    /**
     * Stores data for a RabbitMQ message.
     */
    private static final class MessageData {
        private String exchange;
        private String routingKey;
        private AMQP.BasicProperties props;
        private byte[] body;

        /**
         * Constructor.
         *
         * @param exchange the exchange to publish the message to
         * @param routingKey the routing key
         * @param props other properties for the message - routing headers etc
         * @param body the message body
         */
        private MessageData(String exchange, String routingKey, AMQP.BasicProperties props, byte[] body) {
            this.exchange = exchange;
            this.routingKey = routingKey;
            this.props = props;
            this.body = body;
        }

        /**
         * Gets the exchange name.
         *
         * @return the exchange name
         */
        private String getExchange() {
            return exchange;
        }

        /**
         * Gets the routing key.
         *
         * @return the routing key
         */
        private String getRoutingKey() {
            return routingKey;
        }

        /**
         * Gets the connection properties.
         *
         * @return the connection properties
         */
        private AMQP.BasicProperties getProps() {
            return props;
        }

        /**
         * Gets the message body.
         *
         * @return the message body
         */
        private byte[] getBody() {
            return body;
        }
    }

    /**
     * Puts a message in the message queue.
     *
     * @param exchange the exchange to publish the message to
     * @param routingKey the routing key
     * @param props other properties for the message - routing headers etc
     * @param body the message body
     */
    public void addMessageToQueue(String exchange, String routingKey, AMQP.BasicProperties props, byte[] body) {
        if (messageQueue == null) {
            messageQueue = new LinkedBlockingQueue(MESSAGE_QUEUE_SIZE);
        }

        if (messageQueueThread == null || !messageQueueThread.isAlive()) {
            messageQueueThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    sendMessages();
                }
            });
            messageQueueThread.start();
            LOGGER.info("messageQueueThread recreated since it was null or not alive.");
        }

        MessageData messageData = new MessageData(exchange, routingKey, props, body);
        if (!messageQueue.offer(messageData)) {
            LOGGER.error("addMessageToQueue() failed, RabbitMQ queue is full!");
        }
    }

    /**
     * Sends messages from the message queue.
     */
    private void sendMessages() {
        while (true) {
            try {
                MessageData messageData = (MessageData)messageQueue.poll(SENDMESSAGE_TIMEOUT,
                        TimeUnit.MILLISECONDS);
                if (messageData != null) {
                    getInstance().send(messageData.getExchange(), messageData.getRoutingKey(),
                            messageData.getProps(), messageData.getBody());
                }
            } catch (InterruptedException ie) {
                LOGGER.info("sendMessages() poll() was interrupted: ", ie);
            }
        }
    }

    /**
     * Gets the connection factory that will enable a connection to the AMQP server.
     *
     * @return the connection factory
     */
    private ConnectionFactory getConnectionFactory() {
        if (LazyRabbit.CF != null) {
            try {
                // Try to recover the topology along with the connection.
                LazyRabbit.CF.setAutomaticRecoveryEnabled(true);
                // set requested heartbeat interval, in seconds
                LazyRabbit.CF.setRequestedHeartbeat(HEARTBEAT_INTERVAL);
                LazyRabbit.CF.setUri(serverUri);
                if (StringUtils.isNotEmpty(virtualHost)) {
                    LazyRabbit.CF.setVirtualHost(virtualHost);
                }
            } catch (KeyManagementException e) {
                LOGGER.error("KeyManagementException: ", e);
            } catch (NoSuchAlgorithmException e) {
                LOGGER.error("NoSuchAlgorithmException: ", e);
            } catch (URISyntaxException e) {
                LOGGER.error("URISyntaxException: ", e);
            }
            if (StringUtils.isNotEmpty(userName)) {
                LazyRabbit.CF.setUsername(userName);
                if (StringUtils.isNotEmpty(Secret.toString(userPassword))) {
                    LazyRabbit.CF.setPassword(Secret.toString(userPassword));
                }
            }
        }
        return LazyRabbit.CF;
    }

    /**
     * Gets the connection.
     *
     * @return the connection.
     */
    public Connection getConnection() {
        if (connection == null) {
            try {
                connection = getConnectionFactory().newConnection();
                connection.addShutdownListener(this);
            } catch (IOException e) {
                LOGGER.warn("Connection refused", e);
            }
        }
        return connection;
    }

    /**
     * Initializes this instance with supplied values.
     *
     * @param name the user name
     * @param password the user password
     * @param uri the server uri
     * @param vh the virtual host
     */
    public void initialize(String name, Secret password, String uri, String vh) {
        userName = name;
        userPassword = password;
        serverUri = uri;
        virtualHost = vh;
        connection = null;
        channel = null;
    }

    /**
     * Sends a message.
     * Keeps trying to get a connection indefinitely.
     *
     * @param exchange the exchange to publish the message to
     * @param routingKey the routing key
     * @param props other properties for the message - routing headers etc
     * @param body the message body
     */
    private void send(String exchange, String routingKey, AMQP.BasicProperties props, byte[] body) {
        if (exchange == null) {
            LOGGER.error("Invalid configuration, exchange must not be null.");
            return;
        }

        while (true) {
            try {
                if (channel == null || !channel.isOpen()) {
                    connection = getConnection();
                    if (connection != null) {
                        channel = connection.createChannel();
                        if (!getConnection().getAddress().isLoopbackAddress()) {
                            channel.exchangeDeclarePassive(exchange);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Cannot create channel", e);
                channel = null; // reset
            } catch (ShutdownSignalException e) {
                LOGGER.error("Cannot create channel", e);
                channel = null; // reset
                break;
            }
            if (channel != null) {
                try {
                    channel.basicPublish(exchange, routingKey, props, body);
                } catch (IOException e) {
                    LOGGER.error("Cannot publish message", e);
                } catch (AlreadyClosedException e) {
                    LOGGER.error("Connection is already closed", e);
                }

                break;
            } else {
                try {
                    Thread.sleep(CONNECTION_WAIT);
                } catch (InterruptedException ie) {
                    LOGGER.error("Thread.sleep() was interrupted", ie);
                }
            }
        }
    }

    @Override
    public void shutdownCompleted(ShutdownSignalException cause) {
        if (cause.isHardError()) {
            if (!cause.isInitiatedByApplication()) {
                LOGGER.warn("MQ connection was suddenly disconnected.");
                try {
                    if (connection != null && connection.isOpen()) {
                        connection.close();
                    }
                    if (channel != null && channel.isOpen()) {
                        channel.close();
                    }
                } catch (IOException e) {
                    LOGGER.error("IOException: ", e);
                } catch (AlreadyClosedException e) {
                    LOGGER.error("AlreadyClosedException: ", e);
                } finally {
                    channel = null;
                    connection = null;
                }
            }
        } else {
            LOGGER.warn("MQ channel was suddenly disconnected.");
        }
    }
}
