#!/usr/bin/python3

import sys
from   optparse import OptionParser
import requests
import json
from   time import sleep

class ProgramError(Exception):
  pass

class UO(object):
    """@brief responsible for user viewable output"""

    def info(self, text):
        print('INFO:  '+str(text))

    def debug(self, text):
        print('DEBUG: '+str(text))

    def warn(self, text):
        print('WARN:  '+str(text))

    def error(self, text):
        print('ERROR: '+str(text))

class ADF4350(object):
    #Responsible for setting the freq of the signal generator via a WiFi connection.
    def __init__(self, uo, options):
        self._uo = uo
        self._options = options

    def setFreq(self, freqMHz):
        """@brief Set the RF output freq
           @param freqMHz The required frequency in MHz (137.5 - 4400)
           @return None"""
        if freqMHz < 137.5 or freqMHz > 4400:
            raise ProgramError("{} is invalid. 137.5 MHz to 4400 MHz are valid.".format(freqMHz))

        url = "http://{}:80/RPC.ydev.set_freq_mhz".format(self._options.host)
        data = {"arg0": "{}".format(freqMHz)}
        headers = {"Content-Type": "application/json"}
        response = requests.put(url, data=json.dumps(data), headers=headers)
        response.json()
        self._uo.info("Set {:.1f} MHz".format(freqMHz))

    def setLevel(self, levelDbm):
        """@brief Set the RF output level
           @param levelDbm The required output level in dBm (only -4, -1, 2 and 5 are valid)
           @return None"""
        if levelDbm not in [-4, -1, 2, 5]:
            raise ProgramError("{} is invalid. -4, -1, 2 and 5 dBm are valid.".format(levelDbm))

        url = "http://{}:80/RPC.ydev.set_level_dbm".format(self._options.host)
        data = {"arg0": "{}".format(levelDbm)}
        headers = {"Content-Type": "application/json"}
        response = requests.put(url, data=json.dumps(data), headers=headers)
        response.json()
        self._uo.info("Set {:.1f} dBm".format(levelDbm))

    def setRFOn(self, rfOn):
        """@brief Set the RF output on/off
           @param rfOn If true the RF output is set on, else off.
           @return None"""
        if rfOn not in [True, False]:
            raise ProgramError("{} is invalid. The RF output must be set either on or off.".format(rfOn))

        url = "http://{}:80/RPC.ydev.rf_on".format(self._options.host)
        if rfOn:
            data = {"arg0": '1'}
        else:
            data = {"arg0": '0'}
        headers = {"Content-Type": "application/json"}
        response = requests.put(url, data=json.dumps(data), headers=headers)
        response.json()
        if rfOn:
            msg = "RF output is ON"
        else:
            msg = "RF output is OFF"
        self._uo.info(msg)

    def scanFreq(self, startMHz, stopMHz, stepMHz, dwellSeconds):
        """@brief Scan the output frequency
           @param startMHz
           @param stopMHz
           @param stepMHz
           @param dwellSeconds"""
        if stopMHz < startMHz:
            raise ProgramError("Stop frequency is below the start frequency.")

        currentFreqMHz = startMHz
        while True:
            self.setFreq(currentFreqMHz)
            sleep(dwellSeconds)
            currentFreqMHz=currentFreqMHz+stepMHz
            if currentFreqMHz > stopMHz:
                break

def main():
    uo = UO()

    opts=OptionParser(usage='Set the freq of the Y SIG GEN device via WiFi.')
    opts.add_option("--debug",      help="Enable debugging.", action="store_true", default=False)
    opts.add_option("--host",       help="The device address.", default=None)
    opts.add_option("--freq",       help="The freq to set in MHz.", type="float", default=None)
    opts.add_option("--level",      help="The output level in dBm (only -4, -1, 2 and 5 dBm are valid).", type="float", default=None)
    opts.add_option("--rf_on",      help="Set the RF output on.", action="store_true", default=False)
    opts.add_option("--rf_off",     help="Set the RF output off.", action="store_true", default=False)

    opts.add_option("--start", help="The starting frequency in MHz.", type="float", default=None)
    opts.add_option("--stop",  help="The end frequency in MHz.", type="float", default=None)
    opts.add_option("--step",  help="The step size in MHz (default=0.1)", type="float", default=0.1)
    opts.add_option("--dwell", help="The dwell period on each frequency in seconds (default = 10.0)", type="float", default=10)

    try:
        (options, args) = opts.parse_args()

        adf4350 = ADF4350(uo, options)

        if options.rf_on and options.rf_off:
            raise ProgramError("The RF output cannot be set on and off at the same time.")

        if options.freq:
            adf4350.setFreq(options.freq)

        if options.level:
            adf4350.setLevel(options.level)

        if options.rf_on:
            adf4350.setRFOn(True)

        if options.rf_off:
            adf4350.setRFOn(False)

        if options.start and options.stop:
            adf4350.scanFreq(options.start, options.stop, options.step, options.dwell)


    #If the program throws a system exit exception
    except SystemExit:
      pass
    #Don't print error information if CTRL C pressed
    except KeyboardInterrupt:
      pass
    except Exception as e:
     if options.debug:
       raise

     else:
       uo.error(e)

if __name__== '__main__':
    main()
