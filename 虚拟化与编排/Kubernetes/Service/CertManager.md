# 基于 Cert Manager 的证书管理

cert-manager is a native Kubernetes certificate management controller. It can help with issuing certificates from a variety of sources, such as Let’s Encrypt, HashiCorp Vault, Venafi, a simple signing keypair, or self signed.

It will ensure certificates are valid and up to date, and attempt to renew certificates at a configured time before expiry. It is loosely based upon the work of kube-lego and has borrowed some wisdom from other similar projects e.g. kube-cert-manager.

# Deploy an Example Service

Your service may have its own chart, or you may be deploying it directly with manifests. This quickstart uses manifests to create and expose a sample service. The example service uses [kuard](https://github.com/kubernetes-up-and-running/kuard), a demo application which makes an excellent back-end for examples.

The quickstart example uses three manifests for the sample. The first two are a sample deployment and an associated service:

- deployment manifest: [deployment.yaml](https://raw.githubusercontent.com/jetstack/cert-manager/release-0.9/docs/tutorials/acme/quick-start/example/deployment.yaml)

```
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  name: kuard
spec:
  replicas: 1
  template:
    metadata:
      labels:
        app: kuard
    spec:
      containers:
      - image: gcr.io/kuar-demo/kuard-amd64:1
        imagePullPolicy: Always
        name: kuard
        ports:
        - containerPort: 8080
```

- service manifest: [service.yaml](https://raw.githubusercontent.com/jetstack/cert-manager/release-0.9/docs/tutorials/acme/quick-start/example/service.yaml)

```
apiVersion: v1
kind: Service
metadata:
  name: kuard
spec:
  ports:
  - port: 80
    targetPort: 8080
    protocol: TCP
  selector:
    app: kuard
```

You can create download and reference these files locally, or you can reference them from the GitHub source repository for this documentation. To install the example service from the tutorial files straight from GitHub, you may use the commands:

```
$ kubectl apply -f https://raw.githubusercontent.com/jetstack/cert-manager/release-0.9/docs/tutorials/acme/quick-start/example/deployment.yaml
deployment.extensions "kuard" created

$ kubectl apply -f https://raw.githubusercontent.com/jetstack/cert-manager/release-0.9/docs/tutorials/acme/quick-start/example/service.yaml
service "kuard" created
```

An [ingress resource](https://kubernetes.io/docs/concepts/services-networking/ingress/) is what Kubernetes uses to expose this example service outside the cluster. You will need to download and modify the example manifest to reflect the domain that you own or control to complete this example.

A sample ingress you can start with is:

- ingress manifest: [ingress.yaml](https://raw.githubusercontent.com/jetstack/cert-manager/release-0.9/docs/tutorials/acme/quick-start/example/ingress.yaml)

```
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: kuard
  annotations:
    kubernetes.io/ingress.class: "nginx"
    #certmanager.k8s.io/issuer: "letsencrypt-staging"

spec:
  tls:
  - hosts:
    - example.example.com
    secretName: quickstart-example-tls
  rules:
  - host: example.example.com
    http:
      paths:
      - path: /
        backend:
          serviceName: kuard
          servicePort: 80
```

You can download the sample manifest from github, edit it, and submit the manifest to Kubernetes with the command:

```
$ kubectl create --edit -f https://raw.githubusercontent.com/jetstack/cert-manager/release-0.9/docs/tutorials/acme/quick-start/example/ingress.yaml

# edit the file in your editor, and once it is saved:
ingress.extensions "kuard" created
```

Note

The ingress example we show above has a host definition within it. The nginx-ingress-controller will route traffic when the hostname requested matches the definition in the ingress. You *can*deploy an ingress without a host definition in the rule, but that pattern isn’t usable with a TLS certificate, which expects a fully qualified domain name.

- Once it is deployed, you can use the command kubectl get ingress to see the status

  of the ingress:

```
NAME      HOSTS     ADDRESS   PORTS     AGE
kuard     *                   80, 443   17s
```

It may take a few minutes, depending on your service provider, for the ingress to be fully created. When it has been created and linked into place, the ingress will show an address as well:

```
NAME      HOSTS     ADDRESS         PORTS     AGE
kuard     *         35.199.170.62   80        9m
```

Note

The IP address on the ingress _may not_ match the IP address that the nginx-ingress-controller. This is fine, and is a quirk/implementation detail of the service provider hosting your Kubernetes cluster. Since we are using the nginx-ingress-controller instead of any cloud-provider specific ingress backend, use the IP address that was defined and allocated for the nginx-ingress-service LoadBalancer resource as the primary access point for your service.

Make sure the service is reachable at the domain name you added above, for example http://example.your-domain.com. The simplest way is to open a browser and enter the name that you set up in DNS, and for which we just added the ingress.

You may also use a command line tool like curl to check the ingress.

```
$ curl -kivL -H 'Host: example.your-domain.com' 'http://35.199.164.14'
```

The options on this curl command will provide verbose output, following any redirects, show the TLS headers in the output, and not error on insecure certificates. With nginx-ingress-controller, the service will be available with a TLS certificate, but it will be using a self-signed certificate provided as a default from the nginx-ingress-controller. Browsers will show a warning that this is an invalid certificate. This is expected and normal, as we have not yet used cert-manager to get a fully trusted certificate for our site.

Warning

It is critical to make sure that your ingress is available and responding correctly on the internet. This quickstart example uses Let’s Encypt to provide the certificates, which expects and validates both that the service is available and that during the process of issuing a certificate uses that valdiation as proof that the request for the domain belongs to someone with sufficient control over the domain.

# Deploy Cert Manager

We need to install cert-manager to do the work with kubernetes to request a certificate and respond to the challenge to validate it. We can use helm to install cert-manager. This example installed cert-manager into the kube-system namespace from the public helm charts.

```
# Install the cert-manager CRDs. We must do this before installing the Helm
# chart in the next step for `release-0.9` of cert-manager:
$ kubectl apply -f https://raw.githubusercontent.com/jetstack/cert-manager/release-0.9/deploy/manifests/00-crds.yaml

# Create the namespace for cert-manager
$ kubectl create namespace cert-manager

# Label the cert-manager namespace to disable resource validation
$ kubectl label namespace cert-manager certmanager.k8s.io/disable-validation=true

## Add the Jetstack Helm repository
$ helm repo add jetstack https://charts.jetstack.io

## Updating the repo just incase it already existed
$ helm repo update

## Install the cert-manager helm chart
$ helm install \
  --name cert-manager \
  --namespace cert-manager \
  --version v0.9.1 \
  jetstack/cert-manager

NAME:   cert-manager
LAST DEPLOYED: Wed Jan  9 13:36:13 2019
NAMESPACE: cert-manager
STATUS: DEPLOYED

RESOURCES:
==> v1beta1/ClusterRoleBinding
NAME                                 AGE
cert-manager-webhook-ca-sync         2s
cert-manager-webhook:auth-delegator  2s
cert-manager                         2s

==> v1beta1/APIService
NAME                                  AGE
v1beta1.admission.certmanager.k8s.io  2s

==> v1alpha1/Certificate
cert-manager-webhook-webhook-tls  1s
cert-manager-webhook-ca           1s

==> v1beta1/ValidatingWebhookConfiguration
cert-manager-webhook  1s

==> v1/ServiceAccount
NAME                          SECRETS  AGE
cert-manager-webhook-ca-sync  1        2s
cert-manager-webhook          1        2s
cert-manager                  1        2s

==> v1beta1/RoleBinding
NAME                                                AGE
cert-manager-webhook:webhook-authentication-reader  2s

==> v1beta1/Deployment
NAME                  DESIRED  CURRENT  UP-TO-DATE  AVAILABLE  AGE
cert-manager-webhook  1        1        1           0          2s
cert-manager          1        1        1           0          2s

==> v1/Job
NAME                          DESIRED  SUCCESSFUL  AGE
cert-manager-webhook-ca-sync  1        0           2s

==> v1beta1/CronJob
NAME                          SCHEDULE      SUSPEND  ACTIVE  LAST SCHEDULE  AGE
cert-manager-webhook-ca-sync  * * */24 * *  False    0       <none>         2s

==> v1beta1/ClusterRole
NAME                          AGE
cert-manager-webhook-ca-sync  2s
cert-manager                  2s

==> v1/ClusterRole
cert-manager-webhook:webhook-requester  2s
cert-manager-view                       2s
cert-manager-edit                       2s

==> v1/Service
NAME                  TYPE       CLUSTER-IP    EXTERNAL-IP  PORT(S)  AGE
cert-manager-webhook  ClusterIP  10.3.244.237  <none>       443/TCP  2s

==> v1/ConfigMap
NAME                          DATA  AGE
cert-manager-webhook-ca-sync  1     2s

==> v1alpha1/Issuer
NAME                           AGE
cert-manager-webhook-ca        1s
cert-manager-webhook-selfsign  1s

==> v1/Pod(related)
NAME                                   READY  STATUS             RESTARTS  AGE
cert-manager-webhook-745b49d445-rnxm2  0/1    ContainerCreating  0         2s
cert-manager-9cdd9f774-t856z           0/1    ContainerCreating  0         2s
cert-manager-webhook-ca-sync-ddf4b     0/1    ContainerCreating  0         2s

NOTES:
cert-manager has been deployed successfully!

In order to begin issuing certificates, you will need to set up a ClusterIssuer
or Issuer resource (for example, by creating a 'letsencrypt-staging' issuer).

More information on the different types of issuers and how to configure them
can be found in our documentation:

https://docs.cert-manager.io/en/latest/reference/issuers.html

For information on how to configure cert-manager to automatically provision
Certificates for Ingress resources, take a look at the `ingress-shim`
documentation:

https://docs.cert-manager.io/en/latest/reference/ingress-shim.html
```

Cert-manager uses two different custom resources, also known as [CRD](https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/custom-resources/)’s, to configure and control how it operates, as well as share status of its operation. These two resources are:

[Issuers](http://docs.cert-manager.io/en/latest/reference/issuers.html) (or [ClusterIssuers](http://docs.cert-manager.io/en/latest/reference/clusterissuers.html))

> An Issuer is the definition for where cert-manager will get request TLS certificates. An Issuer is specific to a single namespace in Kubernetes, and a ClusterIssuer is meant to be a cluster-wide definition for the same purpose.
>
> Note that if you’re using this document as a guide to configure cert-manager for your own Issuer, you must create the Issuers in the same namespace as your Ingress resouces by adding ‘-n my-namespace’ to your ‘kubectl create’ commands. Your other option is to replace your Issuers with ClusterIssuers. ClusterIssuer resources apply across all Ingress resources in your cluster and don’t have this namespace-matching requirement.
>
> More information on the differences between Issuers and ClusterIssuers and when you might choose to use each can be found at:
>
> https://docs.cert-manager.io/en/latest/tasks/issuers/index.html#difference-between-issuers-and-clusterissuers

[Certificate](http://docs.cert-manager.io/en/latest/reference/certificates.html)

> A certificate is the resource that cert-manager uses to expose the state of a request as well as track upcoming expirations.

# Configure Let’s Encrypt Issuer

We will set up two issuers for Let’s Encrypt in this example. The Let’s Encrypt production issuer has [very strict rate limits](https://letsencrypt.org/docs/rate-limits/). When you are experimenting and learning, it is very easy to hit those limits, and confuse rate limiting with errors in configuration or operation.

Because of this, we will start with the Let’s Encrypt staging issuer, and once that is working switch to a production issuer.

Create this definition locally and update the email address to your own. This email required by Let’s Encrypt and used to notify you of certificate expirations and updates.

- staging issuer: [staging-issuer.yaml](https://raw.githubusercontent.com/jetstack/cert-manager/release-0.9/docs/tutorials/acme/quick-start/example/staging-issuer.yaml)

```
   apiVersion: certmanager.k8s.io/v1alpha1
   kind: Issuer
   metadata:
     name: letsencrypt-staging
   spec:
     acme:
       # The ACME server URL
       server: https://acme-staging-v02.api.letsencrypt.org/directory
       # Email address used for ACME registration
       email: user@example.com
       # Name of a secret used to store the ACME account private key
       privateKeySecretRef:
         name: letsencrypt-staging
       # Enable the HTTP-01 challenge provider
       solvers:
       - http01:
           ingress:
             class:  nginx

```

Once edited, apply the custom resource:

```
$ kubectl create --edit -f https://raw.githubusercontent.com/jetstack/cert-manager/release-0.9/docs/tutorials/acme/quick-start/example/staging-issuer.yaml
issuer.certmanager.k8s.io "letsencrypt-staging" created

```

Also create a production issuer and deploy it. As with the staging issuer, you will need to update this example and add in your own email address.

- production issuer: [production-issuer.yaml](https://raw.githubusercontent.com/jetstack/cert-manager/release-0.9/docs/tutorials/acme/quick-start/example/production-issuer.yaml)

```
   apiVersion: certmanager.k8s.io/v1alpha1
   kind: Issuer
   metadata:
     name: letsencrypt-prod
   spec:
     acme:
       # The ACME server URL
       server: https://acme-v02.api.letsencrypt.org/directory
       # Email address used for ACME registration
       email: user@example.com
       # Name of a secret used to store the ACME account private key
       privateKeySecretRef:
         name: letsencrypt-prod
       # Enable the HTTP-01 challenge provider
       solvers:
       - http01:
           ingress:
             class: nginx
$ kubectl create --edit -f https://raw.githubusercontent.com/jetstack/cert-manager/release-0.9/docs/tutorials/acme/quick-start/example/production-issuer.yaml
issuer.certmanager.k8s.io "letsencrypt-prod" created

```

Both of these issuers are configured to use the [HTTP01](http://docs.cert-manager.io/en/latest/tasks/issuers/setup-acme/http01/index.html) challenge provider.

Check on the status of the issuer after you create it:

```
 $ kubectl describe issuer letsencrypt-staging

 Name:         letsencrypt-staging
 Namespace:    default
 Labels:       <none>
 Annotations:  kubectl.kubernetes.io/last-applied-configuration={"apiVersion":"certmanager.k8s.io/v1alpha1","kind":"Issuer","metadata":{"annotations":{},"name":"letsencrypt-staging","namespace":"default"},"spec":{"a...
 API Version:  certmanager.k8s.io/v1alpha1
 Kind:         Issuer
 Metadata:
   Cluster Name:
   Creation Timestamp:  2018-11-17T18:03:54Z
   Generation:          0
   Resource Version:    9092
   Self Link:           /apis/certmanager.k8s.io/v1alpha1/namespaces/default/issuers/letsencrypt-staging
   UID:                 25b7ae77-ea93-11e8-82f8-42010a8a00b5
 Spec:
   Acme:
     Email:  your.email@your-domain.com
     Private Key Secret Ref:
       Key:
       Name:  letsencrypt-staging
     Server:  https://acme-staging-v02.api.letsencrypt.org/directory
     Solvers:
       Http 01:
         Ingress:
           Class:  nginx
 Status:
   Acme:
     Uri:  https://acme-staging-v02.api.letsencrypt.org/acme/acct/7374163
   Conditions:
     Last Transition Time:  2018-11-17T18:04:00Z
     Message:               The ACME account was registered with the ACME server
     Reason:                ACMEAccountRegistered
     Status:                True
     Type:                  Ready
 Events:                    <none>
```

You should see the issuer listed with a registered account.

# Deploy a TLS Ingress Resource

With all the pre-requisite configuration in place, we can now do the pieces to request the TLS certificate. There are two primary ways to do this: using annotations on the ingress with [ingress-shim](http://docs.cert-manager.io/en/latest/tasks/issuing-certificates/ingress-shim.html) or directly creating a certificate resource.

In this example, we will add annotations to the ingress, and take advantage of ingress-shim to have it create the certificate resource on our behalf. After creating a certificate, the cert-manager will update or create a ingress resource and use that to validate the domain. Once verified and issued, cert-manager will create or update the secret defined in the certificate.

Note

The secret that is used in the ingress should match the secret defined in the certificate. There isn’t any explicit checking, so a typo will resut in the nginx-ingress-controller falling back to its self-signed certificate. In our example, we are using annotations on the ingress (and ingress-shim) which will create the correct secrets on your behalf.

Edit the ingress add the annotations that were commented out in our earlier example:

- ingress tls: [ingress-tls.yaml](https://raw.githubusercontent.com/jetstack/cert-manager/release-0.9/docs/tutorials/acme/quick-start/example/ingress-tls.yaml)

```
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: kuard
  annotations:
    kubernetes.io/ingress.class: "nginx"
    certmanager.k8s.io/issuer: "letsencrypt-staging"

spec:
  tls:
  - hosts:
    - example.example.com
    secretName: quickstart-example-tls
  rules:
  - host: example.example.com
    http:
      paths:
      - path: /
        backend:
          serviceName: kuard
          servicePort: 80
```

and apply it:

```
$ kubectl create --edit -f https://raw.githubusercontent.com/jetstack/cert-manager/release-0.9/docs/tutorials/acme/quick-start/example/ingress-tls.yaml
ingress.extensions "kuard" configured
```

Cert-manager will read these annotations and use them to create a certificate, which you can request and see:

```
$ kubectl get certificate
NAME                     AGE
quickstart-example-tls   38s
```

Cert-manager reflects the state of the process for every request in the certificate object. You can view this information using the kubectl describe command:

```
 $ kubectl describe certificate quickstart-example-tls

 Name:         quickstart-example-tls
 Namespace:    default
 Labels:       <none>
 Annotations:  <none>
 API Version:  certmanager.k8s.io/v1alpha1
 Kind:         Certificate
 Metadata:
   Cluster Name:
   Creation Timestamp:  2018-11-17T17:58:37Z
   Generation:          0
   Owner References:
     API Version:           extensions/v1beta1
     Block Owner Deletion:  true
     Controller:            true
     Kind:                  Ingress
     Name:                  kuard
     UID:                   a3e9f935-ea87-11e8-82f8-42010a8a00b5
   Resource Version:        9295
   Self Link:               /apis/certmanager.k8s.io/v1alpha1/namespaces/default/certificates/quickstart-example-tls
   UID:                     68d43400-ea92-11e8-82f8-42010a8a00b5
 Spec:
   Dns Names:
     example.your-domain.com
   Issuer Ref:
     Kind:       Issuer
     Name:       letsencrypt-staging
   Secret Name:  quickstart-example-tls
 Status:
   Acme:
     Order:
       URL:  https://acme-staging-v02.api.letsencrypt.org/acme/order/7374163/13665676
   Conditions:
     Last Transition Time:  2018-11-17T18:05:57Z
     Message:               Certificate issued successfully
     Reason:                CertIssued
     Status:                True
     Type:                  Ready
 Events:
   Type     Reason          Age                From          Message
   ----     ------          ----               ----          -------
   Normal   CreateOrder     9m                 cert-manager  Created new ACME order, attempting validation...
   Normal   DomainVerified  8m                 cert-manager  Domain "example.your-domain.com" verified with "http-01" validation
   Normal   IssueCert       8m                 cert-manager  Issuing certificate...
   Normal   CertObtained    7m                 cert-manager  Obtained certificate from ACME server
   Normal   CertIssued      7m                 cert-manager  Certificate issued Successfully
```

The events associated with this resource and listed at the bottom of the describe results show the state of the request. In the above example the certificate was validated and issued within a couple of minutes.

Once complete, cert-manager will have created a secret with the details of the certificate based on the secret used in the ingress resource. You can use the describe command as well to see some details:

```
$ kubectl describe secret quickstart-example-tls

Name:         quickstart-example-tls
Namespace:    default
Labels:       certmanager.k8s.io/certificate-name=quickstart-example-tls
Annotations:  certmanager.k8s.io/alt-names=example.your-domain.com
              certmanager.k8s.io/common-name=example.your-domain.com
              certmanager.k8s.io/issuer-kind=Issuer
              certmanager.k8s.io/issuer-name=letsencrypt-staging

Type:  kubernetes.io/tls

Data
====
tls.crt:  3566 bytes
tls.key:  1675 bytes
```

Now that we have confidence that everything is configured correctly, you can update the annotations in the ingress to specify the production issuer:

- ingress tls final: [ingress-tls-final.yaml](https://raw.githubusercontent.com/jetstack/cert-manager/release-0.9/docs/tutorials/acme/quick-start/example/ingress-tls-final.yaml)

```
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: kuard
  annotations:
    kubernetes.io/ingress.class: "nginx"
    certmanager.k8s.io/issuer: "letsencrypt-prod"

spec:
  tls:
  - hosts:
    - example.example.com
    secretName: quickstart-example-tls
  rules:
  - host: example.example.com
    http:
      paths:
      - path: /
        backend:
          serviceName: kuard
          servicePort: 80
$ kubectl create --edit -f https://raw.githubusercontent.com/jetstack/cert-manager/release-0.9/docs/tutorials/acme/quick-start/example/ingress-tls-final.yaml

ingress.extensions "kuard" configured
```

You will also need to delete the existing secret, which cert-manager is watching and will cause it to reprocess the request with the updated issuer.

```
$ kubectl delete secret quickstart-example-tls

secret "quickstart-example-tls" deleted
```

This will start the process to get a new certificate, and using describe you can see the status. Once the production certificate has been updated, you should see the example KUARD running at your domain with a signed TLS certificate.

```
 $ kubectl describe certificate

 Name:         quickstart-example-tls
 Namespace:    default
 Labels:       <none>
 Annotations:  <none>
 API Version:  certmanager.k8s.io/v1alpha1
 Kind:         Certificate
 Metadata:
   Cluster Name:
   Creation Timestamp:  2018-11-17T18:36:48Z
   Generation:          0
   Owner References:
     API Version:           extensions/v1beta1
     Block Owner Deletion:  true
     Controller:            true
     Kind:                  Ingress
     Name:                  kuard
     UID:                   a3e9f935-ea87-11e8-82f8-42010a8a00b5
   Resource Version:        283686
   Self Link:               /apis/certmanager.k8s.io/v1alpha1/namespaces/default/certificates/quickstart-example-tls
   UID:                     bdd93b32-ea97-11e8-82f8-42010a8a00b5
 Spec:
   Dns Names:
     example.your-domain.com
   Issuer Ref:
     Kind:       Issuer
     Name:       letsencrypt-prod
   Secret Name:  quickstart-example-tls
 Status:
   Conditions:
     Last Transition Time:  2019-01-09T13:52:05Z
     Message:               Certificate does not exist
     Reason:                NotFound
     Status:                False
     Type:                  Ready
 Events:
   Type    Reason        Age   From          Message
kubectl describe certificate quickstart-example-tls   ----    ------        ----  ----          -------
   Normal  Generated     18s   cert-manager  Generated new private key
   Normal  OrderCreated  18s   cert-manager  Created Order resource "quickstart-example-tls-889745041"
```

You can see the current state of the ACME Order by running `kubectl describe` on the Order resource that cert-manager has created for your Certificate:

```
$ kubectl describe order quickstart-example-tls-889745041
...
Events:
  Type    Reason      Age   From          Message
  ----    ------      ----  ----          -------
  Normal  Created     90s   cert-manager  Created Challenge resource "quickstart-example-tls-889745041-0" for domain "example.your-domain.com"

```

Here, we can see that cert-manager has created 1 ‘Challenge’ resource to fulfil the Order. You can dig into the state of the current ACME challenge by running `kubectl describe` on the automatically created Challenge resource:

```
$ kubectl describe challenge quickstart-example-tls-889745041-0
...

Status:
  Presented:   true
  Processing:  true
  Reason:      Waiting for http-01 challenge propagation
  State:       pending
Events:
  Type    Reason     Age   From          Message
  ----    ------     ----  ----          -------
  Normal  Started    15s   cert-manager  Challenge scheduled for processing
  Normal  Presented  14s   cert-manager  Presented challenge using http-01 challenge mechanism

```

From above, we can see that the challenge has been ‘presented’ and cert-manager is waiting for the challenge record to propagate to the ingress controller. You should keep an eye out for new events on the challenge resource, as a ‘success’ event should be printed after a minute or so (depending on how fast your ingress controller is at updating rules):

```
$ kubectl describe challenge quickstart-example-tls-889745041-0
...

Status:
  Presented:   false
  Processing:  false
  Reason:      Successfully authorized domain
  State:       valid
Events:
  Type    Reason          Age   From          Message
  ----    ------          ----  ----          -------
  Normal  Started         71s   cert-manager  Challenge scheduled for processing
  Normal  Presented       70s   cert-manager  Presented challenge using http-01 challenge mechanism
  Normal  DomainVerified  2s    cert-manager  Domain "example.your-domain.com" verified with "http-01" validation
```

Note

If your challenges are not becoming ‘valid’ and remain in the ‘pending’ state (or enter into a ‘failed’ state), it is likely there is some kind of configuration error. Read the [Challenge resource reference docs](http://docs.cert-manager.io/en/latest/reference/challenges.html) for more information on debugging failing challenges.

Once the challenge(s) have been completed, their corresponding challenge resources will be _deleted_, and the ‘Order’ will be updated to reflect the new state of the Order:

```
$ kubectl describe order quickstart-example-tls-889745041
...
Events:
  Type    Reason      Age   From          Message
  ----    ------      ----  ----          -------
  Normal  Created     90s   cert-manager  Created Challenge resource "quickstart-example-tls-889745041-0" for domain "example.your-domain.com"
  Normal  OrderValid  16s   cert-manager  Order completed successfully

```

Finally, the ‘Certificate’ resource will be updated to reflect the state of the issuance process. If all is well, you should be able to ‘describe’ the Certificate and see something like the below:

```
$ kubectl describe certificate quickstart-example-tls

Status:
  Conditions:
    Last Transition Time:  2019-01-09T13:57:52Z
    Message:               Certificate is up to date and has not expired
    Reason:                Ready
    Status:                True
    Type:                  Ready
  Not After:               2019-04-09T12:57:50Z
Events:
  Type    Reason         Age                  From          Message
  ----    ------         ----                 ----          -------
  Normal  Generated      11m                  cert-manager  Generated new private key
  Normal  OrderCreated   11m                  cert-manager  Created Order resource "quickstart-example-tls-889745041"
  Normal  OrderComplete  10m                  cert-manager  Order "quickstart-example-tls-889745041" completed successfully
```
