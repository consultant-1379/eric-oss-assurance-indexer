#!/bin/bash
# This script supports building a Micro CBO-based OCI image using the JVM as the runtime environment.
#
# Please check https://adp.ericsson.se/marketplace/common-base-os-docker-layer/documentation/ for additional info on Micro CBO.
#
# It depends on the following scripts:
# - entrypoint.sh: serves as entrypoint for the application running with the JVM.
#
# Environment variables:
# - CBO_VERSION: Micro CBO base image version.
# - CBO_REPO: URL to the location of common base OS repo.
#             Default: https://arm.sero.gic.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles
# - CBO_DEVENV_REPO: URL to the location of common base OS devenv repo.
#             Default: https://arm.sero.gic.ericsson.se/artifactory/proj-ldc-repo-rpm-local/adp-dev/adp-build-env
# - CBO_DEVENV_VERSION: Common base OS devenv repo version.
#             Default: ${CBO_VERSION}
# - MICROCBO_IMAGE_NAME: Micro Common Base OS image path without tag/version.
# - USER_ID: The container non-login numeric user identity as per DR-D1123-122.
# - CONTAINER_NAME: Name of the resulting image without tag/version.
# - IMAGE_TITLE: Value for org.opencontainers.image.title label as per DR-D470203-041-A.
# - REVISION: Value for org.opencontainers.image.revision label as per DR-D470203-041-A.
# - IMAGE_VERSION: Value for org.opencontainers.image.version label as per DR-D470203-041-A.
# - IMAGE_PRODUCT_NUMBER: Value for com.ericsson.product-number label as per DR-D470203-020.
#
# App Specific Environment Variables
# - RUN_TARGET: Source filename to be executed in the container.
#
# Usage:
# Run this script to build a Docker image. the image will be built with JVM as the runtime.
#

# Ensures the process fails if anything fails or is undefined
set -Eeuo pipefail;

export MICROCBO_IMAGE_NAME=${MICROCBO_IMAGE_NAME:-"armdocker.rnd.ericsson.se/proj-ldc/common_base_os_micro_release/sles"}
export MICROCBO_IMAGE="${MICROCBO_IMAGE_NAME}:${CBO_VERSION}"

export CBO_REPO=${CBO_REPO:-"https://arm.sero.gic.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles"}

export CBO_DEVENV_VERSION=${CBO_DEVENV_VERSION:-"${CBO_VERSION}"}
export CBO_DEVENV_REPO=${CBO_DEVENV_REPO:-"https://arm.sero.gic.ericsson.se/artifactory/proj-ldc-repo-rpm-local/adp-dev/adp-build-env"}

## create_builder
##
##  Prepare build environment.
##
create_builder() {
  zypper ar --no-check --gpgcheck-strict -f "${CBO_DEVENV_REPO}/${CBO_DEVENV_VERSION}" CBO_DEVENV
  zypper --gpg-auto-import-keys refresh
  zypper -n install --no-recommends -l buildah skopeo util-linux
  sed -i 's/^driver =.*/driver="vfs"/' /etc/containers/storage.conf
  zypper rr CBO_DEVENV
}


## mount_microcbo_container
##
##  Create a container from microcbo
##
##  The root directory is available with ${rootdir}
##
mount_microcbo_container() {
  zypper ar --no-check --gpgcheck-strict -f "${CBO_REPO}/${CBO_VERSION}" CBO_REPO
  zypper --gpg-auto-import-keys refresh

  container=$(buildah from "${MICROCBO_IMAGE}")
  rootdir=$(buildah mount "${container}")
  mkdir -p "${rootdir}/proc/" "${rootdir}/dev/"
  mount -t proc /proc "${rootdir}/proc/"
  mount --rbind /dev "${rootdir}/dev/"
}


## create_app_layer
##
##  Install application files
##
##
create_app_layer() {
  # Install jdk specific dependencies
  zypper -n --installroot "${rootdir}" install -l -y java-17-openjdk-headless

  buildah config \
    --env JAVA_HOME=/usr/lib64/jvm/java-17-openjdk-17 \
    "${container}"

  # Install app dependencies
  zypper -n --installroot "${rootdir}" install -l -y curl

  # Install application run target file
  mkdir -p "${rootdir}/opt/application/springboot/scripts"
  cp "${RUN_TARGET}" "${rootdir}/opt/application/springboot/application.jar"
  chmod +x "${rootdir}/opt/application/springboot/application.jar"

  # Install application script files
  cp "./scripts/entrypoint.sh" "${rootdir}/opt/application/springboot/scripts/entrypoint.sh"
  cp "./scripts/probe.sh" "${rootdir}/opt/application/springboot/scripts/probe.sh"
  chmod -R +x "${rootdir}/opt/application/springboot/scripts/"

  # If available, install sbom files
  if [ -d "./sbom" ]; then
    cp -r "./sbom" "${rootdir}/opt/application/springboot/"
  fi

  # Set ownership and permissions:
  chown -R "${USER_ID}:0" "${rootdir}/opt/application"
  chmod -R g=u "${rootdir}/opt/application"

  # Configure image user
  echo "${USER_ID}:x:${USER_ID}:${USER_ID}:An Identity for ${CONTAINER_NAME}:/nonexistent:/bin/false" >>/etc/passwd \
  && echo "${USER_ID}:!::0:::::" >>/etc/shadow

  # Add image configurations:
  buildah config \
    --label "org.opencontainers.image.title=${IMAGE_TITLE}" \
    --label "org.opencontainers.image.created=$(date -u +%FT%TZ)" \
    --label "org.opencontainers.image.revision=${REVISION}" \
    --label "org.opencontainers.image.vendor=Ericsson" \
    --label "org.opencontainers.image.version=${IMAGE_VERSION}" \
    --label "com.ericsson.product-number=${IMAGE_PRODUCT_NUMBER}" \
    --workingdir='/opt/application/springboot' \
    --user "${USER_ID}" \
    --entrypoint '["/usr/bin/catatonit", "--", "scripts/entrypoint.sh"]' \
    --cmd "${RUN_ARG}" \
    "${container}"

  # Save info about the packages
  rpm --root "${rootdir}" -qa >"${rootdir}/.app-rpms"
}


## upload_image
##
##  commit and upload the created application image
##
upload_image() {
  umount "${rootdir}/proc/"
  umount -l "${rootdir}/dev/"
  buildah commit -f docker "${container}" "${CONTAINER_NAME}:${IMAGE_VERSION}"
  skopeo copy "containers-storage:localhost/${CONTAINER_NAME}:${IMAGE_VERSION}" \
    docker-daemon:"${CONTAINER_NAME}:${IMAGE_VERSION}"
}

create_builder
mount_microcbo_container
create_app_layer
upload_image