# What is Filebeat?
Filebeat is a lightweight, open source shipper for log file data. As the next-generation Logstash Forwarder, Filebeat tails logs and quickly sends this information to Logstash for further parsing and enrichment.

![alt text](https://static-www.elastic.co/assets/blta28996a125bb8b42/packetbeat-fish-nodes-bkgd.png?q=755 "Filebeat logo")

> https://www.elastic.co/products/beats/filebeat


# Why this image?

This image uses the Docker API to collect the logs of all the running containers on the same machine and ship them to a Logstash. No need to install Filebeat manually on your host or inside your images. Just use this image to create a container that's going to handle everything for you :-)


# How to use this image
Start Filebeat as follows:

```
$ docker run -d 
   -v /var/run/docker.sock:/tmp/docker.sock 
   -e LOGSTASH_HOST=monitoring.xyz -e LOGSTASH_PORT=5044 -e SHIPPER_NAME=$(hostname) 
   bargenson/filebeat
```

Three environment variables are needed:
* `LOGSTASH_HOST`: to specify on which server runs your Logstash
* `LOGSTASH_PORT`: to specify on which port listens your Logstash for beats inputs
* `SHIPPER_NAME`: to specify the Filebeat shipper name (deafult: the container ID) 

The docker-compose service definition should look as follows:
```
filebeat:
  image: bargenson/filebeat
  restart: unless-stopped
  volumes:
   - /var/run/docker.sock:/tmp/docker.sock
  environment:
   - LOGSTASH_HOST=monitoring.xyz
   - LOGSTASH_PORT=5044
   - SHIPPER_NAME=aWonderfulName
```


# Logstash configuration:

Configure the Beats input plugin as follows:

```
input {
  beats {
    port => 5044
  }
}
```

In order to have a `containerName` field and a cleaned `message` field, you have to declare the following filter:

```
filter {

  if [type] == "filebeat-docker-logs" {

    grok {
      match => { 
        "message" => "\[%{WORD:containerName}\] %{GREEDYDATA:message_remainder}"
      }
    }

    mutate {
      replace => { "message" => "%{message_remainder}" }
    }
    
    mutate {
      remove_field => [ "message_remainder" ]
    }

  }

}
```


# User Feedback
## Issues
If you have any problems with or questions about this image, please contact me through a [GitHub issue](https://github.com/bargenson/docker-filebeat/issues).

## Contributing
You are invited to the [GitHub repo](https://github.com/bargenson/docker-filebeat) to contribute new features, fixes, or updates, large or small.