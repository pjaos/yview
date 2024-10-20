FROM ubuntu:24.04

ARG USER
ARG SUDO
ARG SUDO_REQUIRE_PASSWORD
ARG ALLOW_SSH_PASSWORD
ARG USER_PASSWORD

#We define env vars as args here to pass them into the container.
#This is only strictly nessasary for the USER arg as this is used in
#this Dockerfile but it means that all variables are defined under
#args: in the docker-compose.yml file appear as environmental
#variables in the docker container.
ENV USER=${USER}
ENV SUDO=${SUDO}
ENV SUDO_REQUIRE_PASSWORD=${SUDO_REQUIRE_PASSWORD}
ENV ALLOW_SSH_PASSWORD=${ALLOW_SSH_PASSWORD}
ENV USER_PASSWORD=${USER_PASSWORD}

RUN apt-get update
RUN apt-get install -y apt-utils
RUN apt-get install -y ssh 
RUN apt-get install -y sudo 
RUN apt-get install -y mosquitto 
RUN apt-get install -y ne 
RUN apt-get install -y python3-pip 
RUN apt-get install -y git
RUN apt-get install -y pipx
RUN apt-get install -y net-tools

RUN pipx ensurepath

COPY assets/icons_rpc_provider-0.1.0-py3-none-any.whl /tmp
RUN pipx install /tmp/icons_rpc_provider-0.1.0-py3-none-any.whl

COPY assets/rc.local /etc/rc.local
RUN chmod +x /etc/rc.local

RUN groupadd sshgroup && useradd -ms /bin/bash -g sshgroup $USER

CMD /etc/rc.local
