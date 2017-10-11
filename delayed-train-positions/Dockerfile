FROM fabric8/java-jboss-openjdk8-jdk:1.2.3

ENV JAVA_APP_JAR delayed-train-positions-1.0-SNAPSHOT.jar
ENV AB_ENABLED off
ENV AB_JOLOKIA_AUTH_OPENSHIFT true
ENV JAVA_OPTIONS -Xmx256m -XX:+HeapDumpOnOutOfMemoryError

EXPOSE 8080

ADD target/delayed-train-positions-1.0-SNAPSHOT.jar /deployments/
