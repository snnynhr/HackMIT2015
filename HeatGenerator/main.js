Heatmap = new Mongo.Collection("heatmap");
 
if (Meteor.isClient) {
  // Template.lol.created = function() {
  //   if(!window._scriptsLoaded) {
  //     window._scriptsLoaded = true;
  //     console.log('Template onLoad');
  //   }
  // }
  Tracker.autorun(function () {
    if (Heatmap.findOne() != undefined) {
      display_heatmap(Heatmap.findOne().contents);
    }
  });
}
