# This util extracts the version from Java source code
#
# The file must contain the a line of text as
# VERSION   =   X 
# where X is a number. The number will always be presented as a float value. 
# Therfore integers will be converted to floats.

import sys
import os

def fatal(msg):
  print 'ERROR: %s' % (msg)
  sys.exit(-1)
  
if __name__ == "__main__":
  foundVersion=0
  
  if len(sys.argv) < 2:
    fatal('You must supply the text file to extract the version from on the command line.')
    
  ver_file = sys.argv[1]
  if not os.path.isfile(ver_file):
    fatal("%s file not found." % (ver_file) )
    
  fd = open(ver_file)
  lines = fd.readlines() 
  fd.close()
  
  for l in lines:
    l=l.strip()
    l=l.lower()
    l=l.lstrip()
    if l.startswith('public static double') and l.find("version") != -1:
      elems = l.split("=")
      if len(elems) == 2:
        elems = elems[1].split(";")
        if len(elems) > 0:
            lr=elems[0]
            lr=lr.strip()
            try:
              version=float(lr)
              foundVersion=foundVersion+1
              print lr
            except ValueError:
              raise
              fatal("Failed to extract version number from line: %s" % (l) )

  if foundVersion == 0:
    fatal("Failed to extract version from %s" % (ver_file) )
    
  if foundVersion > 1:
    fatal("More than one occurance of version=X was found in %s" % (ver_file) )
    
