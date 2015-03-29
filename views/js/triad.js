var triadTurk = (function($, glo, alertify) {  /**  * Main logic  **/  if ($('#trial').length !== 0) {    TriadExperiment(10, "practice");  }  /**  * Functions used to send out Ajax  * testing: triadTurk.sendOutData(  * {commToken:$.cookie("commToken"), "phase":phase, "verbTop":A, "verbLeft":B,   *  "verbRight":C, "reactionTime":rt, "response":response};  **/  var triadURIPromise = glo.triadURIPromise;  function sendOutData(data) {       return triadURIPromise.then(function(uris) {         return Q($.ajax({            url: uris.triadResultUpload._2,            type: uris.triadResultUpload._1,            dataType: "json",            contentType: 'application/json; charset=UTF-8',            data: JSON.stringify(data)          }))          .fail(function(jqXHR) {            glo.ajaxFailureHandle(jqXHR, uris.triadResultUpload._2);          });      });   }    function TriadExperiment(numTrials, phase) {    var num = 0;    var wordon, listening = false;    var group = ["have", "make", "take", "offer", "bring", "reach", "decide", "report", "expect", "publish", "develop", "feature", "increase", "announce", "complete", "represent", "introduce", "establish", "burn", "view", "bury", "issue", "watch", "order", "select", "attack", "regard", "protect", "control", "acquire", "organize", "surround", "indicate", "encourage", "celebrate", "accompany", "test", "vary", "ride", "trade", "steal", "match", "instal", "attach", "prefer", "comment", "portray", "explore", "complain", "resemble", "persuade", "transform", "implement", "attribute", "pack", "wind", "pour", "shake", "clean", "sweep", "adjust", "invent", "revise", "scatter", "prevail", "divorce", "prohibit", "decrease", "classify", "interpret", "calculate", "authorize", "drag", "spin", "seat", "guess", "enact", "value", "attain", "access", "signal", "impress", "provoke", "balance", "diminish", "endanger", "coincide", "intervene", "advertise", "stimulate", "book", "bend", "bill", "abuse", "evoke", "blend", "scream", "remark", "denote", "enclose", "uncover", "conceal", "televise", "overturn", "showcase", "sacrifice", "intensify", "discharge", "raid", "plot", "sack", "level", "slice", "adore", "unfold", "bypass", "ratify", "caution", "narrate", "venture", "escalate", "heighten", "sanction", "subsidize", "formulate", "reinstate", "curb", "slay", "side", "sneak", "flash", "defer", "sprawl", "clinch", "branch", "distort", "augment", "harvest", "mobilize", "discount", "overtake", "befriend", "reiterate", "supersede", "slap", "poke", "farm", "evict", "crawl", "court", "unload", "detach", "puzzle", "deplete", "flatten", "warrant", "dispense", "forecast", "reaffirm", "fabricate", "discredit", "diversify", "camp", "dock", "trip", "drape", "glide", "stalk", "falter", "perish", "stroll", "exhaust", "silence", "deceive", "encircle", "alienate", "insulate", "eradicate", "safeguard", "persecute", "feud", "dial", "hack", "quell", "nurse", "taunt", "tamper", "hasten", "rework", "corrupt", "reshape", "espouse", "frighten", "infringe", "paralyze", "moderate", "leverage", "sabotage", "prey", "dine", "clip", "chill", "flush", "kneel", "parade", "salute", "mutate", "stagger", "triumph", "journey", "subtract", "decimate", "resettle", "embellish", "terrorize", "formalize", "bust", "tame", "punt", "waver", "scorn", "blink", "dismay", "endear", "tender", "repaint", "coexist", "concoct", "entangle", "handcuff", "confound", "reminisce", "galvanize", "epitomize", "belt", "lick", "herd", "smear", "morph", "clamp", "rebuke", "beware", "berate", "gratify", "enchant", "surmise", "embolden", "reassert", "flounder", "victimize", "outsource", "calibrate", "envy", "rave", "dawn", "spike", "pulse", "wedge", "exhume", "refund", "scorch", "nourish", "instill", "oppress", "amputate", "contrive", "mutilate", "forestall", "deprecate", "vaccinate", "pelt", "claw", "ooze", "braid", "stoop", "hitch", "recoil", "swivel", "menace", "meeting", "entwine", "idolize", "vanquish", "shepherd", "belittle", "castigate", "ostracize", "reinstall"];    var triads = group.length / 3;    var sequence = [];    for(var i = 0; i < triads; i++)      sequence.push(i);    sequence = _.shuffle(sequence);    //var stim = "";    var A = "";    var B = "";    var C = "";    var bLeft = false;    function next() {      if (num === numTrials) {        finish();      } else {        A = group[sequence[num] * 3 ];        if((new Date()).getTime() % 2 === 0)        {          B = group[sequence[num] * 3  + 1];          C = group[sequence[num] * 3  + 2];          bLeft = true;        }        else        {          B = group[ sequence[num] * 3  + 2];          C = group[ sequence[num] * 3  + 1];          bLeft = false;        }        var v = setTimeout(function() {          show_word1(A);        }, 1000);        var t = setTimeout(function() {          show_word2(B, C);        }, 1800);        wordon = (new Date()).getTime();        listening = true;        num = num + 1;      }    }    // function getWord(group) {    //   switch(group) {    //     case "A":    //       return groupA[Math.floor(Math.random() * (groupA.length - 1))];    //     case "B":    //       return groupB[Math.floor(Math.random() * (groupB.length - 1))];    //     case "C":    //       return groupC[Math.floor(Math.random() * (groupC.length - 1))];    //     case "D":    //       return groupD[Math.floor(Math.random() * (groupD.length - 1))];    //     default:    //       return;    //   }    // }    //     function response_handler(e) {      if (!listening) {        return;      }      var keyCode = e.keyCode, response;      switch(keyCode) {        case 37:          response = "L";          break;        case 39:          response = "R";          break;        default:          response = "";          break;      }      if (response.length > 0) {        listening = false;        var hit = response == stim[1];        var rt = String((new Date()).getTime() - wordon);        //sendOutData({commToken:$.cookie("commToken"), "phase":phase, "verbTop":A, "verbTopCate":stim[0], "verbLeft":B, "verbLeftCate":stim[1], "verbRight":C, "verbRightCate":stim[2], "predict":stim[3], "reactionTime":rt, "response":response});        sendOutData({commToken:$.cookie("commToken"), "phase":phase, "verbTop":A, "verbLeft":B, "verbRight":C, "reactionTime":rt, "response":response,          "sequence": sequence[num]});        remove_word();        next();      }    }    /**    * call function sendOutData()    * supposed to be called after each key click    **/    function sendResponse(response) {    }    function finish() {      if (phase == "exp") {        $("body").unbind("keydown", response_handler);        var endingHtml = "<div class='row'><div class='large-12 columns'>" + "<h4>Congratulations!</h4>" + "<br><br><p>You have completed the first two tasks. Before you close this window, " + "be sure to copy and paste this Authentication Code back to your Qualtrics page: </div></div><br>" + "<div class='row text-center'><div class='large-12 columns'><kbd>" + $.cookie("commToken") + "</kbd></div></div><br><br>";        alertify.alert(endingHtml);      }      else if (phase == "practice") {        var phaseChangeHtml = "<div class='row'><div class='large-12 columns'>" + "<h4>Next Phase Ahead</h4>" + "<br><br><p>Great! You've finished the practice trials. Please continue on to the experimental trials.</p>" + "</div></div><br>";        alertify.confirm(phaseChangeHtml, function(e) {          TriadExperiment(96, "exp");        });              }    }    function show_word1(text) {       $("#stim").append("<div id='word'>"+text+"</div>");    }    function show_word2(text1, text2) {      $("#stim2").append("<div id='wordBottom'>"+text1 + "&nbsp; &nbsp; &nbsp; &nbsp;" + text2+"</div>");    }    function remove_word() {      $("#word").remove();      $("#wordBottom").remove();    }    $("body").focus().keydown(response_handler);    next();  }  return {    sendOutData: sendOutData,    triadURIPromise: triadURIPromise  };}(jQuery, mturk.global_access, alertify));