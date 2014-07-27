/* Global library settings */
alertify.set({ buttonFocus: "none" });

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
	* re-establish identity
	* and replace mTurkIdentity with new identity
	* return mTurkIdentity, so no need to call again.
	* Cookies are reset as well
	**/
	function refresh() {
		$.removeCookie('commToken');
		$.removeCookie('countTask');

		var promiseObj = _establishIdentity();

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
		ajaxFailureHandle: ajaxFailureHandle,
		refresh: refresh
	};

}(jQuery, window, location, alertify));

var animate = (function($, glo, alertify) {
	'use strict';

	function taskComplete(progressNum) {
		scrollTop(animateProgressBar, progressNum);
		collapseAnimate(progressNum, $("#panel-instruction-content"));
	}

	function taskFail() {
		alertify.alert("Message");
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
	*	1). turn input field unable to edit (with value saved inside)
	*	2). Submit to server
	* 2. One independent cycle of retrieve doc
	*   1). Change the link on "Read in browser" / "download" dynamically
	*	2). After change, alertifyjs user
	* 3. Task Complete action
	*	1). Examine integrity of extraction (empty fields...)
	*	2). Submit result to server (receive complete user info); update cookie
	*	3). task #2 retrieve cycle
	*	4). animate.taskComplete() to scroll to top and animate progress bar
	*	5). Clear text fields
	* 4. Task Unable to Complete action
	*	1). Submit to server; update cookie
	*	2). 
	* 
	**/

	var turkerId = glo.mTurkIdentity;

	var mTurkURIPromise = glo.mTurkURIPromise;

	var companyURIPromise = glo.companyURIPromise;

	function savemTurkID() {
		
	}

	//talk to backend to retrieve a new doc's url
	//and update on html page
	function loadNewDocURL() {
		companyURIPromise.then(function(data) {
			console.log(data);
		});
	}

	loadNewDocURL();

	return {
		saveId:savemTurkID()
	};

}(jQuery, global_access, animate, alertify));


