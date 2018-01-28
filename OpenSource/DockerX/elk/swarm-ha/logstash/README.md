# ELK stack with logbook

This was created to make a more pluggable logging solution with Docker.

## Usage

1. Follow the steps in the [logbook](https://github.com/ahromis/logbook) repo
2. Clone this repo
3. Set the following on nodes that will be running Elasticsearch:
    1. `sysctl -w vm.max_map_count=262144`
    2. `echo 'vm.max_map_count=262144' >> /etc/sysctl.conf` (to persist reboots)
4. `docker stack deploy -c ./logstash/docker-compose.yml elk`
5. Access Kibana by going to `http://<worker_node_ip>:5601`

## HA Elasticsearch

Elasticsearch can be run in an HA configuration after the initial stack comes up. The first node needs to register as healthy before scaling it out. After the initial Elasticsearch member is healthy, then it can be scaled.

1. Find the Elasticsearch service ID:
    1. `docker service ls`
2. Scale out the service to include more replicas:
    1. `docker service update --replicas=3 <replica_id>`

## Testing

Many times traditional applications will log to multiple locations. Java applications are often configured to log in this way. It's possible to modernize those applications without changing the logger by leverging Go service templating that Docker provides.

When launching a Tomcat application with the following parameters, Docker will create a volume that contains the container name.

```
docker service create \
    -d \
    --name prod-tomcat \
    --label version=1.5 \
    --label environment=prod \
    --mount type=volume,src="{{.Task.Name}}",dst=/usr/local/tomcat/logs \
    --replicas 3 \
    tomcat:latest
```

That will create output similar to this from logstash when using this repo. The `CONTAINER_NAME` will match the output of the `stdout` stream from the container, making it easy to filter based on your container's logs.

```
{
              "path" => "/log/volumes/prod-tomcat.2.sdfuuijyruyzoi98uwpixl5iv/_data/catalina.2017-07-05.log",
        "@timestamp" => 2017-07-06T13:24:14.875Z,
          "@version" => "1",
              "host" => "logstash",
           "message" => "05-Jul-2017 23:40:50.256 INFO [localhost-startStop-1] org.apache.catalina.startup.HostConfig.deployDirectory Deployment of web application directory [/usr/local/tomcat/webapps/docs] has finished in [47] ms",
    "CONTAINER_NAME" => "prod-tomcat.2.sdfuuijyruyzoi98uwpixl5iv",
         "FILE_NAME" => "catalina.2017-07-05.log"
}
```
