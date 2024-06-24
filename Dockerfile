FROM eclipse-temurin:21
RUN apt update -y && apt install maven -y
RUN mkdir -p /opt/mqttbridge-temp /opt/mqttbridge
COPY ./pom.xml /opt/mqttbridge-temp
COPY ./src /opt/mqttbridge-temp/src
RUN mvn -B -f /opt/mqttbridge-temp package
RUN ls /opt/mqttbridge-temp/target | grep -E 'mqttbridge-[-.0-9a-zA-Z]+.jar' | grep -vE 'sources|original|javadoc' | xargs -I '{}' mv '/opt/mqttbridge-temp/target/{}' /opt/mqttbridge/mqttbridge.jar
RUN rm -rf /opt/mqttbridge-temp

CMD ["java", "-jar", "/opt/mqttbridge/mqttbridge.jar", "--config", "/config/config.json"]


