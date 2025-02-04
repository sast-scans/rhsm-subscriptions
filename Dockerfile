FROM registry.access.redhat.com/ubi9/openjdk-17:1.16

USER root
WORKDIR /stage

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle dependencies.gradle ./
COPY buildSrc buildSrc
# The commented out commands are used for quarkus offline as we need the subprojects to run at top level
# Have to add too many files to have quarkus offline run
# We can revist once we refactor the codebases a bit

#COPY swatch-contracts/build.gradle swatch-contracts/build.gradle
#COPY swatch-producer-aws/build.gradle swatch-producer-aws/build.gradle
#COPY clients clients
#COPY clients-core/build.gradle clients-core/build.gradle
#COPY swatch-common-config-workaround/build.gradle swatch-common-config-workaround/build.gradle
#COPY swatch-common-resteasy/build.gradle swatch-common-resteasy/build.gradle
#COPY swatch-product-configuration/build.gradle swatch-product-configuration/build.gradle
#RUN ./gradlew quarkusGoOffline

COPY . .
RUN ./gradlew assemble -x test

FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:1.16-1.1696518671

COPY --from=0 /stage/build/libs/* /deployments/
COPY --from=0 /stage/build/javaagent/* /opt/
ENV JAVA_OPTS_APPEND=-javaagent:/opt/splunk-otel-javaagent.jar
