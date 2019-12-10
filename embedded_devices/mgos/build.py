#!/usr/bin/python3

import  sys
from    optparse import OptionParser
from    subprocess import call

class BuildError(Exception):
  pass

class UO(object):
    """@brief responsible for user viewable output"""

    def info(self, text):
        print('INFO:  {}'.format( str(text)) )

    def debug(self, text):
        print('DEBUG: {}'.format( str(text)))

    def warn(self, text):
        print('WARN:  {}'.format( str(text)))

    def error(self, text):
        print( 'ERROR: {}'.format( str(text)))

class Bob(object):
    """@brief Bob the builder"""

    TARGETS = ["esp32", "esp8266"]

    @staticmethod
    def GetTargetString():
        targetString = ",".join(Bob.TARGETS)
        targetString = targetString.replace(",", " or ")
        return targetString

    def __init__(self, uo, options):
        self._uo = uo
        self._options = options

    def build(self):
        """@brief Build the code"""
        if self._options.target not in Bob.TARGETS:
            raise BuildError("Build error: target must be {}".format(Bob.GetTargetString()))

        cmd = "mos build "
        if self._options.clean:
            cmd = cmd + " --clean "

        cmd = cmd + "--local --verbose --platform={}".format(self._options.target)

        call( cmd, shell=True )

        return True

#Very simple cmd line template using optparse
if __name__== '__main__':
    uo = UO()
    success = False

    opts=OptionParser(usage='A build tool for ydev_example.')
    opts.add_option("--debug",   help="Enable debugging.", action="store_true", default=False)
    opts.add_option("--target",  help="Followed by target (valid targets = {}).".format( Bob.GetTargetString() ) , default=None)
    opts.add_option("--clean",   help="Clean build. The deps and build folders are removed.", action="store_true", default=False)

    try:
        (options, args) = opts.parse_args()

        bob = Bob(uo, options)
        success = bob.build()

    except( SystemExit, KeyboardInterrupt):
      pass

    except Exception as ex:
      if options.debug:
        raise

      else:
        uo.error( str(ex) )

    if success:
        sys.exit(0)
    else:
        sys.exit(-1)
