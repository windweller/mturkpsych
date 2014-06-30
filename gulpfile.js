var gulp = require("gulp");
var browserify = require("browserify");

gulp.task("default", function () {
    console.log("gulp default mode starts working");
});

gulp.task("compile:js", function () {
	var bundle = browserify("");
});