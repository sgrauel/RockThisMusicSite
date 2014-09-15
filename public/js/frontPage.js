/**
 * Created by electro on 6/16/14.
 */

(function () {


    var storefront = angular.module('storefront',[]);

    /*
     TabController
     what ? controls the display of pages depending on what tab is selected on a content bar
     i.e. initializes tab model to a certain value depending on which tab is selected
    */
    storefront.controller('TabController', function() {

        this.tab = 1;

        this.isSet = function(checkTab) {
            return this.tab === checkTab;
        };


        this.setTab = function(activeTab) {
            this.tab = activeTab;
        };

    });

    /*
    formController governs client-side validation and form submission to the server via POST request
    */
    storefront.controller('formController',['$scope','$http', function($scope,$http){


        this.form = {
            name : "",
            address : "",
            city : "",
            state : "",
            zip : "",
            country : "",
            phone : "",
            email : "",
            message : ""
        };

        // get length of message
        this.messageLength = function(message) {
            return message.length;
        };

        // get character limit i.e. maximum length of message minus actual message length
        this.characterLimit = function(someMessage) {
            this.messageLength = function(message) {
                return message.length;
            };
            return  messageLength(someMessage) - 1732;
        };


        // logic to send POST request to server
        // NOTE: re-implement this POST inside a service as services govern the request / reception of data

        this.submitForm = function () {

            return $http.post('http://localhost:9000/home-again', this.form).
                success(function(data, status, headers, config) {
                    // this callback will be called asynchronously
                    // when the response is available
                    
                    console.log("success");
                    console.log(JSON.stringify(status));
                }).
                error(function(data, status, headers, config) {
                    // called asynchronously if an error occurs
                    // or server returns response with an error status.
                    
                    console.log("fail");
                    console.log(JSON.stringify(status));
                });

        };

        // constraints for input fields data

        this.constraints = {
          name: [3,71],
          address: [2,95],
          city: [2,35],
          state: [2,52],
          zip: [5,5],
          phone: [10,10],
          email: [3,254],
          message: [0,1732]
        };

    }]);

    storefront.controller('carouselController', function() {

      this.interval = 3000;

        // array of slide objects
        this.slides = [{ image : 'http://localhost:9000/public/img/BostonCityFlow1366x911.jpg', headline : 'Example headline.',
            text : 'Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.',
            buttonText : 'Sign up Today'},
            { image : 'http://localhost:9000/public/img/RedBench1604x1080.jpg', headline : 'Another example headline.',
                text : 'Praeterea sublata cognitione et scientia tollitur omnis ratio et vitae degendae et rerum gerendarum.',
                buttonText : 'Click here!' },
            { image : 'http://localhost:9000/public/img/FallingAsleepForest1920x1080.jpg', headline : 'One more for good measure.',
                text : 'Quicquid porro animo cernimus, id omne oritur a sensibus, qui si omnes veri erunt, ut Epicuri ratio docet, tum denique poterit aliquid cognosci et percipi. Multi etiam, ut te consule, ipsi se indicaverunt.',
                buttonText : 'Learn more'
            }];

    });



})();
