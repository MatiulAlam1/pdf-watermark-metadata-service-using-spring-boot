# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

# Version set using command to Spring boot maven application
# Jar build:
#version change command:
mvn versions:set -DnewVersion=1.0.1
# maven clean
mvn clean
# jar create command
mvn clean install
# jar run command
java -jar valmet-watermark-service-1.0.0.jar

#project run command
mvn spring-boot:run

# Active profile
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
# Run with active profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod
or
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run

# Run jar with profile setting
java -jar valmet-watermark-service-1.0.1.jar --spring.profiles.active=prod

# Docker image create
mvn spring-boot:build-image # create docker image

docker tag valmet-watermark-service-app:latest REPOSITORY_NAME/watermark-service-rep # tag docker image
# push docker image
docker push REPOSITORY_NAME/watermark-service-rep
# pull docker image
docker pull REPOSITORY_NAME/watermark-service-rep 
# run docker image with nginx
docker-compose up -d  --scale water_mark_service=4 --scale nginx=1

