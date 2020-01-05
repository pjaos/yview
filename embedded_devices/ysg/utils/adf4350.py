#!/usr/bin/python3

import  sys
from    optparse import OptionParser

class ProgramError(Exception):
  pass

class UO(object):
    """@brief responsible for user viewable output"""

    def info(self, text):
        print('INFO:  {}'.format( str(text)) )

    def debug(self, text):
        print('DEBUG: {}'.format( str(text)) )

    def warn(self, text):
        print('WARN:  {}'.format( str(text)) )

    def error(self, text):
        print('ERROR: {}'.format( str(text)) )

class ADF4350(object):
    """@brief This was written to investigate how the ADF4350 registers should be set
              for a given frequency."""
    REF_MHZ = 10

    def __init__(self, uo, options):
        self._uo = uo
        self._options = options

    def calcRegs(self, freqMHz):
        """@brief Calc the register values needed to set the RFout freq.
                  100 kHz channel spacing."""
        self._uo.info("freqMHz      = {}".format(freqMHz) )

        if freqMHz >= 2200:
            divReg=1
        elif freqMHz >= 1100:
            divReg=2
        elif freqMHz >= 550:
            divReg=4
        elif freqMHz >= 275:
            divReg=8
        else:
            divReg=16

        intReg = int( freqMHz/(ADF4350.REF_MHZ/divReg) )
        if intReg < 75 or intReg > 65535:
            raise ProgramError("Invalid int value = {} valid 75 - 65535".format(intReg) )

        self._uo.info("divReg       = {}".format(divReg) )
        self._uo.info("intReg       = {}".format(intReg) )

        baseFreq = intReg*(ADF4350.REF_MHZ/divReg)

        self._uo.info("baseFreq     = {}".format(baseFreq) )

        chnl = freqMHz*10 - baseFreq*10 #*10 as chnl spacing is 100 kHz

        self._uo.info("chnl         = {}".format(chnl) )

        targetFrac = (freqMHz/(ADF4350.REF_MHZ/divReg))-intReg
        self._uo.info("targetFrac   = {}".format(targetFrac) )

        frac, mod = self.getFracMod(targetFrac)
        self._uo.info("frac         = {}".format(frac) )
        self._uo.info("mod          = {}".format(mod) )
        return

        minErr=1E32
        selectedFrac=-1
        selectedMod=-1
        for _mod in range(2,4095):
            for _frac in range(0, 4095):
                #Frac cannot be higher than mod
                if _frac < _mod:
                    calcFrac = _frac/_mod
                    err = abs(calcFrac-targetFrac)
                    if err < minErr:
                        minErr=err
                        selectedFrac=_frac
                        selectedMod=_mod
                        self._uo.info("minErr       = {}".format(minErr) )
                        self._uo.info("selectedFrac = {}".format(selectedFrac) )
                        self._uo.info("selectedMod  = {}".format(selectedMod) )
                        if err < 1E-1:
                            break
            if err < 1E-10:
                break

    def getFracMod(self, targetFrac):
        fracAccuracy=1E-7
        minErr=1E32
        selectedFrac=-1
        selectedMod=-1
        for _mod in range(2,4095):
            for _frac in range(0, 4095):
                #Frac cannot be higher than mod
                if _frac < _mod:
                    calcFrac = _frac/_mod
                    err = abs(calcFrac-targetFrac)
                    if err < minErr:
                        minErr=err
                        selectedFrac=_frac
                        selectedMod=_mod
                        self._uo.info("err          = {}".format(err) )
                        self._uo.info("selectedFrac = {}".format(selectedFrac) )
                        self._uo.info("selectedMod  = {}".format(selectedMod) )
                        if err < fracAccuracy:
                            break
            if err < fracAccuracy:
                break

        return (selectedFrac, selectedMod)

#Very simple cmd line template using optparse
if __name__== '__main__':
    uo = UO()

    opts=OptionParser(usage='This program shows how the registers in the ADF4350 devioce maybe set to achieve a PLL frequency.')
    opts.add_option("--debug", help="Enable debugging.", action="store_true", default=False)
    opts.add_option("-f",      help="The required ADF4350 RF out freq in MHz (default = 2500 MHz).", type="float", default=2500)

    try:
        (options, args) = opts.parse_args()

        #Example of how to calc the registers to set the ADF4350 output frequency.
        adf4350 = ADF4350(uo, options)
        adf4350.calcRegs(options.f)

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
       uo.error(sys.exc_value)
