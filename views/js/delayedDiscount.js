// var delayedTurk = (function($, win, glo, alertify) {

//  /**
//   * Main logic
//   **/

//   //Configured for IE 10
//   if (!win.location.origin)
//    win.location.origin = win.location.protocol+"//"+win.location.host;

//   var baseHostName = win.location.origin;

//   var versionOfTest = "";

//   var delayedDiscountURIPromise = glo.delayedDiscountURIPromise;

//   var todayDollarValue = ["$9.90", "$9.75", "$9.50", "$9.25", "$9.00", "$8.50", "$8.00", 
//   	"$7.50", "$7.00", "$6.50", "$6.00", "$5.50", "$5.00", "$4.50", "$4.00", "$3.50", "$3.00", 
//   	"$2.50", "$2.00", "$1.50", "$1.00", "$0.75", "$0.50", "$0.25", "$0.10"];

//   var intervals = ["in 1 day", "in 2 days", "in 1 week", "in 2 weeks", "in 1 month", "in 6 months", "in 2 years"];
  
//   if (baseHostName.indexOf("delayedDiscountTurkv1")) {
//   	console.log("version 1 of the test detected!");
//   	versionOfTest = "TodayOnLeft";
//   }

//   if (baseHostName.indexOf("delayedDiscountTurkv2")) {
//   	console.log("version 2 of the test detected!");
//   	versionOfTest = "TodayOnRight";
//   }

//   //main run

//   // run(10, "trial");
//   // run(175, "exp")


//   function sendOutData(data) {
//   	return delayedDiscountURIPromise.then(function(uris) {
//          return Q($.ajax({
//             url: uris.delayedResultUpload._2,
//             type: uris.delayedResultUpload._1,
//             dataType: "json",
//             contentType: 'application/json; charset=UTF-8',
//             data: JSON.stringify(data)
//           }))
//           .fail(function(jqXHR) {
//             glo.ajaxFailureHandle(jqXHR, uris.delayedResultUpload._2);
//           });
//       });
//   }

//   	var listening = false;

//   function run(numOfTrials, expType) {

//   	listening = true;

//   	//response is reached and changed from handler
  	
//   	var thisTodayDollarAmount = _.shuffle(todayDollarValue);
//   	var thisIntervals = _.shuffle(intervals);
  
//   	$('body').keydown(handler);

//   	//unbind the event to prevent missfire when exp is done or haven't started
//   	$('body').unbind('keydown', handler);
//   }

//   //because of closure, I can access variables outside of handler
//   function handler(e) {
//       if (!listening) {
//         return;
//       }

//       var response = "";

//       switch (e.keyCode) {
//       	case 37:
//           response = "L";
//           swap();
//           break;
//         case 39:
//           response = "R";
//           break;
//         default:
//           response = "";
//           break;
//       }

//   }

//   function swap(response, remainTodayDollarAmount, remainIntervals) {

    
//   }

// }(jQuery, window, mturk.global_access, alertify));

var delayedTurk = (function($, win, glo, alertify) {
  /**
  * Main logic
  **/
  //run program
   TriadExperiment(10, "practice");

   if (!win.location.origin)
   win.location.origin = win.location.protocol+"//"+win.location.host;

  var baseHostName = win.location.origin;

  var todayLeftOrRight = "";

  if (baseHostName.indexOf("delayedDiscountTurkv1")) {
  	todayLeftOrRight = "TodayLeft";
  }else{
  	todayLeftOrRight = "TodayRight";
  }

  var delayedDiscountURIPromise = glo.delayedDiscountURIPromise;

  /**
  * Functions used to send out Ajax
  * testing: triadTurk.sendOutData(
  * {phase:"phase", verbTop:"A", verbTopCate:"B", verbLeft:"B", 
    verbLeftCate:"C", verbRight:"C", verbRightCate:"D", predict:"E", 
    reactionTime:"rt", response:"response"});
  **/
  var triadURIPromise = glo.triadURIPromise;

  function sendOutData(data) {
  	return delayedDiscountURIPromise.then(function(uris) {
         return Q($.ajax({
            url: uris.delayedResultUpload._2,
            type: uris.delayedResultUpload._1,
            dataType: "json",
            contentType: 'application/json; charset=UTF-8',
            data: JSON.stringify(data)
          }))
          .fail(function(jqXHR) {
            glo.ajaxFailureHandle(jqXHR, uris.delayedResultUpload._2);
          });
      });
  }
  

  function TriadExperiment(numTrials, phase) {
    var num = 0;
    var wordon, listening = false;
    var todayDollarValue = ["$9.90", "$9.75", "$9.50", "$9.25", "$9.00", "$8.50", "$8.00", 
  	"$7.50", "$7.00", "$6.50", "$6.00", "$5.50", "$5.00", "$4.50", "$4.00", "$3.50", "$3.00", 
  	"$2.50", "$2.00", "$1.50", "$1.00", "$0.75", "$0.50", "$0.25", "$0.10"];

  	var intervals = ["in 1 day", "in 2 days", "in 1 week", "in 2 weeks", "in 1 month", "in 6 months", "in 2 years"];
  
    todayDollarValues = _.shuffle(todayDollarValue);
    intervals = _.shuffle(intervals);

    var todayDollarValue = "";
    var interval = "";

    function next() {
      if (num === numTrials) {
        finish();
      } else {
        num = num + 1;
        todayDollarValue = todayDollarValues.shift();
        interval = intervals.shift();
        
        show_word(todayDollarValue, interval);
        
        wordon = (new Date).getTime();
        listening = true;
      }
    }

    function response_handler(e) {
      if (!listening) {
        return;
      }

      var keyCode = e.keyCode, response;
      switch(keyCode) {
        case 37:
          response = "L";
          console.log("pressed L");
          break;
        case 39:
          response = "R";
          console.log("pressed R");
          break;
        default:
          response = "";
          break;
      }
      if (response.length > 0) {
        listening = false;
        var rt = String((new Date).getTime() - wordon);

        // val fields = List("commToken","version", "phase", "response", "todayLeftOrRight", "timeIntervalForDelay",
        // "dollarToday", "reactionTime")
        sendOutData({
        	commToken:$.cookie("commToken"), 
        	"version": 1, //this is preparing for future change
        	"phase":phase,
        	"response":response, 
        	"todayLeftOrRight":todayLeftOrRight, 
        	"timeIntervalForDelay":$('#delayedTime').text(), 
        	"dollarToday":$('#todayDollar').text().substr(1), 
        	"reactionTime":rt
        });
        remove_word();
        next();
      }
    }

    /**
    * call function sendOutData()
    * supposed to be called after each key click
    **/
    function sendResponse(response) {

    }

    function finish() {
      if (phase == "exp") {
        $("body").unbind("keydown", response_handler);
        var endingHtml = "<div class='row'><div class='large-12 columns'>" + "<h4>Congratulations!</h4>" + "<br><br><p>You have completed the task. Before you close this window, " + "be sure to copy and paste this Authentication Code back to your Qualtrics page: </div></div><br>" + "<div class='row text-center'><div class='large-12 columns'><kbd>" + $.cookie("commToken") + "</kbd></div></div><br><br>";
        alertify.alert(endingHtml);
      }
      else if (phase == "practice") {
        var phaseChangeHtml = "<div class='row'><div class='large-12 columns'>" + "<h4>Next Phase Ahead</h4>" + "<br><br><p>Great! You've finished the practice trials. Please continue on to the experiment trials.</p>" + "</div></div><br>";
        alertify.confirm(phaseChangeHtml, function(e) {
          TriadExperiment(175, "exp");
        });
        
      }
    }

    function show_word(todayValue, interval) {
       $('#todayDollar').text(todayValue);
  	   $('#delayedTime').text(interval);
    }

    function remove_word() {
      $("#todayDollarAmount").text("");
      $("#delayedInterval").text("");
    }

    $("body").focus().keydown(response_handler);
    next();
  }

  return {
    sendOutData: sendOutData,
    delayedDiscountURIPromise: delayedDiscountURIPromise
  };
}(jQuery, window, mturk.global_access, alertify));