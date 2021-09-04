#!/usr/bin/python3

import  sys
from    optparse import OptionParser
import  json

class UserOutput(Exception):
  
  def info(self, line):
      print('INFO:  %s' % (line))

  def error(self, line):
      print('ERROR: %s' % (line))
      
class JsonCheckerError(Exception):
  pass

class JsonChecker(object):
    """@brief Responsible for checking json data"""
    
    def __init__(self, uo, options):
        """@brief Constructor
           @param uo UserOutput object
           @param options The command line options object"""
           
        self._uo        = uo
        self._options   = options
    
    def check(self):
        """@brief Check JSON data"""
        
        if not self._options.f:
            raise JsonCheckerError("Please use the -f argument.")

        fd = open(self._options.f, 'r')
        v = fd.read()
        fd.close()

        startPos = v.find("{")

        jsonData=''
        try:
            #Do some parsing to fixup data pasted into a file from syslog output
            if startPos >= 0:
                jsonData = v[startPos:]
                jsonData = jsonData.rstrip("\r")
                jsonData = jsonData.rstrip("\n")

                jsonData = jsonData.replace("#012","\n")
                
                json.loads(jsonData)
                
                self._uo.info("%s" % (jsonData) )
                self._uo.info("*** The above is valid JSON data ***")
                
            else:
                self._uo.error("No { character found.")
        except:
            self._uo.info("%s" % (jsonData) )
            self._uo.error("!!! The above is NOT valid JSON data !!!")
            raise
    
def main():
    uo = UserOutput()
    
    opts=OptionParser(usage='Check that the data in a file is valid json data.')
    opts.add_option("--debug",      help="Enable debugging.", action="store_true", default=False)
    opts.add_option("-f",           help="The file to read.", default=None)

    try:
        (options, args) = opts.parse_args()
            
        jsonChecker = JsonChecker(uo, options)
        jsonChecker.check()
        
    #If the program throws a system exit exception
    except SystemExit:
      pass
    #Don't print error information if CTRL C pressed
    except KeyboardInterrupt:
      pass
    except:
     if options.debug:
       raise
       
     else:
       uo.error(sys.exc_info()[1])

if __name__== '__main__':
    main()