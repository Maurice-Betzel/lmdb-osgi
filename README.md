#LMDb on OSGi

Prerequisites: Java 8 and Maven 3.0.5

Download Apache Karaf 4.0.8, extract and startup

https://karaf.apache.org/download.html

Checkout and build lmdbjava 0.0.5_1 from https://github.com/Maurice-Betzel/lmdb-java/tree/lmdb-java-osgi

This contains a OSGi enabled native library classloader mechanism for Apache Karaf.

Build this project producing the file lmdb-1.0.0.jar.

To install drop this file into the deploy folder found under the Karaf root folder.

Th Karaf console will output some lmdb actions.

###lmdb-osgi License

### License

This project is licensed under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

This project distribution JAR includes LMDB, which is licensed under
[The OpenLDAP Public License](http://www.openldap.org/software/release/license.html).

###lmdbjava:

https://github.com/lmdbjava/lmdbjava