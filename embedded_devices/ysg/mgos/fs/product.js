const CONFIG_OPT    = "config";
const YDEV_OPT      = "ydev";
const MIN_FREQ_MHZ  = 137.5;
const MAX_FREQ_MHZ  = 4400;

var devNameLabel         = document.getElementById("devNameLabel");
var devNameText          = document.getElementById("devNameText");
var groupNameText        = document.getElementById("groupNameText");
var syslogEnableCheckbox = document.getElementById("syslogEnableCheckbox");
var opFreqNumber         = document.getElementById("opFreqNumber");

var tab2Button           = document.getElementById("tab2Button");
var setConfigButton      = document.getElementById("setConfigButton");
var setDefaultsButton    = document.getElementById("setDefaultsButton");
var rebootButton         = document.getElementById("rebootButton");
var rfLevelM4            = document.getElementById("rfLevelM4");
var rfLevelM1            = document.getElementById("rfLevelM1");
var rfLevel2             = document.getElementById("rfLevel2");
var rfLevel5             = document.getElementById("rfLevel5");
var rfStateOn            = document.getElementById("rfStateOn");
var rfStateOff           = document.getElementById("rfStateOff");
var cmdHistoryDiv        = document.getElementById("cmd_history");

var deviceConfig = {}; //Holds the device configuration and is loaded
                       //when updateView() is called.

/**
 * @brief A USerOutput class responsible for displaying log messages
 **/
class UO {
   /**
    * @brief constructor
    * @param debugEnabled If True debug messages are displayed.
    **/
   constructor(debugEnabled) {
       this.debugEnabled=debugEnabled;
   }

   /**
    * @brief Display an info level message.
    * @param msg The message text to be displayed on the console.
    **/
   info(msg) {
       console.log("INFO:  "+msg);
   }

   /**
    * @brief Display a warning level message.
    * @param msg The message text to be displayed on the console.
    **/
   warn(msg) {
       console.log("WARN:  "+msg);
   }

   /**
    * @brief Display a error level message.
    * @param msg The message text to be displayed on the console.
    **/
   error(msg) {
       console.log("ERROR: "+msg);
   }

   /**
    * @brief Display a debug level message if the constructor argument was true.
    * @param msg The message text to be displayed on the console.
    **/
   debug(msg) {
       if( this.debugEnabled ) {
           console.log("DEBUG: "+msg);
       }
   }
}
var uo = new UO(true);

/**
 * @brief log text to the log message area on the page.
 * @param msg
 *            The text message to be displayed.
 */
var logCmd = function(msg) {
    var curentLog = cmdHistoryDiv.innerHTML;
    var now = new Date();
    var dataTimeString = now.toUTCString();
    msg = dataTimeString+": "+msg;
    var newLog = msg + "<BR>" + curentLog;
    cmdHistoryDiv.innerHTML = newLog;
};

/**
 * @brief Update the view of the device.
 **/
function updateView() {
  uo.debug("updateView()");
  $.ajax({
    url: '/rpc/Config.Get',
    success: function(data) {
    deviceConfig = data;

    //This shows all the config options available on the device.
    //uo.info(data);
    console.log(data);
    //Get the subsection specific to yView devices.
    var yDevConfig = data['ydev'];

    devNameText.value = yDevConfig["unit_name"];
    devNameLabel.innerHTML = devNameText.value;
    groupNameText.value = yDevConfig["group_name"];
    syslogEnableCheckbox.checked = yDevConfig['enable_syslog'];
    opFreqNumber.value = yDevConfig["output_mhz"].toFixed(1);

    var rfLevel = yDevConfig["rf_level"];
    if( rfLevel == -4 ) {
      rfLevelM4.checked=true;
    }
    else if( rfLevel == -1 ) {
      rfLevelM1.checked=true;
    }
    else if( rfLevel == 2 ) {
      rfLevel2.checked=true;
    }
    else if( rfLevel == 5 ) {
      rfLevel5.checked=true;
    }
    else {
      uo.error("Invalid level: "+rfLevel);
    }

    var rfOn = yDevConfig["rf_on"];
    if( rfOn ) {
      rfStateOn.checked=true;
    }
    else {
      rfStateOff.checked=true;
    }

    },
  });
}

/**
 * @brief Set the output frequency.
 **/
function setFreqAction() {
  uo.debug("setFreqAction()");
  if( opFreqNumber.value >= MIN_FREQ_MHZ && opFreqNumber.value <= MAX_FREQ_MHZ ) {
    let configData = {arg0: opFreqNumber.value};
    let jsonStr = JSON.stringify(configData);
    uo.debug("jsonStr="+jsonStr);

    $.ajax({
      url: '/rpc/ydev.set_freq_mhz',
      data: jsonStr,
      type: 'POST',
      success: function(data) {
        logCmd("Set frequency to "+opFreqNumber.value+" MHz");
      },
    })
  }
  else {
    alert(opFreqNumber.value+" is invalid. Valid range = 137.5 to 4400 MHz.")
  }
}

/**
 * @brief Set the output frequency if user pressed enter in freq field.
 **/
function setFreqActionEnter(k) {
  if (k.code == 'Enter') {    // only if the key is "Enter"...
    setFreqAction();
  }
  return false;               // no propagation or default
}




/**
 * @brief Set the output level in dBm.
 **/
function setLevelAction() {
  var rfLevel = 0;
  if( rfLevelM4.checked ) {
      rfLevel=-4;
  }
  else if( rfLevelM1.checked ) {
    rfLevel = -1;
  }
  else if( rfLevel2.checked ) {
    rfLevel=2;
  }
  else if( rfLevel5.checked ) {
    rfLevel=5;
  }

  if( rfLevel ) {
    let configData = {arg0: rfLevel};
    let jsonStr = JSON.stringify(configData);
    uo.debug("jsonStr="+jsonStr);
    $.ajax({
      url: '/rpc/ydev.set_level_dbm',
      data: jsonStr,
      type: 'POST',
      success: function(data) {
        logCmd("Set RF output level to "+rfLevel+" dBm");
      },
    })
  }

}

/**
 * @brief Set the output on/off.
 **/
function setOnOffAction() {
  var outputOn = -1;
  if( rfStateOn.checked ) {
    outputOn=1;
  }
  else if( rfStateOff.checked ) {
    outputOn=0;
  }

  if( outputOn == 1 || outputOn == 0 ) {
    let configData = {arg0: outputOn};
    let jsonStr = JSON.stringify(configData);
    uo.debug("jsonStr="+jsonStr);
    $.ajax({
      url: '/rpc/ydev.rf_on',
      data: jsonStr,
      type: 'POST',
      success: function(data) {
        if( outputOn ) {
          logCmd("Set RF output ON");
        }
        else {
          logCmd("Set RF output OFF");
        }
      },
    })
  }
}

/**
 * @brief Called when the Save button is selected.
 **/
function setDeviceConfig() {
  uo.debug("setDeviceConfig()");
  let configData = {};
  configData[CONFIG_OPT] = {};
  configData[CONFIG_OPT][YDEV_OPT] = {};
  configData[CONFIG_OPT][YDEV_OPT]["unit_name"]     = devNameText.value;
  configData[CONFIG_OPT][YDEV_OPT]["group_name"]    = groupNameText.value;
  configData[CONFIG_OPT][YDEV_OPT]["enable_syslog"] = syslogEnableCheckbox.checked;
  let jsonStr = JSON.stringify(configData);
  uo.debug("jsonStr="+jsonStr);

  $.ajax({
    url: '/rpc/config.set',
    data: jsonStr,
    type: 'POST',
    success: function(data) {
      uo.debug("Set config success");

      //Now save the config to a file.
      $.ajax({
        url: '/RPC/Config.Save',
        data: jsonStr,
        type: 'POST',
        success: function(data) {
          uo.debug("Save config success");

          //Now update the syslog state.
          $.ajax({
            url: '/rpc/ydev.update_syslog',
            type: 'POST',
            success: function(data) {
              uo.debug("Update syslog success");
            },
          })

        },
      })
    },
  })
}

/**
 * @brief Called when the factory defaults button is selected.
 **/
function setDefaults() {
  uo.debug("setDefaults()");
  if( confirm("Are you sure that you wish to set the device configuration to factory defaults and reboot ?") ) {
      $.ajax({
      url: '/rpc/ydev.factorydefault',
      type: 'POST',
      success: function(data) {
        uo.debug("Set factory default config success");
        alert("The device is now rebooting.");
        document.body.style.cursor = 'wait';
        setTimeout( location.reload() , 1000);
      },
    })
  }
}

/**
 * @brief Called when the Reboot button is selected.
 **/
function reboot() {
  uo.debug("reboot()");
  if( confirm("Are you sure that you wish to reboot the device ?") ) {
    $.ajax({
      url: '/rpc/sys.reboot',
      type: 'POST',
      success: function(data) {
        uo.debug("Reboot success");
        alert("The device is now rebooting.");
        document.body.style.cursor = 'wait';
        setTimeout( location.reload() , 1000);
      },
    })
  }
}

/**
 * @brief Called when a tab (in product.html) is selected.
 **/
function selectTab(evt, cityName) {
  uo.debug("selectTab()");
  var i, tabcontent, tablinks;
  tabcontent = document.getElementsByClassName("tabcontent");
  for (i = 0; i < tabcontent.length; i++) {
  tabcontent[i].style.display = "none";
  }
  tablinks = document.getElementsByClassName("tablinks");
  for (i = 0; i < tablinks.length; i++) {
  tablinks[i].className = tablinks[i].className.replace(" active", "");
  }
  document.getElementById(cityName).style.display = "block";
  evt.currentTarget.className += " active";
}

/**
 * @brief Called when the page is loaded.
 **/
window.onload = function(e){
  uo.debug("window.onload()");
  updateView();

  setConfigButton.addEventListener("click", setDeviceConfig);
  setDefaultsButton.addEventListener("click", setDefaults);
  rebootButton.addEventListener("click", reboot);

  opFreqNumber.addEventListener("click", setFreqAction);
  opFreqNumber.addEventListener("keypress", setFreqActionEnter);

  rfLevelM4.addEventListener("click", setLevelAction);
  rfLevelM1.addEventListener("click", setLevelAction);
  rfLevel2.addEventListener("click", setLevelAction);
  rfLevel5.addEventListener("click", setLevelAction);

  rfStateOn.addEventListener("click", setOnOffAction);
  rfStateOff.addEventListener("click", setOnOffAction);

}

//Select the required tab when the product.html page is loaded.
document.getElementById("defaultOpen").click();
