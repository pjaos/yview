from setuptools import setup, find_packages

with open("README.md", "r") as fh:
    long_description = fh.read()

setup(
    name="icons_gw",                                                                  # The python module name.
    version="4.5",                                                                    # The version of the module.
    author="Paul Austen",                                                             # The name of the module author.
    author_email="pausten.os@gmail.com",                                              # The email address of the author.
    description="Register a device in the YView network.",                            # A short description of the module.
    long_description="",                                                              # The long description is contained in the README.md file.
    long_description_content_type="text/markdown",
    packages=find_packages(),
    include_package_data=True,
    python_requires='>3.8.2',                                                         # The minimum acceptable python version.
    license="MIT License",                                                            # The License that the module is distributed under.
    url="https://github.com/pjaos/yview/tree/master/icons_gw",                        # The home page for the module
    install_requires=[
        ['p3lib>=1.1.34','paho-mqtt','paramiko','texttable'],                         # A python list of required module dependencies (optionally including versions)
    ],
    scripts=['scripts/icons_gw','scripts/icons_json_check','scripts/mqtt_subscribe'], # A list of command line startup scripts to be installed.
)
