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
  var mTurkIdentity = retrieveIdentity();

  /**
  *
  * Check if the cookie is already set,
  * retrieve value from cookie,
  * if no cookie, establish connection
  *
  * On second thought, establishing identity is kind of important.
  * because it can check if Cookies are dirty
  *
  **/
  function retrieveIdentity() {
    var promiseObj = _establishIdentity();
    return {
      'commToken' : $.cookie('commToken'),
      'countTask' : $.cookie('countTask'),
      'promiseObj' : promiseObj
    };
  }

  /**
  * reFresh the count of countTaks
  * refresh after every task being completed
  **/
  function refresh() {
    $.removeCookie('countTask');

    return retrieveIdentity();
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

    return retrieveIdentity();
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
          + " However, if you are not, or if you are using a public computer, click <kbd>OK</kbd> so we can successfully proceed." + "</p>"
          +"</div></div><br>";
          alertify.confirm(html, function(res) {
            if (res) {
              $.removeCookie('mturkId');
              loc.reload();
            }else{
              savemTurkID($.cookie('mturkId'));
            }
          });
        }
        return data;
      })
      .fail(function(jqXHR, textStatus, errorThrown) {
        //Custom Error Handling
        customAjaxFailureHandle(jqXHR, mTurkUris.mTurkerGet._2, 
          textStatus, errorThrown, function(html) {
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
    .fail(function(jqXHR, textStatus, errorThrown) {
      ajaxFailureHandle(jqXHR, baseHostName+'/mturker/help', textStatus, esrrorThrown);
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
    .fail(function(jqXHR, textStatus, errorThrown) {
      ajaxFailureHandle(jqXHR, baseHostName+'/sec/help',textStatus, errorThrown);
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
      })).fail(function(jqXHR, textStatus, errorThrown) {
        glo.ajaxFailureHandle(jqXHR, uris.updateMTurkId._2, textStatus, errorThrown);
      });
    });
  }


  function _ajaxErrorMsgConstructor(jqXHR, url, textStatus, errorThrown) {
    var errorMessage = "An error has occured from " + url + ": " + textStatus + " - " + errorThrown +
      "; response text: "+ jqXHR.responseText;
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
  function ajaxFailureHandle(jqXHR, url, textStatus, errorThrown) {
    var errorMsgs = _ajaxErrorMsgConstructor(jqXHR, url, textStatus, errorThrown);
    console.error(errorMsgs.txt);
    alertify.alert(errorMsgs.html);
  }

  /**
  * Handle ajax error
  * Last parameter is a callback function
  * that executes a customized alertify.js prompt
  */
  function customAjaxFailureHandle(jqXHR, url, textStatus, errorThrown, callback) {
    var errorMsgs = _ajaxErrorMsgConstructor(jqXHR, url, textStatus, errorThrown);
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

  var turkerId = glo.mTurkIdentity;

  var mTurkURIPromise = glo.mTurkURIPromise;

  var companyURIPromise = glo.companyURIPromise;

  var fileURIPromise = loadNewDocURL();

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
    });

    /**
    * For uncompletable files
    **/
    $('#unableToComplete').click(function(event) {
      var newURIPromises = unableToComplete();
      updateDocLinks(newURIPromises);
      alertify.log("We have swapped the document for you!");
      animate.taskFail($.cookie('countTask'));
    });

    /**
    *  For completed text
    *  check on fields first
    **/
    $('DoneNext').click(function(event) {
      if (textFields.riskFactor.val() != "" 
        && textFields.managementDis.val() != ""
        && textFields.finanState.val() != "") {
        //now they are filled up, but are they authentic?
        //let's check the length of each section
        var lengthOfFirst = textFields.riskFactor.val().length;
        var lengthOfSecond = textFields.managementDis.val().length;
        var lengthOfThird = textFields.finanState.val().length;

        if (lengthOfFirst <= 30 || lengthOfSecond <= 30 || lengthOfThird <= 30) {
          alertify.alert("The input of those text fields appear to be very short. " +
            "We can't let this pass our check system. Sorry. Please click "+
            "<span class='red'>Unable to Complete</span> button.");
        }

        //add more tests/checks here

        //then send out ajax, trigger animation


      }else{
        alertify.alert("One or multiple textareas aren't filled." +
          " If the document is corrupted or doesn't contain all three fields, " +
          "please click the red button <span class='red'>Unable to Complete</span> "+
          " at the bottom of the page. ");
      }
    });

  })();


  /*Supporting functions*/

  /**
  *
  * When there is $.cookie('mturkId') field
  * update mturkId on the server side.
  *
  **/
  (function autoFillmTurkId() {
    if ($.cookie('mturkId')) {
      $('#MturkId').val($.cookie('mturkId'));
      $("input").prop('disabled', true);
    }
  })();

  function updateDocLinks(promise) {
    promise.then(function(data) {
        $("#readInBrowser").attr('href', data.htmlURL);
        $("#downloadFile").attr('href', data.txtURL);
      });
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
        .fail(function (jqXHR, textStatus, errorThrown) {
          glo.ajaxFailureHandle(jqXHR, uris.webOneCompany._2, textStatus, errorThrown);
        });
      });
    }
  }

    function unableToComplete() {
      return companyURIPromise.then(function(uris) {
        return Q($.ajax({
          url: uris.webUnableToComp._2,
          type: uris.webUnableToComp._1,
          datatype: "json",
          contentType: 'application/json; charset=UTF-8'
        }))
        .then(function(data) {
          return data;
        })
        .fail(function(jqXHR, textStatus, errorThrown) {
          glo.ajaxFailureHandle(jqXHR, uris.webUnableToComp._2, textStatus, errorThrown);
        });
      });
    }


  return {
    fileURIPromise: fileURIPromise
  };

}(jQuery, global_access, animate, alertify));


