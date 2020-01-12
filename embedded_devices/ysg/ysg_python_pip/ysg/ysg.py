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

    MIN_MHZ = 138
    MAX_MHZ = 4400

    #Responsible for setting the freq of the signal generator via a WiFi connection.
    def __init__(self, uo, options):
        self._uo = uo
        self._options = options

    def setFreq(self, freqMHz):
        """@brief Set the RF output freq
           @param freqMHz The required frequency in MHz
           @return None"""
        if freqMHz < ADF4350.MIN_MHZ or freqMHz > ADF4350.MAX_MHZ:
            raise ProgramError("{} is invalid. {} MHz to {} MHz are valid.".format(freqMHz, ADF4350.MIN_MHZ, ADF4350.MAX_MHZ))

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

    def setLowNoiseMode(self, enabled):
        """@brief Set the ADF4350 to low noise or low spur mode.
           @param rfOn If true, set low noise mode. If false set low spur mode.
           @return None"""
        if enabled not in [True, False]:
            raise ProgramError("{} is invalid. Low noise mode must be set True or False.".format(enabled))

        url = "http://{}:80/RPC.ydev.set_low_noise_mode".format(self._options.host)
        if enabled:
            data = {"arg0": '1'}
        else:
            data = {"arg0": '0'}
        headers = {"Content-Type": "application/json"}
        response = requests.put(url, data=json.dumps(data), headers=headers)
        response.json()
        if enabled:
            msg = "Set Low noise mode"
        else:
            msg = "Set low spur mode"
        self._uo.info(msg)

    def calibrate(self, calValue):
        """@brief Set the frequency calibration value. The TCXO has a pull pin
                  and this is connected to the DAC (GPIO25) output to allow the
                  TCXO to be pulled. The DAC output voltage is stored persistently
                  and becomes the freq calibration value.
           @param calValue The freq cal value (0-255)
           @return None"""
        if calValue < 0 or calValue > 255:
            raise ProgramError("{} is invalid. The DAC cal value must be in the range 0-255.".format(calValue))

        url = "http://{}:80/RPC.ydev.set_cal".format(self._options.host)
        data = {"arg0": "{:d}".format(calValue)}
        headers = {"Content-Type": "application/json"}
        response = requests.put(url, data=json.dumps(data), headers=headers)
        response.json()
        self._uo.info("Set the freq calibration value to {:d}".format(calValue))

    def muxout(self, mode):
        """@brief Set the ADF4350 muxout pin mode.
           @param mode This maybe one of the following values.
                        REG2_MUXOUT_THREE_STATE_OUTPUT          0
                        REG2_MUXOUT_DV                          1
                        REG2_MUXOUT_DGND                        2
                        REG2_MUXOUT_R_DIVIDER_OUTPUT            3
                        REG2_MUXOUT_N_DIVIDER_OUTPUT            4
                        REG2_MUXOUT_ANALOG_LOCK_DETECT          5
                        REG2_MUXOUT_DIGITAL_LOCK_DETECT         6
                        REG2_MUXOUT_RESERVED                    7
           @return None"""
        if mode < 0 or mode > 7:
            raise ProgramError("{} is invalid. The muxout mode range 0-7.".format(mode))

        url = "http://{}:80/RPC.ydev.set_muxout_mode".format(self._options.host)
        data = {"arg0": "{:d}".format(mode)}
        headers = {"Content-Type": "application/json"}
        response = requests.put(url, data=json.dumps(data), headers=headers)
        response.json()
        self._uo.info("Set the muxout mode to {:d}".format(mode))

    def charge_pump_current(self, index):
        """@brief Set the ADF4350 charge pump current.
           @param mode This maybe one of the following values.
                REG2_CHARGE_PUMP_CURRENT_0_31_MA        0
                REG2_CHARGE_PUMP_CURRENT_0_63_MA        1
                REG2_CHARGE_PUMP_CURRENT_0_94_MA        2
                REG2_CHARGE_PUMP_CURRENT_1_25_MA        3
                REG2_CHARGE_PUMP_CURRENT_1_56_MA        4
                REG2_CHARGE_PUMP_CURRENT_1_88_MA        5
                REG2_CHARGE_PUMP_CURRENT_2_19_MA        6
                REG2_CHARGE_PUMP_CURRENT_2_50_MA        7
                REG2_CHARGE_PUMP_CURRENT_2_81_MA        8
                REG2_CHARGE_PUMP_CURRENT_3_13_MA        9
                REG2_CHARGE_PUMP_CURRENT_3_44_MA        10
                REG2_CHARGE_PUMP_CURRENT_3_75_MA        11
                REG2_CHARGE_PUMP_CURRENT_4_06_MA        12
                REG2_CHARGE_PUMP_CURRENT_4_38_MA        13
                REG2_CHARGE_PUMP_CURRENT_4_69_MA        14
                REG2_CHARGE_PUMP_CURRENT_5_00_MA        15
           @return None"""
        if index < 0 or index > 15:
            raise ProgramError("{} is invalid. The charge pump current value must be in the range 0-15.".format(index))

        url = "http://{}:80/RPC.ydev.set_charge_pump_current".format(self._options.host)
        data = {"arg0": "{:d}".format(index)}
        headers = {"Content-Type": "application/json"}
        response = requests.put(url, data=json.dumps(data), headers=headers)
        response.json()
        self._uo.info("Set the charge pump current value to {:d}".format(index))

    def lock_detect_mode(self, mode):
        """@brief Set the ADF4350 lock detect mode.
           @param mode This maybe one of the following values.
                REG5_LOCK_DETECT_PIN_OP_LOW1             0
                REG5_LOCK_DETECT_PIN_OP_DIGITAL          1
                REG5_LOCK_DETECT_PIN_OP_HIGH             3
           @return None"""
        if mode != 0 and mode != 1 and mode != 3:
            raise ProgramError("{} is invalid. The lock detect mode must be either 0,1 or 3.".format(mode))

        url = "http://{}:80/RPC.ydev.set_lock_detect_mode".format(self._options.host)
        data = {"arg0": "{:d}".format(mode)}
        headers = {"Content-Type": "application/json"}
        response = requests.put(url, data=json.dumps(data), headers=headers)
        response.json()
        self._uo.info("Set the lock detect mode to {:d}".format(mode))

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
    opts.add_option("--debug",  help="Enable debugging.", action="store_true", default=False)
    opts.add_option("--host",   help="The device address.", default=None)
    opts.add_option("--freq",   help="The freq to set in MHz.", type="float", default=None)
    opts.add_option("--level",  help="The output level in dBm (only -4, -1, 2 and 5 dBm are valid).", type="float", default=None)
    opts.add_option("--rf_on",  help="Set the RF output on.", action="store_true", default=False)
    opts.add_option("--rf_off", help="Set the RF output off.", action="store_true", default=False)

    opts.add_option("--start",  help="The starting frequency in MHz.", type="float", default=None)
    opts.add_option("--stop",   help="The end frequency in MHz.", type="float", default=None)
    opts.add_option("--step",   help="The step size in MHz (default=0.1)", type="float", default=0.1)
    opts.add_option("--dwell",  help="The dwell period on each frequency in seconds (default = 10.0)", type="float", default=10)

    opts.add_option("--cal",    help="Set the calibration value. Typical value=140 (range 0 - 255). This value pulls the TCXO to provide an accurate output frequency. When set the calibration value is stored persistently on the YSG device.", type="int", default=-1)

    opts.add_option("--low_noise_mode", help="Set to low noise mode. Developer option, not stored persistently.", action="store_true", default=False)
    opts.add_option("--low_spur_mode", help="Set to low spur mode. Developer option, not stored persistently.", action="store_true", default=False)
    opts.add_option("--muxout", help="Set the muxout mode (0 - 7).  Developer option, not stored persistently.", type="int", default=-1)
    opts.add_option("--charge_pump_current", help="Set the charge pump current (0-15). Developer option, not stored persistently.", type="int", default=-1)
    opts.add_option("--lock_detect_mode", help="Set the lock detect mode (0,1 or 3).  Developer option, not stored persistently.", type="int", default=-1)

    try:
        (options, args) = opts.parse_args()

        adf4350 = ADF4350(uo, options)

        if not options.host:
            raise ProgramError("No host address defined.")

        if options.rf_on and options.rf_off:
            raise ProgramError("The RF output cannot be set on and off at the same time.")

        if options.low_noise_mode and options.low_spur_mode:
            raise ProgramError("Unable to set low noise and low spur mode at the same time.")

        if options.freq:
            adf4350.setFreq(options.freq)

        if options.level:
            adf4350.setLevel(options.level)

        if options.rf_on:
            adf4350.setRFOn(True)

        if options.low_noise_mode:
            adf4350.setLowNoiseMode(True)

        if options.low_spur_mode:
            adf4350.setLowNoiseMode(False)

        if options.rf_off:
            adf4350.setRFOn(False)

        if options.cal != -1:
            adf4350.calibrate(options.cal)

        if options.muxout != -1:
            adf4350.muxout(options.muxout)

        if options.charge_pump_current != -1:
            adf4350.charge_pump_current(options.charge_pump_current)

        if options.lock_detect_mode != -1:
            adf4350.lock_detect_mode(options.lock_detect_mode)


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
