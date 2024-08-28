#
# COPYRIGHT Ericsson 2022
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

ARG BASE_OS_IMAGE_URL=armdocker.rnd.ericsson.se/proj-ldc/common_base_os_release/sles
ARG BASE_OS_VERSION
ARG USER_ID=220513


# -------------------------------------------------------------------------------------------------
FROM ${BASE_OS_IMAGE_URL}:${BASE_OS_VERSION} AS baseimage-java
ARG BASE_OS_IMAGE_REPO_URL=arm.sero.gic.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles
ARG BASE_OS_VERSION
ARG CONTAINER_NAME

RUN zypper addrepo --gpgcheck-strict -f https://${BASE_OS_IMAGE_REPO_URL}/${BASE_OS_VERSION} COMMON_BASE_OS_SLES_REPO \
    && zypper --gpg-auto-import-keys refresh \
    && zypper install -l -y java-17-openjdk-headless curl catatonit  \
    && zypper removerepo COMMON_BASE_OS_SLES_REPO \
    && zypper clean --all \
    && rm -f /var/log/zypper.log

ENV JAVA_HOME /usr/lib64/jvm/java-17-openjdk-17


# -------------------------------------------------------------------------------------------------
FROM baseimage-java AS builder
ARG JAR_FILE=*.jar
ARG USER_ID

RUN echo "${USER_ID}:x:${USER_ID}:${USER_ID}:An Identity for ${CONTAINER_NAME}:/nonexistent:/bin/false" >>/etc/passwd \
    && echo "${USER_ID}:!::0:::::" >>/etc/shadow

# Expanded spring boot application files.
WORKDIR /application
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract
RUN rm -f application.jar
RUN mkdir -p snapshot-dependencies
RUN mkdir -p dependencies

# Organize Dependencies layer
WORKDIR /root-dependencies/etc
RUN cp /etc/passwd .
RUN cp /etc/shadow .

WORKDIR /root-dependencies/opt/application/springboot
RUN cp -R /application/spring-boot-loader/. .
RUN cp -R /application/snapshot-dependencies/. .
RUN cp -R /application/dependencies/. .

WORKDIR /root-dependencies/opt/application
RUN chown -R ${USER_ID}:0 .
RUN chmod -R g=u .

# Organize Service Application specific layer
WORKDIR /root-service/opt/application/springboot
RUN cp -R /application/application/. .
RUN mkdir -p scripts && mv BOOT-INF/classes/scripts/*.sh scripts/
RUN chmod +x scripts/*.sh

WORKDIR /root-service/opt/application
RUN chown -R ${USER_ID}:0 .
RUN chmod -R g=u .

# -------------------------------------------------------------------------------------------------
FROM baseimage-java AS spring-boot-release
ARG USER_ID

# Dependencies layer
COPY --from=builder /root-dependencies/ /
# Service Application layer
COPY --from=builder /root-service/ /

WORKDIR /opt/application

USER ${USER_ID}
ENTRYPOINT ["sh", "-c", "/usr/bin/catatonit -- java ${JAVA_OPTS} -cp springboot org.springframework.boot.loader.JarLauncher"]

