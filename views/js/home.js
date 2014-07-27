var global_access = (function($, window) {
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
	**/
	function retrieveIdentity() {
		if($.cookie('commToken') && $.cookie('countTask')) {
			return {
				'commToken' : $.cookie('commToken'),
				'countTask' : $.cookie('countTask'),
				'promiseObj' : null
			};
		}else{
			return {
				'commToken' : null,
				'countTask' : null,
				'promiseObj' : _establishIdentity()
			};
		}
	}

	/**
	* re-establish identity
	* and replace mTurkIdentity with new identity
	* return mTurkIdentity, so no need to call again.
	**/
	function refresh() {
		return {
			'commToken' : $.cookie('commToken'),
			'countTask' : $.cookie('countTask'),
			'promiseObj' : _establishIdentity()
		};
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
			return $.ajax({
				url: mTurkUris.mTurkerGet._2,
				type: mTurkUris.mTurkerGet._1,
				dataType: 'JSON'
			})
			.then(function(data){
				return data;
			})
			.fail(function(jqXHR, textStatus, errorThrown) {
				ajaxFailureHandle(textStatus, errorThrown);
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
			ajaxFailureHandle(textStatus, esrrorThrown);
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
			ajaxFailureHandle(textStatus, errorThrown);
		});
	}

	/**
	* Handle ajax error
	* produce console.error()
	* but depending on situation, might
	* print out error on browser
	*/
	function ajaxFailureHandle(textStatus, errorThrown) {
		console.error("Ahhhh: " + textStatus + " - " + errorThrown);
	}

	return {
		mTurkIdentity: mTurkIdentity,
		mTurkURIPromise: mTurkURIPromise,
		companyURIPromise: companyURIPromise,
		ajaxFailureHandle: ajaxFailureHandle,
		refresh: refresh
	};

}(jQuery, window));

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

	function taskTextChange() {

	}

	return {
		taskComplete: taskComplete,
		taskFail: taskFail
	};


}(jQuery, global_access, alertify));

var app = (function($, glo) {
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

	var turkerId = global.mTurkIdentity();


	function savemTurkID() {
		
	}

	return {
		saveId:savemTurkID()
	};

}(jQuery, global_access));


