/* Global library settings */
alertify.set({ buttonFocus: "none" });
alertify.set({ delay: 10000 }); //for logging messages

var global_access = (function($, window, loc, alertify) {
  'use strict';

  /**
  *
  * Initialization
  *
  * negotiate with server to receive
  * a specific comm token
  * store token in cookie
  * when cookie counter is 10
  * send back the token to examine
  * and then let users finish
  *
  **/

  var baseHostName = window.location.origin;

  var mTurkURIPromise = mTurkURIretrieve();

  var companyURIPromise = companyURIretrieve();

  //contains progress counter in data.countTask
  var mTurkIdentity = _establishIdentity();

  /**
  * reFresh the count of countTaks
  * refresh after every task being completed
  * countTask is set up by server, not need to manually set up
  **/
  function refresh() {
    $.removeCookie('countTask');
    return _establishIdentity();
  }

  /**
  *
  * Use this with care, only after the individual
  * completed 10 tasks
  * this will eliminate all footprint and
  * even assign user with a new
  * comm token
  *
  **/
  function restart() {
    $.removeCookie('commToken');
    $.removeCookie('countTask');
    return _establishIdentity();
  }

  /**
  * this is the base and private function
  * should only be called by refresh
  * and retrieveIdentity
  * @returns a promise with data
    Object {id: 5,
    countTask: 0,
    commToken: "21ea4fb9-c49f-4c81-835d-be6233d9b0f9"}
  */
  function _establishIdentity() {
    return mTurkURIPromise.then(function(mTurkUris){
      return Q($.ajax({
        url: mTurkUris.mTurkerGet._2,
        type: mTurkUris.mTurkerGet._1,
        dataType: 'JSON'
      }))
      .then(function(data){
        if (data.mturkId && !$.cookie('mturkId')) {
          $.cookie('mturkId', data.mturkId);
          $("input").prop('disabled', true);
          $('#MturkId').val($.cookie('mturkId'));
        }

        if (!data.mturkId && $.cookie('mturkId')) {
          var html = "<div class='row'><div class='large-12 columns'>"+
          "<p>We detect that you have already typed in a mTurk ID: <kbd>" + $.cookie('mturkId') + "</kbd></p>"
          + "<p>If you are the owner of this mTurk ID, click <kbd>Cancel</kbd>. You won't have to type it in again."
          + " However, if you are not, or if you are using a public computer, click <kbd>OK</kbd> so we can successfully proceed." 
          + " Bare in mind if you click <kbd>OK</kbd>, your progress will be completely lost.</p>"
          + "<p> You might also be seeing this message if you are here for the second time. If so, you are welcome! Please click <kbd>Cancel</kbd>. </p>"
          +"</div></div><br>";
          alertify.confirm(html, function(res) {
            if (res) {
              $.removeCookie('mturkId');
              $.removeCookie('commToken');
              $.removeCookie('countTask');
              loc.reload();
            }else{
              savemTurkID($.cookie('mturkId'));
            }
          });
        }

        //reset task count
        $.cookie("countTask", data.countTask);

        return data;
      })
      .fail(function(jqXHR) {
        //Custom Error Handling
        customAjaxFailureHandle(jqXHR, mTurkUris.mTurkerGet._2
          , function(html) {
            var response = html+"<div class='row'><div class='large-12 columns'><div data-alert class='alert-box info radius'> Click 'OK' button, we can solve this problem " +
            "for you by resetting your COOKIE. However, doing so will eliminate your current progress. <a href='#'' class='close'>&times;</a></div></div></div>";
            alertify.confirm(response, function(res) {
              if (res) {
                $.removeCookie('commToken');
                $.removeCookie('countTask');
                $.removeCookie('mturkId');
                loc.reload();
              }
            });
          });
      });
    });
  }

  /**
  * @returns a Promise object
  * call mTurkURIretrieve().then() to
  * get data:
    Object {mTurkerGet: Object, mTurkerRegister: Object}
    mTurkerGet: Object
    mTurkerRegister: Object
    use this to access:
    data.mTurkerGet._1 -> access method
    data.mTurkerGet._2 -> access address
  */
  function mTurkURIretrieve() {
    return Q($.ajax({
      url: baseHostName + '/mturker/help',
      type: 'GET',
      dataType: 'JSON'
    }))
    .then(function(data) {
      return data;
    })
    .fail(function(jqXHR) {
      ajaxFailureHandle(jqXHR, baseHostName+'/mturker/help');
    });
  }

  /**
  *
  * @returns a Promise object
  * call companyURIretrieve().then() to
    get data:
    Object {mTurkerGet: Object, mTurkerRegister: Object}
    mTurkerGet: Object
    mTurkerRegister: Object
    use this to access:
    data.mTurkerGet._1 -> access method
    data.mTurkerGet._2 -> access address
  **/
  function companyURIretrieve() {
    return Q($.ajax({
      url: baseHostName + '/sec/help',
      type: 'GET',
      dataType: 'JSON'
    }))
    .then(function(data) {
      return data;
    })
    .fail(function(jqXHR) {
      ajaxFailureHandle(jqXHR, baseHostName+'/sec/help');
    });
  }

  //jQuery will not JSON.stringify() the data
  //it has to be done manually. So stupid.
  function savemTurkID(id) {
    var data = {"mturkid": id};
    mTurkURIPromise.then(function(uris) {
      Q($.ajax({
        url: uris.updateMTurkId._2,
        type: uris.updateMTurkId._1,
        datatype: "json",
        contentType: 'application/json; charset=UTF-8',
        data: JSON.stringify(data)
      })).fail(function(jqXHR) {
        glo.ajaxFailureHandle(jqXHR, uris.updateMTurkId._2);
      });
    });
  }


  function _ajaxErrorMsgConstructor(jqXHR, url) {
    var errorMessage = "An error has occured from " + url + ": " + jqXHR.status + " - " + jqXHR.statusText +
      ". response text: "+ jqXHR.responseText;
    return {
      txt: errorMessage,
      html: "<div class='row'><div class='large-12 columns'><h4>An error has occured</h4></div></div>" +
      "<div class='row'><div class='large-12 columns'><p>Please copy and paste the following information to anie@emory.edu. Thank you!</p></div></div>"+
      "<div class='row'><div class='large-12 columns'><p>"+errorMessage+"</p></div></div>" +
      "<div class='row'><div class='large-12 columns'><p>CommToken: " +
      $.cookie('commToken') + " <br> CountTask: " + $.cookie('countTask') + "</p></div></div>"
    };
  }

  /**
  * Handle ajax error
  * produce console.error()
  * but depending on situation, might
  * print out error on browser
  */
  function ajaxFailureHandle(jqXHR, url) {
    var errorMsgs = _ajaxErrorMsgConstructor(jqXHR, url);
    console.error(errorMsgs.txt);
    alertify.alert(errorMsgs.html);
  }

  /**
  * Handle ajax error
  * Last parameter is a callback function
  * that executes a customized alertify.js prompt
  */
  function customAjaxFailureHandle(jqXHR, url, callback) {
    var errorMsgs = _ajaxErrorMsgConstructor(jqXHR, url);
    console.error(errorMsgs.txt);
    callback(errorMsgs.html);
  }

  return {
    mTurkIdentity: mTurkIdentity,
    mTurkURIPromise: mTurkURIPromise,
    companyURIPromise: companyURIPromise,
    savemTurkID: savemTurkID,
    ajaxFailureHandle: ajaxFailureHandle,
    customAjaxFailureHandle: customAjaxFailureHandle,
    refresh: refresh,
    restart: restart
  };

}(jQuery, window, location, alertify));

var animate = (function($, glo, alertify) {
  'use strict';

  function taskComplete(progressNum) {
    scrollTop(animateProgressBar, progressNum);
    collapseAnimate(progressNum, $("#panel-instruction-content"));
    alertify.success("Hey! A new task has arrived!");
  }

  function taskFail(progressNum) {
    scrollTopFail();
    collapseAnimate(progressNum, $("#panel-instruction-content"));
  }

  function animateProgressBar(progressNum) {
    $("#progressBar").animate({
      "width": (progressNum*10)+"%"
        }, 1000, "swing");
    $("#taskDisplay").text(progressNum);
  }

  function collapseAnimate(progressNum, elem) {
    if (progressNum >= 1) {
      elem.slideUp('slow');
    }
  }

  (function registerAccordian(elem) {
    $('#instruction-header').click(function(e) {
      e.preventDefault();
      elem.slideToggle('slow');
    });
  })($("#panel-instruction-content"));

  function scrollTop(animateProgressBar, progressNum) {
    var body = $("html, body");
    body.animate({scrollTop:0}, '300', 'swing',
      animateProgressBar(progressNum));
  }

  function scrollTopFail() {
    var body = $("html, body");
    body.animate({scrollTop:0}, '300', 'swing');
  }

  return {
    taskComplete: taskComplete,
    animateProgressBar: animateProgressBar,
    taskFail: taskFail
  };


}(jQuery, global_access, alertify));

var app = (function($, glo, animate) {
  'use strict';

  /**
  *
  * Application Logic
  *
  * Capture/handle various actions inside the page
  * including form submit, form check,
  * call animate instructions
  *
  * Define actions listed below:
  * 1. Save mTurk ID via ajax
  * 1). turn input field unable to edit (with value saved inside)
  * 2). Submit to server
  * 2. One independent cycle of retrieve doc
  *   1). Change the link on "Read in browser" / "download" dynamically
  * 2). After change, alertifyjs user
  * 3. Task Complete action
  * 1). Examine integrity of extraction (empty fields...)
  * 2). Submit result to server (receive complete user info); update cookie
  * 3). task #2 retrieve cycle
  * 4). animate.taskComplete() to scroll to top and animate progress bar
  * 5). Clear text fields
  * 4. Task Unable to Complete action
  * 1). Submit to server; update cookie
  * 2).
  *
  **/

  var turkerIdentity = glo.mTurkIdentity;

  var mTurkURIPromise = glo.mTurkURIPromise;

  var companyURIPromise = glo.companyURIPromise;

  var fileURIPromise = loadNewDocURL();

  var baseHostName = window.location.origin;

  var stateOfDocButtons = {
    readInBrowser: 0,
    downloadFile: 0
  };

  var textFields = {
    riskFactor: $('#riskFactor'),
    managementDis: $('#managementDis'),
    finanState: $('#finanState')
  };

  /*
  * Action triggering main logic
  */

  (function init() {

    /**
    * For File read in broswer or download
    * data format is above loadNewDocURL()
    **/
      updateDocLinks(fileURIPromise);

    /**
    * For saving MTurk Id Action
    **/
    $('#saveMturkId').click(function(event) {
      saveMTurkIdEvent();
    });

    /**
    * mTurk Id input field hit enter key
    **/

    $("#MturkId").keydown(function (e) {
      switch (e.keyCode) {
        case 13: // enter
          saveMTurkIdEvent();
          e.preventDefault();
          break;
      }
    });
    

    /**
    * For Unable to Complete
    **/
    $('#unableToComplete').click(function(event) {
      
      fileURIPromise.then(function(file) {
        var data = {
          companyId: file.id
        };
        var newURIPromises = unableToComplete(data);
        updateDocLinks(newURIPromises);
        alertify.log("We have swapped the document for you!");
        animate.taskFail($.cookie('countTask'));
      });
    });

    /**
    * Force people to click on doc retrieval button first
    **/
    $("#readInBrowser").click(function(event) {
      stateOfDocButtons.readInBrowser = 1;
    });
    $("#downloadFile").click(function(event) {
      stateOfDocButtons.downloadFile = 1;
    });

    /**
    *  For completed text
    *  check on fields first
    **/
    $('#DoneNext').click(function(event) {

      //check if mTurk id is typed in (for BasicAuth sake)
      if (!$.cookie('mturkId')) {
        alertify.alert("You must type in your mTurk ID before starting the task.");
      }
      else {

      //passing through the first check
      if (textFields.riskFactor.val() != ""
        && textFields.managementDis.val() != ""
        && textFields.finanState.val() != "") {
        //now they are filled up, but are they authentic?
        //let's check the length of each section
        var lengthOfFirst = textFields.riskFactor.val().length;
        var lengthOfSecond = textFields.managementDis.val().length;
        var lengthOfThird = textFields.finanState.val().length;

        var passedCheck = true;

        if (lengthOfFirst <= 30 || lengthOfSecond <= 30 || lengthOfThird <= 30) {
          alertify.alert("The input of those text fields appear to be very short. " +
            "We can't let this pass our check system. Sorry. If it is the real "+
            "extraction from the document and we are blocking you, please click "+
            "<span class='red'>Unable to Complete</span> button.");
          passedCheck = false;
        }
        else if (textFields.riskFactor.val() == textFields.managementDis.val() ||
          textFields.managementDis.val() == textFields.finanState.val() ||
          textFields.riskFactor.val() == textFields.finanState.val()) {
          alertify.alert("The input of those text fields appear to be exactly the same. " +
            "We can't let this pass our check system. Sorry. If it is the real "+
            "extraction from the document and we are blocking you, please click "+
            "<span class='red'>Unable to Complete</span> button.");
          passedCheck = false;
        }
        else if (stateOfDocButtons.downloadFile == 0 && 
          stateOfDocButtons.readInBrowser == 0) {
          alertify.alert("Please click <span class='red'>Read in your web browser</span>" +
            " or <span class='red'>Download HTML file</span> first. If this is an error, please email" +
            "anie@emory.edu");
          passedCheck = false;
        }

        //add more tests/checks here if needed

        //then send out ajax
        if (passedCheck) {
          fileURIPromise.then(function(file) {
            var data = {
              companyId: file.id,
              riskFactor: textFields.riskFactor.val(),
              managementDisc: textFields.managementDis.val(),
              finStateSuppData: textFields.finanState.val()
            }

            //this result contains updated mTurker info
            //Do not refresh() because that won't be accurate!
            //result = {id: 2, mturkId: "asdfasdf", countTask: 5, commToken: "f2f6f305-ea5f-49bd-8921-ea8f70718d34"}
            var result = sendOutTextArea(data);

            //decide if the count is already up to 10
            if ($.cookie("countTask") >= 9) {
              //if the cookie indicates it's the 9th one
              //then retrieve identity again

              result.then(function(data) {
                if (data.countTask == 10) {
                  allComplete("You have completed ten tasks.", data.commToken);
                }
              });
            }
          });
        }

      }else{
        alertify.alert("One or multiple textareas aren't filled." +
          " If the document is corrupted or doesn't contain all three fields, " +
          "please click the red button <span class='red'>Unable to Complete</span> "+
          " at the bottom of the page. ");
      }
    }
    });

    //add more binding events below here

  })();


  /*Supporting functions*/

  /**
  *
  * When there is $.cookie('mturkId') field
  * update mturkId on the server side.
  *
  **/
  (function autoFillmTurkId() {
    
    turkerIdentity.then(function(data) {
      if (data.mturkId) {
        $('#MturkId').val(data.mturkId);
        $("input").prop('disabled', true);
      }
      if (data.countTask) {
        animate.animateProgressBar(data.countTask);
      }
    });

  })();

  function updateDocLinks(promise) {
    if (promise) {
      promise.then(function(data) {
        $("#readInBrowser").attr('href', data.htmlURL);
        $("#downloadFile").attr('href', data.txtURL);
      });
    }
  }

  function saveMTurkIdEvent() {
    if (!$('#MturkId').val()) { //is the input null?
      alertify.alert("<span class='red'>Warning: </span> You must type in your MTurk Id before saving it!");
    }
    else if ($("input").prop('disabled')) { //is the input disabled?
      alertify.alert("<span class='red'>Warning: </span> Your mTurk ID already exists. If there is any error, please email anie@emory.edu");
    }
    else { //if not, send update, then disable input and create cookie
      glo.savemTurkID($('#MturkId').val());
      $("input").prop('disabled', true);
      $.cookie('mturkId', $('#MturkId').val());
    };
  }

  function allComplete(customMessage, commToken) {
      //enter complete sequence; no need to handle "else", because
      //refresh process resets cookies
      alertify.alert("<div class='row'><div class='large-12 columns'><h4>Awesome!</h4></div></div>" +
        "<div class='row'><div class='large-12 columns'><p>"+customMessage+" Close this page and go back to" +
        " mTurk now! You have one minute to copy and paste following string to your Amazon mTurk window:</p></div></div>" +
        "<div class='row text-center'><div class='large-12 columns'><kbd>"+commToken+"</kbd></div></div><br><br>");

        //this erases all footprint
        glo.restart();

        //bind event to "OK" to redirect
        $('#alertify-ok').click(function(event) {
           window.location = baseHostName;
        });

        //60 seconds after auto-redirect
        setTimeout(function() {
          window.location = baseHostName;
        }, 60000);
  }

  //when a mTurker did not complete any task at all
  //and we have no more document left
  //this is triggered.
  function rejectMessage() {
    alertify.alert("<div class='row'><div class='large-12 columns'><h4>Sorry!</h4></div></div>" +
        "<div class='row'><div class='large-12 columns'><p>We don't have more tasks for you right now! Close this page and go back to" +
        " mTurk now! We apologize for this inconvenience! </p></div></div><br>");
    //bind event to "OK" to redirect
    $('#alertify-ok').click(function(event) {
       window.location = baseHostName;
    });

    //60 seconds after auto-redirect
    setTimeout(function() {
      window.location = baseHostName;
    }, 60000);
  }


  //talk to backend to retrieve a new doc's url
  //and update on html page
  // {"htmlURL": "http://mturk-company.mindandlanguagelab.com/company/file/html/2012QTR1/0000003453-12-000026-finalDoc.txt",
  //  "id": 2,
  //  "isRetrieved": true,
  //  "localFileLoc": "/Users/Aimingnie/Desktop/webapp/ProcessedSEC10KFiles/2012QTR1/0000003453-12-000026-finalDoc.txt",
  //  "retrievedTime": "2014-08-13T15:57:29Z",
  //  "txtURL": "http://mturk-company.mindandlanguagelab.com/company/file/txt/2012QTR1/0000003453-12-000026-finalDoc.txt",
  //  "unableToCompleteCount": 0 }
  // need id to mark as "unable to retrieve"

  function loadNewDocURL() {
    if (document.URL.indexOf("turk") >= 0) {
      return companyURIPromise.then(function (uris) {
        return Q($.ajax({
          url: uris.webOneCompany._2,
          type: uris.webOneCompany._1,
          datatype: "json",
          contentType: 'application/json; charset=UTF-8'
        }))
        .then(function (data) {
          return data;
        })
        .fail(function (jqXHR) {
          //this might fail under multiple situations
          if (jqXHR.responseText == JSON.stringify("There is no document fitting requirements left")) {
            //one is that there is no more document to retrieve

            var data = glo.refresh();
            //then decide if the user completed at least one task
            data.then(function(mturker) {
              if (mturker.countTask >= 1) {
                var message = "You are the lucky one! It turns out we don't have more documents for you to complete! Congrats!";
                allComplete(message, $.cookie("commToken"));
              }
              else{
                //the mturker didn't complete shit
                rejectMessage();
              }
            });
          }
          else{
            glo.ajaxFailureHandle(jqXHR, uris.webOneCompany._2);
          }

        });
      });
    }
  }

    /**
    * Must pass Basic Authen
    * Must send in company Id
    **/
    function unableToComplete(data) {
      return companyURIPromise.then(function(uris) {
        return Q($.ajax({
          url: uris.webUnableToComp._2,
          type: uris.webUnableToComp._1,
          datatype: "json",
          data:JSON.stringify(data),
          contentType: 'application/json; charset=UTF-8'
        }))
        .then(function(data) {
          return data;
        })
        .fail(function(jqXHR) {
          glo.ajaxFailureHandle(jqXHR, uris.webUnableToComp._2);
        });
      });
    }

    /**
    * This function sends out data to server
    * Must pass Basic Auth
    * Expect nothing in return
    **/
    function sendOutTextArea(data) {
        return companyURIPromise.then(function(uris) {
          return Q($.ajax({
            url: uris.webUpload._2,
            type: uris.webUpload._1,
            datatype: "json",
            contentType: 'application/json; charset=UTF-8',
            data: JSON.stringify(data)
          }))
          .then(function(data) {
              //trigger animation, cookie of count is already updated through the process
              return oneTaskCompletMTurk();
          })
          .fail(function(jqXHR) {
            glo.ajaxFailureHandle(jqXHR, uris.webUpload._2);
          });
        });
    }

    /**
    * This function receive response from server
    * as data, then update the COOKIE of "countTask"
    * because the server side can't send out cookie
    *   var textFields = {
          riskFactor: $('#riskFactor'),
          managementDis: $('#managementDis'),
          finanState: $('#finanState')
        };
    * clean out those fields
    **/
    function oneTaskCompletMTurk() {
      return mTurkURIPromise.then(function(uris) {
        return Q($.ajax({
          url: uris.taskComplete._2,
          type: uris.taskComplete._1,
          datatype: "json",
          contentType: 'application/json; charset=UTF-8'
        }))
        .then(function(data) {
          //Object {id: 1, mturkId: "asdfasdfwere", countTask: 1, commToken: "ff52be2a-ee2c-4a58-b4f6-7ee2b4aaa9fb"}
          //update COOKIE
          $.cookie('countTask', data.countTask);
          animate.taskComplete(data.countTask);
          textFields.riskFactor.val("");
          textFields.managementDis.val("");
          textFields.finanState.val("");

          return data;
        })
        .fail(function(jqXHR) {
          glo.ajaxFailureHandle(jqXHR, uris.taskComplete._2);
        });
      });
    }


  return {
    fileURIPromise: fileURIPromise
  };

}(jQuery, global_access, animate, alertify));


