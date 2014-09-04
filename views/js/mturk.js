/* Global library settings */
alertify.set({ buttonFocus: "none" });
//for logging messages
alertify.set({ delay: 10000 }); 

var mturk = {};

mturk.global_access = (function($, window, loc, alertify) {
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

  //Configured for IE 10
  if (!window.location.origin)
   window.location.origin = window.location.protocol+"//"+window.location.host;

  var baseHostName = window.location.origin;

  var mTurkURIPromise = mTurkURIretrieve();

  var companyURIPromise = companyURIretrieve();

  var triadURIPromise = triadURIretrieve();

  var delayedDiscountURIPromise = delayedDiscountURI();

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

  /**
  *
  * @returns a Promise object
  * the data looks like
  * {
    "triadResultUpload": {
        "_1": "POST",
        "_2": "http://127.0.0.1:8080/triad/result/"
    }
  }
  *
  **/
  function triadURIretrieve() {
    return Q($.ajax({
      url: baseHostName + '/triad/help',
      type: 'GET',
      dataType: 'JSON'
    }))
    .then(function(data) {
      return data;
    })
    .fail(function(jqXHR) {
      ajaxFailureHandle(jqXHR, baseHostName+'/triad/help');
    });
  }

  /**
  *
  * @returns a Promise object
  * the data looks like
  * {
    "delayedResultUpload": {
        "_1": "POST",
        "_2": "http://127.0.0.1:8080/delayed/result"
    }
  }
  *
  **/
  function delayedDiscountURI() {
    return Q($.ajax({
      url: baseHostName + '/delayed/help',
      type: 'GET',
      dataType: 'JSON'
    }))
    .then(function(data) {
      return data;
    })
    .fail(function(jqXHR) {
      ajaxFailureHandle(jqXHR, baseHostName+'/delayed/help');
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
    triadURIPromise: triadURIPromise,
    delayedDiscountURIPromise: delayedDiscountURIPromise,
    savemTurkID: savemTurkID,
    ajaxFailureHandle: ajaxFailureHandle,
    customAjaxFailureHandle: customAjaxFailureHandle,
    refresh: refresh,
    restart: restart
  };

}(jQuery, window, location, alertify));

/**
*
* Animate Module Features:
* 1. Progress Bar animation
* 2. Scroll animation
*
**/

mturk.animate = (function($, glo, alertify) {
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


}(jQuery, mturk.global_access, alertify));
