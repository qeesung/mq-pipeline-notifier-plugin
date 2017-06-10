package org.jenkinsci.plugins.mqpipelinenotifier;

/**
 * Created by qeesung on 2017/6/10.
 */

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.PossibleAuthenticationFailureException;
import hudson.Extension;
import hudson.Plugin;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Adds the MQ notifier plugin configuration to the system config page.
 *
 * @author Örjan Percy &lt;orjan.percy@sonymobile.com&gt;
 */
@Extension
public final class MQPipelineNotifierConfig extends Plugin implements Describable<MQPipelineNotifierConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MQPipelineNotifierConfig.class);
    private final String[] schemes = {"amqp", "amqps"};
    private static final String SERVER_URI = "serverUri";
    private static final String USERNAME = "userName";
    private static final String PASSWORD = "userPassword";

    /* The MQ server URI */
    private String serverUri;
    private String userName;
    private Secret userPassword;

    /* The notifier plugin sends messages to an exchange which will push the messages to one or several queues.*/
    private String exchangeName;

    /* The virtual host which the connection intends to operate within. */
    private String virtualHost;

    /* Messages will be sent with a routing key which allows messages to be delivered to queues that are bound with a
     * matching binding key. The routing key must be a list of words, delimited by dots.
     */
    private String routingKey;
    /* Messages delivered to durable queues will be logged to disk if persistent delivery is set. */
    private boolean persistentDelivery;
    /* Application id that can be read by the consumer (optional). */
    private String appId;

    /**
     * Creates an instance with specified parameters.
     *
     * @param serverUri          the server uri
     * @param userName           the user name
     * @param userPassword       the user password
     * @param exchangeName       the name of the exchange
     * @param virtualHost        the name of the virtual host
     * @param routingKey         the routing key
     * @param persistentDelivery if using persistent delivery mode
     * @param appId              the application id
     */
    @DataBoundConstructor
    public MQPipelineNotifierConfig(String serverUri, String userName, Secret userPassword, String exchangeName,
                                    String virtualHost, String routingKey, boolean persistentDelivery, String appId) {
        this.serverUri = serverUri;
        this.userName = userName;
        this.userPassword = userPassword;
        this.exchangeName = exchangeName;
        this.virtualHost = virtualHost;
        this.routingKey = routingKey;
        this.persistentDelivery = persistentDelivery;
        this.appId = appId;
    }

    @Override
    public void start() throws Exception {
        super.start();
        LOGGER.info("Starting MQNotifier Plugin");
        load();
        MQPipelineConnection.getInstance().initialize(userName, userPassword, serverUri, virtualHost);
    }

    /**
     * Load configuration on invoke.
     */
    public MQPipelineNotifierConfig() {
        this.persistentDelivery = true; // default value
    }

    @Override
    public void configure(StaplerRequest req, JSONObject formData) throws IOException, ServletException,
            Descriptor.FormException {
        req.bindJSON(this, formData);
        save();
        MQPipelineConnection.getInstance().initialize(userName, userPassword, serverUri, virtualHost);
    }

    /**
     * Gets URI for MQ server.
     *
     * @return the URI.
     */
    public String getServerUri() {
        return this.serverUri;
    }

    /**
     * Sets URI for MQ server.
     *
     * @param serverUri the URI.
     */
    public void setServerUri(final String serverUri) {
        this.serverUri = StringUtils.strip(StringUtils.stripToNull(serverUri), "/");
    }

    /**
     * Gets user name.
     *
     * @return the user name.
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * Sets user name.
     *
     * @param userName the user name.
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Gets user password.
     *
     * @return the user password.
     */
    public Secret getUserPassword() {
        return this.userPassword;
    }

    /**
     * Sets user password.
     *
     * @param userPassword the user password.
     */
    public void setUserPassword(Secret userPassword) {
        this.userPassword = userPassword;
    }

    /**
     * Gets this extension's instance.
     * <p>
     * If {@link jenkins.model.Jenkins#getInstance()} isn't available
     * or the plugin class isn't registered null will be returned.
     *
     * @return the instance of this extension.
     */
    public static MQPipelineNotifierConfig getInstance() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            return jenkins.getPlugin(MQPipelineNotifierConfig.class);
        } else {
            LOGGER.error("Error, Jenkins could not be found, so no plugin!");
            return null;
        }
    }

    /**
     * Gets the exchange name.
     *
     * @return the exchange name.
     */
    public String getExchangeName() {
        return this.exchangeName;
    }

    /**
     * Sets the exchange name.
     *
     * @param exchangeName the exchange name.
     */
    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    /**
     * Gets the virtual host name.
     *
     * @return the virtual host name.
     */
    public String getVirtualHost() {
        return this.virtualHost;
    }

    /**
     * Sets the virtual host name.
     *
     * @param virtualHost the exchange name.
     */
    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    /**
     * Gets the routing key.
     *
     * @return the routing key.
     */
    public String getRoutingKey() {
        return this.routingKey;
    }

    /**
     * Sets the routing key.
     *
     * @param routingKey the routing key.
     */
    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    /**
     * Returns true if persistentDelivery is to be used.
     *
     * @return if persistentDelivery is to be used.
     */
    public boolean getPersistentDelivery() {
        return this.persistentDelivery;
    }

    /**
     * Sets persistent delivery mode.
     *
     * @param pd if persistentDelivery is to be used.
     */
    public void setPersistentDelivery(boolean pd) {
        this.persistentDelivery = pd;
    }

    /**
     * Returns application id.
     *
     * @return application id.
     */
    public String getAppId() {
        return this.appId;
    }

    /**
     * Sets application id.
     *
     * @param appId Application id to use
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }


    /**
     * Returns the descriptor instance.
     *
     * @return descriptor instance.
     */
    @Override
    public Descriptor<MQPipelineNotifierConfig> getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    /**
     * Implementation of the Descriptor interface.
     */
    @Extension
    public static final class DescriptorImpl extends Descriptor<MQPipelineNotifierConfig> {
        @Override
        public String getDisplayName() {
            return "MQ Notifier Plugin";
        }

        /**
         * Tests connection to the server URI.
         *
         * @param uri the URI.
         * @param name the user name.
         * @param pw the user password.
         * @return FormValidation object that indicates ok or error.
         * @throws javax.servlet.ServletException Exception for servlet.
         */
        public FormValidation doTestConnection(@QueryParameter(SERVER_URI) final String uri,
                                               @QueryParameter(USERNAME) final String name,
                                               @QueryParameter(PASSWORD) final Secret pw) throws ServletException {
            UrlValidator urlValidator = new UrlValidator(getInstance().schemes, UrlValidator.ALLOW_LOCAL_URLS);
            FormValidation result = FormValidation.ok();
            if (urlValidator.isValid(uri)) {
                try {
                    ConnectionFactory conn = new ConnectionFactory();
                    conn.setUri(uri);
                    if (StringUtils.isNotEmpty(name)) {
                        conn.setUsername(name);
                        if (StringUtils.isNotEmpty(Secret.toString(pw))) {
                            conn.setPassword(Secret.toString(pw));
                        }
                    }
                    conn.newConnection();
                } catch (URISyntaxException e) {
                    result = FormValidation.error("Invalid Uri");
                } catch (PossibleAuthenticationFailureException e) {
                    result = FormValidation.error("Authentication Failure");
                } catch (Exception e) {
                    result = FormValidation.error(e.getMessage());
                }
            } else {
                result = FormValidation.error("Invalid Uri");
            }
            return result;
        }
    }

}
