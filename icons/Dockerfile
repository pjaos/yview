FROM python:2

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
RUN apt-get install -y openssh-server sudo mosquitto ne python-pip git

COPY assets/pip_requirements.txt ./
RUN pip install --no-cache-dir -r pip_requirements.txt

COPY assets/python-pjalib-4.0-all.deb /tmp
COPY assets/python-icons-rpc-provider-1.1-all.deb /tmp
RUN dpkg -i /tmp/python-pjalib-4.0-all.deb
RUN dpkg -i /tmp/python-icons-rpc-provider-1.1-all.deb
RUN ln -s /usr/local/lib/python2.7/dist-packages/pjalib /usr/local/lib/python2.7/site-packages/pjalib
RUN sudo ln -s /usr/local/lib/python2.7/site-packages/paho /usr/local/lib/python2.7/dist-packages/paho
RUN sudo ln -s /usr/local/lib/python2.7/dist-packages/icons_mqtt_rpc_provider.py /usr/local/lib/python2.7/site-packages/icons_mqtt_rpc_provider.py

COPY assets/rc.local /etc/rc.local
RUN chmod +x /etc/rc.local

RUN groupadd sshgroup && useradd -ms /bin/bash -g sshgroup $USER

CMD /etc/rc.local
