Heatmap = new Mongo.Collection("heatmap");
 
if (Meteor.isClient) {
  Tracker.autorun(function () {
    if (Heatmap.findOne() != undefined) {
      display_heatmap(Heatmap.findOne().contents);
    }
  });
}
