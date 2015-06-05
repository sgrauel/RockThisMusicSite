/**
 * Created by electro on 6/16/14.
 */

(function () {


    var storefront = angular.module('storefront',['ui.bootstrap', 'angularFileUpload']);


    // service for making asynchronous GET request for youTube API data from server
    storefront.factory('ytPlaylistService', ['$http', '$sce', function($http,$sce) {

        var doRequest = function(url,thisVar) {
            return $http.get(url).success(function(galleryData){


                // list of video interviews to distribute evenly over two collumns for one or more rows

                thisVar.galleryList = galleryData;

                // create of even indicies for each collumn and even numbered videos
                for (var i = 0; i < thisVar.galleryList.length; i += 2) {
                    thisVar.galleryIndiciesEven.push(i);
                }

                // fill up an array of youtube videoId URL references and mark url as safe to use in ng-src context
                // i.e. creating a whitelist of trusted urls

                for (var i = 0; i < thisVar.galleryList.length; i++) {
                    // mark url as safe to use in ng-src context i.e. creating a whitelist of trusted urls
                    var safeUrl = $sce.trustAsResourceUrl("http://www.youtube.com/embed/" +
                        thisVar.galleryList[i].videoId + "?autoplay=1");
                    thisVar.videoIdUrls.push(safeUrl);
                }



            });
        }

        return {
            requestPlaylist: function(url,thisVar) { return doRequest(url,thisVar); }
        };
    }]);


    // service for making asynchronous POST request to server to submit form

    storefront.factory('formSubmission', ['$http', function($http) {

        var doRequest = function (url,form) {

            return $http.post(url,form).
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

        return {
            submitForm: function(url,form) { return doRequest(url,form); }
        };
    }]);


    // service for obtaining Feature's list from server

    storefront.factory('featureService', ['$http', function($http) {

        var doRequest = function(url,thisVar) {
            return $http.get(url).success(function(featureData){


                // list of video interviews to distribute evenly over two collumns for one or more rows

                thisVar.featureList = featureData;

            });
        };

        return {
            requestFeatures: function(url,thisVar) { return doRequest(url,thisVar); }
        };
    }]);



    // service for constructing objects e.g. Feature's object

    storefront.factory('constructObjects', function () {

        // model classes for feature objects

        function BandVideo(bandVideoUrl,title) {
            this.bandVideoUrl = bandVideoUrl;
            this.title = title;
        };

         function Website(websiteUrls,websiteNames,show,selectedWebsite) {
             this.websiteURLs = websiteUrls;
             this.websiteNames = websiteNames;
             this.show = show;
             this.selectedWebsite = selectedWebsite;
         };

         function Intro(introTitle,introductoryText) {
             this.introTitle = introTitle;
             this.introductoryText = introductoryText;
         };

         function Caption (artistTitle,artistImage,captionText,website) {
             this.artistTitle = artistTitle;
             this.artistImage = artistImage;
             this.captionText = captionText;
             this.website = website;
         };

         function Feature (date,time,intro,caption,audioPlayer,listOfBandVideos) {

             // feature object constructor
             this.date = date;
             this.time = time;
             this.intro = intro;
             this.caption = caption;
             this.audioPlayer = audioPlayer;
             this.listOfBandVideos = listOfBandVideos;

         };

         return {
             Feature: function (date, time, intro, caption, audioPlayer, listOfBandVideos) {
                 return new Feature(date, time, intro, caption, audioPlayer, listOfBandVideos);
             },
             Caption: function (artistTitle,artistImage,captionText,website) {
                 return new Caption(artistTitle,artistImage,captionText,website);
             },
             Intro: function (introTitle,introductoryText) {
                 return new Intro(introTitle,introductoryText);
             },
             Website: function (websiteUrls,websiteNames,show,selectedWebsite) {
                 return new Website(websiteUrls,websiteNames,show,selectedWebsite);
             },
             BandVideo: function (bandVideoUrl,title) {
                 return new BandVideo(bandVideoUrl,title);
             }
         };

    });

    storefront.directive('ngHeader', function () {

       return {
           restrict: 'E',
           templateUrl: 'http://localhost:9000/header'
       }

    });


    storefront.directive('ngFeatures', function () {

        return {
            restrict: 'E',
            templateUrl: 'http://localhost:9000/features',
            controller: 'featuresController as featuresCtrl'
        }

    });

    storefront.controller('featuresController', ['$sce', '$scope', 'featureService', function ($sce, $scope, featureService) {

        var thisVar = this;

        thisVar.featureList = [];

        // model for hiding / showing artist websites
        $scope.show = false;

        // model for a selected website
        $scope.selectedWebsite = -1;

        // request feature list from our server
        featureService.requestFeatures('http://localhost:9000/feature-list',thisVar);

        // whitelist an iframe
        $scope.whiteList = function (iframe) {
            var trustedIframePlayer = $sce.trustAsHtml(iframe);
            return trustedIframePlayer;
        };

        // return the feature list
        $scope.getFeatureList = function () {
            return thisVar.featureList;
        };

        // set show to true else false
        $scope.setShow = function () {
            if ($scope.show == false) $scope.show = true;
            else $scope.show = false;
        };

    }]);

    storefront.directive('ngAbout', function () {

        return {
            restrict: 'E',
            templateUrl: 'http://localhost:9000/about'
        }

    });

    // the directive describing the footer
    storefront.directive('ngFooter', function () {

        return {
            restrict: 'E',
            templateUrl: 'http://localhost:9000/footer'
        }

    });

    /*
     TabController
     what ? controls the display of pages depending on what tab is selected on a content bar
     i.e. initializes tab model to a certain value depending on which tab is selected
    */
    storefront.controller('TabController', function() {

        this.tab = 0;

        this.isSet = function(checkTab) {
            return this.tab === checkTab;
        };


        this.setTab = function(activeTab) {
            this.tab = activeTab;
        };

    });

    /*
    Contact form directive and controller governing client-side validation and form submission to the server via POST request
    */

    storefront.directive('ngContactForm', function () {

        return {
            restrict: 'E',
            templateUrl: 'http://localhost:9000/contact-form',
            controller: 'formController as formCtrl'
        }

    });

    storefront.controller('formController', ['formSubmission', function(formSubmission){


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


        // function for form submission
        // return a promise object passed to ng-submit

        this.submitForm = function () {
            formSubmission.submitForm('http://localhost:9000/home-again',this.form);
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

    // carousel directive and controller governing the structure and function of carousel component

    // carousel component will freeze i.e. will not cycle when described as a directive; could be due to the oddity of referencing of deps
    /*
    storefront.directive('ngFrontPage', function () {
        return {
            restrict: 'E',
            templateUrl: 'http://localhost:9000/front-page'
        }
    });
    */

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


    // interview gallery controller and directive describing the structure and function of interview gallery component

    storefront.directive('ngInterviewGallery', function () {
        return {
            restrict: 'E',
            templateUrl: 'http://localhost:9000/interview-gallery',
            controller: 'interviewGalleryController as interviewGalleryCtrl'
        }
    });

    storefront.controller('interviewGalleryController', ['ytPlaylistService', function(ytPlaylistService) {

        var thisVar = this;

        // list of video gallery interviews
        thisVar.galleryList = [];
        // list of even indicies mapping to every other video in video gallery interviews
        thisVar.galleryIndiciesEven = [];
        // list of video gallery interviews youtube API references to white-list and utilize in iframe
        thisVar.videoIdUrls = [];

        // control the selection of a widget
        thisVar.widgetId = -1;

        // request interviews data from our server
        ytPlaylistService.requestPlaylist("http://localhost:9000/rock-this-interviews",thisVar);

    }]);

    storefront.directive('ngPerformanceGallery', function () {

        return {
            restrict: 'E',
            templateUrl: 'http://localhost:9000/performance-gallery',
            controller: 'performanceGalleryController as performanceGalleryCtrl'
        }

    });

    storefront.controller('performanceGalleryController', ['ytPlaylistService', function(ytPlaylistService) {

        var thisVar = this;

        // list of video gallery interviews
        thisVar.galleryList = [];
        // list of even indicies mapping to every other video in video gallery interviews
        thisVar.galleryIndiciesEven = [];
        // list of video gallery interviews youtube API references to white-list and utilize in iframe
        thisVar.videoIdUrls = [];

        // control the selection of a widget
        thisVar.widgetId = -1;

        // request performaces data from our server
        ytPlaylistService.requestPlaylist("http://localhost:9000/rock-this-performances",thisVar);

    }]);

    // admin-panel directive, controller
    // TASK: data synchronized between admin-panel view and model, then sent to the server for persistence
    // a. features page
    // b. carousel
    // ect.
    // NOTE: inject the Features class as a service into admin-panel ctrl and feature ctrl

    storefront.directive('ngAdminPanel', function () {
        return {
            restrict: 'E',
            templateUrl: 'http://localhost:9000/admin-panel-directive',
            controller: 'adminPanelController as adminPanelCtrl'
        }
    });

    storefront.controller('adminPanelController', ['constructObjects', 'formSubmission', '$scope', function (constructObjects,formSubmission,$scope) {

        // models for datepicker input element

        // set to today's date
        $scope.today = function() {
            $scope.feature.date = new Date();
        };

        // clear date
        $scope.clear = function () {
            $scope.feature.date = null;
        };

        // models for admin panel tabs and sub-tabs
        $scope.showComponents = 1;
        $scope.showCRUD = 1;

        // create an empty feature object
        // date, intro, caption, audioPlayer, listOfBandVideos
        this.feature = $scope.feature = constructObjects.Feature("","",constructObjects.Intro("",""),constructObjects.Caption("","","",constructObjects.Website([],[],false,-1)),"",[]);

        // artist website functions

        // function for adding artist websites
        $scope.addArtistWebsite = function () {
            if ($scope.feature.caption.website.websiteURLs.length == 5) {
                console.log("Error. Stack cannot exceed a length of 5");
                return -1;
            } else {
                $scope.feature.caption.website.websiteURLs.push('');
                $scope.feature.caption.website.websiteNames.push('');
                return 0;
            }
        };

        // function for removing artist websites
        $scope.removeArtistWebsite = function () {
            if ( $scope.feature.caption.website.websiteURLs.length == 0) {
                console.log("Error. Cannot pop an empty stack!");
                return -1;
            } else {
                $scope.feature.caption.website.websiteURLs.pop();
                $scope.feature.caption.website.websiteNames.pop();
                return 0;
            }
        };

        $scope.xs = [];

        // takes an array length and generates an array of integers 1 ... n , where n is the array length
        $scope.generateNumberOfSites = function (num) {
            // clear 'xs' of any previous values
            $scope.xs = [];
            for (var i = 1; i <= num; i++) {
                $scope.xs.push(i);
            }
            return $scope.xs;
        };

        // band video functions

        // function for adding Band Videos
        $scope.addBandVideo = function () {
            if ($scope.feature.listOfBandVideos.length == 5) {
                console.log("Error. Stack cannot exceed a length of 5");
                return -1;
            } else {
                $scope.feature.listOfBandVideos.push(constructObjects.BandVideo('',''));
                return 0;
            }
        };

        // function for removing band videos
        $scope.removeBandVideo = function () {
            if ($scope.feature.listOfBandVideos.length == 0) {
                console.log("Error. Stack cannot pop an empty array");
                return -1;
            } else {
                $scope.feature.listOfBandVideos.pop();
                return 0;
            }
        };

        // function for clearing the form state
        $scope.clearForm = function () {
          // clear all input fields of form
          $scope.featureForm.$setPristine();
          // reset feature object to default
          $scope.feature = constructObjects.Feature("","",constructObjects.Intro("",""),constructObjects.Caption("","","",constructObjects.Website([],[],false,-1)),"",[]);
        };

        // function for submitting form

        $scope.submitForm = function () {
            formSubmission.submitForm("http://localhost:9000/feature-form",$scope.feature);
        };

        // iframe example
        $scope.iframe = "<iframe width='100%' height='500px' ...></iframe>";

        // contraints for input fields
        $scope.constraints = {
            introTitle: [1,250],
            introductoryText: [1,2500], // 2500 chars =~ 500 words if average word size is 5 chars
            artistTitle: [1,250],
            captionText: [1,2500], // 5000 chars =~ 1000 words
            audioPlayer: [1,1500] // about double the normal size of an iframe
        }


    }]);

    // timepicker controller and directive


    storefront.controller('timepickerCtrl', function ($scope, $log) {
        $scope.mytime = new Date();

        $scope.hstep = 1;
        $scope.mstep = 15;

        $scope.options = {
            hstep: [1, 2, 3],
            mstep: [1, 5, 10, 15, 25, 30]
        };

        $scope.ismeridian = true;
        $scope.toggleMode = function() {
            $scope.ismeridian = ! $scope.ismeridian;
        };

        $scope.update = function() {
            var d = new Date();
            d.setHours( 14 );
            d.setMinutes( 0 );
            $scope.$parent.feature.time = d;
        };

        $scope.changed = function () {
            $log.log('Time changed to: ' + $scope.mytime);
        };

        $scope.clear = function() {
            $scope.$parent.feature.time = null;
        };
    });


    storefront.directive('ngUploadImage', function () {
        return {
            restrict: 'E',
            templateUrl: 'http://localhost:9000/uploadimage',
            controller: 'fileUploadController as fileUploadCtrl'
        }
    });

    // controller that describes the behavior of uploading a file
    storefront.controller('fileUploadController', ['$scope', 'FileUploader', function ($scope,FileUploader) {

        var uploader = $scope.uploader = new FileUploader({
            url: 'http://localhost:9000/artist-image'
        });

        uploader.filters.push({
            name: 'imageFilter',
            fn: function(item /*{File|FileLikeObject}*/, options) {
                var type = '|' + item.type.slice(item.type.lastIndexOf('/') + 1) + '|';
                return '|jpg|png|jpeg|bmp|gif|'.indexOf(type) !== -1;
            }
        });

        uploader.onWhenAddingFileFailed = function(item /*{File|FileLikeObject}*/, filter, options) {
            console.info('onWhenAddingFileFailed', item, filter, options);
        };
        uploader.onAfterAddingFile = function(fileItem) {
            console.info('onAfterAddingFile', fileItem);
        };
        uploader.onAfterAddingAll = function(addedFileItems) {
            console.info('onAfterAddingAll', addedFileItems);
        };
        uploader.onBeforeUploadItem = function(item) {
            console.info('onBeforeUploadItem', item);
        };
        uploader.onProgressItem = function(fileItem, progress) {
            console.info('onProgressItem', fileItem, progress);
        };
        uploader.onProgressAll = function(progress) {
            console.info('onProgressAll', progress);
        };
        uploader.onSuccessItem = function(fileItem, response, status, headers) {
            console.info('onSuccessItem', fileItem, response, status, headers);
        };
        uploader.onErrorItem = function(fileItem, response, status, headers) {
            console.info('onErrorItem', fileItem, response, status, headers);
        };
        uploader.onCancelItem = function(fileItem, response, status, headers) {
            console.info('onCancelItem', fileItem, response, status, headers);
        };
        uploader.onCompleteItem = function(fileItem, response, status, headers) {
            console.info('onCompleteItem', fileItem, response, status, headers);
        };
        uploader.onCompleteAll = function() {
            console.info('onCompleteAll');
        };
        console.info('uploader', uploader);

    }]);








})();
