import sys
import os
import traceback
import setuptools
from setuptools.command.install import install
from tempfile import NamedTemporaryFile

MODULE_NAME="ydev"                                                              #The python module name
VERSION = "2.1"                                                                 #The version of the application
AUTHOR  = "Paul Austen"                                                         #The name of the applications author
AUTHOR_EMAIL = "pausten.os@gmail.com"                                           #The email address of the author
DESCRIPTION = "yView device responder."                                         # A short description of the application
PYTHON_VERSION = 2                                                              #The python applications version
LICENSE = "MIT License"                                                         #The License that the application is distributed under
REQUIRED_LIBS = ['pjalib>=3.5']                                                 #A python list of required libs (optionally including versions)
                                                                                #A list of python files (minus the .py extension) in the module containing the main entry point.
MAIN_FILES=["ydev"]

LOGGING_FOLDER="/var/log/"                                                      #The location of the modules install log file

class CustomInstallCommand(install):
    """@brief Respinsible for installing one or more startup scripts
              for the python code. These should then be available to users
              as they should be found in the $PATH."""

    def run(self):
        """@brief Install one or more startup scripts."""
        try:
            install.run(self)
            self._log("PLATFORM: %s" % (sys.platform) )
            if sys.platform.startswith("linux"):
                for mainFile in MAIN_FILES:
                    self._createCmd(MODULE_NAME, mainFile)
        except:
            traceBackLines = self._getExceptionTraceback()
            self._log("%s Install error." % (MODULE_NAME) )
            for line in traceBackLines:
                self._log(line)

    def _getExceptionTraceback(self):
        """ @return the exception traceback lines assuming an exception has occured."""
        return traceback.format_exc().split('\n')

    def _log(self, msg):
        """@brief Write the log message to the log file.
           @param msg The message to be displayed."""
        if os.path.isdir(LOGGING_FOLDER):
            logFile = os.path.join(LOGGING_FOLDER, "%s_pip_install.log" % (MODULE_NAME) )
            fd = open(logFile, 'a')
            fd.write("%s\n" % (msg) )
            fd.close()

    def _createCmd(self, moduleName, mainModule, path="/usr/local/bin"):
        """@brief Create a startup script for the cmd.
           @param moduleName The python module name.
           @param mainModule The python file with the main() program entry point.
           @param path The path for the cmd.
           @return The command file or an empty string if not created."""
        startupScript=""
        if os.path.isdir(path):
            startupScript = os.path.join(path, mainModule)
            fd = NamedTemporaryFile(delete=False)
            tmpFile = fd.name
            fd.write("#!/usr/bin/python\n")
            fd.write("from %s import %s\n" % (moduleName, mainModule) )
            fd.write("%s.main()\n" % (mainModule) )
            fd.close()
            os.rename(tmpFile, startupScript)
            self._makeExecutable(startupScript)
            self._log("Installed %s" % (startupScript) )
        return startupScript

    def _runCmd(self, cmd):
        """@brief Run a command
           @param cmd The command to run.
           @return The return code of the external cmd."""
        self._log("Running: %s" % (cmd) )
        rc = os.system(cmd)
        if rc != 0:
            self._log("!!! COMMAND FAILED !!!")
        return rc

    def _makeExecutable(self, progFile):
        """@brief Make a file executable.
           @param progFile The file to be made executable.
           @return None"""
        self._runCmd("chmod 755 %s" % (progFile))

with open("README.md", "r") as fh:
    long_description = fh.read()

setuptools.setup(
    name=MODULE_NAME,
    version=VERSION,
    author=AUTHOR,
    author_email=AUTHOR_EMAIL,
    description=DESCRIPTION,
    long_description="", #This will be read from the README.md file
    long_description_content_type="text/markdown",
    packages=setuptools.find_packages(),
    classifiers=[
        "Programming Language :: Python :: %d" % (PYTHON_VERSION),
        "License :: %s" % (LICENSE),
        "Operating System :: OS Independent",
    ],
    install_requires=[
        REQUIRED_LIBS
    ],
    cmdclass={
        'install': CustomInstallCommand,
    },
)
