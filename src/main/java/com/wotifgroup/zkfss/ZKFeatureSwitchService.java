package com.wotifgroup.zkfss;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * ZKFeatureSwitchService (zkfss) is a feature switch service implementation based on Apache Zookeeper (using Netflix Curator API).
 * <p>
 * See the README.md for more details on zkfss (https://github.com/lazjen/zkfss).
 * 
 * @author lazjen
 *
 */
public class ZKFeatureSwitchService implements FeatureSwitchService {

    private static final String DEFAULT_FEATURE_SWITCH_NAMESPACE = "/zkfss/";

    private static final int DEFAULT_CONNECTION_TIMEOUT_MILLIS = 30000;
    private static final String DEFAULT_CONNECT_STRING = "localhost:2181";

    private boolean running = false;

    private CuratorFramework client;
    private RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    private String connectString = DEFAULT_CONNECT_STRING;
    private int connectionTimeoutMillis = DEFAULT_CONNECTION_TIMEOUT_MILLIS;

    private boolean useHostnameSubKey = true;
    private String hostname;
    private String applicationName;
    private String featureSwitchNamespace = DEFAULT_FEATURE_SWITCH_NAMESPACE;

    private Map<String, Boolean> featureValues = new HashMap<String, Boolean>();
    private Map<String, NodeCache> nodeCaches = new HashMap<String, NodeCache>();

    /**
     * Set a CuratorFramework for the system. If you set this to null, the service will create its own CuratorFramework.
     * <P>
     * NOTE: Service must not be running.
     * 
     * @param client
     *            CuratorFramework to use.
     * @return this service
     */
    public ZKFeatureSwitchService setCuratorFrameworkClient(CuratorFramework client) {
        ensureServiceIsNotRunning();
        this.client = client;
        return this;
    }

    /**
     * Returns the CuratorFramework used in this service which can be useful if you want to use a framework created by the
     * service.
     * 
     * @return the CuratorFramework used in this service
     */
    public CuratorFramework getCuratorFrameworkClient() {
        return client;
    }

    /**
     * Set the connection retry policy for the CuratorFramework. This is only used if the service creates the CuratorFramework. <br>
     * Default: ExponentialBackoffRetry(1000, 3) NOTE: Service must not be running.
     * <P>
     * NOTE: Service must not be running.
     * 
     * @param retryPolicy
     *            connection retry policy for the CuratorFramework
     * @return this service
     */
    public ZKFeatureSwitchService setCuratorFrameworkConnectionRetryPolicy(RetryPolicy retryPolicy) {
        ensureServiceIsNotRunning();
        this.retryPolicy = retryPolicy;
        return this;
    }

    /**
     * Allows the feature switch name to be overridden by a sub-key based on the hostname. This is on by default. <br>
     * If your feature switch is "X", your hostname is "Y" and you are using the default namespace ("/zkfss/"), then the hostname
     * override value is is found at "/zkfss/X/Y". If this value is set, it has precedence over the feature switch value at
     * "/zkfss/X".
     * <P>
     * NOTE: Service must not be running.
     * 
     * @return this service
     */
    public ZKFeatureSwitchService enableHostnameSubKey() {
        ensureServiceIsNotRunning();
        useHostnameSubKey = true;
        return this;
    }

    /**
     * Disables the hostname sub-key override capability. By default hostname sub-key override is ON.
     * <P>
     * NOTE: Service must not be running.
     * 
     * @return this service
     */
    public ZKFeatureSwitchService disableHostnameSubKey() {
        ensureServiceIsNotRunning();
        useHostnameSubKey = false;
        return this;
    }

    /**
     * Set the zookeeper connection string to use in the CuratorFramework. This is only used if the service creates the
     * CuratorFramework.
     * <P>
     * Default: localhost:2181
     * <P>
     * NOTE: Service must not be running.
     * 
     * @param connectString
     *            The zookeeper connection string
     * @return this service
     */
    public ZKFeatureSwitchService setConnectString(String connectString) {
        ensureServiceIsNotRunning();
        this.connectString = connectString;
        return this;
    }

    /**
     * Set the zookeeper connection timeout (in millis) to use in the CuratorFramework. This is only used if the service creates
     * the CuratorFramework.
     * <P>
     * Default: 30000 ms
     * <P>
     * NOTE: Service must not be running.
     * 
     * @param connectionTimeoutMillis
     *            the connection timeout (in millis) to use when connecting to zookeeper
     * @return this service
     */
    public ZKFeatureSwitchService setConnectionTimeoutMillis(int connectionTimeoutMillis) {
        ensureServiceIsNotRunning();
        this.connectionTimeoutMillis = connectionTimeoutMillis;
        return this;
    }

    /**
     * Set the application name to use as a sub-key for a potential feature switch value override. You should make this be unique
     * per application instance. By default this name is not set and therefore, not used.<br>
     * If your feature switch is "X", your application name is "A" and you are using the default namespace ("/zkfss/"), then the
     * hostname override value is is found at "/zkfss/X/A". This value, if set, will take precedence over hostname override (if
     * on).
     * <P>
     * Default: Not set
     * <P>
     * NOTE: Service must not be running.
     * 
     * @param applicationName
     *            Application name to use as a sub-key.
     * @return this service
     */
    public ZKFeatureSwitchService setApplicationName(String applicationName) {
        ensureServiceIsNotRunning();
        this.applicationName = applicationName;
        return this;
    }

    /**
     * Set the namespace to use for feature switches. If the namespace supplied does not start and end with a "/", these are
     * added.
     * <P>
     * Default: /zkfss/
     * <P>
     * NOTE: Service must not be running.
     * 
     * @param featureSwitchNamespace
     *            Namespace for feature switches
     * @return this service
     */

    public ZKFeatureSwitchService setFeatureSwitchNamespace(String featureSwitchNamespace) {
        ensureServiceIsNotRunning();
        this.featureSwitchNamespace = featureSwitchNamespace;
        if (this.featureSwitchNamespace == null) {
            this.featureSwitchNamespace = DEFAULT_FEATURE_SWITCH_NAMESPACE;
        }
        if (!this.featureSwitchNamespace.endsWith("/")) {
            this.featureSwitchNamespace = this.featureSwitchNamespace + "/";
        }
        if (!this.featureSwitchNamespace.startsWith("/")) {
            this.featureSwitchNamespace = "/" + this.featureSwitchNamespace;
        }
        return this;
    }

    /**
     * Start this service. Must be called before any use of isEnabled(). Should be called after all configuration details have
     * been set.
     * 
     * @return this service
     */
    public ZKFeatureSwitchService start() {
        if (running) {
            return this;
        }
        if (client == null) {
            client =
                    CuratorFrameworkFactory.builder().connectString(connectString).retryPolicy(retryPolicy)
                            .connectionTimeoutMs(connectionTimeoutMillis).build();
            client.start();
        }

        if (useHostnameSubKey) {
            try {
                hostname = java.net.InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }

        running = true;

        return this;
    }

    private void ensureServiceIsNotRunning() {
        if (running) {
            throw new IllegalStateException("Configuration changes to running ZKFeatureSwitchService are not allowed!");
        }
    }

    /**
     * Stop the service. This will close the CuratorFramework.
     */
    public void stop() {
        running = false;
        client.close();
        client = null;
    }

    public boolean isEnabled(String key) {

        if (!running) {
            throw new IllegalStateException("ZKFeatureSwitchService not running!");
        }

        if (applicationName != null && useHostnameSubKey) {
            String keyForApplicationName = featureSwitchNamespace + key + "/" + applicationName + "/" + hostname;

            if (featureValues.containsKey(keyForApplicationName)) {
                return featureValues.get(keyForApplicationName);
            } else {
                if (!nodeCaches.containsKey(keyForApplicationName)) {
                    // create watch on node
                    Boolean result = setupNodeWatch(keyForApplicationName);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }

        if (applicationName != null) {
            String keyForApplicationName = featureSwitchNamespace + key + "/" + applicationName;

            if (featureValues.containsKey(keyForApplicationName)) {
                return featureValues.get(keyForApplicationName);
            } else {
                if (!nodeCaches.containsKey(keyForApplicationName)) {
                    // create watch on node
                    Boolean result = setupNodeWatch(keyForApplicationName);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }

        if (useHostnameSubKey) {
            String keyForHostname = featureSwitchNamespace + key + "/" + hostname;

            if (featureValues.containsKey(keyForHostname)) {
                return featureValues.get(keyForHostname);
            } else {
                if (!nodeCaches.containsKey(keyForHostname)) {
                    // create watch on node
                    Boolean result = setupNodeWatch(keyForHostname);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }

        String fsKey = featureSwitchNamespace + key;
        if (featureValues.containsKey(fsKey)) {
            return featureValues.get(fsKey);
        } else {
            if (!nodeCaches.containsKey(fsKey)) {
                // create watch on node
                Boolean result =  setupNodeWatch(fsKey);
                if (result != null) {
                    return result;
                }
            }
        }

        return false;
    }

    private Boolean setupNodeWatch(final String key) {
        final NodeCache nc = new NodeCache(client, key);
        nodeCaches.put(key, nc);

        try {
            nc.start(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        nc.getListenable().addListener(new NodeCacheListener() {

            public void nodeChanged() throws Exception {
                cacheCurrentValue(key, nc);
            }
        });

        // ensure the current value is setup if it's set
        return cacheCurrentValue(key, nc);
    }

    private Boolean cacheCurrentValue(final String key, final NodeCache nc) {
        Boolean value = null;  // default if no correct data set
        ChildData currentData = nc.getCurrentData();
        if (currentData != null) {
            byte[] newDataValue = currentData.getData();
            if (newDataValue != null) {
                String stringvalue = new String(newDataValue);
                if ("true".equalsIgnoreCase(stringvalue) || "1".equals(stringvalue)) {
                    value = true;
                } else if ("false".equalsIgnoreCase(stringvalue) || "0".equals(stringvalue)) {
                    value = false;
                }
            }
        }
        if (value != null) {
            featureValues.put(key, value);
        } else {
            featureValues.remove(key);
        }
        return value;
    }

}
