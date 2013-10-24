zkfss
=====

Feature Switch Service based on Apache Zookeeper (using Netflix Curator API).

zkfss provides a service to handle feature switches stored within Zookeeper.  This can be useful if you run multiple 
instances of an application or need to control features over multiple applications.  It's complete overkill if you've
got a single instance application unless perhaps you're already using Zookeeper.
 
Features
--------

* Feature switches can be set site wide and optionally overridden at the hostname and/or application name (instance).
* Feature switch values are watched for changes and are updated automatically when changed.
* Simple interface for use within your code: isEnabled(key).  This is safe to call as many times as you like without
incurring network I/O overhead. 
* Uses a CuratorFramework supplied, or creates one for use if required.

Getting Started
---------------

To create an instance of the service, use ZKFeatureSwitchService.  There are a number of optional methods available to 
configure the service before starting it via start().  If you use the default configuration, zkfss will use the following
values:

* Expect Zookeeper to be available via "localhost:2181"
* Connection timeout of 30 seconds
* RetryPolicy of ExponentialBackoffRetry(1000, 3)
* Create a CuratorFramework using the above values
* Feature Switch name space of "/zkfss/"
* Hostname subkey is true
* No Application name is set

By default, feature switch values are read from nodes under "/zkfss/".  If you want to store your feature switches under
another path, use the setFeatureSwitchNamespace method.  

Values in feature switches nodes are expected to be boolean.  In fact, the system looks for the word "true" or "1" for 
true and "false" or "0" is evaluated as false.  A node is ignored if the value is not valid.

Your feature switch names can be anything that matches a legal Zookeeper node name.  If the hostname subkey option is set, 
the hostname is used as a sub-node to the feature switch name path.  A similar approach is used if an application name is
set.

Example using the default name space of "/zkfss/", a feature switch named "X":

* The boolean value is stored at /zkfss/X.
* If hostname subkey is set, and the hostname is "myHost", then an override value for the host is stored at "/zkfss/X/myHost".
* If an application name is set, e.g. "myApp", then an override value for the application is stored at "/zkfss/X/myApp".
* If hostname subkey is set, and the hostname is "myHost" and an application name is set, e.g. "myApp", then an override value 
for the application is stored at "/zkfss/X/myApp/myHost" 

Feature switch values are evaluated in the following order:

* Host name node value override of an Application name node value
* Application name node value override
* Host name node value override
* Feature Switch node value
* Finally, if the node does not exist, the feature switch is deemed to be set to false.

Usage Examples
--------------

* Using a default configuration.

```java
ZKFeatureSwitchService zkfss = new ZKFeatureSwitchService().start();
...

if (zkfss.isEnabled("myFeature")) {
  ...feature is enabled, do stuff...
}

...
zkfss.stop()
```

* Using configuration overrides

```java
ZKFeatureSwitchService zkfss = new ZKFeatureSwitchService()
.setFeatureSwitchNamespace("myNS")  // uses /myNS/ instead of /zkfss/ for the name space
.enableHostnameSubKey()             // hostname sub-key is on by default anyway
.setApplicationName("myAppName")    // set our application name - this should be unique per application instance
.setConnectString("zk1:2181,zk2:2181,zk3:2181")  // connect to zookeeper hosts zk1, zk2, zk3
.setConnectionTimeoutMillis(2000)   // use a 2 second connection timeout to zookeeper       
.start();
...

if (zkfss.isEnabled("myFeature")) {
  ...feature is enabled at either 
    "/myNS/myFeature/myAppName/myHost", 
    "/myNS/myFeature/myAppName", 
    "/myNS/myFeature/myHost" or 
    "/myNS/myFeature" 
  (in that order and assuming your host is "myHost")
  ...do stuff...
}

...
zkfss.stop()
```

Contact Details
---------------

If you have suggestions, feedback, questions, etc, you can contact me via the email address below.  Use "zkfss" at the 
beginning of your subject line to avoid spam filtering.

Chris
chrisr AT rymich.com
