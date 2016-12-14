# REST API

The default prefix used in Dynomite Manager is as follows 

`http://localhost:8080/REST/v1/admin`

The following operations are currently implemented in [DynomiteAdmin](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/resources/DynomiteAdmin.java), which is the central location for the REST calls

* `/start`: starts Dynomite
* `/stop`: stops Dynomite
* `/startstorageprocess`: starts storage process
* `/stopstorageprocess`: stops storage process
* `/get_seeds`: responds with the hostnames and tokens
* `/cluster_describe`: responds with a JSON file of the cluster level information
* `/backup`: forces an S3 backups
* `/restore`:  forces an S3 restore
* `/takesnapshot`: persist the storage data (if Redis) to the drive (based on configuration properties, this can be RDB or AOF)
* `/status`:  returns the status of the processes managed by Dynomitemanager and itself.

# Example

`curl http://localhost:8080/REST/v1/admin/status`
![screen shot 2016-07-19 at 11 42 29 am](https://cloud.githubusercontent.com/assets/4562887/16962323/eefd9390-4da5-11e6-83c3-74373e6aee87.png)

