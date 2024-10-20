# ICONS
Internet Connection Server (ICONS) for the yView network. The ICONS is docker container.

This server exposes a single SSH port (default 2222). This allows any device offering a service over a TCP socket (E.G web/ssh/vnc server etc) to be securely (tunnelled through an encypted ssh connection over the Internet) connected without passing through a cloud service provider.

## Prerequisites
Before this docker image can be used the following must be installed.

 - Docker must be installed in the linux host
 	[Installing Docker on Ubuntu](https://docs.docker.com/engine/install/ubuntu/)
 - docker-compose must be also be installed

```
sudo apt-get install docker-compose
```

## Docker image configuration
The docker-compose.yml file contains some environmental variables that may be changed. These are

### USER
This is the username inside the container that is used for the ssh login. This should be set on the command line when the docker image is built. See `Building the docker container` section for more details.

### USER_PASSWORD
This is the password for the above user. By default this is not set.

### SUDO
This defines whether `sudo` is enabled. The default for this value is true but it maybe set to false if you wish to prohibit sudo access.

### SUDO_REQUIRE_PASSWORD
If SUDO=true then this options is used. If you wish to allow sudo access but require that the user must enter the user password then this option should be set to true. In this case a user password must be set. The default for this value is false.

### ALLOW_SSH_PASSWORD
If this is set to true then a password maybe entered over the ssh connection to login to the ssh server. The default for this option is false. By default the only way to login to the ssh server is to include a public ssh key in the ssh/authorized_keys file.

## Building the docker container

```
USER=ausername && docker-compose build
```

Where ausername is the ssh username. This should be set to your selected username.

## To connect to the server once running
Use ssh to connect to the container once it is started

```
ssh -p 2222 <username>@localhost
```
