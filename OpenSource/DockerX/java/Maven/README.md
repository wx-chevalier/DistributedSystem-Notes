docker build --build-arg url=https://github.com/spring-projects/spring-petclinic.git\
  --build-arg project=spring-petclinic\
  --build-arg artifactid=spring-petclinic\
  --build-arg version=1.5.1\
  -t nfrankel/spring-petclinic - < Dockerfile