const CONFIG_OPT = "config";
const YDEV_OPT   = "ydev";

var devNameLabel         = document.getElementById("devNameLabel");
var devNameText          = document.getElementById("devNameText");
var groupNameText        = document.getElementById("groupNameText");
var syslogEnableCheckbox = document.getElementById("syslogEnableCheckbox");
var pullDownListA        = document.getElementById("pullDownListA");

var stepSizeSelect       = document.getElementById("stepSizeSelect");
var speedRange           = document.getElementById("speedRangeInput");

var clockWiseButton      = document.getElementById("clockWiseButton");
var antiClockWiseButton  = document.getElementById("antiClockWiseButton");
var stopButton      		 = document.getElementById("stopButton");

var setConfigButton      = document.getElementById("setConfigButton");
var setDefaultsButton    = document.getElementById("setDefaultsButton");
var rebootButton         = document.getElementById("rebootButton");

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
    pullDownListA.selectedIndex = yDevConfig['option_a'];

    }
  });
}

/**
 * @brief Called when the clockWiseButton is selected.
 **/
function clockWiseButtonHandler(buttonSelected) {
  uo.debug("clockWiseButton()");
  let configData = {stepSizeSelect: stepSizeSelect.value,
                    speedRange: speedRange.value};
  let jsonStr = JSON.stringify(configData);
  uo.debug("jsonStr="+jsonStr);

  $.ajax({
    url: '/rpc/ydev.action0',
    data: jsonStr,
    type: 'POST',
    success: function(data) {
      uo.debug("clockWiseButton() success");
    },
  })
}

/**
 * @brief Called when the antiClockWiseButton is selected.
 **/
function antiClockWiseButtonHandler(buttonSelected) {
  uo.debug("antiClockWiseButton()");
  let configData = {stepSizeSelect: stepSizeSelect.value,
                    speedRange: speedRange.value};
  let jsonStr = JSON.stringify(configData);
  uo.debug("jsonStr="+jsonStr);

  $.ajax({
    url: '/rpc/ydev.action1',
    data: jsonStr,
    type: 'POST',
    success: function(data) {
      uo.debug("antiClockWiseButton() success");
    },
  })
}

/**
 * @brief Called when the stopButton is selected.
 **/
function stopButtonHandler(buttonSelected) {
  uo.debug("stopButton()");
  $.ajax({
    url: '/rpc/ydev.action2',
    type: 'POST',
    success: function(data) {
      uo.debug("stopButton() success");
    },
  })
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
  configData[CONFIG_OPT][YDEV_OPT]["option_a"]      = pullDownListA.selectedIndex;
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

  clockWiseButton.addEventListener("click", clockWiseButtonHandler);
  antiClockWiseButton.addEventListener("click", antiClockWiseButtonHandler);
  stopButton.addEventListener("click", stopButtonHandler);
  
  setConfigButton.addEventListener("click", setDeviceConfig);
  setDefaultsButton.addEventListener("click", setDefaults);
  rebootButton.addEventListener("click", reboot);
}

//Select the required tab when the product.html page is loaded.
document.getElementById("defaultOpen").click();
