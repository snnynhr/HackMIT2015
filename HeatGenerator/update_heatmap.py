import pymongo
import math

con = pymongo.MongoClient(host='127.0.0.1', port=3001)
db = con.meteor

# The heatmap will have (2 * num_rows - 1) rows and (2 * num_columns + 1) 
# columns. The origin cell will be the center cell (num_rows, num_columns).
num_rows = 10
num_columns = 10
cell_width = 1.0
cell_height = 1.0

heatmap = []
cell_radius = (math.sqrt(cell_width ** 2 + cell_height ** 2) + 
  (cell_width + cell_height) / 2) / 4
person_radius = 0.5
objects = []

def update_frontend_heatmap(array):
  result = db.heatmap.update_one(
    {"name": "default"},
    {
      "$set": {
          "contents": array
      }
    }
  )

def reset_heatmap():
  global heatmap
  heatmap = [[0.0] * num_columns] * num_rows

# objects is of type [object1, object2, ...].
# object1 = (name, [(1.2, 2.1), (3.2, 5.6), ...]) where the list contains
# 2D outline of the object.
def process_objects(objects):
  pass

def distance(x1, y1, x2, y2):
  return math.sqrt((x2 - x1) ** 2 + (y2 - y1) ** 2)

def get_cell_location(row, col):
  return ((row - num_rows) * cell_width, (col - num_columns) * cell_height)

def person_in_cell(xcoord, ycoord, row, col):
  (cell_x, cell_y) = get_cell_location(row, col)
  # TODO: possibly replace this with more complex square intersection formula.
  return distance(xcoord, ycoord, cell_x, cell_y) <=
    cell_radius + person_radius

def add_person_heatmap(xcoord, ycoord):
  global heatmap
  for row in range(2*num_rows+1):
    for col in range(2*num_columns+1):
      if person_in_cell(xcoord, ycoord, row, col):
        heatmap[row][col] += 1.0

# people is of type [(id1, 0.5, 0.6), (id2, 0, 1.2), (id3, -0.1, -0.6)].
def process_people(people):
  for (_, xcoord, ycoord) in people:
    add_person_heatmap(xcoord, ycoord)
  update_frontend_heatmap(heatmap)

# TODO insert parsing code, and call process_people

# update_frontend_heatmap([800,6,89]);
