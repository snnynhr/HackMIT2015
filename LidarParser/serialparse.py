import serial
import sys

def align(ser):
	endpacket = ser.read(1)
	while(endpacket != b'\xfa'):
		#print("aligning", endpacket)
		endpacket = ser.read(1)
	#print("aligned")
	ser.read(21)
	return 
	
def checksum(data):
    """Compute and return the checksum as an int."""
    # group the data by word, little-endian
    data_list = []
    for t in range(10):
        data_list.append( data[2*t] + (data[2*t+1]<<8) )
 
    # compute the checksum on 32 bits
    chk32 = 0
    for d in data_list:
        chk32 = (chk32 << 1) + d
 
    # return a value wrapped around on 15bits, and truncated to still fit into 15 bits
    checksum = (chk32 & 0x7FFF) + ( chk32 >> 15 ) # wrap around to fit into 15 bits
    checksum = checksum & 0x7FFF # truncate to 15 bits
    return int( checksum )

ser = serial.Serial(2, 115200, timeout=1)

#print(ser.name)

align(ser)

while(1):
	line = ser.read(22)
	
	if(len(line) != 22):
		#print("length not 22")
		continue
	
	if(line[0] != 0xfa):
		#print(line[0])
		#print("ALIGNMENT ERROR")
		align(ser)
		
	else:
		if(checksum(line) != (int(line[20]) + int(line[21]<<8))):
			#print(checksum(line))
			#print(int(line[20]) + int(line[21]<<8))
			#print("CHECKSUM ERROR")
		
		#print(line)
		
		index = int(line[1]) - 0xA0 # index
		#print("index: ", index)
		
		for i in range(0,4):
			strength_warning = False
			speed = int(line[2]) + (int(line[3])<<8)	
			angle = 4*index + i			
			if int(line[4+4*i+1])&0b10000000 != 0:
				continue #invalid data flag set
			if int(line[4+4*i+1])&0b01000000 != 0:
				#print("Strength warning")
				strength_warning = True
			hibyte = int((line[4+4*i+1]&0b00111111))
			lobyte = int((line[4+4*i]))
			#print(hex(hibyte), hex(hibyte<<8), hex(lobyte), hex((hibyte<<8) + lobyte))
			distance = (hibyte<<8) + lobyte
			
			
			
			print(angle, distance, strength_warning)
			sys.stdout.flush()
			#if(angle < 2):
				#print(angle, distance, strength_warning)
				#print('#'*(distance>>8))
				#print('#'*(lobyte>>4))
				#print('#'*((distance>>5)&0b1111111))