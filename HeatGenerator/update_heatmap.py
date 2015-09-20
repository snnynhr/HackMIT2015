import pymongo

con = pymongo.MongoClient(host='127.0.0.1', port=3001)
db = con.meteor

def update_heatmap(array):
  result = db.heatmap.update_one(
    {"name": "default"},
    {
      "$set": {
          "contents": array
      }
    }
  )

update_heatmap([800,6,89]);
