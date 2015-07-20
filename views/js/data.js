/*
 * Real-time data generators for the example graphs in the documentation section.
 */
(function() {

    /*
     * Class for generating real-time data for the area, line, and bar plots.
     */
    var RealTimeData = function(layers) {
        this.layers = layers;
        this.timestamp = ((new Date()).getTime() / 1000)|0;
    };

    RealTimeData.prototype.rand = function() {
        var maximum = 7;
        var minimum = 0;
        var number = [65,43,25,14,32,17,43,90];
        var randomnumber = Math.floor(Math.random() * (maximum - minimum + 1)) + minimum;
        //var number = Math.random();
        console.log(number[randomnumber]);
        return parseInt(number[randomnumber]);
    };

    RealTimeData.prototype.history = function(entries) {
        if (typeof(entries) != 'number' || !entries) {
            entries = 60;
        }

        var history = [];
        for (var k = 0; k < this.layers; k++) {
            history.push({ values: [] });
        }

        for (var i = 0; i < entries; i++) {
            for (var j = 0; j < this.layers; j++) {
                history[j].values.push({time: this.timestamp, y: this.rand()});
            }
            this.timestamp++;
        }

        return history;
    };

    //what gets pushed
    RealTimeData.prototype.next = function() {
        var entry = [];
        for (var i = 0; i < this.layers; i++) {
            entry.push({ time: this.timestamp, y: this.rand() });
        }
        this.timestamp++;
        return entry;
    }

    window.RealTimeData = RealTimeData;


    /*
     * Gauge Data Generator.
     */
    var GaugeData = function() {};

    GaugeData.prototype.next = function() {
        return Math.random();
    };

    window.GaugeData = GaugeData;



    /*
     * Heatmap Data Generator.
     */

    var HeatmapData = function(layers) {
        this.layers = layers;
        this.timestamp = ((new Date()).getTime() / 1000)|0;
    };

    window.normal = function() {
        var U = Math.random(),
            V = Math.random();
        return Math.sqrt(-2*Math.log(U)) * Math.cos(2*Math.PI*V);
    };

    HeatmapData.prototype.rand = function() {
        var histogram = {};

        for (var i = 0; i < 1000; i ++) {
            var r = parseInt(normal() * 12.5 + 50);
            if (!histogram[r]) {
                histogram[r] = 1;
            }
            else {
                histogram[r]++;
            }
        }

        return histogram;
    };

    HeatmapData.prototype.history = function(entries) {
        if (typeof(entries) != 'number' || !entries) {
            entries = 60;
        }

        var history = [];
        for (var k = 0; k < this.layers; k++) {
            history.push({ values: [] });
        }

        for (var i = 0; i < entries; i++) {
            for (var j = 0; j < this.layers; j++) {
                history[j].values.push({time: this.timestamp, histogram: this.rand()});
            }
            this.timestamp++;
        }

        return history;
    };

    HeatmapData.prototype.next = function() {
        var entry = [];
        for (var i = 0; i < this.layers; i++) {
            entry.push({ time: this.timestamp, histogram: this.rand() });
        }
        this.timestamp++;
        return entry;
    }

    window.HeatmapData = HeatmapData;


})();
