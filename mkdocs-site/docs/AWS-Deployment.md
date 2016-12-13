
## Environmental Variables:
If you plan to use the default configuration, [DynomitemanagerConfiguration](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/defaultimpl/DynomitemanagerConfiguration.java), then the following environmental variables must be setup:

*  `EC2_REGION`
*  `ASG_NAME` 
*  `AUTO_SCALE_GROUP`

## Setting up AWS Roles:
In order to access the AWS services and metadata information, the user must setup the proper IAM roles (for example add a Dynomite role) and on the launch configuration that role can be used. The IAM roles need to have access to iam/security-credentials/ for the following services (depending on the environment)
* EC2
* S3 (for backups/restores)
* SimpleDB (if this use as the IConfigSource)

## S3 Backups and Restores
In order to make backups and restore feature to work, Dynomite-manager must have access to `/mnt/data` and the directory must be configured in the configured in the data store configuration (e.g. Redis) as the location of the persisting the data.