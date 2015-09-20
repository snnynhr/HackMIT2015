import pymongo
import math
import sympy
import sys

con = pymongo.MongoClient(host='127.0.0.1', port=3001)
db = con.meteor

# The heatmap will have (2 * num_rows - 1) rows and (2 * num_columns + 1) 
# columns. The origin cell will be the center cell (num_rows, num_columns).
num_rows = 5
num_columns = 5
cell_width = 0.5
cell_height = 0.5

heatmap = []
cell_radius = (math.sqrt(cell_width ** 2 + cell_height ** 2) + 
  (cell_width + cell_height) / 2) / 4
person_radius = 0.5
objects = []
people = []

###########################
# Heatmap Init and I/O
###########################

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
  for i in range(2*num_rows+1):
    heatmap.append([0.0] * (2*num_columns+1))

def print_heatmap():
  for i in heatmap:
    print i

###########################
# Vector utilities
###########################

def segments_intersect(x1, y1, x2, y2, x3, y3, x4, y4):
  seg1 = sympy.Segment(sympy.Point(x1, y1), sympy.Point(x2, y2))
  seg2 = sympy.Segment(sympy.Point(x3, y3), sympy.Point(x4, y4))
  return sympy.intersection(seg1, seg2) != []

def distance(x1, y1, x2, y2):
  return math.sqrt((x2 - x1) ** 2 + (y2 - y1) ** 2)

def degrees_to_rad(degrees):
  return degrees / 180.0 * math.pi

# angle in degrees
def get_y(degrees, distance):
  return distance * math.sin(degrees_to_rad(degrees))

# angle in degrees
def get_x(degrees, distance):
  return distance * math.cos(degrees_to_rad(degrees))

###########################
# Heatmap people operations
###########################

def get_cell_location(row, col):
  return ((col - num_columns) * cell_width, (num_rows - row) * cell_height)

def person_in_cell(xcoord, ycoord, row, col):
  (cell_x, cell_y) = get_cell_location(row, col)
  # TODO: possibly replace this with more complex square intersection formula.
  return (distance(xcoord, ycoord, cell_x, cell_y) <=
    cell_radius + person_radius)

def add_person_heatmap(xcoord, ycoord):
  global heatmap
  for row in range(2*num_rows+1):
    for col in range(2*num_columns+1):
      if person_in_cell(xcoord, ycoord, row, col) and heatmap[row][col] != -1.0:
        heatmap[row][col] += 1.0

# people is of type [(angle1, distance1), (angle2, distance2),...
# Angles in degrees.
def process_people(people):
  for (angle, distance) in people:
    add_person_heatmap(get_x(angle, distance), get_y(angle, distance))
  update_frontend_heatmap(heatmap)

###########################
# Heatmap Object operations
###########################

def cell_intersect_line(row, col, x1, y1, x2, y2):
  (cell_x, cell_y) = get_cell_location(row, col)
  (p0x, p0y) = (cell_x - cell_width / 2.0, cell_y - cell_height / 2.0)
  (p1x, p1y) = (cell_x + cell_width / 2.0, cell_y - cell_height / 2.0)
  (p2x, p2y) = (cell_x + cell_width / 2.0, cell_y + cell_height / 2.0)
  (p3x, p3y) = (cell_x - cell_width / 2.0, cell_y + cell_height / 2.0)
  return (segments_intersect(p0x, p0y, p1x, p1y, x1, y1, x2, y2) or
    segments_intersect(p1x, p1y, p2x, p2y, x1, y1, x2, y2) or
    segments_intersect(p2x, p2y, p3x, p3y, x1, y1, x2, y2) or
    segments_intersect(p3x, p3y, p0x, p0y, x1, y1, x2, y2))

def add_line_heatmap(x1, y1, x2, y2):
  for row in range(2*num_rows+1):
    for col in range(2*num_columns+1):
      if cell_intersect_line(row, col, x1, y1, x2, y2):
        heatmap[row][col] = -1.0

def add_object_heatmap(obj):
  object_xy = []
  for (angle, distance) in obj:
    object_xy.append((get_x(angle, distance), get_y(angle, distance)))
  for i in range(len(object_xy) - 1):
    (x1, y1) = object_xy[i]
    (x2, y2) = object_xy[i+1]
    add_line_heatmap(x1, y1, x2, y2)

# objects is of type [object1, object2, ...].
# object1 = [(1.2, 2.1), (3.2, 5.6), ...] where the list contains
# 2D outline of the object in (angle degrees, distance) coordinates.
def process_objects(objects):
  for obj in objects:
    add_object_heatmap(obj)

###########################
# Parsing Code
###########################

def parse_background(word_array):
  global objects
  if word_array[1] == 'START' or word_array[1] == 'END':
    return
  if word_array[1] == 'POLY1':
    objects.append([])
  objects[len(objects) - 1].append((float(word_array[2]), float(word_array[3])))

def parse_person(word_array):
  global people
  people.append((float(word_array[1]), float(word_array[2])))

def keep_parsing():
  global people
  processingBackground = True
  reset_heatmap()
  while True:
    line = raw_input("")
    word_array = line.split()
    if word_array[0] == 'BACKGROUND':
      processingBackground = True
      parse_background(word_array)
    elif word_array[0] == 'OBJECT':
      parse_person(word_array)
      if processingBackground:
        # process_objects(objects)
        print objects
        processingBackground = False
    elif word_array[0] == 'TIMESLICE':
      process_people(people)
      people = []

if __name__ == "__main__":
  keep_parsing()
  # TODO insert parsing code, and call process_people
  # reset_heatmap()
  # process_objects([[(180, 1), (0, 1), (30, 1.5)]])
  # process_people([(200, 1), (90, 1)])
  # print_heatmap()
