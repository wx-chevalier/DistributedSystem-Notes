# MySQL 部署

# Persistent Volume

# Chart

```sh
root@kube-master:# helm install --name mysql --set mysqlRootPassword=rootpassword,mysqlUser=mysql,mysqlPassword=my-password,mysqlDatabase=mydatabase,persistence.existingClaim=mysql-pvc stable/mysql

NAME:   mysql
LAST DEPLOYED: Thu Jan 10 16:18:31 2019
NAMESPACE: default
STATUS: DEPLOYED

RESOURCES:
==> v1beta1/Deployment
NAME   DESIRED  CURRENT  UP-TO-DATE  AVAILABLE  AGE
mysql  1        0        0           0          1s

==> v1/Pod(related)
NAME                    READY  STATUS   RESTARTS  AGE
mysql-7b688448b9-pxml5  0/1    Pending  0         1s

==> v1/Secret
NAME   TYPE    DATA  AGE
mysql  Opaque  2     1s

==> v1/ConfigMap
NAME        DATA  AGE
mysql-test  1     1s

==> v1/Service
NAME   TYPE       CLUSTER-IP     EXTERNAL-IP  PORT(S)   AGE
mysql  ClusterIP  10.100.139.57  <none>       3306/TCP  1s


NOTES:
MySQL can be accessed via port 3306 on the following DNS name from within your cluster:
mysql.default.svc.cluster.local

To get your root password run:

    MYSQL_ROOT_PASSWORD=$(kubectl get secret --namespace default mysql -o jsonpath="{.data.mysql-root-password}" | base64 --decode; echo)

To connect to your database:

1. Run an Ubuntu pod that you can use as a client:

    kubectl run -i --tty ubuntu --image=ubuntu:16.04 --restart=Never -- bash -il

2. Install the mysql client:

    $ apt-get update && apt-get install mysql-client -y

3. Connect using the mysql cli, then provide your password:
    $ mysql -h mysql -p

To connect to your database directly from outside the K8s cluster:
    MYSQL_HOST=127.0.0.1
    MYSQL_PORT=3306

    # Execute the following command to route the connection:
    kubectl port-forward svc/mysql 3306

    mysql -h ${MYSQL_HOST} -P${MYSQL_PORT} -u root -p${MYSQL_ROOT_PASSWORD}
```
