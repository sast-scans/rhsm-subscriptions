./gradlew quarkusBuild && docker build -f src/main/docker/Dockerfile.jvm -t quay.io/lburnett/rhsm:02172022 . && docker push quay.io/lburnett/rhsm:02162022 && ./deploy/bonfire_deploy.shephemeral-w9ysl9