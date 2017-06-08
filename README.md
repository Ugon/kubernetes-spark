# kubernetes-spark

## What is this?
This is a guide on how to setup a Kubernetes cluser, deploy Apache Spark on it and use it to run a simple application. 
It is based on materials that can be found on the internet:
https://kubernetes.io/docs/getting-started-guides/kubeadm/
https://github.com/kubernetes/kubernetes/tree/master/examples/spark

## Deploying Kubernetes cluster
To deploy a K8s cluster you need at least two nodes (you can successfully deploy and use K8s on one node, but that's not a cluster). 
This guide uses 4 VirtualBox virtual machines running Ubuntu 16.04 Server as K8s nodes (1 for K8s master and 3 for minions).

1. Create 4 VMs and install Ubuntu 16.04 Serve OS on all of them. Please note that this guide is specific to Ubuntu distribution. If
you want to use another LINUX/UNIX distribution, it may be necessary to use some different commands than presented in this tutorial
(e.g. there are some issues related to the Docker installation with the Debian-Jessie distribution). While creating the VMs please
also take into account that each VM should have different host name.
2. Login to all of them as root (use ssh for convenience).
3. Install Docker and K8s on all VMs.
```
apt-get update && apt-get install -y apt-transport-https
curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add -
cat <<EOF >/etc/apt/sources.list.d/kubernetes.list
deb http://apt.kubernetes.io/ kubernetes-xenial main
EOF
apt-get update
apt-get install -y docker-engine
apt-get install -y kubelet kubeadm kubectl kubernetes-cni
```
Please note that the `docker0` interface is configured by default during the Docker installation in the network addressed by: 
`172.17.0.0/16`. This may cause problems when the Ubuntu VMs are placed in the same network. To workaround this issue, remove the
`docker0` entry in the routing table by typing:
```
sudo ip route del 172.17.0.0/16 dev docker0
```
4. On master node run:
```
kubeadm init
``` 
kubeadm is a tool for quick and easy deployment of K8s. Make note of last line of output, you'll use it later on other nodes to join master.
```
kubeadm join --token 0f953e.8ca02dfe7d72f2d4 192.168.0.32:6443
```

5. If you want to manage your K8s cluster from remote host (other than VMs that you deployed earlier), install kubectl on that host. kubectl is a tool for managing K8s cluster. 
You can also manage your cluster from master node - if so, skip this step.
```
curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
chmod +x ./kubectl
sudo mv ./kubectl /usr/local/bin/kubectl
```

6. Configuring kubectl to work with your cluster.
* If you are using kubectl on master node, run on master node:
```
sudo cp /etc/kubernetes/admin.conf $HOME/
sudo chown $(id -u):$(id -g) $HOME/admin.conf
export KUBECONFIG=$HOME/admin.conf

```
* If you are using kubectl on remote host, run on that host:
```
scp root@<K8S_MASTER_IP>:/etc/kubernetes/admin.conf $HOME
export KUBECONFIG=$HOME/admin.conf
```

7. If you want master node to be used for scheduling pods, run:
```
kubectl taint nodes --all node-role.kubernetes.io/master-
```

8. Install pod network:
```
kubectl apply -f https://git.io/weave-kube-1.6
```

9. To check if everything is working as expected, run:
```
kubectl get pods --all-namespaces
```
`kube-dns` pod should have `RUNNING` status.

10. On all minion VMs run `kubeadm join` command that you took note of in step 4. This will connect your nodes to K8s mater node and form a cluster.
You can confirm that nodes joined cluster by running `kubectl get nodes` command. After a minute or so all nodes should be have `RUNNING` status.

Your cluster is now up and running, you can close all your shh connections (apart from master node if you are not configuring it from remote host).

## Deploying Apache Spark on Kubernetes cluster

1. Download configuration files from K8s Spark example:
```
curl https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/spark/spark-master-controller.yaml -o spark-master-controller.yaml
curl https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/spark/spark-master-service.yaml -o spark-master-service.yaml
curl https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/spark/spark-worker-controller.yaml -o spark-worker-controller.yaml
```

2. Start spark-master node.
```
kubectl create -f spark-master-controller.yaml
kubectl create -f spark-master-service.yaml
```

3. Start spark-worker nodes. To select how many workers should be stared, modify `replicas: 2` in spark-worker-controller.yaml.
```
kubectl create -f spark-worker-controller.yaml
```
To check if they are working, run `kubectl get pods`. You can also check this in spark web ui.

4. Spark operates in internal kubernetes network. To gain access to fully functional Spark UI a connection with this network is required.
To achieve this we recommend connecting your workstation to that k8s network using `weave` (network overlay which is used by k8s).

```
sudo curl -L git.io/weave -o /usr/local/bin/weave
sudo chmod a+x /usr/local/bin/weave
sudo weave launch
sudo weave connect <NODE_IP>
sudo weave expose
```
`<NODE_IP>` is ip address of any node running k8s.

5. To display Spark UI first find out Spark master pod id and then its ip address inside k8s network.
```
kubectl get pods
kubectl describe pod spark-master-controller-<ID>
```
In web browser go to `<ip>:8080`.

## Installing and using Zeppelin UI
1. Download configuration files from K8s Spark example: 
```
curl https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/spark/zeppelin-controller.yaml -o zeppelin-controller.yaml
curl https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/spark/zeppelin-service.yaml -o zeppelin-service.yaml
```
2. Start ZeppelinUI controller and service
```
kubectl create -f zeppelin-controller.yaml
kubectl create -f zeppelin-service.yaml
```
3. Check if ZeppelinUi is running
```
kubectl get pods
```

4. ZeppelinUI service runs in K8s in NodePort mode. Therefore the ZeppelinUI is available via the public IP of the K8s node hosting
the zeppelin service, on a port determined by K8s. To find out what that port is use:
```
kubectl get svc zeppelin
NAME       CLUSTER-IP      EXTERNAL-IP   PORT(S)        AGE
zeppelin   10.97.252.171   <pending>     80:32627/TCP   3d
```
In this example the port is 32627.

5. ZeppelinUI can take jobs written in Scala or Python. To create a job first create a Note (multiple concurrent Notes are allowed), write in it your job and press Shift+Enter to run it.

Python example:
```
%pyspark
import random

NUM_SAMPLES = 50000000
def inside(p):
    x, y = random.random(), random.random()
    return x*x + y*y < 1

count = sc.parallelize(xrange(0, NUM_SAMPLES)) \
             .filter(inside).count()
print "Pi is roughly %f" % (4.0 * count / NUM_SAMPLES)
```

Scala example:
```
%spark
val NumSamples = 50000000
val count = sc.parallelize(1 to NumSamples).filter { _ =>
  val x = math.random
  val y = math.random
  x*x + y*y < 1
}.count()
println(s"Pi is roughly ${4.0 * count / NumSamples}")
```

## Submitting jobs to Spark running in Kubernetes cluster
1. Package your code in a `.jar`
```
sbt package
```

2. Use `submit.sh` 
```
submit.sh <JAR_LOCATION> <DATA_FILE_LOCATION> <JOB_ARGUMENTS>
```

## Sample application
This project contains the sample Apache Spark application built on top of the [GraphX library](http://spark.apache.org/graphx/). The
application counts the number of triangles in a directed graph passing through each node of this graph. A vertex is a part of a triangle
when it has two adjacent vertices with an edge between them.

The input file comes from the [Stanford Large Network Dataset Collection](https://snap.stanford.edu) and is the [collaboration network of
Arxiv High Energy Physics Theory](https://snap.stanford.edu/data/ca-HepTh.html). This network consists of 9'877 nodes and 25'998 edges.
The detailed description of this network is available under the above link. The graph file consits of two columns - the first one is the
ID of a researcher, who co-authored a paper with a researcher having the ID placed in the second column. If a paper is co-authored by more
than one researcher, this authors generate a completely connected clique.

